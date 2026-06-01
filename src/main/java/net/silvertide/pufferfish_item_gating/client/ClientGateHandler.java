package net.silvertide.pufferfish_item_gating.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (ClientBlocked.isBlocked(stack.getItem(), ItemGate.USE)) {
            event.setCanceled(true);
            ClientGateFeedback.notifyLocked(event.getEntity(), ItemGate.USE, stack.getHoverName());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (ClientBlocked.isBlocked(stack.getItem(), ItemGate.BREAK)) {
            event.setCanceled(true);
            ClientGateFeedback.notifyLocked(event.getEntity(), ItemGate.BREAK, stack.getHoverName());
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) {
            return;
        }
        ItemStack stack = event.getEntity().getMainHandItem();
        if (ClientBlocked.isBlocked(stack.getItem(), ItemGate.ATTACK)) {
            event.setCanceled(true);
            ClientGateFeedback.notifyLocked(event.getEntity(), ItemGate.ATTACK, stack.getHoverName());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) {
            return;
        }
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (ClientBlocked.isBlocked(block, ItemGate.INTERACT)) {
            event.setCanceled(true);
            ClientGateFeedback.notifyLocked(event.getEntity(), ItemGate.INTERACT, block.getName());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) {
            return;
        }
        EntityType<?> type = event.getTarget().getType();
        if (ClientBlocked.isBlocked(type, ItemGate.INTERACT)) {
            event.setCanceled(true);
            ClientGateFeedback.notifyLocked(event.getEntity(), ItemGate.INTERACT, type.getDescription());
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity().isCreative() || event.getEntity().isSpectator()) {
            return;
        }
        EntityType<?> type = event.getTarget().getType();
        if (ClientBlocked.isBlocked(type, ItemGate.INTERACT)) {
            event.setCanceled(true);
            ClientGateFeedback.notifyLocked(event.getEntity(), ItemGate.INTERACT, type.getDescription());
        }
    }
}
