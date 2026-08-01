# Licensing Analysis: EmpiresMC (LGPL-3.0-or-later) vs. the Minecraft Modding Ecosystem

## TL;DR
- **LGPL-3.0-or-later is a defensible, above-average choice for EmpiresMC and is the most common copyleft license in the Minecraft claim-mod niche** — the closest comparator, Open Parties and Claims (LGPL-3.0-only, 59,081,929 CurseForge downloads as of July 2026 per its CurseForge project page, Project ID 636608), chose it for the exact same reasons (keep the mod itself free, let other mods link its API). But LGPL's "linking" semantics map poorly onto Minecraft's mixin/remapped-jar reality, and for a single-player *content/gameplay* mod (not a library) the cleaner options are **GPL-3.0-with-a-linking-exception** (maximum copyleft protection against closed forks) or **MIT/Apache-2.0** (maximum reach and packability).
- **The niche is a genuine grab-bag**: ARR/source-visible (FTB Chunks, Flan), permissive MIT (Cadmus, Argonauts; HuskTowns is Apache-2.0), weak copyleft LGPL (Open Parties and Claims), strong copyleft GPL (GriefPrevention), non-OSS content licenses (Towny = CC BY-NC-ND), and fully proprietary (Lands). There is no single "standard."
- **The one thing to fix regardless of license: distribution mechanics.** LGPL/GPL are fully "packable" on Modrinth and CurseForge, but modpack redistribution rights, the CurseForge third-party-download toggle, and Minecraft's EULA (no selling mods; mappings-license constraints) matter more in practice to a mod's spread than the copyleft/permissive distinction.

## Key Findings
1. **EmpiresMC's current license (LGPL-3.0-or-later) has a direct, successful precedent in its own niche.** Open Parties and Claims (by xaero96) is LGPL-3.0-only and its author states the choice was deliberate: it lets other mods use the API "through Java/JVM mechanics," and "is very similar to what Minecraft Forge is currently released under."
2. **The niche spans the entire license spectrum**, so EmpiresMC's choice will not look anomalous whatever it picks.
3. **Big-name reference mods skew permissive or weak-copyleft:** Fabric API (Apache-2.0), fabric-language-kotlin (Apache-2.0), Create (MIT), EMI (MIT), Sodium/Lithium (formerly LGPL-3.0, now PolyForm Shield). This matters because EmpiresMC depends on Apache-2.0 Fabric API, and Apache-2.0 → (L)GPL-3.0 is one-way compatible, so the dependency direction is legally clean.
4. **The community gravitates to MIT first, LGPL-3.0 second, CC0 third.** Per Modrinth's own "Beginner's Guide to Licensing your Mods": "The most popular license on Modrinth, the MIT License"; "The second most common license on Modrinth is a copyleft license: the GNU Lesser General Public License Version 3"; "The third most common license used on Modrinth is the Creative Commons Zero." GPL/AGPL are widely avoided because of the Minecraft-linking problem.
5. **Distribution policy, not license family, is the real friction** — CurseForge's ARR default and third-party-download toggle, FTB's no-external-modpack policy, and Minecraft's EULA/mappings license.

## Details

### 1. Comparable mods — chunk claims / land protection
Licenses verified from each project's GitHub LICENSE file, Modrinth "Licensed" field, or CurseForge license field:

| Mod / plugin | License | Source public? | Where verified |
|---|---|---|---|
| **Open Parties and Claims** (thexaero/xaero96) | **LGPL-3.0-only** | Yes | Modrinth ("Licensed LGPL-3.0-only"), GitHub LICENSE |
| **FTB Chunks / FTB Teams** (Feed The Beast Ltd) | **All Rights Reserved** ("visible source") | Visible-source only | GitHub README/LICENSE.md ("All Rights Reserved to Feed The Beast Ltd. Source code is visible source") |
| **Flan** (Flemmli97) | **ARR** | Yes (source visible) | Modrinth ("Licensed ARR") |
| **Cadmus / Odyssey Claims** (Terrarium Earth) | **MIT** | Yes | GitHub, Modrinth, CurseForge |
| **Argonauts / Odyssey Allies** (Terrarium Earth) | **MIT** | Yes | GitHub, CurseForge |
| **GriefPrevention** (Bukkit/Spigot, TechFortress→GriefPrevention org) | **GPL-3.0-or-later** | Yes | GitHub source headers, Maven |
| **GriefDefender** (bloodmc) | **MIT** source; paid compiled builds | Yes | GitHub per-file MIT headers, GriefDefenderAPI LICENSE |
| **Towny / TownyAdvanced** | **CC BY-NC-ND 3.0** (FlagWar = Apache-2.0) | Yes | GitHub LICENSE.md |
| **HuskTowns** (William278) | **Apache-2.0**; paid binaries | Yes | GitHub README, William278.net |
| **Lands** (Angeschossen) | **Proprietary / paid, closed-source** (only API public) | No | Polymart product page ("You may not redistribute this plugin"), LandsAPI repo |

Note the "source-visible but not open-source" pattern (FTB Chunks ARR, Flan ARR) and the "open-source code, commercial binaries" pattern (GriefDefender MIT, HuskTowns Apache-2.0). Towny is the cautionary example: **CC BY-NC-ND is not an open-source license** (no derivatives, no commercial use), yet it's one of the oldest claim plugins.

### 2. Reference / progression / large ecosystem mods
- **Fabric API: Apache-2.0**; **fabric-language-kotlin: Apache-2.0** — both direct EmpiresMC dependencies.
- **Create: MIT** (Copyright simibubi). **EMI: MIT.** **JEI/REI**: permissive/open (JEI historically MIT-family).
- **Sodium / Lithium: formerly LGPL-3.0-only, now relicensed to PolyForm Shield 1.0.0** (a source-available, *non-OSI* license). CaffeineMC's own repo now carries PolyForm Shield; the relicensing proposal (CaffeineMC/sodium Issue #2400, filed when Sodium had "nearly 41 million downloads") stated the change "would only prevent third-party forks from continuing to cut us out of our own project, which includes ports to other mod loaders." This is a live example of copyleft-to-source-available drift driven by rogue forks.
- **Botania: a custom "Botania License"** — source-available and modpack-friendly but with a non-commercial distribution restriction and a share-alike-style requirement; not an OSI license.
- **Applied Energistics 2: historically LGPL** (per community license inventories; verify against its live LICENSE file before relying).
- **Forge**: LGPL-2.1 historically; **Mojang official mappings** are the binding constraint for everyone (below).

### 3. Community gravitation & platform policy
- **Modrinth uses SPDX identifiers** for the license field (e.g., `LGPL-3.0-or-later`), and its own guide ranks the most-used licenses as **MIT (permissive) > LGPL-3.0 (copyleft) > CC0 (public domain)**. Modrinth explicitly warns that GPL-3.0/AGPL-3.0 "are incompatible if linking into Minecraft, due to an issue with the difference between proprietary and free software... An exception can be added to allow linking... but it is recommended to just use the LGPL-3.0 instead if possible." (AGPL-3.0 — Modrinth's own license — is not even selectable on Modrinth.)
- **CurseForge** defaults new projects to **All Rights Reserved.** Its **May 16, 2022 official API** introduced an author toggle for "allow third-party distribution"; when off, third-party launchers/managers (MultiMC, etc.) cannot auto-download the file. This is the well-known redistribution controversy — it broke tooling and pushed many authors toward Modrinth.
- **FTB's policy** forbids embedding FTB mods in modpacks on platforms other than FTB's own and CurseForge — a redistribution restriction layered on top of the ARR license.
- **Minecraft EULA**: any mod you make is yours, "as long as you don't sell them for money"; ARR mods are still bound by this — you cannot sell an EmpiresMC even if you reserve all rights.
- **Mappings are the deep constraint.** Mojang's official mappings (MojMap) carry a license: "you may copy and use the mappings for development purposes, but you may not redistribute the mappings complete and unmodified" — which Forge's cpw called "legal poison." Fabric's **Yarn** mappings are more permissive (community, open); **Parchment** is open; **Intermediary** provides the stable runtime layer. EmpiresMC on Fabric/Yarn is on the cleanest mappings footing.

### 4. Broader open-source license families (general software)
- **Public domain / CC0 / Unlicense**: maximal freedom, no attribution; CC0 is GPL-compatible and CC-endorsed for software (the only CC tool CC endorses for code).
- **Permissive (MIT, BSD-2/3, Apache-2.0, ISC, zlib)**: use/modify/relicense/close freely with attribution. **Apache-2.0 adds an explicit patent grant and a modified-file-notice requirement; MIT has neither.**
- **Weak copyleft (LGPL-2.1/3.0, MPL-2.0, EPL-2.0, CDDL)**: modifications to the covered code stay open; the covered work can be combined with proprietary code. MPL-2.0 is *file-level* copyleft and is GPL-compatible by default.
- **Strong copyleft (GPL-2.0/3.0, AGPL-3.0)**: entire derivative must ship under the same license; AGPL extends this to network use.
- **Source-available / non-OSI (BSL, SSPL, Elastic, PolyForm, Fair Source, Commons Clause)**: source visible but restricted (often no commercial or no-compete use) — **not open source.**
- **Content licenses (Creative Commons)**: CC officially **recommends against CC licenses for software** — "Unlike software-specific licenses, CC licenses do not contain specific terms about the distribution of source code... our licenses are currently not compatible with the major software licenses." **CC BY-NC and CC BY-ND fail the OSD** (no commercial use / no derivatives).
- **License-popularity data**: The macro trend is a durable shift toward permissive. RedMonk/Black Duck data showed GPLv2 falling from ~46% to ~19% of surveyed repos (2010–2017) while MIT rose ~8%→29% and Apache ~5%→15%. Per RedMonk's Stephen O'Grady, "The State of Open Source Licensing in 2026" (Mar 25, 2026), Apache use peaked around 30% in 2022; Mend.io data placed the GPL family's combined copyleft share (including LGPL) at ~22% in 2022 with MIT dominating permissive usage. RedMonk's 2026 read has permissive licenses at roughly three-quarters of GitHub components.

### 5. Advantages / disadvantages for a mod like EmpiresMC
- **MIT / Apache-2.0** — *Pros:* maximum reach, trivially packable, any addon/fork/continuation is frictionless (critical for the abandonware-continuation pattern), Apache adds patent protection. *Cons:* permits proprietary re-capture — a server host or another author can fork EmpiresMC, close it, and monetize the play experience with no obligation to share improvements.
- **LGPL-3.0-or-later (current)** — *Pros:* keeps EmpiresMC's own code open through forks; explicitly allows other mods to depend on/extend it; fully packable; a proven fit for a claim mod (Open Parties and Claims). *Cons:* LGPL's "linking"/relinking machinery was written for C shared libraries and maps incoherently onto a shaded/mixin/remapped Minecraft jar where a "mod" isn't linking in the classic sense; the relinking obligation is largely unenforceable and arguably meaningless in the JVM/mod-loader context. For a *gameplay mod* rather than a *library*, LGPL's core benefit (protecting a linkable library while allowing proprietary consumers) is a weak match.
- **GPL-3.0 (+ linking exception)** — *Pros:* strongest guarantee that forks/continuations stay open — well-aligned with modding's remix culture. *Cons:* bare GPL-3.0 has the Minecraft-linking problem; needs a linking exception; can deter some addon authors.
- **CC0 / public domain** — *Pros:* zero friction. *Cons:* no attribution guarantee, no patent clarity; rare for a full mod.
- **ARR / source-available** — *Pros:* control (FTB/Flan model). *Cons:* not open source, blocks contribution/forks, and (per EULA) still can't be sold.

**Assessment:** LGPL-3.0-or-later is *fine* and *credible*, but it is arguably the wrong tool for the job in a subtle way — it's a *library* license applied to a *gameplay* mod. If EmpiresMC's goal is "the mod and its forks must always stay open, including abandonware continuations" (the copyleft spirit), **GPL-3.0-or-later with an explicit Classpath-style/Minecraft linking exception** expresses that intent more honestly than LGPL. If the goal is "maximum adoption, easy addons, easy continuation forks," **MIT or Apache-2.0** is the better fit, with Apache-2.0 preferred for its patent grant given the Apache-2.0 Fabric API dependency. LGPL is the reasonable middle, and keeping it is not a mistake — but it should be a *deliberate* middle, not a default.

## Open-Source Principles (OSD / FSF Four Freedoms)
Measured against the **Open Source Definition** (free redistribution, source access, derived works, no field-of-use/no-discrimination clauses) and the **FSF Four Freedoms** (use, study, modify, share):
- **Permissive (MIT/Apache/BSD)**: fully OSD/FSF-free; maximize *downstream* freedom (freedoms 0–3 for everyone) but permit **proprietary re-capture** — a downstream actor can strip freedoms from *their* users. Maximal liberty, minimal guarantee.
- **Copyleft (GPL/LGPL/AGPL, MPL)**: fully OSD/FSF-free; **preserve user freedom transitively** at the cost of restricting a downstream author's licensing choices. This is the philosophical free-software-vs-open-source split: FSF prioritizes protecting the *user's* freedom, the permissive camp prioritizes the *developer's* freedom to do anything.
- **FSF's own guidance is directly relevant**: in "Why you shouldn't use the Lesser GPL for your next library," the FSF states that "Using the ordinary GPL for a library gives free software developers an advantage over proprietary developers: a library that they can use, while proprietary developers cannot use it." By FSF's own logic, if EmpiresMC wants to advance software freedom, **LGPL is the weaker copyleft choice**, and GPL is preferred; LGPL is justified mainly when proprietary alternatives already exist and the library needs adoption to matter.
- **Source-available / BSL / "no commercial use" (Botania's NC clause, Towny's CC BY-NC-ND, Lands proprietary, FTB/Flan ARR): NOT open source.** They fail the OSD's no-discrimination and free-redistribution criteria. **ARR-with-public-source (FTB Chunks) is source-visible, not open.** These are legitimate business/anti-abuse choices but should not be described as open source.
- **Modding-culture tension**: this community's core norms are **forking abandoned mods** and **remixing** to keep them alive across Minecraft versions. Permissive and copyleft licenses *both* enable this; ARR, NC, and ND licenses actively *break* it — an ARR mod that stops updating is legally frozen, which is why continuation forks of ARR mods live in a gray zone. **For a mod that wants to be continued after EmpiresMC's author moves on, any OSI-approved license (permissive or copyleft) serves the community; LGPL-3.0 already satisfies this.**

## Recommendations
1. **Keep LGPL-3.0-or-later only if the intent is explicitly "protect this mod's code from being closed, while letting other mods build on it."** It is a legitimate, precedented choice (Open Parties and Claims). If you keep it, **add a short written rationale** in the README and, ideally, an explicit linking/relinking exception clarifying how LGPL applies in a mixin/remapped-jar context — because the default LGPL text does not map cleanly onto Minecraft mods.
2. **If your real priority is copyleft protection of forks/continuations**, switch to **GPL-3.0-or-later + a linking exception** (the Modrinth-recommended pattern). Benchmark to change: you observe or expect closed-source forks or paid re-skins of the gameplay.
3. **If your real priority is adoption, easy addons, and painless continuation forks**, switch to **Apache-2.0** (preferred over MIT for the patent grant and because your Fabric API dependency is already Apache-2.0). Benchmark to change: you want other mods to depend on EmpiresMC's claim API, or you want to lower the barrier for someone to port it to future Minecraft versions.
4. **Regardless of license, get the distribution mechanics right** (these matter more than the license family): ship the LICENSE in the jar (already done); attach a sources jar (already planned — required under LGPL/GPL); on CurseForge **enable third-party distribution** so modpacks and launchers can include EmpiresMC; on Modrinth set the exact SPDX field (`LGPL-3.0-or-later`); and **do not attempt to sell the mod** (Minecraft EULA forbids it regardless of license).
5. **Do not adopt a non-OSI license** (ARR, BSL, PolyForm, any CC NC/ND) unless you have a specific monetization/anti-abuse reason — they break the community's forking/remix norms and disqualify the project from being "open source."

## Caveats
- Licenses change; verify each comparator's current SPDX field on its live Modrinth/CurseForge/GitHub page before relying on it. Several projects (Sodium — already relicensed to PolyForm Shield; GriefDefender; HuskTowns) have adopted or blended commercial/source-available terms.
- The legal question of whether a Minecraft mod "links" to Minecraft or to another mod in the GPL/LGPL sense **has not been tested in court**; all copyleft-linking analysis here (and in the community) is best-effort interpretation, not settled law.
- Applied Energistics 2's current license was inferred from community inventories, not confirmed from its live LICENSE file in this pass; treat as "historically LGPL, verify before relying."
- Note the two distinct "GriefPrevention" codebases: the mainstream Bukkit/Spigot plugin (TechFortress → GriefPrevention org) is **GPL-3.0-or-later**; the unrelated, superseded Sponge port (MinecraftPortCentral) was MIT. Do not conflate them.
- "Enforceability" of any of these licenses against a hobbyist forker is limited in practice; the practical value of license choice is signalling intent and enabling/blocking legitimate reuse, more than litigation.

---

### Sources
- Open Parties and Claims: modrinth.com/mod/open-parties-and-claims; github.com/thexaero/open-parties-and-claims
- FTB Chunks: github.com/FTBTeam/FTB-Chunks (README/LICENSE.md)
- Flan: modrinth.com/mod/flan
- Cadmus / Argonauts: github.com/terrarium-earth/Cadmus; curseforge.com/minecraft/mc-mods/odyssey-claims; curseforge.com/minecraft/mc-mods/odyssey-allies
- GriefPrevention: github.com/TechFortress/GriefPrevention; mvnrepository.com/artifact/com.github.TechFortress/GriefPrevention
- GriefDefender: github.com/bloodmc/GriefDefender; github.com/bloodmc/GriefDefenderAPI
- Towny: github.com/TownyAdvanced/Towny/blob/master/LICENSE.md
- HuskTowns: github.com/WiIIiam278/HuskTowns; william278.net/project/husktowns
- Lands: polymart.org/product/876; github.com/Angeschossen/LandsAPI
- Create: github.com/Creators-of-Create/Create/blob/mc1.18/dev/LICENSE
- EMI: curseforge.com/minecraft/mc-mods/emi
- Fabric API: modrinth.com/mod/fabric-api
- Sodium (LGPL→PolyForm): github.com/CaffeineMC/sodium/issues/2400
- Botania License: botaniamod.net/license.html
- Modrinth licensing guide: blog.modrinth.com/p/licensing-guide (and Modrinth SPDX license field: docs.modrinth.com/openapi.yaml)
- CurseForge API/redistribution: medium.com/overwolf/the-curseforge-official-api-is-now-live-d314606355c2
- Mojang mappings license / MojMap: minecraft.fandom.com/wiki/Obfuscation_map; cpw.github.io/MinecraftMappingData.html; wiki.fabricmc.net/tutorial:mappings; parchmentmc.org
- FSF "Why you shouldn't use the Lesser GPL": gnu.org/licenses/why-not-lgpl.html
- Creative Commons on software: creativecommons.org/faq; creativecommons.org/2011/04/15/using-cc0-for-public-domain-software
- License-popularity data: redmonk.com/sogrady/2017/01/13/the-state-of-open-source-licensing; redmonk.com/sogrady/2026/03/25/open-source-licensing-2026; mend.io/blog/open-source-licenses-trends-and-predictions
- MPL-2.0 reference: mozilla.org/en-US/MPL/2.0/FAQ; en.wikipedia.org/wiki/Mozilla_Public_License
- LGPL linking semantics: en.wikipedia.org/wiki/GNU_Lesser_General_Public_License