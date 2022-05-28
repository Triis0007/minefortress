package org.minefortress.blueprints.data;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.Structure;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.minefortress.MineFortressMod;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ServerBlueprintBlockDataManager extends AbstractBlueprintBlockDataManager{

    private final MinecraftServer server;
    private final Map<String, NbtCompound> updatedStructures = new HashMap<>();

    public ServerBlueprintBlockDataManager(MinecraftServer server) {
        this.server = server;
    }

    public NbtCompound getStructureNbt(String fileName) {
        NbtCompound compound = new NbtCompound();
        getStructure(fileName).writeNbt(compound);
        return compound;
    }

    public void update(String fileName, NbtCompound tag) {
        updatedStructures.put(fileName, tag);
        invalidateBlueprint(fileName);
    }

    @Override
    protected Structure getStructure(String blueprintFileName) {
        if(updatedStructures.containsKey(blueprintFileName)) {
            final NbtCompound structureTag = updatedStructures.get(blueprintFileName);
            final Structure structure = new Structure();
            structure.readNbt(structureTag);
            return structure;
        } else {
            final Identifier id = getId(blueprintFileName);
            return server
                    .getStructureManager()
                    .getStructure(id)
                    .orElseThrow(() -> new IllegalArgumentException("Blueprint file not found: " + blueprintFileName));
        }


    }

    @Override
    protected BlueprintBlockData buildBlueprint(Structure structure, BlockRotation rotation, int floorLevel) {
        final var sizeAndPivot = getSizeAndPivot(structure, rotation);
        final var size = sizeAndPivot.size();
        final var pivot = sizeAndPivot.pivot();

        final Map<BlockPos, BlockState> structureData = getStrcutureData(structure, rotation, pivot);

        final Map<BlockPos, BlockState> structureEntityData = structureData.entrySet()
            .stream()
            .filter(entry -> entry.getValue().getBlock() instanceof BlockEntityProvider || !entry.getValue().getFluidState().isEmpty())
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        final Map<BlockPos, BlockState> allBlocksWithoutEntities = structureData.entrySet()
            .stream()
            .filter(entry -> !(entry.getValue().getBlock() instanceof BlockEntityProvider) && entry.getValue().getFluidState().isEmpty())
            .filter(entry -> {
                final BlockPos pos = entry.getKey();
                final BlockState state = entry.getValue();
                return pos.getY() < floorLevel || !state.isAir();
            })
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        final Map<BlockPos, BlockState> manualData = new HashMap<>();
        final Map<BlockPos, BlockState> automaticData = new HashMap<>();

        for(int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                boolean isManual = true;
                for (int y = 0; y < size.getY(); y++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    boolean contains = allBlocksWithoutEntities.containsKey(pos);
                    if(!contains && y >= floorLevel) {
                        isManual = false;
                        continue;
                    }

                    if(isManual) {
                        if(contains)
                            manualData.put(pos, allBlocksWithoutEntities.get(pos));
                        else if(structureEntityData.containsKey(pos)) {
                            manualData.put(pos, Blocks.AIR.getDefaultState());
                        }
                    } else {
                        automaticData.put(pos, allBlocksWithoutEntities.get(pos));
                    }
                }
            }
        }

        return BlueprintBlockData
                .withBlueprintSize(size)
                .setLayer(BlueprintDataLayer.GENERAL, structureData)
                .setLayer(BlueprintDataLayer.MANUAL, manualData)
                .setLayer(BlueprintDataLayer.AUTOMATIC, automaticData)
                .setLayer(BlueprintDataLayer.ENTITY, structureEntityData)
                .build();
    }

    @NotNull
    private Identifier getId(String fileName) {
        return new Identifier(MineFortressMod.MOD_ID, fileName);
    }

    public void writeBlockDataManager(NbtCompound tag) {
        if(updatedStructures.isEmpty()) return;
        final NbtList nbtElements = new NbtList();
        for(Map.Entry<String, NbtCompound> entry : updatedStructures.entrySet()) {
            final NbtCompound mapEntry = new NbtCompound();
            mapEntry.putString("fileName", entry.getKey());
            mapEntry.put("structure", entry.getValue());
            nbtElements.add(mapEntry);
        }

        tag.put("updatedStructures", nbtElements);
    }

    public void readBlockDataManager(NbtCompound tag) {
        if(!tag.contains("updatedStructures")) return;
        updatedStructures.clear();
        final NbtList nbtElements = tag.getList("updatedStructures", NbtType.COMPOUND);
        for (int i = 0; i < nbtElements.size(); i++) {
            final NbtCompound mapEntry = nbtElements.getCompound(i);
            final String fileName = mapEntry.getString("fileName");
            final NbtCompound structure = mapEntry.getCompound("structure");
            updatedStructures.put(fileName, structure);
        }
    }

}


//    final BlueprintMetadata blueprintMetadata = BlueprintMetadataManager.getByFile(fileName);
//
//    final Map<BlockPos, BlockState> structureEntityData = structureData.entrySet()
//            .stream()
//            .filter(entry -> entry.getValue().getBlock() instanceof BlockEntityProvider)
//            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
//
//    final Map<BlockPos, BlockState> structureManualData = structureData.entrySet()
//            .stream()
//            .filter(entry -> !(entry.getValue().getBlock() instanceof BlockEntityProvider))
//            .filter(ent -> blueprintMetadata == null || !blueprintMetadata.isPartOfAutomaticLayer(ent.getKey(), ent.getValue()))
//            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
//
//    final Map<BlockPos, BlockState> structureAutomaticData = structureData.entrySet()
//            .stream()
//            .filter(entry -> !(entry.getValue().getBlock() instanceof BlockEntityProvider))
//            .filter(ent -> blueprintMetadata != null && blueprintMetadata.isPartOfAutomaticLayer(ent.getKey(), ent.getValue()))
//            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
//
//    BlueprintBlockData blockData = new BlueprintBlockData(structureData, size, standsOnGrass, structureEntityData, structureManualData, structureAutomaticData);
//                blueprints.put(key, blockData);