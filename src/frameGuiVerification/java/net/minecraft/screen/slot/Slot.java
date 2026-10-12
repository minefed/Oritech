package net.minecraft.screen.slot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
public class Slot {
    public Slot(Inventory inventory, int index, int x, int y) {}
    public boolean hasStack() { return false; }
    public ItemStack getStack() { return ItemStack.EMPTY; }
    public void setStack(ItemStack stack) {}
    public void markDirty() {}
}
