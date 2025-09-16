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
    private final Map<DamageFeedEntry, String> entryToScoreHolderMap = new HashMap<>();
    private int uniqueIdCounter = 0;

    public DamageFeedManager(MinecraftServer server) {
        this.server = server;
        initializeScoreboard();
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

        if (entries.size() >= MAX_ENTRIES) {
            DamageFeedEntry oldestEntry = entries.removeLast();
            removeScoreboardEntry(oldestEntry);
        }

        entries.addFirst(newEntry);
        updateScoreboard();
    }

    private void removeScoreboardEntry(DamageFeedEntry entry) {
        if (entryToScoreHolderMap.containsKey(entry)) {
            String scoreHolderName = entryToScoreHolderMap.get(entry);
            ScoreHolder scoreHolder = ScoreHolder.fromName(scoreHolderName);
            server.getScoreboard().removeScore(scoreHolder, damageObjective);
            entryToScoreHolderMap.remove(entry);
        }
    }

    private void updateScoreboard() {
        Scoreboard scoreboard = server.getScoreboard();

        for (String holderName : entryToScoreHolderMap.values()) {
            ScoreHolder holder = ScoreHolder.fromName(holderName);
            scoreboard.removeScore(holder, damageObjective);
        }
        entryToScoreHolderMap.clear();

        int score = MAX_ENTRIES;
        for (DamageFeedEntry entry : entries) {
            String uniqueHolder = "§" + uniqueIdCounter++;
            String displayText = entry.getFormattedEntry();

            ScoreHolder scoreHolder = ScoreHolder.fromName(uniqueHolder);
            ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, damageObjective);
            scoreAccess.setScore(score);

            scoreAccess.setDisplayText(Text.literal(displayText));

            entryToScoreHolderMap.put(entry, uniqueHolder);
            score--;
        }
    }

    public void clearFeed() {
        Scoreboard scoreboard = server.getScoreboard();
        for (String holderName : entryToScoreHolderMap.values()) {
            ScoreHolder holder = ScoreHolder.fromName(holderName);
            scoreboard.removeScore(holder, damageObjective);
        }
        entries.clear();
        entryToScoreHolderMap.clear();
    }
}