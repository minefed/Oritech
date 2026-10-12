package net.minecraft.item;
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack();
    private final Object item;
    public ItemStack() { item = null; }
    public ItemStack(Object item) { this.item = item; }
    public ItemStack copy() { return this; } public boolean isEmpty() { return item == null; }
    public Object getItem() { return item; } public void decrement(int amount) {}
}
