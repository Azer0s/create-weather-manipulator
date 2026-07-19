# Weather Inducer: a Create addon

<img src="src/main/resources/logo.png" alt="Weather Inducer logo" width="160" align="right">

A [Create](https://github.com/Creators-of-Create/Create) addon for **Minecraft 1.21.1 / NeoForge 21.1.233 / Create 6.0.10** that adds two kinetic blocks:

| Block | What it does |
| ----- | ------------ |
| **Weather Inducer** | Charges from the connected kinetic network's Stress Units (SU) up to **100,000 SU**. When fully charged and pulsed with redstone, it applies the selected weather effect: **rain**, **clear**, or **lightning** at a configurable position, provided it can see the sky. |
| **SU Resistor** | An inline shaft block with a configurable **SU/tick** cap. It throttles how fast an inducer sitting downstream of it can charge. Without a resistor, the inducer fills in a single tick. |

---

## Gameplay

### Weather Inducer
- **Kinetic input:** a shaft on the front/back faces (the facing axis).
- **Charging:** while the shaft is turning, the inducer absorbs SU from its kinetic network each tick, up to 100,000 SU. Charge rate = `min(network capacity, inline resistor cap)`. With no inline resistor it charges instantly.
- **Sky line-of-sight:** the block directly above must be able to see the sky, or firing is blocked.
- **Mode (top value box):** scroll to pick `Rain` / `Clear` / `Lightning`.
- **Lightning offset (side value boxes):** two scrolls set the X/Z offset (-64 to +64) of the lightning strike, measured from the inducer. The strike lands on the surface at that column.
- **Trigger:** a **rising redstone edge** fires the selected effect when fully charged, then discharges the block back to 0 SU (it must recharge before firing again).
- **Comparator:** emits a redstone signal (0 to 15) proportional to charge.

### SU Resistor
- **Inline shaft:** rotation passes straight through along its axis, exactly like a shaft.
- **SU/tick cap (value box on the four side faces):** scroll to set the limit (0 to 1,000,000, default 1,000).
- Place one (or several) on the shaft feeding an inducer to throttle its charge rate. In series the tightest resistor wins; in parallel the caps add.

---

## Crafting recipes

**Weather Inducer** (Precision-Mechanism tier):
```
L C L      L = Lightning Rod        C = Copper Block
E P E      E = Electron Tube        P = Precision Mechanism
B S B      B = Brass Sheet          S = Shaft
```

**SU Resistor** (yields 2):
```
N B N      N = Andesite Alloy       B = Brass Sheet
S G S      S = Shaft                G = Cogwheel
N B N
```

---

## Mod integrations

All integrations are optional; the mod runs with none of them installed, and each
integration class only loads when its mod is present.

### Ponder
Both blocks ship in-game Ponder scenes (see them from the item tooltip's "Ponder"
key or in JEI/EMI). They demonstrate a creative motor driving a shaft through
an SU Resistor into a Weather Inducer, and explain SU charging, the sky
requirement, redstone firing, and the resistor throttle.

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
spawns a bolt; a blocked sky prevents firing; and firing below 100k SU is a no-op.

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
   `KineticNetwork.members` / `KineticNetwork.sources` maps used for the
   inline-resistor traversal.

2. **Value boxes**: `ScrollValueBehaviour` (`between`, `withFormatter`,
   `getValue`, `setValue`) in the two block entities, positioned via
   `CenteredSideValueBoxTransform` (`content/util/SideValueBoxTransform.java`).

3. **Rendering**: `client/WeatherInducerClient.java` registers Create's
   generic `ShaftRenderer<>`. Block models are casing-only by design; the
   renderer draws the spinning shaft on the connecting faces.

4. **Models and textures**: the mod ships its own 16x16 pixel art under
   `assets/weatherinducer/textures/block/`, styled after Create's brass and
   andesite casings (frame bars with corner brackets, plank interiors, a
   top-left light source and light dithering). Both blocks use element
   models built in datagen, with matching voxel shapes and noOcclusion. The
   Weather Inducer is a stepped machine: a 13px casing base with shaft
   bearings, topped by a raised copper emitter cap with a teal aperture,
   and the bolt emblem embossed half a pixel proud of both side faces (the
   raised geometry samples the same texture pixels as the flat art, so the
   two always line up). The SU Resistor is shaped like its namesake: two
   andesite collar flanges at the shaft ends with the banded ceramic body
   suspended between them; the bands read brown-black-red with a gold
   tolerance band, which is 1000 in the resistor color code and also its
   default SU/tick cap.

### The custom SU model, in short
Create has no built-in battery/drain mechanic, so "SU" here is a thin custom
layer: the inducer treats its network's stress capacity as a pool it absorbs
from each tick. "Strictly inline upstream" resistor scope is implemented as a
barrier-BFS over network members (6-neighbour adjacency): a resistor caps its
branch and stops expansion; if any branch reaches a source ungated, the inducer
fills in one tick. This matches the design: *unthrottled means instant; add
resistors to slow it down.*

## License
MIT
