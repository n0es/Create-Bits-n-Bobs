package com.kipti.bnb.content.cogwheel_chain.item;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.graph.ChainInteractionFailedException;
import com.kipti.bnb.content.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.cogwheel_chain.graph.PlacingCogwheelNode;
import com.kipti.bnb.network.BnbPackets;
import com.kipti.bnb.network.packets.from_client.PlaceCogwheelChainPacket;
import com.kipti.bnb.registry.BnbFeatureFlag;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.trains.entity.TrainPromptPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CogwheelChainPlacementInteraction {

    protected static @Nullable PlacingCogwheelChain currentBuildingChain = null;
    protected static @Nullable ResourceKey<Level> currentChainLevel = null;

    @SubscribeEvent
    public static void onClickInput(final InputEvent.InteractionKeyMappingTriggered event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null)
            return;

        final KeyMapping key = event.getKeyMapping();

        if (key == mc.options.keyUse && onRightClick(event)) {
            event.setCanceled(true);
        }
    }

    /**
     * Attached blocks (pumps) aren't replaced: a loop needs a real cogwheel to hold it, and nothing else stops a pump
     * joining two chains. The server checks the same.
     */
    private static @Nullable String getAttachmentProblem(final PlacingCogwheelChain chain, final ClientLevel level) {
        if (!chain.hasHoldingNode(level))
            return "needs_cogwheel";
        for (final PlacingCogwheelNode node : chain.getNodes()) {
            if (!PlacingCogwheelChain.isAttachedBlockTarget(level.getBlockState(node.pos())))
                continue;
            for (final CogwheelChainBlockEntity be : CogwheelChainBlockEntity.getClientLoaded()) {
                if (be.isController() && be.getChain() != null && be.getChain().getMemberPositions(be.getBlockPos()).contains(node.pos()))
                    return "already_chained";
            }
        }
        return null;
    }

    private static boolean onRightClick(final InputEvent.InteractionKeyMappingTriggered event) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final ClientLevel level = Minecraft.getInstance().level;
        if (player == null || level == null)
            return false;

        //If it is a chain targeting a cogwheel
        final ItemStack itemInHand = CogwheelChainItems.findHeldChain(player, null);

        if (itemInHand == null) {
            return false;
        }

        //If crouching, try clear
        if (player.isShiftKeyDown()) {
            if (currentBuildingChain != null) {
                currentBuildingChain = null;
                currentChainLevel = null;
            }
            return true;
        }

        final HitResult hitResult = Minecraft.getInstance().hitResult;

        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return currentBuildingChain != null;
        }

        final BlockHitResult bhr = (BlockHitResult) hitResult;
        final BlockPos hitPos = bhr.getBlockPos();
        final BlockState targetedState = level.getBlockState(hitPos);

        final boolean validBlockTarget = PlacingCogwheelChain.isValidBlockTarget(targetedState);

        if (validBlockTarget && !BnbFeatureFlag.COGWHEEL_CHAIN_DRIVES.get()) {
            player.displayClientMessage(new ChainInteractionFailedException("config_forbids").getComponent(), true);
            return true;
        }

        if (!validBlockTarget) {
            return currentBuildingChain != null;
        }
        //Cancel normal interaction
        event.setSwingHand(true);

        if (currentBuildingChain == null || currentChainLevel == null || !currentChainLevel.equals(level.dimension())) {
            //Start a new chain
            currentBuildingChain = new PlacingCogwheelChain(hitPos, PlacingCogwheelChain.getAxis(targetedState), PlacingCogwheelChain.isLargeBlockTarget(targetedState), PlacingCogwheelChain.hasSmallCogwheelOffset(targetedState));
            currentChainLevel = level.dimension();

            player.displayClientMessage(Component.translatable("tooltip.bits_n_bobs.chain_drive_placing_hint"), true);
        } else {
            //if this is the last node, then remove the last one
            if (currentBuildingChain.getLastNode().pos().equals(hitPos)) {
                currentBuildingChain.removeLastNode();
                //If no nodes left, clear chain
                if (currentBuildingChain.getNodes().isEmpty()) {
                    currentBuildingChain = null;
                    currentChainLevel = null;
                }
                return true;
            }

            //Try to add to existing chain
            try {
                final boolean added = currentBuildingChain.tryAddNode(hitPos, targetedState);

                if (!added) { //Only happens with invalid target, ignore quietly
                    return true;
                }

                final boolean completed;
                try {
                    completed = currentBuildingChain.canBuildChainIfLooping();
                } catch (final ChainInteractionFailedException exception) {
                    player.displayClientMessage(exception.getComponent(), true);
                    currentBuildingChain = null;
                    currentChainLevel = null;
                    return true;
                }

                final String attachmentProblem = completed ? getAttachmentProblem(currentBuildingChain, level) : null;
                if (attachmentProblem != null) {
                    player.displayClientMessage(new ChainInteractionFailedException(attachmentProblem).getComponent(), true);
                    currentBuildingChain = null;
                    currentChainLevel = null;
                    return true;
                }

                if (completed) {
                    //If completed, send to server, clear current chain
                    BnbPackets.getChannel().sendToServer(new PlaceCogwheelChainPacket(currentBuildingChain, event.getHand().ordinal()));

                    currentBuildingChain = null;
                    currentChainLevel = null;
                }
            } catch (final ChainInteractionFailedException exception) {
                //Send message on fail
                player.displayClientMessage(exception.getComponent(), true);
            }
        }
        return true;
    }

}
