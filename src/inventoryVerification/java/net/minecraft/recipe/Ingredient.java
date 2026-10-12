package net.minecraft.recipe;
import net.minecraft.item.ItemStack;
import java.util.function.Predicate;
public class Ingredient {
    public static final Ingredient EMPTY = new Ingredient(stack -> false);
    private final Predicate<ItemStack> predicate;
    public Ingredient(Predicate<ItemStack> predicate) { this.predicate = predicate; }
    public boolean test(ItemStack stack) { return predicate.test(stack); }
    public static Ingredient ofStacks(ItemStack... stacks) { return new Ingredient(stack -> !stack.isEmpty() && java.util.Arrays.stream(stacks).anyMatch(s -> s.getItem().equals(stack.getItem()))); }
}
