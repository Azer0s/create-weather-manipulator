# Weather Inducer — a Create addon

A [Create](https://github.com/Creators-of-Create/Create) addon for **Minecraft 1.21.1 / NeoForge 21.1.233 / Create 6.0.10** that adds two kinetic blocks:

| Block | What it does |
| ----- | ------------ |
| **Weather Inducer** | Charges from the connected kinetic network's Stress Units (SU) up to **100,000 SU**. When fully charged and pulsed with redstone, it applies the selected weather effect — **rain**, **clear**, or **lightning** at a configurable position — provided it can see the sky. |
| **SU Resistor** | An inline shaft block with a configurable **SU/tick** cap. It throttles how fast an inducer sitting downstream of it can charge. Without a resistor, the inducer fills in a single tick. |

---

## Gameplay

### Weather Inducer
- **Kinetic input:** a shaft on the front/back faces (the facing axis).
- **Charging:** while the shaft is turning, the inducer absorbs SU from its kinetic network each tick, up to 100,000 SU. Charge rate = `min(network capacity, inline resistor cap)`. With no inline resistor it charges instantly.
- **Sky line-of-sight:** the block directly above must be able to see the sky, or firing is blocked.
- **Mode (top value box):** scroll to pick `Rain` / `Clear` / `Lightning`.
- **Lightning offset (side value boxes):** two scrolls set the X/Z offset (±64) of the lightning strike, measured from the inducer. The strike lands on the surface at that column.
- **Trigger:** a **rising redstone edge** fires the selected effect when fully charged, then discharges the block back to 0 SU (it must recharge before firing again).
- **Comparator:** emits a redstone signal (0–15) proportional to charge.

### SU Resistor
- **Inline shaft:** rotation passes straight through along its axis, exactly like a shaft.
- **SU/tick cap (value box on the four side faces):** scroll to set the limit (0 – 1,000,000, default 1,000).
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

## Building

```bash
./gradlew build
```
The built jar lands in `build/libs/`. To launch a dev client/server:
```bash
./gradlew runClient
./gradlew runServer
```

> **Heads-up on dependencies:** the buildscript pulls Create (and its split-out
> runtime deps — Flywheel, Registrate, Ponder, Catnip) from
> `https://maven.createmod.net` and `https://maven.tterrag.com`. These hosts
> were **blocked by egress policy in the environment this addon was authored
> in**, so the exact artifact coordinates in `build.gradle` could not be
> resolved live. Before your first build, cross-check the coordinates in
> `build.gradle` (search for the "Create + its dependency stack" comment)
> against Create 6.0.10's published artifacts. The Create wiki's
> "Setting up an addon workspace" page lists the current coordinates per
> MC/NeoForge version.

---

## Implementation notes & verify-points

Because the build environment could not reach Create/NeoForge Maven, the code
could not be compiled here. It is written to Create 6.0.x conventions, and the
**version-sensitive Create API touchpoints are deliberately isolated** so a
first local compile has few, obvious places to adjust:

1. **`network/SUNetwork.java`** — the only class that reads Create's kinetic
   internals:
   - `KineticBlockEntity#getOrCreateNetwork()`
   - `KineticNetwork#getCapacity()` (total provided SU)
   - `KineticNetwork.members` / `KineticNetwork.sources` (public maps used for
     the inline-resistor traversal)
   If any of these were renamed, this is the single file to fix.

2. **Value boxes** — `ScrollValueBehaviour` fluent calls (`between`,
   `withFormatter`, `setValue`, `getValue`) in
   `WeatherInducerBlockEntity` / `SUResistorBlockEntity`, and the
   `CenteredSideValueBoxTransform(BiPredicate)` constructor in
   `content/util/SideValueBoxTransform.java`.

3. **Rendering** — `client/WeatherInducerClient.java` registers Create's
   `ShaftRenderer<>`. If `ShaftRenderer` is non-generic in your build, adjust
   the constructor call. Block models are casing-only by design; the renderer
   draws the spinning shaft.

4. **Base classes / interfaces** — `HorizontalKineticBlock`,
   `RotatedPillarKineticBlock`, `IBE`, and
   `IHaveGoggleInformation` (`content.equipment.goggles`) package paths.

5. **Textures** are borrowed from Create/vanilla (`create:block/brass_casing`,
   `create:block/andesite_casing`, `minecraft:block/copper_block`) so nothing
   renders as a missing texture. Replace them under
   `assets/weatherinducer/textures/` for custom art.

### The custom SU model, in short
Create has no built-in battery/drain mechanic, so "SU" here is a thin custom
layer: the inducer treats its network's stress capacity as a pool it absorbs
from each tick. "Strictly inline upstream" resistor scope is implemented as a
barrier-BFS over network members (6-neighbour adjacency): a resistor caps its
branch and stops expansion; if any branch reaches a source ungated, the inducer
fills in one tick. This matches the design: *unthrottled ⇒ instant; add
resistors to slow it down.*

## License
MIT
