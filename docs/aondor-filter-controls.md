# Targeting filters (0.59.0)

[Stroke guide](aondor-filter-guide.svg). Draw these outside their parent, or use Draw Argument / Alt-click to select the parent. Direct children are arguments clockwise from twelve o’clock. Each glyph remains subject to normal recognition and size tolerance.

| Aon | Arguments | Result |
| --- | --- | --- |
| Exclude | Entity list, then Entity or Entity list | New list with matching entities removed |
| Players | Entity list | Only players |
| Mobs | Entity list | Only Minecraft mobs, including animals; excludes players and armor stands |
| Nearest | Entity list, optional Position | One entity closest to that position; defaults to caster feet |

Unfiltered lists include all entity types: mobs, players, dropped items, projectiles, XP orbs, vehicles and more. Removed/dead entities and spectators are unavailable. Lists retain their original order. Nearest breaks equal-distance ties by entity ID. Empty lists remain empty through filters; Nearest on an empty list fails without casting a default effect. The server evaluates all arguments before applying effects. Existing range, loaded-terrain and maximum-target limits remain: filtering does not allow an oversized initial Sphere query.

Examples use expression notation to explain the drawing hierarchy:

- Warm nearby entities except yourself: `Heat(Exclude(Entities In(Sphere(Position Of(Caster), Number(3))), Caster))`.
- Warm only nearby mobs: `Heat(Mobs(Entities In(Sphere(Position Of(Caster), Number(3)))))`.
- Push the closest nearby mob: `Force(Nearest(Mobs(Entities In(Sphere(Position Of(Caster), Number(3))))))`.
- Select players with `Players(Entities In(...))`, or remove that list using `Exclude(Entities In(...), Players(Entities In(...)))`.

The full drawing still has a 17-glyph budget. Keep duplicated selections small. No saved variables or new effect Aons are introduced.
