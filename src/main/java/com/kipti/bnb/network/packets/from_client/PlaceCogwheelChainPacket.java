package com.kipti.bnb.network.packets.from_client;

import com.kipti.bnb.content.cogwheel_chain.graph.*;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.kipti.bnb.content.cogwheel_chain.item.CogwheelChainItems;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlaceCogwheelChainPacket extends SimplePacketBase {

    PlacingCogwheelChain worldSpacePartialChain;
    int priorityChainTakeHand;

    public PlaceCogwheelChainPacket(PlacingCogwheelChain worldSpacePartialChain, int priorityChainTakeHand) {
        this.worldSpacePartialChain = worldSpacePartialChain;
        this.priorityChainTakeHand = priorityChainTakeHand;
    }

    public PlaceCogwheelChainPacket(FriendlyByteBuf buffer) {
        this.worldSpacePartialChain = PlacingCogwheelChain.readFromBuffer(buffer);
        this.priorityChainTakeHand = buffer.readInt();
    }

    @Override
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            MinecraftServer server = player.getServer();
            handle(server, player);
        });
        return true;
    }

    public void handle(@Nullable MinecraftServer server, @Nullable ServerPlayer player) {
        //Server side validation of the chain
        if (worldSpacePartialChain.maxBounds() > PlacingCogwheelChain.MAX_CHAIN_BOUNDS)
            return;

        if (!worldSpacePartialChain.checkMatchingNodesInLevel(player.level()))
            return;

        // Attached blocks (pumps) are never replaced, so a real cogwheel has to hold the chain as its controller
        if (!worldSpacePartialChain.hasHoldingNode(player.level()))
            return;
        // An attached block isn't replaced, so nothing else stops it joining a second chain
        for (final PlacingCogwheelNode node : worldSpacePartialChain.getVisitedNodes()) {
            if (PlacingCogwheelChain.isAttachedBlockTarget(player.level().getBlockState(node.pos()))
                    && CogwheelChainAttachments.getController(player.level(), node.pos()) != null)
                return;
        }
        worldSpacePartialChain.startAtHoldingNode(player.level());

        final int chainsRequired = worldSpacePartialChain.getChainsRequiredInLoop();

        final InteractionHand[] hands = InteractionHand.values();
        final InteractionHand preferredHand = priorityChainTakeHand >= 0 && priorityChainTakeHand < hands.length ? hands[priorityChainTakeHand] : null;
        final ItemStack heldChain = CogwheelChainItems.findHeldChain(player, preferredHand);
        if (heldChain == null)
            return;
        // Captured before consumption, which may replace the held stack
        final Item chainItem = heldChain.getItem();
        final ItemStack chainType = new ItemStack(chainItem);

        final boolean hasEnough = player.isCreative() || ChainConveyorBlockEntity.getChainsFromInventory(player, chainType, chainsRequired, true);
        if (!hasEnough)
            return;

        final List<PathedCogwheelNode> chainGeometry;
        try {
            chainGeometry = CogwheelChainPathfinder.buildChainPath(worldSpacePartialChain);
        } catch (final
        ChainInteractionFailedException ignored) { //We assume the client has been notified if the path was invalid, anything else is tampering
            return;
        }
        if (chainGeometry == null)
            return;

        if (!player.isCreative())
            ChainConveyorBlockEntity.getChainsFromInventory(player, chainType, chainsRequired, false);

        final CogwheelChain chain = new CogwheelChain(chainGeometry);

        chain.placeInLevel(player.level(), worldSpacePartialChain, chainItem);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        PlacingCogwheelChain.writeToBuffer(worldSpacePartialChain, buffer);
        buffer.writeInt(priorityChainTakeHand);
    }

}
