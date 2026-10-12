package net.minecraft.recipe;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
public interface Recipe<C extends Inventory> {
    boolean matches(C inventory, World world); ItemStack craft(C inventory, DynamicRegistryManager manager);
    boolean fits(int width, int height); ItemStack getResult(DynamicRegistryManager manager);
    RecipeSerializer<?> getSerializer(); RecipeType<?> getType();
    default boolean isIgnoredInRecipeBook() { return false; }
    default DefaultedList<Ingredient> getIngredients() { return DefaultedList.of(); }
}
