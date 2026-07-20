package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.displayLink.LinkWithBulbBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Keeps a Charger Link's network id and name, and wires both into the
 * host charger: every half second (and on load) they are pushed onto the
 * Kinetic Charger the link is bolted to, and pulled off it again when
 * the link goes. A link named with a name tag keeps that name; an
 * unnamed one is handed the next free number in its network, so the
 * goggles can always say which charger holds the discharge lead.
 *
 * <p>Extends the display link's own bulb base class, so the heartbeat
 * flash runs on the very same pulse machinery: the server sends a pulse
 * every two seconds, timed off the network id, and every link of one
 * network blinks in unison.
 */
public class ChargerLinkBlockEntity extends LinkWithBulbBlockEntity
        implements IHaveGoggleInformation {

    /** Ticks between heartbeat flashes. */
    private static final int PULSE_PERIOD = 40;

    private UUID network;
    private String customName;
    private int number;

    public ChargerLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER_LINK.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (network != null
                && Math.floorMod(level.getGameTime() + network.hashCode(), PULSE_PERIOD) == 0) {
            // The heartbeat, on the display link's pulse plumbing.
            sendPulseNextSync();
            sendData();
        }
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        if (network != null) {
            ChargerNetworks.registerLink(network, this);
            if (number == 0) {
                number = ChargerNetworks.nextLinkNumber(network);
                setChanged();
                sendData();
            }
        }
        applyToHost();
    }

    public void setNetwork(UUID network) {
        this.network = network;
        setChanged();
        applyToHost();
    }

    public UUID getNetwork() {
        return network;
    }

    public void setCustomName(String name) {
        this.customName = name;
        setChanged();
        applyToHost();
        sendData();
    }

    public int getNumber() {
        return number;
    }

    /** The name the goggles use: the given one, or "Charger N". */
    public String getDisplayName() {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        return "Charger " + (number > 0 ? number : "?");
    }

    private KineticChargerBlockEntity host() {
        if (level == null
                || !(getBlockState().getBlock() instanceof ChargerLinkBlock)) {
            return null;
        }
        BlockPos hostPos = worldPosition.relative(
                getBlockState().getValue(ChargerLinkBlock.FACING).getOpposite());
        return level.getBlockEntity(hostPos) instanceof KineticChargerBlockEntity charger
                ? charger : null;
    }

    void applyToHost() {
        KineticChargerBlockEntity charger = host();
        if (charger != null) {
            charger.setLinkNetwork(network);
            charger.setLinkDisplayName(getDisplayName());
        }
    }

    // The glow partial is authored in place, so the renderer needs no
    // extra offset; the facing steers its rotation switch.
    @Override
    public Vec3 getBulbOffset(BlockState state) {
        return Vec3.ZERO;
    }

    @Override
    public Direction getBulbFacing(BlockState state) {
        return state.getValue(ChargerLinkBlock.FACING);
    }

    @Override
    public void invalidate() {
        if (network != null) {
            ChargerNetworks.unregisterLink(network, this);
        }
        super.invalidate();
    }

    @Override
    public void destroy() {
        // Only on actual removal, not chunk unload: unhook the host.
        KineticChargerBlockEntity charger = host();
        if (charger != null) {
            charger.setLinkNetwork(null);
            charger.setLinkDisplayName(null);
        }
        super.destroy();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (network != null) {
            tag.putUUID("Network", network);
        }
        if (customName != null) {
            tag.putString("CustomName", customName);
        }
        tag.putInt("Number", number);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.hasUUID("Network")) {
            network = tag.getUUID("Network");
        }
        if (tag.contains("CustomName")) {
            customName = tag.getString("CustomName");
        }
        number = tag.getInt("Number");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (network == null) {
            return false;
        }
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.charger_link")
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("    ").append(
                Component.translatable("weatherinducer.tooltip.link_name", getDisplayName())
                        .withStyle(ChatFormatting.AQUA)));
        KineticChargerBlockEntity charger = host();
        if (charger != null && charger.getGroupSize() > 0) {
            BlockPos lead = charger.getGroupLeadPos();
            tooltip.add(Component.literal("    ").append(
                    Component.translatable("weatherinducer.tooltip.link_network",
                            charger.getGroupSize(),
                            lead == null
                                    ? Component.translatable("weatherinducer.tooltip.link_lead_none")
                                    : Component.literal(charger.getGroupLeadName()))
                            .withStyle(ChatFormatting.AQUA)));
        }
        return true;
    }
}
