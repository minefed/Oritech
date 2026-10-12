package net.minecraft.util.collection;
import java.util.ArrayList;
public class DefaultedList<T> extends ArrayList<T> { public static <T> DefaultedList<T> of() { return new DefaultedList<>(); } }
