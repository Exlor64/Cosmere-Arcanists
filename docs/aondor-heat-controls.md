# Heat power and optional arguments (0.63.0)

Heat accepts `Heat([Entity / Entity list / Block], [Number power = 1])`. Both slots are optional. Arguments are still read clockwise from twelve o’clock. Use the new **Skip** argument glyph to leave an earlier optional slot at its default while supplying a later one: `Heat(Skip, Number(8))` uses normal gaze/block targeting at power8. Omit or Skip the final argument for power1. Skip does not occupy a real value slot in memory/lists.

| Effective power | Enabled behavior at default settings |
| --- | --- |
| 0 | Old thawing/melting/campfire warming only |
| 1+ | Ignite living entities and create real fire next to a targeted block |
| 2+ | Also deal direct fire-tagged damage to living entities |
| 4+ | Cook dropped food stacks using furnace recipes |
| 8+ | Smelt other dropped item stacks using furnace recipes |

Effective power is the numeric argument multiplied by AonDor strength and drawing quality. Fractional values work. Negative/nonfinite powers reject before effects. Default power and damage balance caps are disabled (config0); Minecraft float numeric overflow still rejects before effects. Higher power gives longer ignition (2seconds per effective power, capped30seconds) and more direct damage (1damage per effective power from the threshold, continuing above8 without a default40damage ceiling). Administrators may opt into positive `maximumEffectivePower`/`maximumDirectDamage` caps;0means no balance cap. All new behaviors and thresholds/caps are configurable in the AonDor server config `heat` section. Shape-only drawing may fall below a threshold; e.g. power1 at quality.75 still thaws but does not ignite.

Thawing remains140frozen ticks times strength/quality. Heat still melts one snow layer or ordinary/frosted ice and lights dry campfires. Block fire follows normal Minecraft behavior, including fire spread and gamerules. Build permission, interaction checks, loaded chunks, world border and a cancellable NeoForge block-place event apply to new ignition. It places at most one nearby fire per targeted block. Real fire can cause its normal later effects.

Direct damage uses a vanilla fire damage type attributed to the caster, preserving fire resistance/immunity and other fire-damage hooks. Team/PvP restrictions prevent hostile entity ignition and damage; explicit self-targeting is possible. Nonliving entities other than recipe-compatible dropped items have no new heating effect. Generic item/zone selectors and recalled entity references all work.

Dropped items are **transformed, not ignited**. Cooking requires an edible recipe output (including dried kelp); other valid furnace transformations require the smelting threshold. The entire input stack is transformed once, preserving the item entity’s UUID, motion, pickup/ownership metadata and counts; existing fire is cleared. Input/output limits and all target recipes are checked before any target changes. Default maximum input64items per target; output must fit one legal stack, otherwise split the input before casting. Non-recipe items are unchanged. Filed materials, bound metalminds and charged/marked spikes cannot be processed. No furnace XP is generated. Heat never repeatedly smelts its own output in a single cast.

Examples:

- `Heat(Skip, Number(2))`: default aimed target, with ignition/direct fire damage.
- `Heat(Caster, Number(0))`: thaw yourself without ignition or damage.
- `Heat(Gaze Block(), Number(1))`: ignite near the targeted block.
- `Heat(Gaze Entity(), Number(4))`: cook an aimed dropped food stack.
- `Heat(Recall(Number(1)), Number(8))`: heat a saved entity or entity list at smelting power.

## Skip

Skip has no arguments and is accepted only in slots with a defined default:

| Slot | Default |
| --- | --- |
| Heat target / power | Normal gaze/block targeting / power1 |
| Force target / direction | Normal gaze targeting / caster look direction |
| Light position | Standalone light placement near caster gaze |
| Look Direction entity | Caster |
| Nearest reference position | Caster feet |
| Remember key / value | Key0 / Caster |
| Recall key | Key0 |

Critical arguments (Sphere center/radius, Position Of input, math operands, list indices, Boolean conditions etc.) reject Skip. List/Append/If value entries also reject it; it is an omission marker, not a stored value. An optional argument at the end can simply be left undrawn. Empty targets and missing references still report errors rather than silently selecting a new target.

## Dynamic cost requirement (planned)

All Aon actions and functional modifiers must have costs based on the resolved work: effective power/magnitude, actual unique targets or blocks, duration, range/volume when relevant, and every cooked/smelted item, not just one price per glyph or stack. Skip resolves its ordinary default before pricing; it must not make defaults free. Power0 still has a cost when it performs actual thawing/melting. Heat prepares its entire list of target/recipe work before executing it, retaining processed unit counts so a future cost quote/payment can run before mutation.

Dor source/storage, conversion rates, payment/refund rules and cross-world Connection rules have not been selected. No resource pool, actual deductions or cost formula is introduced in this release. Preserve this requirement for Force, Light, memory, future effect Aons and further functional modifiers as the economy is implemented.

Light now supports entity Glowing and permanent coordinate lights: [Light controls](aondor-light-controls.md).
