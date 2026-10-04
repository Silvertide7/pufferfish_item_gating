package net.silvertide.pufferfish_item_gating;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.silvertide.pufferfish_item_gating.network.NetworkSetup;
import net.silvertide.pufferfish_item_gating.setup.CuriosSetup;
import net.silvertide.pufferfish_item_gating.setup.SkillEventsSetup;

@Mod(PufferfishItemGating.MODID)
public class PufferfishItemGating {
    public static final String MODID = "pufferfish_item_gating";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PufferfishItemGating() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(CuriosSetup::init);
        modEventBus.addListener(SkillEventsSetup::init);
        NetworkSetup.register();
    }
}
