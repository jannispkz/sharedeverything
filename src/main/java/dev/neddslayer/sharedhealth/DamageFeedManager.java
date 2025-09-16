package dev.neddslayer.sharedhealth;

import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

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
        // Don't add placeholders initially - scoreboard starts hidden
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
            Text.empty(), // Completely empty text
            ScoreboardCriterion.RenderType.INTEGER,
            false,
            BlankNumberFormat.INSTANCE
        );

        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, damageObjective);
    }

    public void addDamageEntry(String playerName, float damageAmount, String damageSource) {
        DamageFeedEntry newEntry = new DamageFeedEntry(playerName, damageAmount, damageSource);

        // Remove oldest entry if we're at max entries
        if (entries.size() >= MAX_ENTRIES) {
            entries.removeLast();
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

        // Remove expired entries completely
        int sizeBefore = entries.size();
        entries.removeIf(entry -> !entry.isPlaceholder() && entry.isExpired());
        boolean hasChanges = entries.size() != sizeBefore;

        // Always update to handle color transitions or if entries were removed
        if (hasChanges || !entries.isEmpty()) {
            updateScoreboard();
        }
    }

    private void updateScoreboard() {
        Scoreboard scoreboard = server.getScoreboard();

        // Clear existing scores
        for (String holderName : slotToScoreHolderMap.values()) {
            ScoreHolder holder = ScoreHolder.fromName(holderName);
            scoreboard.removeScore(holder, damageObjective);
        }
        slotToScoreHolderMap.clear();

        // If no entries, scoreboard will be empty (hidden)
        if (entries.isEmpty()) {
            return;
        }

        // Add entries with proper ordering
        int score = MAX_ENTRIES;
        for (int i = 0; i < entries.size() && i < MAX_ENTRIES; i++) {
            DamageFeedEntry entry = entries.get(i);

            // Skip placeholder entries
            if (entry.isPlaceholder()) {
                continue;
            }

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
        // Don't add placeholders - let scoreboard disappear
        updateScoreboard();
    }
}