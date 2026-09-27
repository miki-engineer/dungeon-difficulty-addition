# Anvil salvaging

Uses vanilla anvils, including chipped and damaged anvils. No custom table or salvage screen. The previous unreleased workstation prototype is retired; placed prototype tables will disappear when loading that development world. Upgrading uses the normal anvil screen.

1. Sneak-right-click an anvil with levelled equipment in your main hand to place one item.
2. Left-click the placed equipment or anvil with a salvage hammer three times. Allow at least six ticks between hits.
3. The third accepted hit consumes the equipment and drops one fragment of the same level and category.

## Crafting gems and upgrading

Put four same-type, same-level fragments into four separate crafting slots (inventory 2x2 or crafting table, any arrangement). This consumes one fragment per slot and produces one same-type, same-level upgrade gem. Mixed levels, mixed categories and extra ingredients do not craft.

In the normal anvil screen, put equipment on the left and a matching upgrade gem on the right. Upgrades require exactly the next level: level 3 equipment + level 4 gem becomes level 4; level 2 + level 4 is rejected. One gem is consumed. The provisional XP cost is 1 level (`upgrade_xp_levels`, configurable from 1 to 39 in `anvil_salvage.json`; restart to reload). Existing configs can add this key; absent means 1. Enchantments and damage are preserved, with vanilla-style optional renaming. Config-fixed equipment cannot be upgraded. Unlevelled equipment must acquire level 1 through the existing minimum-level system first.

Sneak-right-click with an empty main hand to retrieve the original equipment before completion. Breaking/removing the anvil releases the equipment. Placement and progress persist across saves. Hoppers cannot extract the displayed equipment. Progress is shared between players; each accepted strike uses the current hammer's level limit.

## Hammers

| Tier | Maximum item level | Durability |
| --- | --- | --- |
| Gold | 3 | 32 |
| Diamond | 5 | 1561 |
| Netherite | Unrestricted | 2031 |

Each accepted strike costs one durability using vanilla durability rules; rejected strikes cost nothing. Creative does not consume durability. Each hammer is crafted like a pickaxe, with three matching material blocks (gold, diamond or netherite) across the top and two sticks down the center. They appear in Tools & Utilities.

Configure `config/dungeon_difficulty_addition/anvil_salvage.json`, then restart. Keys: `enabled`, `gold_max_level`, `diamond_max_level`, `gold_durability`, `diamond_durability`, `netherite_durability`. Netherite has no configurable level cap. An invalid config is preserved and disables salvaging rather than silently replacing it.

Armor and shields yield armor fragments; supported accessories yield accessory fragments; other eligible equipment (including tools) yields weapon fragments. Item tags `dungeon_difficulty_addition:salvage/weapon`, `salvage/armor`, and `salvage/accessory` can override the category of eligible equipment. Spellbooks, scrolls, ordinary items and gems are excluded. Equipment must have a positive valid level. Gem levels use the existing 1–100000 supported range.

## In-game checks before release

- Place each equipment category; verify the correct fragment type and exact level after three separate hits.
- Craft four matching fragments; reject mixed types/levels. Test shift-crafting with stacked fragments.
- Upgrade level 3 to 4 in survival; verify one gem and the configured XP cost are consumed. Reject level 2 to 4, wrong gem types, fragments and fixed-level items. Check enchantments, name and damage are preserved.
- Try gold at levels 3 and 4, diamond at 5 and 6, and netherite above 5. Rejected hits must not change progress or durability.
- Verify durability loss in survival, including a hammer breaking midway through salvaging.
- Retrieve after one or two hits; save/reload with an item placed; remove or let the anvil fall. Original equipment and its components must survive.
- Try two players, repeated clicks, occupied anvils and hoppers: no duplication or disappearing equipment.
- Verify books, scrolls, unlevelled items and gems cannot be placed, and normal anvil use still works.

## Supplied artwork

Fragment textures use the earlier sprites, including the replacement armor fragment. Upgrade gem textures use the latest sprites in accessory, weapon, armor attachment order. Hammer models/textures come from the supplied `different tridents.zip` (Fischvogel assets); original model credits are retained. Confirm the asset license permits redistribution inside a public mod before publishing. Archived station concept art is not used by the new system.
