package org.minefortress.interfaces;

import org.minefortress.blueprints.manager.ServerBlueprintManager;
import org.minefortress.fortress.FortressServerManager;
import org.minefortress.tasks.TaskManager;

import java.util.UUID;

public interface FortressServerPlayerEntity {

    UUID getFortressUuid();
    FortressServerManager getFortressServerManager();
    ServerBlueprintManager getServerBlueprintManager();
    TaskManager getTaskManager();

}
