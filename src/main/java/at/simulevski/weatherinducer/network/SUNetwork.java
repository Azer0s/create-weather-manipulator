package at.simulevski.weatherinducer.network;

import at.simulevski.weatherinducer.content.charger.SUChargerBlockEntity;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlockEntity;
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
 * minus what the machines use), and the SU Resistor polices real stress
 * demand as a breaker.
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
     * The total SU the network of {@code be} provides right now. The SU
     * Resistor's breaker reads this to decide when a recorded break demand
     * is covered again; capacity is source-side only, so the reading works
     * even while the breaker holds the downstream detached.
     */
    public static double providedSU(KineticBlockEntity be) {
        KineticNetwork network = be.getOrCreateNetwork();
        return network == null ? 0 : Math.max(0, network.calculateCapacity());
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
     * The per-tick SU draw cap the SU Resistors around {@code consumer}
     * impose: when every reachable generator sits behind a resistor, the
     * consumer may pull at most the sum of their limits per tick, and a
     * tripped resistor passes nothing. Unlimited when no resistor is
     * nearby, or when some generator is reachable without crossing one.
     * The walk is physical and bounded like the others; chargers end it
     * (SU never crosses a charger).
     */
    public static double resistorIntakeCap(KineticBlockEntity consumer) {
        Level level = consumer.getLevel();
        if (level == null) {
            return 0;
        }
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(consumer.getBlockPos());
        for (Direction d : Direction.values()) {
            queue.add(consumer.getBlockPos().relative(d));
        }

        boolean sawResistor = false;
        double cap = 0;
        while (!queue.isEmpty() && visited.size() < MAX_WALK) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity member)) {
                continue;
            }
            if (member instanceof SUChargerBlockEntity) {
                continue;
            }
            if (member instanceof SUResistorBlockEntity resistor) {
                sawResistor = true;
                if (!resistor.isTripped()) {
                    cap += resistor.getSuLimit();
                }
                continue;
            }
            if (member.getGeneratedSpeed() != 0) {
                // A live generator with no resistor between us and it.
                return Double.POSITIVE_INFINITY;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!visited.contains(next)) {
                    queue.add(next);
                }
            }
        }
        return sawResistor ? cap : Double.POSITIVE_INFINITY;
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

    /**
     * Total stress demand, in SU, of the kinetic machines strictly downstream
     * of {@code origin} through {@code face}: a physical 6-neighbour walk over
     * loaded kinetic block entities, summing each member's
     * {@code calculateStressApplied()} times the <em>origin's</em> speed.
     * Using the origin's speed (instead of each member's) makes the estimate
     * identical whether the breaker is closed or tripped, so the SU Resistor
     * latches cleanly instead of oscillating. Straight shaft lines run at one
     * speed anyway; ratio changes through cogs are outside the walk's scope.
     * Other resistors and chargers end the walk (their segments answer for
     * themselves, and SU never crosses a charger), though their own impact
     * still counts.
     */
    public static double downstreamStressDemand(KineticBlockEntity origin, Direction face) {
        Level level = origin.getLevel();
        if (level == null) {
            return 0;
        }
        double speed = Math.abs(origin.getSpeed());
        if (speed == 0) {
            return 0;
        }
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin.getBlockPos());
        queue.add(origin.getBlockPos().relative(face));

        double demand = 0;
        while (!queue.isEmpty() && visited.size() < MAX_WALK) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity member)) {
                continue;
            }
            demand += member.calculateStressApplied() * speed;
            if (isSuBarrier(member)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!visited.contains(next)) {
                    queue.add(next);
                }
            }
        }
        return demand;
    }

    private static boolean isSuBarrier(KineticBlockEntity member) {
        return member instanceof SUChargerBlockEntity
                || member instanceof SUResistorBlockEntity;
    }
}
