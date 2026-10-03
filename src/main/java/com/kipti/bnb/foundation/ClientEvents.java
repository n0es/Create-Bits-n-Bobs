package com.kipti.bnb.foundation;

import com.kipti.bnb.content.cogwheel_chain.item.CogwheelChainPlacementEffect;
import com.kipti.bnb.content.cogwheel_chain.item.CogwheelChainWrenchInteraction;
import com.kipti.bnb.content.girder_strut.GirderStrutPlacementEffects;
import com.kipti.bnb.content.weathered_girder.WeatheredGirderWrenchBehaviour;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

    static final List<Pair<Vec3, Vec3>> deferredDebugRenderOutlines = Collections.synchronizedList(new ArrayList<>());

    @SubscribeEvent
    public static void onTickPost(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        WeatheredGirderWrenchBehaviour.tick();

        //Render deferred debug outlines
        synchronized (deferredDebugRenderOutlines) {
            for (final Pair<Vec3, Vec3> outline : deferredDebugRenderOutlines) {
                Outliner.getInstance().showLine(outline, outline.getFirst(), outline.getSecond());
            }
        }
    }

    public static void pushNewDeferredDebugRenderOutline(final Pair<Vec3, Vec3> outline) {
        //Synchronized list to avoid concurrent modification exceptions
        synchronized (deferredDebugRenderOutlines) {
            deferredDebugRenderOutlines.add(outline);
        }
    }

    public static void clearDeferredDebugRenderOutlines() {
        synchronized (deferredDebugRenderOutlines) {
            deferredDebugRenderOutlines.clear();
        }
    }

    @SubscribeEvent
    public static void onTickPre(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START)
            return;
        //If in a level, there is a player, and the player is holding a girder strut block item, update the preview
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            GirderStrutPlacementEffects.tick(mc.player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTickPostLow(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            CogwheelChainPlacementEffect.tick(mc.player);
            CogwheelChainWrenchInteraction.tick(mc.player);
        }
    }

    @SubscribeEvent
    public static void modify(final ItemTooltipEvent context) {
//        if (context.getItemStack().is(AllBlocks.COGWHEEL.asItem()) ||
//                context.getItemStack().is(AllBlocks.LARGE_COGWHEEL.asItem())) {
//            context.getToolTip().add(1, Component.translatable("tooltip.bits_n_bobs.new_ponder_notification")
//                    .withStyle(FontHelper.Palette.STANDARD_CREATE.primary().getColor().getValue()));
//        }
    }

}
