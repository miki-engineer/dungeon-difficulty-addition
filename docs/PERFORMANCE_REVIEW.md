# Performance and correctness review

First pass, 2026-09-27. Reviewed fixed/minimum item scaling, container hooks, item decorations and tooltips, anvil salvage/upgrades, and encounter/readiness paths. This is source review plus automated checks, not a full modpack runtime certification.

## Changes

- Fixed-level rules compile regexes once per config load. Repeated item-ID lookups (including no-match results) use a synchronized, bounded 4096-entry cache owned by that config instance. Reloading creates a fresh cache; highest-level precedence and legacy rules are unchanged.
- Gem/fragment level reads no longer copy their NBT compound for each icon, tooltip or recipe check. The underlying data is read only.
- Fixed-modifier cleanup leaves attribute components untouched when no fixed modifiers exist, including the optional Accessories integration.
- Nonstandard equipment scaling only formats diagnostic attribute lists for the two diagnostic items, and only until they have been logged.
- Relic tooltip number patterns use a bounded 256-entry cache. Only regex syntax is cached, never scaled tooltip output. Missing or malformed relic effect files are retried at most every five seconds instead of on every tooltip update.
- Encounter tick handlers reject non-mobs/client entities before config work. Disabled damage tracing exits early.
- Stationary anvil display entities avoid redundant position updates; equipment copies for shatter particles are made only on the final strike.
- Container handlers reject unrelated screens before config work and avoid dirty/sync notifications if no level was actually assigned. Chest checking remains immediate; no delayed polling or stale stack cache was introduced.

No changes to level formulas, hammer balance, gem costs, salvage outcomes, readiness calculations or level-1 eligibility. No automatic commit, push, config modification or Minecraft restart is part of this pass.

## Measurement

`tools/benchmarks/FixedLevelLookupBenchmark.java` compares the old regex-on-every-lookup implementation with the new path, checking equal results. One local Java 21 run: 32 rules, 64 repeatedly queried item IDs, 100,000 measured lookups after warmup; old 274.33 ms, new 1.79 ms; equal checksum 134379.

This intentionally measures a repeated-lookup workload that benefits from caching. It does not estimate whole-game FPS/TPS, cold lookup performance or packs with no fixed-level rules. JVM warmup, machine load and cache hit rate affect timings. Compile the benchmark together with `config/FixedItemLevels.java` against Gson, then run its `com.miki.dungeondifficultyaddition.config.FixedLevelLookupBenchmark` main class. Do not turn timings into pass/fail tests.

Regression coverage includes rule priority, legacy decoding, malformed regex, cached misses, fresh config instances, cache eviction, and tooltip number boundaries/replacement order. Build and JUnit checks are supplemented by the benchmark, not replaced by it.

## Follow-up risks / limits

- `SpellExecutionMixin` and similar HEAD/RETURN context hooks are not exception-safe: an exception during the wrapped call can skip the pop. A scoped try/finally wrapper should be validated against Spell Engine in a dedicated runtime test before changing these hooks. This is a source-level risk, not a reproduced crash.
- Server salvage settings are not synchronized to clients. Server checks remain authoritative, but locally configured hammer tooltips can disagree on multiplayer servers.
- Minimum-level container handling covers generic chest/barrel menus and shulker boxes, not every custom storage UI a mod can implement.
- Anvil upgrade previews call the existing randomized scaler. Recomputing a preview can reroll stats; changing that requires a deliberate roll/persistence policy, not just caching previews.
- Very high Roman numerals must shrink to fit the badge; legibility is a design limit rather than a tick-performance issue.
- Old table prototype art/tools remain untracked locally and were not deleted or changed. Their removal can be a separate cleanup pass.

## Next in-game verification

Open a loot chest and verify level-1 assignment without pickup, with existing higher levels unchanged. Hover several relics and both gem types while toggling Shift. Salvage and upgrade once. Exercise incoming/outgoing readiness damage and an active relic, then inspect the log. Profile a representative crowded dungeon and inventory session before making broader caching or tick-frequency changes.
