package net.silvertide.pufferfish_item_gating.network;

import net.minecraft.network.FriendlyByteBuf;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.GateTarget;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record S2CSyncBlockedItemsPacket(Map<ItemGate, Set<GateTarget>> blockedByGate) {
    public S2CSyncBlockedItemsPacket {
        EnumMap<ItemGate, Set<GateTarget>> snapshot = new EnumMap<>(ItemGate.class);
        for (Map.Entry<ItemGate, Set<GateTarget>> entry : blockedByGate.entrySet()) {
            snapshot.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        blockedByGate = snapshot;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(blockedByGate.size());
        for (Map.Entry<ItemGate, Set<GateTarget>> entry : blockedByGate.entrySet()) {
            buf.writeEnum(entry.getKey());
            Set<GateTarget> targets = entry.getValue();
            buf.writeVarInt(targets.size());
            for (GateTarget target : targets) {
                GateTarget.writeTo(buf, target);
            }
        }
    }

    public static S2CSyncBlockedItemsPacket decode(FriendlyByteBuf buf) {
        int gateCount = buf.readVarInt();
        EnumMap<ItemGate, Set<GateTarget>> map = new EnumMap<>(ItemGate.class);
        for (int i = 0; i < gateCount; i++) {
            ItemGate gate = buf.readEnum(ItemGate.class);
            int targetCount = buf.readVarInt();
            Set<GateTarget> targets = new HashSet<>();
            for (int j = 0; j < targetCount; j++) {
                int readerIndex = buf.readerIndex();
                Optional<GateTarget> target = GateTarget.readFrom(buf);
                if (target.isPresent()) {
                    targets.add(target.get());
                } else {
                    PufferfishItemGating.LOGGER.warn("Skipping unknown target in sync packet at buffer offset {}", readerIndex);
                }
            }
            map.put(gate, targets);
        }
        return new S2CSyncBlockedItemsPacket(map);
    }
}
