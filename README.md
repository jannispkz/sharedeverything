# SharedHealth Speedrun Edition (1.21.4)

A fork of [Neddslayer's SharedHealth](https://github.com/Neddslayer/sharedhealth) mod with extensive speedrun and challenge features.

## Why This Fork Exists

I found the original SharedHealth mod and noticed it skipped 1.21.4, so I thought "how hard could it be to port it?" Well, after successfully porting it, I started adding features my friends and I wanted for our speedrun challenges. One thing led to another, and it evolved into this speedrun-focused hybrid. So if you like this kind of stuff feel free to use it.

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
- **Shared Air/Oxygen** (`/gamerule shareAir`) – Advanced underwater breathing mechanics
- **Shared Fire** (`/gamerule shareFire`) – Fire damage and burning state synchronized
- **Shared Freeze** (`/gamerule shareFreeze`) – Powder-snow freezing mirrors across the team
- **Countdown Timer System** (`/countdownstart`, `/countdownstop`)
- **Real-time Damage Feed** (sidebar scoreboard)
- **Death System with Statistics** (team wipe mechanics)
- **Ender Dragon Victory Celebration** (fireworks, music, effects)
- **Coordinate Sharing** (`/coords <label>`)
- **World Reset Commands** (`/reset`, automatic deletion)
- **Auto World Preservation** (victory worlds saved as `victory_{timestamp}`)
- **Run Leaderboard & History** (`/leaderboard`, `/history`) with persistent storage in `config/sharedhealth_runs.json`

---

## Detailed Feature Descriptions

### 🔄 Additional Sharing Options

- **Shared Status Effects** (`/gamerule shareStatusEffects`)
  - All players receive eligible potion effects together
  - Synchronized duration and amplifier

- **Shared Experience** (`/gamerule shareExperience`)
  - XP orbs collected by one player benefit all
  - Shared XP levels and points

- **Shared Ender Pearls** (`/gamerule shareEnderPearls`)
  - Using an ender pearl teleports everyone to the throw location
  - Works across dimensions

- **Shared Air/Oxygen** (`/gamerule shareAir`)
  - Shared bubble meter; drowning spreads to the team

- **Shared Fire** (`/gamerule shareFire`)
  - Burn state mirrors across the group, but only natural ignitions deal damage
  - Water or rain extinguishes all players simultaneously

- **Shared Freeze** (`/gamerule shareFreeze`)
  - Powder-snow freezing syncs visually while only natural targets take damage

### 🎮 Core Speedrun System

- **Countdown Timer** (`/countdownstart`, `/countdownstop`)
  - Timer in the action bar with a 5s lobby that wipes inventories, tightens the border, freezes hunger/damage, and respawns everyone instantly
  - Launch cue (“Timer lööft”) plus goat horn; glowing reapplies every few seconds during the run

- **Damage Feed Sidebar** – Rolling log of the last eight damage events (auto expires, reset with `/resetscoreboard`)

- **Death System Overhaul** – Shared-health wipes trigger only during active runs, broadcast a summary, log the attempt, and delete the world 30 seconds later

### 🐉 Ender Dragon Victory Celebration

*(Only triggers if countdown was active)*

- Ender Dragon victory event with cool effects and automatic player protection
- World is preserved as `victory_{timestamp}` and the run is logged for the leaderboard

### 📝 Utility Commands

- **`/coords <label>`** – Share coordinates + dimension tag to all players
- **`/reset`** – Immediate world deletion and server shutdown (no confirmation)
- **`/resetscoreboard`** – Clear damage feed sidebar
- **`/leaderboard`** – Show top 5 recorded runs (victories first, faster runs first)
- **`/history`** – Show your last 5 runs (victory/failure, time, MVP/cause)

### 🧾 Run Tracking & MVP

- Runs (victory or failure) are saved to `config/sharedhealth_runs.json`
- Each entry stores:
  - Victory status
  - Duration
  - Participants list
  - MVP (player with highest damage + kills score)
  - Failure cause (first death message) when applicable
  - Timestamp

### 🔧 Technical Improvements

- Tab list names update across dimensions
- Proper shutdown manager with world cleanup
- Countdown state tightly controlled to avoid exploits
- Lobby boundaries animate from radius 16 → 100 → 1,000,000 during start

---

## ⚠️ IMPORTANT WARNINGS

1. **This mod WILL DELETE YOUR WORLD when:**
   - Any player dies during an active countdown (30 seconds later)
   - Someone runs `/reset`
   - (Safe) Deaths outside countdown do not delete the world

2. **Victory worlds are preserved** as `victory_{timestamp}` folders
   - Clean them up periodically if storage matters

3. **`/reset` is unprotected**
   - Any player can run it and nuke the world immediately
   - Designed for private friend groups, not public servers

---

## Installation

1. Requires Fabric Loader 0.17.2+
2. Requires Fabric API
3. Requires Cardinal Components API
4. Built for Minecraft 1.21.4

> **Note:** Mod is built for server-side use. Clients do not need the mod installed.

### Recommended Companion Mod

Use [SharedInv](https://modrinth.com/mod/sharedinv) to share inventories for a fully synchronized experience.

---

## Configuration

Everything is controlled via gamerules and commands – no manual config required (other than the automatically generated run history file).

---

## License

Original SharedHealth mod by Neddslayer (GPL-3.0); this fork inherits the same license.
