package net.minecraft.block;
public class BlockState {
    public boolean get(Object property) { return true; }
    public Block getBlock() { return Blocks.AIR; }
    public boolean canPlaceAt(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) { return true; }
    public SoundGroup getSoundGroup() { return new SoundGroup(); }
    public static class SoundGroup { public Object getPlaceSound() { return new Object(); } }
}
