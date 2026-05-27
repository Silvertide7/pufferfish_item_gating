package net.silvertide.pufferfish_item_gating.config;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

public final class ItemGatingRules {
    private static volatile Map<Item, List<ItemGatingRule>> rulesByItem = Map.of();

    private ItemGatingRules() {
    }

    public static void replaceAll(Map<Item, List<ItemGatingRule>> rules) {
        rulesByItem = rules;
    }

    public static List<ItemGatingRule> forItem(Item item) {
        return rulesByItem.getOrDefault(item, List.of());
    }
}
