package net.silvertide.pufferfish_item_gating.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemGatingReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "item_gates";

    public ItemGatingReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Item, List<ItemGatingRule>> rulesByItem = new HashMap<>();
        Map<Block, List<ItemGatingRule>> rulesByBlock = new HashMap<>();
        Map<EntityType<?>, List<ItemGatingRule>> rulesByEntityType = new HashMap<>();
        Map<SkillRequirement, Set<GatePair>> entriesBySkill = new HashMap<>();
        Set<GatePair> allGatedEntries = new HashSet<>();
        int loadedCount = 0;
        int skippedCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            var parsed = ItemGatingRule.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> PufferfishItemGating.LOGGER.warn("Skipping item gating rule '{}': {}", id, error));
            if (parsed.isEmpty()) {
                skippedCount++;
                continue;
            }
            ItemGatingRule rule = parsed.get();
            indexRule(rule, rulesByItem, rulesByBlock, rulesByEntityType);
            for (ItemGate gate : rule.gates()) {
                GatePair pair = new GatePair(rule.target(), gate);
                allGatedEntries.add(pair);
                for (SkillRequirement requirement : rule.requiredSkills()) {
                    entriesBySkill.computeIfAbsent(requirement, key -> new HashSet<>()).add(pair);
                }
            }
            loadedCount++;
        }

        Map<Item, List<ItemGatingRule>> frozenItemRules = freezeLists(rulesByItem);
        Map<Block, List<ItemGatingRule>> frozenBlockRules = freezeLists(rulesByBlock);
        Map<EntityType<?>, List<ItemGatingRule>> frozenEntityRules = freezeLists(rulesByEntityType);
        Map<SkillRequirement, Set<GatePair>> frozenEntries = new HashMap<>();
        entriesBySkill.forEach((requirement, pairs) -> frozenEntries.put(requirement, Set.copyOf(pairs)));

        ItemGatingRules.replaceAll(
                Map.copyOf(frozenItemRules),
                Map.copyOf(frozenBlockRules),
                Map.copyOf(frozenEntityRules),
                Map.copyOf(frozenEntries),
                Set.copyOf(allGatedEntries));
        ItemGateEvaluator.onRulesReloaded();

        int targetCount = frozenItemRules.size() + frozenBlockRules.size() + frozenEntityRules.size();
        if (skippedCount > 0) {
            PufferfishItemGating.LOGGER.info("Loaded {} item gating rule(s) for {} target(s); skipped {} invalid rule(s)", loadedCount, targetCount, skippedCount);
        } else {
            PufferfishItemGating.LOGGER.info("Loaded {} item gating rule(s) for {} target(s)", loadedCount, targetCount);
        }
    }

    private static void indexRule(ItemGatingRule rule,
                                  Map<Item, List<ItemGatingRule>> rulesByItem,
                                  Map<Block, List<ItemGatingRule>> rulesByBlock,
                                  Map<EntityType<?>, List<ItemGatingRule>> rulesByEntityType) {
        switch (rule.target()) {
            case GateTarget.ItemTarget it -> rulesByItem.computeIfAbsent(it.value(), key -> new ArrayList<>()).add(rule);
            case GateTarget.BlockTarget bt -> rulesByBlock.computeIfAbsent(bt.value(), key -> new ArrayList<>()).add(rule);
            case GateTarget.EntityTypeTarget et -> rulesByEntityType.computeIfAbsent(et.value(), key -> new ArrayList<>()).add(rule);
        }
    }

    private static <K> Map<K, List<ItemGatingRule>> freezeLists(Map<K, List<ItemGatingRule>> mutable) {
        Map<K, List<ItemGatingRule>> frozen = new HashMap<>();
        mutable.forEach((key, rules) -> frozen.put(key, List.copyOf(rules)));
        return frozen;
    }
}
