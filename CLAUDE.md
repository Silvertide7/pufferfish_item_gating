# Pufferfish Item Gating

A NeoForge **1.21.1** mod (Java 21, NeoForge 21.1.230) that gates item usage behind player skills from **Pufferfish's Skills** (`puffish_skills`). When a player has not unlocked the required skill(s), the mod prevents them from using a gated item to:

- deal damage to entities
- break blocks
- use it (right-click — e.g. draw a bow, cast a fishing rod, raise a shield)
- equip it as armor
- equip it as a Curios accessory

Gating rules are **server-authoritative** and defined by datapacks, loaded through a `SimpleJsonResourceReloadListener` so packs can add or override rules without code changes.

**Creative-mode players bypass every gate.** All five gates plus the validation sweep early-return when `player.isCreative()` — operators and testers in creative can already `/give` themselves any item and bypass restrictions trivially, so enforcing gates in creative is theatre. Adventure and survival players are still gated normally. Minecraft's creative inventory also uses a separate slot-sync protocol (`ServerboundSetCreativeModeSlotPacket`) that doesn't play well with the deferred armor eject — bypassing creative sidesteps that incompatibility cleanly.

## Tech Stack & Versions

- Minecraft 1.21.1 / NeoForge 21.1.230 / Java 21
- Parchment mappings 2024.11.17 (1.21.1)
- Hard dependency: Pufferfish's Skills — `net.puffish:skillsmod` (CurseForge `puffish-skills-835091`), mod id `puffish_skills`
- Optional dependency: Curios API (mod id `curios`) — when present, enforces the `equip_curio` gate; compiled against the `:api` artifact only (`compileOnly`)

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

- `PufferfishItemGating` — `@Mod` entry point; owns `MODID` and `LOGGER`; wires the skill-event setup and the optional Curios integration on the mod bus.
- Datapack loader (`config` package: data model + `Codec` + `ItemGatingReloadListener`) registered on `AddReloadListenerEvent`. `ItemGatingRules` holds three coordinated structures: `rulesByItem`, a reverse index `entriesBySkill` (`SkillRequirement → Set<ItemGatePair>`), and `allGatedEntries` (for cache rebuilds).
- `enforcement/ItemGateEvaluator` — per-player cache of blocked items (`UUID → EnumMap<ItemGate, Set<Item>>`). `isAllowed(player, item, gate)` is two-to-three hash lookups with **no Puffish calls in the hot path**. Built on player join, targeted updates on `Events.SkillUnlock` / `Events.SkillLock` via the reverse index, cleared on logout. **OR** semantics within and across rules — any unlocked skill in any applicable rule lets the action through. Pushes the player's blocked-items map to that player's client via `S2CSyncBlockedItemsPacket` after every build/update.
- `network/S2CSyncBlockedItemsPacket` + `network/NetworkSetup` — server→client sync of the blocked-items map. Sent on player join and after each `SkillUnlock`/`SkillLock` recompute. The record's compact constructor deep-copies the map (with `Set.copyOf` per inner set) so the packet carries a snapshot; respec fires `SkillLock` rapidly and the Netty IO thread encodes earlier packets while the server thread is still mutating the live cache — without the snapshot the inner `HashSet` iteration races and throws `ConcurrentModificationException`.
- `client/ClientBlockedItems` — client-side mirror of the blocked items, populated by the packet handler.
- `client/ClientGateHandler` — `@EventBusSubscriber(value = Dist.CLIENT)`; cancels `PlayerInteractEvent.RightClickItem` (use — bow draw, shield raise, fishing-rod cast), `PlayerInteractEvent.LeftClickBlock` (break — destroy animation/particles), and `AttackEntityEvent` (attack — swing animation) before MC's client-side prediction can run vanilla `Item.use` / destroy / `Player.attack`. Server-side `VanillaGateHandler` remains authoritative; the client handler just suppresses the misleading animations.
- `enforcement/GateFeedback` — throttled (~1s per player) action-bar message when a gate blocks an item; entry cleared on logout.
- `enforcement/Validation` — sweeps a player's worn armor (always) and curios (when Curios is loaded), removing items the player no longer satisfies (queries the cache) and returning them to the inventory. Used by `OnDatapackSyncEvent` (login + `/reload`) and Puffish's `SkillLock` event — moments where multiple slots may flip at once.
- `events/VanillaGateHandler` — server-side NeoForge listeners enforcing the vanilla gates: `AttackEntityEvent` (attack), `BlockEvent.BreakEvent` (break), `PlayerInteractEvent.RightClickItem` (use), and `LivingEquipmentChangeEvent` (equip_armor). For armor specifically, the eject is *deferred* to the next tick's `ServerTickEvent.Pre` so the slot mutation runs *after* `LivingEntity.detectEquipmentUpdates` has captured `lastArmorItemStacks[slot]` correctly. Mutating the slot during the event itself desyncs that tracking and causes alternating equip-success behavior on shift-click. `MinecraftServer.tell(new TickTask(...))` looks like it would work but `shouldRun` falls through to `haveTime()` and runs the task in the same tick whenever the server isn't lagging — `ServerTickEvent.Pre` is the only reliable way to defer to a future tick. The deferred task re-checks the slot (item may have changed or skill may have been unlocked in the meantime) before ejecting.
- `events/ValidationEventHandler` — `OnDatapackSyncEvent` builds the cache (and runs `Validation`) for the joining player (login) or every online player (`/reload`). `PlayerLoggedOutEvent` clears that player's cache and feedback throttle entry.
- `setup/SkillEventsSetup` — `FMLCommonSetupEvent` registers Puffish's `SkillUnlock` (cache update only) and `SkillLock` (cache update plus `Validation`, so newly-blocked worn items eject immediately).
- `setup/CuriosSetup` + `compat/CuriosCompat` — optional Curios integration (see below).

## Datapack format

Rules load from `data/<namespace>/item_gates/*.json` across **all** namespaces (directory `item_gates`, handled by `ItemGatingReloadListener`). One file = one rule.

Fields:

- `item` *(required)* — registry id of the gated item, e.g. `"minecraft:diamond_sword"`. Item ids only; tags are not yet supported.
- `gates` *(optional)* — which actions this rule gates. Any of `"attack"`, `"break"`, `"use"`, `"equip_armor"`, `"equip_curio"`. **Omit to gate all five.**
- `skills` *(required)* — list of skill requirements. Each entry is `{ "category": <ResourceLocation>, "skill": <string> }`, where `category` is the Pufferfish's Skills category id and `skill` is the skill id within it. **OR semantics: the player passes the rule if they have unlocked *any one* of the listed skills.**

Multiple rules may target the same item — typically to gate different actions (`gates`). When two or more rules apply to the same `(item, gate)` combination, the player passes if **any one** of them is satisfied (OR across rules, mirroring the OR within a rule). Loaded rules are keyed by item in `ItemGatingRules` (`forItem(Item)` is the read seam the enforcement layer uses).

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

## Curios compatibility (optional)

Curios is a **soft dependency**: `compileOnly` against the `:api` artifact, declared `optional` in `neoforge.mods.toml`. Nothing may touch a Curios class unless Curios is loaded, or the JVM throws `NoClassDefFoundError`.

- All Curios-referencing code lives in `compat/CuriosCompat`, which is **not** `@EventBusSubscriber`-annotated (that would classload it unconditionally).
- `setup/CuriosSetup#init` (an `FMLCommonSetupEvent` listener registered from the mod constructor) guards with `ModList.get().isLoaded("curios")`, then calls `CuriosCompat.initialize(NeoForge.EVENT_BUS)`. Lazy classloading means `CuriosCompat` and its Curios imports link only inside that branch.

What it enforces:

- `CurioCanEquipEvent` — if the wearer is a `ServerPlayer` who fails the `equip_curio` gate, `setEquipResult(TriState.FALSE)` blocks the equip (and `GateFeedback` shows a throttled message).
- `ejectInvalidCurios(ServerPlayer)` is called externally by `enforcement/Validation`. Its triggers — `OnDatapackSyncEvent` (login + `/reload`) and Puffish's `SkillLock` (in-session respec) — live in `events/ValidationEventHandler` and `setup/SkillEventsSetup` respectively, so this compat module stays focused on Curios-only logic.

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

## Version Control

**The user handles commits in git.** Never run `git add`, `git commit`, or `git push` — and don't suggest doing so — unless the user explicitly asks. Wrap up work by reporting what changed; staging and pushing are the user's job.
