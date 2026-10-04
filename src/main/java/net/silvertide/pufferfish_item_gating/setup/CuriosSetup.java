package net.silvertide.pufferfish_item_gating.setup;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CuriosSetup {
    private CuriosSetup() {
    }

    public static void init(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        net.silvertide.pufferfish_item_gating.compat.CuriosCompat.initialize(MinecraftForge.EVENT_BUS);
    }
}
