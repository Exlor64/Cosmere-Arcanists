# Cosmere Arcanists
Minecraft **1.21.1**, NeoForge **21.1.256**, Java **21**. Author: **Ex**.

## Current milestone: origin foundation
- Persistent planet/race and natural Allomantic/Feruchemical power identifiers.
- Command-based Scadrial and separate Overworld origins; human race only so far.
- RANDOM (default), CHOICE, and ANCESTRY assignment modes.
- Per-art and per-metal switches, birth probabilities, Twinborn and reroll controls.
- Data survives death and relogging. Config changes do not reroll saved origins.
- Hemalurgy is acquired rather than a birth power; its switch reserves the future feature gate.

**No magic effects, custom dimensions, travel, or selection screen yet.** Scadrial will be Era 2 with an Overworld-like environment. The ancestry mode currently uses only a Scadrian-human probability multiplier; family inheritance and additional races are future work.

## Build and run
Install JDK 21 and import the root Gradle project into IntelliJ.

Windows: `./gradlew.bat build`, `./gradlew.bat runClient`, `./gradlew.bat runServer`.
Linux/macOS: `./gradlew build`, `./gradlew runClient`, `./gradlew runServer`.

Server settings are generated at `<world>/serverconfig/cosmerearcanists-server.toml`.
Use `defaultconfigs/cosmerearcanists-server.toml` to provide defaults for new worlds. Stop the server before editing config.

## Prototype commands
- `/cosmere origin scadrial` or `/cosmere origin overworld` in RANDOM/ANCESTRY modes.
- `/cosmere origin scadrial steel iron` in CHOICE mode (Allomancy first, Feruchemy second).
- Use `none` for either art in CHOICE mode.
- `/cosmere status` reports saved origin identifiers, including powers disabled by config.
- `/cosmere reset <player>` requires permission level 2 and clears an online player's origin.

Default birth chances are provisional: 25% for each natural art, independently. Each successful roll chooses uniformly among enabled metals. If Twinborn are disabled and both rolls succeed, one art is retained at random. A disabled art or empty metal pool produces no power. Origin selection does not teleport players yet.

## Development direction
1. Origin UI, validated client/server messaging, and automated persistence tests.
2. Shared Investiture and ability access services respecting every relevant feature gate.
3. Metal items/reserves and Allomancy, then metalminds/Feruchemy, then spikes/Hemalurgy.
4. Compounding and cross-system physics.
5. Scadrial terrain, Cognitive Realm, and perpendicularities.

Add behavior settings alongside each implemented feature; avoid presenting unused settings as working mechanics.

## Metallurgy (0.2.0)
Added tin, zinc, aluminum, chromium, cadmium, lead, silver, and nickel with raw materials, ingots, nuggets, and stone/deepslate ores. Vanilla iron, copper, and gold are reused; copper gains nuggets. All eight alloys have ingots/nuggets. Nicrosil is made from nickel and chromium only, as requested.

Alloy crafting uses coal for steel, tin/lead for pewter, copper/zinc for brass, copper/tin for bronze, aluminum/copper for duralumin, gold/silver for electrum, lead/tin/cadmium for bendalloy, and nickel/chromium for nicrosil. These are interim crafting batches, not canonical percentage recipes. Bismuth and silicon are not added.

Ores require an iron-tier pickaxe, support Silk Touch/Fortune, and smelt/blast into ingots. New ores generate in new Overworld chunks at Y -64 to 64 with six attempts per chunk and vein size seven. This is provisional balance. Adjust global/per-metal generation switches and vein acceptance probability in `config/cosmerearcanists-common.toml`; restart required. Per-alloy crafting switches are in the same file. Datapacks can modify vein size, height, attempt count, and all recipe ingredients/output quantities. Existing ores/items remain registered when generation or crafting is disabled.

The Cosmere Metals creative tab contains the new materials. Textures are initial distinct pixel palettes. `cosmerearcanists:metals` and `cosmerearcanists:aluminum_containing` item tags seed later detection; blue-line detection and pushes/pulls are not implemented yet. Duralumin is included in the aluminum exclusion tag.
