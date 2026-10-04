package net.silvertide.pufferfish_item_gating.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.world.level.GameType;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.enforcement.GateFeedback;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;
import net.silvertide.pufferfish_item_gating.enforcement.Validation;

import java.util.List;

@Mod.EventBusSubscriber(modid = PufferfishItemGating.MODID)
public final class ValidationEventHandler {
    private ValidationEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        List<ServerPlayer> players = event.getPlayer() != null ? List.of(event.getPlayer()) : event.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            ItemGateEvaluator.buildForPlayer(player);
            Validation.validatePlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemGateEvaluator.buildForPlayer(player);
            Validation.validatePlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        GameType newMode = event.getNewGameMode();
        if (newMode == GameType.CREATIVE || newMode == GameType.SPECTATOR) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.server;
        server.tell(new TickTask(server.getTickCount(), () -> Validation.validatePlayer(player)));
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
