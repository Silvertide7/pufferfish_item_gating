package net.silvertide.pufferfish_item_gating.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.silvertide.pufferfish_item_gating.config.GateTarget;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ClientBlocked {
    private static volatile Map<ItemGate, Set<GateTarget>> blocked = Map.of();

    private ClientBlocked() {
    }

    public static void replaceAll(Map<ItemGate, Set<GateTarget>> newBlocked) {
        EnumMap<ItemGate, Set<GateTarget>> copy = new EnumMap<>(ItemGate.class);
        newBlocked.forEach((gate, targets) -> copy.put(gate, Set.copyOf(targets)));
        blocked = copy;
    }

    public static boolean isAllowed(GateTarget target, ItemGate gate) {
        Set<GateTarget> gateBlocked = blocked.get(gate);
        return gateBlocked == null || !gateBlocked.contains(target);
    }

    public static boolean isAllowed(Item item, ItemGate gate) {
        return isAllowed(new GateTarget.ItemTarget(item), gate);
    }

    public static boolean isAllowed(Block block, ItemGate gate) {
        return isAllowed(new GateTarget.BlockTarget(block), gate);
    }

    public static boolean isAllowed(EntityType<?> type, ItemGate gate) {
        return isAllowed(new GateTarget.EntityTypeTarget(type), gate);
    }
}
