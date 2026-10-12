package net.fabricmc.fabric.api.transfer.v1.item;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.Direction;
public final class InventoryStorage { public static InventoryStorage of(Inventory inv, Direction side) { return new InventoryStorage(); } }
