# Lists, sets and conditional values (0.62.0)

[Lists and sets stroke guide](aondor-list-guide.svg) · [Logic stroke guide](aondor-logic-guide.svg). These thirteen argument Aons use the existing drawing/palette controls. No new effect Aons are added. Draw direct arguments clockwise from twelve o’clock; List/Append/set operations accept additional children in that same order.

| Aon | Arguments | Output |
| --- | --- | --- |
| List | Zero or more Values | Ordered list, including an empty list |
| Append | List, then one or more Values | New list with those entries appended |
| Remove At | List, Number index | New list with exactly that entry removed |
| At | List, Number index | Value at that index, retaining its type |
| Size | List | Number of entries |
| Intersection | Two or more Lists | Unique entries present in every input |
| Union | Two or more Lists | All unique entries from the inputs |
| Difference | Two or more Lists | Unique entries in the first input absent from every later input |
| Contains | List, Value | Boolean membership result |
| If | Boolean condition, Value when true, Value when false | Selected value |
| And | Two or more Booleans | True when all are true |
| Or | Two or more Booleans | True when at least one is true |
| Not | Boolean | Opposite Boolean |

Indices start at **0**. Indexing/removal requires a finite whole number within the list; negative, fractional or out-of-range indices fail. At on an empty list fails. List creation, Append and Remove At preserve duplicates and do not mutate their inputs. Appending a list adds it as one nested entry, rather than flattening it.

Set operations remove duplicates. Union preserves first encounter order across all inputs; Intersection/Difference retain the first input’s order. Equality is typed: a Position and Vector with the same components are different values; an entity reference denotes that entity. Nested list equality includes entry types and order. Numeric positive/negative zero compare equally. Contains does not coerce numbers to Booleans. Contains produces Boolean values for If/And/Or/Not; no separate true/false glyph is required.

If evaluates only the chosen value branch. And stops at the first false input; Or stops at the first true input. An unused Recall or selector is not evaluated. All drawn glyphs must still be recognized and structurally valid; compile-time-invalid expressions such as a literal division by zero still reject the drawing even in an unused branch. These modifiers select values; they do not execute nested effect spells.

Lists may contain any current value: numbers, Booleans, positions, vectors/directions, blocks, entities, regions and other lists. Entity zone results are accepted by all list/set operations. A generic list containing only entities can feed Players/Mobs/Exclude/Nearest, Force or Heat; a nonentity entry in an entity context rejects before effects. Repeated entity entries cannot apply Force more than once to the same target. Target-list size limits are checked before that effect deduplication.

Generic lists and Booleans work with temporary Remember/Recall and copperminds, retaining types. Their normal expiry/copper mode/Identity/capacity rules still apply. No arbitrary item/entity NBT or saved spell execution is added.

Examples (expression notation explains the drawing hierarchy):

- Create and remember a list: `Remember(Number(1), List(Caster, Gaze Entity()))`.
- Append your current location: `Remember(Number(1), Append(Recall(Number(1)), Position Of(Caster)))`.
- Remove the second entry: `Remember(Number(1), Remove At(Recall(Number(1)), Number(1)))`.
- Read the first entry: `At(Recall(Number(1)), Number(0))`.
- Three-way intersection: `Remember(Number(0), Intersection(List(Number(1), Number(2)), List(Number(2), Number(3)), List(Number(2), Number(4))))` stores `[2]`.
- Conditional output: `Remember(Number(2), If(Contains(Recall(Number(1)), Caster), Number(7), Number(0)))` stores 7 when the list includes you, otherwise 0.
- Use a numeric list entry as a radius: `Sphere(Position Of(Caster), At(Recall(Number(3)), Number(0)))`.

Each list allows at most128 entries; nested values have a512-value budget and nesting depth8. Copper also has its existing byte/charge/entry limits. The full drawing remains limited to17glyphs/3000points; variadic operations accept at most16arguments, and many practical drawings reach the overall glyph limit first. Entity-query and effect caps remain separate (default32targets,8blocks), including initial unfiltered queries before later set operations. No loops, maps, strings, predicate-function filters, numeric comparisons or inscription system in this slice.

Published to main with explicit user approval. 3336production checks and both Java21 builds/JUnit/all393required server tests with and without Curios passed, including standalone graphical client startup in [CI42269207658](https://github.com/Exlor64/Cosmere-Arcanists/actions/runs/42269207658). Hands-on drawing and multiplayer controls remain to be tested.
