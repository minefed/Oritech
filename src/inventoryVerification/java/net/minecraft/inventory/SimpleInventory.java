package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
public class SimpleInventory implements Inventory {
    public static int sizedConstructions, varargsConstructions;
    public final DefaultedList<ItemStack> heldStacks;
    public SimpleInventory(int size) { sizedConstructions++; heldStacks = DefaultedList.ofSize(size, ItemStack.EMPTY); }
    public SimpleInventory(ItemStack... stacks) { varargsConstructions++; heldStacks = DefaultedList.copyOf(ItemStack.EMPTY, stacks); }
    public void markDirty() {}
    public void readNbtList(NbtList list) {} public NbtList toNbtList() { return new NbtList(); }
    public int size() { return heldStacks.size(); }
    public ItemStack getStack(int slot) { return slot >= 0 && slot < size() ? heldStacks.get(slot) : ItemStack.EMPTY; }
    public void setStack(int slot, ItemStack stack) { heldStacks.set(slot, stack); if (!stack.isEmpty() && stack.getCount() > 64) stack.setCount(64); markDirty(); }
    public ItemStack removeStack(int slot, int count) { var result = Inventories.splitStack(heldStacks, slot, count); if (!result.isEmpty()) markDirty(); return result; }
    public ItemStack removeStack(int slot) { var old = getStack(slot); if (!old.isEmpty()) heldStacks.set(slot, ItemStack.EMPTY); return old; }
    public void clear() { for (int i = 0; i < size(); i++) heldStacks.set(i, ItemStack.EMPTY); markDirty(); }
}
