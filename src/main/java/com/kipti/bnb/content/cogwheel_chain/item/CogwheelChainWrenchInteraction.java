package com.kipti.bnb.content.cogwheel_chain.item;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.network.BnbPackets;
import com.kipti.bnb.network.packets.from_client.RemoveCogwheelChainPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Wrench interaction with a chain drive's chain itself, like Create's chain conveyor: holding a wrench gives the
 * chain segment you are looking at a block-style selection outline, and sneak + use takes the whole loop down,
 * returning the chains.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CogwheelChainWrenchInteraction {

    /** Half-width of the selection box around a segment, as Create's chain conveyor uses. */
    private static final double SELECTION_RADIUS = 0.175;

    @Nullable
    private static BlockPos selectedController = null;
    @Nullable
    private static Vec3 selectedStart = null;
    @Nullable
    private static Vec3 selectedEnd = null;

    public static void tick(final LocalPlayer player) {
        clearSelection();
        if (!isHoldingWrench(player))
            return;

        final Minecraft mc = Minecraft.getInstance();
        final double range = player.getBlockReach() + 1;
        final Vec3 from = player.getEyePosition();
        final Vec3 to = from.add(player.getViewVector(1).scale(range));

        // A block in front of the chain takes the click instead
        double bestDistance = range;
        final HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS)
            bestDistance = hitResult.getLocation().distanceTo(from);

        for (final CogwheelChainBlockEntity be : CogwheelChainBlockEntity.getClientLoaded()) {
            final CogwheelChain chain = be.getChain();
            if (!be.isController() || chain == null || be.isRemoved() || be.getLevel() != mc.level)
                continue;
            final CogwheelChain.RayHit hit = chain.getRayHit(be.getBlockPos(), from, to, SELECTION_RADIUS);
            if (hit != null && hit.distance() < bestDistance) {
                bestDistance = hit.distance();
                selectedController = be.getBlockPos();
                selectedStart = chain.getSegmentStart(be.getBlockPos(), hit.segment());
                selectedEnd = chain.getSegmentEnd(be.getBlockPos(), hit.segment());
            }
        }
    }

    private static void clearSelection() {
        selectedController = null;
        selectedStart = null;
        selectedEnd = null;
    }

    @SubscribeEvent
    public static void renderSelection(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || selectedStart == null || selectedEnd == null)
            return;

        // An oriented box along the segment, drawn the way vanilla draws a block selection
        final Vec3 diff = selectedEnd.subtract(selectedStart);
        final double yaw = Mth.RAD_TO_DEG * Mth.atan2(diff.x, diff.z);
        final double pitch = Mth.RAD_TO_DEG * Mth.atan2(-diff.y, diff.multiply(1, 0, 1).length());
        final AABB bounds = new AABB(Vec3.ZERO, Vec3.ZERO)
                .expandTowards(0, 0, diff.length())
                .inflate(SELECTION_RADIUS, SELECTION_RADIUS, 0);

        final PoseStack ms = event.getPoseStack();
        final Vec3 camera = event.getCamera().getPosition();
        ms.pushPose();
        TransformStack.of(ms)
                .translate(selectedStart.subtract(camera))
                .rotateYDegrees((float) yaw)
                .rotateXDegrees((float) pitch);
        final MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        TrackBlockOutline.renderShape(Shapes.create(bounds), ms, buffer.getBuffer(RenderType.lines()), null);
        buffer.endBatch(RenderType.lines());
        ms.popPose();
    }

    @SubscribeEvent
    public static void hideVanillaBlockSelection(final RenderHighlightEvent.Block event) {
        if (selectedController != null)
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onClickInput(final InputEvent.InteractionKeyMappingTriggered event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || selectedController == null)
            return;
        final KeyMapping key = event.getKeyMapping();
        if (key != mc.options.keyUse || !mc.player.isShiftKeyDown() || !isHoldingWrench(mc.player))
            return;

        BnbPackets.getChannel().sendToServer(new RemoveCogwheelChainPacket(selectedController));
        clearSelection();
        event.setSwingHand(true);
        // Otherwise the wrench would also act on whatever block is behind the chain
        event.setCanceled(true);
    }

    public static boolean isHoldingWrench(final Player player) {
        return AllTags.AllItemTags.WRENCH.matches(player.getItemInHand(InteractionHand.MAIN_HAND))
                || AllTags.AllItemTags.WRENCH.matches(player.getItemInHand(InteractionHand.OFF_HAND));
    }

}
