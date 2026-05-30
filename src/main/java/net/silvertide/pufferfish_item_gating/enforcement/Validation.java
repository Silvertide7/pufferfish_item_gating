package net.silvertide.pufferfish_item_gating.enforcement;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.silvertide.pufferfish_item_gating.config.ItemGate;

public final class Validation {
    private Validation() {
    }

    public static void validatePlayer(ServerPlayer player) {
        if (player.isCreative()) {
            return;
        }
        validateArmor(player);
        if (ModList.get().isLoaded("curios")) {
            net.silvertide.pufferfish_item_gating.compat.CuriosCompat.ejectInvalidCurios(player);
        }
    }

    public static void validateAllPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            validatePlayer(player);
        }
    }

    private static void validateArmor(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.EQUIP_ARMOR)) {
                continue;
            }
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
