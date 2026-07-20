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

Licensed under the [GNU Lesser General Public License v3.0 or later](LICENSE) (LGPL-3.0-or-later).
