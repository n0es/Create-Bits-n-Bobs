package com.kipti.bnb.compat.greate;

import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import electrolyte.greate.content.kinetics.simpleRelays.TieredCogwheelBlock;
import electrolyte.greate.content.kinetics.simpleRelays.encased.TieredEncasedCogwheelBlock;
import electrolyte.greate.foundation.client.models.GreateModelUtils;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

final class GreateChainModels {

    private GreateChainModels() {
    }

    static @Nullable PartialModel getShaftHalfModel(final Block encased) {
        return encased instanceof TieredEncasedCogwheelBlock ? GreateModelUtils.getPartialModel(encased, "/shaft_half") : null;
    }

    static @Nullable Block getBareCogwheel(final Block encased) {
        return encased instanceof final TieredEncasedCogwheelBlock tiered ? tiered.getCogWheel() : null;
    }

    /**
     * Greate retextures Create's cogwheel models per material at runtime, under {@code greate:block/<material>/}.
     */
    static @Nullable PartialModel getCogwheelModel(final Block source) {
        final boolean large = ICogWheel.isLargeCog(source.defaultBlockState());
        if (source instanceof TieredCogwheelBlock)
            return GreateModelUtils.getPartialModel(source, large ? "/large_cogwheel" : "/cogwheel");
        // Encased: the shaft would poke through the casing; the casing is drawn separately
        if (source instanceof TieredEncasedCogwheelBlock)
            return GreateModelUtils.getPartialModel(source, large ? "/large_cogwheel_shaftless" : "/cogwheel_shaftless");
        return null;
    }

}
