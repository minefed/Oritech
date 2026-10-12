package rearth.oritech.network;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.InventoryInputMode;
public class NetworkContent {
    public record MachineSyncPacket(BlockPos pos, long energy, long maxEnergy, long maxInsert, int progress, Identifier activeRecipeId, OritechRecipe activeRecipe, InventoryInputMode inputMode) {}
    public record MachineFrameGuiPacket(BlockPos pos, long energy, long maxEnergy, int progress) {}
    public static final Channel MACHINE_CHANNEL = new Channel();
    public static class Channel {
        public Handle serverHandle(Object owner) { return new Handle(); }
        public static class Handle { public void send(Object packet) {} }
    }
}
