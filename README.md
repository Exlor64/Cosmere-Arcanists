# Cosmere Arcanists
Minecraft **1.21.1**, NeoForge **21.1.256**, Java **21**. Author: **Ex**.

## Origins and assignment
- Persistent planet/race and natural Allomantic/Feruchemical power identifiers.
- Command-based Scadrial and separate Overworld origins; human race only so far.
- RANDOM (default), CHOICE, and ANCESTRY assignment modes.
- Per-art and per-metal switches, birth probabilities, Twinborn and reroll controls.
- Data survives death and relogging. Config changes do not reroll saved origins.
- Hemalurgy is acquired rather than a birth power; its switch reserves the future feature gate.

**Iron/steel Allomancy and the power selector are implemented. Other magic effects, custom dimensions, planetary travel, and the origin selection screen remain in development.** Scadrial will be Era 2 with an Overworld-like environment. The ancestry mode currently uses only a Scadrian-human probability multiplier; family inheritance and additional races are future work.

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
1. Origin selection screen and expanded ancestry options.
2. Shared Investiture and ability access services respecting every relevant feature gate.
3. Remaining Allomancy, then metalminds/Feruchemy, then spikes/Hemalurgy.
4. Compounding and cross-system physics.
5. Scadrial terrain, Cognitive Realm, and perpendicularities.

Add behavior settings alongside each implemented feature; avoid presenting unused settings as working mechanics.

## Metallurgy (0.2.0)
Added tin, zinc, aluminum, chromium, cadmium, lead, silver, and nickel with raw materials, ingots, nuggets, and stone/deepslate ores. Vanilla iron, copper, and gold are reused; copper gains nuggets. All eight alloys have ingots/nuggets. Nicrosil is made from nickel and chromium only, as requested.

Alloy crafting uses coal for steel, tin/lead for pewter, copper/zinc for brass, copper/tin for bronze, aluminum/copper for duralumin, gold/silver for electrum, lead/tin/cadmium for bendalloy, and nickel/chromium for nicrosil. These are interim crafting batches, not canonical percentage recipes. Bismuth and silicon are not added.

Ores require an iron-tier pickaxe, support Silk Touch/Fortune, and smelt/blast into ingots. New ores generate in new Overworld chunks at Y -64 to 64 with six attempts per chunk and vein size seven. This is provisional balance. Adjust global/per-metal generation switches and vein acceptance probability in `config/cosmerearcanists-common.toml`; restart required. Per-alloy crafting switches are in the same file. Datapacks can modify vein size, height, attempt count, and all recipe ingredients/output quantities. Existing ores/items remain registered when generation or crafting is disabled.

The Cosmere Metals creative tab contains the new materials. Textures are initial distinct pixel palettes. `cosmerearcanists:metals` and `cosmerearcanists:aluminum_containing` item tags seed iron/steel detection. Duralumin is included in the aluminum exclusion tag.

## Iron/steel and power controls (0.3.0)
Only iron and steel Allomantic effects are implemented. Other assigned powers appear in the selector with **effects pending**; Feruchemical state levels are controls only, with no storage/tapping mechanics yet.

Default keys (all rebindable):
- Hold **R**, scroll to a power, then release R to select it. Tap R to cycle that power's state. Left-click while the menu is open also cycles the highlighted power's state.
- Hold **V** and scroll to change state; release to apply. Tap V cycles state. Allomancy has Off/Burning/Flaring. Feruchemy has Off, Storing 1–5, and Tapping 1–5.
- Hold **G** (Activate ability) to activate the currently selected implemented power. Iron/steel pull/push the metal source closest to your aim within the target cone while burning/flaring. Blue lines run from your chest to blocks, dropped items, and individual equipment sources. Lines are private to the Allomancer.

Craft an iron or steel vial with a glass bottle and one corresponding nugget. Right-click the vial to ingest it, returning the bottle. Each adds 60 reserve units by default (capacity 300). Burning costs one unit/second; flaring triples consumption and doubles force. Sensing consumes reserves too. States/reserves are separate per metal; switching selected power does not turn off other burning metals. Death retains powers/reserves/selection but turns active states off.

Fixed blocks pull/push the player. Dropped stacks and equipped entities move under force, with player reaction and mass-dependent acceleration. Forces weaken with distance and have a configurable velocity cap. Armor sources affect the equipped entity, not a detached piece of equipment. Upward iron/steel force reduces accumulated fall distance in proportion to the reduction in squared downward speed. Arresting a descent can cancel its accumulated damage; a later unbraked descent adds fall distance normally. Sideways/downward force or merely burning metal grants no protection. This mitigation is configurable. Server-approved propulsion resets vanilla's floating timeout without granting creative flight.

Server-authoritative controls/physics settings: `<world>/serverconfig/cosmerearcanists-powers-server.toml`. Client HUD/line settings: `config/cosmerearcanists-client.toml`. Existing per-art/per-metal gates still apply. Settings cover range, force, flaring, reserves, reaction, player targets, target categories, scanner budget, and sync interval. Block checks are spread across ticks and do not load chunks.

Detection seeds use common metal tags plus `cosmerearcanists:metals`, then trace crafting/smithing/stonecutting recipes recursively. Furnace recycling is excluded from ingredient propagation. Aluminum anywhere in a recipe chain makes the result undetectable, even when another recipe avoids aluminum. This conservative treatment prevents aluminum bypasses. `cosmerearcanists:aluminum_containing` seeds exclusions and `cosmerearcanists:undetectable` directly excludes specific items. Unknown mod materials should supply common ingot/nugget/ore tags or the explicit seed tag. Dynamic/NBT-dependent recipes may need explicit tags.

Operator testing commands:
- `/cosmere power grant <player> a:iron`
- `/cosmere power grant <player> a:steel`
- `/cosmere power grant <player> f:iron` (selector test only)
- `/cosmere power revoke <player> <power>` sets the power’s base attribute to zero, including natural origin powers.
- `/cosmere power refill <player>` fills iron and steel reserves.

`./gradlew build` runs tests for recursive metal/aluminum classification, cyclic recipes, power states, and mass/distance/flaring force rules. CI also runs the GameTest server to validate runtime recipe detection and player data cloning. Multiplayer input, blue-line appearance, and hands-on movement still require client playtesting.

Activation uses START/TICK/STOP phases, allowing future instant abilities to act on press and held abilities to act continuously. Selection/state changes, release, disconnect, reload, death, and dimension changes stop the old handler. Unimplemented powers still show effects pending. The activation key's internal identifier is preserved so existing keybinds remain intact.

### Persistent power attributes

All 16 Allomantic and 16 Feruchemical powers have player attributes named
`cosmerearcanists:allomancy_<metal>` and `cosmerearcanists:feruchemy_<metal>`.
Zero means no power; 1 is normal strength. Positive effective values grant access,
and iron/steel force scales with the effective value (including attribute modifiers).
Other powers still have selection/state support only. Config switches still gate access.

Operator examples:
```mcfunction
/attribute @s cosmerearcanists:allomancy_steel base set 1
/attribute @s cosmerearcanists:allomancy_steel base set 2
/attribute @s cosmerearcanists:allomancy_steel base set 0
/attribute @s cosmerearcanists:feruchemy_iron base set 1
```

Attributes persist through saving, reconnecting, death and dimension changes.
Respawning copies base values and permanent modifiers; burning/tapping states reset.
Old origins and operator grants migrate once. Origins describe initial ancestry;
attributes determine current powers. Grant sets at least normal strength; revoke
sets the base to zero. Modifiers can still grant a positive effective value.
Origin reset clears all power attributes and modifiers. Origin rerolls replace
the previous origin's base powers while preserving other attribute powers.

Metal block scanning publishes discoveries every tick, scanning a sphere from near to far. Moving does not restart an unfinished pass; teleporting beyond twice the radius does. The default snapshot interval is now 2 ticks (100 ms). Existing server configs retain their chosen interval; set `metalDetection.syncIntervalTicks = 2` for the faster cadence.
