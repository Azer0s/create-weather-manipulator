package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.network.SUNetwork;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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

import java.util.List;

/**
 * Battery logic for the SU Charger.
 *
 * <p>While the input shaft turns, the charger passes rotation through and
 * soaks the network's spare SU into an internal buffer, up to
 * {@link #MAX_RATE_PER_TICK} per tick (SU Resistors on the way cap that
 * further). Once the input stops and the buffer holds charge, it flips to
 * discharge: the input face disconnects, the charger itself becomes the
 * kinetic source of its output side, spinning it at the speed it was
 * charged with and providing {@link #DISCHARGE_CAPACITY} SU. Each tick the
 * buffer drops by the stress the driven machines actually use, and when it
 * runs dry (or the input side starts turning again) the charger reconnects
 * and goes back to charging. Raw network SU never passes through; the
 * Weather Inducer may additionally drain the buffer directly through the
 * output face (see {@link SUNetwork#drawFromChargers}). The buffer fill is
 * also published as a 0..15 redstone signal via {@link SUChargerBlock#POWER}.
 */
public class SUChargerBlockEntity extends GeneratingKineticBlockEntity
        implements IHaveGoggleInformation {

    /** How much SU the buffer holds; one full buffer is one inducer charge. */
    public static final double MAX_BUFFER = 1_048_576.0; // 2^20

    /** Charge and discharge ceiling per tick. */
    public static final double MAX_RATE_PER_TICK = 131_072.0; // 2^17

    /** SU the battery provides to its output side while discharging. */
    public static final float DISCHARGE_CAPACITY = 131_072f; // 2^17

    private double buffer;
    private double lastSyncedBuffer;

    /** The last non-zero input speed; discharge drives the output at it. */
    private float chargeSpeed;

    public SUChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
        if (state.getValue(SUChargerBlock.POWER) != power) {
            level.setBlock(worldPosition, state.setValue(SUChargerBlock.POWER, power),
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

        if (getSpeed() != 0) {
            // Charge mode. Remember the input speed; it becomes the
            // discharge speed once the input stops.
            if (chargeSpeed != getSpeed()) {
                chargeSpeed = getSpeed();
                setChanged();
            }
            if (buffer >= MAX_BUFFER) {
                return;
            }
            double wanted = Math.min(MAX_RATE_PER_TICK, MAX_BUFFER - buffer);
            wanted = Math.min(wanted, SUNetwork.resistorIntakeCap(this));
            double intake = Math.min(wanted, SUNetwork.remainingSU(this));
            if (intake > 0) {
                buffer = Math.min(MAX_BUFFER, buffer + intake);
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
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
        if (state.getValue(SUChargerBlock.DISCHARGING) == discharging) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(SUChargerBlock.DISCHARGING, discharging),
                Block.UPDATE_ALL);
        // Re-propagate rotation with the new face connections, and let the
        // generating base class re-apply (or drop) our source speed.
        if (level.getBlockState(worldPosition).getBlock() instanceof SUChargerBlock block) {
            block.detachKinetics(level, worldPosition, true);
        }
        reActivateSource = true;
    }

    public boolean isDischargingState() {
        return getBlockState().getOptionalValue(SUChargerBlock.DISCHARGING).orElse(false);
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

    // A charger under load is a modest consumer; as a source it is none.
    @Override
    public float calculateStressApplied() {
        float impact = isDischargingState() ? 0 : 4f; // SU per RPM
        this.lastStressApplied = impact;
        return impact;
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
        return (int) Math.ceil(15.0 * Math.min(1.0, buffer / MAX_BUFFER));
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

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("Buffer", buffer);
        compound.putFloat("ChargeSpeed", chargeSpeed);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        buffer = compound.getDouble("Buffer");
        chargeSpeed = compound.getFloat("ChargeSpeed");
    }

    // @Override intentionally present: if Create ever changes this signature,
    // the compile breaks here instead of goggles silently going blank.
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.su_charger")
                        .withStyle(ChatFormatting.GRAY)));

        int percent = (int) Math.floor(100.0 * Math.min(1.0, buffer / MAX_BUFFER));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.buffer",
                        String.format("%,.0f", buffer), String.format("%,.0f", MAX_BUFFER), percent)
                        .withStyle(buffer >= MAX_BUFFER ? ChatFormatting.GREEN : ChatFormatting.AQUA)));

        String modeKey = isDischarging() ? "discharging" : (getSpeed() != 0 ? "charging" : "idle");
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.charger_mode",
                        Component.translatable("weatherinducer.charger_mode." + modeKey))
                        .withStyle(ChatFormatting.GRAY)));

        return true;
    }
}
