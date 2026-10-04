# Pufferfish Item Gating

A Forge **1.20.1** mod (Java 17, Forge 47.4.10) that gates usage behind player skills from **Pufferfish's Skills** (`puffish_skills`). A rule can target an item, a block, or an entity type. When a player has not unlocked the required skill(s):

- **Item targets** can block: damaging entities (`attack`), breaking blocks (`break`), right-clicking the item (`use` — bow draw, shield raise, fishing-rod cast), equipping as armor (`equip_armor`), or equipping as a Curios accessory (`equip_curio`).
- **Block targets** can block: right-clicking the block (`interact` — opening a crafting table, lever, door, etc.).
- **Entity targets** can block: right-clicking the entity (`interact` — trading with a villager, mounting a boat, saddling a horse, etc.).

Gating rules are **server-authoritative** and defined by datapacks, loaded through a `SimpleJsonResourceReloadListener` so packs can add or override rules without code changes.

**Creative-mode players bypass every gate.** Every gate handler plus the validation sweep early-return when `player.isCreative()` — operators and testers in creative can already `/give` themselves any item and bypass restrictions trivially, so enforcing gates in creative is theatre. Adventure and survival players are still gated normally. Minecraft's creative inventory also uses a separate slot-sync protocol (`ServerboundSetCreativeModeSlotPacket`) that doesn't play well with the deferred armor eject — bypassing creative sidesteps that incompatibility cleanly.

## Tech Stack & Versions

- Minecraft 1.20.1 / Forge 47.4.10 / Java 17, built with ForgeGradle 6 + Parchment librarian (Gradle 8.8)
- Parchment mappings 2023.09.03-1.20.1
- Hard dependency: Pufferfish's Skills 0.19.x — CurseForge `puffish-skills-835091` via cursemaven, mod id `puffish_skills`; the 1.20.1 API surface is identical to the 1.21 build
- Optional dependency: Curios API 5.x (mod id `curios`) — when present, enforces the `equip_curio` gate; compiled against the `:api` artifact only (`compileOnly`)
- Every mod dependency is wrapped in `fg.deobf(...)`; the shipped jar is SRG-remapped by `reobfJar`
- Mod metadata lives in `src/main/resources/META-INF/mods.toml` + `pack.mcmeta` (pack format 15), expanded by `processResources` from `gradle.properties`
- This branch (`1-20-1`) is the Forge backport of the NeoForge 1.21.1 mod on `main`; see `~/Workspace/minecraft/personal/claude_reference/forge-port/` for the port guide

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

- `PufferfishItemGating` — `@Mod` entry point; owns `MODID` and `LOGGER`; wires the skill-event setup and the optional Curios integration on the mod bus (obtained via `FMLJavaModLoadingContext.get()` — deprecated in Forge 47.4 in favor of constructor injection, but injection only exists from 47.4 and `mods.toml` allows any 47.x, so `get()` stays) and registers the network channel directly from the constructor.
- `config/GateTarget` — sealed interface with three records: `ItemTarget(Item)`, `BlockTarget(Block)`, `EntityTypeTarget(EntityType<?>)`. Records auto-generate `equals`/`hashCode` so `Set<GateTarget>` holds all three kinds without collision (an `ItemTarget(crafting_table)` is not equal to a `BlockTarget(crafting_table)` because the record types differ). Static `writeTo` / `readFrom` helpers encode the target on the wire as `(byte kind, ResourceLocation id)` with kind 0=item, 1=block, 2=entity; unknown registry ids return `Optional.empty` instead of falling back to `AIR`/`PIG`.
- Datapack loader (`config` package: data model + `Codec` + `ItemGatingReloadListener`) registered on `AddReloadListenerEvent`. `ItemGatingRules` holds five coordinated structures: `rulesByItem`, `rulesByBlock`, `rulesByEntityType`, a reverse index `entriesBySkill` (`SkillRequirement → Set<GatePair>`), and `allGatedEntries` (for cache rebuilds). The rule codec accepts exactly one of `items` / `blocks` / `entities` fields and validates gate-target compatibility (`interact` is only valid on block/entity, the rest only on item) — mismatches are logged + the rule skipped.
- `enforcement/ItemGateEvaluator` — per-player cache of blocked targets (`UUID → EnumMap<ItemGate, Set<GateTarget>>`). `isBlocked(player, target, gate)` is two-to-three hash lookups with **no Puffish calls in the hot path**. Overloads accept `Item`, `Block`, or `EntityType<?>` for ergonomics. Built on player join, targeted updates on `Events.SkillUnlock` / `Events.SkillLock` via the reverse index, cleared on logout. **OR** semantics within and across rules — any unlocked skill in any applicable rule lets the action through. Pushes the player's blocked map to the client via `S2CSyncBlockedItemsPacket` after every build/update.
- `network/S2CSyncBlockedItemsPacket` + `network/NetworkSetup` — server→client sync of the blocked map over a `SimpleChannel` (`NetworkSetup.sendToPlayer`). The packet is a plain record with `encode(FriendlyByteBuf)` / `static decode`; the client handler is registered as a direct lambda because `ClientBlocked` references no client-only classes, so it is safe to classload on the dedicated server. Sent on player join and after each `SkillUnlock`/`SkillLock` recompute. The record's compact constructor deep-copies the map (with `Set.copyOf` per inner set) so the packet carries a snapshot; respec fires `SkillLock` rapidly and the Netty IO thread encodes earlier packets while the server thread is still mutating the live cache — without the snapshot the inner `HashSet` iteration races and throws `ConcurrentModificationException`. Wire format: per gate, a list of `(kind:byte, registryId:ResourceLocation)` entries.
- `client/ClientBlocked` — client-side mirror of the blocked map, populated by the packet handler. Same shape as the server-side cache.
- `client/ClientGateHandler` — `@Mod.EventBusSubscriber(value = Dist.CLIENT)`. Cancels `PlayerInteractEvent.RightClickItem` (use), `PlayerInteractEvent.LeftClickBlock` (break), `AttackEntityEvent` (attack), `PlayerInteractEvent.RightClickBlock` (interact on block — full `setCanceled(true)` so block placement is also denied when the targeted block is gated), `PlayerInteractEvent.EntityInteract` (interact on entity), and `PlayerInteractEvent.EntityInteractSpecific` (covers `interactAt` paths like saddling horses or equipping armor stands). Server-side `VanillaGateHandler` remains authoritative; the client handler just suppresses the misleading animations and shows the action-bar feedback.
- `enforcement/GateFeedback` — throttled (~1s per player) action-bar message when a gate blocks an action. Takes the `ItemGate` plus a `Component` for the name; the gate selects the translation key (`...locked` for item gates, `...interact_locked` for the `INTERACT` gate so the message reads "interact with X" rather than "use X"). Callers pass `stack.getHoverName()` / `block.getName()` / `type.getDescription()`. Entry cleared on logout. Returns early when `player.connection == null`: Curios evaluates `isItemValid` (and so posts `CurioEquipEvent`) while deserializing the player's capability inside `Entity.load`, which runs before the connection is assigned on login — sending a message there would NPE and crash the server. `client/ClientGateFeedback` is the client mirror and throttles on `Util.getMillis()` rather than game time, since a static game-time stamp carried from a long-running server would suppress feedback in the next world.
- `enforcement/Validation` — sweeps a player's worn armor (always) and curios (when Curios is loaded), removing items the player no longer satisfies (queries the cache) and returning them to the inventory. Used by `OnDatapackSyncEvent` (login + `/reload`) and Puffish's `SkillLock` event — moments where multiple slots may flip at once. `interact` is session-only and has no persistent state, so validation doesn't touch it.
- `events/VanillaGateHandler` — server-side Forge listeners. `AttackEntityEvent` / `BlockEvent.BreakEvent` / `PlayerInteractEvent.RightClickItem` for the original item gates. `PlayerInteractEvent.RightClickBlock` (full `setCanceled(true)` — denies both `Block#use` and `Item#useOn` so the player can't place an item-in-hand onto a gated block either; the handler skips when `player.isShiftKeyDown()` so sneak+right-click falls through to vanilla's behavior, which is how players can still place items adjacent to gated blocks). `PlayerInteractEvent.EntityInteract`, and `PlayerInteractEvent.EntityInteractSpecific` for the `interact` gate (entity gates do not honor sneak — there's no equivalent vanilla bypass). `LivingEquipmentChangeEvent` for `equip_armor`. All handlers skip creative and spectator players (operators can `/give` themselves anything anyway, and creative's separate slot-sync protocol breaks the deferred armor eject). For armor specifically, the ejection is *deferred* to the next `TickEvent.ServerTickEvent` at `Phase.START` so the slot mutation runs *after* `LivingEntity.detectEquipmentUpdates` has captured `lastArmorItemStacks[slot]` correctly. Mutating the slot during the event itself desyncs that tracking and causes alternating equip-success behavior on shift-click. `MinecraftServer.tell(new TickTask(...))` looks like it would work but `shouldRun` falls through to `haveTime()` and runs the task in the same tick whenever the server isn't lagging — `TickEvent.ServerTickEvent` at `Phase.START` is the only reliable way to defer to a future tick. The deferred task re-checks the slot (item may have changed or skill may have been unlocked in the meantime) before ejecting.
- `events/ValidationEventHandler` — `OnDatapackSyncEvent` (Forge's `getPlayer()` for a single joining player, else `getPlayerList().getPlayers()`) builds the cache (and runs `Validation`) for the joining player (login) or every online player (`/reload`). `PlayerRespawnEvent` rebuilds the cache and re-validates the new `ServerPlayer` instance, because Puffish's erase-on-death and `importPlayerData` paths change skill state **without** firing `SkillLock`/`SkillUnlock`. `PlayerChangeGameModeEvent` (fired *before* the mode flips, so `isCreative()` still reports the old mode) schedules `Validation` via `server.tell(new TickTask(...))` when leaving creative/spectator — same-tick execution is exactly what's wanted here, unlike the armor eject. `PlayerLoggedOutEvent` clears that player's cache, feedback throttle entry, and pending armor ejects.
- `setup/SkillEventsSetup` — `FMLCommonSetupEvent` registers Puffish's `SkillUnlock` (cache update only) and `SkillLock` (cache update plus `Validation`, so newly-blocked worn items eject immediately). Registration is wrapped in `event.enqueueWork` because `FMLCommonSetupEvent` dispatches on worker threads and Puffish's listener list is a plain `ArrayList`.
- `setup/CuriosSetup` + `compat/CuriosCompat` — optional Curios integration (see below).

## Datapack format

Rules load from `data/<namespace>/pufferfish_skill_gate_rules/*.json` across **all** namespaces (directory `pufferfish_skill_gate_rules`, handled by `ItemGatingReloadListener`). One file = one rule. The directory name is mod-specific so it cannot collide with anything else's datapack layout.

Fields:

- Exactly one of (each is a non-empty list of registry ids — single-entry lists are fine for single-target rules):
  - `items` — gated items, e.g. `["minecraft:diamond_sword", "minecraft:diamond_pickaxe"]`. Valid gates: `attack`, `break`, `use`, `equip_armor`, `equip_curio`.
  - `blocks` — gated blocks, e.g. `["minecraft:crafting_table"]`. Valid gate: `interact`.
  - `entities` — gated entity types, e.g. `["minecraft:villager"]`. Valid gate: `interact`.
- `gates` *(optional)* — which actions this rule gates. **Omit to gate the default set for the target type:** all five item gates for `item` targets, `[interact]` for `block` and `entity` targets. Gate/target combinations that don't make sense (e.g., `attack` on a block) cause the rule to be skipped with a warn log.
- **Strict parsing (1.20.1 note).** Vanilla 1.20.1's `byNameCodec()` resolves unknown ids to the registry default (`air`/`pig`) and DFU 6's `optionalFieldOf` silently drops a field that fails to decode (so `"gates": ["atack"]` would have meant "all gates"). `ItemGatingRule` therefore uses its own `strictByName` (registry `getOptional`) and `strictOptionalField` (`MapCodec` that propagates errors), so unknown ids, empty lists, and typos skip the rule with a warn log, matching the 1.21 behavior.
- `skills` *(required)* — list of skill requirements. Each entry is `{ "category": <ResourceLocation>, "skill": <string> }`, where `category` is the Pufferfish's Skills category id and `skill` is the skill id within it. **OR semantics: the player passes the rule if they have unlocked *any one* of the listed skills.**

The `interact` gate keys on the *Block* (or *EntityType*) — not the BlockState — so different states of the same block (powered/unpowered, water level, etc.) share gating. Same for entity types: a baby vs. adult villager share their gate.

Multiple rules may target the same item, block, or entity — typically to gate different actions (`gates`). When two or more rules apply to the same `(target, gate)` combination, the player passes if **any one** of them is satisfied (OR across rules, mirroring the OR within a rule).

Examples:

```json
// data/my_pack/pufferfish_skill_gate_rules/diamond_tools.json
{
  "items": ["minecraft:diamond_sword", "minecraft:diamond_pickaxe", "minecraft:diamond_axe"],
  "gates": ["attack", "break"],
  "skills": [
    { "category": "my_skills:combat", "skill": "swordsmanship" },
    { "category": "my_skills:combat", "skill": "blade_mastery" }
  ]
}

// data/my_pack/pufferfish_skill_gate_rules/crafting_table.json
{
  "blocks": ["minecraft:crafting_table"],
  "skills": [
    { "category": "my_skills:crafting", "skill": "basic_crafting" }
  ]
}

// data/my_pack/pufferfish_skill_gate_rules/villager.json
{
  "entities": ["minecraft:villager"],
  "skills": [
    { "category": "my_skills:social", "skill": "trading" }
  ]
}
```


## Curios compatibility (optional)

Curios is a **soft dependency**: `compileOnly fg.deobf(...)` against the `:api` artifact, declared `mandatory=false` in `mods.toml`. Nothing may touch a Curios class unless Curios is loaded, or the JVM throws `NoClassDefFoundError`.

- All Curios-referencing code lives in `compat/CuriosCompat`, which is **not** `@Mod.EventBusSubscriber`-annotated (that would classload it unconditionally).
- `setup/CuriosSetup#init` (an `FMLCommonSetupEvent` listener registered from the mod constructor) guards with `ModList.get().isLoaded("curios")`, then calls `CuriosCompat.initialize(MinecraftForge.EVENT_BUS)`. Lazy classloading means the Curios imports only link inside that branch.

What it enforces:

- `CurioEquipEvent` — Curios 5.x has no `CurioCanEquipEvent`/`TriState`; it posts this `@HasResult` event from `DynamicStackHandler.isItemValid` and treats `Result.DENY` as "stack not valid for the slot". `isItemValid` runs on **both** sides (menu slot `mayPlace` is evaluated on the client too), so one handler covers everything: a `ServerPlayer` is checked via `ItemGateEvaluator` + `GateFeedback`, any other `Player` (the client) via `ClientBlocked` + `ClientGateFeedback`. The client branch is what keeps the curios menu from visually accepting a drop the server will reject.
- `ejectInvalidCurios(ServerPlayer)` is called externally by `enforcement/Validation`. Its triggers — `OnDatapackSyncEvent` (login + `/reload`) and Puffish's `SkillLock` (in-session respec) — live in `events/ValidationEventHandler` and `setup/SkillEventsSetup` respectively, so the compat module stays focused on Curios-only logic. `extractItem` itself posts `CurioUnequipEvent`, so a `DENY` from another mod leaves the stack in place (handled by the `extracted.isEmpty()` check).

## Build & Run

- `./gradlew build` — compile and package the mod jar
- `./gradlew runClient` — launch a dev client with the mod loaded
- `./gradlew runServer` — launch a dedicated dev server (`--nogui`)
- `./gradlew runData` — run data generators (output to `src/generated/resources`)
- Working/run directory is `run/`. The master copy of the `gating_test` datapack lives in `claude_reference/test_datapacks/gating_test` (gitignored, survives world deletion) with a README test checklist; copy it into `run/saves/<world>/datapacks/` or `run/world/datapacks/` and it auto-enables on next load. It ships a Puffish category `gating_test:gating` with one skill per gate family, ~20 rules across every gate type (items of every kind, blocks, entities, OR-within-rule, OR-across-rules, default gates), seven deliberately invalid rules that must be skipped at load, one rule referencing a missing skill (fail-open at evaluation), and Curios `charm`/`ring` slots for players with vanilla items tagged into them. Puffish commands: `/puffish_skills skills unlock <player> gating_test:gating <skill>`, `.../skills lock ...`, `.../skills reset <player> gating_test:gating`.
- Sources for every Forge/MC API claim live in `~/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.10_mapped_parchment_2023.09.03-1.20.1/*-sources.jar` — `unzip -p` the class and read it rather than guessing.

## Conventions

- **Server-authoritative.** Every gating check needs a `ServerPlayer`; server handlers gate on `instanceof ServerPlayer` so logical-client dispatches fall through. `FakePlayer` extends `ServerPlayer` and has no cache entry, so automation holding gated items takes the uncached evaluation path and is gated like a player.
- **Missing-skill rules.** A rule whose skill id doesn't exist in a loaded category is skipped (fail-open, warned once per reload). If Puffish has *no* categories at all, rules stay enforceable and nobody can satisfy them (fail-closed) — this catches a broken Puffish config loudly rather than silently ungating everything.
- **Java 17.** No switch pattern matching, record patterns, or `_` variables; records, sealed interfaces, and `instanceof` binding are fine.

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
3. **Project Problems check** by running a whole project search in the problems / project tab. Have the user download and put this file into the claude_reference/problems to check. Ask before deploys if we should do this.

Don't skip step 2 because step 1 looked clean — the value of the independent reviewer is exactly that it doesn't share your blind spots.

## Version Control

**The user handles commits in git.** Never run `git add`, `git commit`, or `git push` — and don't suggest doing so — unless the user explicitly asks. Wrap up work by reporting what changed; staging and pushing are the user's job.
