package net.minecraft.server.network;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
public class ServerPlayerEntity extends PlayerEntity { public ServerPlayerEntity(World world) { this.world = world; } }
