package net.silvertide.pufferfish_item_gating.setup;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.puffish.skillsmod.api.SkillsAPI;
import net.silvertide.pufferfish_item_gating.enforcement.ItemGateEvaluator;
import net.silvertide.pufferfish_item_gating.enforcement.Validation;

public final class SkillEventsSetup {
    private SkillEventsSetup() {
    }

    public static void init(FMLCommonSetupEvent event) {
        SkillsAPI.registerSkillUnlockEvent(ItemGateEvaluator::onSkillUnlock);
        SkillsAPI.registerSkillLockEvent((player, category, skillId) -> {
            ItemGateEvaluator.onSkillLock(player, category, skillId);
            Validation.validatePlayer(player);
        });
    }
}
