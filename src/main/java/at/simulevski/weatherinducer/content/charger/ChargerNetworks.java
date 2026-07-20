package at.simulevski.weatherinducer.content.charger;

import net.minecraft.core.BlockPos;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The runtime side of charger networks: which loaded Kinetic Chargers
 * belong to which link network. Membership is re-registered by the charger
 * block entities themselves every tick (and dropped when they unload), so
 * this needs no persistence; the network id itself lives on the Charger
 * Link blocks and each charger saves its own buffer.
 *
 * <p>A link network coordinates discharging and nothing else: no energy
 * ever moves between chargers. At any moment exactly one member holds the
 * discharge lead (the lowest position that still has energy in its
 * buffer), the rest stand by, and when the lead runs dry the next in line
 * takes over. That is what keeps several batteries on one shaft from
 * pushing at once and shearing it apart.
 */
public final class ChargerNetworks {

    private static final Map<UUID, Set<KineticChargerBlockEntity>> MEMBERS =
            new ConcurrentHashMap<>();

    private ChargerNetworks() {
    }

    public static void register(UUID network, KineticChargerBlockEntity charger) {
        MEMBERS.computeIfAbsent(network, id -> new CopyOnWriteArraySet<>()).add(charger);
    }

    public static void unregister(UUID network, KineticChargerBlockEntity charger) {
        Set<KineticChargerBlockEntity> members = MEMBERS.get(network);
        if (members != null) {
            members.remove(charger);
            if (members.isEmpty()) {
                MEMBERS.remove(network);
            }
        }
    }

    /** Loaded members of the given network, stale entries pruned. */
    public static Set<KineticChargerBlockEntity> members(UUID network) {
        Set<KineticChargerBlockEntity> members = MEMBERS.getOrDefault(network, Set.of());
        members.removeIf(KineticChargerBlockEntity::isRemoved);
        return members;
    }

    /**
     * The one member currently allowed to discharge: the lowest position
     * that still holds energy. Null when the whole group is empty.
     */
    public static KineticChargerBlockEntity dischargeLeader(UUID network) {
        return members(network).stream()
                .filter(member -> member.getBuffer() > 0)
                .min(Comparator.comparingLong(member -> member.getBlockPos().asLong()))
                .orElse(null);
    }

    /** True when {@code charger} holds the group's discharge lead. */
    public static boolean isDischargeLeader(UUID network, KineticChargerBlockEntity charger) {
        return dischargeLeader(network) == charger;
    }

    /** Position of the current discharge lead, for the goggle readout. */
    public static BlockPos leaderPos(UUID network) {
        KineticChargerBlockEntity leader = dischargeLeader(network);
        return leader != null ? leader.getBlockPos() : null;
    }
}
