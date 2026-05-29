package net.silvertide.pufferfish_item_gating.config;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemGatingRules {
    private static volatile Map<Item, List<ItemGatingRule>> rulesByItem = Map.of();
    private static volatile Map<SkillRequirement, Set<ItemGatePair>> entriesBySkill = Map.of();
    private static volatile Set<ItemGatePair> allGatedEntries = Set.of();

    private ItemGatingRules() {
    }

    public static void replaceAll(Map<Item, List<ItemGatingRule>> rulesByItem,
                                  Map<SkillRequirement, Set<ItemGatePair>> entriesBySkill,
                                  Set<ItemGatePair> allGatedEntries) {
        ItemGatingRules.rulesByItem = rulesByItem;
        ItemGatingRules.entriesBySkill = entriesBySkill;
        ItemGatingRules.allGatedEntries = allGatedEntries;
    }

    public static List<ItemGatingRule> forItem(Item item) {
        return rulesByItem.getOrDefault(item, List.of());
    }

    public static Set<ItemGatePair> forSkill(SkillRequirement requirement) {
        return entriesBySkill.getOrDefault(requirement, Set.of());
    }

    public static Set<ItemGatePair> allGatedEntries() {
        return allGatedEntries;
    }
}
