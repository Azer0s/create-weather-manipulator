package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.network.SUNetwork;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
 * Buffer logic for the SU Charger.
 *
 * <p>While the shaft turns, the charger is in charge mode: it soaks up the
 * network's spare SU into an internal buffer, up to
 * {@link #MAX_RATE_PER_TICK} per tick. Once the input stops (the shaft
 * stands still), it flips to discharge mode: consumers reachable through the
 * output face may drain the buffer (see
 * {@link SUNetwork#drawFromChargers}). Raw network SU never passes through
 * the block. The buffer fill is also published as a 0..15 redstone signal via
 * {@link SUChargerBlock#POWER}.
 */
public class SUChargerBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    /** How much SU the buffer holds; one full buffer is one inducer charge. */
    public static final double MAX_BUFFER = 1_000_000.0;

    /** Charge and discharge ceiling per tick. */
    public static final double MAX_RATE_PER_TICK = 100_000.0;

    private double buffer;

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

        // A turning shaft means charge mode; discharge only happens while the
        // input is stopped (consumers pull, nothing to do here).
        if (getSpeed() == 0 || buffer >= MAX_BUFFER) {
            return;
        }
        double wanted = Math.min(MAX_RATE_PER_TICK, MAX_BUFFER - buffer);
        double intake = Math.min(wanted, SUNetwork.remainingSU(this));
        if (intake > 0) {
            buffer = Math.min(MAX_BUFFER, buffer + intake);
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public Direction getOutputFace() {
        return getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }

    public Direction getInputFace() {
        return getOutputFace().getOpposite();
    }

    /** Discharging = the input shaft stands still and the buffer is not empty. */
    public boolean isDischarging() {
        return getSpeed() == 0 && buffer > 0;
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

    // --- Test hook (used by the game tests; harmless in normal play) ---------

    public void setBufferForTesting(double value) {
        this.buffer = Math.max(0, Math.min(MAX_BUFFER, value));
        setChanged();
    }

    // A charger under load is a modest consumer on the kinetic network.
    @Override
    public float calculateStressApplied() {
        float impact = 4f; // SU per RPM
        this.lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("Buffer", buffer);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        buffer = compound.getDouble("Buffer");
    }

    // @Override intentionally present: if Create ever changes this signature,
    // the compile breaks here instead of goggles silently going blank.
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.su_charger")
                        .withStyle(ChatFormatting.GRAY)));

        int percent = (int) Math.floor(100.0 * Math.min(1.0, buffer / MAX_BUFFER));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.buffer",
                        String.format("%,.0f", buffer), String.format("%,.0f", MAX_BUFFER), percent)
                        .withStyle(buffer >= MAX_BUFFER ? ChatFormatting.GREEN : ChatFormatting.AQUA)));

        String modeKey = getSpeed() != 0 ? "charging" : (buffer > 0 ? "discharging" : "idle");
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.charger_mode",
                        Component.translatable("weatherinducer.charger_mode." + modeKey))
                        .withStyle(ChatFormatting.GRAY)));

        return true;
    }
}
