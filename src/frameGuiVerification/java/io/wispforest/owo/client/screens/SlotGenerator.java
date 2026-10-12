package io.wispforest.owo.client.screens;
import java.util.function.Consumer;
import net.minecraft.screen.slot.Slot;
import net.minecraft.entity.player.PlayerInventory;
public class SlotGenerator { public static SlotGenerator begin(Consumer<Slot> consumer, int x, int y) { return new SlotGenerator(); } public void playerInventory(PlayerInventory inv) {} }
