package net.minecraft.nbt;
import java.util.*;
import net.minecraft.item.ItemStack;
public class NbtCompound {
    private final Map<String, Object> values = new HashMap<>();
    public List<ItemStack> stacks = List.of();
    public NbtList getList(String key, int type) { return new NbtList(); }
    public long getLong(String key) { return ((Number) values.getOrDefault(key, 0)).longValue(); }
    public int getInt(String key) { return ((Number) values.getOrDefault(key, 0)).intValue(); }
    public short getShort(String key) { return ((Number) values.getOrDefault(key, 0)).shortValue(); }
    public void put(String key, Object value) { values.put(key, value); } public void putLong(String key, long value) { put(key, value); }
    public void putInt(String key, int value) { put(key, value); } public void putShort(String key, short value) { put(key, value); }
}
