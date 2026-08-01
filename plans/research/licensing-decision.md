# Licensing Decision: MPL-2.0

**Decided 2026-08-01.** Supersedes the 2026-07-31 decision to relicense to MIT, which was made
during Phase 6 on a factually incorrect premise (below). Supersedes Phase 1's LGPL-3.0-or-later.

**Inputs:** [`project_licensing_part1.md`](project_licensing_part1.md),
[`project-licensing-part2.md`](project-licensing-part2.md),
[`market_research.md`](market_research.md).

## Decision

**Code: MPL-2.0.** **Assets (textures, models, sounds): CC BY-SA 4.0.**

## Why the MIT decision was reopened

Two premises supported it. Both fail.

**1. Factual error in the evidence base.** [`market_research.md`](market_research.md) claimed
"MIT for Chunk By Chunk/OPAC source," and Phase 1's amendment repeated it. Verified 2026-08-01:

| Claim | Reality |
|---|---|
| Chunk By Chunk = MIT | Correct — repo LICENSE, "Chunk By Chunk code is released under The MIT License" |
| Open Parties and Claims = MIT | **Wrong** — Modrinth reads "Licensed LGPL-3.0-only" |

OPaC is the largest mod in the adjacent niche (~59M downloads) and it is copyleft. Half the
evidence for "permissive is the norm" was inverted. Adding Skyblocker (LGPL-3.0-only, 3.9M),
GregTech CEu (LGPL-3.0), and Cobblemon (MPL-2.0, ~26.5M), the field is a genuine split.

**2. Mechanical error.** The stated lever was modpack adoption. Modpacks redistribute
**unmodified jars**, which every OSI license permits identically — LGPL, MPL, and MIT are equally
packable on Modrinth and CurseForge. What actually blocks modpacks is ARR/NC/ND licensing and
CurseForge's third-party-download toggle, none of which apply. There is **no adoption delta**
between the three candidates.

## Why not LGPL-3.0-or-later (the status quo)

LGPL's machinery exists to let *proprietary consumers link a library*. EmpiresMC is not a library.
Its extension story is datapacks, config, and map-mod integration — not other mods calling a claim
API — so LGPL's central benefit is structurally unused here, while its costs (an incoherent §4
relink obligation against a remapped Fabric jar; ambiguity over what is "the Library" versus a
"Combined Work" in a Kotlin mod) are real. It is the only candidate with a concrete defect and no
compensating upside for this project.

## Why not MIT

MIT is a legitimate choice with the best-verified continuation precedent in this exact sub-genre:
Chunk By Chunk → Gathering Chunks, license and attribution carried forward cleanly. It was rejected
only because Phase 1's copyleft goal was deliberate and reasoned, and was overturned on the two
failed premises above — that warrants reinstating the goal, not inheriting its reversal.

MIT remains the correct answer if the project's priority ever inverts to "maximize the odds someone
continues this" over "keep forks open."

## Why MPL-2.0

- **File-level copyleft.** Modified EmpiresMC source files stay open. Anyone may combine them into
  a larger work under any license.
- **No linking question.** §3.2 permits distributing the Executable Form under other terms provided
  covered source stays MPL — this maps onto a remapped, modpack-redistributed Fabric jar far more
  cleanly than LGPL ever will. This is the specific defect it fixes.
- **Proven at scale on a *gameplay* mod**, not a library: Cobblemon, MPL-2.0, ~26.5M downloads, with
  a healthy third-party addon ecosystem and no LGPL-style linking machinery needed.
- **GPL-compatible** (secondary-license provision) and on Modrinth's SPDX list.
- **Already Phase 1's own stated fallback:** "MPL-2.0 is the fallback if weaker file-level copyleft
  is ever preferred." This is not a reversal of Phase 1 — it is the amendment Phase 1 anticipated.

**Known limitation, accepted:** a fork can add new features in *new* files under a proprietary
license; only covered files carry the obligation. In practice, meaningfully changing claim behavior
requires editing existing files, so the valuable core stays covered.

## What copyleft does and does not buy

It prevents forks from **closing the source**. It does **not** prevent being forked and
outcompeted — Sodium was LGPL-3.0 and still relicensed to PolyForm Shield because *compliant* forks
cut the maintainers out. No OSI license addresses that; only non-OSS terms do, which are out of
scope. Enforcement here is signalling and norm-setting, not litigation.

## Execution

- [x] `LICENSE` — canonical MPL-2.0 body (`gh api /licenses/mpl-2.0`, SHA-verified against source).
- [x] MPL Exhibit A header on all 26 `.kt` files. **Required** — MPL §1.4 defines Covered Software
      by the attached notice, so unlike LGPL/MIT a `LICENSE` file alone does not establish coverage.
- [x] `checkLicenseHeaders` Gradle task enforcing the above, wired into `check` — so `./gradlew build`
      and CI both catch a missing header with no workflow change. Enforced rather than documented in
      a project rule because the failure is otherwise **silent**: an unheadered source compiles,
      passes every test, and ships, surfacing only when someone forks. A rule would guide agents but
      bind neither humans nor CI. Verified in both directions — passes clean, and fails naming the
      file with a copy-pasteable notice when a header is absent.
- [x] `fabric.mod.json` `license` field → `MPL-2.0`.
- [x] README license section: code/asset split, plus an explicit continuation invitation.
- [x] `CONTRIBUTING.md` — inbound=outbound terms, no CLA/DCO, **no relicensing grant** (considered
      and declined; reasoning below).
- [ ] Modrinth/CurseForge project license fields at release (Phase 11).

## Contributor relicensing grant — considered and declined, 2026-08-01

[`CONTRIBUTING.md`](../../CONTRIBUTING.md) was written with a relicensing grant (bounded to
OSI-approved licenses) and it was then **removed deliberately**. Contributions are inbound=outbound
under MPL-2.0 and nothing more; contributors keep copyright and are asked for no additional rights.

**Why it was dropped:**

1. **MPL already provides most of the flexibility the grant was buying.** §10.2 permits distributing
   under "any subsequent version published by the license steward," so a future MPL 2.1/3.0 needs no
   consent from anyone. §3.3 separately allows covered files to be distributed under GPL/LGPL/AGPL
   terms as part of a larger work.
2. **The residual scenario is narrow and compound:** it requires wanting MPL → MIT/Apache
   *specifically*, **and** external contributions having landed, **and** those contributors being
   unreachable. Having just chosen copyleft deliberately, a near-term move to permissive is the
   least likely direction this project goes.
3. **The chase-down cost is smaller here than in the general case.** Most contributions to a mod
   this size are a few lines, often below the originality threshold for independent copyright. MPL
   being file-level, plus git blame, means an unreachable contributor's work can be identified and
   rewritten surgically rather than blocking a whole-work relicense.
4. **It carries a real cost.** A standing relicensing grant is a soft CLA. Phase 1 declined a CLA on
   principle; re-introducing a lighter version of it is inconsistent with that, adds legal ceremony
   to a first-time contributor's path, and cuts against the fork-and-continue posture the README
   explicitly invites.

**What replaces it:** nothing, by design. If a relicense is ever wanted, ask the contributors at
that moment. Consent then is specific and informed rather than buried in a document nobody read,
and it costs nothing until the day it is actually needed. Leaving the grant out forecloses nothing;
it moves the ask to the point of use.

**Accepted risk:** if that day comes and a contributor is unreachable, their contribution must be
rewritten or the relicense abandoned. Judged small relative to the costs above.

## Asset license — confirmed 2026-08-01

**CC BY-SA 4.0**, chosen over CC0. Share-alike matches the MPL copyleft intent and follows
TerraFirmaCraft's precedent; the cost accepted is that resource-pack authors cannot freely
relicense the Scepter art, in exchange for a guaranteed attribution and share-alike chain.

**Scope — what the split actually covers:**

| Under CC BY-SA 4.0 | Under MPL-2.0 |
|---|---|
| Textures (`.png`), sounds (`.ogg`) — *art* | Kotlin sources |
| | Functional resource files: `models/`, `items/`, `lang/`, `data/` recipes |

Model definitions, lang strings, and recipe JSON are functional declarations rather than art, so
they stay with the code. **Today the CC BY-SA set is empty** — the Scepter is still the stick
placeholder and no `.png` or `.ogg` ships. The declaration is forward-looking for
[Phase 9](../1.0-release/phase-9-commands-and-polish.md)'s real art and audio identity.

**Phase 9 follow-through:** binary art can't carry an inline notice the way `.kt` files carry
Exhibit A, so when the textures and sounds land, place a short `LICENSE` note in their directory
naming CC BY-SA 4.0 and the author. MPL's own Exhibit A endorses exactly this fallback ("a LICENSE
file in a relevant directory where a recipient would be likely to look"). Without it, someone who
extracts a texture from the jar has no in-jar signal that it is licensed differently from the code.
