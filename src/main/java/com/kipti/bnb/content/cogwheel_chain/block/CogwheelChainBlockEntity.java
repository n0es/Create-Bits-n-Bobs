package com.kipti.bnb.content.cogwheel_chain.block;

import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChainAttachments;
import com.kipti.bnb.content.cogwheel_chain.graph.PathedCogwheelNode;
import com.kipti.bnb.compat.greate.GreateCompat;
import com.kipti.bnb.content.girder_strut.IBlockEntityRelighter;
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class CogwheelChainBlockEntity extends SimpleKineticBlockEntity implements IBlockEntityRelighter, SpecialBlockEntityItemRequirement {

    /** Chain cogwheels loaded on the client, so chains can be picked out of the air with a wrench. */
    private static final Set<CogwheelChainBlockEntity> CLIENT_LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private boolean isController = false;
    @Nullable
    private CogwheelChain chain = null;
    @Nullable
    private Vec3i controllerOffset = null;
    private int chainsToRefund = 0;
    /** Controller only: the chain item the loop was built from, refunded when it breaks. */
    private Item chainItem = Items.CHAIN;
    /** Generic chain cogwheels only: the cogwheel this replaced, with its axis (and casing shafts, if encased). */
    @Nullable
    private BlockState sourceState = null;

    public CogwheelChainBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        setLazyTickRate(5);
    }

    public static CogwheelChainBlockEntity create(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        return GreateCompat.isLoaded() ? GreateCompat.createChainBlockEntity(type, pos, state) : new CogwheelChainBlockEntity(type, pos, state);
    }

    public static Set<CogwheelChainBlockEntity> getClientLoaded() {
        return CLIENT_LOADED;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide)
            CLIENT_LOADED.add(this);
        if (level != null && isController && chain != null)
            CogwheelChainAttachments.register(level, worldPosition, chain);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        CLIENT_LOADED.remove(this);
        if (level != null && isController)
            CogwheelChainAttachments.unregister(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_LOADED.remove(this);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (isController && chain != null) {
            if (!chain.checkIntegrity(level, worldPosition)) {
                destroyChain(true);
            }
        } else {
            if (controllerOffset != null && level != null) {
                final BlockPos controllerPos = worldPosition.offset(controllerOffset);
                final BlockEntity be = level.getBlockEntity(controllerPos);
                if (!(be instanceof CogwheelChainBlockEntity)) {
                    CogwheelChain.removeChainCogwheelFromLevelIfPresent(level, getBlockPos());
                }
            }
        }
    }

    @Override
    protected void read(final CompoundTag compound, final boolean clientPacket) {
        super.read(compound, clientPacket);
        final BlockState previousSource = sourceState;
        sourceState = compound.contains("SourceState")
                ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), compound.getCompound("SourceState"))
                : null;
        if (sourceState != null && sourceState.isAir())
            sourceState = null;
        if (clientPacket && previousSource != sourceState)
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CogwheelChainClientHooks.sourceChanged(this));

        isController = compound.getBoolean("IsController");
        if (compound.contains("ControllerOffsetX")) {
            controllerOffset = new Vec3i(
                    compound.getInt("ControllerOffsetX"),
                    compound.getInt("ControllerOffsetY"),
                    compound.getInt("ControllerOffsetZ")
            );
        } else {
            controllerOffset = null;
        }

        if (isController) {
            chainsToRefund = compound.getInt("ChainsToRefund");
            final Item storedChain = compound.contains("ChainItem")
                    ? ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(compound.getString("ChainItem")))
                    : null;
            chainItem = storedChain == null || storedChain == Items.AIR ? Items.CHAIN : storedChain;
            if (chain != null && compound.contains("Chain")) {
                chain.read(compound.getCompound("Chain"));
            } else {
                chain = new CogwheelChain(compound.getCompound("Chain"));
            }
        } else {
            chain = null;
        }
    }

    @Override
    protected void write(final CompoundTag compound, final boolean clientPacket) {
        super.write(compound, clientPacket);
        writeConnectionInfo(compound);
    }

    @Override
    public void writeSafe(final CompoundTag tag) {
        super.writeSafe(tag);
        writeConnectionInfo(tag);
    }

    private void writeConnectionInfo(final CompoundTag compound) {
        compound.putBoolean("IsController", isController);

        if (sourceState != null)
            compound.put("SourceState", NbtUtils.writeBlockState(sourceState));

        if (controllerOffset != null) {
            compound.putInt("ControllerOffsetX", controllerOffset.getX());
            compound.putInt("ControllerOffsetY", controllerOffset.getY());
            compound.putInt("ControllerOffsetZ", controllerOffset.getZ());
        }

        if (isController && chain != null) {
            final CompoundTag chainTag = new CompoundTag();
            chain.write(chainTag);
            compound.put("Chain", chainTag);
            compound.putInt("ChainsToRefund", chainsToRefund);
            compound.putString("ChainItem", ForgeRegistries.ITEMS.getKey(chainItem).toString());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        destroyChain(true);
    }

    public ItemStack destroyChain(final boolean dropItemsInWorld) {
        //Try drop chains from the current block for convenience
        int chainsToReturn = chainsToRefund;
        Item chainToReturn = chainItem;
        if (!isController && controllerOffset != null && level != null) {
            final BlockPos controllerPos = worldPosition.offset(controllerOffset);
            final BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof final CogwheelChainBlockEntity controllerBE) {
                chainsToReturn = controllerBE.chainsToRefund;
                chainToReturn = controllerBE.chainItem;
                controllerBE.chainsToRefund = 0;
            }
        }
        final ItemStack drops = new ItemStack(chainToReturn, chainsToReturn);
        if (dropItemsInWorld) {
            Block.popResource(level, worldPosition, drops);
        }
        this.chainsToRefund = 0; // Reset after dropping

        if (isController && chain != null) {
            chain.destroy(level, worldPosition);
        }
        if (!isController && controllerOffset != null && level != null) {
            final BlockPos controllerPos = worldPosition.offset(controllerOffset);
            final BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof final CogwheelChainBlockEntity controllerBE) {
                assert controllerBE.chain != null;
                controllerBE.chain.destroy(level, controllerPos);
            }
        }
        return drops;
    }

    public void setController(final Vec3i offset) {
        this.isController = false;
        this.controllerOffset = offset;
    }

    public void setAsController(final CogwheelChain cogwheelChain) {
        this.isController = true;
        this.chain = cogwheelChain;
        if (level != null)
            CogwheelChainAttachments.register(level, worldPosition, cogwheelChain);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(64);
    }

    @Override
    public List<BlockPos> addPropagationLocations(final IRotate block, final BlockState state, final List<BlockPos> neighbours) {
        final List<BlockPos> toPropagate = new ArrayList<>(super.addPropagationLocations(block, state, neighbours));

        if (isController && chain != null) {
            addPropogationLocationsFromController(toPropagate, getBlockPos());
        } else {
            //Test putting child to child connections
            if (controllerOffset != null && level != null) {
                final BlockPos controllerPos = worldPosition.offset(controllerOffset);
                final BlockEntity be = level.getBlockEntity(controllerPos);
                if (be instanceof final CogwheelChainBlockEntity controllerBE) {
                    controllerBE.addPropogationLocationsFromController(toPropagate, getBlockPos());
                }
            }
        }

        return toPropagate;
    }

    @Override
    public float propagateRotationTo(final KineticBlockEntity target, final BlockState stateFrom, final BlockState stateTo, final BlockPos diff, final boolean connectedViaAxes, final boolean connectedViaCogs) {
        if (connectedViaAxes && Math.abs(diff.get(getBlockState().getValue(CogwheelChainBlock.AXIS))) == 1)
            return 0;

        //Else, check if this is the same chain structure.
        if (target instanceof final CogwheelChainBlockEntity chainTarget) {
            final boolean isControlledBySame = this.isController &&
                    chainTarget.controllerOffset != null &&
                    chainTarget.controllerOffset.equals(this.getBlockPos().subtract(target.getBlockPos())) ||

                    chainTarget.isController &&
                            this.controllerOffset != null &&
                            this.controllerOffset.equals(target.getBlockPos().subtract(this.getBlockPos())) ||

                    chainTarget.controllerOffset != null &&
                            this.controllerOffset != null &&
                            this.controllerOffset.offset(this.getBlockPos()).equals(chainTarget.controllerOffset.offset(target.getBlockPos()));

            if (isControlledBySame) {
                final float currentSide = this.getChainRotationFactor();
                final float otherSide = chainTarget.getChainRotationFactor();
                return currentSide / otherSide;
            }
        }
        return 0;
    }

    public float getChainRotationFactor() {
        if (isController) {
            if (chain == null) return 0;

            final PathedCogwheelNode controllerNode = chain.getNodeFromControllerOffset(new Vec3i(0, 0, 0));
            if (controllerNode == null) return 0;

            return controllerNode.sideFactor();
        }

        if (level == null || controllerOffset == null) return 0;

        final BlockPos controllerPos = worldPosition.offset(controllerOffset);
        final BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof final CogwheelChainBlockEntity controllerBE) {

            final CogwheelChain controllerChain = controllerBE.chain;
            if (controllerChain == null) return 0;

            final PathedCogwheelNode nodeInChain = controllerChain.getNodeFromControllerOffset(controllerOffset);
            return nodeInChain == null ? 0 : nodeInChain.sideFactor();
        }
        return 0;
    }

    private void addPropogationLocationsFromController(final List<BlockPos> toPropagate, final BlockPos exclude) {
        assert chain != null;
        for (final var cogwheelNode : chain.getChainPathCogwheelNodes()) {
            final BlockPos cogwheelPos = worldPosition.offset(cogwheelNode.localPos());
            if (!toPropagate.contains(cogwheelPos) && !cogwheelPos.equals(exclude)) {
                toPropagate.add(cogwheelPos);
            }
        }
    }

    public boolean isController() {
        return isController;
    }

    public void setController(final boolean controller) {
        isController = controller;
    }

    public @Nullable CogwheelChain getChain() {
        return chain;
    }

    public void setChain(@Nullable final CogwheelChain chain) {
        this.chain = chain;
    }

    public @Nullable Vec3i getControllerOffset() {
        return controllerOffset;
    }

    public void setControllerOffset(@Nullable final Vec3i controllerOffset) {
        this.controllerOffset = controllerOffset;
    }

    public void setChainsUsed(final int chainsUsed) {
        this.chainsToRefund = chainsUsed;
    }

    public Item getChainItem() {
        return chainItem;
    }

    public void setChainItem(final Item chainItem) {
        this.chainItem = chainItem;
    }

    public @Nullable Block getSourceBlock() {
        return sourceState == null ? null : sourceState.getBlock();
    }

    public @Nullable BlockState getSourceState() {
        return sourceState;
    }

    public void setSourceState(@Nullable final BlockState sourceState) {
        this.sourceState = sourceState;
    }

    public void clearStoredChains() {
        if (isController) {
            this.chainsToRefund = 0;
        } else {
            if (controllerOffset != null && level != null) {
                final BlockPos controllerPos = worldPosition.offset(controllerOffset);
                final BlockEntity be = level.getBlockEntity(controllerPos);
                if (be instanceof final CogwheelChainBlockEntity controllerBE) {
                    controllerBE.chainsToRefund = 0;
                }
            }
        }
    }

    @Override
    public ItemRequirement getRequiredItems(final BlockState state) {
        return isController ? new ItemRequirement(
                ItemRequirement.ItemUseType.CONSUME,
                new ItemStack(chainItem, chain != null ? chain.getChainsRequired() : 0)
        ) : ItemRequirement.NONE;
    }
}
