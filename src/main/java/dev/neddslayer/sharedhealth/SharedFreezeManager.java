package dev.neddslayer.sharedhealth;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SharedFreezeManager {
    private final MinecraftServer server;
    private final Set<UUID> managedFrozenPlayers = new HashSet<>();
    private boolean sharedFreezeActive = false;

    public SharedFreezeManager(MinecraftServer server) {
        this.server = server;
    }

    public void tick(boolean enabled) {
        List<ServerPlayerEntity> players = collectPlayers();

        if (!enabled) {
            if (sharedFreezeActive) {
                thawAll(players);
            }
            resetInternalState();
            return;
        }

        List<ServerPlayerEntity> naturalSources = new ArrayList<>();
        managedFrozenPlayers.clear();

        for (ServerPlayerEntity player : players) {
            if (!player.isAlive() || player.getAbilities().invulnerable) {
                continue;
            }

            if (player.getFrozenTicks() > 0 && isInPowderSnow(player)) {
                naturalSources.add(player);
            }
        }

        if (naturalSources.isEmpty()) {
            thawAll(players);
            resetInternalState();
            return;
        }

        sharedFreezeActive = true;

        int maxFrozenTicks = 0;
        for (ServerPlayerEntity player : naturalSources) {
            maxFrozenTicks = Math.max(maxFrozenTicks, player.getFrozenTicks());
        }
        if (maxFrozenTicks <= 0) {
            maxFrozenTicks = 1;
        }

        for (ServerPlayerEntity player : players) {
            if (!player.isAlive() || player.getAbilities().invulnerable) {
                continue;
            }

            boolean natural = naturalSources.contains(player);

            int desiredTicks = natural ? Math.max(player.getFrozenTicks(), maxFrozenTicks) : Math.max(1, maxFrozenTicks);
            player.setFrozenTicks(desiredTicks);

            if (!natural) {
                managedFrozenPlayers.add(player.getUuid());
            }
        }
    }

    public boolean shouldCancelFreezeDamage(ServerPlayerEntity player, DamageSource source) {
        if (!sharedFreezeActive) {
            return false;
        }
        if (!source.isOf(DamageTypes.FREEZE)) {
            return false;
        }
        return managedFrozenPlayers.contains(player.getUuid());
    }

    public void resetState() {
        thawAll(collectPlayers());
        resetInternalState();
    }

    private void thawAll(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (!player.isAlive()) {
                continue;
            }
            player.setFrozenTicks(0);
        }
    }

    private void resetInternalState() {
        sharedFreezeActive = false;
        managedFrozenPlayers.clear();
    }

    private List<ServerPlayerEntity> collectPlayers() {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            players.addAll(world.getPlayers());
        }
        return players;
    }

    private boolean isInPowderSnow(ServerPlayerEntity player) {
        Box box = player.getBoundingBox();

        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX + 1.0E-6);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY + 1.0E-6);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ + 1.0E-6);

        for (BlockPos pos : BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.isOf(Blocks.POWDER_SNOW)) {
                return true;
            }
        }

        return false;
    }
}
