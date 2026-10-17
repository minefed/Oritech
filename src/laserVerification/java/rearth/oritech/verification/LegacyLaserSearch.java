package rearth.oritech.verification;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.function.Predicate;

/** Search methods copied verbatim from integration commit a3051c0f1ac1eaab0d995f8d1540e49561bc2ac1. */
final class LegacyLaserSearch {
    private final BlockPos pos;
    private final ReadOnlyWorld world;
    LegacyLaserSearch(BlockPos pos, Predicate<BlockPos> predicate) {
        this.pos = pos;
        this.world = new ReadOnlyWorld(predicate);
    }
    ArrayDeque<BlockPos> area(BlockPos center, int radius) { return findNextAreaBlockTarget(center, radius); }
    BlockPos ray(Vec3d from, Vec3d direction, int range) { return basicRaycast(from, direction, range); }
    private boolean canPassThrough(boolean state) { return state; }
    private record ReadOnlyWorld(Predicate<BlockPos> predicate) {
        boolean getBlockState(BlockPos position) { return predicate.test(position); }
    }

    // returns the first block in an X*X*X cube, from the outside in
    private ArrayDeque<BlockPos> findNextAreaBlockTarget(BlockPos center, int scanDist) {
        
        var targets = new ArrayList<BlockPos>();
        
        for (int x = -scanDist; x < scanDist; x++) {
            for (int y = -scanDist; y < scanDist; y++) {
                for (int z = -scanDist; z < scanDist; z++) {
                    var pos = center.add(x, y, z);
                    if (!canPassThrough(world.getBlockState(pos)) && !center.equals(pos))
                        targets.add(pos);
                }
            }
        }
        
        targets.sort(Comparator.comparingInt(pos::getManhattanDistance));
        return new ArrayDeque<>(targets);
    }
    
    private BlockPos basicRaycast(Vec3d from, Vec3d direction, int range) {
        
        var searchOffset = 0.45;
        
        for (float i = 0; i < range; i += 0.3f) {
            var to = from.add(direction.multiply(i));
            var targetBlockPos = BlockPos.ofFloored(to.add(0, 0.3f, 0));
            var targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetTop = to.add(0, -searchOffset, 0);
            targetBlockPos = BlockPos.ofFloored(offsetTop);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetLeft = to.add(-searchOffset, 0, 0);
            targetBlockPos = BlockPos.ofFloored(offsetLeft);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetRight = to.add(searchOffset, 0, 0);
            targetBlockPos = BlockPos.ofFloored(offsetRight);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetFront = to.add(0, 0, searchOffset);
            targetBlockPos = BlockPos.ofFloored(offsetFront);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetBack = to.add(0, 0, -searchOffset);
            targetBlockPos = BlockPos.ofFloored(offsetBack);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
        }
        
        return null;
    }
    
}
