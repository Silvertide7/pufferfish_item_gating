package net.silvertide.pufferfish_item_gating.enforcement;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GateFeedback {
    private static final long NOTIFY_COOLDOWN_TICKS = 20L;
    private static final Map<UUID, Long> lastNotifiedGameTime = new HashMap<>();

    private GateFeedback() {
    }

    public static void notifyLocked(ServerPlayer player, ItemGate gate, Component targetName) {
        long gameTime = player.level().getGameTime();
        Long previous = lastNotifiedGameTime.get(player.getUUID());
        if (previous != null && gameTime - previous < NOTIFY_COOLDOWN_TICKS) {
            return;
        }
        lastNotifiedGameTime.put(player.getUUID(), gameTime);
        player.displayClientMessage(Component.translatable(gate.lockedMessageKey(), targetName), true);
    }

    public static void clearForPlayer(UUID uuid) {
        lastNotifiedGameTime.remove(uuid);
    }
}
