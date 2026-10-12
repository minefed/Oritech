package net.minecraft.world;
import java.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
public class World {
    public final boolean isClient;
    public long time = 100;
    public final Random random = new Random(42);
    public RecipeManager recipeManager = new RecipeManager();
    public final List<ServerPlayerEntity> trackers = new ArrayList<>();
    public final Map<BlockPos, BlockEntity> entities = new HashMap<>();
    public World(boolean client) { isClient = client; }
    public long getTime() { return time; } public RecipeManager getRecipeManager() { return recipeManager; }
    public DynamicRegistryManager getRegistryManager() { return new DynamicRegistryManager(); }
    public BlockEntity getBlockEntity(BlockPos pos) { return entities.get(pos); }
    public BlockState getBlockState(BlockPos pos) { return new BlockState(); }
    public ServerPlayerEntity getClosestPlayer(double x, double y, double z, double radius, boolean creative) { return null; }
}
