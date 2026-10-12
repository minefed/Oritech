package rearth.oritech.verification;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.LegacyMachineBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.machines.processing.LegacyPoweredFurnaceBlockEntity;
import rearth.oritech.block.entity.machines.processing.PoweredFurnaceBlockEntity;
import rearth.oritech.init.recipes.*;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.MachineInventorySnapshot;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/** Executes actual production classes and class-renamed, frozen baseline sources. */
public final class MachineInventoryVerification {
    private static int assertions, scenarios;

    public static void main(String[] args) throws Exception {
        freshSnapshotContract();
        recipeOrderAndAllocationWorkload();
        dynamicSlotsAndEmptyRanges();
        craftingEnergyAndOutputBlocking();
        sidedInsertionRemovalAndNbt();
        inheritedOverrides();
        unknownRecipesAndManagers();
        reentrantAndThrowingIngredients();
        reloadEligibilityAndWeakReferences();
        furnaceTimingAndLookupWorkload();
        reentrantOutputAndThrowCleanup();
        lookupAndSlotReadOrder();
        System.out.println("Machine inventory snapshots: " + scenarios + " scenarios, " + assertions + " assertions passed");
    }

    private static void freshSnapshotContract() {
        for (var machine : List.of(new NewMachine(), new OldMachine())) {
            var stack = new ItemStack("a", 90);
            machine.inventory().heldStacks.set(1, stack);
            var old = (SimpleInventory) machine.input();
            check(old.getStack(0) == stack && stack.getCount() == 90, "Snapshots alias ItemStack without clamping");
            stack.decrement(2);
            check(old.getStack(0).getCount() == 88, "In-place count changes remain visible through captured stack");
            var replacement = new ItemStack("b", 3);
            machine.inventory().heldStacks.set(1, replacement);
            var fresh = machine.input();
            check(old != fresh && old.getStack(0) == stack && fresh.getStack(0) == replacement,
              "Protected API retains independent old slot references across later calls");
            old.setStack(0, new ItemStack("local", 2));
            check(machine.inventory().getStack(1) == replacement, "Snapshot slot replacement is detached from machine");
            var output = machine.output();
            check(output != fresh && output.size() == 2, "Input and output snapshots remain independent");
        }
        scenario();
    }

    private static void recipeOrderAndAllocationWorkload() throws Exception {
        var pair = pair();
        var miss = recipe(List.of(ingredient("wrong")), List.of(new ItemStack("x")), 3);
        var match = recipe(List.of(ingredient("a"), ingredient("b")), List.of(new ItemStack("x"), new ItemStack("y", 2)), 3);
        installRecipes(pair, entry("first", miss), entry("second", match));
        for (var machine : pair.machines()) fill(machine, "a", 4, "b", 4);
        check(pair.newer.recipe().orElseThrow().id().equals(pair.older.recipe().orElseThrow().id()), "First matching recipe unchanged");
        equal(pair.newer.world().recipeManager.matchingTrace, pair.older.world().recipeManager.matchingTrace, "Recipe match order unchanged");
        equal(pair.newer.world().recipeManager.firstMatchCalls, pair.older.world().recipeManager.firstMatchCalls, "Exactly one manager lookup per query");
        clearAllocationCounters();
        for (int i = 0; i < 10_000; i++) { pair.older.recipe(); pair.older.canOutput(match); }
        int oldInventories = SimpleInventory.varargsConstructions, oldArrays = DefaultedList.toArrayCalls;
        clearAllocationCounters();
        for (int i = 0; i < 10_000; i++) { pair.newer.recipe(); pair.newer.canOutput(match); }
        check(oldInventories == 20_000 && oldArrays == 20_000, "Frozen baseline workload makes 20000 temporary inventories and toArray calls");
        check(SimpleInventory.varargsConstructions == 0 && DefaultedList.toArrayCalls == 0,
          "Safe query/output hot paths make no temporary varargs inventories or toArray calls");
        check(SimpleInventory.sizedConstructions == 1, "Only output snapshot first-use construction after input warmup");
        equal(pair.newer.slotReads(), pair.older.slotReads(), "One getSlots call per ordinary input/output query");
        checkReleased(pair.newer, "inputSnapshot"); checkReleased(pair.newer, "outputSnapshot");
        System.out.println("Deterministic 10000 input lookups + 10000 output checks: temporary varargs inventories 20000 -> 0; toArray calls 20000 -> 0; one output snapshot warmup (not measured GC/RAM).");
        scenario();
    }

    private static void dynamicSlotsAndEmptyRanges() {
        var pair = pair();
        var one = recipe(List.of(ingredient("c")), List.of(new ItemStack("z")), 3);
        installRecipes(pair, entry("one", one));
        for (var machine : pair.machines()) {
            machine.slots(new InventorySlotAssignment(3, 1, 6, 1));
            machine.inventory().heldStacks.set(3, new ItemStack("c", 2));
            machine.inventory().heldStacks.set(6, new ItemStack("z", 64));
        }
        compareQueryAndOutput(pair, one, "Changed range start and count");
        for (var machine : pair.machines()) {
            machine.slots(new InventorySlotAssignment(1, 2, 4, 2));
            fill(machine, "c", 2, "b", 2);
        }
        compareQueryAndOutput(pair, one, "Snapshot resized back to larger range");
        var zero = recipe(List.of(), List.of(), 1);
        installRecipes(pair, entry("zero", zero));
        for (var machine : pair.machines()) machine.slots(new InventorySlotAssignment(0, 0, 8, 0));
        compareQueryAndOutput(pair, zero, "Zero-sized ranges at list boundary");
        scenario();
    }

    private static void craftingEnergyAndOutputBlocking() {
        var pair = pair();
        var recipe = recipe(List.of(ingredient("a"), ingredient("b")), List.of(new ItemStack("x"), new ItemStack("y", 2)), 3);
        installRecipes(pair, entry("craft", recipe));
        for (var machine : pair.machines()) { fill(machine, "a", 3, "b", 3); machine.energy(100); }
        for (int tick = 1; tick <= 7; tick++) {
            tickBoth(pair);
            equal(pair.newer.state(), pair.older.state(), "Craft inventory/progress/energy at tick " + tick);
            if (tick == 2) check(pair.newer.inventory().getStack(4).isEmpty(), "No early output");
            if (tick == 3) check(pair.newer.inventory().getStack(4).getCount() == 1, "Craft finishes on exact third tick");
        }
        for (var machine : pair.machines()) machine.inventory().heldStacks.set(4, new ItemStack("wrong", 1));
        tickBoth(pair); equal(pair.newer.state(), pair.older.state(), "Mismatched output resets progress identically");
        check(pair.newer.progressValue() == 0, "Blocked output resets progress");
        for (var machine : pair.machines()) machine.inventory().heldStacks.set(4, new ItemStack("x", 64));
        tickBoth(pair); equal(pair.newer.state(), pair.older.state(), "Full output blocks identically");
        for (var machine : pair.machines()) { machine.inventory().heldStacks.set(4, ItemStack.EMPTY); machine.energy(0); }
        tickBoth(pair); equal(pair.newer.state(), pair.older.state(), "No-energy timing unchanged");
        for (var machine : pair.machines()) machine.energy(100);
        tickBoth(pair);
        for (var machine : pair.machines()) machine.energy(0);
        tickBoth(pair); equal(pair.newer.state(), pair.older.state(), "Energy shortage retains valid progress");
        check(pair.newer.progressValue() == 1, "Valid recipe progress pauses without energy");
        scenario();
    }

    private static void sidedInsertionRemovalAndNbt() {
        var pair = pair();
        var recipe = recipe(List.of(ingredient("a"), ingredient("b")), List.of(new ItemStack("x")), 3);
        installRecipes(pair, entry("sided", recipe));
        for (var mode : InventoryInputMode.values()) {
            for (var machine : pair.machines()) { machine.inventory().clear(); machine.mode(mode); }
            for (var direction : Direction.values()) {
                var newer = (SidedInventory) pair.newer.inventory(); var older = (SidedInventory) pair.older.inventory();
                equal(Arrays.toString(newer.getAvailableSlots(direction)), Arrays.toString(older.getAvailableSlots(direction)), "Hopper/pipe exposed slot order " + mode + direction);
                for (int slot = 0; slot < 8; slot++) {
                    equal(newer.canInsert(slot, new ItemStack("a"), direction), older.canInsert(slot, new ItemStack("a"), direction), "Insertion rule " + mode + direction + slot);
                    equal(newer.canExtract(slot, new ItemStack("x"), direction), older.canExtract(slot, new ItemStack("x"), direction), "Extraction rule " + mode + direction + slot);
                }
            }
        }
        for (var machine : pair.machines()) {
            machine.mode(InventoryInputMode.FILL_LEFT_TO_RIGHT);
            fill(machine, "a", 2, "b", 2);
            machine.recipe();
            machine.inventory().removeStack(1, 1);
            machine.inventory().getStack(2).increment(1);
        }
        compareQueryAndOutput(pair, recipe, "Removal and in-place counts reflected next query");
        for (var machine : pair.machines()) {
            var saved = new NbtCompound(); machine.write(saved);
            machine.inventory().clear(); machine.read(saved);
        }
        compareQueryAndOutput(pair, recipe, "NBT replacement after snapshot use");
        equal(pair.newer.state(), pair.older.state(), "NBT/crafting state unchanged");
        scenario();
    }

    private static void inheritedOverrides() {
        var pairs = List.of(new Pair(new InheritedInputGetter(), new LegacyInheritedInputGetter()),
          new Pair(new InheritedInputView(), new LegacyInheritedInputView()),
          new Pair(new InheritedOutputGetter(), new LegacyInheritedOutputGetter()),
          new Pair(new InheritedOutputView(), new LegacyInheritedOutputView()));
        for (var pair : pairs) {
            var recipe = recipe(List.of(ingredient("b")), List.of(new ItemStack("x")), 2);
            installRecipes(pair, entry("override", recipe));
            for (var machine : pair.machines()) {
                fill(machine, "a", 2, "b", 2);
                machine.inventory().heldStacks.set(6, new ItemStack("wrong", 2));
            }
            compareQueryAndOutput(pair, recipe, "Inherited covariant getter/list-view override");
        }
        scenario();
    }

    private static void unknownRecipesAndManagers() {
        var pair = pair();
        var newerRecipe = new RetainingRecipe();
        var olderRecipe = new RetainingRecipe();
        pair.newer.world().recipeManager.setRecipes(List.of(entry("retain", newerRecipe)));
        pair.older.world().recipeManager.setRecipes(List.of(entry("retain", olderRecipe)));
        for (var machine : pair.machines()) { fill(machine, "a", 3, "b", 3); machine.recipe(); machine.inventory().heldStacks.set(1, new ItemStack("next", 4)); machine.recipe(); }
        equal(pair.newer.state(), pair.older.state(), "Unknown recipe mutation alias semantics");
        check(newerRecipe.retained.get(0) != newerRecipe.retained.get(1), "Unknown recipe gets fresh inventories");
        equal(values(newerRecipe.retained.get(0)), values(olderRecipe.retained.get(0)), "Unknown retained first snapshot unchanged by next query");
        equal(values(newerRecipe.retained.get(1)), values(olderRecipe.retained.get(1)), "Unknown retained second snapshot equivalent");
        for (var machine : pair.machines()) {
            var manager = new RetainingManager(); machine.world().recipeManager = manager;
            manager.setRecipes(List.of(entry("plain", recipe(List.of(ingredient("a")), List.of(), 2))));
            fill(machine, "a", 2, "b", 2);
            machine.recipe(); machine.recipe();
            check(manager.retained.get(0) != manager.retained.get(1), "Custom manager retains ordinary fresh snapshots");
            check(manager.mapReads == 2, "Unknown manager receives no optimization map probe");
            check(!manager.retained.get(0).getStack(0).isEmpty(), "Custom manager's retained snapshot is not cleared");
        }
        scenario();
    }

    private static void reentrantAndThrowingIngredients() throws Exception {
        var runs = new ArrayList<List<String>>();
        for (var machine : List.of(new NewMachine(), new OldMachine())) {
            fill(machine, "a", 2, "b", 2);
            boolean[] nested = {false};
            var first = new Ingredient(stack -> {
                if (!nested[0]) {
                    nested[0] = true;
                    machine.inventory().heldStacks.set(2, new ItemStack("wrong", 1));
                    check(machine.recipe().isEmpty(), "Nested query gets independent refreshed snapshot");
                }
                return stack.getItem().equals("a");
            });
            machine.world().recipeManager.setRecipes(List.of(entry("nested", recipe(List.of(first, ingredient("b")), List.of(), 2))));
            check(machine.recipe().isPresent(), "Outer detached snapshot keeps its old second-slot reference during reentry");
            check(machine.world().recipeManager.firstMatchCalls == 2, "Reentrant lookup count unchanged");
            check(machine.slotReads() == 2, "Reentrant input query reads slots once per invocation");
            runs.add(machine.world().recipeManager.matchingTrace);
        }
        equal(runs.get(0), runs.get(1), "Reentrant recipe order equivalent");
        var pair = pair();
        for (var machine : pair.machines()) {
            fill(machine, "a", 4, "b", 2);
            boolean[] throwOnce = {true};
            var throwing = new Ingredient(stack -> { if (throwOnce[0]) { throwOnce[0] = false; stack.decrement(1); throw new IllegalStateException("ingredient"); } return true; });
            machine.world().recipeManager.setRecipes(List.of(entry("throw", recipe(List.of(throwing), List.of(), 2))));
            try { machine.recipe(); throw new AssertionError("Expected ingredient exception"); } catch (IllegalStateException expected) {}
            check(machine.recipe().isPresent(), "Query succeeds after ingredient exception");
        }
        equal(pair.newer.state(), pair.older.state(), "Throwing ingredient's aliased stack mutation equivalent");
        checkReleased(pair.newer, "inputSnapshot");
        scenario();
    }

    private static void reloadEligibilityAndWeakReferences() throws Exception {
        var pair = pair();
        installRecipes(pair, entry("original", recipe(List.of(ingredient("a")), List.of(), 2)));
        for (var machine : pair.machines()) { fill(machine, "a", 2, "b", 2); machine.recipe(); }
        var manager = pair.newer.world().recipeManager;
        var oldMap = manager.getAllOfType(RecipeContent.ASSEMBLER);
        var newerRecipe = new RetainingRecipe();
        var olderRecipe = new RetainingRecipe();
        manager.setRecipes(List.of(entry("custom-after-reload", newerRecipe)));
        pair.older.world().recipeManager.setRecipes(List.of(entry("custom-after-reload", olderRecipe)));
        check(oldMap != manager.getAllOfType(RecipeContent.ASSEMBLER), "setRecipes replaces type-map identity");
        pair.newer.recipe(); pair.older.recipe();
        check(!newerRecipe.retained.get(0).getStack(0).isEmpty(), "Known-to-unknown reload invalidates reuse before lookup");
        equal(values(newerRecipe.retained.get(0)), values(olderRecipe.retained.get(0)), "Retained snapshot after reload equivalent");
        installRecipes(pair, entry("replacement", recipe(List.of(ingredient("a")), List.of(), 5)));
        check(pair.newer.recipe().orElseThrow().id().value().equals("replacement"), "New recipe result observed without cached selection");
        equal(pair.newer.world().recipeManager.firstMatchCalls, pair.older.world().recipeManager.firstMatchCalls + 1, "Reload lookup calls stay one per invocation");
        var field = MachineBlockEntity.class.getDeclaredField("recipeSnapshotEligibility"); field.setAccessible(true);
        var eligibility = field.get(pair.newer);
        for (var name : List.of("manager", "entries")) {
            var accessor = eligibility.getClass().getDeclaredMethod(name); accessor.setAccessible(true);
            check(accessor.invoke(eligibility) instanceof WeakReference<?>, "Eligibility " + name + " reference is weak");
        }
        scenario();
    }

    private static void furnaceTimingAndLookupWorkload() {
        var newer = new NewFurnace(); var older = new OldFurnace();
        var entry = new RecipeEntry<>(new Identifier("smelt"), new SmeltingRecipe(ingredient("a"), new ItemStack("x"), 4));
        newer.getWorld().recipeManager.setRecipes(List.of(entry)); older.getWorld().recipeManager.setRecipes(List.of(entry));
        newer.inventory.heldStacks.set(0, new ItemStack("a", 3)); older.inventory.heldStacks.set(0, new ItemStack("a", 3));
        newer.energyStorage.amount = older.energyStorage.amount = 100;
        for (int i = 1; i <= 9; i++) {
            newer.runTick(); older.runTick();
            equal(newer.getProgress(), older.getProgress(), "Furnace displayed progress at tick " + i);
            equal(newer.state(), older.state(), "Furnace crafting/energy/inventory timing " + i);
            equal(newer.getWorld().recipeManager.firstMatchCalls, older.getWorld().recipeManager.firstMatchCalls, "Furnace GUI lookup count " + i);
        }
        var longEntry = new RecipeEntry<>(new Identifier("long-smelt"), new SmeltingRecipe(ingredient("a"), new ItemStack("x"), 100_000));
        newer.getWorld().recipeManager.setRecipes(List.of(longEntry)); older.getWorld().recipeManager.setRecipes(List.of(longEntry));
        newer.energyStorage.amount = older.energyStorage.amount = 100_000;
        clearAllocationCounters();
        for (int i = 0; i < 1_000; i++) { older.runTick(); older.getProgress(); }
        int oldCount = SimpleInventory.varargsConstructions, oldArrays = DefaultedList.toArrayCalls;
        clearAllocationCounters();
        for (int i = 0; i < 1_000; i++) { newer.runTick(); newer.getProgress(); }
        check(oldCount == 3_000 && oldArrays == 3_000, "Frozen furnace baseline does tick plus two GUI recipe queries");
        check(SimpleInventory.varargsConstructions == 0 && DefaultedList.toArrayCalls == 0, "Furnace tick and GUI reuse input snapshot");
        equal(newer.state(), older.state(), "Long furnace workload equivalent");
        equal(newer.getWorld().recipeManager.firstMatchCalls, older.getWorld().recipeManager.firstMatchCalls, "Long furnace workload preserves exact lookup count");
        System.out.println("Deterministic 1000 furnace ticks + progress reads: temporary inventories/toArray calls 3000 -> 0; lookup count/order preserved.");
        scenario();
    }

    private static void reentrantOutputAndThrowCleanup() throws Exception {
        var pair = pair();
        var ordinary = recipe(List.of(), List.of(new ItemStack("x")), 2);
        for (var machine : pair.machines()) {
            machine.inventory().heldStacks.set(4, new ItemStack("x", 2));
            boolean[] nested = {false};
            var reentrant = new OritechRecipe(2, List.of(), List.of(new ItemStack("x")), RecipeContent.ASSEMBLER, null, null) {
                public List<ItemStack> getResults() {
                    if (!nested[0]) { nested[0] = true; machine.inventory().heldStacks.set(4, new ItemStack("wrong", 2)); check(!machine.canOutput(ordinary), "Nested output query sees refreshed replacement"); }
                    return super.getResults();
                }
            };
            check(machine.canOutput(reentrant), "Outer output snapshot retains its old slot during reentry");
            var throwing = new OritechRecipe(2, List.of(), List.of(), RecipeContent.ASSEMBLER, null, null) {
                public List<ItemStack> getResults() { throw new IllegalStateException("output"); }
            };
            try { machine.canOutput(throwing); throw new AssertionError("Expected output exception"); } catch (IllegalStateException expected) {}
            check(!machine.canOutput(ordinary), "Output query succeeds after exception with latest source");
        }
        checkReleased(pair.newer, "outputSnapshot");
        scenario();
    }

    private static void lookupAndSlotReadOrder() {
        var newer = new OrderedNewMachine(); var older = new OrderedOldMachine();
        var pair = new Pair(newer, older);
        installRecipes(pair, entry("ordered", recipe(List.of(ingredient("a")), List.of(), 1)));
        for (var machine : pair.machines()) fill(machine, "a", 2, "b", 2);
        newer.recipe(); older.recipe();
        equal(newer.events, older.events, "Recipe manager, recipe type and slot lookup order preserved");
        equal(newer.events, List.of("manager", "type", "slots"), "Baseline manager-before-type ordering");
        scenario();
    }

    private static void compareQueryAndOutput(Pair pair, OritechRecipe recipe, String label) {
        equal(pair.newer.recipe().map(RecipeEntry::id), pair.older.recipe().map(RecipeEntry::id), label + " recipe");
        equal(pair.newer.canOutput(recipe), pair.older.canOutput(recipe), label + " output");
    }
    private static void checkReleased(NewMachine machine, String name) throws Exception {
        var field = MachineBlockEntity.class.getDeclaredField(name); field.setAccessible(true);
        var holder = (MachineInventorySnapshot) field.get(machine);
        var snapshot = MachineInventorySnapshot.class.getDeclaredField("snapshot"); snapshot.setAccessible(true);
        var inventory = (SimpleInventory) snapshot.get(holder);
        check(inventory == null || inventory.isEmpty(), name + " releases all stack references");
        var borrowed = MachineInventorySnapshot.class.getDeclaredField("borrowed"); borrowed.setAccessible(true);
        check(!(boolean) borrowed.get(holder), name + " releases borrow state");
    }
    private static void tickBoth(Pair pair) { pair.newer.tick(); pair.older.tick(); }
    private static Pair pair() { return new Pair(new NewMachine(), new OldMachine()); }
    private static void fill(Machine machine, String first, int firstCount, String second, int secondCount) { var slots = machine.slots(); machine.inventory().heldStacks.set(slots.inputStart(), new ItemStack(first, firstCount)); machine.inventory().heldStacks.set(slots.inputStart() + 1, new ItemStack(second, secondCount)); }
    private static Ingredient ingredient(String item) { return new Ingredient(stack -> !stack.isEmpty() && stack.getItem().equals(item)); }
    private static OritechRecipe recipe(List<Ingredient> inputs, List<ItemStack> results, int time) { return new OritechRecipe(time, inputs, results, RecipeContent.ASSEMBLER, null, null); }
    private static RecipeEntry<OritechRecipe> entry(String id, OritechRecipe recipe) { return new RecipeEntry<>(new Identifier(id), recipe); }
    private static void installRecipes(Pair pair, RecipeEntry<?>... entries) { for (var machine : pair.machines()) machine.world().recipeManager.setRecipes(Arrays.asList(entries)); }
    private static List<String> values(Inventory inventory) { var values = new ArrayList<String>(); for (int i = 0; i < inventory.size(); i++) values.add(inventory.getStack(i).toString()); return values; }
    private static void clearAllocationCounters() { SimpleInventory.sizedConstructions = SimpleInventory.varargsConstructions = DefaultedList.toArrayCalls = DefaultedList.subListCalls = 0; }
    private static void equal(Object actual, Object expected, String label) { check(Objects.equals(actual, expected), label + ": " + actual + " vs " + expected); }
    private static void check(boolean condition, String label) { assertions++; if (!condition) throw new AssertionError(label); }
    private static void scenario() { scenarios++; }
    private static boolean output(Object machine, Class<?> owner, OritechRecipe recipe) {
        try { var method = owner.getDeclaredMethod("canOutputRecipe", OritechRecipe.class); method.setAccessible(true); return (boolean) method.invoke(machine, recipe); }
        catch (InvocationTargetException failure) { if (failure.getCause() instanceof RuntimeException cause) throw cause; throw new AssertionError(failure.getCause()); }
        catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private record Pair(NewMachine newer, OldMachine older) { List<Machine> machines() { return List.of(newer, older); } }
    private interface Machine {
        SimpleInventory inventory(); World world(); Inventory input(); Inventory output(); Optional<RecipeEntry<OritechRecipe>> recipe();
        InventorySlotAssignment slots(); void slots(InventorySlotAssignment slots); void energy(long amount); void mode(InventoryInputMode mode);
        int progressValue(); int slotReads(); void tick(); boolean canOutput(OritechRecipe recipe); String state(); void write(NbtCompound nbt); void read(NbtCompound nbt);
    }
    private static class NewMachine extends MachineBlockEntity implements Machine {
        private InventorySlotAssignment assignment = new InventorySlotAssignment(1, 2, 4, 2);
        private int slotReads;
        NewMachine() { super(new BlockEntityType<>(), BlockPos.ORIGIN, new BlockState(), 2); setWorld(new World(false)); }
        public SimpleInventory inventory() { return inventory; } public World world() { return getWorld(); }
        public Inventory input() { return getInputInventory(); } public Inventory output() { return getOutputInventory(); }
        public Optional<RecipeEntry<OritechRecipe>> recipe() { return getRecipe(); }
        public InventorySlotAssignment getSlots() { slotReads++; return assignment; } public InventorySlotAssignment slots() { return assignment; }
        public int slotReads() { return slotReads; }
        public void slots(InventorySlotAssignment slots) { assignment = slots; } public void energy(long amount) { energyStorage.amount = amount; }
        public void mode(InventoryInputMode mode) { setInventoryInputMode(mode); } public int progressValue() { return progress; }
        public void tick() { tick(world, pos, getCachedState(), this); world.time++; }
        public boolean canOutput(OritechRecipe recipe) { return MachineInventoryVerification.output(this, MachineBlockEntity.class, recipe); }
        public String state() { return values(inventory) + "/" + progress + "/" + energyStorage.amount + "/" + currentRecipe.getTime(); }
        public void write(NbtCompound nbt) { writeNbt(nbt); } public void read(NbtCompound nbt) { readNbt(nbt); }
        public int getInventorySize() { return 8; } protected OritechRecipeType getOwnRecipeType() { return RecipeContent.ASSEMBLER; }
        public List<GuiSlot> getGuiSlots() { return List.of(); }
    }
    private static class OldMachine extends LegacyMachineBlockEntity implements Machine {
        private InventorySlotAssignment assignment = new InventorySlotAssignment(1, 2, 4, 2);
        private int slotReads;
        OldMachine() { super(new BlockEntityType<>(), BlockPos.ORIGIN, new BlockState(), 2); setWorld(new World(false)); }
        public SimpleInventory inventory() { return inventory; } public World world() { return getWorld(); }
        public Inventory input() { return getInputInventory(); } public Inventory output() { return getOutputInventory(); }
        public Optional<RecipeEntry<OritechRecipe>> recipe() { return getRecipe(); }
        public InventorySlotAssignment getSlots() { slotReads++; return assignment; } public InventorySlotAssignment slots() { return assignment; }
        public int slotReads() { return slotReads; }
        public void slots(InventorySlotAssignment slots) { assignment = slots; } public void energy(long amount) { energyStorage.amount = amount; }
        public void mode(InventoryInputMode mode) { setInventoryInputMode(mode); } public int progressValue() { return progress; }
        public void tick() { tick(world, pos, getCachedState(), this); world.time++; }
        public boolean canOutput(OritechRecipe recipe) { return MachineInventoryVerification.output(this, LegacyMachineBlockEntity.class, recipe); }
        public String state() { return values(inventory) + "/" + progress + "/" + energyStorage.amount + "/" + currentRecipe.getTime(); }
        public void write(NbtCompound nbt) { writeNbt(nbt); } public void read(NbtCompound nbt) { readNbt(nbt); }
        public int getInventorySize() { return 8; } protected OritechRecipeType getOwnRecipeType() { return RecipeContent.ASSEMBLER; }
        public List<GuiSlot> getGuiSlots() { return List.of(); }
    }
    private static class InputGetter extends NewMachine { protected SimpleInventory getInputInventory() { return new SimpleInventory(inventory.getStack(2)); } }
    private static class LegacyInputGetter extends OldMachine { protected SimpleInventory getInputInventory() { return new SimpleInventory(inventory.getStack(2)); } }
    private static final class InheritedInputGetter extends InputGetter {}
    private static final class LegacyInheritedInputGetter extends LegacyInputGetter {}
    private static class InputView extends NewMachine { protected List<ItemStack> getInputView() { return inventory.heldStacks.subList(2, 3); } }
    private static class LegacyInputView extends OldMachine { protected List<ItemStack> getInputView() { return inventory.heldStacks.subList(2, 3); } }
    private static final class InheritedInputView extends InputView {}
    private static final class LegacyInheritedInputView extends LegacyInputView {}
    private static class OutputGetter extends NewMachine { protected SimpleInventory getOutputInventory() { return new SimpleInventory(inventory.getStack(6)); } }
    private static class LegacyOutputGetter extends OldMachine { protected SimpleInventory getOutputInventory() { return new SimpleInventory(inventory.getStack(6)); } }
    private static final class InheritedOutputGetter extends OutputGetter {}
    private static final class LegacyInheritedOutputGetter extends LegacyOutputGetter {}
    private static class OutputView extends NewMachine { protected List<ItemStack> getOutputView() { return inventory.heldStacks.subList(6, 7); } }
    private static class LegacyOutputView extends OldMachine { protected List<ItemStack> getOutputView() { return inventory.heldStacks.subList(6, 7); } }
    private static final class InheritedOutputView extends OutputView {}
    private static final class LegacyInheritedOutputView extends LegacyOutputView {}
    private static final class RetainingRecipe extends OritechRecipe {
        final List<Inventory> retained = new ArrayList<>();
        RetainingRecipe() { super(2, List.of(), List.of(), RecipeContent.ASSEMBLER, null, null); }
        public boolean matches(Inventory inventory, World world) { retained.add(inventory); inventory.getStack(1).decrement(1); ((SimpleInventory) inventory).setStack(0, new ItemStack("private-slot", 2)); return true; }
    }
    private static final class RetainingManager extends RecipeManager {
        final List<Inventory> retained = new ArrayList<>();
        public <C extends Inventory, T extends Recipe<C>> Optional<RecipeEntry<T>> getFirstMatch(RecipeType<T> type, C inventory, World world) { retained.add(inventory); return super.getFirstMatch(type, inventory, world); }
    }
    private static final class NewFurnace extends PoweredFurnaceBlockEntity {
        NewFurnace() { super(BlockPos.ORIGIN, new BlockState()); setWorld(new World(false)); }
        void runTick() { tick(world, pos, getCachedState(), this); world.time++; }
        String state() { return values(inventory) + "/" + progress + "/" + energyStorage.amount; }
    }
    private static final class OldFurnace extends LegacyPoweredFurnaceBlockEntity {
        OldFurnace() { super(BlockPos.ORIGIN, new BlockState()); setWorld(new World(false)); }
        void runTick() { tick(world, pos, getCachedState(), this); world.time++; }
        String state() { return values(inventory) + "/" + progress + "/" + energyStorage.amount; }
    }
    private static final class OrderedNewMachine extends NewMachine {
        final List<String> events = new ArrayList<>();
        OrderedNewMachine() { setWorld(new World(false) { public RecipeManager getRecipeManager() { events.add("manager"); return super.getRecipeManager(); } }); }
        protected OritechRecipeType getOwnRecipeType() { events.add("type"); return super.getOwnRecipeType(); }
        public InventorySlotAssignment getSlots() { events.add("slots"); return super.getSlots(); }
    }
    private static final class OrderedOldMachine extends OldMachine {
        final List<String> events = new ArrayList<>();
        OrderedOldMachine() { setWorld(new World(false) { public RecipeManager getRecipeManager() { events.add("manager"); return super.getRecipeManager(); } }); }
        protected OritechRecipeType getOwnRecipeType() { events.add("type"); return super.getOwnRecipeType(); }
        public InventorySlotAssignment getSlots() { events.add("slots"); return super.getSlots(); }
    }
}
