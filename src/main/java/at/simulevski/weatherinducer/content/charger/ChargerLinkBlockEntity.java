package at.simulevski.weatherinducer.content.charger;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * Keeps a Charger Link's network id and wires it into the host charger:
 * every half second (and on load) the id is pushed onto the Kinetic
 * Charger the link is bolted to, and pulled off it again when the link
 * goes. Goggles on the link show the whole network's pooled numbers.
 */
public class ChargerLinkBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    private UUID network;

    public ChargerLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER_LINK.get(), pos, state);
    }

    public void setNetwork(UUID network) {
        this.network = network;
        setChanged();
        applyToHost();
    }

    public UUID getNetwork() {
        return network;
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
        }
    }

    /** Server ticker: keeps the host wired even across chunk reloads. */
    public static void tick(Level level, BlockPos pos, BlockState state,
                            ChargerLinkBlockEntity link) {
        if (!level.isClientSide && level.getGameTime() % 10 == 0) {
            link.applyToHost();
        }
    }

    @Override
    public void setRemoved() {
        KineticChargerBlockEntity charger = host();
        if (charger != null) {
            charger.setLinkNetwork(null);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (network != null) {
            tag.putUUID("Network", network);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("Network")) {
            network = tag.getUUID("Network");
        }
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
                Component.translatable("weatherinducer.tooltip.link_network",
                        String.format("%,.0f", ChargerNetworks.totalEnergy(network)),
                        String.format("%,.0f", ChargerNetworks.totalCapacity(network)),
                        ChargerNetworks.members(network).size())
                        .withStyle(ChatFormatting.AQUA)));
        return true;
    }
}
