package net.silvertide.pufferfish_item_gating.events;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.silvertide.pufferfish_item_gating.PufferfishItemGating;
import net.silvertide.pufferfish_item_gating.config.ItemGatingReloadListener;

@Mod.EventBusSubscriber(modid = PufferfishItemGating.MODID)
public final class DataReloadEventHandler {
    private DataReloadEventHandler() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ItemGatingReloadListener());
    }
}
