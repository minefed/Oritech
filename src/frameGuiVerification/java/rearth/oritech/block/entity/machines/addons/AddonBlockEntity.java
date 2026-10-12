package rearth.oritech.block.entity.machines.addons;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
public class AddonBlockEntity extends BlockEntity {
    private BlockPos controller = BlockPos.ORIGIN;
    public AddonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public BlockPos getControllerPos() { return controller; } public void setControllerPos(BlockPos pos) { controller = pos; }
}
