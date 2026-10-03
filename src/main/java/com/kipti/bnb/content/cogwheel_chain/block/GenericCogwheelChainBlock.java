package com.kipti.bnb.content.cogwheel_chain.block;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Chain cogwheel for addon cogwheels, bare or encased (anything in {@code bits_n_bobs:chain_drive_cogwheels}). Rather
 * than one chain block per source cogwheel, the block entity records the block state it replaced, so it can look,
 * collide, connect and drop like it, and revert to it when the chain is removed.
 */
public class GenericCogwheelChainBlock extends ConnectingCogwheelChainBlock {

    protected GenericCogwheelChainBlock(final boolean large, final Properties properties) {
        super(large, properties, null);
    }

    public static GenericCogwheelChainBlock small(final Properties properties) {
        return new GenericCogwheelChainBlock(false, properties);
    }

    public static GenericCogwheelChainBlock large(final Properties properties) {
        return new GenericCogwheelChainBlock(true, properties);
    }

    private static @Nullable BlockState getRecordedSource(@Nullable final BlockEntity be) {
        return be instanceof final CogwheelChainBlockEntity chainBE ? chainBE.getSourceState() : null;
    }

    @Override
    protected BlockState getSourceBlockState(@Nullable final BlockEntity be) {
        final BlockState source = getRecordedSource(be);
        if (source != null)
            return source;
        // Only reachable if the recorded source was lost (e.g. its mod was removed)
        return (isLargeChainCog() ? AllBlocks.LARGE_COGWHEEL : AllBlocks.COGWHEEL).getDefaultState();
    }

    @Override
    public @NotNull VoxelShape getShape(final BlockState state, final @NotNull BlockGetter level, final @NotNull BlockPos pos, final @NotNull CollisionContext context) {
        // An encased cogwheel is a full block
        final BlockState source = getRecordedSource(level.getBlockEntity(pos));
        return source != null ? source.getShape(level, pos, context) : super.getShape(state, level, pos, context);
    }

    @Override
    public boolean hasShaftTowards(final LevelReader world, final BlockPos pos, final BlockState state, final Direction face) {
        // An encased cogwheel only has shafts on the faces it was given them
        final BlockState source = getRecordedSource(world.getBlockEntity(pos));
        if (source != null && source.getBlock() instanceof final IRotate rotate)
            return rotate.hasShaftTowards(world, pos, source, face);
        return super.hasShaftTowards(world, pos, state, face);
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        final Block source = getSourceBlockState(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)).getBlock();
        return List.of(new ItemStack(source));
    }

}
