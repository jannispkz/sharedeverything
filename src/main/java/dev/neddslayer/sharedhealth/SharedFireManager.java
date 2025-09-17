package dev.neddslayer.sharedhealth;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SharedFireManager {
    private final MinecraftServer server;
    private final Set<UUID> managedBurningPlayers = new HashSet<>();
    private final Set<UUID> naturalBurningPlayers = new HashSet<>();
    private boolean sharedFireActive = false;

    public SharedFireManager(MinecraftServer server) {
        this.server = server;
    }

    public void tick(boolean enabled) {
        List<ServerPlayerEntity> players = collectPlayers();

        if (!enabled) {
            if (sharedFireActive) {
                extinguishAll(players);
            }
            resetInternalState();
            return;
        }

        List<ServerPlayerEntity> naturalSources = new ArrayList<>();
        boolean extinguishRequested = false;

        for (ServerPlayerEntity player : players) {
            if (!player.isAlive() || player.getAbilities().invulnerable) {
                continue;
            }

            if (player.isTouchingWaterOrRain()) {
                extinguishRequested = true;
            }

            if (player.getFireTicks() > 0 && !player.isTouchingWaterOrRain()) {
                naturalSources.add(player);
            }
        }

        if (extinguishRequested || naturalSources.isEmpty()) {
            extinguishAll(players);
            resetInternalState();
            return;
        }

        sharedFireActive = true;

        int maxFireTicks = 0;
        for (ServerPlayerEntity player : naturalSources) {
            maxFireTicks = Math.max(maxFireTicks, player.getFireTicks());
        }
        if (maxFireTicks <= 0) {
            maxFireTicks = 20;
        }

        managedBurningPlayers.clear();

        for (ServerPlayerEntity player : players) {
            if (!player.isAlive() || player.getAbilities().invulnerable) {
                continue;
            }

            if (player.isTouchingWaterOrRain()) {
                player.extinguish();
                continue;
            }

            boolean natural = naturalSources.contains(player);
            int desiredTicks = natural ? Math.max(player.getFireTicks(), maxFireTicks) : Math.max(1, maxFireTicks);
            player.setFireTicks(desiredTicks);

            if (natural) {
                naturalBurningPlayers.add(player.getUuid());
            } else {
                managedBurningPlayers.add(player.getUuid());
            }
        }
    }

    public boolean shouldCancelFireDamage(ServerPlayerEntity player, DamageSource source) {
        if (!sharedFireActive) {
            return false;
        }
        if (!source.isOf(DamageTypes.ON_FIRE) && !source.isOf(DamageTypes.IN_FIRE)) {
            return false;
        }
        return managedBurningPlayers.contains(player.getUuid());
    }

    public void resetState() {
        extinguishAll(collectPlayers());
        resetInternalState();
    }

    private void extinguishAll(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (!player.isAlive()) {
                continue;
            }
            player.extinguish();
        }
    }

    private void resetInternalState() {
        sharedFireActive = false;
        naturalBurningPlayers.clear();
        managedBurningPlayers.clear();
    }

    private List<ServerPlayerEntity> collectPlayers() {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            players.addAll(world.getPlayers());
        }
        return players;
    }
}
