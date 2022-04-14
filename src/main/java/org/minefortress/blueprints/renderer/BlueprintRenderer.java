package org.minefortress.blueprints.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.minefortress.blueprints.data.ClientBlueprintBlockDataManager;
import org.minefortress.blueprints.manager.ClientBlueprintManager;
import org.minefortress.blueprints.manager.BlueprintMetadata;
import org.minefortress.interfaces.FortressMinecraftClient;
import org.minefortress.renderer.custom.AbstractCustomRenderer;
import org.minefortress.renderer.custom.BuiltModel;

import java.util.Optional;

public final class BlueprintRenderer extends AbstractCustomRenderer {

    private static final Vec3f WRONG_PLACEMENT_COLOR = new Vec3f(1.0F, 0.5F, 0.5F);
    private static final Vec3f CORRECT_PLACEMENT_COLOR = new Vec3f(1F, 1.0F, 1F);

    private final BlueprintsModelBuilder blueprintsModelBuilder;

    public BlueprintRenderer(ClientBlueprintBlockDataManager blockDataManager, MinecraftClient client, BlockBufferBuilderStorage blockBufferBuilderStorage) {
        super(client);
        blueprintsModelBuilder  = new BlueprintsModelBuilder(
                blockBufferBuilderStorage,
                blockDataManager
        );
    }

    @Override
    public void prepareForRender() {
        final ClientBlueprintManager clientBlueprintManager = getBlueprintManager();
        if(clientBlueprintManager.hasSelectedBlueprint()) {
            final BlueprintMetadata selectedStructure = clientBlueprintManager.getSelectedStructure();
            final BlockRotation blockRotation = selectedStructure.getRotation();
            final String fileName = selectedStructure.getFile();
            blueprintsModelBuilder.buildBlueprint(fileName, blockRotation);
        }
    }

    @Override
    protected boolean shouldRender() {
        return getBlueprintManager().hasSelectedBlueprint();
    }

    @Override
    protected Vec3f getColorModulator() {
        return getBlueprintManager().isCantBuild() ? WRONG_PLACEMENT_COLOR : CORRECT_PLACEMENT_COLOR;
    }

    @Override
    protected Optional<BlockPos> getRenderTargetPosition() {
        final ClientBlueprintManager blueprintManager = getBlueprintManager();
        final int floorLevel = blueprintManager.getSelectedStructure().getFloorLevel();
        return Optional.ofNullable(blueprintManager.getBlueprintBuildPos()).map(o -> o.down(floorLevel));
    }


    public void renderBlueprintPreview(String fileName, BlockRotation blockRotation) {
        final BuiltBlueprint builtBlueprint = getBuiltBlueprint(fileName, blockRotation);

        final Vec3i size = builtBlueprint.getSize();
        final int biggestSideSize = Math.max(Math.max(size.getX(), size.getY()), size.getZ());

        final float scale = 80f / biggestSideSize;
        final float scaleFactor = 2f / scale;
        final float x = 130f * scaleFactor;
        final float y = -60f * scaleFactor;
        final float z = 30f * scaleFactor;

        renderBlueprintInGui(builtBlueprint, scale, x, y, z);
    }

    public void renderBlueprintInGui(String fileName, BlockRotation blockRotation, int slotColumn, int slotRow) {
        final BuiltBlueprint builtBlueprint = getBuiltBlueprint(fileName, blockRotation);

        final Vec3i size = builtBlueprint.getSize();
        final int biggestSideSize = Math.max(Math.max(size.getX(), size.getY()), size.getZ());

        final float scale = 1.6f * 7 / biggestSideSize;
        final float scaleFactor = 2f/scale;
        final float x = 8.5f * scaleFactor + 11.25f * slotColumn * scaleFactor / 1.25f;
        final float y = -17f * scaleFactor - 11.25f * slotRow  * scaleFactor / 1.25f;
        final float z = 22f * scaleFactor;

        renderBlueprintInGui(builtBlueprint, scale, x, y, z);
    }

    public BlueprintsModelBuilder getBlueprintsModelBuilder() {
        return blueprintsModelBuilder;
    }

    private void renderBlueprintInGui(BuiltBlueprint builtBlueprint, float scale, float x, float y, float z) {
        super.client.getProfiler().push("blueprint_render_model");
        DiffuseLighting.enableGuiDepthLighting();

        // calculating matrix
        final MatrixStack matrices = RenderSystem.getModelViewStack();
        final Matrix4f projectionMatrix4f = RenderSystem.getProjectionMatrix();

        final Vec3f cameraMove = new Vec3f(x, y, z);
        matrices.push();

        rotateScene(matrices, cameraMove);
        matrices.scale(scale, -scale, scale);
        matrices.translate(cameraMove.getX(), cameraMove.getY(), cameraMove.getZ());

        this.renderLayer(RenderLayer.getSolid(), builtBlueprint, matrices, projectionMatrix4f);
        this.renderLayer(RenderLayer.getCutout(), builtBlueprint, matrices, projectionMatrix4f);
        this.renderLayer(RenderLayer.getCutoutMipped(), builtBlueprint, matrices, projectionMatrix4f);

        matrices.pop();
        super.client.getProfiler().pop();
    }

    @Override
    protected Optional<BuiltModel> getBuiltModel() {
        final BlueprintMetadata selectedStructure = getBlueprintManager().getSelectedStructure();
        final BuiltBlueprint nullableBlueprint = this.blueprintsModelBuilder.getOrBuildBlueprint(selectedStructure.getFile(), selectedStructure.getRotation());
        return Optional.ofNullable(nullableBlueprint);
    }

    private BuiltBlueprint getBuiltBlueprint(String fileName, BlockRotation blockRotation) {
        this.client.getProfiler().push("blueprint_build_model");
        final BuiltBlueprint builtBlueprint = blueprintsModelBuilder.getOrBuildBlueprint(fileName, blockRotation);
        this.client.getProfiler().pop();

        return builtBlueprint;
    }

    private void rotateScene(MatrixStack matrices, Vec3f cameraMove) {
        final float yaw = 135f;
        final float pitch = -30f;

        // calculating rotations
        final Vec3f yawSceneRotationAxis = Vec3f.POSITIVE_Y;
        final Vec3f yawMoveRotationAxis = Vec3f.NEGATIVE_Y;
        final Quaternion yawSceneRotation = yawSceneRotationAxis.getDegreesQuaternion(yaw);
        final Quaternion yawMoveRotation = yawMoveRotationAxis.getDegreesQuaternion(yaw);

        final Vec3f pitchSceneRotationAxis = Vec3f.POSITIVE_X.copy();
        final Vec3f pitchMoveRotationAxis = Vec3f.POSITIVE_X.copy();
        pitchSceneRotationAxis.rotate(yawMoveRotation);
        pitchMoveRotationAxis.rotate(yawMoveRotation);
        final Quaternion pitchSceneRotation = pitchSceneRotationAxis.getDegreesQuaternion(pitch);
        final Quaternion pitchMoveRotation = pitchMoveRotationAxis.getDegreesQuaternion(pitch);

        // rotating camera
        cameraMove.rotate(yawMoveRotation);
        cameraMove.rotate(pitchMoveRotation);

        matrices.multiply(yawSceneRotation);
        matrices.multiply(pitchSceneRotation);
    }

    private void renderLayer(RenderLayer renderLayer, BuiltBlueprint builtBlueprint, MatrixStack matrices, Matrix4f matrix4f) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);

        renderLayer.startDrawing();

        VertexFormat vertexFormat = renderLayer.getVertexFormat();

        Shader shader = RenderSystem.getShader();
        BufferRenderer.unbindAll();
        int k;
        for (int i = 0; i < 12; ++i) {
            k = RenderSystem.getShaderTexture(i);
            shader.addSampler("Sampler" + i, k);
        }
        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(matrices.peek().getModel());
        }
        if (shader.projectionMat != null) {
            shader.projectionMat.set(matrix4f);
        }
        if (shader.colorModulator != null) {
            shader.colorModulator.set(new Vec3f(1F, 1.0F, 1F));
        }
        if (shader.fogStart != null) {
            shader.fogStart.set(RenderSystem.getShaderFogStart());
        }
        if (shader.fogEnd != null) {
            shader.fogEnd.set(RenderSystem.getShaderFogEnd());
        }
        if (shader.fogColor != null) {
            shader.fogColor.set(RenderSystem.getShaderFogColor());
        }
        if (shader.textureMat != null) {
            shader.textureMat.set(RenderSystem.getTextureMatrix());
        }
        if (shader.gameTime != null) {
            shader.gameTime.set(RenderSystem.getShaderGameTime());
        }
        RenderSystem.setupShaderLights(shader);
        shader.bind();

        final GlUniform chunkOffset = shader.chunkOffset;

        final boolean hasLayer = builtBlueprint.hasLayer(renderLayer);
        if(hasLayer) {
            final VertexBuffer vertexBuffer = builtBlueprint.getBuffer(renderLayer);

            if (chunkOffset != null) {
                chunkOffset.set(Vec3f.ZERO);
                chunkOffset.upload();
            }

            vertexBuffer.drawVertices();
        }

        if(chunkOffset != null) chunkOffset.set(Vec3f.ZERO);
        shader.unbind();

        if(hasLayer) vertexFormat.endDrawing();

        VertexBuffer.unbind();
        VertexBuffer.unbindVertexArray();
        renderLayer.endDrawing();
    }

    private ClientBlueprintManager getBlueprintManager() {
        final FortressMinecraftClient fortressClient = (FortressMinecraftClient) this.client;
        return fortressClient.getBlueprintManager();
    }
}
