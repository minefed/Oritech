package rearth.oritech.block.base.entity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
public abstract class FrameInteractionBlockEntity extends BlockEntity {
    public int simulationTicks;
    public boolean advanceSimulation;
    private int progress;
    public FrameInteractionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public void tick(World world, BlockPos pos, BlockState state, FrameInteractionBlockEntity entity) { simulationTicks++; if (advanceSimulation) progress++; }
    protected abstract boolean canProgress(); protected abstract void doProgress(boolean moving); public abstract void finishBlockWork(BlockPos pos);
    protected Direction getFacing() { return Direction.NORTH; }
    public int getCurrentProgress() { return progress; } public void setCurrentProgress(int progress) { this.progress = progress; }
    public int getMoveTime() { return 10; } public int getWorkTime() { return 20; } public long getMoveStartedAt() { return 0; }
    public float getSpeedMultiplier() { return 1; }
    public void updateNetwork() {}
    protected boolean hasWorkAvailable(BlockPos pos) { return true; }
    public BlockState getMachineHead() { return new BlockState(); }
    public java.util.List<Vec3i> getAddonSlots() { return java.util.List.of(); }
    public net.minecraft.item.ItemStack getToolheadAdditionalRender() { return null; }
    public net.minecraft.screen.ScreenHandlerType<?> getScreenHandlerType() { return null; }
}
