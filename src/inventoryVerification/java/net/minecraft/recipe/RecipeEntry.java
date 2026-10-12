package net.minecraft.recipe;
public record RecipeEntry<T extends Recipe<?>>(net.minecraft.util.Identifier id, T value) {}
