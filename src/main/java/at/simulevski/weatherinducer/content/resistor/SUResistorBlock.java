package at.simulevski.weatherinducer.content.resistor;

import at.simulevski.weatherinducer.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The SU Resistor: an inline shaft block (rotation passes straight through
 * along its axis, exactly like a shaft) that carries a configurable SU/tick
 * cap. It does not itself alter Create's kinetics; instead a Weather Inducer
 * that sits downstream reads any resistor found inline upstream of it and uses
 * the tightest cap to throttle how fast it charges (see
 * {@code at.simulevski.weatherinducer.network.SUNetwork}).
 */
public class SUResistorBlock extends RotatedPillarKineticBlock
        implements IBE<SUResistorBlockEntity> {

    public SUResistorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<SUResistorBlockEntity> getBlockEntityClass() {
        return SUResistorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SUResistorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SU_RESISTOR.get();
    }
}
