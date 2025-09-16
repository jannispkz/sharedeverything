package dev.neddslayer.sharedhealth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CountdownManager {
    private final MinecraftServer server;
    private long startTime;
    private boolean isActive;
    private boolean isPreCountdown;
    private int preCountdownTicks;
    private static final int PRE_COUNTDOWN_DELAY = 100; // 5 seconds * 20 ticks

    public CountdownManager(MinecraftServer server) {
        this.server = server;
        this.startTime = 0;
        this.isActive = false;
        this.isPreCountdown = false;
        this.preCountdownTicks = 0;
    }

    public void start() {
        // Start the 5-second pre-countdown with blindness
        this.isPreCountdown = true;
        this.preCountdownTicks = PRE_COUNTDOWN_DELAY;

        // Give all players blindness for the pre-countdown
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.BLINDNESS, PRE_COUNTDOWN_DELAY, 0, false, false, true));
        }
    }

    private void startActualTimer() {
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
        this.isPreCountdown = false;

        // Send title and play sound to all players
        Text title = Text.literal("Timer lööft").formatted(Formatting.GREEN);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Show title using network handler
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(title));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.empty()));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 70, 20));

            // Play horn sound (note block horn)
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.MASTER, 1.0f, 0.5f);
        }
    }

    public void stop() {
        this.isActive = false;
        this.isPreCountdown = false;
    }

    public boolean isActive() {
        return isActive || isPreCountdown;
    }

    public void tick() {
        // Handle pre-countdown first
        if (isPreCountdown) {
            preCountdownTicks--;

            // Calculate seconds remaining
            int secondsLeft = (preCountdownTicks + 19) / 20;

            // Show pre-countdown every second
            if (preCountdownTicks % 20 == 0 && secondsLeft > 0) {
                Text preCountdownMessage = Text.literal("Timer starting in: " + secondsLeft).formatted(Formatting.YELLOW, Formatting.BOLD);
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(preCountdownMessage, true);
                    // Play tick sound
                    player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 1.0f, 1.0f);
                }
            }

            // Start actual timer when pre-countdown ends
            if (preCountdownTicks <= 0) {
                startActualTimer();
            }
            return;
        }

        if (!isActive) {
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;
        long totalSeconds = elapsedMillis / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String timeString = String.format("%d:%02d:%02d", hours, minutes, seconds);
        Text message = Text.literal(timeString).formatted(Formatting.WHITE);

        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(message, true);
        });
    }
}