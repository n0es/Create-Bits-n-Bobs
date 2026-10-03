package com.kipti.bnb.content.cogwheel_chain.block;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The rotating cogwheel of a chain cogwheel, plus the static casing when it stands in for an encased cogwheel.
 */
public class CogwheelChainVisual extends SingleAxisRotatingVisual<CogwheelChainBlockEntity> {

    @Nullable
    private final TransformedInstance casing;

    public CogwheelChainVisual(final VisualizationContext context, final CogwheelChainBlockEntity blockEntity, final float partialTick, final Model model) {
        super(context, blockEntity, partialTick, model);
        final BlockState casingState = getCasingState(blockEntity);
        if (casingState != null) {
            final Vec3i position = getVisualPosition();
            casing = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.block(casingState)).createInstance();
            casing.translate(position.getX(), position.getY(), position.getZ()).setChanged();
        } else {
            casing = null;
        }
    }

    /**
     * An encased cogwheel's block model is just the casing; its cogwheel is drawn by the block entity.
     */
    public static @Nullable BlockState getCasingState(final CogwheelChainBlockEntity blockEntity) {
        final BlockState source = blockEntity.getSourceState();
        return source != null && source.getBlock() instanceof EncasedCogwheelBlock ? source : null;
    }

    @Override
    public void updateLight(final float partialTick) {
        super.updateLight(partialTick);
        if (casing != null)
            relight(casing);
    }

    @Override
    protected void _delete() {
        super._delete();
        if (casing != null)
            casing.delete();
    }

    @Override
    public void collectCrumblingInstances(final Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        if (casing != null)
            consumer.accept(casing);
    }

}
