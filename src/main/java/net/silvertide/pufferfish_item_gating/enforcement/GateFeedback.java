package net.silvertide.pufferfish_item_gating.enforcement;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GateFeedback {
    private static final long NOTIFY_COOLDOWN_TICKS = 20L;
    private static final Map<UUID, Long> lastNotifiedGameTime = new HashMap<>();

    private GateFeedback() {
    }

    public static void notifyLocked(ServerPlayer player, ItemStack stack) {
        long gameTime = player.level().getGameTime();
        Long previous = lastNotifiedGameTime.get(player.getUUID());
        if (previous != null && gameTime - previous < NOTIFY_COOLDOWN_TICKS) {
            return;
        }
        lastNotifiedGameTime.put(player.getUUID(), gameTime);
        player.displayClientMessage(Component.translatable("message.pufferfish_item_gating.locked", stack.getHoverName()), true);
    }

    public static void clearForPlayer(UUID uuid) {
        lastNotifiedGameTime.remove(uuid);
    }
}
