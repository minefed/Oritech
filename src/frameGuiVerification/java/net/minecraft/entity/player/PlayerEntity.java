package net.minecraft.entity.player;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
public class PlayerEntity {
    public ScreenHandler currentScreenHandler;
    public World world;
    public double x = 1, y = 2, z = 3;
    public World getWorld() { return world; }
}
