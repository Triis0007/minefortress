package org.minefortress.interfaces;

import net.minecraft.server.WorldGenerationProgressListener;
import org.minefortress.blueprints.world.BlueprintsWorld;

public interface FortressServer {

    BlueprintsWorld getBlueprintsWorld();
    WorldGenerationProgressListener getWorldGenerationProgressListener();
    void setTicksMultiplier(int multiplier);

}
