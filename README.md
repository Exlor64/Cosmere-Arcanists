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

## Iron/steel and power controls (0.3.0)
Only iron and steel Allomantic effects are implemented. Other assigned powers appear in the selector with **effects pending**; Feruchemical state levels are controls only, with no storage/tapping mechanics yet.

Default keys (all rebindable):
- Hold **R**, scroll to a power, then release R to select it. Tap R to cycle that power's state. Left-click while the menu is open also cycles the highlighted power's state.
- Hold **V** and scroll to change state; release to apply. Tap V cycles state. Allomancy has Off/Burning/Flaring. Feruchemy has Off, Storing 1–5, and Tapping 1–5.
- While burning/flaring iron or steel, hold **G** to pull/push the metal source closest to your aim within the target cone. Blue lines run from your chest to blocks, dropped items, and individual equipment sources. Lines are private to the Allomancer.

Craft an iron or steel vial with a glass bottle and one corresponding nugget. Right-click the vial to ingest it, returning the bottle. Each adds 60 reserve units by default (capacity 300). Burning costs one unit/second; flaring triples consumption and doubles force. Sensing consumes reserves too. States/reserves are separate per metal; switching selected power does not turn off other burning metals. Death retains powers/reserves/selection but turns active states off.

Fixed blocks pull/push the player. Dropped stacks and equipped entities move under force, with player reaction and mass-dependent acceleration. Forces weaken with distance and have a configurable velocity cap. Armor sources affect the equipped entity, not a detached piece of equipment. Fall damage remains vanilla. Server-approved propulsion resets vanilla's floating timeout without granting creative flight.

Server-authoritative controls/physics settings: `<world>/serverconfig/cosmerearcanists-powers-server.toml`. Client HUD/line settings: `config/cosmerearcanists-client.toml`. Existing per-art/per-metal gates still apply. Settings cover range, force, flaring, reserves, reaction, player targets, target categories, scanner budget, and sync interval. Block checks are spread across ticks and do not load chunks.

Detection seeds use common metal tags plus `cosmerearcanists:metals`, then trace crafting/smithing/stonecutting recipes recursively. Furnace recycling is excluded from ingredient propagation. Aluminum anywhere in a recipe chain makes the result undetectable, even when another recipe avoids aluminum. This conservative treatment prevents aluminum bypasses. `cosmerearcanists:aluminum_containing` seeds exclusions and `cosmerearcanists:undetectable` directly excludes specific items. Unknown mod materials should supply common ingot/nugget/ore tags or the explicit seed tag. Dynamic/NBT-dependent recipes may need explicit tags.

Operator testing commands:
- `/cosmere power grant <player> a:iron`
- `/cosmere power grant <player> a:steel`
- `/cosmere power grant <player> f:iron` (selector test only)
- `/cosmere power revoke <player> <power>` removes an operator grant; natural origin powers remain.
- `/cosmere power refill <player>` fills iron and steel reserves.

`./gradlew build` runs tests for recursive metal/aluminum classification, cyclic recipes, power states, and mass/distance/flaring force rules. CI also runs the GameTest server to validate runtime recipe detection and player data cloning. Multiplayer input, blue-line appearance, and hands-on movement still require client playtesting.
