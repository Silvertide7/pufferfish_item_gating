package net.silvertide.pufferfish_item_gating.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.enforcement.GateFeedback;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;
import net.silvertide.pufferfish_item_gating.enforcement.Validation;

@EventBusSubscriber(modid = PufferfishItemGating.MODID)
public final class ValidationEventHandler {
    private ValidationEventHandler() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(player -> {
            ItemGateEvaluator.buildForPlayer(player);
            Validation.validatePlayer(player);
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemGateEvaluator.clearForPlayer(player.getUUID());
            GateFeedback.clearForPlayer(player.getUUID());
            VanillaGateHandler.clearForPlayer(player.getUUID());
        }
    }
}
