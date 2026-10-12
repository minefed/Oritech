package net.minecraft.item;
public class Items {
    public static final Item IRON_INGOT = new Item("iron"), IRON_BLOCK = new Item("iron-block");
    public record Item(String name) { public ItemStack getDefaultStack() { return new ItemStack(name); } }
}
