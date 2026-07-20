package at.simulevski.weatherinducer.content.util;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * {@link ScrollOptionBehaviour} variant of
 * {@link KeyedScrollValueBehaviour}: a per-instance behaviour type, a
 * caller-chosen packet id and a private NBT key, so an option box (like the
 * Weather Inducer's mode selector) survives next to other scroll boxes on
 * the same block entity instead of being dropped from Create's type-keyed
 * behaviour map.
 */
public class KeyedScrollOptionBehaviour<E extends Enum<E> & INamedIconOptions>
        extends ScrollOptionBehaviour<E> {

    private final BehaviourType<ScrollValueBehaviour> type;
    private final String nbtKey;
    private final int packetId;

    public KeyedScrollOptionBehaviour(String key, int packetId, Class<E> enumClass,
                                      Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(enumClass, label, be, slot);
        this.type = new BehaviourType<>(key);
        this.nbtKey = "ScrollValue" + key;
        this.packetId = packetId;
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    @Override
    public int netId() {
        return packetId;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putInt(nbtKey, value);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        if (nbt.contains(nbtKey)) {
            value = nbt.getInt(nbtKey);
        }
    }
}
