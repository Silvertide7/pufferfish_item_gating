package net.silvertide.pufferfish_item_gating.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ItemGate implements StringRepresentable {
    ATTACK("attack"),
    BREAK("break"),
    USE("use"),
    EQUIP_ARMOR("equip_armor"),
    EQUIP_CURIO("equip_curio"),
    INTERACT("interact");

    public static final Codec<ItemGate> CODEC = StringRepresentable.fromEnum(ItemGate::values);

    private final String serializedName;

    ItemGate(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String lockedMessageKey() {
        return this == INTERACT
                ? "message.pufferfish_item_gating.interact_locked"
                : "message.pufferfish_item_gating.locked";
    }
}
