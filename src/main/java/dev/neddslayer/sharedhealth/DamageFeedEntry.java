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
        return System.currentTimeMillis() - timestamp < 2000; // 2 seconds
    }

    public Text getFormattedText() {
        if (isPlaceholder) {
            return Text.literal("");
        }

        String formattedDamage;
        if (damageAmount < 1) {
            formattedDamage = String.format("%.1f", damageAmount);
        } else {
            formattedDamage = String.valueOf((int) Math.floor(damageAmount));
        }

        String entryText = String.format("%s %s damage by %s",
            formattedDamage,
            damageSource,
            playerName);

        // Return red text if new (< 2 seconds), gray otherwise
        if (isNew()) {
            return Text.literal(entryText).formatted(Formatting.RED);
        } else {
            return Text.literal(entryText).formatted(Formatting.GRAY);
        }
    }
}