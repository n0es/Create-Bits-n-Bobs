package com.kipti.bnb.content.cogwheel_chain.item;

import com.kipti.bnb.registry.BnbTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Which items count as chain for a cogwheel chain drive. Driven by the
 * {@code bits_n_bobs:cogwheel_chains} item tag rather than vanilla chain alone, so packs
 * that replace vanilla chain (e.g. with metal chains) can still build chain drives.
 */
public final class CogwheelChainItems {

    private CogwheelChainItems() {
    }

    public static boolean isChain(final ItemStack stack) {
        return !stack.isEmpty() && BnbTags.BnbItemTags.COGWHEEL_CHAINS.matches(stack);
    }

    /**
     * The chain stack the player is holding, preferring the given hand, then the main hand, then the offhand.
     */
    public static @Nullable ItemStack findHeldChain(final Player player, @Nullable final InteractionHand preferredHand) {
        if (preferredHand != null && isChain(player.getItemInHand(preferredHand)))
            return player.getItemInHand(preferredHand);
        if (isChain(player.getMainHandItem()))
            return player.getMainHandItem();
        if (isChain(player.getOffhandItem()))
            return player.getOffhandItem();
        return null;
    }

}
