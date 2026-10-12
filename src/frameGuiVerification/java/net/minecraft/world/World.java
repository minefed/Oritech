package net.minecraft.world;
import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
public class World {
    public final boolean isClient;
    public final List<ServerPlayerEntity> trackers = new ArrayList<>();
    public final Map<BlockPos, BlockEntity> entities = new HashMap<>();
    public World(boolean client) { isClient = client; }
    public long getTime() { return 100; }
    public BlockEntity getBlockEntity(BlockPos pos) { return entities.get(pos); }
    public BlockState getBlockState(BlockPos pos) { return new BlockState(); }
    public ServerPlayerEntity getClosestPlayer(double x, double y, double z, double radius, boolean ignoreCreative) {
        ServerPlayerEntity closest = null;
        double best = radius * radius;
        for (var player : trackers) {
            double distance = Math.pow(player.x - x, 2) + Math.pow(player.y - y, 2) + Math.pow(player.z - z, 2);
            if (player.getWorld() == this && distance < best) { closest = player; best = distance; }
        }
        return closest;
    }
    public void setBlockState(BlockPos pos, BlockState state) {}
    public void playSound(Object player, BlockPos pos, Object sound, Object category, float volume, float pitch) {}
}
