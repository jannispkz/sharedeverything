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

        List<ServerPlayerEntity> submergedPlayers = new ArrayList<>();
        int computedMaxAir = 0;

        for (ServerPlayerEntity player : players) {
            if (!player.isAlive()) {
                continue;
            }

            computedMaxAir = Math.max(computedMaxAir, player.getMaxAir());

            boolean invulnerable = player.getAbilities().invulnerable;
            boolean canBreathe = player.canBreatheInWater();
            boolean isSubmerged = player.isSubmergedInWater() || player.isInsideWaterOrBubbleColumn();

            if (!invulnerable && isSubmerged && !canBreathe) {
                submergedPlayers.add(player);
            }
        }

        if (computedMaxAir <= 0) {
            computedMaxAir = DEFAULT_MAX_AIR;
        }
        if (computedMaxAir != sharedMaxAir) {
            component.setMaxAir(computedMaxAir);
            sharedMaxAir = computedMaxAir;
            sharedAir = Math.min(sharedAir, sharedMaxAir);
        }

        int newAir = sharedAir;
        if (!submergedPlayers.isEmpty()) {
            newAir = Math.max(sharedAir - submergedPlayers.size(), 0);
        } else if (sharedAir < sharedMaxAir) {
            newAir = Math.min(sharedMaxAir, sharedAir + REGEN_RATE);
        }

        component.setAir(newAir);

        if (newAir == 0 && !submergedPlayers.isEmpty()) {
            drowningTicks += 1;
            int events = drowningTicks / DAMAGE_INTERVAL;
            if (events > 0) {
                applyDrowningDamage(submergedPlayers, events);
                drowningTicks -= events * DAMAGE_INTERVAL;
            }
        } else {
            drowningTicks = 0;
        }

        component.setDrowningTicks(Math.max(0, drowningTicks));

        int finalAir = component.getAir();
        for (ServerPlayerEntity player : players) {
            if (!player.isAlive()) {
                continue;
            }
            if (player.getAbilities().invulnerable) {
                player.setAir(player.getMaxAir());
                continue;
            }

            int clampedAir = Math.min(finalAir, player.getMaxAir());
            player.setAir(clampedAir);
        }
    }

    private List<ServerPlayerEntity> collectPlayers() {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            players.addAll(world.getPlayers());
        }
        return players;
    }

    private void applyDrowningDamage(List<ServerPlayerEntity> submergedPlayers, int events) {
        if (events <= 0) {
            return;
        }

        if (submergedPlayers.isEmpty()) {
            return;
        }

        boolean sharedHealthEnabled = submergedPlayers.get(0).getServerWorld().getGameRules().getBoolean(SharedHealth.SYNC_HEALTH);

        if (sharedHealthEnabled) {
            float totalDamage = BASE_DROWN_DAMAGE * events * submergedPlayers.size();
            ServerPlayerEntity target = submergedPlayers.get(0);
            if (target.isAlive() && !target.getAbilities().invulnerable) {
                SharedHealth.setPendingSharedAir(target.getUuid(), BASE_DROWN_DAMAGE * events);
                try {
                    target.damage(target.getServerWorld(), target.getServerWorld().getDamageSources().drown(), totalDamage);
                } finally {
                    SharedHealth.clearPendingSharedAir();
                }

                if (SharedHealth.damageFeedManager != null && submergedPlayers.size() > 1) {
                    for (int i = 1; i < submergedPlayers.size(); i++) {
                        ServerPlayerEntity extraPlayer = submergedPlayers.get(i);
                        SharedHealth.damageFeedManager.addDamageEntry(
                            extraPlayer.getName().getString(),
                            BASE_DROWN_DAMAGE * events,
                            "drowning"
                        );
                    }
                }
            }
        } else {
            float damageAmount = BASE_DROWN_DAMAGE * events;
            for (ServerPlayerEntity player : submergedPlayers) {
                if (!player.isAlive() || player.getAbilities().invulnerable) {
                    continue;
                }
                player.damage(player.getServerWorld(), player.getServerWorld().getDamageSources().drown(), damageAmount);
            }
        }
    }
}
