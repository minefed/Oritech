package net.minecraft.nbt;
public class NbtCompound {
    public NbtList getList(String key, int type) { return new NbtList(); }
    public long getLong(String key) { return 0; }
    public int getInt(String key) { return 0; }
    public void put(String key, Object value) {}
    public void putLong(String key, long value) {}
    public void putInt(String key, int value) {}
}
