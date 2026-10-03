package com.kipti.bnb.network.packets.from_client;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChain;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sneak + use with a wrench on a chain drive's chain: takes the loop down and returns the chains.
 */
public class RemoveCogwheelChainPacket extends SimplePacketBase {

    private final BlockPos controllerPos;

    public RemoveCogwheelChainPacket(final BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public RemoveCogwheelChainPacket(final FriendlyByteBuf buffer) {
        this.controllerPos = buffer.readBlockPos();
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(controllerPos);
    }

    @Override
    public boolean handle(final NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player != null)
                handle(player);
        });
        return true;
    }

    private void handle(final ServerPlayer player) {
        final Level level = player.level();
        if (!level.isLoaded(controllerPos) || !level.mayInteract(player, controllerPos) || !player.mayBuild())
            return;
        if (!AllTags.AllItemTags.WRENCH.matches(player.getMainHandItem()) && !AllTags.AllItemTags.WRENCH.matches(player.getOffhandItem()))
            return;

        final BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof final CogwheelChainBlockEntity controller) || !controller.isController())
            return;
        final CogwheelChain chain = controller.getChain();
        if (chain == null)
            return;

        // The client picked the chain by raycast; make sure it is actually within reach
        final double reach = player.getBlockReach() + 2;
        if (chain.getDistanceSqTo(controllerPos, player.getEyePosition()) > reach * reach)
            return;

        final ItemStack drops = controller.destroyChain(false);
        if (!player.isCreative())
            player.getInventory().placeItemBackInInventory(drops);

        level.playSound(null, controllerPos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1, 1);
        AllSoundEvents.WRENCH_REMOVE.playOnServer(level, controllerPos, 1, level.random.nextFloat() * .5f + .5f);
    }

}
