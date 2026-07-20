package at.simulevski.weatherinducer.content.util;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A {@link ScrollValueBehaviour} that can coexist with siblings on the same
 * block entity. Create stores behaviours in a map keyed by their
 * {@link BehaviourType} and every stock scroll behaviour shares one static
 * type, so adding two to a block entity silently drops the first. This
 * subclass fixes the three collision points:
 * <ul>
 *   <li>{@link #getType()} returns a per-instance type, so the behaviour
 *       map keeps every box;</li>
 *   <li>{@link #netId()} returns a caller-chosen id, which the value
 *       settings packet uses to route a click to the right box;</li>
 *   <li>NBT is written under a per-key name instead of the shared
 *       {@code ScrollValue} tag.</li>
 * </ul>
 */
public class KeyedScrollValueBehaviour extends ScrollValueBehaviour {

    private final BehaviourType<ScrollValueBehaviour> type;
    private final String nbtKey;
    private final int packetId;

    // Shadow copies of the bounds: the originals are not visible here (min
    // is package-private in Create) but read() below needs them to clamp.
    // No initializers on purpose: between() runs from the super constructor,
    // and an initializer would wipe what it stored.
    private int minValue;
    private int maxValue;

    public KeyedScrollValueBehaviour(String key, int packetId, Component label,
                                     SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
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
        // pre-keyed save belongs to whichever sibling box wrote it last.
        // Never trust it: use this box's own tag, or keep the value we had.
        // Clamp regardless; setValue() guards every other write path, but
        // read() assigns the field raw, and an out-of-range index crashes
        // Create's option renderer.
        value = nbt.contains(nbtKey) ? nbt.getInt(nbtKey) : before;
        value = Mth.clamp(value, minValue, maxValue);
    }
}
