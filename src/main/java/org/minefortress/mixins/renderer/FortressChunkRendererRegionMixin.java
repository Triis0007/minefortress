package org.minefortress.mixins.renderer;

import com.chocohead.mm.api.ClassTinkerers;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.minefortress.fortress.FortressClientManager;
import org.minefortress.interfaces.FortressMinecraftClient;
import org.minefortress.selections.ClickType;
import org.minefortress.selections.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ChunkRendererRegion.class)
public abstract class FortressChunkRendererRegionMixin {

    @Shadow
    protected BlockState[] blockStates;

    @Shadow protected abstract int getIndex(BlockPos pos);

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(World world, int chunkX, int chunkZ, WorldChunk[][] chunks, BlockPos startPos, BlockPos endPos, CallbackInfo ci) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if(client.interactionManager == null || client.interactionManager.getCurrentGameMode() != ClassTinkerers.getEnum(GameMode.class, "FORTRESS"))
            return;

//        final FortressMinecraftClient fortressClient = getFortressClient();
//        final SelectionManager selectionManager = fortressClient.getSelectionManager();

//        if(selectionManager.getClickType() == ClickType.BUILD) {
//            final Set<BlockPos> selectedBlocks = selectionManager.getSelectedBlocks();
//            final BlockState state = selectionManager.getClickingBlock();
//
//            for (BlockPos blockPos : BlockPos.iterate(startPos, endPos)) {
//                if (selectedBlocks.contains(blockPos)) {
//                    final int index = this.getIndex(blockPos);
//                    this.blockStates[index] = state;
//                }
//            }
//        }
    }

    private FortressMinecraftClient getFortressClient() {
        return (FortressMinecraftClient) MinecraftClient.getInstance();
    }

}
