# AonDor access, work costs, Connection and saved layouts (0.65.0)

[New glyph stroke guide](aondor-access-guide.svg). This release implements the agreed access rules without introducing personal Elantrian reserves or inventing fuel items for unfinished power systems.

## Casting access

- **On Sel:** ambient Dor is effectively unlimited. The spell is priced and validated, but no player reserve is deducted.
- **Elsewhere:** effective Connection to Sel must be at least **3× the current unboosted baseline**, and one registered external Investiture source must cover the full work quote. Either missing requirement rejects the cast before effects.
- **Funded feruchemical duralumin** contributes its existing real Connection multiplier. Merely selecting tapping or possessing a power does not qualify.
- `WorldConnectionAccess.register` lets any other power report Connection, including Bondsmith abilities when implemented. Providers receive baseline/effective measurements and own their funding, expiry and stacking rules.

There is not yet a numeric planetary Connection ledger. An existing Sel art/native Connection is normalized to baseline1; this is a relative measurement, not AonDor strength. A provider can replace it with a real baseline/effective measurement. A zero baseline fails closed rather than unlocking off-world casting at0. No unboosted baseline is replaced with the boosted result.

**Sel's dimension is not generated yet.** The default ambient dimension ID is `cosmerearcanists:sel`, ready for the later world implementation. For current development playtesting, explicitly add the desired test dimension to `investiture.ambientDorDimensions` in the AonDor server config. Do not mistake that development override for default Overworld access. `enforceAccess=false` is an explicit administrative bypass, defaulttrue. Drawing, learning and loading a layout remain possible for practice; executing a spell is gated.

No external fuel item, personal reserve, Bondsmith implementation or unfinished Investiture power is added. Off-world operation is ready for a registered source; without one, it is deliberately unavailable even with enough Connection.

## Connection Aon

`Connection([Number multiplier = 3], [Number seconds = 60])` boosts the caster's own Sel Connection. The Aon works alone. Skip can default either optional slot.

Effective multiplier is `1 + (requested − 1) × AonDor strength × recognition quality`. The duration is the specified number of seconds. Default limits are multiplier32, duration3600seconds and8active timed boosts per player. These are server-configurable. Each Aon boost retains its own expiry; the strongest live Aon boost wins, so a cheap weaker recast cannot extend a stronger boost's paid duration. It can coexist with provider-reported Connection, taking the stronger result; existing duralumin storage attenuation still applies to the Aon contribution.

Cast this while you already have access, for example on Sel before travelling. It can maintain off-world eligibility for its duration, but it does not supply fuel. It cannot bootstrap or refresh itself after access is lost. Expiry uses persisted overworld game ticks, surviving normal travel/relog/player-data cloning for the remaining duration without resetting the timer.

- `Connection()` requests3×baseline for60seconds.
- `Connection(Number(4), Number(10))` requests a stronger short boost.
- `Connection(Skip, Multiply(Number(6), Number(10)))` keeps multiplier3 and specifies60seconds.

## Comparisons

| Aon | Inputs | Output |
| --- | --- | --- |
| Equal | Value, Value | Boolean typed equality |
| Greater Than | Number, Number | Boolean: first > second |
| Less Than | Number, Number | Boolean: first < second |

Use them in If/And/Or/Not. Equal uses the existing typed value/list equality, with numeric±0 equal. Positions and vectors are different types. There is no implicit Boolean/Number conversion.

## Dynamic work and payment

The server prepares the actual chosen expression path, targets, recipes, item counts and deferred effects first. The drawing feedback shows **Estimated work** and ambient/Connection status. This is a relative work quote, not a personal Dor meter or a promise that an external source has enough fuel. Source-specific units/conversions belong to that source.

The quote includes the following prototype work units, multiplied by server `investiture.workCostScale` (default1):

| Work | Relative units |
| --- | --- |
| Cast / evaluated expression node | 1 / 0.01 |
| Spherical query | 0.01×volume |
| Force | Unique eligible targets × planned impulse magnitude |
| Heat on living targets | Actual thaw amount/140 + planned direct damage + ignition power and0.1×burn seconds |
| Cooking / smelting | Effective power × every processed input item |
| Block warming / ignition | 1+power / power |
| Temporary light | Brightness × ticks/400 |
| Permanent light | Brightness ×20, one creation cost |
| Entity Glowing | Unique targets × ticks/200 |
| Memory read | Encoded value bytes ×0.001 |
| Memory write | Encoded bytes ×0.001 × max(1,temporary seconds/60), or ×20 for copper persistence |
| Connection boost | Effective bonus above1 × seconds |

These are configurable-scale prototype work units, not claimed canonical Investiture conversions. Power0 thawing still costs work; Skip resolves real defaults rather than waiving their cost. Lazy If/And/Or do not price unused branches. Set/list/math operations are priced when evaluated. Protected or duplicate Force targets do not multiply its work. Numeric overflow rejects rather than creating infinite/negative prices.

After preparation, the source reserves the complete quote. Access, ownership and world/position are rechecked before/after committing payment. Failed reservation/commit produces no spell effects. Invalid target recipes reject before any list member changes. Heat inputs and targets are revalidated after payment so consuming/changing a targeted item as fuel cannot duplicate its earlier contents. An application reported by the effect as fully denied/no-work is refunded; attempted/resisted work may still cost units. thrown application failures request a refund. Existing permission/place events still apply. No multi-source partial spending is introduced: one source must cover the quote.

## Saved spell layouts

- Draw/check a complete spell, then `/aondor save name`.
- `/aondor list` lists your layouts.
- `/aondor load name` restores the drawing for the next menu opening; it **does not cast**.
- `/aondor delete name` removes a layout.

Names use1–32lowercase letters, digits, underscores or hyphens. Up to8layouts are saved per player. Raw ink/geometry/parent links are stored in player data and preserved through normal saves and clones. Existing17glyph/3000point budgets apply. Loading re-recognizes and re-types against current server templates; casting rechecks live context, Connection, targets and funds. No executable IDs, cached targets, cached cost/payment or other-player memories are trusted. Saved layouts and learned glyphs are preserved when access is unavailable.

Protocol44 adds the bounded server-to-client draft restore payload; update both clients/server. The4newglyphs bring the library to42templates (5effects,37arguments). Other worlds, regional acquisition, Bondsmith powers and physical external-source items remain deferred.

Verified on test branch `aondor-access-0.65.0`:3947production checks, both Java21 builds/JUnit, all413required server tests with/withoutCurios and graphical client startup passed in [CI42307582358](https://github.com/Exlor64/Cosmere-Arcanists/actions/runs/42307582358). Actual command/drawing/provider usability and multiplayer remain for hands-on testing. Main unchanged.
