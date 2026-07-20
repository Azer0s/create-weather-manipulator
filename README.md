# Weather Inducer: a Create addon

<img src="src/main/resources/logo.png" alt="Weather Inducer logo" width="160" align="right">

A [Create](https://github.com/Creators-of-Create/Create) addon for **Minecraft 1.21.1 / NeoForge 21.1.233 / Create 6.0.10** for controlling the weather with kinetic power:

| Block | What it does |
| ----- | ------------ |
| **Weather Inducer** | Charges from the network's **spare SU** (provided capacity minus used stress) up to **1,048,576 SU** (2^20), at up to **131,072 SU per tick** (2^17). When fully charged and pulsed with redstone, it applies the selected weather effect: **rain**, **clear**, or **lightning** at a configurable position, provided it can see the sky. |
| **Stress Gate** | An inline shaft block that stays **locked** until the kinetic network provides at least a set amount of total SU. Use it to keep contraptions dormant until the power plant is big enough. |
| **Kinetic Charger** | A kinetic battery with two sides: power flows in and out through the front I/O face, and flywheels banked on the back set the capacity (2,048 SU-seconds bare, ~104,858 per wheel, up to ten; more jams it). It stores SU-seconds like watt-hours: charging loads the network with capacity divided by charge time, discharge drains the machines' stress per second, and the flywheels keep spinning as long as energy remains. Emits a redstone signal proportional to its fill. |
| **Weather Sensor** | A daylight-detector-shaped slab that reads the sky: redstone **0** when clear, **7** in rain, **15** in a thunderstorm. Covered, it reads 0. |
| **Lightning Medium** | A beacon and an end crystal encased in glass; the crystal bobs and spins inside the shell like a real end crystal. Struck by lightning, it shatters into a **Bottle o' Lightning**, the base of the lightning gear. |

---

## Gameplay

### Weather Inducer
- **Kinetic input:** a shaft on the front/back faces (the facing axis).
- **Charging:** the inducer charges while its shaft turns, and it pays for it: the network carries a real stress load of the 1,048,576 SU it needs divided by the charge time in seconds. Ten seconds costs a monstrous 104,858 SU; 1,280 seconds a gentle 819. Overstress the network and everything, charging included, halts.
- **Charge time (value boxes on the shaft faces):** picks how long a full charge takes, from 10 seconds (the floor: even flat out, the inducer fires at most once every 10 seconds) up a doubling ladder to 1,280 seconds. The slider locks while any charge is in the block, and goggles show the configured time and the stress it costs.
- **Sky line-of-sight:** the block directly above must be able to see the sky, or firing is blocked.
- **Mode (top value box):** Create's option menu with an icon and label per entry; scroll or drag to pick `Rain` / `Clear` / `Lightning`.
- **Lightning offset (side value boxes):** two scrolls set the X/Z offset (-64 to +64) of the lightning strike, measured from the inducer. The strike lands on the surface at that column.
- **Trigger:** a **rising redstone edge** fires the selected effect when fully charged, then discharges the block back to 0 SU (it must recharge before firing again).
- **Charge indicator:** the bolt emblem on the sides lights up gold from the tip upward in sixths of a full charge, so the fill level is readable at a glance. Goggles show a live charge bar plus the exact SU numbers, updated tick by tick.
- **Comparator:** emits a redstone signal (0 to 15) proportional to charge.

### Kinetic Charger
- **Two sides:** the front face (teal ring) is the I/O side, where power flows in and out; the back is the flywheel side, where the capacity bank hangs. Placed dropper-style, the I/O face points away from you. Both sides stay connected at all times.
- **Flywheel bank:** capacity comes from flywheels connected behind the back face: 2,048 SU-seconds bare, about 104,858 per wheel, ten wheels tops. An eleventh wheel is more inertia than the charger can spin: it grinds the network to an overstressed halt until removed. Wheels can be added or removed while the charger runs; the capacity follows within half a second, and a shrunken bank spills any charge it can no longer hold.
- **Watt-hour storage:** the buffer holds SU-seconds, power times time. Charging loads the network with real stress (capacity divided by the charge time slider, 10 s at the fastest, slider locked while charged) and banks a tick's worth each tick; discharging drains the driven machines' stress per second, so a full ten-wheel bank runs a 1,024 SU load for about 1,021 seconds.
- **Neutral drain:** holding charge is never free: 5 SU-seconds bleed away every second no matter what the charger is doing, so an idle bare charger empties in about seven minutes and even a full ten-wheel bank slowly winds down over a couple of days.
- **Battery mode:** when no other source powers its network and the buffer holds energy, the charger becomes the source itself, driving the I/O side at the speed it charged with and providing 131,072 SU. The flywheels keep spinning the whole time; when the buffer runs dry, everything coasts to a stop.
- **Redstone output:** the block emits a signal of 0 to 15 proportional to the buffer fill; a comparator reads the same value, and goggles show the exact numbers, the wheel count and the current mode.
- **Fill indicator:** the gauge on the drum fills with teal as the buffer charges.

### Stress Gate
- **Inline shaft with a lock:** below its threshold the gate passes no rotation downstream, like a disengaged clutch; the padlock on its body shows a red pip.
- **Threshold (value box on the four side faces):** a power-of-two ladder from 0 to 1,048,576 SU (default 131,072 SU). Once the network's **total provided SU** reaches it, the gate unlocks and rotation flows.
- The provided capacity comes purely from the source side, so locking the downstream away never changes the reading: the gate cannot oscillate. Goggles show the threshold, the current provision, and the lock state.

### Weather Sensor
- **Daylight detector, but for weather:** a 6px slab that must see the sky.
- **Output:** redstone 0 under clear skies, 7 in rain, 15 during a thunderstorm; 0 when covered. It re-reads the weather every half second.
- Feed it into the Weather Inducer's trigger line to build self-acting weather machines, for example one that clears every storm as it rolls in.

### Lightning gear
The endgame chain, powered by the Weather Inducer's own lightning:

0. **Bottle o' Lightning**: also a throwable. Snowball physics, except the
   point of impact gets struck by lightning. Throwing one at another
   Lightning Medium bottles the strike right back.
1. **Lightning Medium**: craft a beacon and an end crystal into a glass shell
   (recipe below). Place it under open sky and strike it with lightning; the
   block is consumed and drops a **Bottle o' Lightning**. An inducer in
   lightning mode with a matching offset automates the whole thing, and the
   bottle survives the strike that creates it.
2. **Lightning Bolt**: one ancient debris plus two bottles, laid out as a
   diagonal in the crafting grid.
3. **Tools** (sword, pickaxe, axe, shovel, hoe): the usual shapes, with
   stripped logs instead of sticks. 4,096 durability, mining speed 16, and
   the diggers come out of the crafting table with Efficiency V already on
   them. The sword hits for 1,024 damage, which one-shots everything up to
   and including the warden. Holding any lightning tool grants Speed II.
4. **Armor**: Thor-styled steel with gold discs and a winged helm. The
   chestplate recipe also takes an elytra. Each piece comes with Thorns III;
   the full set grants water breathing, fire resistance, Resistance IV,
   Strength II, Speed II, Regeneration, creative flight, and no fall damage.
   With 40 armor points, toughness 16, and full knockback resistance on top,
   it is practically invincible.

---

## Crafting recipes

**Weather Inducer** (Precision-Mechanism tier):
```
L C L      L = Lightning Rod        C = Copper Block
E P E      E = Electron Tube        P = Precision Mechanism
B S B      B = Brass Sheet          S = Shaft
```

**Kinetic Charger**:
```
B C B      B = Brass Sheet          C = Copper Block
S E S      S = Shaft                E = Electron Tube
B C B
```

**Stress Gate**:
```
N B N      N = Andesite Alloy       B = Brass Sheet
S E S      S = Shaft                E = Electron Tube
N B N
```

**Weather Sensor** (daylight detector layout):
```
G G G      G = Glass
C E C      C = Copper Sheet         E = Electron Tube
A A A      A = Andesite Alloy
```

**Lightning Medium**:
```
G G G      G = Glass
G C G      C = End Crystal
G B G      B = Beacon
```

**Lightning Bolt** (a diagonal, like its namesake):
```
. . B      B = Bottle o' Lightning
. B .      A = Netherite Ingot
A . .
```

**Lightning tools and armor**: vanilla shapes with Lightning Bolts as the
material; tools take stripped logs (any kind) instead of sticks, and the
chestplate takes an elytra in its centre slot. Worn, the armor uses its own
3D model: a winged, crested helm with a gold brow, shoulder pauldrons, a
raised chest emblem, a belt, a cape plate, knee guards, and trimmed boots.

---

## Mod integrations

All integrations are optional; the mod runs with none of them installed, and each
integration class only loads when its mod is present.

### Ponder
The Weather Inducer and Kinetic Charger ship in-game Ponder scenes (the
Weather Sensor and Stress Gate do not have scenes yet; their JEI/EMI info
pages cover the mechanics). See them via the item tooltip's Ponder key or in
JEI/EMI. They explain stress-paid charging, the charge time slider, the
flywheel capacity bank, battery discharge, the sky requirement and redstone
firing. The scene text is
authored inline in `ModPonderScenes`; datagen runs Ponder's registration and
writes the generated lang entries into `en_us.json` (`ModLanguageProvider`),
which is what makes the text actually show up in game.

### JEI & EMI
Both recipe viewers show the crafting recipes automatically, plus an **information
page** for each block describing its mechanics.

### ComputerCraft: Tweaked
A Weather Inducer exposes a `weather_inducer` peripheral:
```lua
local w = peripheral.find("weather_inducer")
print(w.getCharge() .. " / " .. w.getMaxCharge() .. " SU")
w.setMode("lightning")          -- "rain", "clear" or "lightning"
w.setLightningOffset(10, -4)    -- X/Z offset from the inducer
if w.isCharged() and w.canSeeSky() then w.fire() end
```

### KubeJS
A `WeatherInducer` binding is available to scripts:
```js
// server script
BlockEvents.rightClicked("minecraft:stick", event => {
  const pos = event.block.pos
  if (WeatherInducer.isCharged(event.level, pos)) {
    WeatherInducer.setMode(event.level, pos, "lightning")
    WeatherInducer.fire(event.level, pos)
  }
})
```

---

## Testing

Runtime game tests cover the Weather Inducer's fire logic. Run them headlessly:
```bash
./gradlew runGameTestServer
```
They verify: full-charge + sky + redstone fires and discharges; lightning mode
spawns a bolt; a blocked sky prevents firing; firing below full charge is a
no-op; a lightning strike consumes a Lightning Medium and bottles the strike
(the drop surviving the bolt); the Weather Sensor's signal follows thunder and clear skies (in its own
test batch, since weather is global); the Kinetic Charger offers its buffer only
while its input is stopped, publishes the fill level as redstone, and drains
exactly what is taken; the charger drives its output side from the buffer,
with and without a clutch on the input; the charge time slider paces the
inducer's intake; and the Stress Gate stays locked below its threshold
and opens once the network provides enough.

---

## Building

```bash
./gradlew build
```
The built jar lands in `build/libs/` (`weatherinducer-<version>-mc1.21.1.jar`).
To launch a dev client/server:
```bash
./gradlew runClient
./gradlew runServer
```

This project **compiles cleanly against Create `6.0.10-281`** (the last 6.0.10
build) and its runtime stack. The exact coordinates are pinned in
`gradle.properties`; the repositories that serve them are declared in
`build.gradle`:
- Create, Flywheel, Ponder, Catnip: `https://maven.createmod.net`
- Registrate (`MC1.21-1.3.0+67`): `https://maven.ithundxr.dev/snapshots`

---

## Implementation notes

The **Create API touchpoints are deliberately isolated** so the integration is
easy to follow and maintain:

1. **`network/SUNetwork.java`** is the only class that reads Create's kinetic
   internals: `KineticBlockEntity#getOrCreateNetwork()`,
   `KineticNetwork#calculateCapacity()` (total provided SU), and the public
   `KineticNetwork.sources` map used to keep battery-provided capacity out
   of the spare-SU reading.

2. **Value boxes**: the inducer's mode selector is a
   `ScrollOptionBehaviour<WeatherMode>` (the enum implements
   `INamedIconOptions`, so Create renders its option menu with icons); the
   offsets, the charge time and the gate threshold are
   `ScrollValueBehaviour`s scrolling over ladder indices rather than raw
   values. All are positioned via `CenteredSideValueBoxTransform`
   (`content/util/SideValueBoxTransform.java`).

3. **Rendering**: `client/WeatherInducerClient.java` registers Create's
   generic `ShaftRenderer<>`. The block models leave a 2px deep socket
   around every shaft connection, so the spinning shaft the renderer draws
   is actually visible, like on other Create machines.

4. **Models and textures**: the mod ships its own 16x16 pixel art under
   `assets/weatherinducer/textures/block/`, styled after Create's brass and
   andesite casings (frame bars with corner brackets, plank interiors, a
   top-left light source and light dithering). The kinetic blocks use
   element models built in datagen, with matching voxel shapes and
   noOcclusion. The Weather Inducer is a stepped machine: a 13px casing
   base with socketed shaft bearings, topped by a raised copper emitter cap
   with a teal aperture, and the bolt emblem embossed half a pixel proud of
   both side faces (the raised geometry samples the same texture pixels as
   the flat art, so the two always line up). The inducer and the charger
   bake their fill level into the blockstate: six models each, pointing at
   side-texture variants where the bolt lights up gold (inducer) or the
   capacitor gap fills with teal (charger). A little copper lightning rod stands centered
   on the cap, plugged into the emitter aperture: a 2x2 pole with the
   vanilla rod's thicker tip.

### The SU model, in short
There is no custom SU bookkeeping layer. The two capacitors (Weather
Inducer, Kinetic Charger) charge by being honest Create consumers: while
filling, each loads its network with real stress equal to its capacity
divided by its charge time, and banks a tick's worth of that every tick.
Stopped or overstressed shafts charge nothing. The Kinetic Charger is the
battery half: once its input stops it becomes a kinetic source itself,
driving its output side and draining the buffer by exactly the stress the
driven machines use.

The Stress Gate
uses Create's clutch mechanism (`SplitShaftBlockEntity`): it stays locked until
`calculateCapacity()` reaches its threshold, a reading that only depends on
the source side, so it cannot flap either. Both are covered by game tests
that spin a real creative motor and encased fan.

## License
MIT
