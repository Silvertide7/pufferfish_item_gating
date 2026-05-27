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
import java.util.List;
import java.util.Map;

public class ItemGatingReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "item_gates";

    public ItemGatingReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Item, List<ItemGatingRule>> rulesByItem = new HashMap<>();
        int ruleCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            var parsed = ItemGatingRule.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> PufferfishItemGating.LOGGER.error("Failed to load item gating rule '{}': {}", id, error));
            if (parsed.isPresent()) {
                ItemGatingRule rule = parsed.get();
                rulesByItem.computeIfAbsent(rule.item(), key -> new ArrayList<>()).add(rule);
                ruleCount++;
            }
        }

        Map<Item, List<ItemGatingRule>> frozen = new HashMap<>();
        rulesByItem.forEach((item, rules) -> frozen.put(item, List.copyOf(rules)));
        ItemGatingRules.replaceAll(Map.copyOf(frozen));

        PufferfishItemGating.LOGGER.info("Loaded {} item gating rule(s) for {} item(s)", ruleCount, frozen.size());
    }
}
