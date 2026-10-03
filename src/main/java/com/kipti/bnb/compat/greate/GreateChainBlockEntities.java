package com.kipti.bnb.compat.greate;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

final class GreateChainBlockEntities {

    private GreateChainBlockEntities() {
    }

    static CogwheelChainBlockEntity create(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        return new TieredCogwheelChainBlockEntity(type, pos, state);
    }

}
