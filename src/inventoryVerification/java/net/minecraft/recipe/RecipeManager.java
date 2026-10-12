package net.minecraft.recipe;
import java.util.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
public class RecipeManager {
    private Map<RecipeType<?>, Map<Identifier, RecipeEntry<?>>> recipes = Map.of();
    public int firstMatchCalls, mapReads;
    public final List<String> matchingTrace = new ArrayList<>();
    public <C extends Inventory, T extends Recipe<C>> Optional<RecipeEntry<T>> getFirstMatch(RecipeType<T> type, C inventory, World world) {
        firstMatchCalls++;
        return this.<C, T>getAllOfType(type).values().stream().filter(entry -> {
            matchingTrace.add(entry.id().value());
            return entry.value().matches(inventory, world);
        }).findFirst();
    }
    @SuppressWarnings("unchecked") public <C extends Inventory, T extends Recipe<C>> Map<Identifier, RecipeEntry<T>> getAllOfType(RecipeType<T> type) { mapReads++; return (Map<Identifier, RecipeEntry<T>>) (Map<?, ?>) recipes.getOrDefault(type, Map.of()); }
    public <C extends Inventory, T extends Recipe<C>> List<RecipeEntry<T>> listAllOfType(RecipeType<T> type) { return List.copyOf(this.<C, T>getAllOfType(type).values()); }
    public Optional<RecipeEntry<?>> get(Identifier id) { for (var map : recipes.values()) { var entry = map.get(id); if (entry != null) return Optional.of(entry); } return Optional.empty(); }
    public void setRecipes(Iterable<RecipeEntry<?>> entries) {
        var byType = new HashMap<RecipeType<?>, Map<Identifier, RecipeEntry<?>>>();
        for (var entry : entries) byType.computeIfAbsent(entry.value().getType(), ignored -> new LinkedHashMap<>()).put(entry.id(), entry);
        var replacement = new HashMap<RecipeType<?>, Map<Identifier, RecipeEntry<?>>>();
        byType.forEach((type, map) -> replacement.put(type, Collections.unmodifiableMap(map)));
        recipes = Collections.unmodifiableMap(replacement);
    }
}
