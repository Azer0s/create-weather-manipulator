package at.simulevski.weatherinducer.network;

import at.simulevski.weatherinducer.content.charger.SUChargerBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Helpers that read Create's kinetic network for the SU-consuming blocks.
 * There is deliberately no custom "SU draw" model here anymore: consumers
 * simply soak up the network's spare capacity (what the sources provide
 * minus what the machines use). The Weather Inducer's own charge time
 * slider throttles how fast that happens.
 *
 * <p><b>Version-sensitive code lives here.</b> Everything this addon does
 * that pokes at Create's internal kinetic API is centralised in this one
 * class, so if a method name drifts between Create versions this is the
 * single place to fix it. The touchpoints are
 * {@link KineticBlockEntity#getOrCreateNetwork()},
 * {@link KineticNetwork#calculateCapacity()} and
 * {@link KineticNetwork#calculateStress()}.
 */
public final class SUNetwork {

    /** Walk bound so pathological shaft mazes stay cheap to scan. */
    private static final int MAX_WALK = 512;

    private SUNetwork() {
    }

    /**
     * The network's spare SU right now: total provided capacity minus the
     * stress its machines currently use. This is what the Weather Inducer
     * and the SU Charger charge from; a stopped or overstressed network has
     * nothing to spare.
     *
     * <p>Capacity contributed by a discharging SU Charger does not count.
     * The charger spins the shafts it feeds, but its stored SU is only
     * obtainable by draining the buffer ({@link #drawFromChargers});
     * counting its capacity as spare would let consumers charge for free.
     */
    public static double remainingSU(KineticBlockEntity be) {
        KineticNetwork network = be.getOrCreateNetwork();
        if (network == null) {
            return 0;
        }
        // calculateCapacity first: it also prunes stale source entries.
        double capacity = network.calculateCapacity();
        for (KineticBlockEntity source : network.sources.keySet()) {
            if (source instanceof SUChargerBlockEntity) {
                capacity -= network.getActualCapacityOf(source);
            }
        }
        return Math.max(0, capacity - network.calculateStress());
    }

    /**
     * Pulls up to {@code wanted} SU out of discharging SU Chargers that feed
     * {@code consumer}, and returns how much was actually taken. The walk is
     * physical (6-neighbour over loaded kinetic block entities, bounded), a
     * charger always ends it (SU never passes through one), and only
     * chargers reached through their output face supply anything.
     */
    public static double drawFromChargers(KineticBlockEntity consumer, double wanted) {
        Level level = consumer.getLevel();
        if (level == null || wanted <= 0) {
            return 0;
        }
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos[]> queue = new ArrayDeque<>(); // {position, reached from}
        BlockPos origin = consumer.getBlockPos();
        visited.add(origin);
        for (Direction d : Direction.values()) {
            queue.add(new BlockPos[]{origin.relative(d), origin});
        }

        double taken = 0;
        while (!queue.isEmpty() && visited.size() < MAX_WALK && taken < wanted) {
            BlockPos[] entry = queue.poll();
            BlockPos pos = entry[0];
            BlockPos from = entry[1];
            if (!visited.add(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity member)) {
                continue;
            }
            if (member instanceof SUChargerBlockEntity charger) {
                if (pos.relative(charger.getOutputFace()).equals(from)) {
                    double offer = Math.min(charger.availableDischarge(), wanted - taken);
                    if (offer > 0) {
                        taken += charger.drain(offer);
                    }
                }
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!visited.contains(next)) {
                    queue.add(new BlockPos[]{next, pos});
                }
            }
        }
        return taken;
    }

}
