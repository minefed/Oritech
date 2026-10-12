package rearth.oritech.util;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
public interface ImplementedInventory extends Inventory {
    DefaultedList<ItemStack> getItems();
    boolean canInsert(int slot, ItemStack stack, Direction direction);
    boolean canExtract(int slot, ItemStack stack, Direction direction);
}
