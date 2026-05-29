package net.silvertide.pufferfish_item_gating.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Set;

public record ItemGatingRule(Item item, Set<ItemGate> gates, List<SkillRequirement> requiredSkills) {
    private static final List<ItemGate> ALL_GATES = List.of(ItemGate.values());

    private static <T> Codec<List<T>> nonEmptyListOf(Codec<T> elementCodec, String fieldName) {
        return elementCodec.listOf().flatXmap(
                list -> list.isEmpty()
                        ? DataResult.error(() -> "'" + fieldName + "' must not be empty")
                        : DataResult.success(list),
                DataResult::success);
    }

    public static final Codec<ItemGatingRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemGatingRule::item),
            nonEmptyListOf(ItemGate.CODEC, "gates").optionalFieldOf("gates", ALL_GATES).forGetter(rule -> List.copyOf(rule.gates())),
            nonEmptyListOf(SkillRequirement.CODEC, "skills").fieldOf("skills").forGetter(ItemGatingRule::requiredSkills)
    ).apply(instance, (item, gates, requiredSkills) -> new ItemGatingRule(item, Set.copyOf(gates), List.copyOf(requiredSkills))));
}
