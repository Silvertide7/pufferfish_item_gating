package net.silvertide.pufferfish_item_gating.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record ItemGatingRule(GateTarget target, Set<ItemGate> gates, List<SkillRequirement> requiredSkills) {
    private static final Set<ItemGate> ITEM_GATES = Set.of(ItemGate.ATTACK, ItemGate.BREAK, ItemGate.USE, ItemGate.EQUIP_ARMOR, ItemGate.EQUIP_CURIO);
    private static final Set<ItemGate> BLOCK_GATES = Set.of(ItemGate.INTERACT);
    private static final Set<ItemGate> ENTITY_GATES = Set.of(ItemGate.INTERACT);

    private static <T> Codec<List<T>> nonEmptyListOf(Codec<T> elementCodec, String fieldName) {
        return elementCodec.listOf().flatXmap(
                list -> list.isEmpty()
                        ? DataResult.error(() -> "'" + fieldName + "' must not be empty")
                        : DataResult.success(list),
                DataResult::success);
    }

    private record RawRule(
            Optional<Item> item,
            Optional<Block> block,
            Optional<EntityType<?>> entity,
            Optional<List<ItemGate>> gates,
            List<SkillRequirement> requiredSkills
    ) {
    }

    private static final Codec<RawRule> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(RawRule::item),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(RawRule::block),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("entity").forGetter(RawRule::entity),
            nonEmptyListOf(ItemGate.CODEC, "gates").optionalFieldOf("gates").forGetter(RawRule::gates),
            nonEmptyListOf(SkillRequirement.CODEC, "skills").fieldOf("skills").forGetter(RawRule::requiredSkills)
    ).apply(instance, RawRule::new));

    public static final Codec<ItemGatingRule> CODEC = RAW_CODEC.flatXmap(
            ItemGatingRule::buildFromRaw,
            ItemGatingRule::toRaw);

    private static DataResult<ItemGatingRule> buildFromRaw(RawRule raw) {
        int present = (raw.item.isPresent() ? 1 : 0) + (raw.block.isPresent() ? 1 : 0) + (raw.entity.isPresent() ? 1 : 0);
        if (present != 1) {
            return DataResult.error(() -> "Exactly one of 'item', 'block', or 'entity' must be present");
        }
        GateTarget target;
        Set<ItemGate> defaultGates;
        if (raw.item.isPresent()) {
            target = new GateTarget.ItemTarget(raw.item.get());
            defaultGates = ITEM_GATES;
        } else if (raw.block.isPresent()) {
            target = new GateTarget.BlockTarget(raw.block.get());
            defaultGates = BLOCK_GATES;
        } else {
            target = new GateTarget.EntityTypeTarget(raw.entity.get());
            defaultGates = ENTITY_GATES;
        }
        Set<ItemGate> gates = raw.gates.<Set<ItemGate>>map(EnumSet::copyOf).orElse(defaultGates);
        Set<ItemGate> compatible = compatibleGates(target);
        for (ItemGate gate : gates) {
            if (!compatible.contains(gate)) {
                String targetType = target.typeName();
                return DataResult.error(() -> "Gate '" + gate.getSerializedName() + "' is not valid for target type '" + targetType + "'");
            }
        }
        return DataResult.success(new ItemGatingRule(target, Set.copyOf(gates), raw.requiredSkills));
    }

    private static DataResult<RawRule> toRaw(ItemGatingRule rule) {
        Optional<Item> item = rule.target instanceof GateTarget.ItemTarget(Item itemValue) ? Optional.of(itemValue) : Optional.empty();
        Optional<Block> block = rule.target instanceof GateTarget.BlockTarget(Block blockValue) ? Optional.of(blockValue) : Optional.empty();
        Optional<EntityType<?>> entity = rule.target instanceof GateTarget.EntityTypeTarget(EntityType<?> typeValue) ? Optional.of(typeValue) : Optional.empty();
        return DataResult.success(new RawRule(item, block, entity, Optional.of(List.copyOf(rule.gates)), rule.requiredSkills));
    }

    private static Set<ItemGate> compatibleGates(GateTarget target) {
        return switch (target) {
            case GateTarget.ItemTarget ignored -> ITEM_GATES;
            case GateTarget.BlockTarget ignored -> BLOCK_GATES;
            case GateTarget.EntityTypeTarget ignored -> ENTITY_GATES;
        };
    }
}
