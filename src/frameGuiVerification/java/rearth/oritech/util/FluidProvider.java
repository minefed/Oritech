package rearth.oritech.util;
import java.util.*;
import net.minecraft.util.math.Direction;
public interface FluidProvider { Object getForDirectFluidAccess(); default Iterable<Object> getFluidStorage(Direction direction) { return List.of(); } }
