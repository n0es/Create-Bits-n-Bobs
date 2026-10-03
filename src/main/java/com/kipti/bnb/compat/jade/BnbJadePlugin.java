package com.kipti.bnb.compat.jade;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * A chain cogwheel stands in for the cogwheel it was made from, so show that cogwheel (name, mod and icon) rather
 * than "Cogwheel Chain". Only loaded by Jade, via the annotation.
 */
@WailaPlugin
public class BnbJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(final IWailaClientRegistration registration) {
        registration.addRayTraceCallback((hitResult, accessor, originalAccessor) -> {
            if (!(accessor instanceof final BlockAccessor blockAccessor)
                    || !(blockAccessor.getBlock() instanceof final CogwheelChainBlock chainBlock))
                return accessor;
            return registration.blockAccessor()
                    .from(blockAccessor)
                    .blockState(chainBlock.getSourceBlockState(blockAccessor.getLevel(), blockAccessor.getPosition()))
                    .build();
        });
    }

}
