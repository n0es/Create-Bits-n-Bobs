package com.kipti.bnb.compat.greate;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point for Greate integration. Deliberately free of Greate types so it is safe to load without Greate;
 * everything that touches Greate lives in classes only reached once {@link #isLoaded()} is true.
 */
public final class GreateCompat {

    private static Boolean loaded;

    private GreateCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null)
            loaded = ModList.get().isLoaded("greate");
        return loaded;
    }

    /**
     * A chain block entity that reports the tier of the Greate cogwheel it replaced, so Greate still caps the
     * network's stress capacity at that cogwheel's material.
     */
    public static CogwheelChainBlockEntity createChainBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        return GreateChainBlockEntities.create(type, pos, state);
    }

    /**
     * The model Greate renders for this cogwheel, or null if it isn't a Greate cogwheel.
     */
    public static @Nullable PartialModel getCogwheelModel(@Nullable final Block source) {
        return isLoaded() && source != null ? GreateChainModels.getCogwheelModel(source) : null;
    }

}
