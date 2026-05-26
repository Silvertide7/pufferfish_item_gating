package net.silvertide.pufferfish_item_gating.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;

@EventBusSubscriber(modid= PufferfishItemGating.MODID)
public class EventHandler {

    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent.Pre damageEvent) {

    }
}
