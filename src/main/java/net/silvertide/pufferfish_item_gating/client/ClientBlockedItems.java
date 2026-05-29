package net.silvertide.pufferfish_item_gating.client;

import net.minecraft.world.item.Item;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ClientBlockedItems {
    private static volatile Map<ItemGate, Set<Item>> blocked = Map.of();

    private ClientBlockedItems() {
    }

    public static void replaceAll(Map<ItemGate, Set<Item>> newBlocked) {
        EnumMap<ItemGate, Set<Item>> copy = new EnumMap<>(ItemGate.class);
        newBlocked.forEach((gate, items) -> copy.put(gate, Set.copyOf(items)));
        blocked = copy;
    }

    public static boolean isAllowed(Item item, ItemGate gate) {
        Set<Item> gateBlocked = blocked.get(gate);
        return gateBlocked == null || !gateBlocked.contains(item);
    }
}
