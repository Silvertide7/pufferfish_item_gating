package net.silvertide.pufferfish_item_gating.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.client.ClientBlocked;
import net.silvertide.pufferfish_item_gating.client.ClientGateFeedback;
import net.silvertide.pufferfish_item_gating.config.ItemGate;
import net.silvertide.pufferfish_item_gating.enforcement.GateFeedback;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static void initialize(IEventBus gameEventBus) {
        gameEventBus.addListener(CuriosCompat::onCurioEquip);
        PufferfishItemGating.LOGGER.info("Curios detected; enforcing the equip_curio gate.");
    }

    private static void onCurioEquip(CurioEquipEvent event) {
        if (event.getResult() == Event.Result.DENY) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack stack = event.getStack();
        if (player instanceof ServerPlayer serverPlayer) {
            if (ItemGateEvaluator.isBlocked(serverPlayer, stack.getItem(), ItemGate.EQUIP_CURIO)) {
                event.setResult(Event.Result.DENY);
                GateFeedback.notifyLocked(serverPlayer, ItemGate.EQUIP_CURIO, stack.getHoverName());
            }
        } else if (ClientBlocked.isBlocked(stack.getItem(), ItemGate.EQUIP_CURIO)) {
            event.setResult(Event.Result.DENY);
            ClientGateFeedback.notifyLocked(player, ItemGate.EQUIP_CURIO, stack.getHoverName());
        }
    }

    public static void ejectInvalidCurios(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(curiosInventory -> {
            for (ICurioStacksHandler stacksHandler : curiosInventory.getCurios().values()) {
                ejectInvalidStacks(player, stacksHandler.getStacks());
                ejectInvalidStacks(player, stacksHandler.getCosmeticStacks());
            }
        });
    }

    private static void ejectInvalidStacks(ServerPlayer player, IDynamicStackHandler stacks) {
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack stack = stacks.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!ItemGateEvaluator.isBlocked(player, stack.getItem(), ItemGate.EQUIP_CURIO)) {
                continue;
            }
            ItemStack extracted = stacks.extractItem(slot, stack.getCount(), false);
            if (extracted.isEmpty()) {
                continue;
            }
            player.getInventory().add(extracted);
            if (!extracted.isEmpty()) {
                player.drop(extracted, false);
            }
        }
    }
}
