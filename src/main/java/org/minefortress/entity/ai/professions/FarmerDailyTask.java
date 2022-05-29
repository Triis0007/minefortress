package org.minefortress.entity.ai.professions;

import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.minefortress.entity.Colonist;
import org.minefortress.fortress.FortressBulding;
import org.minefortress.fortress.FortressServerManager;
import org.minefortress.tasks.block.info.BlockStateTaskBlockInfo;
import org.minefortress.tasks.block.info.DigTaskBlockInfo;
import org.spongepowered.include.com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class FarmerDailyTask implements ProfessionDailyTask{

    private static final Set<Item> FARMER_SEEDS = Sets.newHashSet(
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.CARROT,
            Items.POTATO,
            Items.COCOA_BEANS
    );

    private FortressBulding currentFarm;
    private Iterator<BlockPos> farmIterator;
    private BlockPos goal;
    private long stopTime = 0L;
    private long workingTicks = 0L;

    @Override
    public boolean canStart(Colonist colonist) {
        return colonist.world.isDay() && isEnoughTimeSinceLastTimePassed(colonist);
    }

    @Override
    public void start(Colonist colonist) {
        getFarm(colonist).ifPresent(f -> this.currentFarm = f);
        initIterator();
    }

    @Override
    public void tick(Colonist colonist) {
        if(this.currentFarm == null) return;
        if(!this.farmIterator.hasNext()) return;
        if(this.goal == null) {
            findCorrectGoal(colonist);
            if(this.goal == null) return;
            colonist.getMovementHelper().set(goal);
        }


        if(colonist.getMovementHelper().hasReachedWorkGoal()) {
            this.workingTicks++;
            final var goalBLockState = colonist.world.getBlockState(this.goal);
            if (goalBLockState.isOf(Blocks.DIRT) || goalBLockState.isOf(Blocks.GRASS_BLOCK)) {
                colonist.putItemInHand(Items.WOODEN_HOE);
                colonist.swingHand(Hand.MAIN_HAND);
                if(workingTicks % 20 == 0) {
                    colonist.world.setBlockState(goal, Blocks.FARMLAND.getDefaultState(), 3);
                    colonist.world.emitGameEvent(colonist, GameEvent.BLOCK_PLACE, goal);
                }
            } else if(goalBLockState.isOf(Blocks.FARMLAND)) {
                if(colonist.getPlaceControl().isDone() && colonist.getDigControl().isDone()) {
                    final var aboveBlock = goal.up();
                    final var aboveGoal = colonist.world.getBlockState(aboveBlock);

                    if(aboveGoal.isIn(BlockTags.CROPS) && aboveGoal.getBlock() instanceof CropBlock cropBlock) {
                        if(aboveGoal.get(cropBlock.getAgeProperty()) == cropBlock.getMaxAge()) {
                            final var digTaskBlockInfo = new DigTaskBlockInfo(aboveBlock);
                            colonist.setGoal(digTaskBlockInfo);
                        } else {
                            this.goal = null;
                        }
                    } else if (aboveGoal.isAir()) {
                        if(isCreative(colonist)) {
                            final var wheatSeeds = (BlockItem) Items.WHEAT_SEEDS;
                            final var blockStateTaskBlockInfo = new BlockStateTaskBlockInfo(wheatSeeds, aboveBlock, wheatSeeds.getBlock().getDefaultState());
                            colonist.setGoal(blockStateTaskBlockInfo);
                        } else {
                            final var seedsOpt = getSeeds(colonist);
                            if(seedsOpt.isPresent()) {
                                final var blockItem = (BlockItem) seedsOpt.get();
                                final var bsTaskBlockInfo = new BlockStateTaskBlockInfo(blockItem, aboveBlock, blockItem.getBlock().getDefaultState());
                                colonist.setGoal(bsTaskBlockInfo);
                            } else {
                                this.goal = null;
                            }
                        }
                    } else {
                        this.goal = null;
                    }
                }
            } else {
                this.goal = null;
            }
        }

        colonist.getMovementHelper().tick();
    }

    @Override
    public void stop(Colonist colonist) {
        this.currentFarm = null;
        this.farmIterator = Collections.emptyIterator();
        this.stopTime = colonist.world.getTime();
        this.workingTicks = 0;
    }

    @Override
    public boolean shouldContinue(Colonist colonist) {
        return colonist.world.isDay() && farmIterator.hasNext();
    }

    private Optional<FortressBulding> getFarm(Colonist colonist) {
        return colonist
            .getFortressServerManager()
            .flatMap(it -> it.getRandomBuilding("farmer", colonist.world.random));
    }

    private boolean isEnoughTimeSinceLastTimePassed(Colonist colonist) {
        return colonist.world.getTime() - this.stopTime > 400;
    }

    private void initIterator() {
        if(this.currentFarm == null) {
            this.farmIterator = Collections.emptyIterator();
        } else {
            this.farmIterator = BlockPos.iterate(this.currentFarm.getStart(), this.currentFarm.getEnd()).iterator();
        }
    }

    private Optional<Item> getSeeds(Colonist colonist) {
        final var itemOpt = colonist.getFortressServerManager()
                .map(FortressServerManager::getServerResourceManager)
                .flatMap(rm -> rm.getAllItems()
                        .stream()
                        .filter(it -> !it.isEmpty())
                        .map(ItemStack::getItem)
                        .filter(FARMER_SEEDS::contains)
                        .findFirst()
                );

        itemOpt.ifPresent(
                i -> colonist.getFortressServerManager()
                        .ifPresent(m -> m.getServerResourceManager().removeItemIfExists(i))
        );

        return itemOpt;
    }

    private void findCorrectGoal(Colonist colonist) {
        this.goal = null;
        while (farmIterator.hasNext()) {
            final var possibleGoal = this.farmIterator.next();
            if(isCorrectGoal(colonist.world, possibleGoal)) {
                this.goal = possibleGoal;
                return;
            }
        }
    }

    private boolean isCorrectGoal(World world, BlockPos goal) {
        final var blockState = world.getBlockState(goal);
        final var goalCorrect = blockState.isOf(Blocks.FARMLAND) || blockState.isOf(Blocks.DIRT) || blockState.isOf(Blocks.GRASS_BLOCK);
        final var aboveGoalState = world.getBlockState(goal.up());
        final var aboveGoalCorrect = BlockTags.CROPS.contains(aboveGoalState.getBlock()) || aboveGoalState.isAir();
        return goalCorrect && aboveGoalCorrect;
    }

    private boolean isCreative(Colonist colonist) {
        return colonist.getFortressServerManager().map(FortressServerManager::isCreative).orElse(false);
    }
}
