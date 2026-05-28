package net.silvertide.pufferfish_item_gating.setup;

import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CuriosSetup {
    private CuriosSetup() {
    }

    public static void init(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("curios")) {
            net.silvertide.pufferfish_item_gating.compat.CuriosCompat.initialize(NeoForge.EVENT_BUS);
        }
    }
}
