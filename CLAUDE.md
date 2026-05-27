# Pufferfish Item Gating

A NeoForge **1.21.1** mod (Java 21, NeoForge 21.1.230) that gates item usage behind player skills from **Pufferfish's Skills** (`puffish_skills`). When a player has not unlocked the required skill(s), the mod prevents them from using a gated item to:

- deal damage to entities
- break blocks
- use it (right-click — e.g. draw a bow, cast a fishing rod, raise a shield)
- equip it as armor
- equip it as a Curios accessory

Gating rules are **server-authoritative** and defined by datapacks, loaded through a `SimpleJsonResourceReloadListener` so packs can add or override rules without code changes.

## Tech Stack & Versions

- Minecraft 1.21.1 / NeoForge 21.1.230 / Java 21
- Parchment mappings 2024.11.17 (1.21.1)
- Hard dependency: Pufferfish's Skills — `net.puffish:skillsmod` (CurseForge `puffish-skills-835091`), mod id `puffish_skills`
- Planned optional dependency: Curios API — needed only to enforce the curios-equip gate

## Reading skills (Pufferfish's Skills API)

All skill queries are **server-side** and require a `net.minecraft.server.level.ServerPlayer`. Entry point: `net.puffish.skillsmod.api.SkillsAPI`.

- A skill is addressed by a **category** (`ResourceLocation`) plus a **skill id** (`String`).
- To check whether a player has unlocked a skill:

  ```java
  boolean unlocked = SkillsAPI.getCategory(categoryId)
      .flatMap(category -> category.getSkill(skillId))
      .map(skill -> skill.getState(serverPlayer) == Skill.State.UNLOCKED)
      .orElse(false);
  ```

- `Skill.State` is one of `LOCKED, AVAILABLE, AFFORDABLE, UNLOCKED, EXCLUDED`; only `UNLOCKED` counts as "the player has the skill".
- A reference to a missing category or skill id resolves to "not unlocked" (the rule points at something that does not exist).

## Architecture

- `PufferfishItemGating` — `@Mod` entry point; owns `MODID` and `LOGGER`.
- `events/EventHandler` — NeoForge game-bus subscribers that enforce the gates (attack, block break, use, equip). Run server-side only.
- Datapack loader (data model + `Codec` + `SimpleJsonResourceReloadListener`) registered on `AddReloadListenerEvent`.

## Datapack format

Rules load from `data/<namespace>/item_gates/*.json` across **all** namespaces (directory `item_gates`, handled by `ItemGatingReloadListener`). One file = one rule.

Fields:

- `item` *(required)* — registry id of the gated item, e.g. `"minecraft:diamond_sword"`. Item ids only; tags are not yet supported.
- `gates` *(optional)* — which actions this rule gates. Any of `"attack"`, `"break"`, `"use"`, `"equip_armor"`, `"equip_curio"`. **Omit to gate all five.**
- `skills` *(required)* — list of skill requirements. Each entry is `{ "category": <ResourceLocation>, "skill": <string> }`, where `category` is the Pufferfish's Skills category id and `skill` is the skill id within it. **OR semantics: the player passes the rule if they have unlocked *any one* of the listed skills.**

Multiple rules may target the same item — to require different skills per action, write one rule per action. Loaded rules are keyed by item in `ItemGatingRules` (`forItem(Item)` is the read seam the enforcement layer uses).

Example — `data/my_pack/item_gates/diamond_sword.json`:

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

## Build & Run

- `./gradlew build` — compile and package the mod jar
- `./gradlew runClient` — launch a dev client with the mod loaded
- `./gradlew runServer` — launch a dedicated dev server (`--nogui`)
- `./gradlew runData` — run data generators (output to `src/generated/resources`)
- Working/run directory is `run/`.

## Conventions

- **Server-authoritative.** Every gating check needs a `ServerPlayer`; event handlers must skip on the logical client (`level.isClientSide`) and only act on the server.

---

# Reusable Engineering Standards

The sections below are project-agnostic. Copy this block (everything below the `---` separator above) into any other project's `CLAUDE.md` unchanged to apply the same standards there.

## Code Style

**Never write comments.** No inline `//` comments, no `/* */` blocks, no javadoc, no leading explanatory headers on methods or fields. Code must be self-documenting through naming alone.

- Variable names describe what the value *is* (e.g. `armorCoveragePercent`, not `acp` with a comment).
- Method names describe what they *do* and under what conditions (e.g. `applyMultiplierIfAttackerIsPlayer`, not `applyBonus` with a comment explaining the player check).
- Extract a well-named helper method instead of writing a comment to explain a block.
- Constants get descriptive names that encode their meaning and unit (e.g. `KNIGHTMETAL_BONUS_DAMAGE_AT_FULL_ARMOR`, not `MAX` with a `// 2.0 vs fully-armored target` comment).
- If a name would need a comment to explain it, rename it until it doesn't.

Existing files may still contain comments and javadoc — leave them in place when editing unrelated code, but do not add new ones and prefer to delete obsolete ones when touching the surrounding code.

**Never leave dead code.** No unused methods, fields, classes, parameters, or imports. No "escape hatch" or "just in case" code. No commented-out blocks. If it's not called, delete it — the git history is the archive.

## Code Review

When asked to review code, do a "pass", check for issues, or otherwise audit a recent change, do **two** passes in order:

1. **Self-audit first.** Read the diff yourself. Fix the obvious — dead code, comments, naming, anything that violates the Code Style rules above. Report findings.
2. **Then spawn an independent reviewer** via the `/code-review` skill or a fresh agent. Give it only the diff and the goal, no context about why you made the choices you did. That catches the bugs you would otherwise rationalize away.

Don't skip step 2 because step 1 looked clean — the value of the independent reviewer is exactly that it doesn't share your blind spots.
