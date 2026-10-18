# Cosmere Arcanists — project handoff

Last updated: **2026-10-18**. Current mod version: **0.7.1-alpha**.
Repository: [Exlor64/Cosmere-Arcanists](https://github.com/Exlor64/Cosmere-Arcanists).
Platform: **Minecraft 1.21.1 / NeoForge 21.1.256 / Java 21 / Gradle 9.2.1**.
Author: **Ex**. Package/mod ID: `dev.cosmerearcanists` / `cosmerearcanists`.

Read this and `AGENTS.md` when beginning a session. The user requires this file to be updated after every change or project chat. Keep it current rather than appending conflicting summaries.

## Latest session and next action

- User reports the projectile concept works, but clarified it must fire only while **actively pushing with steel**, not merely burning steel.
- 0.7.1 adds a server-side held-input gate: steel selected, burning, fresh G-held heartbeat. Releasing G, selecting another power, brief taps and stale input prevent launches without spending fuel or replacing ordinary drops. Already-fired projectiles continue in flight.
- 0.7.1 build, 27 JUnit methods, 13 server GameTests and artifact upload passed in CI (evidence below).
- Next: collect G+Q versus ordinary Q playtest feedback. Do not restore automatic fire on burning alone.

## Vision and accepted constraints

- A large Brandon Sanderson Cosmere mod: planets, perpendicularities and Cognitive Realm travel, planetary powers, magical physics, origins/powers/races.
- Vanilla Overworld is a separate planet. Start with **Scadrial, Era 2**, with an Overworld-like environment, and its three Metallic Arts.
- Allomancy, Feruchemy, Hemalurgy and later Compounding are intended. Most of these are not yet implemented: see scope below.
- Default origins assign powers randomly. Current implemented modes are RANDOM, CHOICE, and a provisional ANCESTRY probability multiplier. No GUI yet.
- Major features should be disableable/tunable through configuration; preserve registered content and player data when disabled.
- Powers must be **persistent player attributes**. Do not introduce single-power tag exclusivity from the user's unrelated mods: multiple powers are supported here.

## Implemented scope

- Command-based origins for Scadrial and separate Overworld; human race only. Assignment probabilities, Twinborn, rerolls and per-art/per-metal gates are configurable.
- Metal ores, processing, ingots/nuggets and alloys; ore generation, creative tab, and recipe/generation gates.
- Allomantic **iron and steel effects**, detection lines, aimed push/pull, movement assistance, metal reserves/vials, fall braking, and steel-thrown projectiles.
- 16 Allomantic plus 16 Feruchemical attributes and selector entries. Other Allomantic effects are pending. Feruchemy has selector states only, no storing/tapping effects.
- **Not implemented:** Scadrial dimension, other planets/dimensions, Cognitive Realm/perpendicularities/travel, origin GUI, expanded races/ancestry, other Allomancy effects, Feruchemy mechanics/metalminds, Hemalurgy/spikes, Compounding.

## Metallurgy and detection decisions

- Added mined metals: tin, zinc, aluminum, chromium, cadmium, lead, silver, nickel; vanilla iron/copper/gold reused.
- Alloys: steel, pewter, brass, bronze, duralumin, electrum, bendalloy, nicrosil.
- Use **Cosmere metallurgy**, not real-world alloy substitutions. Coal supplies steel's carbon. Bronze: copper + tin only. Bendalloy: lead + tin + cadmium, no bismuth. Nicrosil: nickel + chromium, no silicon/magnesium.
- Alloy batch quantities are provisional balance choices, not canonical percentage claims.
- Detect metal-containing blocks/items recursively through crafting, smithing and stonecutting. Any aluminum content excludes the entire item, even if it contains other metals. Separately detectable equipment on an entity still works.
- Furnace recycling does not propagate alloy ingredients back into purified metals. Common ingot/nugget tag groups identify metal ore seeds without treating coal/diamond ore as metal.
- Custom item tags: `cosmerearcanists:metals`, `cosmerearcanists:aluminum_containing`, `cosmerearcanists:undetectable`. NBT-dependent/dynamic recipes can need explicit tags. Datapack reload rebuilds classification and projectile metal amounts.

## Persistent powers, commands and save migrations

- Attributes: `cosmerearcanists:allomancy_<metal>` and `cosmerearcanists:feruchemy_<metal>` for the 16 ordinary metals. Default 0; positive effective values grant access; 1 is baseline, maximum 1024. Config gates still apply.
- Effective attributes include modifiers; iron/steel force scales with effective strength. Base values persist through save/relog. Respawn copies base values and permanent modifiers, clears temporary effects, and resets activation states while retaining reserves/selection.
- Origin/grant NBT migrates once to attributes; legacy grant booleans are no longer the access source. Setting an attribute to zero must not be undone by origin metadata.
- `PowerData.KEY = cosmerearcanists_powers`: states, reserves, selected power, migration markers. Origin metadata remains under `cosmerearcanists_origin` (schema 1).
- `burn_levels_schema = 1` migrates old active states 1/2 to current 2/3 once; Off and Feruchemy states retain their meanings.
- `/attribute @s cosmerearcanists:allomancy_steel base set 1` grants baseline steel. Replace `steel` with `iron` as needed; 0 removes the base power.
- `/cosmere power grant <player> a:steel` sets at least baseline strength. `revoke` sets the base to zero, including a natural origin power; a positive modifier can still grant access. `/cosmere power refill <player>` refills iron/steel reserves.
- Grant/revoke use `ResourceLocationArgument`, accepting IDs such as `a:steel` and `f:iron` without the old trailing-data parser error.
- `/cosmere origin <scadrial|overworld>` in random/ancestry modes; CHOICE adds `<allomancy metal or none> <feruchemy metal or none>`. `/cosmere reset <player>` clears origin, powers data, and power attributes/modifiers. Rerolls replace previous origin base powers while retaining other attribute powers.

## Controls and burn levels

- **R:** hold and scroll to select a power; release selects. Short tap cycles state; clicking within selection also cycles it. HUD shows selected ingot icon/name/state/reserve.
- **V:** hold and scroll to select state; release applies. Tap cycles states.
- **G:** generic **Activate ability**, routed to the selected power through START/TICK/STOP handlers.
- **Left Alt:** hold **before** pressing G to latch the alternate mode for that activation. Release G before changing modes. All keys are rebindable.
- Allomancy states: **0 Off, 1 Low burn, 2 Burning, 3 Flaring**. Default force multipliers: 0 / 0.5 / 1 / 2. Consumption multipliers: 0 / 0.5 / 1 / 3. Disabling flaring falls back to Burning.
- Feruchemy: zero Off, negative Storing levels, positive Tapping levels (default 1–5); no effects yet.
- Iron baseline multiplier is **2** on top of the global force, attributes and burn level. At Burning, attribute 1 equals pre-0.6.0 iron attribute 2. Low burn intentionally gives a weaker pull. Steel's baseline was not increased.
- Glass bottle + iron/steel nugget crafts its vial. Drinking adds 60 reserve units by default (cap 300), returns the bottle. Normal Burning costs 1 unit/second. Sensing also consumes reserves. Switching selection does not turn off other burning metals.

## Iron/steel targeting, movement and lines

- Private blue lines originate at interpolated chest position and end at metal blocks, dropped items and individual equipped/held metal sources.
- Rendering uses the event's **world pose stack** with camera-relative vertices (subtract camera in doubles before floats), fixing north-offset/inverted rotation bugs. Do not restore the old independent matrix transform.
- Server targeting prefers the actual metal block hit by the crosshair, including edges; otherwise it uses a cone. Force follows the physical chest-to-anchor vector, not an invented camera-aligned direction.
- Steel pushes away, iron pulls toward. Fixed blocks anchor the player; movable targets receive mass-dependent opposite force and optional player reaction.
- Alt+G uses WASD relative to yaw, Space upward, Shift downward. Steel needs metal opposite the requested movement; iron needs metal toward it (overhead metal for upward pull).
- Assistance uses nonnegative, direction-limited real-anchor contributions with one force budget. Default anchor acceptance 60 degrees; oblique anchors are weaker, and net force >45 degrees off intention is rejected. No viable metal/input means no assisted force. Equipment on one entity is one physical anchor.
- **Player motion:** the server sends an additive impulse; client adds it to its actual velocity. Never replace client velocity with measured walking displacement: earlier attempts caused vertical-only movement or horizontal feedback/amplification. Server observed displacement is still used to estimate fall braking.
- While an impulse is recent, vanilla walking input opposing its horizontal force is filtered, retaining perpendicular strafing and real inertia. Config: `preventWalkingAgainstForce`. Real momentum takes time to reverse.
- Upward force reduces accumulated fall distance only when it actually slows descent, using a squared downward-speed ratio. Later unbraked falls still hurt; there is no flat immunity. Speed caps do not instantly arrest an existing fast fall.
- Access transformer exposes `ServerGamePacketListenerImpl.aboveGroundTickCount`; reset only after a validated applied force, not as a flight permission.

## Detection latency and resistance

- Background near-first spherical scanning publishes discoveries immediately and does not restart just because the player walks a few blocks. Loaded chunks only; no forced loading.
- Every active tick scans the current 6-block neighborhood. Falling/fast movement into a new block, or beginning activation, adds an urgent near-first scan (default 32768 checks: covers the full default 16-block sphere).
- Target-list changes sync immediately, independent of older saved sync intervals. Periodic state/reserve updates default to every tick. Entity endpoints interpolate client-side.
- Solid intervening voxels add hardness-based detection distance: default 0.25 per hardness, cap 50 per blocker (also unbreakable-block fallback). Player/target voxels, air and noncolliding blocks are excluded. One obsidian adds 12.5 distance units. Applies to displayed and usable targets, including gear/items.
- Limits remain: network latency/tick rate, loaded chunks, configured scan budget/range/target count, and hardness resistance. A larger configured range may exceed the default urgent budget.

## Steel-thrown projectiles — 0.7.0

- Hold **G** with steel **selected and burning**, then throw with **Q**. Burning alone is insufficient. Server checks a fresh held-input heartbeat (up to 15 ticks); release, other selection, brief released taps, expired input, disabled/unavailable/unfueled steel, nonmetal and aluminum leave normal drops. Inventory drops only qualify if a valid held activation still exists; GUI opening normally releases it. Already-fired projectiles do not require continued G holding.
- One thrown stack becomes one `SteelProjectile`; it cannot merge in flight. The exact stack/components/count are stored separately from the rendered single item and persist through save/load.
- `MetalAmounts` / `RecipeMetalAmounts` count nugget-equivalent content: nugget 1, ingot 9, iron block 81, anvil 279. Repeated ingredients and output counts matter; nonmetal ingredients add zero. Ingredient/recipe alternatives use the minimum resolvable amount.
- Primitive ore/raw items use 9 units as a balance approximation; unresolved detectable items default to 9 units per item (configurable). Multiply by thrown stack count. This is not a real-world mass/metallurgy claim.
- At baseline steel Burning: one nugget launches at 3.5 blocks/tick and deals about **2 raw damage**; armor and hurt immunity still apply. Power is effective steel force divided by 7, so attributes, force config and burn levels scale it.
- Speed = `min(maxSpeed, nuggetSpeed * sqrt(power) / metalUnits^0.25)`. Default max speed 8. A baseline anvil launches about 0.86 blocks/tick.
- Recoil = `metalUnits * nuggetMass * speed / playerMass` opposite aim; default nugget mass 0.2. Anvils kick much harder than nuggets. Existing reaction-force toggle applies.
- Damage = `cap * -expm1(log1p(-nuggetDamage/cap) * energy)` where `energy = metalUnits * (impactSpeed/nuggetSpeed)^2`. Calibrated at 2 per baseline nugget; smooth saturation approaches **100**, with a hard raw damage ceiling of 100 regardless of metal/power. Config can lower the cap.
- Vanilla swept projectile collision, gravity and drag; bounded knockback. Respect player-target/team/vanilla damage rules. No terrain destruction or piercing. Recover original stack at first impact, or after 100 flight ticks (default), with normal pickup delay. No automatic relaunch of recovered drops.

## Configuration and architecture map

- Origin/per-art/per-metal SERVER config: `ServerConfig`. Powers SERVER config: `cosmerearcanists-powers-server.toml` (world/server config location follows NeoForge). Metallurgy COMMON config: `MetallurgyConfig`. Client HUD/line config: `PowerClientConfig`.
- `ironSteel`: force, iron multiplier, burn strength/drain, speed/mass, reaction/fall braking, player targets, separate iron/steel alternate toggles, angle limit, input filtering, reserves.
- `metalDetection`: categories, recursive recipes, hardness cost/cap, range-related scanning budgets, target count and sync cadence.
- `steelProjectiles`: enabled, reference/max speed, nugget damage, cap ≤100, recoil mass, unknown content fallback, per-shot reserve cost (default 1), lifetime.
- Existing config files retain saved values. New fields obtain defaults. Datapacks can override generation and recipes. Feature disablement must not delete saved data or registry entries.
- `PowerService`: validated inputs, ticks/sessions, dispatcher, target selection, forces, throw event. `PowerAttributes`/`PowerData`: powers/persistence. `PowerNetwork`: controls, snapshots, additive impulse; **current protocol 3**. Update both server and client, especially for new projectile registration/renderer.
- `PowerClient`/`PowerClientMod`: input, HUD, lines, impulse reception, renderer. Common classes must not reference client classes. Client supplies no target positions or force magnitudes.
- `MetalDetector`/`MetalGraph`: classification/aluminum closure. `MetalAmounts`/`RecipeMetalAmounts`: quantities. `MetalProjectilePhysics`: pure scaling formulas. `MetalProjectiles`: registration/launch. `SteelProjectile`: collision, damage, persistence and recovery.
- Session state clears on logout, dimension change, respawn, datapack reload and server stop. Keep migrations idempotent and configuration gates server-authoritative.

## Verification, evidence and limits

- Latest gameplay commit validated: `944eadea0747c85d2ee70bef4b03e63c7d9e7c5a`.
- [Successful GitHub Actions checks 39149645187](https://github.com/Exlor64/Cosmere-Arcanists/actions/runs/39149645187): build, **27 JUnit methods**, **13 server GameTests**, and artifact upload passed. Counts are source-method counts at this version.
- Tests cover metal/aluminum/cycles, ingredient quantities, power states/migrations/attributes/cloning, ability phases, scanning/falling discovery, hardness, impulse/input math, movement solver, burn/iron scaling, projectile scaling/cap, actual launch/fuel/recoil, save/load/recovery, and swept mob collision with ~2-damage nugget.
- Collision-test pitfall: the small empty template is encased by GameTest barriers. Clear an airborne corridor before projectile tests; otherwise the shot can hit the barrier roof instead of its target. Temporary collision diagnostic logs were removed.
- User reported steel movement working perfectly and iron working well before requesting its doubled baseline and alternate mode. Do not treat those later additions or projectile visuals as user-confirmed.
- User reports the projectile concept works in game; this does not independently validate every projectile behavior or balance setting. No manual client/multiplayer visual playtest has been performed by the assistant. Projectile appearance, movement feel/balance, real network latency, and modded/dynamic recipe edge cases still need playtesting.
- Java **21** required. Commands: `./gradlew build --no-daemon`, `./gradlew runGameTestServer --no-daemon`; on Windows use `gradlew.bat`. CI installs Java 21 and uploads `build/libs/*.jar` as `Cosmere-Arcanists`. In this session local builds were unavailable because of local runtime/download constraints; CI is the verification evidence.
- Passed 0.7.1 checks add regression coverage for burning without G, iron selected, pulse-only input, G release, stale heartbeat, fuel preservation and valid held steel launch.
