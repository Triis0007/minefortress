package org.minefortress.fortress.buildings;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;

import java.util.*;

class FortressBuildingBlockData {

    private int blockPointer = 0;
    private final List<PositionedState> referenceState = new ArrayList<>();
    private final Map<BlockPos, BuildingBlockState> actualState = new HashMap<>();
    private List<BlockPos> preservedPositions;

    FortressBuildingBlockData(Map<BlockPos, BlockState> preservedState) {
        for (Map.Entry<BlockPos, BlockState> entry : preservedState.entrySet()) {
            final var pos = entry.getKey();
            final var state = entry.getValue();
            final var block = state.getBlock();
            if(block == Blocks.AIR)
                continue;
            final var positionedState = new PositionedState(pos, block);
            this.referenceState.add(positionedState);
            this.actualState.put(pos, BuildingBlockState.PRESERVED);
        }
    }

    private FortressBuildingBlockData(NbtCompound tag) {
        if(tag.contains("pointer", NbtType.NUMBER))
            blockPointer = tag.getInt("pointer");

        if(tag.contains("referenceState", NbtType.LIST)) {
            final var list = tag.getList("referenceState", NbtType.COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                final var compound = list.getCompound(i);
                final var pos = BlockPos.fromLong(compound.getLong("pos"));
                final var block = Registry.BLOCK.get(compound.getInt("block"));
                final var positionedState = new PositionedState(pos, block);
                referenceState.add(positionedState);
            }
        }

        if(tag.contains("actualState", NbtType.LIST)) {
            final var list = tag.getList("actualState", NbtType.COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                final var compound = list.getCompound(i);
                final var pos = BlockPos.fromLong(compound.getLong("pos"));
                final var block = BuildingBlockState.valueOf(compound.getString("block"));
                actualState.put(pos, block);
            }
        }

        recalculatePreservedPositions();
    }

    boolean checkTheNextBlocksState(int blocksAmount, ServerWorld world) {
        if(world.getRegistryKey() != World.OVERWORLD)
            throw new IllegalArgumentException("The world must be the overworld");

        boolean stateUpdated = false;
        for (int i = 0; i < blocksAmount; i++) {
            blockPointer = blockPointer % referenceState.size();
            final var state = referenceState.get(blockPointer);
            final var pos = state.pos;
            final var block = state.block;

            final var actualBlock = world.getBlockState(pos).getBlock();

            final var previousState = actualState.getOrDefault(pos, BuildingBlockState.PRESERVED);
            final var newState = Objects.equals(block, actualBlock)? BuildingBlockState.PRESERVED : BuildingBlockState.DESTROYED;

            actualState.put(pos, newState);

            blockPointer++;
            stateUpdated = stateUpdated || previousState != newState;
        }

        if(stateUpdated)
            recalculatePreservedPositions();


        return stateUpdated;
    }

    private void recalculatePreservedPositions() {
        preservedPositions = actualState.entrySet()
                .stream()
                .filter(it -> it.getValue() == BuildingBlockState.PRESERVED)
                .map(Map.Entry::getKey)
                .toList();
    }

    int getHealth() {
        final var preserved = actualState.values().stream().filter(state -> state == BuildingBlockState.PRESERVED).count();
        final var delta = (float) preserved / (float) actualState.size();
        return (int)MathHelper.clampedLerpFromProgress(delta, 0.5f, 1, 0, 100);
    }

    NbtCompound toNbt() {
        final var tag = new NbtCompound();
        final var preservedStateList = new NbtList();
        for (PositionedState state : referenceState) {
            final var compound = new NbtCompound();
            compound.putLong("pos", state.pos.asLong());
            compound.putInt("block", Registry.BLOCK.getRawId(state.block));
            preservedStateList.add(compound);
        }
        tag.put("referenceState", preservedStateList);

        final var actualStateList = new NbtList();
        for (Map.Entry<BlockPos, BuildingBlockState> entry : actualState.entrySet()) {
            final var compound = new NbtCompound();
            compound.putLong("pos", entry.getKey().asLong());
            compound.putString("block", entry.getValue().name());
            actualStateList.add(compound);
        }

        tag.put("actualState", actualStateList);
        tag.putInt("pointer", blockPointer);

        return tag;
    }

    void attack(HostileEntity attacker) {
        final var world = attacker.getWorld();
        final var random = world.random;
        for (Map.Entry<BlockPos, BuildingBlockState> entries : actualState.entrySet()) {
            final var pos = entries.getKey();
            final var state = entries.getValue();
            if(state == BuildingBlockState.DESTROYED)
                continue;


            if(random.nextFloat() >= 0.6f) {
                world.syncWorldEvent(
                        WorldEvents.BLOCK_BROKEN,
                        pos,
                        Block.getRawIdFromState(world.getBlockState(pos))
                );
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos);

            }
            break;
        }

        if(preservedPositions!=null) {
            final var pos = preservedPositions.get(random.nextInt(preservedPositions.size()));
            world.setBlockBreakingInfo(attacker.getId(), pos, random.nextInt(10));
        }
    }

    Map<BlockPos, BlockState> getAllBlockStatesToRepairTheBuilding() {
        final var map = new HashMap<BlockPos, BlockState>();
        for (Map.Entry<BlockPos, BuildingBlockState> entry : actualState.entrySet()) {
            final var pos = entry.getKey();
            final var state = entry.getValue();
            if(state == BuildingBlockState.PRESERVED)
                continue;

            final var block = referenceState.stream().filter(it -> it.pos.equals(pos)).findFirst().orElseThrow().block;
            map.put(pos, block.getDefaultState());
        }
        return map;
    }

    static FortressBuildingBlockData fromNbt(NbtCompound compound) {
        return new FortressBuildingBlockData(compound);
    }

    private record PositionedState(BlockPos pos, Block block) {}

    private enum BuildingBlockState {
        DESTROYED,
        PRESERVED,
    }

}