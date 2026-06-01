package net.silvertide.pufferfish_item_gating.enforcement;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

public final class Validation {
    private Validation() {
    }

    public static void validatePlayer(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        validateArmor(player);
        if (ModList.get().isLoaded("curios")) {
            net.silvertide.pufferfish_item_gating.compat.CuriosCompat.ejectInvalidCurios(player);
        }
    }

    private static void validateArmor(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack inSlot = player.getItemBySlot(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            if (!ItemGateEvaluator.isBlocked(player, inSlot.getItem(), ItemGate.EQUIP_ARMOR)) {
                continue;
            }
            ItemStack ejected = inSlot.copy();
            player.setItemSlot(slot, ItemStack.EMPTY);
            GateFeedback.notifyLocked(player, ItemGate.EQUIP_ARMOR, ejected.getHoverName());
            player.getInventory().add(ejected);
            if (!ejected.isEmpty()) {
                player.drop(ejected, false);
            }
        }
    }
}
