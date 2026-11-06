# Light targets (0.64.0)

| Spell | Effect |
| --- | --- |
| `Light()` or `Light(Skip)` | Existing temporary invisible light near caster gaze, default20seconds |
| `Light(Position)` | Permanent small visible light block at those coordinates |
| `Light(Entity)` | Glowing outline on that entity |
| `Light(Entity list)` | Glowing outlines on all valid unique targets |

Examples:

- `Light(Caster)` gives you Glowing.
- `Light(Gaze Entity())` outlines the entity you aim at, including an item, projectile or XP orb.
- `Light(Entities In(Sphere(Position Of(Caster), Number(3))))` outlines entities in that sphere.
- `Light(Position Of(Gaze Entity()))` places a stationary permanent light at the entity's sampled coordinates.
- `Light(Recall(Number(1)))` uses a saved position/entity/entity list according to its stored type.

Permanent lights are tiny cubes (2modelpixels per side), using the existing sea-lantern texture and real block light, default brightness12. AonDor strength/drawing quality scales brightness up to the configured level. They do not expire or follow entities. They survive normal world saves/chunk unloads, have no physical collision, and have a tiny outline so you can aim at and break them; they drop no item and can be replaced while building. They occupy an air block only, at the exact floored coordinates rather than trying adjacent blocks. An existing temporary light at that position may be upgraded; its old scheduled expiry cannot remove the new permanent light. An existing permanent light or solid block is not overwritten.

Permanent placement respects casting range, loaded chunks, world border, build/use-item/interaction checks and the NeoForge block-place event with snapshot rollback. No chunks are forced to load. Temporary no-argument lights retain their empty outline and existing expiration behavior.

Entity Glowing defaults to20seconds, configurable with `baseEffects.entityGlowingSeconds`, scaled by AonDor strength and drawing quality. It is the vanilla visible outline, not a moving source of world illumination. Living entities use the Glowing status effect and preserve a stronger/longer existing effect. Nonliving entities use the glowing flag with a saved server-time expiry; expiry is restored/checked when an entity loads, so unloading does not make the effect permanent. A preexisting nonliving glow is left untouched. As with any shared glowing flag, another mod changing the same flag during an owned timer cannot be separately identified.

The complete target list is range/type/availability checked before any entity receives Glowing; existing target caps apply and repeated references affect each entity once. Empty entity lists affect zero entities. Generic lists must contain only entity entries. Blocks require Position Of; raw vectors, numbers and blocks are not accepted as coordinates.

Protocol43 is required because the existing light block now has temporary/permanent state variants and permanent geometry; update both clients/server. Existing saved temporary lights default to temporary and keep their scheduled expiry. The block registry ID remains unchanged.

Dynamic Dor costs remain planned, with no new pool or deductions. Future pricing must distinguish temporary duration, permanent placement/brightness, and unique glowing targets/duration. Skip uses the ordinary temporary default and must not waive its cost.

These changes are local. User explicitly approved uploading the combined Heat/Skip/Light changes and running full Minecraft CI. Verification is in progress; main is unchanged.
