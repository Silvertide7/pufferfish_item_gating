package net.silvertide.pufferfish_item_gating.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.client.ClientBlocked;

public final class NetworkSetup {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PufferfishItemGating.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private NetworkSetup() {
    }

    public static void register() {
        CHANNEL.messageBuilder(S2CSyncBlockedItemsPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSyncBlockedItemsPacket::encode)
                .decoder(S2CSyncBlockedItemsPacket::decode)
                .consumerMainThread((packet, context) -> ClientBlocked.replaceAll(packet.blockedByGate()))
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, S2CSyncBlockedItemsPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
