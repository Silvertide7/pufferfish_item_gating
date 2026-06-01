package net.silvertide.pufferfish_item_gating.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGate;
import net.silvertide.pufferfish_item_gating.enforcement.GateFeedback;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
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
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.ATTACK)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, ItemGate.ATTACK, stack.getHoverName());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.BREAK)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, ItemGate.BREAK, stack.getHoverName());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!ItemGateEvaluator.isAllowed(player, stack.getItem(), ItemGate.USE)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, ItemGate.USE, stack.getHoverName());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            return;
        }
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!ItemGateEvaluator.isAllowed(player, block, ItemGate.INTERACT)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, ItemGate.INTERACT, block.getName());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        EntityType<?> type = event.getTarget().getType();
        if (!ItemGateEvaluator.isAllowed(player, type, ItemGate.INTERACT)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, ItemGate.INTERACT, type.getDescription());
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        EntityType<?> type = event.getTarget().getType();
        if (!ItemGateEvaluator.isAllowed(player, type, ItemGate.INTERACT)) {
            event.setCanceled(true);
            GateFeedback.notifyLocked(player, ItemGate.INTERACT, type.getDescription());
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
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
        EnumSet<EquipmentSlot> pending = pendingArmorEjects.computeIfAbsent(player.getUUID(), key -> EnumSet.noneOf(EquipmentSlot.class));
        pending.add(slot);
    }

    @SubscribeEvent
    public static void onServerPreTick(ServerTickEvent.Pre event) {
        if (pendingArmorEjects.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, EnumSet<EquipmentSlot>>> playerIt = pendingArmorEjects.entrySet().iterator();
        while (playerIt.hasNext()) {
            Map.Entry<UUID, EnumSet<EquipmentSlot>> entry = playerIt.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                playerIt.remove();
                continue;
            }
            for (EquipmentSlot slot : entry.getValue()) {
                ejectIfStillBlocked(player, slot);
            }
            playerIt.remove();
        }
    }

    private static void ejectIfStillBlocked(ServerPlayer player, EquipmentSlot slot) {
        if (player.isRemoved() || player.isCreative() || player.isSpectator()) {
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
        GateFeedback.notifyLocked(player, ItemGate.EQUIP_ARMOR, ejected.getHoverName());
        player.getInventory().add(ejected);
        if (!ejected.isEmpty()) {
            player.drop(ejected, false);
        }
    }
}
