package rearth.oritech.network;
import java.util.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
public final class NetworkContent {
    public record MachineFrameGuiPacket(BlockPos position, long currentEnergy, long maxEnergy, int progress) {}
    public record Delivery(PlayerEntity player, MachineFrameGuiPacket packet) {}
    public record InventorySyncPacket(BlockPos position, java.util.List<net.minecraft.item.ItemStack> heldStacks) {}
    public static final Channel MACHINE_CHANNEL = new Channel();
    public static final class Channel {
        public final List<Delivery> deliveries = new ArrayList<>();
        public boolean failNext;
        public int inventoryBroadcasts;
        public Handle serverHandle(PlayerEntity player) { return new Handle(player); }
        public Handle serverHandle(net.minecraft.block.entity.BlockEntity entity) { return new Handle(null); }
        public final class Handle {
            private final PlayerEntity player;
            Handle(PlayerEntity player) { this.player = player; }
            public void send(MachineFrameGuiPacket packet) {
                if (player == null) throw new AssertionError("Unexpected GUI tracker broadcast");
                if (failNext) { failNext = false; throw new IllegalStateException("Injected send failure"); }
                deliveries.add(new Delivery(player, packet));
            }
            public void send(InventorySyncPacket packet) {
                if (player != null) throw new AssertionError("Inventory broadcast contract changed");
                inventoryBroadcasts++;
            }
        }
    }
}
