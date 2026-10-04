package net.silvertide.pufferfish_item_gating.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public record ItemGatingRule(Set<GateTarget> targets, Set<ItemGate> gates, List<SkillRequirement> requiredSkills) {
    private static final Set<ItemGate> ITEM_GATES = Set.of(ItemGate.ATTACK, ItemGate.BREAK, ItemGate.USE, ItemGate.EQUIP_ARMOR, ItemGate.EQUIP_CURIO);
    private static final Set<ItemGate> BLOCK_GATES = Set.of(ItemGate.INTERACT);
    private static final Set<ItemGate> ENTITY_GATES = Set.of(ItemGate.INTERACT);

    private static <T> Codec<T> strictByName(Registry<T> registry) {
        return ResourceLocation.CODEC.flatXmap(
                id -> registry.getOptional(id)
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + registry.key() + ": " + id)),
                value -> DataResult.success(registry.getKey(value)));
    }

    private static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> codec, String name) {
        return new MapCodec<>() {
            @Override
            public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get(name);
                if (value == null) {
                    return DataResult.success(Optional.empty());
                }
                return codec.parse(ops, value).map(Optional::of);
            }

            @Override
            public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return input.map(value -> prefix.add(name, codec.encodeStart(ops, value))).orElse(prefix);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString(name));
            }
        };
    }

    private static <T> Codec<List<T>> nonEmptyListOf(Codec<T> elementCodec, String fieldName) {
        return elementCodec.listOf().flatXmap(
                list -> list.isEmpty()
                        ? DataResult.error(() -> "'" + fieldName + "' must not be empty")
                        : DataResult.success(list),
                DataResult::success);
    }

    private record RawRule(
            Optional<List<Item>> items,
            Optional<List<Block>> blocks,
            Optional<List<EntityType<?>>> entities,
            Optional<List<ItemGate>> gates,
            List<SkillRequirement> requiredSkills
    ) {
    }

    private static final Codec<RawRule> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            strictOptionalField(nonEmptyListOf(strictByName(BuiltInRegistries.ITEM), "items"), "items").forGetter(RawRule::items),
            strictOptionalField(nonEmptyListOf(strictByName(BuiltInRegistries.BLOCK), "blocks"), "blocks").forGetter(RawRule::blocks),
            strictOptionalField(nonEmptyListOf(strictByName(BuiltInRegistries.ENTITY_TYPE), "entities"), "entities").forGetter(RawRule::entities),
            strictOptionalField(nonEmptyListOf(ItemGate.CODEC, "gates"), "gates").forGetter(RawRule::gates),
            nonEmptyListOf(SkillRequirement.CODEC, "skills").fieldOf("skills").forGetter(RawRule::requiredSkills)
    ).apply(instance, RawRule::new));

    public static final Codec<ItemGatingRule> CODEC = RAW_CODEC.flatXmap(
            ItemGatingRule::buildFromRaw,
            ItemGatingRule::toRaw);

    private static DataResult<ItemGatingRule> buildFromRaw(RawRule raw) {
        int present = (raw.items.isPresent() ? 1 : 0) + (raw.blocks.isPresent() ? 1 : 0) + (raw.entities.isPresent() ? 1 : 0);
        if (present != 1) {
            return DataResult.error(() -> "Exactly one of 'items', 'blocks', or 'entities' must be present");
        }
        Set<GateTarget> targets = new LinkedHashSet<>();
        Set<ItemGate> compatibleGates;
        String kindName;
        if (raw.items.isPresent()) {
            for (Item item : raw.items.get()) {
                targets.add(new GateTarget.ItemTarget(item));
            }
            compatibleGates = ITEM_GATES;
            kindName = "item";
        } else if (raw.blocks.isPresent()) {
            for (Block block : raw.blocks.get()) {
                targets.add(new GateTarget.BlockTarget(block));
            }
            compatibleGates = BLOCK_GATES;
            kindName = "block";
        } else {
            for (EntityType<?> type : raw.entities.get()) {
                targets.add(new GateTarget.EntityTypeTarget(type));
            }
            compatibleGates = ENTITY_GATES;
            kindName = "entity";
        }
        Set<ItemGate> gates = raw.gates.<Set<ItemGate>>map(EnumSet::copyOf).orElse(compatibleGates);
        for (ItemGate gate : gates) {
            if (!compatibleGates.contains(gate)) {
                return DataResult.error(() -> "Gate '" + gate.getSerializedName() + "' is not valid for target type '" + kindName + "'");
            }
        }
        return DataResult.success(new ItemGatingRule(Set.copyOf(targets), Set.copyOf(gates), raw.requiredSkills));
    }

    private static DataResult<RawRule> toRaw(ItemGatingRule rule) {
        Optional<List<Item>> items = Optional.empty();
        Optional<List<Block>> blocks = Optional.empty();
        Optional<List<EntityType<?>>> entities = Optional.empty();
        GateTarget first = rule.targets.iterator().next();
        if (first instanceof GateTarget.ItemTarget) {
            List<Item> list = new ArrayList<>();
            for (GateTarget target : rule.targets) {
                list.add(((GateTarget.ItemTarget) target).value());
            }
            items = Optional.of(List.copyOf(list));
        } else if (first instanceof GateTarget.BlockTarget) {
            List<Block> list = new ArrayList<>();
            for (GateTarget target : rule.targets) {
                list.add(((GateTarget.BlockTarget) target).value());
            }
            blocks = Optional.of(List.copyOf(list));
        } else {
            List<EntityType<?>> list = new ArrayList<>();
            for (GateTarget target : rule.targets) {
                list.add(((GateTarget.EntityTypeTarget) target).value());
            }
            entities = Optional.of(List.copyOf(list));
        }
        return DataResult.success(new RawRule(items, blocks, entities, Optional.of(List.copyOf(rule.gates)), rule.requiredSkills));
    }
}
