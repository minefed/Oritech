# Frame GUI synchronization verification

`gradlew verifyFrameGuiSync` compiles and executes the **production**
`ItemEnergyFrameInteractionBlockEntity`, `BasicMachineScreenHandler`,
`InventoryProxyScreenHandler`, `InventoryProxyAddonBlockEntity` and
`PlacerBlockEntity` sources.
Minecraft, Fabric and unrelated machine systems are small platform doubles.
The channel double rejects a GUI block-entity broadcast, records recipients and
payload values, and can inject a send failure. The superclass double records
simulation calls and can advance progress before synchronization.

This verifies recipient and snapshot lifetime behavior, not Minecraft's real
physics, native rendering, socket compression or owo serialization. `remapJar`
separately compiles the same changed production sources against the actual
Minecraft 1.20.4 / Fabric / owo dependencies. No fixture classes enter that JAR.

The cases cover unchanged and changing state, all transmitted fields, simultaneous
viewers, unrelated trackers, identical screen IDs with different menu objects,
opening/closing, tracking loss/reentry, replacement connections, dimension changes,
removed/replaced/revived block entities, proxy menus, stale proxy controllers,
client worlds, failed sends, unchanged opening data, and superclass-before-sync
order. It executes the real placer's `updateNetwork()` and verifies that proxy
menus still allow its existing inventory broadcast, which feeds the gantry's
additional item rendering. The protected closest-player inventory contract is
unchanged; GUI recipients use a separate exact entity identity check. Proxy
slot-selection screens do not consume or subscribe to frame GUI fields.
Delivery counts are deterministic fixture results, not live bandwidth.

Initial snapshot ordering was checked in the actual Fabric dependency
`fabric-screen-handler-api-v1:1.3.56+78d798af4f` (Fabric API 0.97.0+1.20.4).
`ServerPlayerEntityMixin.fabric_storeOpenedScreenHandler` injects immediately
before the intercepted vanilla send and installs `currentScreenHandler`.
`fabric_replaceVanillaScreenPacket` then calls `Networking.sendOpenPacket`,
which calls `writeScreenOpeningData` before sending the opening packet. The
fixture therefore installs the new handler before calling the opening writer.
The same order was verified using `javap -p -c` on the resolved dependency JAR.

The GUI packet format, client receiver, simulation, inventory broadcasts and
world renderers are unchanged. Source review found frame energy/capacity/progress
readers in `BasicMachineScreen`; `TooltipHelper` reads default capacity from a
newly constructed entity rather than a live world instance. `MachineGantryRenderer`
uses target/movement fields from the separate movement update and the placer's
inventory item, not these GUI fields. This is source-based compatibility evidence;
the fixture does not replace an interactive client visual check.

Validated on 2026-10-12 from integration baseline
`5085fb1e03783d19f47d5b68f12d5d269d6f08ba` with Java 17 and Gradle 8.7:

```text
gradlew.bat :verifyFrameGuiSync :remapJar --no-daemon --max-workers=1 -Dorg.gradle.parallel=false
Frame GUI sync: 13 scenarios, 96 assertions passed
BUILD SUCCESSFUL
```

In the deterministic workload with ten tracking players and one closest viewer,
twenty idle ticks previously implied 200 broadcasts-to-player deliveries; the
new code sends one opening snapshot and no duplicate tick updates. Twenty changing
ticks deliver 20 updates to that viewer rather than 200 to all trackers. These
are packet recipient counts, not measured bytes or server/GPU performance.
No operational server or running game was used or modified during verification.
