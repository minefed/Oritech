package rearth.oritech.util;
import java.util.List;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
public interface MachineAddonController {
    Object ADDON_UI_ENDEC = new Object();
    BaseAddonData DEFAULT_ADDON_DATA = new BaseAddonData(1, 1);
    List<BlockPos> getConnectedAddons(); List<BlockPos> getOpenSlots();
    BlockPos getMachinePos(); World getMachineWorld(); Direction getFacingForAddon();
    DynamicEnergyStorage getStorageForAddon(); SimpleInventory getInventoryForAddon(); ScreenProvider getScreenProvider();
    BaseAddonData getBaseAddonData(); void setBaseAddonData(BaseAddonData data);
    long getDefaultCapacity(); long getDefaultInsertRate();
    default float getCoreQuality() { return 1; }
    default Object getUiData() { return List.of(); }
    default void loadAddonNbtData(NbtCompound nbt) {} default void writeAddonToNbt(NbtCompound nbt) {} default void updateEnergyContainer() {}
}
