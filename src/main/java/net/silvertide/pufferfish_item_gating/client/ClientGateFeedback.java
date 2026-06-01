package net.silvertide.pufferfish_item_gating.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

public final class ClientGateFeedback {
    private static final long NOTIFY_COOLDOWN_TICKS = 20L;
    private static Long lastNotifiedGameTime = null;

    private ClientGateFeedback() {
    }

    public static void notifyLocked(Player player, ItemGate gate, Component targetName) {
        long gameTime = player.level().getGameTime();
        if (lastNotifiedGameTime != null && gameTime - lastNotifiedGameTime < NOTIFY_COOLDOWN_TICKS) {
            return;
        }
        lastNotifiedGameTime = gameTime;
        player.displayClientMessage(Component.translatable(gate.lockedMessageKey(), targetName), true);
    }
}
