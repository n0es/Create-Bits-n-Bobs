package com.kipti.bnb.content.cogwheel_chain.block;

import com.kipti.bnb.compat.greate.GreateCompat;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The rotating cogwheel of a chain cogwheel, plus, when it stands in for an encased cogwheel, the static casing and
 * a shaft stub on each side the casing is open.
 */
public class CogwheelChainVisual extends SingleAxisRotatingVisual<CogwheelChainBlockEntity> {

    @Nullable
    private final TransformedInstance casing;
    private final List<RotatingInstance> shaftHalves = new ArrayList<>();

    public CogwheelChainVisual(final VisualizationContext context, final CogwheelChainBlockEntity blockEntity, final float partialTick, final Model model) {
        super(context, blockEntity, partialTick, model);
        final BlockState casingState = getCasingState(blockEntity);
        if (casingState == null) {
            casing = null;
            return;
        }

        final Vec3i position = getVisualPosition();
        casing = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.block(casingState)).createInstance();
        casing.translate(position.getX(), position.getY(), position.getZ()).setChanged();

        // As EncasedCogVisual (and Greate's tiered one) does it
        final Model shaftHalf = Models.partial(getShaftHalfModel(casingState.getBlock()));
        final boolean large = ((EncasedCogwheelBlock) casingState.getBlock()).isLargeCog();
        for (final Direction d : getOpenSides(blockEntity, casingState)) {
            final RotatingInstance shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, shaftHalf).createInstance();
            shaft.setup(blockEntity).setPosition(position).rotateToFace(Direction.SOUTH, d);
            if (large)
                shaft.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
            shaft.setChanged();
            shaftHalves.add(shaft);
        }
    }

    /**
     * An encased cogwheel's block model is just the casing; its cogwheel and shafts are drawn by the block entity.
     */
    public static @Nullable BlockState getCasingState(final CogwheelChainBlockEntity blockEntity) {
        final BlockState source = blockEntity.getSourceState();
        return source != null && source.getBlock() instanceof EncasedCogwheelBlock ? source : null;
    }

    /**
     * The sides along the axis where the casing is open, so a shaft stub shows.
     */
    public static List<Direction> getOpenSides(final CogwheelChainBlockEntity blockEntity, final BlockState casingState) {
        final List<Direction> sides = new ArrayList<>();
        if (!(casingState.getBlock() instanceof final IRotate rotate))
            return sides;
        for (final Direction d : Iterate.directionsInAxis(casingState.getValue(EncasedCogwheelBlock.AXIS))) {
            if (rotate.hasShaftTowards(blockEntity.getLevel(), blockEntity.getBlockPos(), casingState, d))
                sides.add(d);
        }
        return sides;
    }

    /**
     * Greate retextures the shaft per material; anything else uses Create's.
     */
    public static PartialModel getShaftHalfModel(final Block encased) {
        final PartialModel greate = GreateCompat.getShaftHalfModel(encased);
        return greate != null ? greate : AllPartialModels.SHAFT_HALF;
    }

    @Override
    public void update(final float partialTick) {
        super.update(partialTick);
        for (final RotatingInstance shaft : shaftHalves)
            shaft.setup(blockEntity).setChanged();
    }

    @Override
    public void updateLight(final float partialTick) {
        super.updateLight(partialTick);
        if (casing != null)
            relight(casing);
        for (final RotatingInstance shaft : shaftHalves)
            relight(shaft);
    }

    @Override
    protected void _delete() {
        super._delete();
        if (casing != null)
            casing.delete();
        shaftHalves.forEach(Instance::delete);
    }

    @Override
    public void collectCrumblingInstances(final Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        if (casing != null)
            consumer.accept(casing);
        shaftHalves.forEach(consumer);
    }

}
