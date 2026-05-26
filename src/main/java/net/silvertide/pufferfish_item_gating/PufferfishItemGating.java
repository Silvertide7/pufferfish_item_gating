package net.silvertide.pufferfish_item_gating;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(PufferfishItemGating.MODID)
public class PufferfishItemGating {
    public static final String MODID = "pufferfish_item_gating";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PufferfishItemGating(IEventBus modEventBus, ModContainer modContainer) {
    }
}
