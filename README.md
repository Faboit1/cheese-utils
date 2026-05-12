# CheeseUtils

Production-focused Paper/Purpur SMP utility plugin bundle.

## Stack
- Java 21
- Gradle Kotlin DSL
- Paper API + Adventure + MiniMessage
- SQLite default, optional MySQL

## Features included
- Donut-style Combat Tag (`/combatstatus`, `/combatreload`)
- TPA suite (`/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, toggles)
- Spawn system (`/setspawn`, `/spawn`)
- Warps (`/setwarp`, `/warp`, `/delwarp`, `/warps`)
- Homes (`/sethome`, `/home`, `/delhome`, `/homes`)
- Daily rewards (`/daily`)
- Pay system + formula limits (`/pay`, `/paytoggle`, `/payblock`)
- Settings framework (`/settings`) with unlimited per-setting states
- UltimateHomes migration (`/homes migrate ultimatehomes`, `/homes migrate dryrun`)

## Config files
All configs are generated in:
`/plugins/CheeseUtils/`
- `config.yml`
- `messages.yml`
- `settings.yml`

## Permissions
- `cheeseutils.admin` (grants all admin nodes)
- `cheeseutils.combat.bypass`
- `cheeseutils.combat.reload`
- `cheeseutils.spawn.set`
- `cheeseutils.warp.admin`
- `cheeseutils.pay.bypasslimit`
- `cheeseutils.homes.max.<N>`

## Placeholders used in formulas/messages
- Formula: `{daysplayed}`, `{kills}`, `{deaths}`, `{balance}`
- Messages/config actions: `{player}`, `{state}`, `{setting}`, `{seconds}`, `{name}`, `{amount}`, `{streak}`

## CI
GitHub Actions workflow included to auto-compile and run tests on push/PR.
