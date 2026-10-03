package com.kipti.bnb.content.cogwheel_chain.graph;

import com.kipti.bnb.CreateBitsnBobs;
import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlock;
import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.block.GenericCogwheelChainBlock;
import com.kipti.bnb.registry.BnbBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CogwheelChain {

    private final List<PathedCogwheelNode> cogwheelNodes;
    private List<RenderedChainPathNode> renderedNodes;

    public CogwheelChain(final CompoundTag tag) {
        renderedNodes = new ArrayList<>();
        cogwheelNodes = new ArrayList<>();
        read(tag);
    }

    public CogwheelChain(final List<PathedCogwheelNode> path) {
        this.cogwheelNodes = path;
        this.renderedNodes = CogwheelChainGeometryBuilder.buildFullChainFromPathNodes(path);
    }

    public @Nullable PathedCogwheelNode getNodeFromControllerOffset(final Vec3i controllerOffset) {
        final Vec3i offsetFromStart = controllerOffset.multiply(-1);

        for (final PathedCogwheelNode cogwheelNode : cogwheelNodes) {
            if (cogwheelNode.localPos().equals(offsetFromStart)) {
                return cogwheelNode;
            }
        }
        return null;
    }

    public boolean checkIntegrity(final Level level, final BlockPos origin) {
        for (final PathedCogwheelNode node : this.cogwheelNodes) {
            final BlockState state = level.getBlockState(node.localPos().offset(origin));
            final boolean attached = PlacingCogwheelChain.isAttachedBlockTarget(state);
            if (!attached && !isValidChainCogwheel(state)) {
                return false;
            }
            final Direction.Axis axis = PlacingCogwheelChain.getAxis(state);
            final boolean isLarge = attached ? ICogWheel.isLargeCog(state) :
                    state.getBlock() instanceof final CogwheelChainBlock iCogWheel && iCogWheel.isLargeChainCog();
            if (axis != node.rotationAxis() || isLarge != node.isLarge()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidChainCogwheel(final BlockState state) {
        return BnbBlocks.LARGE_SPROCKET_COGWHEEL_CHAIN.is(state.getBlock()) || BnbBlocks.SMALL_SPROCKET_COGWHEEL_CHAIN.is(state.getBlock()) ||
                BnbBlocks.LARGE_FLANGED_COGWHEEL_CHAIN.is(state.getBlock()) || BnbBlocks.SMALL_FLANGED_COGWHEEL_CHAIN.is(state.getBlock()) ||
                BnbBlocks.LARGE_GENERIC_COGWHEEL_CHAIN.is(state.getBlock()) || BnbBlocks.SMALL_GENERIC_COGWHEEL_CHAIN.is(state.getBlock());
    }

    public int getChainsRequired() {
        double length = 0;
        for (int i = 0; i <= cogwheelNodes.size(); i++) {
            final PathedCogwheelNode startNode = cogwheelNodes.get(i % cogwheelNodes.size());
            final PathedCogwheelNode endNode = cogwheelNodes.get((i + 1) % cogwheelNodes.size());
            length += startNode.dist(endNode);
        }
        return PlacingCogwheelChain.getChainsRequiredForLength(length);
    }

    public void write(final CompoundTag tag) {
        tag.putInt("cogwheel_pos_count", cogwheelNodes.size());
        for (int i = 0; i < cogwheelNodes.size(); i++) {
            final CompoundTag posTag = new CompoundTag();
            cogwheelNodes.get(i).write(posTag);
            tag.put("cogwheel_pos_" + i, posTag);
        }
    }

    public void read(final CompoundTag tag) {
        cogwheelNodes.clear();
        final int cogWheelPosCount = tag.getInt("cogwheel_pos_count");
        for (int i = 0; i < cogWheelPosCount; i++) {
            final CompoundTag posTag = tag.getCompound("cogwheel_pos_" + i);
            final PathedCogwheelNode pos = PathedCogwheelNode.read(posTag);
            cogwheelNodes.add(pos);
        }
        renderedNodes.clear();
        renderedNodes = CogwheelChainGeometryBuilder.buildFullChainFromPathNodes(cogwheelNodes);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final CogwheelChain that = (CogwheelChain) o;
        return Objects.equals(renderedNodes, that.renderedNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(renderedNodes);
    }

    public void placeInLevel(final Level level, final PlacingCogwheelChain source, final Item chainItem) {
        boolean isController = true;
        final BlockPos controllerPos = source.getFirstNode().pos();
        final int chainsUsed = source.getChainsRequiredInLoop();
        for (final PlacingCogwheelNode node : source.getVisitedNodes()) {
            placeChainCogwheelInLevel(level, node, isController, chainsUsed, controllerPos, chainItem);
            isController = false;
        }

    }

    private void placeChainCogwheelInLevel(final Level level, final PlacingCogwheelNode node, final boolean isController, final int chainsUsed, final BlockPos controllerPos, final Item chainItem) {
        final BlockState existingState = level.getBlockState(node.pos());
        // Attached blocks (pumps) stay as they are; the controller links them in via CogwheelChainAttachments
        if (!isController && PlacingCogwheelChain.isAttachedBlockTarget(existingState))
            return;

        @Nullable final BlockState newState = CogwheelChainBlock.getChainState(existingState, node.isLarge(), node.rotationAxis());

        if (newState == null) {
            CreateBitsnBobs.LOGGER.error("Failed to place cogwheel chain at {}, existing block {}, because the chain state could not be resolved", node.pos(), existingState);
            return;
        }
        level.setBlockAndUpdate(node.pos(), newState);

        final BlockEntity be = level.getBlockEntity(node.pos());
        if (be instanceof final CogwheelChainBlockEntity chainBE) {
            // Set in the same tick as placement, before the block entity joins a kinetic network
            if (newState.getBlock() instanceof GenericCogwheelChainBlock)
                chainBE.setSourceState(existingState);
            if (isController) {
                chainBE.setAsController(this);
                chainBE.setChainsUsed(chainsUsed);
                chainBE.setChainItem(chainItem);
            } else {
                chainBE.setController(controllerPos.subtract(node.pos()));
            }
        } else {
            throw new IllegalStateException("Expected CogwheelChainBlockEntity at " + node.pos());
        }
    }

    public void destroy(final Level level, final BlockPos worldPosition) {
        for (final PathedCogwheelNode cogwheel : cogwheelNodes) {
            final BlockPos pos = worldPosition.offset(cogwheel.localPos());
            removeChainCogwheelFromLevelIfPresent(level, pos);
        }
    }

    public static void removeChainCogwheelFromLevelIfPresent(final Level level, final BlockPos pos) {
        final BlockEntity be = level.getBlockEntity(pos);
        final BlockState state = level.getBlockState(pos);
        if (be instanceof CogwheelChainBlockEntity && (state.getBlock() instanceof final CogwheelChainBlock cogwheelChainBlock)) {
            level.setBlockAndUpdate(pos, cogwheelChainBlock.getSourceBlockState(level, pos)
                    .setValue(CogwheelChainBlock.AXIS, state.getValue(CogwheelChainBlock.AXIS)));
        }
    }

    /**
     * Where the ray {@code from -> to} first passes within {@code radius} of the chain.
     *
     * @param origin the controller's position, which the path nodes are relative to
     */
    public @Nullable RayHit getRayHit(final BlockPos origin, final Vec3 from, final Vec3 to, final double radius) {
        final int size = renderedNodes.size();
        RayHit best = null;
        for (int i = 0; i < size; i++) {
            final double[] closest = closestPointsBetweenSegments(from, to, getSegmentStart(origin, i), getSegmentEnd(origin, i));
            if (closest[2] > radius * radius)
                continue;
            final double distance = closest[0] * from.distanceTo(to);
            if (best == null || distance < best.distance())
                best = new RayHit(i, distance);
        }
        return best;
    }

    public record RayHit(int segment, double distance) {
    }

    /**
     * World position of the start of a straight chain segment; segment i runs from node i to node i + 1.
     */
    public Vec3 getSegmentStart(final BlockPos origin, final int segment) {
        return renderedNodes.get(segment % renderedNodes.size()).getPosition().add(Vec3.atLowerCornerOf(origin));
    }

    public Vec3 getSegmentEnd(final BlockPos origin, final int segment) {
        return getSegmentStart(origin, segment + 1);
    }

    /**
     * World positions of every cogwheel (or attached block) the chain runs around.
     */
    public List<BlockPos> getMemberPositions(final BlockPos origin) {
        return cogwheelNodes.stream().map(node -> origin.offset(node.localPos())).toList();
    }

    /**
     * Which way round the chain turns the member at {@code pos} (+1 or -1), or 0 if it isn't part of the chain.
     */
    public float getSideFactorAt(final BlockPos origin, final BlockPos pos) {
        final PathedCogwheelNode node = getNodeFromControllerOffset(origin.subtract(pos));
        return node == null ? 0 : node.sideFactor();
    }

    /**
     * Squared distance from {@code point} to the nearest part of the chain.
     */
    public double getDistanceSqTo(final BlockPos origin, final Vec3 point) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < renderedNodes.size(); i++)
            best = Math.min(best, closestPointsBetweenSegments(point, point, getSegmentStart(origin, i), getSegmentEnd(origin, i))[2]);
        return best;
    }

    /**
     * Closest approach between segments p0-p1 and q0-q1 (Ericson, Real-Time Collision Detection 5.1.9).
     *
     * @return {s, t, squared distance}, where s and t are the parameters of the closest points along each segment
     */
    private static double[] closestPointsBetweenSegments(final Vec3 p0, final Vec3 p1, final Vec3 q0, final Vec3 q1) {
        final Vec3 d1 = p1.subtract(p0);
        final Vec3 d2 = q1.subtract(q0);
        final Vec3 r = p0.subtract(q0);
        final double a = d1.lengthSqr();
        final double e = d2.lengthSqr();
        final double f = d2.dot(r);
        double s;
        double t;
        if (a <= 1e-9 && e <= 1e-9) {
            s = t = 0;
        } else if (a <= 1e-9) {
            s = 0;
            t = Mth.clamp(f / e, 0, 1);
        } else {
            final double c = d1.dot(r);
            if (e <= 1e-9) {
                t = 0;
                s = Mth.clamp(-c / a, 0, 1);
            } else {
                final double b = d1.dot(d2);
                final double denom = a * e - b * b;
                s = denom > 1e-9 ? Mth.clamp((b * f - c * e) / denom, 0, 1) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = Mth.clamp(-c / a, 0, 1);
                } else if (t > 1) {
                    t = 1;
                    s = Mth.clamp((b - c) / a, 0, 1);
                }
            }
        }
        final Vec3 closestP = p0.add(d1.scale(s));
        final Vec3 closestQ = q0.add(d2.scale(t));
        return new double[]{s, t, closestP.distanceToSqr(closestQ)};
    }

    /**
     * All nodes in the chain, there are typically multiple, as the path wraps around cogwheels
     */
    public List<RenderedChainPathNode> getChainPathNodes() {
        return renderedNodes;
    }

    /**
     * Each cogwheel in the chain
     */
    public List<PathedCogwheelNode> getChainPathCogwheelNodes() {
        return cogwheelNodes;
    }
}
