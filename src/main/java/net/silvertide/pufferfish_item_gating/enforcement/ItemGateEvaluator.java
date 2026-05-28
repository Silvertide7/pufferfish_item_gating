package net.silvertide.pufferfish_item_gating.enforcement;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;
import net.silvertide.pufferfish_item_gating.config.ItemGate;
import net.silvertide.pufferfish_item_gating.config.ItemGatingRule;
import net.silvertide.pufferfish_item_gating.config.ItemGatingRules;
import net.silvertide.pufferfish_item_gating.config.SkillRequirement;

public final class ItemGateEvaluator {
    private ItemGateEvaluator() {
    }

    public static boolean isAllowed(ServerPlayer player, Item item, ItemGate gate) {
        for (ItemGatingRule rule : ItemGatingRules.forItem(item)) {
            if (rule.gates().contains(gate) && !satisfiesRule(player, rule)) {
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
