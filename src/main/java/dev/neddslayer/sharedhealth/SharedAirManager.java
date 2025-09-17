package dev.neddslayer.sharedhealth;

import dev.neddslayer.sharedhealth.components.SharedAirComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.SHARED_AIR;

public class SharedAirManager {
    private static final int DEFAULT_MAX_AIR = 300;
    private static final int REGEN_RATE = 4;
    private static final int DAMAGE_INTERVAL = 20;
    private static final float BASE_DROWN_DAMAGE = 2.0f;

    private final MinecraftServer server;

    public SharedAirManager(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        SharedAirComponent component = SHARED_AIR.get(server.getScoreboard());
        int sharedAir = component.getAir();
        int sharedMaxAir = component.getMaxAir();
        int drowningTicks = component.getDrowningTicks();

        List<ServerPlayerEntity> players = collectPlayers();
        if (players.isEmpty()) {
            if (sharedAir != sharedMaxAir) {
                component.setAir(sharedMaxAir);
            }
            component.setDrowningTicks(0);
            return;
        }

        int computedMaxAir = 0;
        int totalConsumption = 0;
        int consumingPlayers = 0;
        boolean anySubmerged = false;

        for (ServerPlayerEntity player : players) {
            if (!player.isAlive()) {
                continue;
            }

            computedMaxAir = Math.max(computedMaxAir, player.getMaxAir());

            boolean invulnerable = player.getAbilities().invulnerable;
            boolean canBreathe = player.canBreatheInWater();
            boolean isSubmerged = player.isSubmergedInWater();

            if (isSubmerged && !canBreathe && !invulnerable) {
                anySubmerged = true;
            }

            int playerAir = player.getAir();
            int delta = sharedAir - playerAir;
            if (delta > 0 && !invulnerable) {
                totalConsumption += delta;
                consumingPlayers++;
            }
        }

        if (computedMaxAir <= 0) {
            computedMaxAir = DEFAULT_MAX_AIR;
        }
        if (computedMaxAir != sharedMaxAir) {
            component.setMaxAir(computedMaxAir);
            sharedMaxAir = computedMaxAir;
        }

        int newAir = sharedAir;
        if (totalConsumption > 0) {
            newAir = sharedAir - totalConsumption;
            if (newAir < 0) {
                int deficit = -newAir;
                newAir = 0;
                drowningTicks += deficit;
            }
        } else if (!anySubmerged) {
            if (sharedAir < sharedMaxAir) {
                newAir = Math.min(sharedMaxAir, sharedAir + REGEN_RATE);
            }
            if (newAir == sharedMaxAir) {
                drowningTicks = 0;
            }
        }

        if (newAir > 0 && totalConsumption == 0) {
            drowningTicks = 0;
        }

        component.setAir(Math.max(0, Math.min(newAir, sharedMaxAir)));

        if (component.getAir() == 0 && consumingPlayers > 0) {
            if (drowningTicks >= DAMAGE_INTERVAL) {
                int events = drowningTicks / DAMAGE_INTERVAL;
                applyDrowningDamage(players, consumingPlayers, events);
                drowningTicks -= events * DAMAGE_INTERVAL;
            }
        } else if (component.getAir() > 0) {
            drowningTicks = 0;
        }

        component.setDrowningTicks(Math.max(0, drowningTicks));

        int finalAir = component.getAir();
        for (ServerPlayerEntity player : players) {
            if (player.isAlive()) {
                int clampedAir = Math.min(finalAir, player.getMaxAir());
                player.setAir(clampedAir);
            }
        }
    }

    private List<ServerPlayerEntity> collectPlayers() {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            players.addAll(world.getPlayers());
        }
        return players;
    }

    private void applyDrowningDamage(List<ServerPlayerEntity> players, int consumingPlayers, int events) {
        if (consumingPlayers <= 0 || events <= 0) {
            return;
        }

        float damageAmount = BASE_DROWN_DAMAGE * consumingPlayers * events;
        for (ServerPlayerEntity player : players) {
            if (!player.isAlive() || player.getAbilities().invulnerable) {
                continue;
            }
            player.damage(player.getServerWorld(), player.getServerWorld().getDamageSources().drown(), damageAmount);
        }
    }
}
