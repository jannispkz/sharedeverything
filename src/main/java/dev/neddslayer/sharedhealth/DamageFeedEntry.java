package dev.neddslayer.sharedhealth;

public class DamageFeedEntry {
    private final String playerName;
    private final float damageAmount;
    private final String damageSource;
    private final long timestamp;

    public DamageFeedEntry(String playerName, float damageAmount, String damageSource) {
        this.playerName = playerName;
        this.damageAmount = damageAmount;
        this.damageSource = damageSource;
        this.timestamp = System.currentTimeMillis();
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

    public String getFormattedEntry() {
        String formattedDamage = damageAmount == (int) damageAmount ?
            String.valueOf((int) damageAmount) :
            String.format("%.1f", damageAmount);

        return String.format("%s %s damage by %s",
            formattedDamage,
            damageSource,
            playerName);
    }
}