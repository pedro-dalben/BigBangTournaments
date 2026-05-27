# BigBang Tournaments

**BigBang Tournaments** is a server-side NeoForge mod for Minecraft 1.21.1 and Cobblemon 1.7.3. It is designed to help server operators run tournaments by managing tournament team preparation, level scaling, level validation, and original party state restoration.

This is a **server-side only** mod. Players do not need to install it on their clients.

---

## Features

- **Tournament Team Preparation**: Scales player Pokémon parties to level 50 or 100 for tournament play and fully heals the party.
- **Atomic State Snapshots**: Automatically saves the original state of the player's Pokémon (level, species, form, held item, original HP) into a JSON snapshot prior to scaling. Overwrites are blocked unless explicitly forced.
- **Robust Restoration**: Restores the original levels of the player's Pokémon from their snapshot. It matches Pokémon using their unique UUIDs, ensuring correct levels are restored even if the player reorders their party slots.
- **Level Validation**: Checks if all Pokémon in a player's party are at the expected tournament level (e.g., exactly level 50 or 100), reporting specific invalid slots and reasons.
- **Safe Persistence**: Saves snapshots inside the active world's `serverconfig/bigbang_tournaments/` directory. Writes are atomic (writing to a `.json.tmp` file and moving it) to prevent corruption during sudden server crashes.

---

## Commands

All commands require operator permissions (Level 2).

### 1. Prepare Team
Scales a player's party to the target level, saves a snapshot of their original state, and heals the party.
```bash
/tournament prepare <player> <level> [force]
```
- `<player>`: The target player name.
- `<level>`: Must be either `50` or `100`.
- `[force]` (Optional, boolean): If `true`, overwrites any existing active snapshot for this player. Default is `false`.

### 2. Restore Team
Restores the original levels of a player's Pokémon using the saved snapshot and deletes the snapshot file.
```bash
/tournament restore <player>
```
- `<player>`: The target player name.

### 3. Validate Team
Verifies that all Pokémon in the player's party match the specified tournament level.
```bash
/tournament validate <player> <level>
```
- `<player>`: The target player name.
- `<level>`: The expected level (usually `50` or `100`).

---

## Snapshot Storage Structure

Snapshots are stored as JSON files under the world save directory:
`<world_save>/serverconfig/bigbang_tournaments/<player-uuid>.json`

Example snapshot file structure:
```json
{
  "playerUuid": "e7b0fa56-11f8-4cb9-90de-3453715cbe24",
  "playerName": "Pedro",
  "createdAt": 1716769854000,
  "updatedAt": 1716769854000,
  "preparedLevel": 50,
  "status": "ACTIVE",
  "party": [
    {
      "pokemonUuid": "a8c9b20e-8ef2-48f6-ad39-bb478201a03f",
      "slot": 0,
      "originalLevel": 78,
      "species": "Charizard",
      "form": "",
      "shiny": false,
      "heldItem": "minecraft:charcoal",
      "originalHp": 240,
      "notes": ""
    }
  ]
}
```

---

## Building from Source

To compile the mod and package the NeoForge JAR, run the following command from the root directory:

```bash
./gradlew build
```

The output JAR file will be located at:
`neoforge/build/libs/bigbang_tournaments-1.0.0.jar`
