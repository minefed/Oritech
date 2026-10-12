package rearth.oritech;
public class Oritech {
    public static final Config CONFIG = new Config();
    public static class Config { public PlacerConfig placerConfig = new PlacerConfig(); }
    public static class PlacerConfig {
        public int moveDuration() { return 10; } public int workDuration() { return 20; }
        public int moveEnergyUsage() { return 1; } public int workEnergyUsage() { return 1; }
    }
}
