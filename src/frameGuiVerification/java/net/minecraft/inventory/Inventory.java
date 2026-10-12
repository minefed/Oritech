package net.minecraft.inventory;
import net.minecraft.entity.player.PlayerEntity;
public interface Inventory {
    default void onOpen(PlayerEntity player) {}
    default boolean canPlayerUse(PlayerEntity player) { return true; }
    default int size() { return 1; }
}
