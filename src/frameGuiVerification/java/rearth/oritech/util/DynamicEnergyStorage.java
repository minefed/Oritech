package rearth.oritech.util;
import team.reborn.energy.api.EnergyStorage;
public class DynamicEnergyStorage implements EnergyStorage {
    public long amount, capacity, maxInsert, maxExtract;
    public DynamicEnergyStorage(long capacity, long insert, long extract) { this.capacity = capacity; maxInsert = insert; maxExtract = extract; }
    public void onFinalCommit() {}
}
