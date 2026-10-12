package rearth.oritech.util;
import java.util.List;
import net.minecraft.inventory.Inventory;
public interface ScreenProvider {
    record GuiSlot(int index, int x, int y) {}
    List<GuiSlot> getGuiSlots();
    float getProgress();
    float getDisplayedEnergyUsage();
    InventoryInputMode getInventoryInputMode();
    Inventory getDisplayedInventory();
    default Object getScreenHandlerType() { return new Object(); }
    default boolean inputOptionsEnabled() { return true; }
}
