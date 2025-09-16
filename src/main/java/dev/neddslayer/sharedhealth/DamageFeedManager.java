package dev.neddslayer.sharedhealth;

import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class DamageFeedManager {
    private static final String OBJECTIVE_NAME = "damageFeed";
    private static final int MAX_ENTRIES = 8;

    private final MinecraftServer server;
    private ScoreboardObjective damageObjective;
    private final LinkedList<DamageFeedEntry> entries = new LinkedList<>();
    private final Map<Integer, String> slotToScoreHolderMap = new HashMap<>();
    private long lastUpdateTime = 0;

    public DamageFeedManager(MinecraftServer server) {
        this.server = server;
        initializeScoreboard();
        // Initialize with placeholder entries
        for (int i = 0; i < MAX_ENTRIES; i++) {
            entries.add(new DamageFeedEntry());
        }
        updateScoreboard();
    }

    private void initializeScoreboard() {
        Scoreboard scoreboard = server.getScoreboard();

        ScoreboardObjective existing = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }

        damageObjective = scoreboard.addObjective(
            OBJECTIVE_NAME,
            ScoreboardCriterion.DUMMY,
            Text.literal("Damage").formatted(Formatting.RED, Formatting.BOLD),
            ScoreboardCriterion.RenderType.INTEGER,
            false,
            BlankNumberFormat.INSTANCE
        );

        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, damageObjective);
    }

    public void addDamageEntry(String playerName, float damageAmount, String damageSource) {
        DamageFeedEntry newEntry = new DamageFeedEntry(playerName, damageAmount, damageSource);

        // Remove oldest non-placeholder entry if we're at max real entries
        int realEntryCount = 0;
        for (DamageFeedEntry entry : entries) {
            if (!entry.isPlaceholder()) {
                realEntryCount++;
            }
        }

        if (realEntryCount >= MAX_ENTRIES) {
            // Remove the oldest non-placeholder entry
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (!entries.get(i).isPlaceholder()) {
                    entries.remove(i);
                    break;
                }
            }
        } else {
            // Remove a placeholder from the end
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i).isPlaceholder()) {
                    entries.remove(i);
                    break;
                }
            }
        }

        // Add new entry at the beginning
        entries.addFirst(newEntry);
        updateScoreboard();
    }

    public void tick() {
        long currentTime = System.currentTimeMillis();

        // Check every 500ms for expired entries and color updates
        if (currentTime - lastUpdateTime < 500) {
            return;
        }
        lastUpdateTime = currentTime;

        // Remove expired entries and replace with placeholders
        for (int i = 0; i < entries.size(); i++) {
            DamageFeedEntry entry = entries.get(i);
            if (!entry.isPlaceholder() && entry.isExpired()) {
                entries.set(i, new DamageFeedEntry());
            }
        }

        // Ensure we always have exactly MAX_ENTRIES
        while (entries.size() < MAX_ENTRIES) {
            entries.add(new DamageFeedEntry());
        }
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }

        // Always update to handle color transitions
        updateScoreboard();
    }

    private void updateScoreboard() {
        Scoreboard scoreboard = server.getScoreboard();

        // Clear existing scores
        for (String holderName : slotToScoreHolderMap.values()) {
            ScoreHolder holder = ScoreHolder.fromName(holderName);
            scoreboard.removeScore(holder, damageObjective);
        }
        slotToScoreHolderMap.clear();

        // Add entries with proper ordering
        int score = MAX_ENTRIES;
        for (int i = 0; i < entries.size() && i < MAX_ENTRIES; i++) {
            DamageFeedEntry entry = entries.get(i);

            // Use unique invisible characters for each slot to maintain order
            String uniqueHolder = generateUniqueHolder(i);

            ScoreHolder scoreHolder = ScoreHolder.fromName(uniqueHolder);
            ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, damageObjective);
            scoreAccess.setScore(score);

            // Set display text with appropriate color
            scoreAccess.setDisplayText(entry.getFormattedText());

            slotToScoreHolderMap.put(i, uniqueHolder);
            score--;
        }
    }

    private String generateUniqueHolder(int slot) {
        // Use formatting codes to create unique invisible entries
        StringBuilder holder = new StringBuilder();
        for (int i = 0; i <= slot; i++) {
            holder.append("§r");
        }
        return holder.toString();
    }

    public void clearFeed() {
        Scoreboard scoreboard = server.getScoreboard();
        for (String holderName : slotToScoreHolderMap.values()) {
            ScoreHolder holder = ScoreHolder.fromName(holderName);
            scoreboard.removeScore(holder, damageObjective);
        }
        entries.clear();
        slotToScoreHolderMap.clear();

        // Re-add placeholders
        for (int i = 0; i < MAX_ENTRIES; i++) {
            entries.add(new DamageFeedEntry());
        }
        updateScoreboard();
    }
}