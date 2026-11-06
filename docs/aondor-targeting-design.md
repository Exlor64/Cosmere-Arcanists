# AonDor targeting language — proposed next phase

Status: the initial eight operations and grouping are implemented for0.57.0-alpha; broader vocabulary below remains roadmap. See [current controls](aondor-targeting-controls.md).
User reports the current effects and drawing controls work perfectly in-game.

The user requests targeting modifiers with Hex Casting-like composition and freedom.
Keep our clockwise, typed expression language, rather than copying a stack UI or claiming
feature parity. Every operation has typed inputs/outputs; computed results can fill
other operations' arguments. Missing main-effect arguments retain standalone defaults.

## Proposed vocabulary

| Family | Proposed operations | Values |
|---|---|---|
| Caster and gaze | Caster; Gaze Entity; Gaze Block; Gaze Hit Position; Gaze Face Normal | Entity, Block reference, Position, Direction |
| Spatial data | Position Of; Eye Position Of; Look Direction Of; Velocity Of; Coordinates | Position, Vector |
| Derived points | Offset; Point Along Ray; Midpoint; Between; Local To World | Position, Vector |
| Ray selection | First Entity On Ray; First Block On Ray; Ray Hit Position; Entities Along Ray | Entity/Block/Position, lists |
| Areas | Sphere; Box; Cylinder; Cone; Line/Capsule | Region |
| Region queries | Entities In; Blocks In | List<Entity>, List<Block> |
| Filters | Living; Players; Items; Type/Tag; Visible; Within Distance; Include/Exclude Caster; Predicate | Filtered list |
| Relations and choice | Nearest; Farthest; Closest To Ray; Nth; Random; Union; Intersection; Difference | Single value or list |
| Reuse | Remember/Read Value; Current Target in iteration | Typed stored/current value |

Names above are proposals, not canonical Aon names or shipped glyphs. Parameters such as
area radius use number expressions, not dozens of dedicated 'large/small' symbols.
Regions are geometry, not entity lists: the same sphere can query entities or blocks.
Position, direction and velocity are semantically distinct even when each contains3numbers.
Entity position conversion is explicit, avoiding silent changes between moving targets
and sampled points. Empty selections/type mismatches must be visible before/at cast.

## First implementation slice proposed

1. Caster -> Entity.
2. Gaze Entity -> Entity (or no target).
3. Gaze Block -> Block reference (or no target).
4. Position Of(Entity or Block) -> Position.
5. Look Direction Of(Entity), default caster -> Direction.
6. Number -> Number: diamond + center circle for0; surrounding circles add1 up to10. Larger values need future math.
7. Sphere(Position, radius) -> Region.
8. Entities In(Region) -> List<Entity>.

This is an incremental starting vocabulary, not a cap on the language. Blocks In,
filters, vectors/math, other regions/rays, lists, variables and control flow follow.
The eight initial templates are implemented; further art should be previewed in small batches. First art previews should remain small,
consistent with existing angular/circle style, restrained curves, distinct silhouettes,
precise-stroke recognition plus weaker shape fallback and catalog-wide confusion checks.

## Drawing and expression ownership

Proposed UI: select the main/parent Aon, choose Add Argument, then draw or place a
learned glyph in its own child region. Once recognized it can receive children too.
Each parent reads its direct children clockwise; nested children are evaluated before
being passed to the parent. Show accepted signatures, numbered argument links, missing
inputs and actual types. Child strokes must never enter the main glyph's recognizer.
Recognition stays editable: no manual Lock step. Ctrl+Z, palette sizing, white grid,
in-air ink and release-to-cast remain. Argument creation/selection should be explicit
rather than guessing where an arbitrary new stroke belongs. Starting angle12o'clock
remains provisional; this document does not resolve it.

Example: Force(Entities In(Sphere(Position Of(Caster), Number(5))), Look Direction Of(Caster)).
Later filtering can exclude caster/players, select approaching entities and calculate
an individual outward direction, without a bespoke 'repel approaching mobs' spell.

## Effect integration and runtime boundaries

Proposed target signatures: Light(Position), Force(Entity or List<Entity>, Direction),
Heat(Entity or Block or corresponding list). These extend existing defaults; actual
arity/optional ordering and light-following/persistent semantics still need implementation.
Explicit Force self-targeting should be supported as language functionality, rather than
letting the current default gaze/self exclusion become a permanent language restriction.
No new cross-world Connection rules are introduced. World queries must remain loaded,
server-authoritative and bounded; per-target validation/costs and evaluation budgets must
scale with work, without arbitrary forced chunk generation. No Dor economy chosen yet.
Do not silently treat a missing required explicit input as the default, or cast only a
partial type-invalid expression. Numeric/vector operations must preserve their values;
drawing quality affects effect potency/efficiency, not silently altered coordinates.
