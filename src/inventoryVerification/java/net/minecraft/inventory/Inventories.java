package net.minecraft.inventory;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
public class Inventories {
    public static ItemStack splitStack(List<ItemStack> stacks, int slot, int count) {
        var stack = stacks.get(slot);
        if (stack.isEmpty() || count <= 0) return ItemStack.EMPTY;
        var taken = stack.split(count);
        if (stack.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
        return taken;
    }
    public static void writeNbt(NbtCompound nbt, List<ItemStack> stacks, boolean empty) { nbt.stacks = stacks.stream().map(ItemStack::copy).toList(); }
    public static void readNbt(NbtCompound nbt, List<ItemStack> stacks) { for (int i = 0; i < stacks.size(); i++) stacks.set(i, nbt.stacks.get(i).copy()); }
}
