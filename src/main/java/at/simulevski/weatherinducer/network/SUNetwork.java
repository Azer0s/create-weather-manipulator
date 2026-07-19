package at.simulevski.weatherinducer.network;

import at.simulevski.weatherinducer.content.resistor.SUResistorBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helpers that read Create's kinetic network to drive the custom SU (stress
 * unit) charging model.
 *
 * <p><b>Version-sensitive code lives here.</b> Everything this addon does that
 * pokes at Create's internal kinetic API is centralised in this one class, so
 * if a method/field name drifted between Create versions this is the single
 * place to fix it. The touchpoints are:
 * <ul>
 *   <li>{@link KineticBlockEntity#getOrCreateNetwork()} &mdash; obtain the
 *       network a block entity belongs to.</li>
 *   <li>{@link KineticNetwork#calculateCapacity()} &mdash; total SU the
 *       network's sources provide.</li>
 *   <li>{@code KineticNetwork.members} / {@code KineticNetwork.sources}
 *       &mdash; the public maps of member and source block entities, used for
 *       the inline-resistor traversal. (Adjust {@link #inlineCapPerTick} if the
 *       field names changed.)</li>
 * </ul>
 */
public final class SUNetwork {

    /** Sentinel meaning "no inline resistor gates this inducer" -> fills in one tick. */
    public static final double UNLIMITED = Double.MAX_VALUE;

    private SUNetwork() {
    }

    /**
     * Total SU the inducer's network can currently deliver. We model charging
     * as absorbing the network's stress-unit capacity, so a bigger power
     * source charges the inducer faster.
     */
    public static double availableSU(KineticBlockEntity be) {
        KineticNetwork network = be.getOrCreateNetwork();
        if (network == null) {
            return 0;
        }
        // calculateCapacity() returns the network's total provided capacity in SU.
        return Math.max(0, network.calculateCapacity());
    }

    /**
     * Computes the SU/tick the inducer is allowed to draw, given the resistors
     * that sit inline upstream of it.
     *
     * <p>Algorithm ("strictly inline upstream"): breadth-first traversal over
     * the kinetic network members starting at the inducer, using 6-neighbour
     * block adjacency as the graph edges. An {@link SUResistorBlockEntity}
     * acts as a <em>barrier</em>: its branch is capped at the resistor's
     * configured limit and the traversal does not expand past it. If any branch
     * reaches a network <em>source</em> without first passing through a
     * resistor, the inducer is ungated and can absorb everything in a single
     * tick ({@link #UNLIMITED}). Otherwise the cap is the sum of the gating
     * resistors (so parallel feeds add, and a tighter resistor in series wins
     * because it is the first barrier hit on its branch).
     *
     * <p>Limitation: adjacency is physical (straight shafts, encased shafts,
     * shaft-to-shaft). Kinetic routing that turns via large cogwheels or
     * gearboxes is not followed by the 6-neighbour walk, which is the common
     * "source -&gt; shaft -&gt; resistor -&gt; shaft -&gt; inducer" layout the
     * feature targets.
     */
    public static double inlineCapPerTick(KineticBlockEntity inducer) {
        KineticNetwork network = inducer.getOrCreateNetwork();
        if (network == null) {
            return 0;
        }

        Map<BlockPos, KineticBlockEntity> byPos = new HashMap<>();
        for (KineticBlockEntity member : network.members.keySet()) {
            byPos.put(member.getBlockPos(), member);
        }

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = inducer.getBlockPos();
        visited.add(origin);
        for (Direction d : Direction.values()) {
            queue.add(origin.relative(d));
        }

        double gatedSum = 0;
        boolean ungatedSourceReached = false;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
                continue;
            }
            KineticBlockEntity member = byPos.get(pos);
            if (member == null) {
                // No kinetic network member here; not a connection to follow.
                continue;
            }
            if (member instanceof SUResistorBlockEntity resistor) {
                // Barrier: this branch is throttled; do not expand past it.
                gatedSum += resistor.getSuLimit();
                continue;
            }
            if (network.sources.containsKey(member)) {
                ungatedSourceReached = true;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!visited.contains(next)) {
                    queue.add(next);
                }
            }
        }

        if (ungatedSourceReached) {
            return UNLIMITED;
        }
        return gatedSum;
    }

    /**
     * SU the inducer may add to its charge this tick: the network capacity,
     * clamped by whatever inline resistors allow.
     */
    public static double intakeThisTick(KineticBlockEntity inducer) {
        double available = availableSU(inducer);
        if (available <= 0) {
            return 0;
        }
        double cap = inlineCapPerTick(inducer);
        return Math.min(available, cap);
    }
}
