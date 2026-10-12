package net.minecraft.block;
public class BlockState {
    @SuppressWarnings("unchecked") public <T> T get(Object property) { return (T) (property == net.minecraft.state.property.Properties.HORIZONTAL_FACING ? net.minecraft.util.math.Direction.NORTH : Boolean.TRUE); }
}
