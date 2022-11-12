package org.minefortress.entity.ai.goal.warrior;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import org.minefortress.entity.IWarriorPawn;

import java.util.EnumSet;

public class MoveToBlockGoal extends Goal {

    private final IWarriorPawn pawn;
    private BlockPos target;

    public MoveToBlockGoal(IWarriorPawn pawn) {
        this.pawn = pawn;
        setControls(EnumSet.of(Control.MOVE, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        return hasMoveTarget() && farFromMoveTarget();
    }

    @Override
    public void start() {
        target = pawn.getMoveTarget();
        pawn.getFighterMoveControl().moveTo(target);
    }

    @Override
    public boolean shouldContinue() {
        return hasMoveTarget() && stillOnTheSameTarget() && farFromMoveTarget() && !pawn.getFighterMoveControl().isStuck();
    }

    @Override
    public boolean canStop() {
        return false;
    }

    @Override
    public void stop() {
        pawn.getFighterMoveControl().reset();
        target = null;
    }

    private boolean hasMoveTarget() {
        return pawn.getMoveTarget() != null;
    }

    private boolean stillOnTheSameTarget() {
        return target != null && target.equals(pawn.getMoveTarget());
    }

    private boolean farFromMoveTarget() {
        return !pawn.getMoveTarget().isWithinDistance(pawn.getPos(), pawn.getReachRange());
    }

}