package com.kipti.bnb.content.cogwheel_chain.block;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

public final class CogwheelChainClientHooks {

    private CogwheelChainClientHooks() {
    }

    /**
     * The visual's model is picked from the source cogwheel when it is created, so rebuild it if the source arrives
     * (or changes) after the block entity was first visualised.
     */
    public static void sourceChanged(final CogwheelChainBlockEntity be) {
        if (be.getLevel() == null)
            return;
        final VisualizationManager manager = VisualizationManager.get(be.getLevel());
        if (manager == null)
            return;
        manager.blockEntities().queueRemove(be);
        manager.blockEntities().queueAdd(be);
    }

}
