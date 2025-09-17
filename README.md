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
  - All players share the same air supply (oxygen bubbles)
  - Mechanics:
    - Air drains per submerged player who cannot breathe
    - Drowning damage hits everyone equally when air hits zero
    - Players in bubble columns (magma/soul sand) don’t drain the shared supply
  - Air regenerates only when nobody is drowning

- **Shared Fire** (`/gamerule shareFire`)
  - When one player is on fire, the burn spreads to everyone
  - Natural burn sources define the shared burn duration
  - Water/rain extinguishes everyone together
  - Shared fire damage is canceled for teammates who only received fire from sync

- **Shared Freeze** (`/gamerule shareFreeze`)
  - Powder snow freezing spreads to every teammate
  - Only natural powder-snow victims take freeze damage; synced players keep the visual freeze without damage

### 🎮 Core Speedrun System

- **Countdown Timer** (`/countdownstart`, `/countdownstop`)
  - Displays timer in action bar
  - Pre-countdown lobby (5 seconds): blindness, slowness, mining fatigue, glowing
  - Lobby lockdown:
    - Inventories cleared and players silently respawned at spawn
    - World border clamped to 16-block radius and gradually expands after start
    - `doImmediateRespawn` turned on
    - No hunger drain, block breaking, or damage during lobby
  - Start signal: “Timer lööft” title + goat horn
  - Persistent glowing re-applied every 3 seconds during runs

- **Damage Feed Sidebar**
  - Tracks last eight damage events with color-coded aging
  - Updates every 500ms; clears automatically
  - Reset using `/resetscoreboard`

- **Death System Overhaul**
  - Team wipes only trigger if countdown is active
  - “EVERYONE DIED” title and wither spawn sound
  - Displays death summary (deaths, kills, damage, distance)
  - Records the run in the historical log with failure cause
  - Auto world deletion 30 seconds after wipe

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
