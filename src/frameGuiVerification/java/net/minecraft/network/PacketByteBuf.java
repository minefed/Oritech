package net.minecraft.network;
import java.util.*;
import net.minecraft.util.math.BlockPos;
public class PacketByteBuf {
    public final List<Object> values = new ArrayList<>();
    private int index;
    public void writeBlockPos(BlockPos pos) { values.add(pos); }
    public void write(Object codec, Object value) { values.add(value); }
    public void writeFloat(float value) { values.add(value); }
    public void writeInt(int value) { values.add(value); }
    public BlockPos readBlockPos() { return (BlockPos) values.get(index++); }
    public int readInt() { return (int) values.get(index++); }
}
