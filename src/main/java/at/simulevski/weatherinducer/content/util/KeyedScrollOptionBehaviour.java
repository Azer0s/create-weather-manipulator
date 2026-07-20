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
import net.minecraft.util.Mth;

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

    // Shadow copies of the bounds for clamping in read(); Create's own min
    // field is package-private. No initializers on purpose: the super
    // constructor calls between(0, options - 1), and an initializer would
    // wipe what it stored.
    private int minValue;
    private int maxValue;

    public KeyedScrollOptionBehaviour(String key, int packetId, Class<E> enumClass,
                                      Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(enumClass, label, be, slot);
        this.type = new BehaviourType<>(key);
        this.nbtKey = "ScrollValue" + key;
        this.packetId = packetId;
    }

    @Override
    public ScrollValueBehaviour between(int min, int max) {
        minValue = min;
        maxValue = max;
        return super.between(min, max);
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
        int before = value;
        super.read(nbt, registries, clientPacket);
        // super.read pulled the legacy shared "ScrollValue" tag, which on a
        // pre-keyed save belongs to whichever sibling box wrote it last (a
        // lightning offset here, so possibly far outside this enum). Never
        // trust it: use this box's own tag, or keep the value we had. The
        // clamp matters: read() assigns the field raw, and Create's option
        // renderer indexes the enum array with it, so an out-of-range value
        // crashes the client on hover.
        value = nbt.contains(nbtKey) ? nbt.getInt(nbtKey) : before;
        value = Mth.clamp(value, minValue, maxValue);
    }
}
