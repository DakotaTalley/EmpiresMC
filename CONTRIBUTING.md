# Contributing to EmpiresMC

Thanks for wanting to help. This is a small project — an issue describing what you plan to change,
before you write it, will save us both time on anything larger than a bug fix.

## Licensing of contributions

**There is no CLA and no DCO to sign.** Contributions are accepted inbound=outbound: what you
submit is licensed under the same terms the project already uses —

- **Code** — [MPL-2.0](LICENSE)
- **Art assets** (textures, sounds) — [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)

You keep copyright in your contribution. There is no rights assignment, and you are not asked to
grant any rights beyond the project licence itself.

### New source files need a licence header

MPL-2.0 §1.4 defines covered software by the notice attached to each file — unlike MIT or the GPLs,
a root `LICENSE` file alone does **not** cover a new source file. Every `.kt` file must therefore
start with:

```kotlin
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
```

`./gradlew build` enforces this via the `checkLicenseHeaders` task, so you cannot forget it silently.

## Development setup

Requires **JDK 25**.

```sh
git clone https://github.com/DakotaTalley/EmpiresMC.git
cd EmpiresMC
./gradlew build
```

- `./gradlew runClient` — dev client with the mod loaded
- `./gradlew test` — unit tests
- `./gradlew runGameTest` — gametests (also run as part of `build`)

## Before opening a PR

- **`./gradlew build` passes locally.** This runs unit tests, gametests, and the licence-header
  check in one go.
- **New behaviour comes with a test.** Prefer a unit test; use a gametest where the behaviour only
  exists against a running server.
- **Some things CI cannot check for you.** Sounds and translation keys fail *silently* — a sound
  that never reaches the client throws nothing and passes every gametest, and a missing translation
  key renders as the raw key. If you touch either, verify by ear or by eye in a real client and say
  so in the PR.
- Play sounds through `util/PlayerSounds.playTo(...)`, never `player.playSound(...)` — the latter,
  called server-side, plays to everyone *except* that player, making it inaudible in single-player.

## Project conventions

- Development is organised as phased plans under [`plans/`](plans/). If your change belongs to a
  phase, link it in the PR; standalone fixes can say "N/A".
- Repo-wide development and security guidance lives in [`.ai/`](.ai/), with framework-specific rules
  under [`.frameworks/`](.frameworks/). Worth skimming before a first contribution.
