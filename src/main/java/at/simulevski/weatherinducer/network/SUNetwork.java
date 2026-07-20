package at.simulevski.weatherinducer.network;

import at.simulevski.weatherinducer.content.charger.SUChargerBlockEntity;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helpers that read Create's kinetic network for the SU-consuming blocks.
 * There is deliberately no custom "SU draw" model here anymore: consumers
 * simply soak up the network's spare capacity (what the sources provide
 * minus what the machines use), and the SU Resistor polices real stress
 * demand as a breaker. Demand and draw paths are read off Create's own
 * propagation tree (each kinetic block records which neighbour drives it),
 * so belts, cogwheels and ratio gearing are accounted exactly like the
 * stress network itself accounts them.
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
     * The per-tick SU draw cap the SU Resistors between {@code consumer}
     * and its power source impose. Create records for every kinetic block
     * which neighbour drives it, so the chain of sources from the consumer
     * to the generator is exactly the path its power flows along; every
     * resistor on that chain throttles the draw, and the tightest one wins
     * (series resistors bottleneck, they do not add). A tripped resistor
     * passes nothing. Unlimited when no resistor is on the chain.
     */
    public static double resistorIntakeCap(KineticBlockEntity consumer) {
        Level level = consumer.getLevel();
        if (level == null) {
            return 0;
        }
        double cap = Double.POSITIVE_INFINITY;
        KineticBlockEntity current = consumer;
        for (int i = 0; i < MAX_WALK && current.hasSource(); i++) {
            if (!(level.getBlockEntity(current.source) instanceof KineticBlockEntity next)) {
                break;
            }
            if (next instanceof SUResistorBlockEntity resistor) {
                cap = Math.min(cap, resistor.isTripped() ? 0 : resistor.getSuLimit());
            }
            current = next;
        }
        return cap;
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
     * Total stress demand, in SU, of everything Create powers through
     * {@code origin}, measured on Create's own books. Every kinetic block
     * records which neighbour drives it, so the network's propagation forms
     * a tree; a member is downstream of {@code origin} exactly when its
     * chain of sources passes through {@code origin}'s position. For each
     * such member the demand is the network's own per-member stress
     * ({@code getActualStressOf}: impact times actual speed), so belts,
     * diagonal cogwheels and ratio changes all measure exactly what the
     * stress network charges for them.
     */
    public static double downstreamStressDemand(KineticBlockEntity origin) {
        Level level = origin.getLevel();
        KineticNetwork network = origin.getOrCreateNetwork();
        if (level == null || network == null) {
            return 0;
        }
        // Prunes stale member entries before we iterate them.
        network.calculateStress();

        Map<BlockPos, Boolean> memo = new HashMap<>();
        memo.put(origin.getBlockPos(), true);

        double demand = 0;
        for (KineticBlockEntity member : network.members.keySet()) {
            if (member == origin || member.getLevel() != level) {
                continue;
            }
            if (poweredThrough(level, member, memo)) {
                demand += Math.max(0, network.getActualStressOf(member));
            }
        }
        return demand;
    }

    /**
     * Follows {@code member}'s chain of rotation sources. True when the
     * chain runs through a position already known to lead through the
     * origin (seeded with the origin itself), false when it reaches the
     * generator without doing so. Every node on the way shares the verdict
     * and lands in the memo, so each block is walked at most once per scan.
     */
    private static boolean poweredThrough(Level level, KineticBlockEntity member,
                                          Map<BlockPos, Boolean> memo) {
        List<BlockPos> path = new ArrayList<>();
        KineticBlockEntity current = member;
        Boolean verdict = null;
        for (int i = 0; i < MAX_WALK && verdict == null; i++) {
            BlockPos pos = current.getBlockPos();
            Boolean known = memo.get(pos);
            if (known != null) {
                verdict = known;
                break;
            }
            path.add(pos);
            if (!current.hasSource()
                    || !(level.getBlockEntity(current.source) instanceof KineticBlockEntity next)) {
                verdict = false;
                break;
            }
            current = next;
        }
        boolean result = verdict != null && verdict;
        for (BlockPos pos : path) {
            memo.put(pos, result);
        }
        return result;
    }
}
