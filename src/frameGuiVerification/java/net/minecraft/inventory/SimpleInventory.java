package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
public class SimpleInventory implements Inventory {
    public final DefaultedList<ItemStack> heldStacks = DefaultedList.of();
    public SimpleInventory(int size) { for (int i = 0; i < size; i++) heldStacks.add(ItemStack.EMPTY); }
    public void markDirty() {}
    public void readNbtList(NbtList list) {}
    public NbtList toNbtList() { return new NbtList(); }
    public int size() { return heldStacks.size(); }
    public void setStack(int slot, ItemStack stack) { heldStacks.set(slot, stack); markDirty(); }
}
