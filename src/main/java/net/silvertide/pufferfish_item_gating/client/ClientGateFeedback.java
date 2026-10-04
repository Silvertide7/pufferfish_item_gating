package net.silvertide.pufferfish_item_gating.client;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

public final class ClientGateFeedback {
    private static final long NOTIFY_COOLDOWN_MILLIS = 1000L;
    private static long nextAllowedMillis = Long.MIN_VALUE;

    private ClientGateFeedback() {
    }

    public static void notifyLocked(Player player, ItemGate gate, Component targetName) {
        long now = Util.getMillis();
        if (now < nextAllowedMillis) {
            return;
        }
        nextAllowedMillis = now + NOTIFY_COOLDOWN_MILLIS;
        player.displayClientMessage(Component.translatable(gate.lockedMessageKey(), targetName), true);
    }
}
