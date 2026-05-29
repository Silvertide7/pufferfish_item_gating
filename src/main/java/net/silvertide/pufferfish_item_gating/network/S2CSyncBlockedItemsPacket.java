package net.silvertide.pufferfish_item_gating.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record S2CSyncBlockedItemsPacket(Map<ItemGate, Set<Item>> blockedByGate) implements CustomPacketPayload {
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
        Map<ItemGate, Set<Item>> map = packet.blockedByGate();
        buf.writeVarInt(map.size());
        for (Map.Entry<ItemGate, Set<Item>> entry : map.entrySet()) {
            buf.writeEnum(entry.getKey());
            Set<Item> items = entry.getValue();
            buf.writeVarInt(items.size());
            for (Item item : items) {
                buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
            }
        }
    }

    private static S2CSyncBlockedItemsPacket decode(RegistryFriendlyByteBuf buf) {
        int gateCount = buf.readVarInt();
        Map<ItemGate, Set<Item>> map = new EnumMap<>(ItemGate.class);
        for (int i = 0; i < gateCount; i++) {
            ItemGate gate = buf.readEnum(ItemGate.class);
            int itemCount = buf.readVarInt();
            Set<Item> items = new HashSet<>();
            for (int j = 0; j < itemCount; j++) {
                ResourceLocation id = buf.readResourceLocation();
                items.add(BuiltInRegistries.ITEM.get(id));
            }
            map.put(gate, items);
        }
        return new S2CSyncBlockedItemsPacket(map);
    }
}
