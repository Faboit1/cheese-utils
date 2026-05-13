# CheeseUtils

CheeseUtils is a Paper utility plugin for SMP-style servers that bundles combat tagging, teleport tools, homes, warps, spawn management, daily rewards, player settings, payments, and migration helpers into one plugin.

## Requirements

- Java 21
- Paper/Purpur 1.21.x

## Features

- Combat tagging with blocked commands, logout punishment, and bypass support
- TPA and TPA Here requests with toggles, cooldowns, expiry, and warmups
- Spawn management with first-join and join teleport options
- Player homes with configurable limits and warmups
- Named warps with categories and admin controls
- Daily reward tracking with streak support
- Player payments with cooldowns, tax, and formula-based daily limits
- Per-player settings states managed through `/settings`
- UltimateHomes migration commands
- SQLite by default with optional MySQL storage

## Installation

1. Build the plugin jar with Gradle or download a built artifact from CI.
2. Place the jar in your server `plugins/` folder.
3. Start the server once to generate configuration files.
4. Edit the generated files in `/plugins/CheeseUtils/`.
5. Restart the server or reload the relevant features.

## Building

```bash
./gradlew build
```

The project uses Gradle Kotlin DSL, Java 21, and Paperweight userdev.

## Configuration

Generated files:

- `/plugins/CheeseUtils/config.yml`
- `/plugins/CheeseUtils/messages.yml`
- `/plugins/CheeseUtils/settings.yml`

Important configuration areas in `config.yml`:

- `database`: SQLite or MySQL backend settings
- `combat`: combat timer, blocked commands, logout punishment
- `tpa`: request expiry, cooldown, warmup, combat restrictions
- `spawn`: join behavior and warmup settings
- `homes`: default limits and teleport warmup
- `warp`: warp cooldowns
- `daily`: cooldown and reward commands
- `pay`: minimum, tax, cooldown, and daily limit formula
- `settings`: GUI behavior

### Payment formula placeholders

The `pay.daily-limit-formula` field supports:

- `{daysplayed}`
- `{kills}`
- `{deaths}`
- `{balance}`

Example:

```yaml
daily-limit-formula: "1000*{daysplayed}+10000"
```

## Commands

- `/combatstatus`
- `/combatreload`
- `/tpa <player>`
- `/tpahere <player>`
- `/tpaccept`
- `/tpdeny`
- `/tpauto`
- `/tpatoggle`
- `/tpaheretoggle`
- `/tpaguitoggle`
- `/setspawn`
- `/spawn`
- `/setwarp <name> [category]`
- `/warp <name>`
- `/delwarp <name>`
- `/warps`
- `/daily`
- `/pay <player> <amount>`
- `/paytoggle`
- `/payblock`
- `/home <name>`
- `/sethome <name>`
- `/delhome <name>`
- `/homes`
- `/settings`

## Permissions

- `cheeseutils.admin`
- `cheeseutils.combat.bypass`
- `cheeseutils.combat.reload`
- `cheeseutils.spawn.set`
- `cheeseutils.warp.admin`
- `cheeseutils.pay.bypasslimit`
- `cheeseutils.homes.max.<N>`

## Optional integrations

- PlaceholderAPI
- Vault
- DecentHolograms

## CI

GitHub Actions runs `./gradlew build` for pushes and pull requests.
