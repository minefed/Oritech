package net.minecraft.inventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
public interface Inventory {
    int size(); default ItemStack getStack(int slot) { return ItemStack.EMPTY; }
    default boolean isEmpty() { for (int i = 0; i < size(); i++) if (!getStack(i).isEmpty()) return false; return true; }
    default void onOpen(PlayerEntity player) {} default boolean canPlayerUse(PlayerEntity player) { return true; }
    default int getMaxCountPerStack() { return 64; }
}
