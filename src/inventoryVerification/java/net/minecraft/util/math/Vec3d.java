package net.minecraft.util.math;
public record Vec3d(double x, double y, double z) { public static Vec3d of(BlockPos pos) { return new Vec3d(pos.getX(), pos.getY(), pos.getZ()); } }
