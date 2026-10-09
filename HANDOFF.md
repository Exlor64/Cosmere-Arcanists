# Cosmere Arcanists handoff — 0.3.1

Minecraft 1.21.1, NeoForge 21.1.256, Java 21, author Ex. Repository: Exlor64/Cosmere-Arcanists. Overworld is a separate planet. Scadrial will be Era 2 with an Overworld-like environment. Plan includes all three Metallic Arts, Cognitive Realm travel, perpendicularities, planetary origins/races, and configurable major features.

## Implemented
- Command-based human origins; RANDOM default, CHOICE, basic ANCESTRY multiplier; persistent natural power identifiers and configurable assignment gates.
- Eight added mined metals: tin, zinc, aluminum, chromium, cadmium, lead, silver, nickel. Vanilla iron/copper/gold reused. All eight ordinary alloys, raw processing, ingots/nuggets, stone/deepslate ores, creative tab, generation/alloy recipe gates.
- Iron/steel Allomancy: private chest-to-metal blue lines, equipment endpoints, aimed push/pull, anchored-block player movement, mass-dependent entity movement, reaction forces, flaring and reserves.
- Rebindable R hold-scroll power carousel, release to select, tap or click to cycle state; V hold-scroll state selector; G is Activate ability, dispatched through START/TICK/STOP handlers for the selected power. Selected-power ingot icon/state/reserve HUD. Allomancy states Off/Burning/Flaring; Feruchemy selector has signed storing/tapping levels but **no Feruchemical effects yet**. Other powers explicitly show effects pending.
- Iron/steel vials: glass bottle + nugget, consume vial to fill reserves, return bottle. Operator grant/revoke/refill commands documented in README.

## Agreed metallurgy/detection rules
Coal supplies steel's carbon. Bronze contains only copper/tin. Bendalloy contains only lead/tin/cadmium, no bismuth. Nicrosil contains only nickel/chromium, no silicon or magnesium. Alloy batch quantities are provisional, not canonical percentage claims.

Any aluminum content excludes an item, including descendants through recipe ingredients; aluminum takes precedence over detectable-metal classification. Common ingot/nugget tag groups identify metal ore groups, avoiding coal/diamond ores as seeds. Reverse closure covers crafting/smithing/stonecutting and handles cycles. Furnace recycling does not propagate alloy ingredients into purified metals. Explicit tags: cosmerearcanists:metals, aluminum_containing, undetectable. Dynamic/NBT-dependent recipes may need tags. Rebuild detection on datapack reload.

## Architecture/constraints
Common code never references client classes. PowerNetwork validates serverbound controls against the player's available powers; client sends no target positions or force amounts. Targets encode entity/slot or block position; clients interpolate endpoints. Block scans are budgeted and never load chunks. Session caches clear on logout, dimensions, respawn/reload/server stop. Death preserves origin, grants, reserves, selection but resets states. Switching selected power does not stop other burning metals.

PowerData stores per-metal states/reserves and operator grants in namespaced persistent NBT; future Hemalurgic grants must remain distinct from natural powers. Origin schema is 1. Preserve unknown identifiers and disabled-feature data. Feature gates apply at use time.

Configuration: origin/per-art/per-metal SERVER config; powers SERVER config cosmerearcanists-powers-server.toml; metallurgy COMMON config; client HUD/line config. Configs are additive; no registered content disappears when generation/crafting is disabled. World generation distribution and recipe quantities also have datapack overrides.

Access transformer exposes ServerGamePacketListenerImpl.aboveGroundTickCount; reset only following server-validated force acceleration to prevent vanilla floating kicks without granting creative flight. Fall mitigation is configurable and only applies when an upward iron/steel impulse actually slows a descending entity. Accumulated fall distance scales by squared downward-speed ratio; stopping ascent/descent resets past damage but later unbraked falling remains dangerous. Velocity cap no longer instantly clamps an existing fast fall.

## Verification and next work
GitHub CI runs build, eleven JUnit tests (recipe graph/aluminum/cycles, states, mass/distance/flaring), and two GameTest server tests (real recipe/tag detection and player clone persistence). Compilation, all eleven JUnit tests, and both GameTest server tests passed on commit 3bfb0066326ee52b81e2a9f2f10d059a97be37c4. Local Gradle downloads remain blocked by network restrictions. Client visual/input/multiplayer playtesting has not been performed.

Next: client playtest iron/steel and selector, adjust force/targeting/line visibility based on feedback, then define further Metallic Arts. Origin GUI, expanded ancestry/races, Scadrial dimension, travel, other Allomancy, all Feruchemy effects, Hemalurgy, and Compounding are not implemented.

0.3.1 verified in CI: generic AbilityDispatcher routes independently of iron/steel sensing state and handles lifecycle cleanup; future instant powers should respond to START only. Existing key identifier preserved. New tests cover generic activation phases and appropriate fall braking.

Server player acceleration uses observed position displacement on the first impulse per tick, preserving real falling momentum; subsequent impulses accumulate. Zero samples fall back to existing velocity and teleport-sized samples are ignored.
