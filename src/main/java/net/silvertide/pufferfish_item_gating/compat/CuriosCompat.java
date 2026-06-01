package net.silvertide.pufferfish_item_gating.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGate;
import net.silvertide.pufferfish_item_gating.enforcement.GateFeedback;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static void initialize(IEventBus gameEventBus) {
        gameEventBus.addListener(CuriosCompat::onCurioCanEquip);
        PufferfishItemGating.LOGGER.info("Curios detected; enforcing the equip_curio gate.");
    }

    private static void onCurioCanEquip(CurioCanEquipEvent event) {
        if (event.getEquipResult() == TriState.FALSE) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack stack = event.getStack();
        if (!ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.EQUIP_CURIO)) {
            event.setEquipResult(TriState.FALSE);
            GateFeedback.notifyLocked(player, ItemGate.EQUIP_CURIO, stack.getHoverName());
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
            if (ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.EQUIP_CURIO)) {
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
