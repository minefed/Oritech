package rearth.oritech.verification;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rearth.oritech.util.LaserTargetSearch;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class LaserTargetSearchVerification {
    private static int checks;

    public static void main(String[] args) {
        Random random = new Random(0x5ca11a5eL);
        for (int scenario = 0; scenario < 500; scenario++) {
            BlockPos origin = new BlockPos(random.nextInt(400) - 200, random.nextInt(400) - 100, random.nextInt(400) - 200);
            BlockPos center = origin.add(random.nextInt(100) - 50, random.nextInt(100) - 50, random.nextInt(100) - 50);
            int radius = random.nextInt(10);
            int seed = random.nextInt();
            Predicate<BlockPos> blocks = pos -> Math.floorMod(mix(pos, seed), 7) < 3;
            checkArea(origin, center, radius, blocks);
        }
        for (int radius : new int[] {0, 1, 2, 8, 16}) {
            checkArea(BlockPos.ORIGIN, new BlockPos(-16, -64, 16), radius, pos -> false);
            checkArea(BlockPos.ORIGIN, BlockPos.ORIGIN, radius, pos -> true);
        }
        // Equal-distance ties, asymmetric cube bounds, negative coordinates, and world borders.
        checkArea(BlockPos.ORIGIN, BlockPos.ORIGIN, 4, pos -> false);
        checkArea(new BlockPos(29_999_950, 320, -29_999_950), new BlockPos(29_999_980, 319, -29_999_980), 8,
                  pos -> (pos.getX() & 1) == 0);

        for (int scenario = 0; scenario < 2_000; scenario++) {
            Vec3d from = new Vec3d(random.nextDouble() * 100 - 50, random.nextDouble() * 100 - 50, random.nextDouble() * 100 - 50);
            Vec3d direction = new Vec3d(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1).normalize();
            int seed = random.nextInt();
            checkRay(from, direction, random.nextInt(129), pos -> Math.floorMod(mix(pos, seed), 97) != 0);
        }
        for (Vec3d from : List.of(Vec3d.ZERO, new Vec3d(-16.000001, -64, 15.999999), new Vec3d(0.45, 0.3f, -0.45))) {
            for (Vec3d direction : List.of(Vec3d.ZERO, new Vec3d(1, 0, 0), new Vec3d(0, -1, 0), new Vec3d(0, 0, -1))) {
                checkRay(from, direction, 128, pos -> true);
                checkRay(from, direction, 128, pos -> false);
                checkRay(from, direction, 0, pos -> false);
            }
        }
        // A fresh call must observe edits, chunk reloads or tag changes immediately.
        Set<BlockPos> solid = new HashSet<>();
        Vec3d from = new Vec3d(0.5, 64.5, 0.5);
        Vec3d direction = new Vec3d(1, 0, 0);
        checkRay(from, direction, 128, pos -> !solid.contains(pos));

        checkChunkLoadMutation();
        solid.add(new BlockPos(6, 64, 0));
        checkRay(from, direction, 128, pos -> !solid.contains(pos));
        solid.clear();
        solid.add(new BlockPos(2, 64, 0));
        checkRay(from, direction, 128, pos -> !solid.contains(pos));

        AtomicInteger oldReads = new AtomicInteger();
        AtomicInteger newReads = new AtomicInteger();
        new LegacyLaserSearch(BlockPos.ORIGIN, pos -> { oldReads.incrementAndGet(); return true; }).ray(from, direction, 128);
        LaserTargetSearch.raycast(from, direction, 128, pos -> { newReads.incrementAndGet(); return true; });
        if (newReads.get() >= oldReads.get() / 2) throw new AssertionError("Expected more than 50% fewer ray reads");
        System.out.printf(Locale.ROOT, "Laser checks=%d; empty 128-block axial ray reads: %d -> %d (%.1f%% fewer)%n",
                          checks, oldReads.get(), newReads.get(), 100.0 * (oldReads.get() - newReads.get()) / oldReads.get());
        System.out.println("Area scans retain all (2r)^3 reads; stable distance ordering uses three linear distance passes without a distance buffer.");
        benchmarkArea("dense", pos -> false);
        benchmarkArea("sparse", pos -> Math.floorMod(mix(pos, 347), 10) != 0);
    }

    private static void checkArea(BlockPos origin, BlockPos center, int radius, Predicate<BlockPos> passable) {
        List<BlockPos> oldReads = new ArrayList<>();
        List<BlockPos> newReads = new ArrayList<>();
        var expected = new LegacyLaserSearch(origin, pos -> { oldReads.add(pos.toImmutable()); return passable.test(pos); }).area(center, radius);
        var actual = LaserTargetSearch.collectArea(center, radius, origin, pos -> { newReads.add(pos.toImmutable()); return passable.test(pos); });
        equal(new ArrayList<>(expected), new ArrayList<>(actual), "complete ordered area targets");
        equal(oldReads, newReads, "area world lookup order");
        if (actual.stream().anyMatch(pos -> pos instanceof BlockPos.Mutable)) throw new AssertionError("Mutable queued target");
    }

    private static void checkRay(Vec3d from, Vec3d direction, int range, Predicate<BlockPos> passable) {
        List<BlockPos> oldReads = new ArrayList<>();
        List<BlockPos> newReads = new ArrayList<>();
        var expected = new LegacyLaserSearch(BlockPos.ORIGIN, pos -> { oldReads.add(pos); return passable.test(pos); }).ray(from, direction, range);
        var actual = LaserTargetSearch.raycast(from, direction, range, pos -> { newReads.add(pos); return passable.test(pos); });
        equal(expected, actual, "first hit");
        equal(new ArrayList<>(new LinkedHashSet<>(oldReads)), newReads, "first-occurrence probe order");
    }

    private static int mix(BlockPos pos, int seed) {
        return Integer.rotateLeft(pos.getX() * 73856093 ^ seed, 13) ^ pos.getY() * 19349663 ^ pos.getZ() * 83492791;
    }

    private static void checkChunkLoadMutation() {
        // A probe first enters chunk X=1; its load changes the already-seen block at X=15.
        // Subsequent offsets in the same ray step revisit X=15 and must see the new solid block.
        class LoadingWorld {
            boolean loaded;
            boolean read(BlockPos pos) {
                if (pos.getX() >= 16) loaded = true;
                return !loaded || !pos.equals(new BlockPos(15, 64, 0));
            }
            boolean mayLoad(BlockPos pos) { return pos.getX() >= 16 && !loaded; }
        }
        var oldWorld = new LoadingWorld();
        var newWorld = new LoadingWorld();
        var from = new Vec3d(15.5, 64.5, 0.5);
        var direction = new Vec3d(1, 0, 0);
        BlockPos expected = new LegacyLaserSearch(BlockPos.ORIGIN, oldWorld::read).ray(from, direction, 8);
        BlockPos actual = LaserTargetSearch.raycast(from, direction, 8, newWorld::read, newWorld::mayLoad);
        equal(new BlockPos(15, 64, 0), expected, "baseline revisits changed block after load");
        equal(expected, actual, "reentrant chunk-load mutation");
    }

    private static volatile int benchmarkSink;

    private static void benchmarkArea(String name, Predicate<BlockPos> passable) {
        BlockPos center = new BlockPos(40, 64, 30);
        var legacy = new LegacyLaserSearch(BlockPos.ORIGIN, passable);
        for (int warmup = 0; warmup < 40; warmup++) {
            benchmarkSink = legacy.area(center, 16).size();
            benchmarkSink = LaserTargetSearch.collectArea(center, 16, BlockPos.ORIGIN, passable).size();
        }
        long[] oldNanos = new long[15];
        long[] newNanos = new long[15];
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        boolean allocations = bean.isThreadAllocatedMemorySupported();
        if (allocations) bean.setThreadAllocatedMemoryEnabled(true);
        long oldAllocated = 0;
        long newAllocated = 0;
        long thread = Thread.currentThread().getId();
        for (int run = 0; run < oldNanos.length; run++) {
            long bytes = allocations ? bean.getThreadAllocatedBytes(thread) : 0;
            long start = System.nanoTime();
            benchmarkSink = legacy.area(center, 16).size();
            oldNanos[run] = System.nanoTime() - start;
            oldAllocated += allocations ? bean.getThreadAllocatedBytes(thread) - bytes : 0;
            bytes = allocations ? bean.getThreadAllocatedBytes(thread) : 0;
            start = System.nanoTime();
            benchmarkSink = LaserTargetSearch.collectArea(center, 16, BlockPos.ORIGIN, passable).size();
            newNanos[run] = System.nanoTime() - start;
            newAllocated += allocations ? bean.getThreadAllocatedBytes(thread) - bytes : 0;
        }
        Arrays.sort(oldNanos);
        Arrays.sort(newNanos);
        System.out.printf(Locale.ROOT,
          "Area microbenchmark %s radius=16 cells=32768 targets=%d: median %.3f -> %.3f ms; allocated bytes/call %d -> %d (supported=%s)%n",
          name, benchmarkSink, oldNanos[7] / 1e6, newNanos[7] / 1e6, oldAllocated / 15, newAllocated / 15, allocations);
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": " + expected + " != " + actual);
    }
}
