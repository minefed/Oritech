package rearth.oritech.verification;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.block.entity.machines.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.machines.interaction.PlacerBlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.InventoryProxyScreenHandler;
import rearth.oritech.network.NetworkContent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/** Runs the production tick, opening-data and menu-identity methods, not copies of them. */
public final class FrameGuiSyncVerification {
    private static int assertions;
    private static int scenarios;

    public static void main(String[] args) throws Exception {
        unchangedStateAndNonViewers();
        independentViewers();
        everyStateField();
        reopenAndChangeMenu();
        trackingAndConnectionChanges();
        dimensionChanges();
        entityReplacementAndRemoval();
        proxyOpeningAndUpdates();
        staleProxyController();
        clientWorld();
        failedSendRetries();
        postSimulationSnapshot();
        legacyViewContractAndPlacerInventory();
        System.out.println("Frame GUI sync: " + scenarios + " scenarios, " + assertions + " assertions passed");
        System.out.println("Deterministic fixture: 10 trackers, 1 viewer, 20 idle ticks: 200 legacy deliveries -> 1 opening snapshot;"
          + " 20 changed ticks: 200 legacy deliveries -> 20 viewer deliveries (not measured network traffic).");
    }

    private static void unchangedStateAndNonViewers() throws Exception {
        var f = fixture();
        var viewer = f.player();
        for (int i = 0; i < 9; i++) f.player();
        var opening = f.open(viewer, 4);
        check(opening.values.equals(List.of(f.machine.getPos(), List.of(), 1f)), "Direct opening data unchanged");
        check(deliveries() == 1, "Immediate opening snapshot");
        check(last().player() == viewer, "Only actual menu owner receives opening snapshot");
        for (int i = 0; i < 20; i++) f.tick();
        check(deliveries() == 1, "Twenty unchanged ticks send no duplicates to ten trackers");
        check(f.machine.simulationTicks == 20, "Superclass tick still called every tick");
        check(f.machine.hasViewer(), "Legacy closest-player contract retained");
        check(snapshotCount(f.machine) == 1, "Only active viewer retained");
        scenario();
    }

    private static void independentViewers() {
        var f = fixture();
        var first = f.player();
        var second = f.player();
        f.player();
        f.open(first, 1);
        f.tick();
        f.open(second, 2);
        check(deliveries() == 2, "Second viewer receives its own initial snapshot");
        f.tick();
        check(deliveries() == 2, "Opening second viewer does not invalidate first");
        f.machine.energyStorage.amount = 57;
        f.tick();
        check(deliveries() == 4, "Changed state sent once to each viewer");
        check(NetworkContent.MACHINE_CHANNEL.deliveries.stream().filter(d -> d.player() == first).count() == 2,
          "First viewer receives exactly initial and change");
        second.currentScreenHandler = null;
        f.machine.energyStorage.amount++;
        f.tick();
        check(deliveries() == 5 && last().player() == first, "Closed menu receives no updates");
        scenario();
    }

    private static void everyStateField() {
        var f = fixture();
        var viewer = f.player();
        f.open(viewer, 1);
        f.machine.energyStorage.amount = Long.MIN_VALUE;
        f.tick();
        check(last().packet().currentEnergy() == Long.MIN_VALUE, "Energy precision retained");
        f.machine.energyStorage.capacity = Long.MAX_VALUE;
        f.tick();
        check(last().packet().maxEnergy() == Long.MAX_VALUE, "Capacity change retained");
        f.machine.setCurrentProgress(Integer.MAX_VALUE);
        f.tick();
        check(last().packet().progress() == Integer.MAX_VALUE, "Progress change retained");
        f.machine.setCurrentProgress(Integer.MIN_VALUE);
        f.tick();
        check(last().packet().progress() == Integer.MIN_VALUE, "Progress signed range retained");
        check(deliveries() == 5, "All three fields participate in change detection");
        f.tick();
        check(deliveries() == 5, "Boundary values also suppress duplicates");
        scenario();
    }

    private static void reopenAndChangeMenu() throws Exception {
        var f = fixture();
        var viewer = f.player();
        f.open(viewer, 9);
        viewer.currentScreenHandler = null;
        f.tick();
        check(snapshotCount(f.machine) == 0, "Closing menu releases snapshot and menu reference");
        f.open(viewer, 9);
        check(deliveries() == 2, "Reopening same sync id still receives fresh snapshot");
        f.tick();
        check(deliveries() == 2, "Opening snapshot is not duplicated next tick");
        viewer.currentScreenHandler = f.machine.createMenu(9, new PlayerInventory(viewer), viewer);
        f.tick();
        check(deliveries() == 3, "New handler identity receives snapshot even without opening callback");
        var other = new TestMachine(new BlockPos(8, 9, 10));
        install(f.world, other);
        viewer.currentScreenHandler = new BasicMachineScreenHandler(10, new PlayerInventory(viewer), other);
        f.tick();
        check(deliveries() == 3 && !f.machine.hasViewer(), "Menu for another machine is excluded");
        scenario();
    }

    private static void trackingAndConnectionChanges() throws Exception {
        var f = fixture();
        var viewer = f.player();
        f.open(viewer, 3);
        f.world.trackers.remove(viewer);
        f.tick();
        check(snapshotCount(f.machine) == 0, "Tracking loss releases previous snapshot");
        f.world.trackers.add(viewer);
        f.tick();
        check(deliveries() == 2, "Tracking resumes with a full snapshot");
        f.world.trackers.remove(viewer);
        f.tick();
        var reconnected = f.player();
        f.open(reconnected, 3);
        check(deliveries() == 3 && last().player() == reconnected, "New connection receives full snapshot");
        check(snapshotCount(f.machine) == 1, "Disconnected player is not retained");
        scenario();
    }

    private static void dimensionChanges() throws Exception {
        var f = fixture();
        var viewer = f.player();
        f.open(viewer, 1);
        viewer.world = new World(false);
        f.tick();
        check(deliveries() == 1 && snapshotCount(f.machine) == 0, "Wrong dimension player is excluded");
        viewer.world = f.world;
        f.tick();
        check(deliveries() == 2, "Returning viewer receives full snapshot");
        var replacementWorld = new World(false);
        install(replacementWorld, f.machine);
        check(snapshotCount(f.machine) == 0, "Changing entity world clears viewer state");
        viewer.world = replacementWorld;
        replacementWorld.trackers.add(viewer);
        f.machine.tick(replacementWorld, f.machine.getPos(), new BlockState(), f.machine);
        check(deliveries() == 3, "Snapshot uses new world");
        scenario();
    }

    private static void entityReplacementAndRemoval() throws Exception {
        var f = fixture();
        var viewer = f.player();
        f.open(viewer, 1);
        var replacement = new TestMachine(f.machine.getPos());
        install(f.world, replacement);
        f.tick();
        replacement.tick(f.world, replacement.getPos(), new BlockState(), replacement);
        check(deliveries() == 1, "Same position does not make old handler view replacement entity");
        check(snapshotCount(f.machine) == 0, "Replaced entity releases stale viewer");
        viewer.currentScreenHandler = replacement.createMenu(1, new PlayerInventory(viewer), viewer);
        replacement.writeScreenOpeningData(viewer, new PacketByteBuf());
        check(deliveries() == 2, "Replacement entity menu gets fresh snapshot");
        replacement.markRemoved();
        check(snapshotCount(replacement) == 0, "Removal releases snapshots immediately");
        replacement.sendGuiSnapshot(viewer);
        check(deliveries() == 2, "Removed entity cannot send snapshot");
        replacement.cancelRemoval();
        replacement.tick(f.world, replacement.getPos(), new BlockState(), replacement);
        check(deliveries() == 3, "Revived entity has no stale baseline");
        scenario();
    }

    private static void proxyOpeningAndUpdates() throws Exception {
        var f = fixture();
        var viewer = f.player();
        var addon = proxy(f);
        addon.setTargetSlot(7);
        viewer.currentScreenHandler = addon.createMenu(2, new PlayerInventory(viewer), viewer);
        var buf = new PacketByteBuf();
        addon.writeScreenOpeningData(viewer, buf);
        check(buf.values.equals(List.of(addon.getPos(), f.machine.getPos(), 7)), "Proxy opening payload unchanged");
        check(deliveries() == 0, "Proxy slot-selection screen does not subscribe to unused GUI fields");
        f.tick();
        check(deliveries() == 0, "Proxy receives no frame GUI tick packet");
        f.machine.setCurrentProgress(42);
        f.tick();
        check(deliveries() == 0, "Proxy receives no unused changed controller values");
        viewer.currentScreenHandler = null;
        f.tick();
        check(snapshotCount(f.machine) == 0, "Closing proxy releases controller snapshot");
        viewer.currentScreenHandler = addon.createMenu(2, new PlayerInventory(viewer), viewer);
        addon.writeScreenOpeningData(viewer, new PacketByteBuf());
        check(deliveries() == 0, "Proxy reopening preserves slot-selection-only payload");
        scenario();
    }

    private static void staleProxyController() {
        var f = fixture();
        var viewer = f.player();
        var addon = proxy(f);
        viewer.currentScreenHandler = addon.createMenu(2, new PlayerInventory(viewer), viewer);
        addon.writeScreenOpeningData(viewer, new PacketByteBuf());
        var replacement = new TestMachine(f.machine.getPos());
        install(f.world, replacement);
        addon.writeScreenOpeningData(viewer, new PacketByteBuf());
        replacement.tick(f.world, replacement.getPos(), new BlockState(), replacement);
        check(deliveries() == 0, "Stale proxy cannot send or subscribe to replacement controller by position");
        scenario();
    }

    private static void clientWorld() {
        var f = fixture();
        var viewer = f.player();
        var clientWorld = new World(true);
        install(clientWorld, f.machine);
        viewer.world = clientWorld;
        clientWorld.trackers.add(viewer);
        viewer.currentScreenHandler = f.machine.createMenu(1, new PlayerInventory(viewer), viewer);
        f.machine.writeScreenOpeningData(viewer, new PacketByteBuf());
        f.machine.tick(clientWorld, f.machine.getPos(), new BlockState(), f.machine);
        check(deliveries() == 0, "Client never obtains a server channel handle");
        scenario();
    }

    private static void failedSendRetries() throws Exception {
        var f = fixture();
        var viewer = f.player();
        viewer.currentScreenHandler = f.machine.createMenu(1, new PlayerInventory(viewer), viewer);
        NetworkContent.MACHINE_CHANNEL.failNext = true;
        try {
            f.machine.writeScreenOpeningData(viewer, new PacketByteBuf());
            throw new AssertionError("Injected send failure not propagated");
        } catch (IllegalStateException expected) {
            check(snapshotCount(f.machine) == 0, "Unsent data never becomes baseline");
        }
        f.tick();
        check(deliveries() == 1, "Failed opening send retried at tick");
        f.tick();
        check(deliveries() == 1, "Successful retry creates baseline");
        scenario();
    }

    private static void postSimulationSnapshot() {
        var f = fixture();
        var viewer = f.player();
        for (int i = 0; i < 9; i++) f.player();
        f.open(viewer, 1);
        NetworkContent.MACHINE_CHANNEL.deliveries.clear();
        f.machine.advanceSimulation = true;
        for (int i = 1; i <= 20; i++) {
            f.tick();
            check(last().packet().progress() == i, "Snapshot sees completed superclass tick " + i);
            check(last().player() == viewer, "Changed tick targets actual viewer " + i);
        }
        check(deliveries() == 20, "Twenty changing ticks deliver once to viewer, not ten trackers");
        scenario();
    }

    private static void legacyViewContractAndPlacerInventory() {
        var f = fixture();
        var bystander = f.player();
        var viewer = f.player();
        f.open(viewer, 1);
        check(!f.machine.hasViewer(), "Legacy closest bystander still masks Basic menu for inventory contract");
        f.machine.energyStorage.amount++;
        f.tick();
        check(deliveries() == 2 && last().player() == viewer, "GUI recipient detection does not use legacy closest-player gate");
        bystander.x = 100;
        check(f.machine.hasViewer(), "Legacy Basic closest-player menu still counts");
        viewer.x = 100;
        check(!f.machine.hasViewer(), "Legacy five-block distance limit retained");

        var world = new World(false);
        var placer = new PlacerBlockEntity(new BlockPos(1, 2, 3), new BlockState());
        placer.setWorld(world);
        world.entities.put(placer.getPos(), placer);
        var player = new ServerPlayerEntity(world);
        world.trackers.add(player);
        var addon = new InventoryProxyAddonBlockEntity(new BlockPos(2, 2, 3), new BlockState());
        addon.setWorld(world);
        addon.setControllerPos(placer.getPos());
        world.entities.put(addon.getPos(), addon);
        var renderedStack = new ItemStack(new BlockItem());
        placer.inventory.setStack(0, renderedStack);
        player.currentScreenHandler = addon.createMenu(1, new PlayerInventory(player), player);
        int before = NetworkContent.MACHINE_CHANNEL.inventoryBroadcasts;
        placer.updateNetwork();
        check(NetworkContent.MACHINE_CHANNEL.inventoryBroadcasts == before + 1,
          "Production Placer updateNetwork broadcasts inventory while proxy is open");
        check(placer.getToolheadAdditionalRender() == renderedStack, "Placer toolhead still renders the current inventory block");
        player.currentScreenHandler = placer.createMenu(2, new PlayerInventory(player), player);
        placer.updateNetwork();
        check(NetworkContent.MACHINE_CHANNEL.inventoryBroadcasts == before + 1,
          "Production Placer Basic-menu inventory suppression contract unchanged");
        scenario();
    }

    private static Fixture fixture() {
        NetworkContent.MACHINE_CHANNEL.deliveries.clear();
        NetworkContent.MACHINE_CHANNEL.failNext = false;
        NetworkContent.MACHINE_CHANNEL.inventoryBroadcasts = 0;
        var f = new Fixture(new World(false), new TestMachine(new BlockPos(1, 2, 3)));
        install(f.world, f.machine);
        return f;
    }

    private static InventoryProxyAddonBlockEntity proxy(Fixture f) {
        var addon = new InventoryProxyAddonBlockEntity(new BlockPos(2, 2, 3), new BlockState());
        addon.setWorld(f.world);
        addon.setControllerPos(f.machine.getPos());
        f.world.entities.put(addon.getPos(), addon);
        return addon;
    }

    private static void install(World world, TestMachine machine) {
        machine.setWorld(world);
        world.entities.put(machine.getPos(), machine);
    }

    private static int snapshotCount(TestMachine machine) throws Exception {
        Field field = ItemEnergyFrameInteractionBlockEntity.class.getDeclaredField("guiSnapshots");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(machine)).size();
    }

    private static int deliveries() { return NetworkContent.MACHINE_CHANNEL.deliveries.size(); }
    private static NetworkContent.Delivery last() { return NetworkContent.MACHINE_CHANNEL.deliveries.get(deliveries() - 1); }
    private static void check(boolean condition, String message) { assertions++; if (!condition) throw new AssertionError(message); }
    private static void scenario() { scenarios++; }

    private record Fixture(World world, TestMachine machine) {
        ServerPlayerEntity player() { var player = new ServerPlayerEntity(world); world.trackers.add(player); return player; }
        PacketByteBuf open(ServerPlayerEntity player, int id) {
            player.currentScreenHandler = machine.createMenu(id, new PlayerInventory(player), player);
            var buf = new PacketByteBuf();
            machine.writeScreenOpeningData(player, buf);
            return buf;
        }
        void tick() { machine.tick(world, machine.getPos(), new BlockState(), machine); }
    }

    private static final class TestMachine extends ItemEnergyFrameInteractionBlockEntity {
        TestMachine(BlockPos pos) { super(new BlockEntityType<>(), pos, new BlockState()); }
        public int getMoveEnergyUsage() { return 1; }
        public int getOperationEnergyUsage() { return 1; }
        boolean hasViewer() { return isActivelyViewed(); }
    }
}
