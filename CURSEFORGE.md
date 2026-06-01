# Pufferfish's Skills Item Restrictions

> Requires [Pufferfish's Skills](https://www.curseforge.com/minecraft/mc-mods/puffish-skills).
> Optional: [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) for the `equip_curio` gate.

***

## What it does

Locks items, blocks, and entities behind player skills from Pufferfish's Skills. A rule can target one of three things:

**Item targets** — if a player hasn't unlocked the required skill, they can't use that item to:

- deal damage to entities (`attack`)
- break blocks (`break`)
- right-click it (drawing a bow, raising a shield, casting a fishing rod, etc.) (`use`)
- equip it as armor (`equip_armor`)
- equip it as a Curios accessory (`equip_curio`)

**Block targets** — block right-clicking that specific block (`interact`). Useful for gating things like crafting tables, brewing stands, anvils, doors, levers, or any other interactable block.

**Entity targets** — block right-clicking that entity type (`interact`). Useful for gating things like villager trades, boat/horse mounting, armor stand equipping, etc.

Rules are loaded from datapacks, so packs can configure or override what's gated without touching code.

***

## How to use it

Add a JSON file under `data/<your-namespace>/item_gates/<anything>.json`. One file is one rule.

Fields:

- Exactly one of `item`, `block`, or `entity` (required): the registry id of what's being gated.
- `gates` (optional): which actions to gate. For items, any subset of `"attack"`, `"break"`, `"use"`, `"equip_armor"`, `"equip_curio"`. For blocks and entities, only `"interact"`. Omit to gate the full set for the target type.
- `skills` (required): list of skill requirements. Each entry is `{ "category": "<category id>", "skill": "<skill id>" }`. The player passes if they have unlocked any one of them.

Item example, at `data/my_pack/item_gates/diamond_sword.json`:

```json
{
  "item": "minecraft:diamond_sword",
  "gates": ["attack", "break"],
  "skills": [
    { "category": "my_skills:combat", "skill": "swordsmanship" },
    { "category": "my_skills:combat", "skill": "blade_mastery" }
  ]
}
```

Block example, at `data/my_pack/item_gates/crafting_table.json`:

```json
{
  "block": "minecraft:crafting_table",
  "skills": [
    { "category": "my_skills:crafting", "skill": "basic_crafting" }
  ]
}
```

Entity example, at `data/my_pack/item_gates/villager.json`:

```json
{
  "entity": "minecraft:villager",
  "skills": [
    { "category": "my_skills:social", "skill": "trading" }
  ]
}
```

Multiple rules can target the same item, block, or entity (typically to gate different actions). When more than one rule applies to the same target and gate, the player passes if any of them is satisfied.

If you typo an item, block, entity, or skill id, the mod logs a warning and skips that rule, so the target doesn't end up accidentally locked forever.

***

## Notes

- **Creative and spectator bypass every gate.** Anyone in creative can `/give` themselves any item anyway, so there's no point enforcing it there.
- **Server-authoritative.** Checks happen on the server. The server pushes the player's blocked-targets list to the client so animations (bow draw, attack swing, block-break particles) get suppressed locally too, but the server is the source of truth.
- **Curios is optional.** Without it installed, the `equip_curio` gate just doesn't do anything. No errors, no warnings.
- **Respec safe.** If a player respecs (locks skills) while wearing gated armor or curios, those items get returned to their inventory automatically. The `interact` gate is per-action, so there's nothing to clean up on respec.
- **Performance.** The blocked-targets list is cached per player on join and updated incrementally when skills unlock or lock. No per-tick or per-action queries into Pufferfish's Skills.
- **Sneak bypass on blocks.** When sneaking, the `interact` gate on blocks doesn't fire. This lets players place items (or other blocks) adjacent to a gated block without being told they can't interact. Entity gates always fire regardless of sneak.

***

## Modpack policy

Free to use in any modpack. A link back to this page is appreciated.
