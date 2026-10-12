package net.minecraft.block;
public class Block {
    public BlockState getDefaultState() { return new BlockState(); }
    public static Block getBlockFromItem(Object item) { return new Block(); }
}
