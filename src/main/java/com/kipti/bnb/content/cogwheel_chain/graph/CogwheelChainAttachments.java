package com.kipti.bnb.content.cogwheel_chain.graph;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-side index of which chain each member position belongs to. Chain cogwheels know their chain through their
 * block entity, but attached blocks (pumps) are never replaced, so rotation propagation finds their chain here
 * (see {@code RotationPropagatorChainMixin}). Maintained by each chain's controller while it is loaded.
 */
public final class CogwheelChainAttachments {

    private static final Map<LevelAccessor, Map<BlockPos, BlockPos>> CONTROLLER_BY_MEMBER = new WeakHashMap<>();

    private CogwheelChainAttachments() {
    }

    public static void register(final Level level, final BlockPos controllerPos, final CogwheelChain chain) {
        if (level.isClientSide)
            return;
        final Map<BlockPos, BlockPos> members = CONTROLLER_BY_MEMBER.computeIfAbsent(level, $ -> new HashMap<>());
        for (final BlockPos member : chain.getMemberPositions(controllerPos))
            members.put(member.immutable(), controllerPos.immutable());
    }

    public static void unregister(final Level level, final BlockPos controllerPos) {
        if (level.isClientSide)
            return;
        final Map<BlockPos, BlockPos> members = CONTROLLER_BY_MEMBER.get(level);
        if (members != null)
            members.values().removeIf(controllerPos::equals);
    }

    /**
     * The loaded controller of the chain that runs around {@code member}, if any.
     */
    public static @Nullable CogwheelChainBlockEntity getController(@Nullable final Level level, final BlockPos member) {
        if (level == null || level.isClientSide)
            return null;
        final Map<BlockPos, BlockPos> members = CONTROLLER_BY_MEMBER.get(level);
        if (members == null)
            return null;
        final BlockPos controllerPos = members.get(member);
        if (controllerPos == null)
            return null;
        final BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof final CogwheelChainBlockEntity controller && controller.isController() && controller.getChain() != null)
            return controller;
        return null;
    }

}
