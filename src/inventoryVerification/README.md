# Machine inventory snapshot verification

`gradlew verifyMachineInventorySnapshots` compiles and executes the production
`MachineBlockEntity`, `PoweredFurnaceBlockEntity`, `MachineInventorySnapshot`,
`OritechRecipe` and slot-assignment sources alongside frozen baseline machine
and furnace sources. Only class names are changed in the generated legacy Java
files so both versions can execute in the same process. Minecraft, GeckoLib,
networking and unrelated machine systems use small platform doubles; the
production recipe lookup, output blocking, crafting and furnace methods execute
unchanged in the comparison.

The baseline is integration commit
`a6c9c79ecf58430bdc1ead54b9d03aadd22302fc`. The `.java.txt` copies have the exact
same Git blob IDs as that commit:

The archival files preserve original trailing whitespace and use LF checkouts;
their narrow Git attribute rule keeps byte-exact comparisons reproducible.

| File | Git blob | SHA-256 |
| --- | --- | --- |
| MachineBlockEntity.java | `6277898cb37bb86cfbfc6b3eacb8d80b9528129b` | `a535c0070ed5023f3d5fe38b01490e2214bb8417727aca472b8215db4b99454c` |
| PoweredFurnaceBlockEntity.java | `978ab36cb53c154f14b9c1d74d38a416d14b7ac9` | `e475cb4b0853cbcb39ba91c255fdb2a71a43151864d372c1ab5487bd0c3350f2` |

The protected snapshot getters still return separate `SimpleInventory` objects.
Their slot lists are detached, while their ItemStack objects alias the machine's
stacks. An old inventory keeps its old slot references after another lookup or
source-slot replacement. Snapshot slot writes do not replace machine slots;
in-place stack count changes still affect aliases. The fixture checks these
contracts explicitly, including counts above 64. The reuse helper writes directly
to its own `heldStacks` rather than using `setStack`, which would clamp counts.

Internal synchronous recipe lookups borrow a detached snapshot only when the
manager has exactly the vanilla `RecipeManager` class and every recipe of the
requested type has exactly the known `OritechRecipe` or `SmeltingRecipe` class.
Custom recipes, custom manager subclasses, inherited snapshot getter/list-view
overrides (including covariant bridge methods) and nested queries use the original
fresh snapshot path. The private output checker borrows its separate output
snapshot. A `finally` block clears all retained ItemStack references and releases
the borrow even when a callback throws. Snapshot size changes recreate only that
side's container; changes to slot start or stack references refresh it on each
borrow. No recipe result or matching selection is cached.

The existing private Minecraft 1.20.4 method
`RecipeManager.getAllOfType(RecipeType): Map` is made accessible solely for a
read-only map identity check. The actual resolved Minecraft bytecode (`javap -p
-c`) confirms:

- `getFirstMatch` iterates the type map values, filters by `recipe.matches`, and
  returns the first result without retaining the inventory.
- `apply` and `setRecipes` replace the immutable recipe maps. A changed type-map
  identity invalidates eligibility even if the manager object is unchanged.
- `AbstractCookingRecipe.matches` passes only slot zero's stack to its ingredient.
- `SimpleInventory(ItemStack...)` preserves stack references, while `setStack`
  would clamp counts above the inventory's maximum.

Eligibility records hold weak references to both manager and type map, preventing
an idle machine from retaining a retired recipe graph. The exact method descriptor
is checked by compiling and remapping against Minecraft 1.20.4 / Yarn
1.20.4+build.3; the loader access widener is packaged in the runtime JAR.

The comparison covers retained inventories and stack aliases; first-match and
manager/type/slot lookup order; ordinary, empty and changing slot ranges;
multiple outputs and exact crafting ticks; mismatched/full output blocking;
energy pauses and progress reset; sided insertion/extraction permissions and slot
order for every input mode; removals, in-place count changes and NBT replacement;
inherited getter/view overrides; retaining or mutating unknown recipe/manager
callbacks; input/output reentry; exceptions and subsequent recovery; known to
unknown recipe reloads; weak eligibility references; and furnace GUI lookup count,
progress and output timing.

Allocation counters are platform-double measurements of constructor invocations
and `toArray` calls, not a profiler measurement of live heap or raw array count.
For 10,000 input lookups plus 10,000 output checks, the frozen baseline performs
20,000 temporary varargs `SimpleInventory` constructions and 20,000 `toArray`
calls; the new safe paths perform zero of either, with one lazily constructed
output snapshot after input warmup. For 1,000 furnace ticks plus progress reads,
the baseline makes 3,000 temporary constructions/`toArray` calls and the new path
makes zero, preserving three recipe manager lookups per iteration.

The test does not execute real Fabric transfer transactions, a running game,
native rendering, or a live server. Sided hopper/pipe decisions execute the actual
machine inventory methods; the platform doubles supply stack/NBT operations.
CPU time, live RAM/GC, TX/RX and GPU/VRAM were not measured. The change adds a small
bounded per-machine snapshot cache to remove repeated short-lived allocations;
custom extension paths intentionally retain their existing allocations.
Fixtures and legacy classes are excluded from the runtime JAR. Source and binary
license remain CC0-1.0 with the existing license notice preserved.

Validated on 2026-10-12 with Java 17, Gradle 8.7, one worker and a 1 GiB Gradle heap:

```text
gradlew.bat :verifyMachineInventorySnapshots :verifyFrameGuiSync :remapJar --no-daemon --max-workers=1 '-Dorg.gradle.parallel=false' '-Dorg.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8'
Machine inventory snapshots: 12 scenarios, 433 assertions passed
Frame GUI sync: 13 scenarios, 96 assertions passed
BUILD SUCCESSFUL
```
