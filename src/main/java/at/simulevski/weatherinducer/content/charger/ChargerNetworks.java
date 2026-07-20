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
 * <p>Every second the member with the lowest position balances the
 * group's buffers, weighted by each member's flywheel capacity, so a link
 * network behaves like one big battery no matter which member happens to
 * charge or discharge.
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
     * True when {@code charger} is the member that runs the once-a-second
     * group work (buffer balancing): simply the lowest position loaded.
     */
    public static boolean runsGroupWork(UUID network, KineticChargerBlockEntity charger) {
        return members(network).stream()
                .min(Comparator.comparingLong(be -> be.getBlockPos().asLong()))
                .map(be -> be == charger)
                .orElse(false);
    }

    /**
     * Evens the group's buffers out, weighted by each member's capacity,
     * so the network reads and behaves like a single battery.
     */
    public static void balance(UUID network) {
        Set<KineticChargerBlockEntity> members = members(network);
        if (members.size() < 2) {
            return;
        }
        double total = 0;
        double capacity = 0;
        for (KineticChargerBlockEntity member : members) {
            total += member.getBuffer();
            capacity += member.getMaxBuffer();
        }
        if (capacity <= 0) {
            return;
        }
        for (KineticChargerBlockEntity member : members) {
            double share = Math.min(member.getMaxBuffer(), total * (member.getMaxBuffer() / capacity));
            member.setBufferBalanced(share);
        }
    }

    /** Total stored SU-seconds across the loaded members. */
    public static double totalEnergy(UUID network) {
        return members(network).stream().mapToDouble(KineticChargerBlockEntity::getBuffer).sum();
    }

    /** Total capacity in SU-seconds across the loaded members. */
    public static double totalCapacity(UUID network) {
        return members(network).stream().mapToDouble(KineticChargerBlockEntity::getMaxBuffer).sum();
    }

    /** Sorting helper so the goggle line can name the balancing member. */
    public static BlockPos leaderPos(UUID network) {
        return members(network).stream()
                .min(Comparator.comparingLong(be -> be.getBlockPos().asLong()))
                .map(KineticChargerBlockEntity::getBlockPos)
                .orElse(null);
    }
}
