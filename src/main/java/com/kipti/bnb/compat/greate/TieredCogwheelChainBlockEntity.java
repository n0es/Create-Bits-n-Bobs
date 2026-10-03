package com.kipti.bnb.compat.greate;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import electrolyte.greate.content.kinetics.simpleRelays.ITieredBlock;
import electrolyte.greate.content.kinetics.simpleRelays.ITieredKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Greate caps a kinetic network's capacity at the weakest tiered component in it, by asking each member for the
 * capacity of its block. A chain cogwheel's own block isn't tiered, so answer for the cogwheel it replaced.
 */
public class TieredCogwheelChainBlockEntity extends CogwheelChainBlockEntity implements ITieredKineticBlockEntity {

    public TieredCogwheelChainBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getMaxCapacityFromBlock(final Block block) {
        final Block source = getSourceBlock();
        return ITieredKineticBlockEntity.super.getMaxCapacityFromBlock(source != null ? source : block);
    }

    @Override
    public boolean addToGoggleTooltip(final List<Component> tooltip, final boolean isPlayerSneaking) {
        final boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (getSourceBlock() instanceof final ITieredBlock tieredBlock)
            return ITieredKineticBlockEntity.super.addToGoggleTooltip(tooltip, isPlayerSneaking, tieredBlock.getMaterial(), capacity, stress) || added;
        return added;
    }

}
