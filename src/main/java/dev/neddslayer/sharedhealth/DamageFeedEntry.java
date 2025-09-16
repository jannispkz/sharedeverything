package dev.neddslayer.sharedhealth;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DamageFeedEntry {
    private final String playerName;
    private final float damageAmount;
    private final String damageSource;
    private final long timestamp;
    private final boolean isPlaceholder;

    public DamageFeedEntry(String playerName, float damageAmount, String damageSource) {
        this.playerName = playerName;
        this.damageAmount = damageAmount;
        this.damageSource = damageSource;
        this.timestamp = System.currentTimeMillis();
        this.isPlaceholder = false;
    }

    // Constructor for placeholder entries
    public DamageFeedEntry() {
        this.playerName = "";
        this.damageAmount = 0;
        this.damageSource = "";
        this.timestamp = 0;
        this.isPlaceholder = true;
    }

    public String getPlayerName() {
        return playerName;
    }

    public float getDamageAmount() {
        return damageAmount;
    }

    public String getDamageSource() {
        return damageSource;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isPlaceholder() {
        return isPlaceholder;
    }

    public boolean isExpired() {
        if (isPlaceholder) return false;
        return System.currentTimeMillis() - timestamp > 10000; // 10 seconds
    }

    public boolean isNew() {
        if (isPlaceholder) return false;
        return System.currentTimeMillis() - timestamp < 1000; // 1 second
    }

    public boolean isAboutToExpire() {
        if (isPlaceholder) return false;
        long age = System.currentTimeMillis() - timestamp;
        return age > 8000; // Last 2 seconds (8-10 seconds)
    }

    public Text getFormattedText() {
        if (isPlaceholder) {
            // Should never be displayed now
            return Text.literal("");
        }

        String formattedDamage;
        if (damageAmount < 1) {
            formattedDamage = String.format("%.1f", damageAmount);
        } else {
            formattedDamage = String.valueOf((int) Math.floor(damageAmount));
        }

        String entryText = String.format("%s took %s damage from %s",
            playerName,
            formattedDamage,
            damageSource);

        // Color based on age: red (< 1s), dark gray (8-10s), gray (1-8s)
        if (isNew()) {
            return Text.literal(entryText).formatted(Formatting.RED);
        } else if (isAboutToExpire()) {
            return Text.literal(entryText).formatted(Formatting.DARK_GRAY);
        } else {
            return Text.literal(entryText).formatted(Formatting.GRAY);
        }
    }
}