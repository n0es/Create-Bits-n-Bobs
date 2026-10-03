package com.kipti.bnb.mixin;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChainAttachments;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Links blocks attached to a chain drive without being replaced (pumps) into the chain. Create propagates rotation one
 * direction at a time, asking the sending block entity; a pump's block entity knows nothing of chains, so answer for
 * it here, in both directions.
 */
@Mixin(value = RotationPropagator.class, remap = false)
public class RotationPropagatorChainMixin {

    @Inject(method = "getPotentialNeighbourLocations", at = @At("RETURN"), cancellable = true)
    private static void bits_n_bobs$addChainMembers(final KineticBlockEntity be, final CallbackInfoReturnable<List<BlockPos>> cir) {
        if (be instanceof CogwheelChainBlockEntity)
            return; // Chain cogwheels add their own chain's members
        final CogwheelChainBlockEntity controller = CogwheelChainAttachments.getController(be.getLevel(), be.getBlockPos());
        if (controller == null)
            return;
        final List<BlockPos> neighbours = new ArrayList<>(cir.getReturnValue());
        for (final BlockPos member : controller.getChain().getMemberPositions(controller.getBlockPos())) {
            if (!member.equals(be.getBlockPos()) && !neighbours.contains(member))
                neighbours.add(member);
        }
        cir.setReturnValue(neighbours);
    }

    @Inject(method = "getRotationSpeedModifier", at = @At("HEAD"), cancellable = true)
    private static void bits_n_bobs$chainMemberModifier(final KineticBlockEntity from, final KineticBlockEntity to, final CallbackInfoReturnable<Float> cir) {
        if (from instanceof CogwheelChainBlockEntity && to instanceof CogwheelChainBlockEntity)
            return; // Handled by CogwheelChainBlockEntity#propagateRotationTo
        final CogwheelChainBlockEntity controller = CogwheelChainAttachments.getController(from.getLevel(), from.getBlockPos());
        if (controller == null || controller != CogwheelChainAttachments.getController(to.getLevel(), to.getBlockPos()))
            return;
        final CogwheelChain chain = controller.getChain();
        final float fromSide = chain.getSideFactorAt(controller.getBlockPos(), from.getBlockPos());
        final float toSide = chain.getSideFactorAt(controller.getBlockPos(), to.getBlockPos());
        if (fromSide != 0 && toSide != 0)
            cir.setReturnValue(fromSide / toSide);
    }

}
