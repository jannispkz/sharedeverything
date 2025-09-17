package dev.neddslayer.sharedhealth.components;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;

public class SharedAirComponent implements IAirComponent {
    private static final int DEFAULT_MAX_AIR = 300;

    private int air = DEFAULT_MAX_AIR;
    private int maxAir = DEFAULT_MAX_AIR;
    private int drowningTicks = 0;

    @SuppressWarnings("unused")
    private final Scoreboard scoreboard;
    @SuppressWarnings("unused")
    private final MinecraftServer server;

    public SharedAirComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    @Override
    public int getAir() {
        return air;
    }

    @Override
    public void setAir(int air) {
        this.air = air;
    }

    @Override
    public int getMaxAir() {
        return maxAir;
    }

    @Override
    public void setMaxAir(int maxAir) {
        this.maxAir = maxAir;
    }

    @Override
    public int getDrowningTicks() {
        return drowningTicks;
    }

    @Override
    public void setDrowningTicks(int drowningTicks) {
        this.drowningTicks = drowningTicks;
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        air = tag.contains("Air") ? tag.getInt("Air") : DEFAULT_MAX_AIR;
        maxAir = tag.contains("MaxAir") ? tag.getInt("MaxAir") : DEFAULT_MAX_AIR;
        drowningTicks = tag.contains("DrowningTicks") ? tag.getInt("DrowningTicks") : 0;
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("Air", air);
        tag.putInt("MaxAir", maxAir);
        tag.putInt("DrowningTicks", drowningTicks);
    }
}
