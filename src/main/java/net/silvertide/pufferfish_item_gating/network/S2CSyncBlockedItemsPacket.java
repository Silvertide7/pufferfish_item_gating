package net.silvertide.pufferfish_item_gating.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.GateTarget;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record S2CSyncBlockedItemsPacket(Map<ItemGate, Set<GateTarget>> blockedByGate) implements CustomPacketPayload {
    public S2CSyncBlockedItemsPacket {
        EnumMap<ItemGate, Set<GateTarget>> snapshot = new EnumMap<>(ItemGate.class);
        for (Map.Entry<ItemGate, Set<GateTarget>> entry : blockedByGate.entrySet()) {
            snapshot.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        blockedByGate = snapshot;
    }

    public static final CustomPacketPayload.Type<S2CSyncBlockedItemsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PufferfishItemGating.MODID, "sync_blocked_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncBlockedItemsPacket> STREAM_CODEC = StreamCodec.of(
            S2CSyncBlockedItemsPacket::encode,
            S2CSyncBlockedItemsPacket::decode);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, S2CSyncBlockedItemsPacket packet) {
        Map<ItemGate, Set<GateTarget>> map = packet.blockedByGate();
        buf.writeVarInt(map.size());
        for (Map.Entry<ItemGate, Set<GateTarget>> entry : map.entrySet()) {
            buf.writeEnum(entry.getKey());
            Set<GateTarget> targets = entry.getValue();
            buf.writeVarInt(targets.size());
            for (GateTarget target : targets) {
                GateTarget.writeTo(buf, target);
            }
        }
    }

    private static S2CSyncBlockedItemsPacket decode(RegistryFriendlyByteBuf buf) {
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
