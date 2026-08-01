# EmpiresMC Licensing — Part 2: The Single-Player Progression Angle

*Companion to the main analysis. Fold into it or keep separate.*

## Why this is a different comparator set

The claim-mod comparators in Part 1 are mostly **anti-griefing infrastructure for multiplayer servers** — Towny, GriefPrevention, Lands, HuskTowns exist to stop player A from breaking player B's build. Their licensing is shaped by *server-operator monetization* (paid binaries, proprietary plugins) and by *API-linking* (other plugins hook their claim API). Neither pressure applies to EmpiresMC, which is a single-player mod where the "claim" is a progression gate, not a protection mechanism.

The right comparator set is **single-player progression / world-gating mods**, and it licenses differently: more permissive and weak-copyleft, more fork-tolerant, and far more attentive to modpack packability and code-vs-asset splits.

## The single-player progression field

| Mod | Concept | License | Verified via |
|---|---|---|---|
| **Chunk By Chunk** (immortius) | Start with one chunk, expand the world outward — **the closest existing analog to EmpiresMC** | **MIT** | Repo LICENSE; fork's attribution |
| **Gathering Chunks** (ryvione) | Maintained fork of the above | **MIT** (retained) | Modrinth/CurseForge/GitHub |
| **MCSkyblock** | Skyblock world-type gating | **MIT** | Modrinth project page |
| **Skyblocker** | Skyblock progression/QoL (3.9M downloads) | **LGPL-3.0-only** | Modrinth license field |
| **TerraFirmaCraft** | Total survival/tech-tree overhaul | **EUPL-1.2** (code), **CC-BY-SA-4.0** (art), **CC0** (sounds) | Repo README/LICENSE.txt |
| **GregTech 6** | Deep tech progression | **LGPL** (code), **CC0** (assets), **CC BY-NC-4.0** (logo) | Repo README |
| **GregTech CEu Modern** | Deep tech progression | **LGPL-3.0** | Downstream compliance notices |
| **Cobblemon** | Catch/train progression (26.5M downloads) | **MPL-2.0** | Modrinth + CurseForge |
| **FTB Quests** | Quest-gated progression (212M+ downloads) | **ARR / visible-source** (FTB house policy) | FTB repo policy |
| **Hardcore Questing Mode** | Lives + quest gating | *Not confirmed this pass* | — |

**The headline finding: <cite index="4-1">Chunk By Chunk is "a Minecraft mod in which you unlock the world chunk by chunk"</cite> — mechanically the nearest neighbor EmpiresMC has — and it is MIT, not copyleft.** Its last release was February 2024. <cite index="3-1">A maintainer picked it up: "This is a maintained fork of the original Chunk By Chunk by immortius... Since the original repository appears to be discontinued, I've decided to continue its development, fixing bugs, adding features, and ensuring compatibility with newer Minecraft versions."</cite> <cite index="1-1">The fork retained the license and the attribution: "This mod is licensed under the MIT License. Original work Copyright (c) immortius. Modified work Copyright (c) 2026 Ryvione."</cite>

That is the entire abandonware-continuation lifecycle, executed cleanly, in EmpiresMC's exact sub-genre. It is the strongest single data point in this analysis.

## Four patterns that don't show up in the claim-mod niche

**1. Progression mods get forked, and the license decides whether that's legal or awkward.** TerraFirmaCraft's EUPL-1.2 has spawned a visible fork tree (alcatrazEscapee, Rongmario, Verph, peeperh, MayTheCutie) — all carrying the license forward. Chunk By Chunk's MIT enabled Gathering Chunks. Contrast the counterexample: TerraFabriCraft's team explains they built a spiritual successor rather than a port because <cite index="31-1">"during our initial decision to whether to port TerraFirmaCraft to newer versions or make a spiritual successor we went with the later option due to licensing issues that weren't resolved until after much of our development had already begun."</cite> **Licensing friction in this niche doesn't stop people — it makes them rewrite your mod from scratch, and you get nothing.**

**2. Copyleft in this niche actually gets complied with.** A modpack shipping a patched GregTech build states: <cite index="22-1">"The pack ships a patched build of GregTech CEu Modern (LGPL-3.0). In accordance with LGPL-3.0, the modified source is published here."</cite> This is meaningful evidence against the "copyleft is unenforceable in Minecraft, so why bother" argument — modpack authors in the progression space do read license fields and do publish patched sources.

**3. Split code/asset licensing is standard practice here, and Part 1 missed it.** TerraFirmaCraft: <cite index="32-1">"This project is under the European Union Public Licence v1.2... Textures and other art assets are made available under Creative Commons Attribution Share Alike 4.0 International (CC-BY-SA-4.0)"</cite>. GregTech 6: <cite index="29-1">"This Mod is licensed under the GNU Lesser General Public License. All assets, unless otherwise stated, are dedicated to the public domain according to the CC0 1.0 Universal Public Domain Dedication. Any assets containing the GregTech logo or any derivative of it are licensed under the Creative Commons Attribution-NonCommercial 4.0 International Public License."</cite> **EmpiresMC's Phase 9 ships real Scepter art, a sound set, and advancement text. One LGPL line in `fabric.mod.json` currently covers all of it, which is imprecise.**

**4. Extension happens through data, not code — which undercuts the LGPL rationale.** Open Parties and Claims chose LGPL specifically so other *mods* could link its claim API. EmpiresMC's own backlog points the other way: datapack-driven tier definitions, config-driven upgrades, map-mod overlays. Cobblemon's addon ecosystem (SimpleTMs and others) sits happily under MPL-2.0 without needing LGPL's linking machinery. If EmpiresMC's extension story is datapacks and config, **LGPL's central benefit is largely wasted on it.**

## What this changes

Part 1 leaned on Open Parties and Claims as precedent for LGPL. On the progression side that precedent weakens: OPaC is a multiplayer claim mod whose LGPL rationale was API linking, and EmpiresMC has neither the multiplayer framing nor the linking need. Meanwhile, **MPL-2.0** — barely mentioned in Part 1 — turns out to be the best-evidenced fit:

- It's **file-level copyleft**: your files stay open through forks, but there's no "linking" question to reason about, which sidesteps the mixin/remapped-jar incoherence that is LGPL's real weakness here. <cite index="57-1">MPL-2.0 is a weak copyleft license, FSF- and OSI-approved, GPL-compatible by default, and explicitly permits linking from code under a different license.</cite>
- It's **proven at scale on a gameplay mod** in this exact space: <cite index="52-1">Cobblemon's binaries and source are under MPL-2.0, "allowing public modifications and forks provided that they are also licensed under MPL2.0," with source open "to encourage not only community contributions, but also forking."</cite>
- It's **modpack- and addon-friendly** with no ambiguity.

**Revised ranking for EmpiresMC:**

1. **MPL-2.0** — best fit. Keeps the copyleft intent you signalled by choosing LGPL, drops the linking-semantics problem, and matches the mod type rather than the library type. Note MPL-2.0 is selectable on Modrinth (Forge Config API Port and others use it).
2. **Keep LGPL-3.0-or-later** — fine, defensible, but the *library* license on a *gameplay* mod. If you keep it, say why in the README.
3. **MIT** — what your nearest mechanical analog actually chose, and it demonstrably worked: the mod died, someone continued it, attribution survived. Pick this if continuation matters more to you than keeping forks open.
4. **GPL-3.0 + linking exception** — only if you specifically want to block closed forks; heavier than this mod needs.

## Three concrete actions regardless of license

1. **Split code and assets.** Put the Scepter texture/model, sounds, and lang/advancement text under an explicit asset license (CC-BY-SA-4.0 if you want share-alike, CC0 if you want resource-pack authors to reuse freely) and say so in the README. Modrinth allows one license field — declare the code license there and document the split in the README, as TFC and GregTech do.
2. **Add a contributor relicensing clause now.** TerraFirmaCraft's CONTRIBUTING asks contributors to grant the right to change the license in the future. Without that, switching from LGPL later requires chasing down every contributor. Add it to the Phase 11 `CONTRIBUTING.md` — it costs one sentence and preserves every option above.
3. **Write the continuation invitation into the README.** The Chunk By Chunk → Gathering Chunks handoff worked because the license was clear and the attribution norm was obvious. A line like "if this goes unmaintained for a Minecraft version, fork it — keep the license and the credit" converts a legal permission into a social one, which is what actually drives continuation in this community.

## Caveats
- Hardcore Questing Mode's license was not confirmed in this pass — check `lorddusk/HQM` directly before citing it.
- Applied Energistics 2 remains unverified from Part 1.
- FTB's ARR position is a house policy across all FTB mods, not a per-mod judgment; don't read it as a considered choice specific to quest/progression mods.
- Download counts and license fields are as displayed at time of checking (Aug 2026) and change.

### Additional sources
- github.com/immortius/chunkbychunk · github.com/ryvione/Gathering-Chunks · modrinth.com/project/UgEJpudA
- github.com/TerraFirmaCraft/TerraFirmaCraft (README, LICENSE.txt, CONTRIBUTING)
- github.com/Voleil/TerraFabriCraft (spiritual-successor rationale)
- github.com/GregTech6/gregtech6 (code/asset/logo license split)
- modpackindex.com/modpack/155522/gregnautics-continued (LGPL-3.0 source-publication compliance)
- modrinth.com/mod/cobblemon · curseforge.com/minecraft/mc-mods/cobblemon · modrinth.com/mod/simpletms-tms-and-trs-for-cobblemon
- modrinth.com/mod/skyblocker-liap · modrinth.com/project/4AVPhH7R (MCSkyblock)
- mozilla.org/en-US/MPL/2.0/FAQ · en.wikipedia.org/wiki/Mozilla_Public_License
- interoperable-europe.ec.europa.eu/collection/eupl (EUPL-1.2 text)