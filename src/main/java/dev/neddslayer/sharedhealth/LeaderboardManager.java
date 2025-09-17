package dev.neddslayer.sharedhealth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    private final MinecraftServer server;
    private final Path dataFile;
    private final List<RunRecord> records = new ArrayList<>();

    public LeaderboardManager(MinecraftServer server) {
        this.server = server;
        this.dataFile = FabricLoader.getInstance().getConfigDir().resolve("sharedhealth_runs.json");
        load();
    }

    public synchronized void recordRun(boolean victory, long durationMillis, String mvp, String failureCause) {
        long sanitizedDuration = Math.max(durationMillis, 0);
        RunRecord record = new RunRecord();
        record.victory = victory;
        record.durationMillis = sanitizedDuration;
        record.timestamp = System.currentTimeMillis();
        record.participants = server.getPlayerManager().getPlayerList().stream()
            .map(player -> player.getName().getString())
            .sorted()
            .collect(Collectors.toList());
        record.mvp = mvp;
        record.failureCause = failureCause;

        records.add(record);
        save();
    }

    public synchronized List<RunRecord> getTopRecords(int limit) {
        List<RunRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator
            .comparing((RunRecord record) -> !record.victory)
            .thenComparingLong(record -> record.durationMillis)
            .thenComparingLong(record -> record.timestamp));
        if (sorted.size() > limit) {
            return new ArrayList<>(sorted.subList(0, limit));
        }
        return sorted;
    }

    public synchronized void sendLeaderboard(net.minecraft.server.command.ServerCommandSource source) {
        final int maxEntries = 5;
        source.sendFeedback(() -> Text.literal("=== Shared Health Leaderboard (Top " + maxEntries + ") ===").formatted(Formatting.GOLD, Formatting.BOLD), false);

        List<RunRecord> top = getTopRecords(maxEntries);
        for (int i = 0; i < maxEntries; i++) {
            if (i < top.size()) {
                RunRecord record = top.get(i);
                String index = String.format("%d. ", i + 1);
                String time = formatDuration(record.durationMillis);
                String participants = record.participants.isEmpty()
                    ? "(no participants)"
                    : String.join(", ", record.participants);
                String status = record.victory ? "Victory" : "Failure";
                StringBuilder line = new StringBuilder()
                    .append(index)
                    .append(status)
                    .append(" - ")
                    .append(time)
                    .append(" - Players: ")
                    .append(participants);
                if (record.victory) {
                    if (record.mvp != null && !record.mvp.isEmpty()) {
                        line.append(" - MVP: ").append(record.mvp);
                    }
                } else {
                    if (record.failureCause != null && !record.failureCause.isEmpty()) {
                        line.append(" - Cause: ").append(record.failureCause);
                    }
                    if (record.mvp != null && !record.mvp.isEmpty()) {
                        line.append(" - MVP: ").append(record.mvp);
                    }
                }
                line.append(" - ").append(DATE_FORMAT.format(Instant.ofEpochMilli(record.timestamp)));
                source.sendFeedback(() -> Text.literal(line.toString()).formatted(Formatting.GRAY), false);
            } else {
                String placeholder = String.format("%d. ---", i + 1);
                source.sendFeedback(() -> Text.literal(placeholder).formatted(Formatting.DARK_GRAY), false);
            }
        }
    }

    private void load() {
        try {
            Files.createDirectories(dataFile.getParent());
            if (!Files.exists(dataFile)) {
                save();
                return;
            }
            try (BufferedReader reader = Files.newBufferedReader(dataFile)) {
                List<RunRecord> loaded = GSON.fromJson(reader, new TypeToken<List<RunRecord>>() {}.getType());
                if (loaded != null) {
                    records.clear();
                    records.addAll(loaded);
                }
            }
        } catch (IOException e) {
            SharedHealth.LOGGER.error("Failed to load Shared Health leaderboard", e);
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(dataFile)) {
                GSON.toJson(records, writer);
            }
        } catch (IOException e) {
            SharedHealth.LOGGER.error("Failed to save Shared Health leaderboard", e);
        }
    }

    private String formatDuration(long durationMillis) {
        long totalSeconds = Math.max(durationMillis, 0) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    static class RunRecord {
        boolean victory;
        long durationMillis;
        long timestamp;
        List<String> participants = new ArrayList<>();
        String mvp;
        String failureCause;
    }
}
