package net.silvertide.pufferfish_item_gating.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.silvertide.pufferfish_item_gating.client.ClientBlockedItems;

public final class NetworkSetup {
    private NetworkSetup() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                S2CSyncBlockedItemsPacket.TYPE,
                S2CSyncBlockedItemsPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBlockedItems.replaceAll(payload.blockedByGate()))
        );
    }
}
