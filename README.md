# SharedHealth Speedrun Edition (1.21.4)

A fork of [Neddslayer's SharedHealth](https://github.com/Neddslayer/sharedhealth) mod with extensive speedrun and challenge features.

## Why This Fork Exists

I found the original SharedHealth mod and noticed it skipped 1.21.4, so I thought "how hard could it be to port it?" Well, after successfully porting it, I started adding features my friends and I wanted for our speedrun challenges. One thing led to another, and it evolved into this speedrun-focused hybrid that we use regularly. If you enjoy this kind of competitive Minecraft gameplay, feel free to use it as well (at your own risk)!

⚠️ **WARNING: This mod includes AUTO WORLD DELETION features for speedrun resets. Read carefully before using!** ⚠️

---

## Original Features (by Neddslayer)

- **Shared Health**: All players share the same health pool (`/gamerule shareHealth`)
- **Shared Hunger**: All players share the same hunger bar (`/gamerule shareHunger`)
- **Shared Saturation & Exhaustion**: Hunger mechanics fully synchronized
- **Health Limiting**: Cap shared health at 20 (`/gamerule limitHealth`)

## Features I Added

- **Shared Status Effects** (`/gamerule shareStatusEffects`)
- **Shared Experience** (`/gamerule shareExperience`)
- **Shared Ender Pearls** (`/gamerule shareEnderPearls`)
- **Countdown Timer System** (`/countdownstart`, `/countdownstop`)
- **Real-time Damage Feed** (sidebar scoreboard)
- **Death System with Statistics** (team wipe mechanics)
- **Ender Dragon Victory Celebration** (fireworks, music, effects)
- **Coordinate Sharing** (`/coords <label>`)
- **World Reset Commands** (`/reset`, automatic deletion)
- **Auto World Preservation** (victory worlds saved as `victory_{timestamp}`)

---

## Detailed Feature Descriptions

### 🔄 Additional Sharing Options

- **Shared Status Effects** (`/gamerule shareStatusEffects`)
  - All players receive the same potion effects (depending on which one)
  - Synchronized duration and amplifier

- **Shared Experience** (`/gamerule shareExperience`)
  - XP orbs collected by one player benefit all
  - Shared XP levels and points

- **Shared Ender Pearls** (`/gamerule shareEnderPearls`)
  - When one player uses an ender pearl, all players teleport
  - Useful for keeping teams together
  - Works across dimensions

### 🎮 Core Speedrun System

- **Countdown Timer** (`/countdownstart`, `/countdownstop`)
  - Displays persistent timer in action bar
  - Kills all players and resets them at spawn
  - Applies blindness, slowness, and mining fatigue during pre-countdown
  - Shows "Timer lööft" when starting (lööft meant runs in german but weirdly written)
  - Persistent glowing effect on all players (reapplied every 3 seconds)
  - Clears player inventories on start

- **Damage Feed Sidebar**
  - Real-time damage tracking displayed on the right side of screen
  - Shows last 8 damage events across all players
  - Format: `PlayerName took X damage from Source`
    - Damage < 1: Shows decimal (e.g., "0.5")
    - Damage ≥ 1: Shows whole number (e.g., "6")
  - Color-coded by age:
    - Red: Fresh damage (< 1 second)
    - Gray: Recent damage (1-4 seconds)
    - Dark Gray: Old damage (4-6 seconds)
    - Auto-removes after 6 seconds
  - Updates every 500ms for smooth transitions
  - Automatically hides when empty
  - Reset with `/resetscoreboard` 

- **Death System Overhaul**
  - Team death only triggers during active countdown
  - "EVERYONE DIED" title with wither spawn sound
  - Shows death statistics (deaths, kills, damage dealt/taken, distance walked)
  - **Automatic world deletion after 30 seconds**

### 🐉 Ender Dragon Victory Celebration

**Only triggers if countdown was active!**

- **Epic Victory Sequence**:
  - "DRAGON DEFEATED!" golden title (15 seconds)
  - Displays completion time
  - Pigstep music plays from End center (0, 68, 0)
  - Goat horn victory sound
  - Fireworks every 0.5 seconds for 10 seconds
  - Comprehensive statistics display (MVP player, total stats)

- **Player Protection**:
  - Creative mode for all players
  - Regeneration 100 (instant healing)
  - Resistance 100 (damage immunity)
  - Glowing effect
  - Fall damage disabled
  - Effects reapplied every 2 seconds (milk-proof)
  - Deaths during victory don't trigger run reset

- **End Portal Prevention**:
  - No portal spawns after dragon death
  - Players stay in The End for celebration

- **World Preservation**:
  - Victory worlds renamed to `victory_{timestamp}`
  - Failed runs delete world completely
  - 60-second countdown before server reset

### 📝 Utility Commands

- **`/coords <label>`**: Share coordinates with all players
  - Displays as blue text with dimension indicator
  - Example: "123, 64, -456 [Base] [Nether]"

- **`/reset`**: Force immediate world reset
  - Instantly deletes world and restarts server
  - No countdown or warning

- **`/resetscoreboard`**: Clear the damage feed display

### 🔧 Technical Improvements

- Tab list player names update across dimensions
- Death sounds play correctly from any dimension
- Shutdown manager with proper world cleanup
- Enhanced component synchronization
- Proper countdown state management

---

## ⚠️ IMPORTANT WARNINGS

1. **This mod WILL DELETE YOUR WORLD when:**
   - Players die during an active countdown (30 seconds after death)
   - Using the `/reset` command
   - NOT when dying without active countdown

2. **Victory worlds are PRESERVED as `victory_{timestamp}` folders**
   - These accumulate over time
   - Manual cleanup required

3. **The `/reset` command has NO CONFIRMATION**
   - Immediately deletes world
   - Use with extreme caution

---

## Installation

1. Requires Fabric Loader 0.17.2+
2. Requires Fabric API
3. Requires Cardinal Components API
4. Built for Minecraft 1.21.4

---

## Configuration

All features controlled via gamerules and commands. No config file needed.

Default gamerules can be modified in server.properties or via commands.

---

## License

Original SharedHealth mod by Neddslayer (GPL-3.0)
This fork maintains the same license

---

## Credits

- Original mod by [Neddslayer](https://github.com/Neddslayer)
- Speedrun features added for competitive challenge runs
- Built for Minecraft 1.21.4
