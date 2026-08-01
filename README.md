# EmpiresMC

EmpiresMC is a Fabric mod for Minecraft that lets players claim, manage, and protect land as territory — giving communities a lightweight, in-game way to establish ownership without relying on external plugins.

## Development Setup

Requires JDK 25.

```sh
git clone https://github.com/DakotaTalley/EmpiresMC.git
cd EmpiresMC
./gradlew build
```

Other useful tasks:

- `./gradlew runClient` — launch a development client with the mod loaded.
- `./gradlew test` — run unit tests.
- `./gradlew runGameTest` — run gametests (also runs automatically as part of `build`).

## License

Code is licensed under the [Mozilla Public License 2.0](LICENSE) (MPL-2.0). MPL is file-level
copyleft: you may fork EmpiresMC, combine it with code under any other license, and ship it in
modpacks — but if you modify an MPL-covered source file, that file stays open under MPL.

Art assets — textures (`.png`) and sounds (`.ogg`) — are licensed separately under
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Functional resource files
(model, item, lang, and recipe JSON) are code and stay under MPL-2.0. The single SPDX `license`
field in `fabric.mod.json` reads `MPL-2.0` because it describes the code; it cannot express
the split.

Contributions are accepted inbound=outbound under these same terms, with no CLA to sign — see
[CONTRIBUTING.md](CONTRIBUTING.md).

### Continuing this mod

If EmpiresMC goes unmaintained for a Minecraft version, please fork it and keep it alive — that
is an explicit invitation, not merely a legal permission. Keep the license and the attribution,
and you have everything you need.
