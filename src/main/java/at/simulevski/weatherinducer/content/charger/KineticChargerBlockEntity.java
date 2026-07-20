package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.content.inducer.ChargeTimeScrollBehaviour;
import at.simulevski.weatherinducer.content.util.SideValueBoxTransform;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.RotationPropagator;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
 *
 * <p>Charger Links add discharge coordination on top, and only that: a
 * linked charger generates only while it holds its link network's
 * discharge lead, so batteries take turns instead of pushing at once.
 * Every buffer stays where it was charged; nothing about the link moves
 * energy around.
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

    /** True while this charger won the right to generate (cached, synced). */
    private boolean electedLeader = true;

    /**
     * SU-seconds leaving the buffer per second right now (stress share
     * plus neutral drain), synced so the goggles can say how long the
     * charge lasts at the current draw.
     */
    private double drainPerSecond;

    /** The charger link network this block belongs to, if a link is attached. */
    private UUID linkNetwork;

    // Group stats, aggregated server-side once a second and synced so the
    // goggles can show the whole link network's state on any member and
    // on dedicated servers too. Sentinel for "no lead": Long.MAX_VALUE.
    private double groupEnergy;
    private double groupCapacity;
    private int groupSize;
    private long groupLead = Long.MAX_VALUE;
    private String groupLeadName = "";
    private double groupDrain;
    private boolean groupDischarging;

    /** This charger's own link name, pushed over by the attached link. */
    private String linkName = "";

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
     * 104,857.6 per flywheel up to ten of them. An eleventh wheel never
     * survives: the bank walk pops it right off (see {@link #walkBank}).
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
     * Walks the bank and keeps it honest. The walk follows Create's real
     * rotation connections starting behind the flywheel face, so only
     * blocks that would actually spin with the bank count. The flywheel
     * side tolerates nothing but flywheels: any other kinetic block that
     * connects to the bank pops right off as an item, and so does every
     * wheel past the tenth. Returns the surviving wheel count.
     */
    private int walkBank() {
        if (level == null) {
            return 0;
        }
        if (!(level.getBlockEntity(worldPosition.relative(getFlywheelFace()))
                instanceof KineticBlockEntity first)) {
            return 0;
        }
        if (!RotationPropagator.isConnected(this, first)
                && !RotationPropagator.isConnected(first, this)) {
            return 0;
        }
        Set<BlockPos> visited = new HashSet<>();
        visited.add(worldPosition);
        Deque<KineticBlockEntity> queue = new ArrayDeque<>();
        queue.add(first);
        visited.add(first.getBlockPos());
        List<BlockPos> toPop = new ArrayList<>();
        int wheels = 0;
        while (!queue.isEmpty() && visited.size() < 64) {
            KineticBlockEntity member = queue.poll();
            if (!(member instanceof FlywheelBlockEntity)) {
                // The flywheel side takes nothing but flywheels.
                toPop.add(member.getBlockPos());
                continue;
            }
            wheels++;
            if (wheels > MAX_FLYWHEELS) {
                // The eleventh wheel is one too many; off it comes.
                toPop.add(member.getBlockPos());
                wheels--;
                continue;
            }
            for (BlockPos offset : BlockPos.betweenClosed(-1, -1, -1, 1, 1, 1)) {
                BlockPos next = member.getBlockPos().offset(offset);
                if (visited.contains(next)) {
                    continue;
                }
                if (!(level.getBlockEntity(next) instanceof KineticBlockEntity candidate)) {
                    continue;
                }
                if (RotationPropagator.isConnected(member, candidate)
                        || RotationPropagator.isConnected(candidate, member)) {
                    visited.add(next.immutable());
                    queue.add(candidate);
                }
            }
        }
        for (BlockPos pos : toPop) {
            level.destroyBlock(pos, true);
        }
        return wheels;
    }

    /**
     * True when a real source (not a battery) powers the network. Other
     * chargers do not count: batteries never charge from batteries, they
     * take turns instead (see {@link #findElectedLeader}).
     */
    private boolean findExternalSource() {
        KineticNetwork network = getOrCreateNetwork();
        if (network == null) {
            return false;
        }
        for (KineticBlockEntity source : network.sources.keySet()) {
            if (source != this && !(source instanceof KineticChargerBlockEntity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Discharge coordination comes from Charger Links and nowhere else.
     * A charger wearing a link only generates while it holds its link
     * network's discharge lead (the lowest position with energy); when
     * the lead runs dry the next in line takes over. Unlinked chargers do
     * not coordinate at all: they all push at once, which is fine while
     * their speeds agree and shears the shaft when they do not, exactly
     * like any other pair of fighting sources in Create.
     */
    private boolean findElectedLeader() {
        if (buffer <= 0) {
            return false;
        }
        if (linkNetwork == null) {
            return true;
        }
        return ChargerNetworks.isDischargeLeader(linkNetwork, this);
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
            int counted = walkBank();
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
        if (linkNetwork != null) {
            ChargerNetworks.register(linkNetwork, this);
            if (level.getGameTime() % 20 == 0) {
                refreshGroupStats();
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
        boolean elected = findElectedLeader();
        if (external != externallyPowered || elected != electedLeader) {
            externallyPowered = external;
            electedLeader = elected;
            // Our generated speed just turned on or off with these, and the
            // goggles read the flags on the client, so ship them over.
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
            if (buffer < getMaxBuffer()) {
                // An honest consumer, like the inducer: the network carries
                // the charge load as stress and the buffer banks a tick's
                // worth of SU-seconds.
                buffer = Math.min(getMaxBuffer(), buffer + intakePerTick());
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
            drainPerSecond = buffer > 0 ? NEUTRAL_DRAIN_PER_SECOND : 0;
            return;
        }

        if (isGenerating() && !hasSource()) {
            // Battery mode, and this block really is a driving source (a
            // same-speed battery that Create demoted to a driven member
            // does no work and pays nothing). The machines' stress drains
            // SU-seconds per second, one twentieth of it each tick; when
            // several unlinked batteries co-drive one network, they split
            // the bill.
            KineticNetwork network = getOrCreateNetwork();
            double used = network == null ? 0 : Math.max(0, network.calculateStress());
            if (used > 0) {
                int coDrivers = 1;
                for (KineticBlockEntity source : network.sources.keySet()) {
                    if (source != this
                            && source instanceof KineticChargerBlockEntity other
                            && other.isGenerating() && !other.hasSource()) {
                        coDrivers++;
                    }
                }
                buffer = Math.max(0, buffer - used / 20.0 / coDrivers);
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                if (buffer <= 0) {
                    reActivateSource = true;
                }
                drainPerSecond = used / coDrivers + NEUTRAL_DRAIN_PER_SECOND;
                return;
            }
        }
        drainPerSecond = buffer > 0 ? NEUTRAL_DRAIN_PER_SECOND : 0;
    }

    /** True while the battery itself spins the network. */
    public boolean isGenerating() {
        return !externallyPowered && electedLeader && buffer > 0 && chargeSpeed != 0;
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

    /** Re-aggregates and syncs the link network's stats when they moved. */
    private void refreshGroupStats() {
        double energy = ChargerNetworks.totalEnergy(linkNetwork);
        double capacity = ChargerNetworks.totalCapacity(linkNetwork);
        int size = ChargerNetworks.members(linkNetwork).size();
        KineticChargerBlockEntity lead = ChargerNetworks.dischargeLeader(linkNetwork);
        long leadLong = lead == null ? Long.MAX_VALUE : lead.getBlockPos().asLong();
        String leadName = lead == null ? "" : lead.getLinkDisplayName();
        // The group's current draw is just every member's own: the lead
        // carries the stress share, everyone holding charge bleeds the
        // neutral drain.
        double drain = ChargerNetworks.members(linkNetwork).stream()
                .mapToDouble(KineticChargerBlockEntity::getDrainPerSecond).sum();
        boolean discharging = lead != null && lead.isGenerating();
        if (size != groupSize || leadLong != groupLead
                || !leadName.equals(groupLeadName)
                || discharging != groupDischarging
                || Math.abs(drain - groupDrain) > 0.5
                || Math.abs(energy - groupEnergy) > 1
                || Math.abs(capacity - groupCapacity) > 0.5) {
            groupEnergy = energy;
            groupCapacity = capacity;
            groupSize = size;
            groupLead = leadLong;
            groupLeadName = leadName;
            groupDrain = drain;
            groupDischarging = discharging;
            sendData();
        }
    }

    public double getDrainPerSecond() {
        return drainPerSecond;
    }

    public double getGroupEnergy() {
        return groupEnergy;
    }

    public double getGroupCapacity() {
        return groupCapacity;
    }

    public int getGroupSize() {
        return groupSize;
    }

    /** The synced discharge lead position, or null while the group is empty. */
    public BlockPos getGroupLeadPos() {
        return groupLead == Long.MAX_VALUE ? null : BlockPos.of(groupLead);
    }

    /** The synced name of the discharge lead's link, or empty. */
    public String getGroupLeadName() {
        return groupLeadName;
    }

    /** Wired up by the attached link alongside the network id. */
    public void setLinkDisplayName(String name) {
        linkName = name != null ? name : "";
    }

    /** The name this charger's own link carries, or the position fallback. */
    public String getLinkDisplayName() {
        return linkName.isEmpty() ? worldPosition.toShortString() : linkName;
    }

    /** Readable duration: seconds under two minutes, then m and h. */
    private static String formatSeconds(double seconds) {
        long s = (long) Math.floor(seconds);
        if (s < 120) {
            return s + " s";
        }
        if (s < 7200) {
            return (s / 60) + " min " + (s % 60) + " s";
        }
        return (s / 3600) + " h " + (s % 3600 / 60) + " min";
    }

    /** The mode word the goggle tooltip and display sources share. */
    public String modeKey() {
        return isGenerating() ? "discharging"
                : externallyPowered && getSpeed() != 0 && buffer < getMaxBuffer() ? "charging"
                : linkNetwork != null && buffer > 0 && !electedLeader ? "standby"
                : "idle";
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

    /** Wired up by an attached Charger Link; null when standalone. */
    public void setLinkNetwork(UUID network) {
        if (Objects.equals(linkNetwork, network)) {
            return;
        }
        if (linkNetwork != null) {
            ChargerNetworks.unregister(linkNetwork, this);
        }
        linkNetwork = network;
        // The goggle tooltip renders on the client and gates its whole
        // network section on this field, so membership changes must ship
        // over right away.
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    public UUID getLinkNetwork() {
        return linkNetwork;
    }

    @Override
    public void invalidate() {
        if (linkNetwork != null) {
            ChargerNetworks.unregister(linkNetwork, this);
        }
        super.invalidate();
    }

    // --- Test hooks (used by the game tests; harmless in normal play) --------

    public void setBufferForTesting(double value) {
        this.buffer = Math.max(0, Math.min(MAX_BUFFER, value));
        // In real play every path that changes what getGeneratedSpeed()
        // returns also raises this flag; the hook must do the same or the
        // generator keeps running on stale numbers.
        this.reActivateSource = true;
        setChanged();
    }

    public void setChargeSpeedForTesting(float speed) {
        this.chargeSpeed = speed;
        this.reActivateSource = true;
        setChanged();
    }

    public void recountFlywheelsForTesting() {
        this.flywheels = walkBank();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putDouble("Buffer", buffer);
        compound.putFloat("ChargeSpeed", chargeSpeed);
        compound.putInt("Flywheels", flywheels);
        compound.putBoolean("ExternallyPowered", externallyPowered);
        compound.putBoolean("ElectedLeader", electedLeader);
        compound.putDouble("DrainPerSecond", drainPerSecond);
        compound.putDouble("GroupEnergy", groupEnergy);
        compound.putDouble("GroupCapacity", groupCapacity);
        compound.putInt("GroupSize", groupSize);
        compound.putLong("GroupLead", groupLead);
        compound.putString("GroupLeadName", groupLeadName);
        compound.putDouble("GroupDrain", groupDrain);
        compound.putBoolean("GroupDischarging", groupDischarging);
        compound.putString("LinkName", linkName);
        if (linkNetwork != null) {
            compound.putUUID("LinkNetwork", linkNetwork);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        buffer = compound.getDouble("Buffer");
        chargeSpeed = compound.getFloat("ChargeSpeed");
        flywheels = compound.getInt("Flywheels");
        externallyPowered = compound.getBoolean("ExternallyPowered");
        electedLeader = compound.getBoolean("ElectedLeader");
        drainPerSecond = compound.getDouble("DrainPerSecond");
        groupEnergy = compound.getDouble("GroupEnergy");
        groupCapacity = compound.getDouble("GroupCapacity");
        groupSize = compound.getInt("GroupSize");
        groupLead = compound.contains("GroupLead") ? compound.getLong("GroupLead") : Long.MAX_VALUE;
        groupLeadName = compound.getString("GroupLeadName");
        groupDrain = compound.getDouble("GroupDrain");
        groupDischarging = compound.getBoolean("GroupDischarging");
        linkName = compound.getString("LinkName");
        linkNetwork = compound.hasUUID("LinkNetwork")
                ? compound.getUUID("LinkNetwork") : null;
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
                .append(Component.literal("█".repeat(filled))
                        .withStyle(buffer >= getMaxBuffer() ? ChatFormatting.GREEN : ChatFormatting.AQUA))
                .append(Component.literal("░".repeat(BAR_SEGMENTS - filled))
                        .withStyle(ChatFormatting.DARK_GRAY)));

        int percent = (int) Math.floor(100.0 * fraction);
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.buffer",
                        String.format("%,.0f", buffer), String.format("%,.0f", getMaxBuffer()), percent)
                        .withStyle(buffer >= getMaxBuffer() ? ChatFormatting.GREEN : ChatFormatting.AQUA)));
        // How long the charge lasts at what the network draws right now.
        // Meaningless mid-charge, where the flow runs the other way.
        if (buffer > 0 && drainPerSecond > 0 && !"charging".equals(modeKey())) {
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.remaining",
                            formatSeconds(buffer / drainPerSecond),
                            String.format("%,.0f", drainPerSecond))
                            .withStyle(ChatFormatting.GRAY)));
        }
        // The flywheel bank as a bar, one block per wheel slot.
        int banked = Math.min(flywheels, MAX_FLYWHEELS);
        tooltip.add(Component.literal("    ")
                .append(Component.literal("█".repeat(banked))
                        .withStyle(ChatFormatting.GOLD))
                .append(Component.literal("░".repeat(MAX_FLYWHEELS - banked))
                        .withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" "))
                .append(Component.translatable("weatherinducer.tooltip.flywheels",
                        flywheels, MAX_FLYWHEELS)
                        .withStyle(ChatFormatting.GRAY)));
        if (linkNetwork != null) {
            // The whole link network: its own fill bar, the pooled numbers
            // (display only, energy never moves between members) and who
            // holds the discharge lead, by link name.
            double groupFraction = groupCapacity > 0
                    ? Math.min(1.0, groupEnergy / groupCapacity) : 0;
            int groupFilled = (int) Math.round(BAR_SEGMENTS * groupFraction);
            tooltip.add(Component.literal("    ")
                    .append(Component.literal("█".repeat(groupFilled))
                            .withStyle(groupEnergy >= groupCapacity && groupCapacity > 0
                                    ? ChatFormatting.GREEN : ChatFormatting.BLUE))
                    .append(Component.literal("░".repeat(BAR_SEGMENTS - groupFilled))
                            .withStyle(ChatFormatting.DARK_GRAY)));
            // How long the whole network's charge lasts, only while its
            // lead actually discharges.
            if (groupDischarging && groupDrain > 0 && groupEnergy > 0) {
                tooltip.add(Component.literal("    ").append(
                        Component.translatable("weatherinducer.tooltip.network_remaining",
                                formatSeconds(groupEnergy / groupDrain),
                                String.format("%,.0f", groupDrain))
                                .withStyle(ChatFormatting.BLUE)));
            }
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.network_buffer",
                            String.format("%,.0f", groupEnergy),
                            String.format("%,.0f", groupCapacity),
                            (int) Math.floor(100.0 * groupFraction))
                            .withStyle(ChatFormatting.BLUE)));
            BlockPos lead = getGroupLeadPos();
            Component leadComponent;
            if (lead == null) {
                leadComponent = Component.translatable("weatherinducer.tooltip.link_lead_none");
            } else if (lead.equals(worldPosition)) {
                leadComponent = Component.translatable("weatherinducer.tooltip.link_lead_self",
                        getLinkDisplayName());
            } else {
                leadComponent = Component.literal(groupLeadName.isEmpty()
                        ? lead.toShortString() : groupLeadName);
            }
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.link_network",
                            groupSize, leadComponent)
                            .withStyle(ChatFormatting.AQUA)));
        }

        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.charger_mode",
                        Component.translatable("weatherinducer.charger_mode." + modeKey()))
                        .withStyle(ChatFormatting.GRAY)));

        return true;
    }
}
