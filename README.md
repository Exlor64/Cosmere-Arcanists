# Cosmere Arcanists
Minecraft **1.21.1**, NeoForge **21.1.256**, Java **21**. Author: **Ex**.

For current project status, accepted design decisions, verification and continuation notes, read [HANDOFF.md](HANDOFF.md). Development and handoff-maintenance instructions are in [AGENTS.md](AGENTS.md).

## Origins and assignment
- Persistent planet/race and natural Allomantic/Feruchemical power identifiers.
- Command-based Scadrial and separate Overworld origins; human race only so far.
- RANDOM (default), CHOICE, and ANCESTRY assignment modes.
- Per-art and per-metal switches, birth probabilities, Twinborn and reroll controls.
- Data survives death and relogging. Config changes do not reroll saved origins.
- Hemalurgy is acquired rather than a birth power; its switch reserves the future feature gate.

**Iron/steel/tin/pewter/electrum/atium/bronze/copper/zinc/brass Allomancy and the power selector are implemented. Other magic effects, custom dimensions, planetary travel, and the origin selection screen remain in development.** Scadrial will be Era 2 with an Overworld-like environment. The ancestry mode currently uses only a Scadrian-human probability multiplier; family inheritance and additional races are future work.

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
Iron, steel, tin, pewter, electrum, atium-alloy, bronze, copper, zinc and brass Allomantic effects are implemented. Other assigned powers appear in the selector with **effects pending**; Feruchemical state levels are controls only, with no storage/tapping mechanics yet.

Default keys (all rebindable):
- Hold **R**, scroll to a power, then release R to select it. Tap R to cycle that power's state. Left-click while the menu is open also cycles the highlighted power's state.
- Hold **V** and scroll to change state; release to apply. Tap V cycles state. Allomancy has Off/Low burn/Burning/Flaring. Feruchemy has Off, Storing 1–5, and Tapping 1–5.
- Hold **G** (Activate ability) to activate the currently selected implemented power. Iron/steel pull/push the metal source closest to your aim within the target cone while burning/flaring. Blue lines run from your chest to blocks, dropped items, and individual equipment sources. Lines are private to the Allomancer.

File metal into loose filings, then load up to 16 into a dynamic metal vial. Hold right-click to drink it like a potion. Each consumed filing adds 50 reserve of its metal; filings that cannot fit under the reserve cap (default 300 per metal) remain in the vial. Empty vials return a glass bottle. Low burn applies half force at half consumption; Burning costs one unit/second at normal force; Flaring triples consumption and doubles force. Sensing consumes reserves too. States/reserves are separate per metal; switching selected power does not turn off other burning metals. Death retains powers/reserves/selection but turns active states off.

Fixed blocks pull/push the player. Dropped stacks and equipped entities move under force, with player reaction and mass-dependent acceleration. Forces weaken with distance and have a configurable velocity cap. Armor sources affect the equipped entity, not a detached piece of equipment. Upward iron/steel force reduces accumulated fall distance in proportion to the reduction in squared downward speed. Arresting a descent can cancel its accumulated damage; a later unbraked descent adds fall distance normally. Sideways/downward force or merely burning metal grants no protection. This mitigation is configurable. Server-approved propulsion resets vanilla's floating timeout without granting creative flight.

Server-authoritative controls/physics settings: `<world>/serverconfig/cosmerearcanists-powers-server.toml`. Client HUD/line settings: `config/cosmerearcanists-client.toml`. Existing per-art/per-metal gates still apply. Settings cover range, force, flaring, reserves, reaction, player targets, target categories, scanner budget, and sync interval. Block checks are spread across ticks and do not load chunks.

Detection seeds use common metal tags plus `cosmerearcanists:metals`, then trace crafting/smithing/stonecutting recipes recursively. Furnace recycling is excluded from ingredient propagation. Aluminum anywhere in a recipe chain makes the result undetectable, even when another recipe avoids aluminum. This conservative treatment prevents aluminum bypasses. `cosmerearcanists:aluminum_containing` seeds exclusions and `cosmerearcanists:undetectable` directly excludes specific items. Unknown mod materials should supply common ingot/nugget/ore tags or the explicit seed tag. Dynamic/NBT-dependent recipes may need explicit tags.

Operator testing commands:
- `/cosmere power grant <player> a:iron`
- `/cosmere power grant <player> a:steel`
- `/cosmere power grant <player> f:iron` (selector test only)
- `/cosmere power revoke <player> <power>` sets the power’s base attribute to zero, including natural origin powers.
- `/cosmere power refill <player>` fills all ten implemented Allomantic reserves.

`./gradlew build` runs tests for recursive metal/aluminum classification, cyclic recipes, power states, and mass/distance/flaring force rules. CI also runs the GameTest server to validate runtime recipe detection and player data cloning. Multiplayer input, blue-line appearance, and hands-on movement still require client playtesting.

Activation uses START/TICK/STOP phases, allowing future instant abilities to act on press and held abilities to act continuously. Selection/state changes, release, disconnect, reload, death, and dimension changes stop the old handler. Unimplemented powers still show effects pending. The activation key's internal identifier is preserved so existing keybinds remain intact.

### Persistent power attributes

All 16 ordinary Allomantic and 16 Feruchemical powers have player attributes named
`cosmerearcanists:allomancy_<metal>` and `cosmerearcanists:feruchemy_<metal>`.
The atium–electrum alloy additionally has `cosmerearcanists:allomancy_atium`.
Zero means no power; 1 is normal strength. Positive effective values grant access,
and iron/steel force scales with the effective value (including attribute modifiers).
Powers other than iron, steel, tin, pewter, electrum, atium, bronze, copper, zinc and brass still have selection/state support only. Config switches still gate access.

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

While steel is selected and burning at any active level, hold **G** (Activate
ability) and throw a metal item with **Q**. It launches along your aim only while
the server has a fresh held-activation input. Burning alone, activating another
power, a brief released tap, and expired hold inputs leave ordinary drops.
Inventory drops also require that held activation; opening a GUI normally
releases activation.
With **Alt held before G**, Q instead drops metal straight down at your feet as
an ordinary item, without projectile damage, launch recoil, or shot reserve
cost. This uses the latched alternate mode; release G before switching modes.
Nonmetal and aluminum-containing drops remain ordinary items. Steel must be
available/enabled, with enough reserves when consumption is enabled. The shot
uses normal power attributes and Low burn/Burning/Flaring multipliers.

Content is measured in nugget-equivalents: nugget 1, ingot 9, iron block 81,
anvil 279. Crafting/smithing/stonecutting ingredient counts and output counts
are traced recursively; nonmetal ingredients add no metal. Ingredient/recipe
alternatives use the smallest resolvable amount. Primitive raw metals/ores use
one ingot-equivalent as a balance approximation. Unresolved detectable items
use the configurable fallback. The entire thrown stack is one projectile.

At reference push force 7 a nugget launches at 3.5 blocks/tick and deals about 2 damage
before armor. Launch speed scales with effective push strength divided by the fourth root
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

0.7.1 corrects projectile activation: active steel pushing (G held, steel selected and burning) is required. Releasing G restores normal drops immediately on the server. Projectiles already fired continue their physical flight.

0.7.2: Alt+G+Q drops metal straight down rather than firing. Normal G+Q still shoots. `steelProjectiles.alternateDropDownwards` controls repositioning; disabling it retains ordinary vanilla drops during alternate mode, never projectile fire.

0.7.3: in-flight steel-launched metal is detectable, pushable and pullable by
other iron/steel Allomancers (and the shooter) through normal targeting or
alternate movement assistance. Blue lines follow the projectile center. Range,
hardness, aluminum exclusion, burn level, attributes, fuel and reaction-force
rules still apply. Configure `metalDetection.firedProjectiles` to disable this
interaction separately. Mass uses the actual carried metal content, so anvils
are harder to deflect than nuggets. Forces change existing flight velocity,
without creating a second projectile or replacing its original shooter/cargo.
Damage still depends on impact speed and remains capped at 100. Deflected
projectiles use the projectile speed cap rather than the player movement cap.

0.7.4: projectile launch velocity now scales **linearly** with effective steel
push strength (force setting × effective attribute × burn multiplier), rather
than its square root. At the original 0.7.4 defaults one nugget traveled 1.75 blocks/tick on
Low burn, 3.5 on Burning and 7 on Flaring at attribute 1. Attribute 2 doubles
those speeds until the configured projectile maxSpeed (default 8) is reached.
Heavy cargo still reduces speed. Recoil and impact damage follow the resulting
velocity; the damage ceiling remains 100. Existing in-flight push/pull also
uses effective Allomantic force and cargo mass.

### Two activation slots (0.8.0)

**G** activates the primary power; **H** activates the secondary power. Both
are rebindable and may be held together. Alt mode latches independently when
each activation starts. Each power still needs its own active state/reserves.

Hold R, scroll to highlight a power, and **right-click to switch its assignment
between primary and secondary**. Moving a power already assigned to the other
slot swaps them. Left-click still cycles its state. Releasing R after a
right-click preserves the assignment; ordinary scroll-and-release chooses
primary as before. HUD and menu show the actual bound key for each assignment.
V continues to change the primary power's state; use menu left-click to cycle
secondary's state. Assignments persist through respawn/save/reconnect.

Steel throws use whichever key has steel assigned: normal held G or H + Q
fires; Alt before that key + Q drops downward. The other key cannot authorize
a steel shot. Separate hold heartbeats/release and lifecycle cleanup apply.
Set `controls.secondaryActivation = false` to disable the secondary slot
without deleting its saved assignment. Both sides must update: protocol 4.

### Equipment (0.9.0)

- **Spikes:** two ingots of the same metal diagonally craft two spikes. There
  are 19 metal variants, including aluminum/duralumin (which Allomancy cannot
  detect). These are physical ammunition, not charged Hemalurgic spikes yet.
- **Spike crossbow:** craft a vanilla crossbow with two iron ingots. Load and
  fire like a normal crossbow, using spikes instead of arrows. Spikes stick
  into blocks, can be picked up nearby, and become iron/steel anchors.
- **Metal ball and rope:** craft one non-aluminum ingot with three string.
  Hold steel's assigned activation key while burning, then right-click to
  launch. Damage/speed use steel strength and metal content. Right-click
  again to reel it in for reuse. The weapon stays in inventory.
- **Grappling hook:** craft iron + tripwire hook + three string in the supplied
  shaped recipe. Right-click, charge and release (full charge in 10 ticks).
  It latches to solid blocks; swing on its rope, hold Space to shorten it
  slowly, or target it with Allomantic iron to pull yourself toward it.
  Right-click again recalls it. No Allomantic power is required for normal
  grapple use.

Equipment feature gates, range, return speed, grapple launch/charge, Space
shortening rate and spike damage/lifetime live in the equipment server config.
Tethers have one active head per inventory item; lost ownership/items end the
tether, and range overflow recalls it. Recipe choices beyond the user-specified
spikes and ball ingredients are provisional. Current icons reuse vanilla item
art; physical rope renders separately. Equipment initially used protocol5 and pewter protocol6; current bronze/copper release requires protocol9 on both sides.

Returning tether heads temporarily leave Allomantic targeting until reused, so holding steel does not fight recall. Spike ammunition currently does not implement arrow-only Piercing behavior.


### Steel strength (0.9.1)

Steel now defaults to twice its previous force, matching the iron boost. Attribute
1 now applies the former attribute 2 force at each burn level. Both aimed pushes
and Alt movement assistance use this boost; attributes, burn levels and range
falloff still scale force normally. Tune `ironSteel.steelStrengthMultiplier`
(default 2) in the powers server config; set it to 1 for the previous balance.
Reserve drain is unchanged.

Projectile and reusable ball launch speeds also follow the increased push
strength. At default steel attribute 1, a nugget launches at 3.5 blocks/tick on
Low burn, 7 on Burning and the configured cap of 8 on Flaring. Impact damage and
recoil follow actual velocity; the damage ceiling remains 100.


0.9.2: Alt can now be pressed immediately before activation, within the same
client tick; no extra tick of waiting is required. The alternate mode still
latches when G/H is pressed. Iron movement pulls toward metal in the requested
direction (Space needs overhead metal); steel pushes from metal opposite it.
Hold the activation key while changing WASD/Space/Shift. Release G/H to change
between normal and alternate use. Iron assistance must be enabled via
`ironSteel.ironAlternateMovement` in the powers server config.


### Allomantic pewter (0.10.0)

Grant with `/attribute @s cosmerearcanists:allomancy_pewter base set 1`, then
`/cosmere power refill @s`. Select pewter with R and use V to choose Low burn,
Burning, or Flaring. Pewter works passively while burning, with no G/H hold
required, even if another power is selected. Pewter filings can be loaded into the same dynamic vials as other metals (50 reserve per filing by default).

At attribute 1, Low/Burning/Flaring provide:

| Effect | Low burn | Burning | Flaring |
| --- | --- | --- | --- |
| Movement speed bonus | 10% | 20% | 40% |
| Melee damage bonus (health points) | 1.5 | 3 | 6 |
| Exhaustion prevented | 25% | 50% | 90% |
| Damage mitigation | 12.5% | 25% | 50% |
| Healing per 10 seconds (health points) | 0.5 | 1 | 2 |

Effects multiply by the effective pewter attribute. Mitigation is capped at 50%
for attributes <=1 and 70% for stronger attributes; speed, melee and endurance
also have configurable caps. The exhaustion reduction applies before vanilla
spends saturation or food, including sprinting, jumping, mining and attacking.

After armor, pewter mitigation and absorption, 10% of health damage is **deferred**
as pewter drag; the remaining 90% is taken immediately. For example, a 10-point
unarmored hit while Burning at attribute 1 deals 6.75 now and stores 0.75. A
separate row of gray hearts above the health/armor HUD shows this debt, with an
exact numeric amount (2 points = one heart). These hearts are not bonus health.

Stopping pewter, losing its attribute, or exhausting the reserve charges all
remaining debt once. This is already mitigated injury: the payout directly lowers
health, bypasses armor, absorption, invulnerability frames and totems, and can
kill. Debt persists across saves, relogging and dimension travel. Death clears it.

While burning and stationary on the ground or sleeping, each continuous 5-second
rest interval clears 1% of remaining debt per effective strength. Moving resets
the rest timer. Saturation also clears debt passively: default 0.2 saturation per
second per strength, clearing one damage point per saturation spent. It spends
nothing without debt and cannot spend more than needed. Healing, recovery,
bonuses and mitigation scale with burn strength; the deferred injury fraction
stays 10% across levels.

Tune/disable individual effects in `cosmerearcanists-pewter-server.toml`; zero
rates disable their effect. Disabling pewter, its per-metal/art gate, or drag
freezes saved debt rather than deleting or charging it. Re-enabling resumes
normal behavior. Client `pewterDragHearts` controls the debt HUD independently.
Reserve drain uses the shared burn-level consumption multipliers. Both client
and server must update (network protocol 6).


### Metal filing and mixed vials (0.11.0)

1. Craft a **Metal File** with an iron ingot above/right of a stick in a 2×2 grid.
2. Hold the file in your main hand and an ingot, nugget or metal storage block
   in your offhand. Right-click to produce one filing. One physical piece stays
   in your offhand; the untouched remainder of a stack returns to inventory.
3. Hold the filings in one hand and a glass bottle or metal vial in the other.
   Right-click the filings to load one. A glass bottle becomes a metal vial;
   alternatively craft an empty metal vial from one glass bottle.
4. Hold right-click on the vial for 32 ticks to drink. No reserves are granted
   if you release early. Each consumed filing restores 50 of its own metal's
   reserve. Empty vials return a glass bottle; excess filings remain in the vial
   when their metal reserve cannot fit the full 50. Put loose filings away from
   the other hand before drinking; that combination loads instead.

| Material | Total filings |
| --- | ---: |
| Nugget | 4 |
| Ingot | 36 |
| Metal storage block | 324 |

The file has 256 uses by default. Partially filed pieces become single items
with a durability bar and remaining-filings tooltip. They can be filed further,
but cannot be crafted, repaired/recycled through recipe machines, or placed as
blocks. This prevents turning a worn piece back into full material. Progress
persists through save/load and network synchronization. Full depletion consumes
the piece. The original item type and unrelated custom components are retained.

A vial holds **16 total filings**, freely mixed: 3 iron + 13 steel carries
150 iron reserve and 650 steel reserve. At the existing default cap of 300 per
metal, drinking that mixture leaves 7 steel filings in the vial. Raise
`ironSteel.reserveCapacity` if you want to ingest all 13 steel filings at once.
All 21 supported metals are fileable, including non-Allomantic alloy ingredients;
storing a reserve never grants a power or implements an unfinished effect.
Common tags support other mods' forms of these same metals. Ores/raw metal,
raw storage blocks, equipment, and arbitrary crafted metal items cannot be filed.

Any aluminum or duralumin filing makes the entire vial undetectable to
iron/steel. Loose filings and partial pieces also retain correct detection
and remaining-metal projectile mass.

Tune/disable the system in `cosmerearcanists-filings-server.toml`: filings per
nugget, vial capacity, reserve per filing, drink duration and file wear. Existing
partially filed pieces retain their original capacity. Disabling filing leaves
saved contents/progress intact and keeps worn metal excluded from recipes.
Old iron/steel/pewter vial IDs remain loadable and drinkable as one filing each
(50 reserve); their old bottle+nugget recipes are replaced by the filing workflow.
File/filing/vial icons currently reuse vanilla assets. Both client and server
need the update; protocol remains 6 (no payload changes).


### Allomantic tin (0.12.0)

Tin works passively while burning, even when another power is selected. Night
vision and held zoom work at lower levels. **Sound wireframes require high tin
strength**: at attribute 1, Low burn and Burning reveal nothing; Flaring gives
only a faint, brief glimpse of a small area. Higher attributes develop its
potential for the planned progression system. Reveals show the noisy entity's mesh, plus nearby exposed collision shapes such as
floors, walls, stairs and doors. Silent entities are not scanned or revealed merely
because they stand near a sound. Block-only noises reveal terrain, not a guessed
nearby mob. Echoes fade and remember the sound's location, not a continuously
tracked silent entity. Untracked entity models fall back to a source outline.

Hold tin's assigned activation key (G or H) for zoom. At attribute 1 Burning the
default is 2.5×; higher burn/attribute strength increases zoom up to 8×. Tin uses
the shared burn levels and drain multipliers, with base drain 1 reserve/sec.
Tin filings restore its reserve; `/cosmere power refill @s` includes tin.

**Sound blocking:** intervening collision geometry adds effective hearing
distance. Default per block is 2 + 0.75 × hardness (hardness capped at 10), with
12 additional distance for wool. More thickness/harder blocks muffle wireframes;
enough obstruction hides them completely. Attenuation reduces opacity, revealed
area, detail and persistence. This is a direct-path approximation, not acoustic
reflection/diffraction simulation. It affects tin's visual hearing effect; vanilla
audio playback is unchanged.

Effective strength is attribute × burn multiplier. The default wireframe unlock
threshold is 1.5; potential rises smoothly to full strength at 6 (attribute 3
Flaring). Baseline Flaring reaches 2, about 11% of full potential. Potential
reduces range, opacity, surface radius, detail and persistence. Higher attributes
can eventually reveal sound even without Flaring. The progression/XP system
itself is not implemented yet.

The base hearing range formula is 24 × sqrt(effective strength) × sqrt(volume),
capped at 64, then scaled from half to full range by wireframe potential. Base
surface radius is 3 (cap 6), and fade is 24 ticks (cap 60); potential and
obstruction further limit these. The server sends only validated,
bounded geometry from loaded chunks to active tin users in the same dimension.
Each listener has sound budgets/cooldowns; it is not a continuous X-ray scan.

Configure `cosmerearcanists-tin-server.toml` for enablement, sight/hearing/zoom,
unlock/full-strength thresholds, range, obstruction/hardness/wool costs, audibility cutoff, shape/network budgets,
fade and fuel drain. Client `tin` options control wireframe/model rendering,
opacity, line width, visible echo count and zoom. Night vision supplied by tin
is removed on stop; independent potion effects are preserved. Sensory overload
is not included in this release.

Both client and server must update for **protocol 7**. Model wireframe appearance,
compatibility with third-party entity renderers, and in-game sound coverage still
need client playtesting. Some purely client-local/mod-specific sounds that do not
emit a server sound event cannot generate a server-authorized reveal.


### Atium and lerasium materials (0.13.0)

Added nuggets, ingots and placeable storage blocks for both **atium** and
**lerasium**, available in the mod creative tab. Conversions are reversible:
9 nuggets ↔ 1 ingot, and 9 ingots ↔ 1 block. Blocks require an iron-or-better
pickaxe and drop themselves. Both metals support filings (4/36/324 per form),
mixed vials, fifty reserve per filing, and iron/steel metal detection. Nugget,
ingot and block metal content remains 1/9/81 for projectile/recoil calculations.

Conversion recipes can be disabled globally or per metal under `godMetals` in
the metallurgy common config, followed by a recipe reload/restart. Items, placed
blocks and saved vial contents remain registered. Common ingot/nugget/storage
block tags support recipe compatibility. Current visuals reuse vanilla iron
for atium and gold for lerasium as temporary assets.

Natural acquisition/world generation and lerasium powers remain future work.
As of 0.14.0, existing atium IDs explicitly represent the atium–electrum alloy;
no pure-atium recipe, material, or effect is implied. No alloy ratio is invented.
The existing 32 ordinary Metallic Arts attributes remain; alloy Allomancy adds
one separate attribute. Current network protocol is 10; update both jars.


### Atium-alloy and electrum future sight (0.14.0)

Both work passively while burning, even when another power is selected. Atium
means the familiar **atium–electrum alloy**, including existing nuggets, ingots,
blocks, filings and reserve IDs. Old stacks/vials need no migration. There is
no pure-atium power or new alloy recipe; natural acquisition remains pending.

- **Atium:** silver-blue future silhouettes and paths for visible living entities,
  moving projectiles and dropped items. Threats are prioritized within the target
  budget. Likely projectile hits and aggressive mobs approaching melee range
  produce red warnings with ahead/behind/left/right labels, including threats
  outside the camera view. This is a mental aid, not automatic dodging or buffs.
- **Electrum:** gold silhouettes of your own projected movement, red for likely
  falls, lava/fire/contact hazards, projectile hits and possible nearby mob attacks.
  The attacker's identity/path is never sent for electrum alone. Hold its assigned
  G/H activation key while grounded to compare a hypothetical normal jump path.
  This previews an option without moving or jumping for you.
- An active atium or electrum burner causes another atium user's shadow of them
  to split into divergent purple possibilities. No precise central path or reliable
  hit warning is sent for that target. Possessing a dormant power is insufficient.

Use `/cosmere power grant @s a:atium` or `a:electrum`, then
`/cosmere power refill @s` and select a burn state. Atium uses persistent attribute
`cosmerearcanists:allomancy_atium`; electrum uses its existing attribute. The alloy
is deliberately outside the ordinary sixteen-metal origin/Feruchemy lists and is
currently granted by operators. No natural-origin probability silently changes.

Default horizon at attribute 1: Low 15, Burning 20, Flaring 25 ticks. Attribute
strength extends this modestly, capped at 40 ticks (two seconds). Baseline atium
range is 24 blocks, capped at 48. Updates are every 2 ticks, at most 12 targets
and 6 samples per shadow. Normal burn drains 4 atium or 1 electrum reserve/sec,
modified by shared burn consumption settings. Filings restore 50 as usual.

`cosmerearcanists-temporal-server.toml` controls each power, shadows, warnings,
interference, jump previews, projectile/attack forecasts, horizons, range, fuel,
update cadence and geometry budgets. Disabling preserves attributes/reserves.
Existing global/art/electrum gates apply; atium's per-metal gate is in this file.
Client `futureSight` controls paths, model silhouettes, warning HUD and opacity.

Forecasts extrapolate current movement and physics against loaded block collision
shapes. They cannot know future player inputs or fully simulate mob AI. Mob intent
warnings mean a possible attack, not a guaranteed scheduled strike. Jump previews
assume a normal .42 vertical launch, not every potion/mod/terrain combination.
No forced chunks, wall/invisibility detection, world slow-motion, extra damage,
or invulnerability. Models use the entity's current pose at future positions;
box outlines remain available if model rendering is disabled. Modded physics,
fluids, mounts and complex AI can diverge. Hands-on client/PvP rendering and
balance playtesting remain necessary. Both sides must update to protocol **8**.


### Bronze and copper (0.15.0)

Both work passively while burning, regardless of selected slot. Bronze senses
**active kinetic Investiture across Invested Arts**, not merely an attribute,
reserve inventory, stored metalmind, or an unimplemented power's selected state.
The eight implemented Allomantic powers now emit distinct rhythmic signals.
Other planetary arts are not implemented yet; a shared server API lets them and
compatible mods register providers or emit short-lived active-effect pulses.

Bronze gives private directional drumbeats and a pulse compass through walls.
Strength/burn state increase range and signal clarity. Normal strength can
identify ordinary Allomantic metals; harder foreign signals require more
strength. Low-strength unknown signals do not send their art/effect identity.
Hold bronze's assigned G/H activation key for up to three approximate distance,
height and identity readouts; stronger bronze can also recognize burn state.
No source UUID, player name, exact reserve count or entity outline is transmitted.
Unidentified source positions are coarsened to2-block cells; identified ones to
half-block cells. Rhythm and pitch values are provisional gameplay choices.

Copper creates an invisible field around the burner, hiding compatible signals
inside it. A Seeker standing inside a cloud also loses signals originating
outside it. Multiple Smokers' fields add strength; the same field counts only
once even when both listener and source are inside it. Intermediate clouds are
not treated as solid ray barriers. Sufficiently strong bronze can pierce weaker
clouds, receiving an attenuated signal. Copper does not disable others' powers,
block damage, hide bodies, or remove ordinary sounds.

Emotional protection applies to the **copper burner alone**, via the shared
`EmotionalInfluenceEvent.allowed(target,strength)` server integration point.
Extreme emotional strength may pierce it, configurable. Zinc/brass mechanics
are still pending; third-party emotional powers must call this API to respect
protection. No unrelated vanilla potion, AI or damage effects are canceled.

Default normal-burn reserve costs: bronze .5/sec, copper .25/sec, multiplied by
shared burn-consumption settings. Bronze range24, cap64; copper radius12, cap32;
each scales with square-root strength. Bronze must exceed combined copper
strength ×3 to pierce. Baseline copper blocks emotional strength up to4.
Ordinary effect identification threshold1; burn-state detail threshold2, divided
by the signal's detection difficulty. These are balance values, not lore ratios.

Configure `cosmerearcanists-seeking-server.toml` for global/per-power enablement,
fuel, detection, other arts, identification, held focus, copper masking, emotional
protection/piercing, ranges, thresholds, scan cadence and budgets. Defaults scan
once per4ticks, at most32 detected pulses and1024 collected emissions per
dimension. Disabling preserves attributes/reserves. Client `seeking` options
control pulse/copper HUD, private rhythms/volume and displayed pulse budget.
Per user request, pulses use Minecraft note-block sounds. Client `pulseInstrument`
selects BASS_DRUM (default), BASS, HARP, SNARE, HAT, BELL, CHIME or FLUTE;
effect-specific rhythms and pitches remain. No custom sound pack is required.

Integration API: `investiture.InvestitureSignals.register(id,provider)` reports
active signals on collection; `emit(level,signal,lifetimeTicks)` reports one-shot
use for1..100ticks. Signals carry art/effect IDs, stable source UUID, loaded
position, intensity, difficulty, period, optional burn state and copper-maskable
flag. A source/art/effect has one refreshed signal, not an accumulating trail.
Providers must not load chunks and should report only real kinetic activity;
external systems can opt out of copper masking when their lore requires it.

Use `/cosmere power grant @s a:bronze` or `a:copper`, then
`/cosmere power refill @s`; both reuse existing persistent attributes, filings
and mixed vials. Toggle their burn state in the existing menu. Registry and save
IDs are unchanged. Update client and server together: protocol **9**.

Automated checks cover active-only emissions, foreign providers, concealment,
piercing/stacking, private unknown identities, personal emotional protection,
fuel/disablement/respawn, pulse expiry/focus, budgets and packet round trips.
Actual positional audio, HUD appearance and multiplayer balance need manual
playtesting. Progression XP/savantism and other arts are still future work.


### Emotional Allomancy: zinc and brass (0.16.0)

Zinc intensifies and brass suppresses one of five emotions: **anger, fear, trust,
love, compliance**. Most mobs respond; animals receive half the influence by
default. The highest emotional level determines behavior, with stable enum order
breaking ties. Opposing Rioting/Soothing and multiple users combine rather than
stacking repeated packets from the same user into unlimited power.

Hold the assigned G/H activation key to influence the aimed target. Hold that key
and scroll to select an emotion; R/V menu scrolling retains priority. Hold Alt
before activation for bounded area influence. Selection is stored separately for
zinc/brass and survives respawn/relog. Burning drains1 reserve/sec at normal burn,
with existing low/flare multipliers; passive burning alone does not influence
others. Both powers produce bronze-detectable signals while burning.

- **Anger:** mobs attack nearby mobs, including passive animals using a small
  fallback attack. Players gain about25% melee attack damage per effective
  strength, capped75%.
- **Fear:** mobs flee the influencing user. Players receive a40tick speed burst
  on fear onset and30% lower melee attack damage and attack speed per strength.
  The burst cannot retrigger within100ticks; reductions cap90%.
- **Trust:** animals' avoidance of the influencing user and non-fire injury panic
  are suppressed; villagers' injury/avoidance memories are calmed. Trust lowers
  the influencing user's villager prices by15% per strength, max50%, at trading
  session start. Vanilla resets session prices afterward; base prices and gossip
  are not changed. An already open trading session keeps its quoted prices.
- **Love:** adult breeding-ready animals enter vanilla love mode; babies and
  cooldowns are respected. Willing love-influenced villagers receive mate intent,
  retaining vanilla food/bed checks. Influenced players deal less damage to the
  influencing user only:12.5% per strength means25% at baseline Flaring, cap75%.
- **Compliance:** mobs slowly follow the user. Players receive a weak horizontal
  additive pull every4ticks, .003 per strength capped.02, retaining their input
  and existing momentum. No teleporting or forced look direction.

Effect strength scales with persistent power attributes, burn level, distance and
animal multiplier. Natural baseline emotions and current combat intent seed the
state. Influence fades over80ticks after application; stronger new emotions can
replace the dominant behavior. Only the winning emotion applies its mechanics.

Copper's existing emotional-influence gateway can block actual zinc/brass use.
Aluminum-containing headwear (recursive recipe classification) and items in
`cosmerearcanists:emotion_shielding` also protect. Default entity-type tag
`cosmerearcanists:emotionally_immune` excludes dragon/wither/armor stands; datapacks
can change it. Disabling a power/effect removes its active influence/modifiers
while retaining player attributes, reserves and emotion selections.

`cosmerearcanists-emotions-server.toml` exposes power/entity/animal/area toggles,
per-emotion and breeding/trade toggles, drains, ranges, animal response, duration,
target/state budgets, combat rates, burst timings, discounts and compliance force.
Defaults range16sqrt(strength), cap32,16 targets/use,256 affected entities. Native
pathfinding is requested only inside a verified loaded region; custom mob AI
compatibility and terrain navigation still require playtesting. Client
`emotionControlsAndStatus` toggles the emotion selector/status HUD.

Test with `/cosmere power grant @s a:zinc`, `a:brass`, then
`/cosmere power refill @s`. Existing persistent attributes and mixed vials work;
no new registry IDs or save migration. Client and server need protocol **10**.
The private emotional status packet does not disclose an influencing player's
identity. Player emotional mechanics and species behavior are gameplay choices
requested for this mod; progression XP and Hemalurgic domination remain future.
