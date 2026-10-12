package net.minecraft.recipe;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;
public class SmeltingRecipe implements Recipe<Inventory> {
    private final Ingredient input; private final ItemStack result; private final int time;
    public SmeltingRecipe(Ingredient input, ItemStack result, int time) { this.input = input; this.result = result; this.time = time; }
    public boolean matches(Inventory inventory, World world) { return input.test(inventory.getStack(0)); }
    public ItemStack craft(Inventory inventory, DynamicRegistryManager manager) { return result.copy(); }
    public ItemStack getResult(DynamicRegistryManager manager) { return result; } public int getCookingTime() { return time; }
    public boolean fits(int width, int height) { return true; } public RecipeSerializer<?> getSerializer() { return null; }
    public RecipeType<?> getType() { return RecipeType.SMELTING; }
}
