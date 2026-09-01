# Dungeon Difficulty Additions

A NeoForge mod for Minecraft 1.21.1 that expands
[Dungeon Difficulty](https://www.curseforge.com/minecraft/mc-mods/dungeon-difficulty) loot scaling.

## What It Does

- Scales Jewelry and Relics items with the local dungeon level.
- Scales damage, healing, duration, cooldown, proc chance, range, and radius.
- Shows the scaled values directly in item tooltips.
- Supports fixed levels for boss drops, crafted items, and other modded equipment.
- Supports JEI and EMI recipe-output previews.

Level `0` is the base item. Scaling bonuses begin at level `1`.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1 or newer
- Dungeon Difficulty 3.6.10 or newer

Optional integrations:

- Jewelry 2.3.2 or newer
- Relics (RPG Series) 1.3.0 or newer
- Spell Engine 1.10.1 or newer
- Curios
- JEI or EMI

## Installation

1. Install NeoForge and Dungeon Difficulty.
2. Add this mod and any optional integration mods to the `mods` folder.
3. Start Minecraft once to generate the config files.

The configs are created in:

```text
config/dungeon_difficulty_addition/
├── settings.json
└── fixed_item_levels.json
```

## Basic Settings

`settings.json` controls accessory scaling and tooltip display:

```jsonc
{
  // Turns the mod on or off.
  "enabled": true,
  // Scales Jewelry items.
  "scale_jewelry": true,
  // Scales Relics items.
  "scale_relics": true,
  // Prevents duplicate accessory stat lines.
  "merge_accessory_modifiers": true,
  // Shows scaled values in tooltips.
  "show_compat_tooltip": true,
  // Controls stat scaling per level. Supports attribute IDs and regex.
  "attributes": [
    {
      "attribute": ".*",
      "operation": "MULTIPLY_BASE",
      "randomness": 0.05,
      "value": 0.1,
      "offset": 0.0
    }
  ]
}
```

With `value: 0.1`, an item gains roughly 10% of its base value per level.
For example, a 20% effect at level 4 becomes 28% before randomness.
Cooldowns scale inversely, so higher levels reduce them instead.

## Fixed Item Levels

Use `fixed_item_levels.json` to force specific items to a chosen level:

```jsonc
{
  // Add any level number here.
  // Put item IDs or regex patterns in its list.
  // The highest matching level wins.
  "levels": {
    "3": [
      "example_mod:boss_weapon"
    ],
    "20": [
      "example_mod:endgame_relic",
      "another_mod:.*sword.*"
    ]
  }
}
```

Any positive whole-number level is supported.
Exact item IDs and Java regular expressions can be used. If several rules match,
the highest level wins.

Fixed levels work on loot, boss drops, crafted items, commands, ground items,
and items already held in an inventory.

## License

All Rights Reserved.
