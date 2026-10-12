package net.minecraft.block.entity;
public interface BlockEntityTicker<T> { void tick(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state, T entity); }
