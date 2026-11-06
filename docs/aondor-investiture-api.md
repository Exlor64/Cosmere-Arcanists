# Authoritative AonDor integration API

`connection.WorldConnectionAccess.register(ResourceLocation, Provider)` samples a world Connection measurement. `Provider.sample(player, world, current)` returns finite, nonnegative baseline/effective `State`. Keep the baseline unboosted; do not report the boosted result as the baseline. Providers run in registration order on the server and decide how their source combines with earlier measurements. Real funded duralumin is sampled first; the timed Aon self-buff then uses its strongest live multiplier. Closing registration removes the provider. Registries are bounded to64providers.

`aondor.AonInvestiture.register(ResourceLocation, Source)` adds an external source. `Source.reserve(player, AonCost.Quote)` returns null if the source cannot cover the complete quote, otherwise a `Payment`:

- `commit()` atomically validates ownership/contact/availability and pays the entire quote, returning false without spending on failure.
- `refund()` restores committed resources exactly once when the cast fails or applies no effects.
- `close()` releases any outstanding reservation/lock; it must not refund a successful committed cast.

Convert the server-owned relative work quote into your own storage units. Validate finite conversions and actual funds. Do not grant free eligibility from a held item or client packet. Every callback must be synchronous on the authoritative server thread; do not move players/change worlds/cast spells inside source callbacks. Check that the source still exists/is eligible when committing; closing registration does not by itself invalidate an already issued receipt. Implement rollback for all resource mutations you own. No API introduces a personal Dor bank by default.

Callbacks are trusted mod integrations, not a client extension mechanism. Aon's core rechecks Connection, actor/world stability and targets around payment, but cannot make arbitrary third-party callbacks transactional or undo arbitrary third-party world side effects. Keep provider callbacks exception-free except ordinary fail-closed argument errors. Source-specific ownership/Identity/config/aluminum/fuel protections remain your responsibility; the caller's AonDor ownership/global gates are enforced separately.

Player/network casting goes through `AonDorService.execute`. `AonEvaluator.prepare` produces a validated deferred plan and quote; `AonInvestiture.execute` enforces access/payment. Low-level `AonEvaluator.execute` remains a trusted server engine/test entry point and deliberately does not claim to enforce a player's power slot or funding. Never expose it as a client-request handler.

There is no bundled off-world fuel source yet, intentionally: unfinished powers/items can integrate later. See [player controls and costs](aondor-access-controls.md) for Sel configuration, thresholds and the prototype work table.
