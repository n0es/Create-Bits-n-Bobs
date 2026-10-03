package com.kipti.bnb.content.cogwheel_chain.block;

import com.kipti.bnb.compat.greate.GreateCompat;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Chain cogwheel for addon cogwheels, bare or encased (anything in {@code bits_n_bobs:chain_drive_cogwheels}). Rather
 * than one chain block per source cogwheel, the block entity records the block state it replaced, so it can look,
 * collide, connect and drop like it, and revert to it when the chain is removed. Encasing and the encased cogwheel's
 * wrench actions work on the recorded state, so they work with the chain on.
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

    /**
     * Swap the cogwheel this stands in for, re-linking its kinetics the way {@code KineticBlockEntity.switchToBlockState}
     * does for a real block change (the shafts, and so the connections, may differ).
     */
    private static void switchSource(final CogwheelChainBlockEntity be, final BlockState newSource) {
        if (be.hasNetwork())
            be.getOrCreateNetwork().remove(be);
        be.detachKinetics();
        be.removeSource();
        be.setSourceState(newSource);
        be.sendData();
        be.attachKinetics();
    }

    // Encasing with the chain on, as CogWheelBlock#use / EncasableBlock#tryEncase do for a bare cogwheel

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull InteractionResult use(final @NotNull BlockState state, final @NotNull Level level, final @NotNull BlockPos pos, final Player player, final @NotNull InteractionHand hand, final @NotNull BlockHitResult ray) {
        if (player.isShiftKeyDown() || !player.mayBuild())
            return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof final CogwheelChainBlockEntity be) || be.getSourceState() == null)
            return InteractionResult.PASS;

        final BlockState source = be.getSourceState();
        final ItemStack heldItem = player.getItemInHand(hand);
        for (final Block variant : EncasingRegistry.getVariants(source.getBlock())) {
            if (!(variant instanceof final EncasedBlock encased) || encased.getCasing().asItem() != heldItem.getItem())
                continue;
            if (!variant.defaultBlockState().hasProperty(AXIS))
                continue;
            if (level.isClientSide)
                return InteractionResult.SUCCESS;

            final BlockState encasedState = getEncasedState(variant, source, level, pos);
            switchSource(be, encasedState);
            final SoundType soundType = encasedState.getSoundType();
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1) / 2, soundType.getPitch() * .8f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Mirrors EncasedCogwheelBlock#handleEncasing: open the casing on each side a neighbour has a shaft towards.
     */
    private static BlockState getEncasedState(final Block variant, final BlockState source, final Level level, final BlockPos pos) {
        final Direction.Axis axis = source.getValue(AXIS);
        BlockState encasedState = variant.defaultBlockState().setValue(AXIS, axis);
        if (!(variant instanceof EncasedCogwheelBlock))
            return encasedState;
        for (final Direction d : Iterate.directionsInAxis(axis)) {
            final BlockState adjacentState = level.getBlockState(pos.relative(d));
            if (adjacentState.getBlock() instanceof final IRotate rotate
                    && rotate.hasShaftTowards(level, pos.relative(d), adjacentState, d.getOpposite()))
                encasedState = encasedState.setValue(getShaftProperty(d), true);
        }
        return encasedState;
    }

    private static BooleanProperty getShaftProperty(final Direction face) {
        return face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? EncasedCogwheelBlock.TOP_SHAFT : EncasedCogwheelBlock.BOTTOM_SHAFT;
    }

    // An encased cogwheel's wrench actions, as EncasedCogwheelBlock does them

    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof final CogwheelChainBlockEntity be
                && be.getSourceState() != null
                && be.getSourceState().getBlock() instanceof EncasedCogwheelBlock
                && context.getClickedFace().getAxis() == state.getValue(AXIS)) {
            // Open or close the casing on the clicked side
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            switchSource(be, be.getSourceState().cycle(getShaftProperty(context.getClickedFace())));
            IWrenchable.playRotateSound(level, pos);
            return InteractionResult.SUCCESS;
        }
        return super.onWrenched(state, context);
    }

    @Override
    public InteractionResult onSneakWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof final CogwheelChainBlockEntity be
                && be.getSourceState() != null
                && be.getSourceState().getBlock() instanceof EncasedCogwheelBlock) {
            // Take the casing off, keeping the chain; removing the chain is the next sneak-wrench
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            final BlockState encased = be.getSourceState();
            level.levelEvent(2001, pos, Block.getId(encased));
            switchSource(be, getBareCogwheel(encased.getBlock()).defaultBlockState().setValue(AXIS, encased.getValue(AXIS)));
            return InteractionResult.SUCCESS;
        }
        return super.onSneakWrenched(state, context);
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
    public @NotNull List<ItemStack> getDrops(final @NotNull BlockState state, final LootParams.Builder params) {
        // Drop what the replaced block drops (an encased cogwheel has no item of its own)
        final BlockState source = getSourceBlockState(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY));
        final List<ItemStack> drops = source.getDrops(params);
        if (!drops.isEmpty())
            return drops;
        // Greate ships no loot tables for its encased cogwheels; breaking one gives back the bare cogwheel
        final Block block = source.getBlock() instanceof EncasedCogwheelBlock ? getBareCogwheel(source.getBlock()) : source.getBlock();
        return block.asItem() == Items.AIR ? List.of() : List.of(new ItemStack(block));
    }

    private Block getBareCogwheel(final Block encased) {
        final Block bare = GreateCompat.getBareCogwheel(encased);
        if (bare != null)
            return bare;
        return isLargeChainCog() ? AllBlocks.LARGE_COGWHEEL.get() : AllBlocks.COGWHEEL.get();
    }

}
