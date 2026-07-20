#!/usr/bin/env bash
#
# Installs or updates this template's AI guidance in a target project.
#
# Usage (run from the root of the TARGET project):
#   /path/to/this/template/.ai/setup.sh <template-source> [--allow-dirty]
#
#   <template-source>  Local path to this template repo, or a git URL.
#   --allow-dirty       Skip the clean-working-tree check below. Not
#                       recommended — see why the check exists.
#
# What it does:
#   1. Requires a clean git working tree in the target project. This
#      script has no merge logic of its own — step 2 overwrites files
#      in place, and a clean tree plus `git diff` is what makes that
#      recoverable. Without it, an overwritten local edit is just gone.
#   2. Copies .ai/, .claude/agents/, .claude/skills/, .claude/settings.json,
#      and any detected .frameworks/<name>/ rule sets from the template
#      into the target, overwriting any existing file that differs. No
#      per-rule merging, no mutable/immutable handling, no flagging —
#      that reconciliation happens after this script runs, by reading
#      `git diff` against what it just overwrote. See the `sync-guidance`
#      skill, which walks that reconciliation: it restores locally-edited
#      `mutable` rule rows the copy clobbered, leaves `immutable` rows on
#      the template's version, and flags changed agent/skill/settings
#      files for a keep-local-or-take-template call. A project's own
#      `.ai/project-rules.md` (PROJ-* rules) is never touched by this
#      script at all, since the template doesn't ship one — nothing to
#      reconcile there.
#      Without an AI-assisted step, `git diff` after this script runs is
#      still a complete and accurate record of what changed — resolve it
#      by hand instead.
#
#      This step also DELETES any previously-installed file this run no
#      longer provides — e.g. an agent or skill the template dropped, or
#      a framework rule set for a framework that's no longer detected in
#      the target. It tracks what it installed last time in a manifest
#      at `.ai/.template-manifest`; anything in that manifest but not
#      produced by this run is removed. Files never listed in the
#      manifest (a project's own hand-added agent/skill) are never
#      touched. Deletions are plain working-tree removals — nothing is
#      staged or committed — so `git status`/`git diff` shows them like
#      any other change this script makes.
#   3. Generates or updates a managed block in CLAUDE.md wiring in the
#      constitution, installed agents/skills, and detected framework
#      rule sets. Content outside the managed block is left untouched.
#
# This script intentionally does not know about rule IDs, mutability
# tags, or table formats — that logic used to live here as an awk
# per-rule merge engine, which was fragile and hard to extend. Keeping
# this script to a dumb, deterministic copy (plus the manifest-driven
# deletion above, which only ever acts on files this script itself
# installed) makes the one thing it must get right (never silently
# destroy local work) easy to guarantee via git, and hands the genuinely
# fuzzy part (deciding what a diverged rule or file should end up as) to
# whoever reconciles the diff afterward.

set -eo pipefail

usage() { echo "Usage: $0 <template-source> [--allow-dirty]" >&2; exit 1; }

[[ -n "${1:-}" ]] || usage
TEMPLATE_SOURCE="$1"
ALLOW_DIRTY=false
[[ "${2:-}" == "--allow-dirty" ]] && ALLOW_DIRTY=true

TARGET_DIR="$(pwd)"
WORKDIR=""
cleanup() { if [[ -n "$WORKDIR" ]]; then rm -rf "$WORKDIR"; fi; }
trap cleanup EXIT

if [[ "$TEMPLATE_SOURCE" =~ ^https?:// ]] || [[ "$TEMPLATE_SOURCE" =~ \.git$ ]]; then
  WORKDIR="$(mktemp -d)"
  git clone --depth 1 "$TEMPLATE_SOURCE" "$WORKDIR" >/dev/null
  TEMPLATE_DIR="$WORKDIR"
else
  TEMPLATE_DIR="$(cd "$TEMPLATE_SOURCE" && pwd)"
fi

if [[ ! -d "$TEMPLATE_DIR/.ai" ]]; then
  echo "error: $TEMPLATE_DIR does not look like the ai-guidance template (.ai/ missing)" >&2
  exit 1
fi

if [[ "$ALLOW_DIRTY" == false ]]; then
  if ! git -C "$TARGET_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    cat >&2 <<EOF
error: $TARGET_DIR is not a git repository.

This script overwrites (and can delete) files under .ai/, .claude/, and
.frameworks/ in place with no merge step — git is what makes that
recoverable afterward. Init a repo first, or re-run with --allow-dirty
to proceed without that safety net (anything this script overwrites or
removes will be unrecoverable).
EOF
    exit 1
  fi
  if [[ -n "$(git -C "$TARGET_DIR" status --porcelain -- .ai .claude .frameworks CLAUDE.md 2>/dev/null)" ]]; then
    cat >&2 <<EOF
error: $TARGET_DIR has uncommitted changes under .ai/, .claude/, .frameworks/,
or CLAUDE.md.

Commit or stash them first. This script has no merge logic — a clean tree
is what lets 'git diff'/'git status' show exactly what it changed or
removed, so it can be reconciled or reverted afterward. Re-run with
--allow-dirty to skip this check.
EOF
    exit 1
  fi
fi

echo "Copying guidance from $TEMPLATE_DIR ..."
CHANGED=()
declare -A INSTALLED_SET=()

copy_tree() {
  local src_dir="$1" dest_dir="$2"
  [[ -d "$src_dir" ]] || return 0
  while IFS= read -r -d '' f; do
    local rel dest relpath
    rel="${f#"$src_dir"/}"
    dest="$dest_dir/$rel"
    relpath="${dest#"$TARGET_DIR"/}"
    mkdir -p "$(dirname "$dest")"
    if [[ ! -e "$dest" ]] || ! cmp -s "$f" "$dest"; then
      cp "$f" "$dest"
      CHANGED+=("$relpath")
    fi
    INSTALLED_SET["$relpath"]=1
  done < <(find "$src_dir" -type f -print0)
}

copy_file() {
  local src="$1" dest="$2" relpath
  [[ -f "$src" ]] || return 0
  relpath="${dest#"$TARGET_DIR"/}"
  mkdir -p "$(dirname "$dest")"
  if [[ ! -e "$dest" ]] || ! cmp -s "$src" "$dest"; then
    cp "$src" "$dest"
    CHANGED+=("$relpath")
  fi
  INSTALLED_SET["$relpath"]=1
}

copy_tree "$TEMPLATE_DIR/.ai" "$TARGET_DIR/.ai"
copy_tree "$TEMPLATE_DIR/.claude/agents" "$TARGET_DIR/.claude/agents"
copy_tree "$TEMPLATE_DIR/.claude/skills" "$TARGET_DIR/.claude/skills"
copy_file "$TEMPLATE_DIR/.claude/settings.json" "$TARGET_DIR/.claude/settings.json"

echo "Detecting frameworks ..."
DETECTED=()

# Search the whole project tree (no depth cap) but skip build output,
# dependency, and VCS directories — a maxdepth cap missed real projects
# that nest a .csproj/.sln (or, in a monorepo, a package.json/Cargo.toml/
# project.godot) more than a few levels deep. Shared by has_file and
# content_matches below.
PRUNE_DIRS=( -name node_modules -o -name .git -o -name bin -o -name obj \
      -o -name target -o -name dist -o -name build -o -name vendor \
      -o -name .venv -o -name venv )

has_file() {
  local pattern="$1"
  # -mindepth 1 keeps the prune test from ever matching TARGET_DIR itself —
  # without it, a project root named e.g. "build" or "target" would prune
  # at depth 0 and find nothing anywhere.
  [[ -n "$(find "$TARGET_DIR" -mindepth 1 \( "${PRUNE_DIRS[@]}" \) -prune -o -iname "$pattern" -print -quit 2>/dev/null)" ]]
}

# Like has_file, but also greps each matched file's content for a pattern,
# stopping at the first hit. Used for package.json dependency checks so a
# monorepo's nested package.json (e.g. apps/web/package.json) is detected
# the same way a deeply-nested .csproj already is, instead of only ever
# checking the project root.
content_matches() {
  local name_pattern="$1" grep_pattern="$2" f
  while IFS= read -r -d '' f; do
    grep -q "$grep_pattern" "$f" 2>/dev/null && return 0
  done < <(find "$TARGET_DIR" -mindepth 1 \( "${PRUNE_DIRS[@]}" \) -prune -o -iname "$name_pattern" -print0 2>/dev/null)
  return 1
}

# Like content_matches, but spans several source extensions at once (Canvas
# API usage has no single manifest file to key off of the way a package.json
# dependency or a .csproj does).
has_canvas_usage() {
  local f
  while IFS= read -r -d '' f; do
    grep -qE "getContext\([\"'](2d|webgl2?|bitmaprenderer)[\"']\)|<canvas" "$f" 2>/dev/null && return 0
  done < <(find "$TARGET_DIR" -mindepth 1 \( "${PRUNE_DIRS[@]}" \) -prune -o \
        \( -iname "*.js" -o -iname "*.jsx" -o -iname "*.ts" -o -iname "*.tsx" -o -iname "*.html" \) \
        -print0 2>/dev/null)
  return 1
}

if has_file "*.csproj" || has_file "*.sln"; then
  DETECTED+=("dotnet")
fi
if content_matches "package.json" '"next"[[:space:]]*:'; then
  DETECTED+=("next" "react")
elif content_matches "package.json" '"react"[[:space:]]*:'; then
  DETECTED+=("react")
fi
has_file "Cargo.toml" && DETECTED+=("rust")
has_file "project.godot" && DETECTED+=("godot")
has_file "tsconfig.json" && DETECTED+=("typescript")
has_canvas_usage && DETECTED+=("canvas")

if [[ ${#DETECTED[@]} -eq 0 ]]; then
  echo "  none detected"
else
  for fw in "${DETECTED[@]}"; do
    echo "  detected: $fw"
    copy_tree "$TEMPLATE_DIR/.frameworks/$fw" "$TARGET_DIR/.frameworks/$fw"
  done
fi

# --- Manifest-driven deletion ---
# Anything this script installed on a previous run (per the manifest) but
# didn't reproduce this run — a file the template dropped, or a framework
# rule set for a framework no longer detected — gets removed. Files never
# recorded in the manifest (project-added agents/skills, etc.) are untouched.
MANIFEST_FILE="$TARGET_DIR/.ai/.template-manifest"
REMOVED=()

if [[ -f "$MANIFEST_FILE" ]]; then
  while IFS= read -r line; do
    [[ -z "$line" || "$line" == \#* ]] && continue
    if [[ -z "${INSTALLED_SET[$line]:-}" ]]; then
      REMOVED+=("$line")
      rm -f "$TARGET_DIR/$line"
    fi
  done < "$MANIFEST_FILE"
fi

{
  echo "# Generated by .ai/setup.sh — tracks files this script installed."
  echo "# Do not edit by hand; used to detect and remove stale files on re-sync."
  for path in "${!INSTALLED_SET[@]}"; do echo "$path"; done | sort
} > "$MANIFEST_FILE"

# Clean up any directories (e.g. a dropped skill's own folder) left empty
# by the removals above.
find "$TARGET_DIR/.claude/agents" "$TARGET_DIR/.claude/skills" "$TARGET_DIR/.frameworks" \
  -mindepth 1 -type d -empty -delete 2>/dev/null || true

# --- CLAUDE.md managed block ---
CLAUDE_MD="$TARGET_DIR/CLAUDE.md"
BEGIN_MARK="<!-- ai-template:begin -->"
END_MARK="<!-- ai-template:end -->"

agents_list() {
  local out=() p
  for p in "${!INSTALLED_SET[@]}"; do
    case "$p" in
      .claude/agents/*.md) out+=("$(basename "${p%.md}")") ;;
    esac
  done
  printf '%s\n' "${out[@]}" | sort -u | paste -sd, -
}

skills_list() {
  local out=() p rel
  for p in "${!INSTALLED_SET[@]}"; do
    case "$p" in
      .claude/skills/*/*)
        rel="${p#.claude/skills/}"
        out+=("${rel%%/*}")
        ;;
    esac
  done
  printf '%s\n' "${out[@]}" | sort -u | paste -sd, -
}

write_block() {
  echo "$BEGIN_MARK"
  echo "<!-- Managed by .ai/setup.sh from the ai-guidance template — edit outside these markers, not within. Re-running setup.sh replaces this block. -->"
  echo "This project follows [\`.ai/constitution.md\`](.ai/constitution.md) for general development and security guidance."
  if [[ -f "$TARGET_DIR/.ai/project-rules.md" ]]; then
    echo "This project also has its own [\`.ai/project-rules.md\`](.ai/project-rules.md)."
  fi
  echo
  echo "**Agents** (\`.claude/agents/\`): $(agents_list)"
  echo
  echo "**Skills** (\`.claude/skills/\`): $(skills_list)"
  if [[ ${#DETECTED[@]} -gt 0 ]]; then
    echo
    echo "**Framework guidance:**"
    for fw in "${DETECTED[@]}"; do
      echo "- [\`.frameworks/$fw/rules.md\`](.frameworks/$fw/rules.md)"
    done
  fi
  echo "$END_MARK"
}

if [[ -f "$CLAUDE_MD" ]] && grep -q "$BEGIN_MARK" "$CLAUDE_MD"; then
  awk -v b="$BEGIN_MARK" -v e="$END_MARK" '
    $0==b{skip=1; next}
    $0==e{skip=0; next}
    !skip{print}
  ' "$CLAUDE_MD" > "$CLAUDE_MD.tmp"
  mv "$CLAUDE_MD.tmp" "$CLAUDE_MD"
elif [[ ! -f "$CLAUDE_MD" ]]; then
  printf '# CLAUDE.md\n\n' > "$CLAUDE_MD"
fi
write_block >> "$CLAUDE_MD"

echo
echo "${#CHANGED[@]} file(s) added or overwritten:"
for f in "${CHANGED[@]}"; do echo "  $f"; done
if [[ ${#REMOVED[@]} -gt 0 ]]; then
  echo
  echo "${#REMOVED[@]} file(s) removed (no longer provided by the template for this project):"
  for f in "${REMOVED[@]}"; do echo "  $f"; done
fi
echo
echo "CLAUDE.md updated."
if [[ "$ALLOW_DIRTY" == false ]]; then
  echo
  echo "Run 'git status'/'git diff' to see exactly what changed, was added, or was"
  echo "removed. If this was a re-sync (not a first-time install), that diff may have"
  echo "overwritten locally-edited mutable rules or hand-edited agents/skills/settings"
  echo "— reconcile it yourself, or ask your AI assistant to run the sync-guidance"
  echo "skill. Your own .ai/project-rules.md (PROJ-* rules), if any, is never"
  echo "touched by this script."
fi
