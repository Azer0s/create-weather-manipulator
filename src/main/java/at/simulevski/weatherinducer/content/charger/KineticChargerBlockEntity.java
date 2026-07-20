package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.content.inducer.ChargeTimeScrollBehaviour;
import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
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
 * <p>While the input shaft turns, the charger passes rotation through and
 * fills its buffer the same way the Weather Inducer charges: it loads the
 * network with real stress, the full buffer divided by its charge time
 * slider (ten seconds at the fastest), and banks a tick's worth of that
 * every tick. The slider locks while the buffer holds anything. Once the
 * input stops and the buffer holds charge, it flips to
 * discharge: the input face disconnects, the charger itself becomes the
 * kinetic source of its output side, spinning it at the speed it was
 * charged with and providing {@link #DISCHARGE_CAPACITY} SU. Each tick the
 * buffer drops by the stress the driven machines actually use, and when it
 * runs dry (or the input side starts turning again) the charger reconnects
 * and goes back to charging. Raw network SU never passes through. The
 * buffer fill is also published as a 0..15 redstone signal via
 * {@link KineticChargerBlock#POWER}.
 */
public class KineticChargerBlockEntity extends GeneratingKineticBlockEntity
        implements IHaveGoggleInformation {

    /** Buffer capacity with no flywheels attached: barely a sip. */
    public static final double BASE_CAPACITY = 2_048.0; // 2^11

    /** Capacity each flywheel on the input side adds (2^20 / 10). */
    public static final double FLYWHEEL_CAPACITY = 104_857.6;

    /** Flywheels beyond this jam the charger and overstress the network. */
    public static final int MAX_FLYWHEELS = 10;

    /** The largest buffer any flywheel bank allows. */
    public static final double MAX_BUFFER = BASE_CAPACITY + MAX_FLYWHEELS * FLYWHEEL_CAPACITY;

    /** Discharge offer ceiling per tick for direct buffer draws. */
    public static final double MAX_RATE_PER_TICK = 131_072.0; // 2^17

    /** SU the battery provides to its output side while discharging. */
    public static final float DISCHARGE_CAPACITY = 131_072f; // 2^17

    private double buffer;
    private double lastSyncedBuffer;

    /** The last non-zero input speed; discharge drives the output at it. */
    private float chargeSpeed;

    private ChargeTimeScrollBehaviour chargeTime;

    /** Cached flywheel count on the input side, recounted twice a second. */
    private int flywheels;

    public KineticChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(java.util.List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {
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

    /** SU banked per tick so a full buffer takes the configured seconds. */
    public double intakePerTick() {
        return getMaxBuffer() / (getChargeTimeSeconds() * 20.0);
    }

    /** The stress this battery loads its network with while filling. */
    public double chargeLoad() {
        return getMaxBuffer() / getChargeTimeSeconds();
    }

    /** Flywheels currently banked on the input side (uncapped count). */
    public int getFlywheels() {
        return flywheels;
    }

    /**
     * The buffer this flywheel bank can hold: 2,048 SU bare, plus 104,857.6
     * per flywheel up to ten of them. More than ten does not extend it
     * further; it jams the charger instead (see {@link #targetImpact}).
     */
    public double getMaxBuffer() {
        return BASE_CAPACITY + Math.min(flywheels, MAX_FLYWHEELS) * FLYWHEEL_CAPACITY;
    }

    /**
     * Counts flywheels physically connected on the input side: a bounded
     * flood over kinetic blocks starting behind the input face, so a bank
     * of wheels in any arrangement counts, but nothing across the charger.
     */
    private int countFlywheels() {
        if (level == null) {
            return 0;
        }
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(worldPosition.relative(getInputFace()));
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

        if (isDischargingState()) {
            // The input face is disconnected while discharging, so any spin
            // over there is a returning external supply: yield to it.
            if (buffer <= 0 || inputSideSpinning()) {
                setDischarging(false);
                return;
            }
            // The machines we drive eat the buffer at the rate they load
            // the shaft.
            KineticNetwork network = getOrCreateNetwork();
            double used = network == null ? 0 : Math.max(0, network.calculateStress());
            if (used > 0) {
                buffer = Math.max(0, buffer - used);
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                if (buffer <= 0) {
                    setDischarging(false);
                }
            }
            return;
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
        refreshLoad();
        if (getSpeed() != 0) {
            // Charge mode. Remember the input speed; it becomes the
            // discharge speed once the input stops.
            if (chargeSpeed != getSpeed()) {
                chargeSpeed = getSpeed();
                setChanged();
            }
            if (buffer >= getMaxBuffer() || flywheels > MAX_FLYWHEELS) {
                return;
            }
            // An honest consumer, like the inducer: the network carries the
            // charge load as stress and the buffer banks a tick's worth.
            buffer = Math.min(getMaxBuffer(), buffer + intakePerTick());
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            return;
        }

        // Input stopped with charge in the tank: take over as the source.
        if (buffer > 0 && chargeSpeed != 0) {
            setDischarging(true);
        }
    }

    private boolean inputSideSpinning() {
        if (level == null) {
            return false;
        }
        if (!(level.getBlockEntity(worldPosition.relative(getInputFace()))
                instanceof KineticBlockEntity neighbour)) {
            return false;
        }
        float conveyed = neighbour.getSpeed();
        // A clutch or gearshift right at the input keeps spinning on its own
        // source side even while it cuts us off; what matters is the speed
        // it conveys through the face pointing at us. Without this, a
        // clutch-stopped charger flips out of battery mode every other tick
        // and the output side never keeps its rotation.
        if (neighbour instanceof SplitShaftBlockEntity split) {
            conveyed *= split.getRotationSpeedModifier(getInputFace().getOpposite());
        }
        return conveyed != 0;
    }

    private void setDischarging(boolean discharging) {
        BlockState state = getBlockState();
        if (state.getValue(KineticChargerBlock.DISCHARGING) == discharging) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(KineticChargerBlock.DISCHARGING, discharging),
                Block.UPDATE_ALL);
        // Re-propagate rotation with the new face connections, and let the
        // generating base class re-apply (or drop) our source speed.
        if (level.getBlockState(worldPosition).getBlock() instanceof KineticChargerBlock block) {
            block.detachKinetics(level, worldPosition, true);
        }
        reActivateSource = true;
    }

    public boolean isDischargingState() {
        return getBlockState().getOptionalValue(KineticChargerBlock.DISCHARGING).orElse(false);
    }

    @Override
    public float getGeneratedSpeed() {
        return isDischargingState() && buffer > 0 ? chargeSpeed : 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        // Stored per RPM; Create multiplies by the generated speed, so the
        // battery provides a flat DISCHARGE_CAPACITY SU while running.
        float capacity = isDischargingState() && buffer > 0
                ? DISCHARGE_CAPACITY / Math.max(1f, Math.abs(chargeSpeed))
                : 0;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    // While filling, the charger applies its charge load as stress; as a
    // source (discharging) or when full it applies none. Create stores
    // stress as impact times speed, hence the division; the theoretical
    // speed keeps the load booked while the network is overstressed.
    @Override
    public float calculateStressApplied() {
        float impact = targetImpact();
        this.lastStressApplied = impact;
        return impact;
    }

    private float targetImpact() {
        float speed = Math.abs(getTheoreticalSpeed());
        if (isDischargingState() || speed < 0.01f) {
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

    public Direction getOutputFace() {
        return getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }

    public Direction getInputFace() {
        return getOutputFace().getOpposite();
    }

    /** Discharging = battery mode with charge left in the buffer. */
    public boolean isDischarging() {
        return isDischargingState() && buffer > 0;
    }

    /** SU a consumer on the output side may pull from this charger right now. */
    public double availableDischarge() {
        return isDischarging() ? Math.min(buffer, MAX_RATE_PER_TICK) : 0;
    }

    /** Removes up to {@code amount} SU from the buffer, returns what was taken. */
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

    public void recountFlywheelsForTesting() {
        this.flywheels = countFlywheels();
    }

    public void setChargeSpeedForTesting(float speed) {
        this.chargeSpeed = speed;
        setChanged();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("Buffer", buffer);
        compound.putFloat("ChargeSpeed", chargeSpeed);
        compound.putInt("Flywheels", flywheels);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        buffer = compound.getDouble("Buffer");
        chargeSpeed = compound.getFloat("ChargeSpeed");
        flywheels = compound.getInt("Flywheels");
    }

    // @Override intentionally present: if Create ever changes this signature,
    // the compile breaks here instead of goggles silently going blank.
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.kinetic_charger")
                        .withStyle(ChatFormatting.GRAY)));

        int percent = (int) Math.floor(100.0 * Math.min(1.0, buffer / getMaxBuffer()));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.buffer",
                        String.format("%,.0f", buffer), String.format("%,.0f", getMaxBuffer()), percent)
                        .withStyle(buffer >= getMaxBuffer() ? ChatFormatting.GREEN : ChatFormatting.AQUA)));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.flywheels",
                        flywheels, MAX_FLYWHEELS)
                        .withStyle(flywheels > MAX_FLYWHEELS ? ChatFormatting.RED : ChatFormatting.GRAY)));

        String modeKey = isDischarging() ? "discharging" : (getSpeed() != 0 ? "charging" : "idle");
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.charger_mode",
                        Component.translatable("weatherinducer.charger_mode." + modeKey))
                        .withStyle(ChatFormatting.GRAY)));

        return true;
    }
}
