# Competitive & Market Research: EmpiresMC (Fabric/Kotlin "land-as-progression" mod)

## TL;DR
- **EmpiresMC occupies a real but narrow niche**: it fuses two proven-popular ideas — chunk-unlock progression (Chunklock, Chunk By Chunk) and claim-item UX (GriefPrevention/FTB Chunks) — but as a *single-player-first, item-priced, tiered* system. No public mod named "EmpiresMC" exists on Modrinth/CurseForge/GitHub (the name currently belongs to a live Towny server at play.empiresmc.com and, loosely, to fWhip's private "Empires SMP" mod), so the concept space is open but the name carries brand-collision risk.
- **The biggest bounce risks are well-documented in competitors**: progression soft-locks (needing a resource/structure you can't reach), the "arbitrary chunk boundary" complaint, protection-bypass exploits from machinery/pistons/fluids, and gesture controls being undiscoverable. Several EmpiresMC "backlog" items (map overlays, a GUI, teams/permissions) are actually *table stakes* players will judge it against.
- **Its genuinely novel idea — "chunk scarring/decay" to punish nomadic strip-mining — has no precedent** in any searched mod/plugin/datapack. The established anti-abuse levers are partial refunds (GriefPrevention's `AbandonReturnRatio`, default 1.0/full), unclaim refund-below-cost (Towny), upkeep auto-unclaim (Lands), and cooldowns. This is a defensible differentiator if executed well.

## Key Findings

### The name "EmpiresMC" is not a mod, but it's not collision-free
No mod literally named "EmpiresMC"/"Empires MC" was found on Modrinth, CurseForge, or GitHub (the GitHub repo appears private/unpublished). The name is currently used by a public Spigot survival server (play.empiresmc.com, MC 1.18.2, running Towny/Factions/Kingdoms) and is one letter from **Empires SMP**, fWhip's very popular private YouTuber SMP whose cosmetic "Empires Mod" the creators have said they will never release. "Empires" is also a crowded search term (Epic Empires armor mod; Supernova Empires/Cookiepack modpacks). Positioning risk is moderate: the mod slot is open, but "Empires" searches collide with a large existing YouTube fanbase expecting something else.

### Direct analogues (chunk-unlock as progression) — the closest competitors
- **Chunklock (datapack, by AvidMC & QuillBee)** — the category leader by mindshare. **47,379 downloads on CurseForge (Project ID 1338033, Apache-2.0), last updated May 13, 2026 to v2.1.0 supporting MC 1.21.11**; also on Modrinth/Planet Minecraft. Every chunk except your start is locked; you unlock by throwing biome-specific items at the border. Sportskeeda confirms it "is made by AvidMC and QuillBee, and is set to be released in October 2023"; its reveal on r/Minecraft got 7,000+ upvotes in two days. Datapack, works any seed, single-player + LAN (its listing recommends "a maximum of 6 players and at least 8GB of RAM" for multiplayer). It explicitly warns it "does NOT play well with Optifine" or Lunar client (border rendering breaks). This is the tone/UX benchmark for "the world is locked."
- **Chunklock (Paper plugin, by Lunary1)** — a separate, server-side reimplementation with team management, scaling difficulty, biome-weighted costs, and optional OpenAI-powered dynamic pricing. ~1,000+ downloads, active dev, 8 reviews. Shows the SMP-scaling direction.
- **Chunk By Chunk (mod, by immortius/dividesBy0)** — the mod-based leader, MIT-licensed, Fabric/Forge/NeoForge/Quilt. Per Modrinth it shows ~40.3k downloads there (437K+ across all platforms per Modpack Index), "Published 4 years ago · Updated 2 years ago," latest version **2.1.1 for Fabric 1.20.1 released November 25, 2023** — confirming dormancy. Start in one 16×16 chunk; expand via World Cores → World Forge → Chunk Spawners (random, unstable, or biome-themed). Config from the Mods menu (needs Mod Menu on Fabric). Used in 44 modpacks.
- **Gathering Chunks (mod, unofficial CBC fork)** — actively maintained fork updating the concept to 1.21.1 (Fabric/NeoForge, no Forge), signalling live demand as the original stalls.
- **Skyblock Builder (mod, by MelanX)** — conceptual cousin (island-as-progression). Forge/NeoForge (not Fabric), needs LibX. **832.1K downloads on Modrinth and 20,247,389 on CurseForge (Project ID 446691, Apache-2.0, last updated Jun 7, 2026).** Config-driven island schematics, team islands, multiple spawn points. The base mod does *not* progressively gate expansion — it's config-driven — which is instructive: expansion pacing is exactly the design lever EmpiresMC is adding.
- **Haven Skyblock Builder (by CathieNova)** — ~66,900 CurseForge downloads; adds configurable cooldowns for island creation/home/visit — the closest existing analog to EmpiresMC's unclaim cooldown.
- **World-border-expansion datapacks** — a whole cluster: "Border Expands Per Day" (29.9K CF downloads), "Achievements Expand Border" (+42 blocks/advancement), "Bordercraft: Expanded" (start 1×1, every new item/advancement/mob/structure expands), WadZee's viral 3-blocks/day pack, and Planet Minecraft's "LEVEL = WORLD BORDER" (17.5K). These prove strong appetite for "progression gates space," and their common complaints (spawn RNG trolling, version incompatibility) are relevant.

### Nation/empire/territory mods (paced expansion)
- **Millénaire (by Kinniken)** — **3,856,575 downloads on CurseForge (Project ID 270871)**; NPC villages you earn control of; expansion is trust/trade-paced, not claim-paced. Actively maintained — the **NeoForge rewrite millenaire-9.0.0-beta.2.jar for MC 1.21.1 was released Jul 25, 2026**. Different mechanic, but owns the "empire" fantasy in single-player.
- **Cadmus / Odyssey Claims (by Terrarium)** — 714.2K downloads; Fabric/Forge/NeoForge; individual or team land claims with region flags; integrates with Argonauts/vanilla teams. Modern claim-API reference.
- **Argonauts (by Terrarium)** — the teams/guilds/parties layer many claim mods build on (GUI permission management).

### Mainstream claim ecosystem (the "expected feature baseline")
| Mod/Plugin | Platform | Popularity | Claim UX | Notable features |
|---|---|---|---|---|
| **FTB Chunks** | Fabric/Forge/NeoForge | 124.4M (NeoForge page) / 11.9M (Fabric page) | Map-GUI: left-click/drag to claim, right-click to unclaim, shift to force-load | Built-in minimap/map, teams, force-loading, fluid/fire/piston cross-border protection (perf-gated) |
| **Open Parties and Claims (Xaero)** | Fabric/Forge/NeoForge | 59.0M | Key-opened UI + Xaero map right-click | Parties, Xaero minimap/map integration, permission hooks (LuckPerms/FTB Ranks), API |
| **GriefPrevention** | Bukkit/Spigot/Paper | Most-used protection plugin | Golden-shovel gesture (right-click 2 corners); stick to inspect | Claim blocks accrued over playtime, auto-claim-on-first-chest (9×9), trust levels, `AbandonReturnRatio` |
| **Lands** | Paper/Spigot (paid) | Widely used | GUI menus + claim tool; chunk selection | Nations, wars, taxes/upkeep, level-based claim limits, leaderboards, web-map |
| **Cadmus/Odyssey** | Fabric/Forge/NeoForge | 714.2K | Team/individual claims | Region flags, admin claims, Protection/Flag/Team APIs |
| **Flan, Corail, ClaimIt, UltimateLandClaim** | various | ClaimIt 41.4K; others smaller | shears/two-corner or chunk right-click | ClaimIt built for mod-compat; UltimateLandClaim toggles chunk vs freeform |

**Claim-allowance economies (the conventions players compare against):**
- **GriefPrevention**: claim blocks *accrue over time played*. GriefPrevention's official docs list the config default as `Claims.BlocksAccruedPerHour: 100`, "awarded gradually (about every 5 minutes), but only to players who aren't just standing around doing nothing (idling)," with `Claims.MaxAccruedBlocks: 80000` and `AbandonReturnRatio: 1.0`. On abandoning, the ratio applies (1.0 = full refund; set <1.0 to punish claim-churn). The docs state this exists specifically because "a player could potentially claim land, build/break stuff, then move his land claim. You can discourage this using GriefPrevention's config file option to have players lose some or all claim blocks used in a land claim when abandoning the claim."
- **FTB Chunks**: fixed per-player grant (config), scalable per party member; no time-grind.
- **Lands**: level-based claim limits + recurring **upkeep**; unpaid upkeep auto-unclaims chunks.
- **Towny**: unclaim refund configurable but recommended *below* claim cost; optional `Revert_on_unclaim` rolls a chunk back to pre-claim state.
- **Factions**: power-based (power lost on death), unclaiming refunds nothing.

EmpiresMC's *item + XP-level tier ladder* (T2–T7, cumulative 1→39 chunks) is unusual — most systems use time, playtime, currency, or team size. That's a differentiator, but the risk is it reads as "grind" (see dislikes).

### What users LIKE (praise patterns)
- **The "locked world" reframes exploration as reward.** Chunk By Chunk writeups: sealing the world "turns exploration into a progression goal instead of a given," making "early stone, dirt, and ore feel far more valuable." This forced-focus/anti-sprawl appeal is exactly EmpiresMC's thesis.
- **Streamer/challenge virality.** Chunklock's Reddit reveal hit 7,000+ upvotes in 2 days; the format is inherently content-friendly (planning, scarcity, milestone dopamine).
- **Claim UX that's command-free and visual.** FTB Chunks' map-click model and GUI menus (Lands, Bell Claims — "never need to type a command ever again") are consistently praised as intuitive. GriefPrevention's golden shovel is loved for needing "ONLY the mouse, no slash commands."
- **"It just works" reliability.** Chunk Lock (GOGLEOX) markets exactly what SP players want: "Your claims survive restarts, crashes, backups, and server maintenance without needing a bunch of extra systems running in the background."

### What users DISLIKE / complain about (highest-value section)

**Protection-bypass bugs are the #1 recurring technical complaint in claim mods** — and EmpiresMC's server-side break/place/modify model inherits all of them:
- **Create contraptions bypass FTB Chunks** (FTB-Mods-Issues #1898): a player converts a moving drill contraption back to static blocks *inside* a claim, applies torque from outside, and mines protected blocks — "a serious protection vulnerability." Issue #923: "Anything that moves can grief inside claims from the Create mod."
- **Create mechanical drill** (Creators-of-Create #4367): FTB's own answer — Create "doesn't fire block break events," so "FTB Chunks has no way of preventing blocks being broken by Create."
- **Mekanism Digital Miner** bypasses protection (Mekanism #7697): filter-mines a protected base.
- **FTB Ultimine** harvest bypasses claims (#1587); **Blood Magic Sigils & Supplementaries Slingshot** place/break across claims (#250); **Ars Nouveau fake-players** get wrongly denied (#2048).
- **Indirect edits** FTB had to add opt-in protection for: **fluids flowing across claim boundaries, fire spread across boundaries, and pistons pushing blocks across boundaries** (including flying machines). Per the FTB-Chunks changelog, piston protection is "Enabled by default; can be disabled via 'piston_protection' server setting ... pistons cannot push blocks from the chunk the piston is in to another chunk if the new chunk is owned by a different team, and that team does not have public block-edit permissions." Fluid and fire-spread protection are *disabled by default* due to per-tick performance cost. These are precisely the "indirect edits" EmpiresMC flagged.
- **Explosion/mob-grief protection is coupled and hard to disable** (#329): claiming forces mob-grief protection some players don't want.
- **Map rendering can crash** (FTB #6/#182: "the map begins to crash whenever it's opened rendering FTB chunks unplayable"); border/waypoint rendering conflicts with Optifine/Sodium-family (Chunklock explicitly breaks under Optifine/Lunar).

**Design/balance complaints in progression-gating mods:**
- **Soft-lock risk** is the defining fear: needing a resource/structure inside a chunk you can't afford/unlock. World-border datapack comments repeatedly complain about bad spawns (desert/ocean) trapping players; Bordercraft: Expanded added a "safeguard" giving free early expansions if your first blocks are all dirt/sand precisely to fix this.
- **Structures that must be entered to progress** (strongholds/fortresses/monuments/ancient cities) and **structures straddling chunk lines** are a real generation reality — strongholds' end portals sit in specific chunks; nether fortresses are large. A gate that blocks the stronghold chunk blocks the entire End progression.
- **The "arbitrary chunk boundary" problem** — buildings straddle 16×16 lines; players dislike being unable to build across a border they didn't choose. No competitor solves this elegantly.
- **Farms/mob spawning in unclaimed/locked land** — Nether-portal chunk-loading is how many farms work; locking/gating dimensions or chunks silently breaks farms (Optifine #6346, Paper #7122 show how fragile portal chunk-loading already is).

**Single-player-specific skepticism:** the general modding reflex "why do I need claims in singleplayer?" is real — claim mods are perceived as multiplayer-only. EmpiresMC must communicate up-front that claiming here is a *progression system*, not griefing protection, or SP players will bounce on the category assumption. Conversely, chunk-*lock* formats (Chunklock, Chunk By Chunk) are proven to work great solo — so the framing is everything.

**Onboarding/discoverability:** GriefPrevention's docs admit the golden shovel is undiscoverable — the auto-claim-on-chest exists because players "don't know how to use the golden shovel," and it recommends spawn signs to teach. There are real reports of players unable to figure out claiming ("I managed to get my golden shovel and tried right-clicking blocks... but nothing happened"). This is a direct warning for EmpiresMC's gesture-based Scepter model.

### Design-risk questions answered with evidence
1. **Nether/End & other dimensions**: FTB Chunks and OPAC claim per-dimension. Chunk By Chunk generates dimensions but you still expand chunk-by-chunk. EmpiresMC's "all dimensions claimable, one shared allowance, no adjacency in 1.0" is sensible — the no-adjacency choice is explicitly to keep the Nether reachable, which is the correct call given how easily gating breaks portal-linked farms.
2. **Straddling/required structures**: No competitor elegantly solves buildings straddling chunk lines (it's an accepted annoyance). For *required* structures, the safe pattern is EmpiresMC's own "structure-aware claiming rules" backlog idea (e.g., auto-allow or discount stronghold/fortress chunks) — this should be promoted from backlog to near-term, because soft-lock is the #1 design complaint.
3. **Mob spawning/griefing/farms**: Coupling protection to claims (like FTB) frustrates players; keep explosion/mob-grief policy **configurable and decoupled** from the progression gate. Do not silently block portal chunk-loading.
4. **Visualization**: Map overlays (FTB/Xaero) are the most-liked and most-discoverable; in-world particle/wall borders are liked but are the #1 source of render lag and Optifine breakage. EmpiresMC's hold-Scepter-only + 5-chunk render cap is a reasonable perf compromise, but map-mod integration (its backlog item) is what players will expect.
5. **Unclaiming abuse & chunk scarring**: Full refund (GriefPrevention default `AbandonReturnRatio: 1.0`) is standard; partial refund (GP <1.0, Towny <claim-cost), upkeep auto-unclaim (Lands), and cooldowns are the established anti-abuse levers. **No precedent exists for "chunk scarring/decay"** — it's genuinely novel. The nearest analogs are destructive, not memory-based (Towny `Revert_on_unclaim`; "Damage Deletes Chunk" datapacks). EmpiresMC's current *full refund* + 1-day cooldown is the weakest anti-abuse posture; partial refund is the proven fix and should likely ship in 1.0.
6. **Gesture vs GUI/command**: Evidence favors **map/GUI as most discoverable** (FTB Chunks "as easy as," Bell Claims "never type a command"); gesture (golden shovel) is beloved-but-undiscoverable (GP compensates with auto-claim + signs); pure commands have a documented learning curve ("i still cant get this claiming down"). EmpiresMC's sneak-use gestures risk the same undiscoverability — the planned advancement-chain tutorial and Brigadier `/empiresmc help` are important mitigations, and a GUI/map layer is closer to table-stakes than backlog.
7. **Claim-allowance economies**: covered above — players dislike both grind (time-based) and opacity; the most-liked systems make the cost and remaining allowance always visible (EmpiresMC's HUD used/total + itemized missing costs is aligned with best practice).
8. **Publishing/discovery norms**: Successful claim/progression mods lead their pages with a one-line hook ("Expand your world, one chunk at a time!"), tag World Generation / Game Mechanics / Utility, list loader + MC-version matrix prominently, and state single-player support explicitly. Permissive licensing (MIT for Chunk By Chunk/OPAC source; Apache-2.0 for Chunklock and Skyblock Builder) is the norm. Mod Menu + config-screen (Cloth Config) support is an expected convenience on Fabric.

## Details — Table stakes vs. differentiators

**Table stakes (players will judge EmpiresMC against these; several are currently on its backlog and should move up):**
- Reliable, crash-safe, version-stable claim data (Chunk Lock's whole pitch).
- A **map/overlay view** of claims (FTB/Xaero set this expectation) — EmpiresMC's Xaero/JourneyMap integration is table stakes, not nice-to-have.
- **Discoverable onboarding** (tutorial/advancements, clear feedback messages) — gesture-only control is a known failure mode.
- **Anti-bypass protection** covering pistons/fluids/fire/machinery, or at least documented known-gaps — this is the #1 technical complaint.
- **Not breaking required progression** (strongholds/fortresses/monuments) and not silently breaking farms/portal chunk-loading.
- Config screen (ModMenu + Cloth Config) on Fabric.

**Genuine differentiators (defensible niche):**
- **Single-player-first "land is the progression system"** framing (vs. everyone else's multiplayer-protection framing).
- **Item + XP-level tier ladder** for claim allowance (novel vs. time/currency/team-size).
- **Stateless Scepter** (server-side UUID state, harmless loss/dupe) — clean design, though invisible to players.
- **Chunk scarring/decay** anti-abuse — no precedent anywhere; a true innovation if executed.

## Recommendations

**Stage 1 — Before/at 1.0 (de-risk the bounce points):**
1. **Ship partial refund on unclaim** (not full). Full refund + 1-day cooldown is the weakest anti-abuse posture in the market; a partial refund is the proven, low-effort fix (GriefPrevention `AbandonReturnRatio` <1.0, Towny). Reserve "chunk scarring" as the flagship 1.x feature and market it heavily — it's your only truly novel mechanic.
2. **Promote "structure-aware claiming" from backlog to 1.0.** Auto-allow or heavily discount chunks containing strongholds/fortresses/monuments/ancient cities. Soft-lock (needing an unreachable structure) is the single most-cited progression-mod complaint; shipping without this invites "I got soft-locked" reviews.
3. **Decouple mob-grief/explosion policy from the progression gate and make it config-toggled.** FTB's coupling is a documented irritant (#329). Never block Nether-portal chunk-loading (breaks farms).
4. **Invest in onboarding now.** Ship the advancement-chain tutorial and `/empiresmc help` in 1.0, plus unmissable first-use feedback ("You claimed chunk X; you have N/T left"). Gesture-only control without this repeats GriefPrevention's undiscoverability problem.
5. **Document known protection-bypass gaps** (Create/Mekanism/pistons/fluids) rather than pretending they don't exist — modpack authors specifically look for this.
6. **Rename or clearly differentiate.** "EmpiresMC" collides with a live server and evokes fWhip's Empires SMP. Either pick a name signalling the mechanic (e.g., something with "Claim"/"Scepter"/"Frontier") or lead the page with a crystal-clear one-liner so "Empires SMP" searchers immediately understand this is unrelated.

**Stage 2 — Fast-follow (meet table stakes):**
7. **Map-mod overlay integration (Xaero's/JourneyMap)** — expected, not optional. It's also the most-liked visualization and avoids in-world render lag.
8. **A minimal claim-management GUI** (even if gestures stay) — GUI/map models are consistently rated most discoverable.
9. **Datapack/JSON-driven tier definitions** for modpack authors — this is how you get into modpacks (Chunk By Chunk's 44-modpack footprint came from being modpack-friendly).

**Stage 3 — Differentiate & expand:**
10. Ship **chunk scarring/decay** as the headline 1.x feature; consider a config to disable it for players who find it punishing.
11. Multiplayer (teams/allies/protection-from-others) — this converts you from "solo challenge" to competing with FTB Chunks/OPAC/Cadmus; only pursue if you can match their protection robustness.

**Benchmarks that change the plan:**
- If early reviews cluster on "soft-locked" → structure-aware rules become P0 hotfix.
- If reviews cluster on "how do I claim?" → ship the GUI immediately.
- If download velocity stalls vs. Chunk By Chunk/Gathering Chunks → the incumbent's dormancy (Chunk By Chunk last updated Nov 2023) is your opening; prioritize latest-MC-version support and modpack-friendliness to capture their orphaned audience.

## Open questions for the EmpiresMC team to decide
1. **Name**: keep "EmpiresMC" and accept collision with the live server + Empires SMP fanbase, or rebrand toward the mechanic?
2. **Refund model**: full vs. partial refund in 1.0, and does chunk-scarring replace or supplement it?
3. **Structure gating**: auto-allow, discount, or fully-block chunks with progression-critical structures — and how to detect them cheaply server-side?
4. **Protection scope**: how aggressively to chase indirect-edit/machinery bypasses in a single-player context where griefing isn't the threat — is "prevent accidental edits" enough, or is exploit-tightness needed for the strip-mine anti-abuse story to hold?
5. **Control model**: commit to gesture-only for 1.0, or ship a GUI/map layer sooner given the discoverability evidence?
6. **Tier balance**: is the item+XP-level ladder (1→39 chunks) too grindy or too generous relative to Chunklock's biome-cost curve? Needs playtesting benchmarks.
7. **Loader/version target**: which MC version to launch on to maximize capture of Chunk By Chunk's orphaned audience, and whether to add NeoForge/Forge later.
8. **Licensing**: match the permissive norm (MIT/Apache-2.0) to encourage modpack adoption?

## Caveats
- **No public EmpiresMC repo/page was found**, so all EmpiresMC design details are taken from the task brief, not independently verified. The GitHub repo appears private or not yet published.
- **Download counts differ by page and are cumulative**, inflating perceived "active" userbase (e.g., FTB Chunks shows 124.4M on one page, 11.9M on another; Skyblock Builder 832.1K on Modrinth vs 20.2M on CurseForge). Treat them as order-of-magnitude signals, not active installs.
- Some sentiment is drawn from secondary aggregators (9Minecraft, feed-the-mods, Sportskeeda) and marketing copy; where possible I prioritized primary GitHub issues and official docs. A few Reddit/YouTube comment specifics could not be quoted verbatim within budget.
- The current date is July 31, 2026; competitor "last updated" dates mean the incumbent landscape may shift quickly as forks like Gathering Chunks mature and Millénaire/Skyblock Builder actively update.
- "Chunk scarring has no precedent" is an inference from absence of evidence across searched sources, not proof none exists anywhere.

### Selected sources (URLs)
- Chunklock datapack: https://modrinth.com/datapack/chunklock · https://www.curseforge.com/minecraft/data-packs/chunklock · https://www.planetminecraft.com/data-pack/chunklock/ · https://www.sportskeeda.com/minecraft/news-minecraft-player-creates-chunklock-new-way-play-game
- Chunklock plugin: https://www.spigotmc.org/resources/chunklock.125966/ · https://hangar.papermc.io/Lunary1/Chunklock
- Chunk By Chunk: https://modrinth.com/mod/chunkbychunk · https://github.com/immortius/chunkbychunk · https://www.curseforge.com/minecraft/mc-mods/chunk-by-chunk · https://www.modpackindex.com/mod/32138/chunk-by-chunk
- Gathering Chunks: https://modrinth.com/project/UgEJpudA
- Chunk Lock (GOGLEOX): https://www.curseforge.com/minecraft/mc-mods/chunk-lock
- Skyblock Builder: https://modrinth.com/mod/skyblock-builder · https://www.curseforge.com/minecraft/mc-mods/skyblock-builder · Haven: https://www.curseforge.com/minecraft/mc-mods/haven-skyblock-builder
- World-border datapacks: https://modrinth.com/datapack/achievements-expand-border · https://www.curseforge.com/minecraft/data-packs/bordercraft-expanded · https://www.curseforge.com/minecraft/texture-packs/border-expands-per-day · https://www.planetminecraft.com/data-packs/tag/border/
- FTB Chunks: https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge · https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-fabric · https://github.com/FTBTeam/FTB-Chunks/blob/main/CHANGELOG.md · https://docs.feed-the-beast.com/mod-docs/mods/suite/Chunks/claiming-loading/ · https://www.akliz.net/blog/posts/ftb-chunks
- FTB protection-bypass issues: https://github.com/FTBTeam/FTB-Mods-Issues/issues/1898 · /923 · /1587 · /250 · /2048 · /329 · https://github.com/Creators-of-Create/Create/issues/4367 · https://github.com/mekanism/Mekanism/issues/7697
- Open Parties and Claims: https://modrinth.com/mod/open-parties-and-claims · https://github.com/thexaero/open-parties-and-claims · https://www.curseforge.com/minecraft/mc-mods/open-parties-and-claims
- GriefPrevention: https://www.spigotmc.org/resources/griefprevention.1884/ · https://docs.griefprevention.com/configuration/ · https://dev.bukkit.org/projects/grief-prevention/pages/setup-and-configuration
- Lands / UltimateLandClaim / Bell Claims: https://voxel.shop/resource/lands-land-claim-plugin.876 · https://modrinth.com/plugin/ultimatelandclaim · https://github.com/mizarc/bell-claims
- Cadmus/Odyssey & Argonauts: https://modrinth.com/mod/odyssey-claims · https://github.com/terrarium-earth/Cadmus · https://www.9minecraft.net/argonauts-mod/
- Millénaire: https://www.curseforge.com/minecraft/mc-mods/millenaire · https://www.millenaire.org/downloads
- Name-collision: https://empires-smp.fandom.com/wiki/Mods · https://empires-smp.fandom.com/wiki/Empires_SMP · https://www.planetminecraft.com/server/empiresmc-6502137/
- Rendering/portal-loading context: https://github.com/sp614x/optifine/issues/6346 · https://github.com/PaperMC/Paper/issues/7122 · https://github.com/FTBTeam/FTB-Mods-Issues/issues/6