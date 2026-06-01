package net.silvertide.pufferfish_item_gating.config;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemGatingRules {
    private record Tables(Map<Item, List<ItemGatingRule>> rulesByItem,
                          Map<Block, List<ItemGatingRule>> rulesByBlock,
                          Map<EntityType<?>, List<ItemGatingRule>> rulesByEntityType,
                          Map<SkillRequirement, Set<GatePair>> entriesBySkill,
                          Set<GatePair> allGatedEntries) {
    }

    private static volatile Tables tables = new Tables(Map.of(), Map.of(), Map.of(), Map.of(), Set.of());

    private ItemGatingRules() {
    }

    public static void replaceAll(Map<Item, List<ItemGatingRule>> rulesByItem,
                                  Map<Block, List<ItemGatingRule>> rulesByBlock,
                                  Map<EntityType<?>, List<ItemGatingRule>> rulesByEntityType,
                                  Map<SkillRequirement, Set<GatePair>> entriesBySkill,
                                  Set<GatePair> allGatedEntries) {
        tables = new Tables(rulesByItem, rulesByBlock, rulesByEntityType, entriesBySkill, allGatedEntries);
    }

    public static List<ItemGatingRule> forItem(Item item) {
        return tables.rulesByItem.getOrDefault(item, List.of());
    }

    public static List<ItemGatingRule> forBlock(Block block) {
        return tables.rulesByBlock.getOrDefault(block, List.of());
    }

    public static List<ItemGatingRule> forEntityType(EntityType<?> type) {
        return tables.rulesByEntityType.getOrDefault(type, List.of());
    }

    public static Set<GatePair> forSkill(SkillRequirement requirement) {
        return tables.entriesBySkill.getOrDefault(requirement, Set.of());
    }

    public static Set<GatePair> allGatedEntries() {
        return tables.allGatedEntries;
    }
}
