package net.fabricmc.fabric.api.networking.v1;
import java.util.Collection;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
public final class PlayerLookup {
    public static Collection<ServerPlayerEntity> tracking(BlockEntity entity) { return entity.getWorld().trackers; }
}
