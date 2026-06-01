package net.silvertide.pufferfish_item_gating.enforcement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.GatePair;
import net.silvertide.pufferfish_item_gating.config.GateTarget;
import net.silvertide.pufferfish_item_gating.config.ItemGate;
import net.silvertide.pufferfish_item_gating.config.ItemGatingRule;
import net.silvertide.pufferfish_item_gating.config.ItemGatingRules;
import net.silvertide.pufferfish_item_gating.config.SkillRequirement;
import net.silvertide.pufferfish_item_gating.network.S2CSyncBlockedItemsPacket;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemGateEvaluator {
    private static final Map<UUID, EnumMap<ItemGate, Set<GateTarget>>> blockedByPlayer = new HashMap<>();
    private static final Set<SkillRequirement> warnedMissingSkills = ConcurrentHashMap.newKeySet();

    private ItemGateEvaluator() {
    }

    public static boolean isAllowed(ServerPlayer player, GateTarget target, ItemGate gate) {
        EnumMap<ItemGate, Set<GateTarget>> playerBlocked = blockedByPlayer.get(player.getUUID());
        if (playerBlocked == null) {
            return evaluateFromScratch(player, target, gate);
        }
        Set<GateTarget> blocked = playerBlocked.get(gate);
        return blocked == null || !blocked.contains(target);
    }

    public static boolean isAllowed(ServerPlayer player, Item item, ItemGate gate) {
        return isAllowed(player, new GateTarget.ItemTarget(item), gate);
    }

    public static boolean isAllowed(ServerPlayer player, Block block, ItemGate gate) {
        return isAllowed(player, new GateTarget.BlockTarget(block), gate);
    }

    public static boolean isAllowed(ServerPlayer player, EntityType<?> type, ItemGate gate) {
        return isAllowed(player, new GateTarget.EntityTypeTarget(type), gate);
    }

    public static void buildForPlayer(ServerPlayer player) {
        EnumMap<ItemGate, Set<GateTarget>> playerBlocked = new EnumMap<>(ItemGate.class);
        for (GatePair pair : ItemGatingRules.allGatedEntries()) {
            if (!evaluateFromScratch(player, pair.target(), pair.gate())) {
                playerBlocked.computeIfAbsent(pair.gate(), key -> new HashSet<>()).add(pair.target());
            }
        }
        blockedByPlayer.put(player.getUUID(), playerBlocked);
        syncToClient(player, playerBlocked);
    }

    public static void clearForPlayer(UUID uuid) {
        blockedByPlayer.remove(uuid);
    }

    public static void onRulesReloaded() {
        warnedMissingSkills.clear();
    }

    public static void onSkillUnlock(ServerPlayer player, ResourceLocation category, String skillId) {
        recomputeAffected(player, new SkillRequirement(category, skillId));
    }

    public static void onSkillLock(ServerPlayer player, ResourceLocation category, String skillId) {
        recomputeAffected(player, new SkillRequirement(category, skillId));
    }

    private static void recomputeAffected(ServerPlayer player, SkillRequirement requirement) {
        Set<GatePair> affected = ItemGatingRules.forSkill(requirement);
        if (affected.isEmpty()) {
            return;
        }
        EnumMap<ItemGate, Set<GateTarget>> playerBlocked = blockedByPlayer.get(player.getUUID());
        if (playerBlocked == null) {
            return;
        }
        for (GatePair pair : affected) {
            boolean allowed = evaluateFromScratch(player, pair.target(), pair.gate());
            Set<GateTarget> gateBlocked = playerBlocked.get(pair.gate());
            if (allowed) {
                if (gateBlocked != null) {
                    gateBlocked.remove(pair.target());
                }
            } else {
                if (gateBlocked == null) {
                    gateBlocked = new HashSet<>();
                    playerBlocked.put(pair.gate(), gateBlocked);
                }
                gateBlocked.add(pair.target());
            }
        }
        syncToClient(player, playerBlocked);
    }

    private static void syncToClient(ServerPlayer player, EnumMap<ItemGate, Set<GateTarget>> blocked) {
        PacketDistributor.sendToPlayer(player, new S2CSyncBlockedItemsPacket(blocked));
    }

    private static boolean evaluateFromScratch(ServerPlayer player, GateTarget target, ItemGate gate) {
        List<ItemGatingRule> rules = rulesFor(target);
        boolean hasApplicableRule = false;
        for (ItemGatingRule rule : rules) {
            if (!rule.gates().contains(gate)) {
                continue;
            }
            if (!isRuleEnforceable(rule)) {
                continue;
            }
            hasApplicableRule = true;
            if (satisfiesRule(player, rule)) {
                return true;
            }
        }
        return !hasApplicableRule;
    }

    private static List<ItemGatingRule> rulesFor(GateTarget target) {
        return switch (target) {
            case GateTarget.ItemTarget it -> ItemGatingRules.forItem(it.value());
            case GateTarget.BlockTarget bt -> ItemGatingRules.forBlock(bt.value());
            case GateTarget.EntityTypeTarget et -> ItemGatingRules.forEntityType(et.value());
        };
    }

    private static boolean isRuleEnforceable(ItemGatingRule rule) {
        if (SkillsAPI.streamCategories().findAny().isEmpty()) {
            return true;
        }
        for (SkillRequirement requirement : rule.requiredSkills()) {
            if (SkillsAPI.getCategory(requirement.category())
                    .flatMap(category -> category.getSkill(requirement.skill()))
                    .isEmpty()) {
                if (warnedMissingSkills.add(requirement)) {
                    PufferfishItemGating.LOGGER.warn(
                            "Item gating rule(s) reference unknown skill '{}/{}'; those rules will be ignored",
                            requirement.category(), requirement.skill());
                }
                return false;
            }
        }
        return true;
    }

    private static boolean satisfiesRule(ServerPlayer player, ItemGatingRule rule) {
        return rule.requiredSkills().stream().anyMatch(requirement -> hasUnlockedSkill(player, requirement));
    }

    private static boolean hasUnlockedSkill(ServerPlayer player, SkillRequirement requirement) {
        return SkillsAPI.getCategory(requirement.category())
                .flatMap(category -> category.getSkill(requirement.skill()))
                .map(skill -> skill.getState(player) == Skill.State.UNLOCKED)
                .orElse(false);
    }
}
