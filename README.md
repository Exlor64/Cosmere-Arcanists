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
- Hold **V** and scroll to change state; release to apply. Tap V cycles state. Allomancy has Off/Low burn/Burning/Flaring. Feruchemy has Off, Storing 1–5, and Tapping 1–5.
- Hold **G** (Activate ability) to activate the currently selected implemented power. Iron/steel pull/push the metal source closest to your aim within the target cone while burning/flaring. Blue lines run from your chest to blocks, dropped items, and individual equipment sources. Lines are private to the Allomancer.

Craft an iron or steel vial with a glass bottle and one corresponding nugget. Right-click the vial to ingest it, returning the bottle. Each adds 60 reserve units by default (capacity 300). Low burn applies half force at half consumption; Burning costs one unit/second at normal force; Flaring triples consumption and doubles force. Sensing consumes reserves too. States/reserves are separate per metal; switching selected power does not turn off other burning metals. Death retains powers/reserves/selection but turns active states off.

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

### Resistance and alternate steel movement

Solid blocks between the player's chest and a metal target add hardness-based
detection distance. The target itself and player voxel are excluded; air and
noncolliding blocks add no resistance. Defaults: 0.25 blocks of distance per
hardness point, capped at 50 per blocker (also used for unbreakable blocks).
For example, one obsidian blocker adds 12.5 blocks to the detection distance.
Disable or tune `metalDetection.blockResistance`, `resistanceDistancePerHardness`,
and `resistanceHardnessCap` in the powers server config. This gates both lines
and usable targets, including equipped gear and dropped items.

Hold **Left Alt** (Alternate power function) before pressing **G** with steel selected
and burning/flaring. While holding G, WASD requests horizontal movement relative
to player yaw, Space requests upward movement, and Shift requests downward movement.
The alternate mode latches at activation; release G before changing modes.
It shares the usual fuel, range, power strength, speed cap and fall braking rules.
Iron uses the same alternate controls to pull toward anchors in the requested direction.

Steel assistance uses up to four contributions from real detected metal anchors.
Each push follows the chest-to-metal line, with one combined force budget so dense
metal builds do not multiply power. Oblique anchors receive less force; anchors
more than 60 degrees off the requested direction cannot contribute, and a combined
force more than 45 degrees off is rejected. No movement input or suitable metal
means no assisted push. Set `ironSteel.alternateMovement = false` to disable it;
`movementAnchorAngleDegrees` tunes anchor acceptance. Equipped items on the same
entity share one anchor; entity recoil must be enabled to assist from loose gear/items.

Normal targeting now prefers the actual metal block hit by the crosshair, including
its edges. Forces still follow physical chest-to-metal directions, so an off-center
anchor can cause a diagonal push. Distance and target mass still influence force.
Player impulse calculations preserve observed movement on all three axes, since
server-side player delta motion does not retain horizontal client momentum.

Fast line discovery (0.5.1): each active tick refreshes a 6-block neighborhood at
the current player position. Falling or fast movement into a new block, and
starting activation, additionally scan up to 32768 near-first positions at the
current position (the full default 16-block sphere). All scans use loaded chunks
only and still apply hardness resistance. Configure `immediateScanRadius` and
`urgentBlockChecksPerTick` under `metalDetection`; reducing the urgent budget
reduces server work at the expense of distant anchor discovery during falls.
New or removed targets sync immediately even with an existing slower
`syncIntervalTicks`; periodic state/reserve updates now default to every tick.
Network latency and server tick rate still affect when lines reach the client.

0.5.2 fixes the horizontal-momentum regression introduced in 0.5.0. Horizontal and diagonal pushes/pulls now preserve observed momentum alongside vertical fall momentum.

0.5.3: player forces use an additive server-to-client impulse instead of replacing
client velocity with position displacement, preventing walking feedback while
preserving real inertia on all axes. Vanilla walking input opposing a recent
Allomantic impulse is removed, while perpendicular strafing remains available.
Disable that input rule with `ironSteel.preventWalkingAgainstForce = false`.
Actual momentum still takes force/time to reverse; this does not instantly stop
a player already moving toward an anchor. Input filtering expires after impulses
stop. Both client and server must update to this version (network protocol 2).

0.6.0: iron force now defaults to twice the previous strength, equivalent to the
old iron attribute value of 2 at a current attribute value of 1. The attribute
itself remains unchanged. Tune `ironSteel.ironStrengthMultiplier` (default 2).
This multiplier affects both aimed and assisted iron pulls and stacks with flaring.
Steel strength and reserve drain rates are unchanged. Range falloff still applies.

With iron selected and burning/flaring, hold Left Alt before G, then use WASD,
Space or Shift to pull toward suitable metals. Space requires overhead metal;
strafing left requires metal to the left. No suitable anchor means no force.
It shares steel's angle limits and single combined force budget. Disable iron
assistance separately with `ironSteel.ironAlternateMovement = false`.

0.6.1: Allomancy now has three active levels plus Off: Low burn (state 1),
Burning (2), Flaring (3). R tap/V tap cycle through all four states; hold V and
scroll to select a level. Applies to all Allomantic selectors; only implemented
powers currently have effects. Both aimed and alternate iron/steel use the levels.
Default force multipliers are 0.5/1/2 and consumption multipliers 0.5/1/3.
The iron base multiplier remains 2, so normal Burning at attribute 1 retains the
new iron baseline; Low burn deliberately provides a weaker pull.

Configure `lowBurnStrengthMultiplier`, `burningStrengthMultiplier`,
`flareStrengthMultiplier`, `lowBurnDrainMultiplier`, `burnDrainPerSecond`, and
`flareDrainMultiplier` under `ironSteel`. Existing saved Burning/Flaring states
migrate once to retain their meanings. Disabling flaring falls back to Burning.
Both client and server must update (network protocol 3).

### Steel projectiles (0.7.0)

While steel is burning at any active level, throw a metal item with Q (or drop a
stack from the inventory). It launches along your aim without needing G.
Nonmetal and aluminum-containing drops remain ordinary items. Steel must be
available/enabled, with enough reserves when consumption is enabled. The shot
uses normal power attributes and Low burn/Burning/Flaring multipliers.

Content is measured in nugget-equivalents: nugget 1, ingot 9, iron block 81,
anvil 279. Crafting/smithing/stonecutting ingredient counts and output counts
are traced recursively; nonmetal ingredients add no metal. Ingredient/recipe
alternatives use the smallest resolvable amount. Primitive raw metals/ores use
one ingot-equivalent as a balance approximation. Unresolved detectable items
use the configurable fallback. The entire thrown stack is one projectile.

At normal strength a nugget launches at 3.5 blocks/tick and deals about 2 damage
before armor. Launch speed scales with sqrt(power) divided by the fourth root
of metal content. A baseline anvil launches at about 0.86 blocks/tick and gives
a substantial backward recoil, versus almost no recoil for a nugget.
Impact energy sets damage through an exponential saturation curve approaching
100; no hit can exceed 100 raw damage. Higher impact speed/power and more metal
increase damage smoothly. Armor and normal hurt immunity still apply.

Shots use swept projectile collision, have gravity/drag, and recover the exact
original stack on their first impact or after five seconds in flight. They do
not destroy terrain. Normal pickup delay applies. Player hits respect the
existing player-target toggle, team restrictions, and vanilla damage rules.

The `steelProjectiles` server config controls enablement, reference speed,
maximum speed, nugget damage, damage ceiling (at most 100), recoil mass, unknown
metal fallback, lifetime, and extra reserve cost per shot (default 1).
`ironSteel.reactionForce` also controls launch recoil. Update both sides for
the new projectile entity and renderer. Client appearance still needs in-game
verification; automated tests cover content, damage, collision and recovery.
