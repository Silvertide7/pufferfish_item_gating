package net.silvertide.pufferfish_item_gating.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGate;
import net.silvertide.pufferfish_item_gating.enforcement.GateFeedback;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = PufferfishItemGating.MODID)
public final class VanillaGateHandler {
    private static final Map<UUID, EnumSet<EquipmentSlot>> pendingArmorEjects = new HashMap<>();

    private VanillaGateHandler() {
    }

    public static void clearForPlayer(UUID uuid) {
        pendingArmorEjects.remove(uuid);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.ATTACK)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, stack);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.BREAK)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, stack);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.USE)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, stack);
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EquipmentSlot slot = event.getSlot();
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return;
        }
        ItemStack newStack = event.getTo();
        if (newStack.isEmpty()) {
            return;
        }
        if (event.getFrom().getItem() == newStack.getItem()) {
            return;
        }
        if (ItemGateEvaluator.isAllowed(player, newStack.getItem(), ItemGate.EQUIP_ARMOR)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        EnumSet<EquipmentSlot> pending = pendingArmorEjects.computeIfAbsent(player.getUUID(), key -> EnumSet.noneOf(EquipmentSlot.class));
        if (!pending.add(slot)) {
            return;
        }
        server.tell(new TickTask(server.getTickCount() + 1, () -> ejectIfStillBlocked(player, slot)));
    }

    private static void ejectIfStillBlocked(ServerPlayer player, EquipmentSlot slot) {
        EnumSet<EquipmentSlot> pending = pendingArmorEjects.get(player.getUUID());
        if (pending != null) {
            pending.remove(slot);
        }
        if (player.isRemoved()) {
            return;
        }
        ItemStack inSlot = player.getItemBySlot(slot);
        if (inSlot.isEmpty()) {
            return;
        }
        if (ItemGateEvaluator.isAllowed(player, inSlot.getItem(), ItemGate.EQUIP_ARMOR)) {
            return;
        }
        ItemStack ejected = inSlot.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        GateFeedback.notifyLocked(player, ejected);
        player.getInventory().add(ejected);
        if (!ejected.isEmpty()) {
            player.drop(ejected, false);
        }
    }
}
