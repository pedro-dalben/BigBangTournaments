# BigBang Tournaments

`bigbang_tournaments` is a server-side NeoForge mod for Minecraft `1.21.1` built for Cobblemon `1.7.3`.

It now supports competitive tournament operations without automatic bracket generation:
- participant registration
- competitive team validation
- pending correction windows with automatic revalidation
- complete roster snapshots for audit and lock enforcement
- arena setup
- manual PvP battle start through the Cobblemon API
- spectator teleport plus Cobblemon spectate integration
- automatic winner detection with manual fallback
- restore/unlock flows at the end of the event

Players do not need the mod on the client, but the server must also run:
- Cobblemon `1.7.3+1.21.1`
- Cobblemon: Mega Showdown `1.8.4+1.7.3+1.21.1`
- Accessories `1.1.0-beta.52+1.21.1`
- Architectury API `13.0.8`

## Core Behavior

### Validation before prepare
`/tournament prepare <player> <level>` validates the active party before locking it.

The validator checks:
- empty slots / empty party
- duplicated species
- duplicated held items
- banned legendary and mythical Pokemon
- configured banned species and banned held items
- incorrect level
- more than one special mechanic in the same team
- roster drift against the locked snapshot before battles

If the team is invalid:
- the player is not prepared
- the roster is not locked
- a pending validation record is stored
- the server broadcasts a correction window message
- the player receives the exact violation reasons
- the mod revalidates again after the configured real-time window

### Snapshot and lock
Snapshots are still stored per player under:

`<world>/serverconfig/bigbang_tournaments/<player-uuid>.json`

They now capture the competitive roster in more detail:
- Pokemon UUID and slot
- species, form and aspects
- original level and experience
- held item
- ability
- nature and minted nature
- move set and benched moves
- EVs and IVs
- shiny flag
- friendship
- tera type
- gmax factor
- dynamax level
- HP and status for audit

The snapshot is used for:
- restore
- pre-battle anti-fraud checks
- staff audit logs

It is not used to rebuild Pokemon from scratch.

### Arena and battle flow
The mod supports one main arena with:
- `pos1`
- `pos2`
- `spectator`

Battle flow:
1. validate both players again
2. heal both teams
3. teleport them to the arena
4. enforce a small arena radius
5. announce and count down
6. start PvP with `BattleBuilder.pvp1v1(...)`
7. store active battle metadata and `battleId`
8. detect the winner through Cobblemon events when possible
9. allow `/tournament win <player>` as manual fallback

## Commands

### Admin
- `/tournament participant add <player>`
- `/tournament participant remove <player>`
- `/tournament participant list`
- `/tournament validate <player> <level>`
- `/tournament validateall <level>`
- `/tournament prepare <player> <level> [force]`
- `/tournament prepareall <level> [force]`
- `/tournament restore <player>`
- `/tournament restoreall`
- `/tournament unlock <player>`
- `/tournament arena setpos1`
- `/tournament arena setpos2`
- `/tournament arena setspectator`
- `/tournament arena info`
- `/tournament battle <player1> <player2>`
- `/tournament win <player>`
- `/tournament healall`

### Player
- `/tournament spectate`
- `/assistirbatalha`

## Tournament State Files

Alongside per-player snapshots, the mod persists:
- `serverconfig/bigbang_tournaments/tournament_config.json`
- `serverconfig/bigbang_tournaments/tournament_state.json`

Those files store:
- arena positions
- participant list
- prepared / pending / locked state
- active battle
- battle history
- tournament defaults such as allowed levels, correction window and clauses

## Building

From the repository root:

```bash
./gradlew build
```

The NeoForge jar is generated under:

`neoforge/build/libs/`
