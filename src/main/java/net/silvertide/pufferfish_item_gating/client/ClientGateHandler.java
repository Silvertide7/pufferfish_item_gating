package net.silvertide.pufferfish_item_gating.client;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

@EventBusSubscriber(modid = PufferfishItemGating.MODID, value = Dist.CLIENT)
public final class ClientGateHandler {
    private ClientGateHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!ClientBlockedItems.isAllowed(stack.getItem(), ItemGate.USE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!ClientBlockedItems.isAllowed(stack.getItem(), ItemGate.BREAK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (!ClientBlockedItems.isAllowed(stack.getItem(), ItemGate.ATTACK)) {
            event.setCanceled(true);
        }
    }
}
