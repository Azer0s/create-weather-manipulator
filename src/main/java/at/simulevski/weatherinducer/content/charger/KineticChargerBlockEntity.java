package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.content.inducer.ChargeTimeScrollBehaviour;
import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Battery logic for the Kinetic Charger.
 *
 * <p>The block has two working sides. The <b>I/O face</b> (the front, with
 * the teal ring) is where power flows in and out: an external source
 * charges the battery through it, and once that source stops, the charger
 * itself drives the same face, powering whatever hangs off it. The
 * <b>flywheel face</b> (the back) carries the flywheel bank that sets the
 * capacity, and the wheels keep turning as long as the battery holds
 * energy, whichever way it is flowing.
 *
 * <p>The buffer stores SU-seconds, like a watt-hour meter: while an
 * external source drives the I/O side, the charger loads the network with
 * its capacity divided by its charge time (in SU) and banks that many
 * SU-seconds each second. While it discharges, the machines' stress drains
 * the buffer per second, and holding charge at all bleeds a neutral
 * {@link #NEUTRAL_DRAIN_PER_SECOND} SU-seconds per second on top. A
 * 104,858 SU-second wheel therefore runs a 1,024 SU load for a little
 * over 101 seconds.
 *
 * <p>Whether the charger is charging or discharging is decided by the
 * network itself: if any other source powers the network, the charger
 * banks; if the charger is the only source left and the buffer holds
 * energy, it generates at the speed it was last charged with. No faces
 * ever disconnect. The buffer fill is published as a 0..15 redstone signal
 * via {@link KineticChargerBlock#POWER}.
 */
public class KineticChargerBlockEntity extends GeneratingKineticBlockEntity
        implements IHaveGoggleInformation {

    /** Buffer capacity with no flywheels attached: barely a sip. */
    public static final double BASE_CAPACITY = 2_048.0; // 2^11 SU-seconds

    /** Capacity each flywheel on the flywheel side adds (2^20 / 10). */
    public static final double FLYWHEEL_CAPACITY = 104_857.6;

    /** Flywheels beyond this jam the charger and overstress the network. */
    public static final int MAX_FLYWHEELS = 10;

    /** The largest buffer any flywheel bank allows. */
    public static final double MAX_BUFFER = BASE_CAPACITY + MAX_FLYWHEELS * FLYWHEEL_CAPACITY;

    /** Discharge offer ceiling per tick for direct buffer draws. */
    public static final double MAX_RATE_PER_TICK = 131_072.0; // 2^17

    /** SU-seconds the battery bleeds per second just for holding charge. */
    public static final double NEUTRAL_DRAIN_PER_SECOND = 5.0;

    /** Width of the goggle fill bar, in text segments. */
    private static final int BAR_SEGMENTS = 20;

    /** SU the battery provides while it is the network's source. */
    public static final float DISCHARGE_CAPACITY = 131_072f; // 2^17

    private double buffer;
    private double lastSyncedBuffer;

    /** The last non-zero externally driven speed; discharge repeats it. */
    private float chargeSpeed;

    private ChargeTimeScrollBehaviour chargeTime;

    /** Cached flywheel count on the flywheel side, recounted twice a second. */
    private int flywheels;

    /** True while some other source powers our network (cached each tick). */
    private boolean externallyPowered;

    public KineticChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        // Charge time slider on the four faces beside the shaft; locked
        // while the buffer holds anything, like the inducer's.
        chargeTime = new ChargeTimeScrollBehaviour(this,
                new SideValueBoxTransform((state, dir) ->
                        dir.getAxis() != state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getAxis()));
        chargeTime.onlyActiveWhen(() -> buffer <= 0);
        behaviours.add(chargeTime);
    }

    /** The configured full-buffer duration in seconds (10 at the fastest). */
    public int getChargeTimeSeconds() {
        return ChargeTimeScrollBehaviour.seconds(chargeTime != null ? chargeTime.getValue() : 0);
    }

    /** SU-seconds banked per tick so a full buffer takes the set time. */
    public double intakePerTick() {
        return getMaxBuffer() / (getChargeTimeSeconds() * 20.0);
    }

    /** The stress this battery loads its network with while filling. */
    public double chargeLoad() {
        return getMaxBuffer() / getChargeTimeSeconds();
    }

    /** Flywheels currently banked on the flywheel side (uncapped count). */
    public int getFlywheels() {
        return flywheels;
    }

    /**
     * The SU-seconds this flywheel bank can hold: 2,048 bare, plus
     * 104,857.6 per flywheel up to ten of them. More than ten does not
     * extend it further; it jams the charger instead (see
     * {@link #targetImpact}).
     */
    public double getMaxBuffer() {
        return BASE_CAPACITY + Math.min(flywheels, MAX_FLYWHEELS) * FLYWHEEL_CAPACITY;
    }

    /** The I/O face: power in from the source, power out to the machines. */
    public Direction getIoFace() {
        return getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }

    /** The flywheel face: the capacity bank hangs off the back. */
    public Direction getFlywheelFace() {
        return getIoFace().getOpposite();
    }

    /**
     * Counts flywheels physically connected on the flywheel side: a
     * bounded flood over kinetic blocks starting behind the back face, so
     * a bank of wheels in any arrangement counts, but nothing across the
     * charger.
     */
    private int countFlywheels() {
        if (level == null) {
            return 0;
        }
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(worldPosition.relative(getFlywheelFace()));
        int count = 0;
        while (!queue.isEmpty() && visited.size() < 64) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity member)) {
                continue;
            }
            if (member instanceof FlywheelBlockEntity) {
                count++;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!visited.contains(next)) {
                    queue.add(next);
                }
            }
        }
        return count;
    }

    /** True when any source other than this charger powers the network. */
    private boolean findExternalSource() {
        KineticNetwork network = getOrCreateNetwork();
        if (network == null) {
            return false;
        }
        for (KineticBlockEntity source : network.sources.keySet()) {
            if (source != this) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        // Publish the fill level as redstone; setBlock also fires the
        // neighbour updates wires need.
        BlockState state = getBlockState();
        int power = getComparatorOutput();
        if (state.getValue(KineticChargerBlock.POWER) != power) {
            level.setBlock(worldPosition, state.setValue(KineticChargerBlock.POWER, power),
                    Block.UPDATE_ALL);
        }
        // Sync the exact buffer now and then so the goggle readout tracks
        // it between chunk loads.
        if (buffer != lastSyncedBuffer && level.getGameTime() % 8 == 0) {
            lastSyncedBuffer = buffer;
            sendData();
        }

        if (level.getGameTime() % 10 == 0 || flywheels == 0) {
            int counted = countFlywheels();
            if (counted != flywheels) {
                flywheels = counted;
                sendData();
            }
            // A shrunken bank cannot hold what a bigger one banked.
            if (buffer > getMaxBuffer()) {
                buffer = getMaxBuffer();
                setChanged();
            }
        }

        // Neutral drain: a spinning bank is never free. Five SU-seconds
        // bleed away every second while any charge is held, whether the
        // battery charges, discharges or just sits there.
        if (buffer > 0) {
            buffer = Math.max(0, buffer - NEUTRAL_DRAIN_PER_SECOND / 20.0);
            setChanged();
            if (buffer <= 0) {
                reActivateSource = true;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }

        boolean external = findExternalSource();
        if (external != externallyPowered) {
            externallyPowered = external;
            // Our generated speed just turned on or off with it, and the
            // goggles read this flag on the client, so ship it over.
            reActivateSource = true;
            sendData();
        }
        refreshLoad();
        updateDischargingState();

        if (externallyPowered && getSpeed() != 0) {
            // Charge mode. Remember the driven speed; discharge repeats it.
            if (chargeSpeed != getSpeed()) {
                chargeSpeed = getSpeed();
                setChanged();
            }
            if (buffer < getMaxBuffer() && flywheels <= MAX_FLYWHEELS) {
                // An honest consumer, like the inducer: the network carries
                // the charge load as stress and the buffer banks a tick's
                // worth of SU-seconds.
                buffer = Math.min(getMaxBuffer(), buffer + intakePerTick());
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
            return;
        }

        if (isGenerating()) {
            // Battery mode: the machines' stress drains SU-seconds per
            // second, one twentieth of it each tick.
            KineticNetwork network = getOrCreateNetwork();
            double used = network == null ? 0 : Math.max(0, network.calculateStress());
            if (used > 0) {
                buffer = Math.max(0, buffer - used / 20.0);
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                if (buffer <= 0) {
                    reActivateSource = true;
                }
            }
        }
    }

    /** True while the battery itself spins the network. */
    public boolean isGenerating() {
        return !externallyPowered && buffer > 0 && chargeSpeed != 0;
    }

    private void updateDischargingState() {
        boolean discharging = isGenerating();
        BlockState state = getBlockState();
        if (state.getValue(KineticChargerBlock.DISCHARGING) != discharging) {
            level.setBlock(worldPosition,
                    state.setValue(KineticChargerBlock.DISCHARGING, discharging),
                    Block.UPDATE_ALL);
        }
    }

    public boolean isDischargingState() {
        return getBlockState().getOptionalValue(KineticChargerBlock.DISCHARGING).orElse(false);
    }

    @Override
    public float getGeneratedSpeed() {
        return isGenerating() ? chargeSpeed : 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        // Stored per RPM; Create multiplies by the generated speed, so the
        // battery provides a flat DISCHARGE_CAPACITY SU while running.
        float capacity = isGenerating()
                ? DISCHARGE_CAPACITY / Math.max(1f, Math.abs(chargeSpeed))
                : 0;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    // While filling, the charger applies its charge load as stress; as a
    // source it applies none. Create stores stress as impact times speed,
    // hence the division; the theoretical speed keeps the load booked
    // while the network is overstressed, so it cannot flap.
    @Override
    public float calculateStressApplied() {
        float impact = targetImpact();
        this.lastStressApplied = impact;
        return impact;
    }

    private float targetImpact() {
        float speed = Math.abs(getTheoreticalSpeed());
        if (!externallyPowered || speed < 0.01f) {
            return 0;
        }
        // Eleven or more flywheels is more inertia than the charger can
        // spin up: it grinds the whole network to an overstressed halt.
        if (flywheels > MAX_FLYWHEELS) {
            return (float) (1_073_741_824.0 / speed); // 2^30
        }
        if (buffer >= getMaxBuffer()) {
            return 0;
        }
        return (float) (chargeLoad() / speed);
    }

    /** Re-books our stress with the network whenever the load changes. */
    private void refreshLoad() {
        if (!hasNetwork()) {
            return;
        }
        float target = targetImpact();
        if (Math.abs(target - lastStressApplied) > 1.0e-4f) {
            getOrCreateNetwork().updateStressFor(this, target);
            getOrCreateNetwork().sync();
            lastStressApplied = target;
        }
    }

    /** Battery mode with charge left in the buffer. */
    public boolean isDischarging() {
        return isGenerating();
    }

    /** SU-seconds a consumer may pull from this charger right now. */
    public double availableDischarge() {
        return buffer > 0 ? Math.min(buffer, MAX_RATE_PER_TICK) : 0;
    }

    /** Removes up to {@code amount} SU-seconds, returns what was taken. */
    public double drain(double amount) {
        double taken = Math.max(0, Math.min(amount, buffer));
        if (taken > 0) {
            buffer -= taken;
            setChanged();
            if (level != null) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
        return taken;
    }

    public double getBuffer() {
        return buffer;
    }

    /** 0..15 comparator output scaling with the buffer fill fraction. */
    public int getComparatorOutput() {
        if (buffer <= 0) {
            return 0;
        }
        return (int) Math.ceil(15.0 * Math.min(1.0, buffer / getMaxBuffer()));
    }

    // --- Test hooks (used by the game tests; harmless in normal play) --------

    public void setBufferForTesting(double value) {
        this.buffer = Math.max(0, Math.min(MAX_BUFFER, value));
        setChanged();
    }

    public void setChargeSpeedForTesting(float speed) {
        this.chargeSpeed = speed;
        setChanged();
    }

    public void recountFlywheelsForTesting() {
        this.flywheels = countFlywheels();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("Buffer", buffer);
        compound.putFloat("ChargeSpeed", chargeSpeed);
        compound.putInt("Flywheels", flywheels);
        compound.putBoolean("ExternallyPowered", externallyPowered);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        buffer = compound.getDouble("Buffer");
        chargeSpeed = compound.getFloat("ChargeSpeed");
        flywheels = compound.getInt("Flywheels");
        externallyPowered = compound.getBoolean("ExternallyPowered");
    }

    // @Override intentionally present: if Create ever changes this signature,
    // the compile breaks here instead of goggles silently going blank.
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.kinetic_charger")
                        .withStyle(ChatFormatting.GRAY)));

        // A live fill bar, then the exact numbers under it.
        double fraction = Math.min(1.0, buffer / getMaxBuffer());
        int filled = (int) Math.round(BAR_SEGMENTS * fraction);
        tooltip.add(Component.literal("    ")
                .append(Component.literal("|".repeat(filled))
                        .withStyle(buffer >= getMaxBuffer() ? ChatFormatting.GREEN : ChatFormatting.AQUA))
                .append(Component.literal("|".repeat(BAR_SEGMENTS - filled))
                        .withStyle(ChatFormatting.DARK_GRAY)));

        int percent = (int) Math.floor(100.0 * fraction);
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.buffer",
                        String.format("%,.0f", buffer), String.format("%,.0f", getMaxBuffer()), percent)
                        .withStyle(buffer >= getMaxBuffer() ? ChatFormatting.GREEN : ChatFormatting.AQUA)));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.flywheels",
                        flywheels, MAX_FLYWHEELS)
                        .withStyle(flywheels > MAX_FLYWHEELS ? ChatFormatting.RED : ChatFormatting.GRAY)));

        String modeKey = isGenerating() ? "discharging"
                : (externallyPowered && getSpeed() != 0 && buffer < getMaxBuffer() ? "charging" : "idle");
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.charger_mode",
                        Component.translatable("weatherinducer.charger_mode." + modeKey))
                        .withStyle(ChatFormatting.GRAY)));

        return true;
    }
}
