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
        switch (target) {
            case ItemTarget it -> {
                buf.writeByte(0);
                buf.writeResourceLocation(it.registryId());
            }
            case BlockTarget bt -> {
                buf.writeByte(1);
                buf.writeResourceLocation(bt.registryId());
            }
            case EntityTypeTarget et -> {
                buf.writeByte(2);
                buf.writeResourceLocation(et.registryId());
            }
        }
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
