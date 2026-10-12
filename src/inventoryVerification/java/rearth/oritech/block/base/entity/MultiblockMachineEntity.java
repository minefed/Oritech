package rearth.oritech.block.base.entity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.*;
public abstract class MultiblockMachineEntity extends MachineBlockEntity {
    protected MultiblockMachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energy) { super(type, pos, state, energy); }
    public abstract java.util.List<Vec3i> getCorePositions(); public abstract java.util.List<Vec3i> getAddonSlots();
}
