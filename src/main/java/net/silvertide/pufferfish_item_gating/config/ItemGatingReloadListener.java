package net.silvertide.pufferfish_item_gating.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;

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
        Map<SkillRequirement, Set<ItemGatePair>> entriesBySkill = new HashMap<>();
        Set<ItemGatePair> allGatedEntries = new HashSet<>();
        int ruleCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            var parsed = ItemGatingRule.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> PufferfishItemGating.LOGGER.error("Failed to load item gating rule '{}': {}", id, error));
            if (parsed.isPresent()) {
                ItemGatingRule rule = parsed.get();
                rulesByItem.computeIfAbsent(rule.item(), key -> new ArrayList<>()).add(rule);
                for (ItemGate gate : rule.gates()) {
                    ItemGatePair pair = new ItemGatePair(rule.item(), gate);
                    allGatedEntries.add(pair);
                    for (SkillRequirement requirement : rule.requiredSkills()) {
                        entriesBySkill.computeIfAbsent(requirement, key -> new HashSet<>()).add(pair);
                    }
                }
                ruleCount++;
            }
        }

        Map<Item, List<ItemGatingRule>> frozenRules = new HashMap<>();
        rulesByItem.forEach((item, rules) -> frozenRules.put(item, List.copyOf(rules)));
        Map<SkillRequirement, Set<ItemGatePair>> frozenEntries = new HashMap<>();
        entriesBySkill.forEach((requirement, pairs) -> frozenEntries.put(requirement, Set.copyOf(pairs)));

        ItemGatingRules.replaceAll(Map.copyOf(frozenRules), Map.copyOf(frozenEntries), Set.copyOf(allGatedEntries));

        PufferfishItemGating.LOGGER.info("Loaded {} item gating rule(s) for {} item(s)", ruleCount, frozenRules.size());
    }
}
