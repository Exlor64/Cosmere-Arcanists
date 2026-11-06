# Aon memory (0.60.0)

[Memory stroke guide](aondor-memory-guide.svg). Memory is a pair of distinct glyphs: Remember is a central effect; Recall is an argument. All existing targeting filters and scalar/vector math are included.

| Aon | Arguments | Behavior |
| --- | --- | --- |
| Remember | Optional Number key, optional Value | Store the evaluated value; defaults to key 0 and Caster |
| Recall | Optional Number key | Return the stored value; defaults to key 0 |

Keys are whole numbers 0–1,000,000. Draw 0–10 or calculate a larger key. Arguments follow the normal clockwise order. All current concrete values are supported: Number, Entity, Entity list, Block, Position, Direction, Vector and Region. Remember stores the evaluated value, not the drawing or a spell to run later. Coordinate/region snapshots retain their dimension; entity references retain UUIDs. A stored entity position updates when Position Of resolves its reference; a stored Position stays fixed.

Ordinary entries belong only to the caster and last **60 seconds of server ticks**, configurable with `memory.temporarySeconds`. Each caster can have 64 entries by default (`memory.maximumEntries`). Replacing a key replaces its value and restarts the timer. Reading does not consume it or refresh expiry. Expired keys are reusable. Temporary memory survives dimension travel and relogging within that timer, but clears on server shutdown. It is not a save-file storage system.

While **storing feruchemical copper**, Remember writes to the selected copper notebook, or the first accessible held/worn coppermind. While **tapping copper**, Recall reads from that notebook. Neither operation silently falls back to another coppermind when its key is missing or the selected item is full. Ordinary mode uses temporary memory. Remember while tapping and Recall while storing use temporary memory; switch copper mode to perform the corresponding permanent operation.

Coppermind values persist through saves/restarts, use the existing Identity/aluminum/feature/strength/contact checks, and share physical charged-byte capacity and entry limits with notes/images. Growing data stores the required copper charge. Reading is repeatable; partial drain preserves data under the existing copper rules, while emptying/wiping the coppermind erases it. Removing contact or using foreign keyed copper denies access. Existing notebooks load without migration. Multiple notebooks each have independent numeric keys.

Examples:

- `Remember(Number(1), Gaze Entity())`, then `Heat(Recall(Number(1)))`.
- `Remember(Number(2), Position Of(Caster))`, then `Light(Recall(Number(2)))` to illuminate those saved coordinates while still in range.
- `Remember(Number(3), Vector(Number(0), Number(2), Number(0)))`, then `Force(Caster, Recall(Number(3)))`.
- `Remember(Number(4), Number(7))`, then use `Divide(Recall(Number(4)), Number(2))` for 3.5.

A missing/expired key, unloaded/dead entity, wrong dimension or wrong value type reports an error before effects. Recall does not load chunks, summon entities, bypass casting range, permit cross-dimension effects or lift target/vector caps. Entity lists are snapshots, while a stored Region can be queried again using Entities In. New values keep their type: a Position cannot be multiplied as a Vector. Runtime type checks handle Recall because its stored type may change between casts.

No strings, arbitrary NBT, saved spell execution, conditions, variables local to loops, or inscription system are introduced in this slice. Future language values can extend the typed memory codec.
