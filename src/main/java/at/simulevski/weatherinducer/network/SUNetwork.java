package at.simulevski.weatherinducer.network;

import at.simulevski.weatherinducer.content.charger.SUChargerBlockEntity;
import at.simulevski.weatherinducer.content.resistor.SUResistorBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 *       the inline traversal in {@link #drawSU}. (Adjust there if the field
 *       names changed.)</li>
 * </ul>
 */
public final class SUNetwork {

    private SUNetwork() {
    }

    /**
     * Lets {@code consumer} draw up to {@code wanted} SU from its kinetic
     * surroundings, and returns how much it actually got. This is the one
     * entry point of the SU model; it both computes the allowance and drains
     * any chargers that supplied part of it.
     *
     * <p>The graph walk ("strictly inline upstream") is a breadth-first
     * traversal over the kinetic network members starting at the consumer,
     * using 6-neighbour block adjacency as the edges. Two block types act as
     * barriers:
     * <ul>
     *   <li>An {@link SUResistorBlockEntity} caps its branch at the resistor's
     *       configured limit and stops the walk.</li>
     *   <li>An {@link SUChargerBlockEntity} always stops the walk (raw SU
     *       never passes through a charger). If the charger is discharging
     *       and was reached through its output face, it offers SU from its
     *       buffer instead.</li>
     * </ul>
     * If any branch reaches a network <em>source</em> without hitting a
     * barrier, the raw network supplies the draw (bounded by the network's
     * capacity); otherwise the resistor caps bound the raw part. Parallel
     * feeds add, and a tighter resistor in series wins because it is the
     * first barrier hit on its branch. Whatever the raw side cannot cover is
     * taken out of the contributing chargers' buffers.
     *
     * @param consumer   the drawing block entity (a Weather Inducer, or an SU
     *                   Charger filling its buffer)
     * @param wanted     how much SU the consumer would like this tick; the
     *                   consumer applies its own intake ceiling before calling
     * @param expandOnly if non-null, the walk leaves the consumer only through
     *                   this face (the charger passes its input face so it
     *                   never charges from its own output side)
     *
     * <p>Limitation: adjacency is physical (straight shafts, encased shafts,
     * shaft-to-shaft). Kinetic routing that turns via large cogwheels or
     * gearboxes is not followed by the 6-neighbour walk, which is the common
     * "source, shaft, resistor, machine" layout the feature targets. A
     * position is also visited only once, from whichever side the walk gets
     * there first; a charger fed and drained around a loop of shafts is not a
     * supported layout.
     */
    public static double drawSU(KineticBlockEntity consumer, double wanted,
                                @Nullable Direction expandOnly) {
        if (wanted <= 0) {
            return 0;
        }
        KineticNetwork network = consumer.getOrCreateNetwork();
        if (network == null) {
            return 0;
        }
        // calculateCapacity() returns the network's total provided SU.
        double capacity = Math.max(0, network.calculateCapacity());

        Map<BlockPos, KineticBlockEntity> byPos = new HashMap<>();
        for (KineticBlockEntity member : network.members.keySet()) {
            byPos.put(member.getBlockPos(), member);
        }

        BlockPos origin = consumer.getBlockPos();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos[]> queue = new ArrayDeque<>(); // {position, reached from}
        visited.add(origin);
        for (Direction d : Direction.values()) {
            if (expandOnly == null || d == expandOnly) {
                queue.add(new BlockPos[]{origin.relative(d), origin});
            }
        }

        double resistorSum = 0;
        double chargerSum = 0;
        boolean ungatedSourceReached = false;
        List<SUChargerBlockEntity> chargers = new ArrayList<>();

        while (!queue.isEmpty()) {
            BlockPos[] entry = queue.poll();
            BlockPos pos = entry[0];
            BlockPos from = entry[1];
            if (!visited.add(pos)) {
                continue;
            }
            KineticBlockEntity member = byPos.get(pos);
            if (member == null) {
                // No kinetic network member here; not a connection to follow.
                continue;
            }
            if (member instanceof SUResistorBlockEntity resistor) {
                // Barrier: this branch is capped; do not expand past it.
                resistorSum += resistor.getSuLimit();
                continue;
            }
            if (member instanceof SUChargerBlockEntity charger) {
                // Barrier: raw SU never crosses a charger. Its buffer is on
                // offer only while discharging, and only via the output face.
                if (pos.relative(charger.getOutputFace()).equals(from)) {
                    double offer = charger.availableDischarge();
                    if (offer > 0) {
                        chargers.add(charger);
                        chargerSum += offer;
                    }
                }
                continue;
            }
            if (network.sources.containsKey(member)) {
                ungatedSourceReached = true;
            }
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!visited.contains(next)) {
                    queue.add(new BlockPos[]{next, pos});
                }
            }
        }

        double rawAvailable = ungatedSourceReached ? capacity : Math.min(resistorSum, capacity);
        double granted = Math.min(wanted, rawAvailable + chargerSum);

        // The raw network covers what it can; chargers make up the rest.
        double fromChargers = Math.max(0, granted - rawAvailable);
        for (SUChargerBlockEntity charger : chargers) {
            if (fromChargers <= 0) {
                break;
            }
            fromChargers -= charger.drain(fromChargers);
        }
        return granted;
    }

    /**
     * Total stress demand, in SU, of the kinetic machines strictly downstream
     * of {@code origin} through {@code face}: a physical 6-neighbour walk over
     * loaded kinetic block entities, summing each member's
     * {@code calculateStressApplied() * |speed|}. Other resistors and
     * chargers end the walk (their segments answer for themselves, and SU
     * never crosses a charger anyway), though their own impact still counts.
     * The walk is bounded, so pathological shaft mazes cannot make the
     * breaker expensive.
     */
    public static double downstreamStressDemand(KineticBlockEntity origin, Direction face) {
        Level level = origin.getLevel();
        if (level == null) {
            return 0;
        }
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin.getBlockPos());
        queue.add(origin.getBlockPos().relative(face));

        double demand = 0;
        while (!queue.isEmpty() && visited.size() < 512) {
            BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof KineticBlockEntity member)) {
                continue;
            }
            demand += member.calculateStressApplied() * Math.abs(member.getSpeed());
            if (member instanceof SUResistorBlockEntity
                    || member instanceof SUChargerBlockEntity) {
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
}
