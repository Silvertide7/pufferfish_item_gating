package net.silvertide.pufferfish_item_gating.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record SkillRequirement(ResourceLocation category, String skill) {
    public static final Codec<SkillRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("category").forGetter(SkillRequirement::category),
            Codec.STRING.fieldOf("skill").forGetter(SkillRequirement::skill)
    ).apply(instance, SkillRequirement::new));
}
