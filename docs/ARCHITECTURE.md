# Mod structure

`DungeonDifficultyAddition` is the entry point: it loads configuration and registers events and commands.

```text
com.miki.dungeondifficultyaddition
├── command    /dda give
├── config     Accessory settings, fixed-level rules, and legacy config migration
├── scaling    Item levels, attributes, previews, and optional scaling adapters
├── relic      Active relic effects and spell scaling context
├── readiness  Encounter levels, equipment readiness, damage penalties, and debug commands
├── compat     Optional-mod detection
└── mixin      Game integration hooks, grouped by item / loot / accessory / relic / client
```

Keep gameplay decisions in the feature packages and use mixins as integration hooks. Readiness keeps its own config and commands alongside its logic; optional scaling adapters stay with their callers and must only be accessed after checking the corresponding mod is installed.

## Main paths

- Loot hooks call accessory scaling after Dungeon Difficulty's native loot scaling.
- Crafting, inventory, dropped-item, and recipe-preview hooks share the same fixed/minimum-level enforcement.
- `/dda give` uses the same item scaler and respects configured fixed levels.
- Relic spell hooks share the active scaling context for runtime effects and tooltips.
- Readiness modifies the existing incoming damage event; it does not generate a second hit.

`FixedItemLevels` owns rule parsing and matching without depending on game registries. `ItemLevelData` owns fixed/manual ownership markers stored on items. Neither helper owns gameplay formulas.

## Compatibility and checks

Config file paths and keys, persisted item markers, modifier IDs, command syntax, defaults, and scaling formulas are unchanged by this reorganization. Update `dungeon_difficulty_addition.mixins.json` whenever a mixin moves; the plugin gates optional integrations by the mixin's simple class name.

Run `./gradlew build` with Java 21. Tests cover readiness math/config validation, fixed-level rules and migration, and mixin resource registration. Before releasing, smoke-test startup, crafted/minimum-level equipment, higher-level loot, fixed levels, spellbook/scroll exclusions, and optional accessory/relic integrations in game.
