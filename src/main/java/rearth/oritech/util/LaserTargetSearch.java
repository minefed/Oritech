package rearth.oritech.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

/** Synchronous searches; no world state or results are retained between calls. */
public final class LaserTargetSearch {
    private static final double[][] RAY_OFFSETS = {
        {0, 0.3f, 0}, {0, -0.45, 0}, {-0.45, 0, 0},
        {0.45, 0, 0}, {0, 0, 0.45}, {0, 0, -0.45}
    };

    private LaserTargetSearch() {}

    /** The predicate must only read world state and must not retain the mutable position. */
    public static ArrayDeque<BlockPos> collectArea(BlockPos center, int radius, BlockPos origin,
                                                   Predicate<BlockPos> canPassThrough) {
        var targets = new ArrayList<BlockPos>();
        var cursor = new BlockPos.Mutable();
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        // Preserve the original XYZ lookup order, including the lookup of the excluded center.
        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z < radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!canPassThrough.test(cursor) && !center.equals(cursor)) {
                        targets.add(cursor.toImmutable());
                        int distance = origin.getManhattanDistance(cursor);
                        minimum = Math.min(minimum, distance);
                        maximum = Math.max(maximum, distance);
                    }
                }
            }
        }
        if (targets.isEmpty()) return new ArrayDeque<>();
        long span = (long) maximum - minimum + 1;
        // Very sparse, unusually large configured areas should not allocate a huge bucket table.
        if (span > 4096 && span > 2L * targets.size()) {
            targets.sort(Comparator.comparingInt(origin::getManhattanDistance));
            return new ArrayDeque<>(targets);
        }
        int[] offsets = new int[(int) span];
        for (BlockPos target : targets) offsets[origin.getManhattanDistance(target) - minimum]++;
        int total = 0;
        for (int i = 0; i < offsets.length; i++) {
            int count = offsets[i];
            offsets[i] = total;
            total += count;
        }
        BlockPos[] ordered = new BlockPos[targets.size()];
        // Stable counting sort preserves the original XYZ ordering for equal distances.
        for (int i = 0; i < targets.size(); i++) {
            BlockPos target = targets.get(i);
            ordered[offsets[origin.getManhattanDistance(target) - minimum]++] = target;
        }
        var result = new ArrayDeque<BlockPos>(ordered.length);
        for (BlockPos target : ordered) result.addLast(target);
        return result;
    }

    /** Same floating-point steps, probe ordering and first hit as the original six-probe ray. */
    public static BlockPos raycast(Vec3d from, Vec3d direction, int range,
                                   Predicate<BlockPos> canPassThrough) {
        return raycast(from, direction, range, canPassThrough, target -> false);
    }

    /** Discard previous reads before a chunk load can run queued server work and change blocks. */
    public static BlockPos raycast(Vec3d from, Vec3d direction, int range,
                                   Predicate<BlockPos> canPassThrough, Predicate<BlockPos> mayLoadChunk) {
        var passable = new LongOpenHashSet();
        for (float distance = 0; distance < range; distance += 0.3f) {
            var to = from.add(direction.multiply(distance));
            for (double[] offset : RAY_OFFSETS) {
                var target = BlockPos.ofFloored(to.add(offset[0], offset[1], offset[2]));
                long key = target.asLong();
                if (passable.contains(key)) continue;
                if (mayLoadChunk.test(target)) passable.clear();
                if (!canPassThrough.test(target)) return target;
                passable.add(key);
            }
        }
        return null;
    }
}
