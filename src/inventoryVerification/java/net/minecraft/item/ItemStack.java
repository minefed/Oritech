package net.minecraft.item;
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(null, 0);
    private final Object item;
    private int count;
    public ItemStack() { this(null, 0); } public ItemStack(Object item) { this(item, 1); }
    public ItemStack(Object item, int count) { this.item = item; this.count = count; }
    public ItemStack copy() { return isEmpty() ? EMPTY : new ItemStack(item, count); }
    public boolean isEmpty() { return item == null || count <= 0; }
    public Object getItem() { return item; } public int getCount() { return count; } public int getMaxCount() { return 64; }
    public void setCount(int count) { this.count = count; } public void decrement(int amount) { count -= amount; } public void increment(int amount) { count += amount; }
    public ItemStack split(int amount) { int taken = Math.min(amount, count); count -= taken; return new ItemStack(item, taken); }
    public String toString() { return isEmpty() ? "empty" : item + ":" + count; }
}
