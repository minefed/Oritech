# Laser search verification

`verifyLaserTargetSearch` executes the production `LaserTargetSearch` against real
Minecraft 1.20.4 `BlockPos`/`Vec3d` classes. `LegacyLaserSearch` keeps the original
two search methods verbatim from integration commit
`a3051c0f1ac1eaab0d995f8d1540e49561bc2ac1`; its small boolean world adapter supplies
the same read-only block predicate to both implementations.

Seeded randomized cases compare the complete ordered area queue, XYZ world-read
order, first ray hit, and first-occurrence ray probe order. Edge cases include
negative positions, world borders, exact voxel boundaries, equal-distance ties,
zero range/direction, empty/solid regions, edits between calls, and an unloaded
chunk whose load changes an already-seen block. Counts report block-state reads;
the production ray also makes a nonblocking ready-chunk lookup before each new
read. These are not end-to-end server MSPT measurements.

Searches stay synchronous and preserve the tick at which targets are selected.
The ray's pass-through cache lives for one call only and is cleared before a
potentially blocking chunk read can process queued server work. Area scans still read every
cell; stable counting sort replaces sorting comparisons with three linear distance passes and
only retained targets get immutable position allocations. Arbitrarily changing
the world from a block-state getter is outside the read-only search contract.

Run `gradlew.bat verifyLaserTargetSearch remapJar` with Java 17 (or a compatible
Gradle JVM; all production bytecode is compiled with `--release 17`).
