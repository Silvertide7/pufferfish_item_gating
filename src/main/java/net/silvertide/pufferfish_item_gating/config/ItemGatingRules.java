package net.silvertide.pufferfish_item_gating.config;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemGatingRules {
    private record Tables(Map<Item, List<ItemGatingRule>> rulesByItem,
                          Map<SkillRequirement, Set<ItemGatePair>> entriesBySkill,
                          Set<ItemGatePair> allGatedEntries) {
    }

    private static volatile Tables tables = new Tables(Map.of(), Map.of(), Set.of());

    private ItemGatingRules() {
    }

    public static void replaceAll(Map<Item, List<ItemGatingRule>> rulesByItem,
                                  Map<SkillRequirement, Set<ItemGatePair>> entriesBySkill,
                                  Set<ItemGatePair> allGatedEntries) {
        tables = new Tables(rulesByItem, entriesBySkill, allGatedEntries);
    }

    public static List<ItemGatingRule> forItem(Item item) {
        return tables.rulesByItem.getOrDefault(item, List.of());
    }

    public static Set<ItemGatePair> forSkill(SkillRequirement requirement) {
        return tables.entriesBySkill.getOrDefault(requirement, Set.of());
    }

    public static Set<ItemGatePair> allGatedEntries() {
        return tables.allGatedEntries;
    }
}
