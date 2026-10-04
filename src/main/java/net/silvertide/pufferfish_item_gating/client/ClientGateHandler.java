package net.silvertide.pufferfish_item_gating.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

@Mod.EventBusSubscriber(modid = PufferfishItemGating.MODID, value = Dist.CLIENT)
public final class ClientGateHandler {
    private ClientGateHandler() {
    }

    private static boolean isExemptOrServerSide(Player player) {
        return !player.level().isClientSide() || player.isCreative() || player.isSpectator();
    }

    private static boolean blockItem(Player player, ItemStack stack, ItemGate gate) {
        if (!ClientBlocked.isBlocked(stack.getItem(), gate)) {
            return false;
        }
        ClientGateFeedback.notifyLocked(player, gate, stack.getHoverName());
        return true;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isExemptOrServerSide(event.getEntity())) {
            return;
        }
        if (blockItem(event.getEntity(), event.getItemStack(), ItemGate.USE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isExemptOrServerSide(event.getEntity())) {
            return;
        }
        if (blockItem(event.getEntity(), event.getItemStack(), ItemGate.BREAK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isExemptOrServerSide(event.getEntity())) {
            return;
        }
        if (blockItem(event.getEntity(), event.getEntity().getMainHandItem(), ItemGate.ATTACK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isExemptOrServerSide(event.getEntity()) || event.getEntity().isShiftKeyDown()) {
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
        blockEntityInteract(event, event.getTarget().getType());
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        blockEntityInteract(event, event.getTarget().getType());
    }

    private static void blockEntityInteract(PlayerInteractEvent event, EntityType<?> type) {
        if (isExemptOrServerSide(event.getEntity())) {
            return;
        }
        if (ClientBlocked.isBlocked(type, ItemGate.INTERACT)) {
            event.setCanceled(true);
            ClientGateFeedback.notifyLocked(event.getEntity(), ItemGate.INTERACT, type.getDescription());
        }
    }
}
