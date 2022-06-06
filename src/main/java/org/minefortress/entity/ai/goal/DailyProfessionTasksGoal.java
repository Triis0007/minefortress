package org.minefortress.entity.ai.goal;

import org.minefortress.entity.Colonist;
import org.minefortress.entity.ai.controls.TaskControl;
import org.minefortress.entity.ai.professions.*;

import java.util.Map;

import static java.util.Map.entry;

public class DailyProfessionTasksGoal extends AbstractFortressGoal {

    private final Map<String, ProfessionDailyTask> dailyTasks = Map.ofEntries(
            entry("crafter", new CrafterDailyTask()),
            entry("blacksmith", new BlacksmithDailyTask()),
            entry("forester", new ForesterDailyTask()),
            entry("farmer", new FarmerDailyTask())
    );

    private ProfessionDailyTask currentTask;

    private final WarriorDailyTask warriorTask;

    public DailyProfessionTasksGoal(Colonist colonist) {
        super(colonist);
        this.warriorTask = new WarriorDailyTask();
    }

    @Override
    public boolean canStart() {
        if(!notInCombat()) return false;
        if(this.isStarving()) return false;
        final TaskControl taskControl = getTaskControl();
        if(taskControl.hasTask()) return false;
        final String professionId = colonist.getProfessionId();
        if(colonist.getFortressManager().isEmpty()) return false;
        final var warriorProfession = professionId.startsWith("warrior") || professionId.startsWith("archer");
        if(!dailyTasks.containsKey(professionId) && !warriorProfession) return false;

        this.currentTask = warriorProfession ? warriorTask : dailyTasks.get(professionId);
        return this.currentTask.canStart(colonist);
    }

    @Override
    public void start() {
        colonist.getTaskControl().setDoingEverydayTasks(true);
        this.currentTask.start(colonist);
    }

    @Override
    public void tick() {
        this.currentTask.tick(colonist);
    }

    @Override
    public boolean shouldContinue() {
        return notInCombat() && !isStarving() && this.dailyTasks.containsKey(colonist.getProfessionId())
                && this.currentTask.shouldContinue(colonist)
                && !getTaskControl().hasTask();
    }

    @Override
    public void stop() {
        this.currentTask.stop(colonist);
        colonist.getTaskControl().setDoingEverydayTasks(false);
    }

    @Override
    public boolean canStop() {
        return this.isStarving() || this.isScared() || super.isFighting() || super.isHiding();
    }

    private TaskControl getTaskControl() {
        return colonist.getTaskControl();
    }
}
