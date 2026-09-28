package rearth.oritech.client.renderers;

import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import rearth.oritech.block.entity.machines.storage.SmallFluidTankEntity;

public class SmallTankRenderer implements BlockEntityRenderer<SmallFluidTankEntity> {
    
    // constant render data, allocated once instead of per frame. Never modified.
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float[] FRAME_U = new float[] {0, 1, 1, 0};
    private static final float[] FRAME_V = new float[] {0, 0, 1, 1};
    private static final float[][] VERTICES_UP = new float[][]{
      {0, 1, 0}, // Top-left
      {1, 1, 0}, // Top-right
      {1, 1, 1}, // Bottom-right
      {0, 1, 1}  // Bottom-left
    };
    private static final float[][] VERTICES_DOWN = new float[][]{
      {0, 0, 1}, // Top-left
      {1, 0, 1}, // Top-right
      {1, 0, 0}, // Bottom-right
      {0, 0, 0}  // Bottom-left
    };
    private static final float[][] VERTICES_NORTH = new float[][]{
      {1, 1, 0}, // Top-left
      {0, 1, 0}, // Top-right
      {0, 0, 0}, // Bottom-right
      {1, 0, 0}  // Bottom-left
    };
    private static final float[][] VERTICES_SOUTH = new float[][]{
      {0, 1, 1}, // Top-left
      {1, 1, 1}, // Top-right
      {1, 0, 1}, // Bottom-right
      {0, 0, 1}  // Bottom-left
    };
    private static final float[][] VERTICES_WEST = new float[][]{
      {0, 1, 0}, // Top-left
      {0, 1, 1}, // Top-right
      {0, 0, 1}, // Bottom-right
      {0, 0, 0}  // Bottom-left
    };
    private static final float[][] VERTICES_EAST = new float[][]{
      {1, 1, 1}, // Top-left
      {1, 1, 0}, // Top-right
      {1, 0, 0}, // Bottom-right
      {1, 0, 1}  // Bottom-left
    };
    
    @Override
    public void render(SmallFluidTankEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        var storage = entity.getForDirectFluidAccess();
        if (storage.amount == 0 || storage.variant.isBlank()) return;
        
        var fluid = storage.variant;
        var fill = storage.amount / (float) storage.getCapacity();
        
        var sprite = FluidVariantRendering.getSprite(fluid);
        var spriteColor = FluidVariantRendering.getColor(fluid);
        var consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucent());
        
        matrices.push();
        matrices.translate(0.126, 0.126, 0.126);
        matrices.scale(0.745f, 0.745f * fill, 0.745f);
        
        // because fabric fluidRender() doesnt seem to do the job, we manually draw rects:
        var entry = matrices.peek();
        var modelMatrix = entry.getPositionMatrix();
        var normalMatrix = entry.getNormalMatrix();
        
        // Draw the cube using quads
        for (Direction direction : DIRECTIONS) {
            if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
            drawQuad(direction, consumer, modelMatrix, normalMatrix, sprite, spriteColor, light, overlay);
        }
        
        matrices.pop();
        
    }
    
    private void drawQuad(Direction direction, VertexConsumer consumer, Matrix4f modelMatrix, Matrix3f normalMatrix, Sprite sprite, int color, int light, int overlay) {
        // Define the vertices of the quad based on the direction it's facing
        
        var normal = direction.getUnitVector();
        
        var positions = getQuadVerticesByDirection(direction);
        
        for (int i = positions.length - 1; i >= 0; i--) {
            
            var pos = positions[i];
            var u = sprite.getFrameU(FRAME_U[i]);
            var v = sprite.getFrameV(FRAME_V[i]);
            
            consumer.vertex(modelMatrix, pos[0], pos[1], pos[2])
              .color(color)
              .texture(u, v)
              .light(light)
              .overlay(overlay)
              .normal(normalMatrix, normal.x, normal.y, normal.z)
              .next();
        }
        
    }
    
    private static float[][] getQuadVerticesByDirection(Direction direction) {
        // Define the vertices for each face of the cube
        return switch (direction) {
            case UP -> VERTICES_UP;
            case DOWN -> VERTICES_DOWN;
            case NORTH -> VERTICES_NORTH;
            case SOUTH -> VERTICES_SOUTH;
            case WEST -> VERTICES_WEST;
            case EAST -> VERTICES_EAST;
        };
    }
}
