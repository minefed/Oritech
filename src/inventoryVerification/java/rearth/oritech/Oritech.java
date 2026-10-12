package rearth.oritech;
public class Oritech {
    public static final Config CONFIG = new Config();
    public static class Config { public Processing processingMachines = new Processing(); }
    public static class Processing { public Furnace furnaceData = new Furnace(); }
    public static class Furnace {
        public float speedMultiplier() { return 1; } public int energyPerTick() { return 2; }
        public long energyCapacity() { return 5000; } public long maxEnergyInsertion() { return 1024; }
    }
}
