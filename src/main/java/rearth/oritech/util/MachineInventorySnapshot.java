package rearth.oritech.util;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

import java.util.List;

/** A detached slot snapshot borrowed only for a synchronous, non-retaining operation. */
public final class MachineInventorySnapshot {
    private SimpleInventory snapshot;
    private boolean borrowed;

    public boolean isBorrowed() {
        return borrowed;
    }

    /** A nested query must use its own ordinary snapshot rather than overwrite the outer one. */
    public SimpleInventory borrow(List<ItemStack> source, int start, int count) {
        if (borrowed) return null;
        int end = start + count;
        if (start < 0 || end > source.size()) throw new IndexOutOfBoundsException();
        if (start > end) throw new IllegalArgumentException();
        if (snapshot == null || snapshot.size() != count) snapshot = new SimpleInventory(count);
        for (int i = 0; i < count; i++) {
            // Match SimpleInventory(ItemStack...): share stacks without clamping counts or writing to the source list.
            snapshot.heldStacks.set(i, source.get(start + i));
        }
        borrowed = true;
        return snapshot;
    }

    public void release() {
        for (int i = 0; i < snapshot.size(); i++) snapshot.heldStacks.set(i, ItemStack.EMPTY);
        borrowed = false;
    }
}
