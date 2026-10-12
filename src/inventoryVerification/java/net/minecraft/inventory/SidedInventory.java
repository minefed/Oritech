package net.minecraft.inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
public interface SidedInventory extends Inventory {
    int[] getAvailableSlots(Direction side); boolean canInsert(int slot, ItemStack stack, Direction side); boolean canExtract(int slot, ItemStack stack, Direction side);
}
