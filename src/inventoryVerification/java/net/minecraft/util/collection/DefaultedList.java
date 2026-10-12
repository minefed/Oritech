package net.minecraft.util.collection;
import java.util.*;
public class DefaultedList<T> extends ArrayList<T> {
    public static int toArrayCalls, subListCalls;
    public static <T> DefaultedList<T> of() { return new DefaultedList<>(); }
    public static <T> DefaultedList<T> ofSize(int size, T value) { var list = new DefaultedList<T>(); for (int i = 0; i < size; i++) list.add(value); return list; }
    public static <T> DefaultedList<T> copyOf(T empty, T... values) { var list = new DefaultedList<T>(); Collections.addAll(list, values); return list; }
    public List<T> subList(int from, int to) {
        subListCalls++;
        var view = super.subList(from, to);
        return new AbstractList<>() {
            public T get(int i) { return view.get(i); } public T set(int i, T value) { return view.set(i, value); } public int size() { return view.size(); }
            public <U> U[] toArray(U[] array) { toArrayCalls++; return super.toArray(array); }
        };
    }
}
