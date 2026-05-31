package net.silvertide.pufferfish_item_gating.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGatingReloadListener;

@EventBusSubscriber(modid = PufferfishItemGating.MODID)
public final class DataReloadEventHandler {
    private DataReloadEventHandler() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ItemGatingReloadListener());
    }
}
