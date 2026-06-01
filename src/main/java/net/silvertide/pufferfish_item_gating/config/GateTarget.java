package net.silvertide.pufferfish_item_gating.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public sealed interface GateTarget {
    ResourceLocation registryId();

    String typeName();

    record ItemTarget(net.minecraft.world.item.Item value) implements GateTarget {
        @Override
        public ResourceLocation registryId() {
            return BuiltInRegistries.ITEM.getKey(value);
        }

        @Override
        public String typeName() {
            return "item";
        }
    }

    record BlockTarget(net.minecraft.world.level.block.Block value) implements GateTarget {
        @Override
        public ResourceLocation registryId() {
            return BuiltInRegistries.BLOCK.getKey(value);
        }

        @Override
        public String typeName() {
            return "block";
        }
    }

    record EntityTypeTarget(net.minecraft.world.entity.EntityType<?> value) implements GateTarget {
        @Override
        public ResourceLocation registryId() {
            return BuiltInRegistries.ENTITY_TYPE.getKey(value);
        }

        @Override
        public String typeName() {
            return "entity";
        }
    }

    static void writeTo(RegistryFriendlyByteBuf buf, GateTarget target) {
        byte kind = switch (target) {
            case ItemTarget ignored -> (byte) 0;
            case BlockTarget ignored -> (byte) 1;
            case EntityTypeTarget ignored -> (byte) 2;
        };
        buf.writeByte(kind);
        buf.writeResourceLocation(target.registryId());
    }

    static Optional<GateTarget> readFrom(RegistryFriendlyByteBuf buf) {
        byte kind = buf.readByte();
        ResourceLocation id = buf.readResourceLocation();
        return switch (kind) {
            case 0 -> BuiltInRegistries.ITEM.getOptional(id).map(ItemTarget::new);
            case 1 -> BuiltInRegistries.BLOCK.getOptional(id).map(BlockTarget::new);
            case 2 -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(EntityTypeTarget::new);
            default -> Optional.empty();
        };
    }
}
