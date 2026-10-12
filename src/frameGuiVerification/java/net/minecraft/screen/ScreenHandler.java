package net.minecraft.screen;
import java.util.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
public abstract class ScreenHandler {
    public final int syncId;
    public final List<Slot> slots = new ArrayList<>();
    protected ScreenHandler(Object type, int id) { syncId = id; }
    protected void addSlot(Slot slot) { slots.add(slot); }
    protected boolean insertItem(ItemStack stack, int start, int end, boolean reverse) { return true; }
    public abstract ItemStack quickMove(PlayerEntity player, int slot);
    public abstract boolean canUse(PlayerEntity player);
}
