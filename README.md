# Dungeon Difficulty Additions

A NeoForge mod for Minecraft 1.21.1 that expands
[Dungeon Difficulty](https://www.curseforge.com/minecraft/mc-mods/dungeon-difficulty) loot scaling.

## What It Does

- Scales Jewelry and Relics items with the local dungeon level.
- Scales damage, healing, duration, cooldown, proc chance, range, and radius.
- Shows the scaled values directly in item tooltips.
- Supports fixed levels for boss drops, crafted items, and other modded equipment.
- Supports JEI and EMI recipe-output previews.
- Optionally applies dungeon damage penalties based on equipped item levels.

Level `0` is the base item. Scaling bonuses begin at level `1`.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1 or newer
- Dungeon Difficulty 3.8.0 or newer

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
├── fixed_item_levels.json
└── encounters.json
```

## Command

Operators can give a player an item at any positive level:

```text
/dda give <player> <item> <level>
```

Example:

```text
/dda give @s minecraft:diamond_sword 5
```

Items configured in `fixed_item_levels.json` always use their configured fixed
level, even when the command requests a different level.

## Accessory Settings

`settings.json` controls accessory scaling and tooltip display:

```jsonc
{
  // Turns the mod on or off.
  "enabled": true,
  // Gives unlevelled equipment level 1. Requires enabled = true.
  "minimum_equipment_level_enabled": true,
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

## Encounter Settings

`encounters.json` controls equipment requirements and dungeon damage penalties.
This feature is **disabled by default** and is independent of accessory scaling.

```jsonc
{
  "player_readiness": {
    // Enables equipment readiness and its damage rules.
    "enabled": false,
    // Number of equipped armor/Curios pieces needed at or above a level.
    "required_equipped_items": 3,
    // Level used for items without a Dungeon Difficulty level marker.
    "unmarked_item_level": 0,
    // Adds 25% damage taken per missing readiness level.
    "incoming_penalty_per_level": 0.25,
    // Enables the main-hand level requirement for damage dealt.
    "outgoing_penalty_enabled": true,
    // Removes 10% damage dealt per missing main-hand level.
    "outgoing_penalty_per_level": 0.10,
    // Limits the outgoing damage reduction to 90%.
    "maximum_outgoing_reduction": 0.90
  },
  // Dungeon Difficulty encounter scopes that use these rules.
  "scopes": ["dungeon", "heroic"],
  // Highest level used in readiness calculations.
  "max_level": 1000
}
```

### Required Pieces

With `required_equipped_items: 3`, you need three equipped pieces at level 4
or higher to count as readiness level 4. Set it to `2` to require only two pieces.
For example, level 4 leggings alone give readiness 0; adding a level 4 chest
piece gives readiness 4 when two pieces are required.

Armor and active functional Curios count together. Hands, inventory items,
cosmetic Curios, and empty slots do not count. Each occupied slot counts once.
Fewer than the required number of pieces gives readiness 0; otherwise, the
lowest level among your best required pieces determines readiness.

### Damage Multipliers

Against a level 4 mob, readiness 0 with `incoming_penalty_per_level: 0.25`
means four missing levels: `1 + 4 × 0.25 = 2`, or twice the incoming damage.
Readiness 4 or higher removes this penalty.

Damage dealt uses your main-hand item separately. With a level 0 main hand
against a level 4 mob, `outgoing_penalty_per_level: 0.10` gives a 40% reduction.
Set it to `0.15` for a 60% reduction instead. A level 4 or higher main hand
removes this penalty. The reduction cannot exceed `maximum_outgoing_reduction`.

These multipliers apply before armor and other defenses. Player-attributed
projectiles and spells use the main-hand level when they hit. Set either penalty
rate to `0` to remove that penalty.

### Checking Your Settings

Use `/dda_readiness` to see your equipped item levels, required piece count,
readiness, and main-hand level. In Survival with cheats/operator permission,
use `/dda_damage_debug on` to log your next 20 incoming or outgoing hits in chat
and `logs/latest.log`. Use `/dda_damage_debug off` to stop early.

## License

All Rights Reserved.
