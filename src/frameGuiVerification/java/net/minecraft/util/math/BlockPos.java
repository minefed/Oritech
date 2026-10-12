package net.minecraft.util.math;
public record BlockPos(int x, int y, int z) {
    public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);
    public int getX() { return x; } public int getY() { return y; } public int getZ() { return z; }
    public BlockPos down() { return new BlockPos(x, y - 1, z); }
}
