package net.minecraft.block.entity;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class BlockEntity {
    protected World world;
    protected final BlockPos pos;
    private final BlockState state;
    private boolean removed;
    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { this.pos = pos; this.state = state; }
    public World getWorld() { return world; }
    public void setWorld(World world) { this.world = world; }
    public BlockPos getPos() { return pos; }
    public BlockState getCachedState() { return state; }
    public void markDirty() {}
    public void markRemoved() { removed = true; }
    public void cancelRemoval() { removed = false; }
    public boolean isRemoved() { return removed; }
    public void readNbt(NbtCompound nbt) {}
    protected void writeNbt(NbtCompound nbt) {}
}
