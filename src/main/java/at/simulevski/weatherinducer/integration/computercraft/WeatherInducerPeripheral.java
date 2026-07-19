package at.simulevski.weatherinducer.integration.computercraft;

import at.simulevski.weatherinducer.content.inducer.WeatherInducerBlockEntity;
import at.simulevski.weatherinducer.content.inducer.WeatherMode;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Locale;

/**
 * ComputerCraft: Tweaked peripheral for the Weather Inducer. Attach a computer
 * to an inducer to read its charge and drive it from Lua, e.g.:
 *
 * <pre>
 * local w = peripheral.wrap("right")
 * print(w.getCharge() .. "/" .. w.getMaxCharge())
 * w.setMode("lightning")
 * w.setLightningOffset(10, -4)
 * if w.isCharged() and w.canSeeSky() then w.fire() end
 * </pre>
 */
public class WeatherInducerPeripheral implements IPeripheral {

    private final WeatherInducerBlockEntity blockEntity;

    public WeatherInducerPeripheral(WeatherInducerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "weather_inducer";
    }

    @LuaFunction
    public final double getCharge() {
        return blockEntity.getCharge();
    }

    @LuaFunction
    public final double getMaxCharge() {
        return WeatherInducerBlockEntity.MAX_CHARGE;
    }

    @LuaFunction
    public final boolean isCharged() {
        return blockEntity.isCharged();
    }

    @LuaFunction(mainThread = true)
    public final boolean canSeeSky() {
        return blockEntity.hasSkyAccess();
    }

    @LuaFunction
    public final String getMode() {
        return blockEntity.getMode().translationKey();
    }

    @LuaFunction(mainThread = true)
    public final void setMode(String mode) throws LuaException {
        try {
            blockEntity.setMode(WeatherMode.valueOf(mode.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new LuaException("Unknown mode '" + mode + "' (expected rain, clear or lightning)");
        }
    }

    @LuaFunction(mainThread = true)
    public final int[] getLightningOffset() {
        return new int[]{blockEntity.getLightningOffsetX(), blockEntity.getLightningOffsetZ()};
    }

    @LuaFunction(mainThread = true)
    public final void setLightningOffset(int x, int z) {
        blockEntity.setLightningOffset(x, z);
    }

    /** Attempts to fire; returns true if it fired (charged and sky-visible). */
    @LuaFunction(mainThread = true)
    public final boolean fire() {
        return blockEntity.fire();
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof WeatherInducerPeripheral peripheral
                && peripheral.blockEntity == this.blockEntity;
    }
}
