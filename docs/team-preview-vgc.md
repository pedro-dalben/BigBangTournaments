# Team Preview VGC — BigBang Tournaments

## Visão Geral

O Team Preview VGC é um fluxo opcional para torneios do tipo `regulation_i_doubles`.
Antes da batalha, cada jogador vê os 6 Pokémon do oponente e seleciona exatamente 4
para usar na batalha. O fluxo inteiro é gerenciado por uma `TournamentBattleSession`
persistida em disco.

## Estado da Sessão

```
CREATED → TEAM_PREVIEW → PLAYER_ONE_SELECTED / PLAYER_TWO_SELECTED
  → PREPARING_PARTIES → PARTIES_SWAPPED → BATTLE_STARTING
  → COUNTDOWN → ACTIVE → RESTORE_PENDING → RESTORING → RESTORED
```

Estados terminais: `RESTORED`, `CANCELLED`, `FINISHED`.

## Comandos

| Comando | Descrição |
|---------|-----------|
| `/tournament duel <p1> <p2>` | Inicia batalha (VGC ou padrão conforme formato) |
| `/tournament select <s1> <s2> <s3> <s4>` | Seleciona 4 slots (1-6) durante o preview |
| `/tournament win <player>` | Registra vitória manual |

## Regras de Visibilidade

Configurável em `teamPreview` no `tournament_config.json`:

```json
{
  "teamPreview": {
    "durationSeconds": 60,
    "autoSelectStrategy": "FIRST_FOUR",
    "revealSpecies": true,
    "revealHeldItems": true,
    "revealAbilities": true
  }
}
```

## Bloqueio de Party

Durante qualquer estado não-terminal da sessão, a party do jogador fica bloqueada:

- PC block (Cobblemon) cancelado
- Comandos `pc`, `pokebox`, `storage`, `box`, `pk` cancelados
- Pacotes de rede de storage (move, swap, bench) interceptados
- Eventos de trade, release, held item, cosmetic item cancelados
- Eventos de aspects changed registrados em log

Mensagem exibida: *"Você não pode alterar sua equipe enquanto participa de uma batalha de torneio."*

## Snapshot Atômico

Antes de alterar qualquer party, o snapshot é salvo atomicamente:

1. Serializa party completa em NBT com header validável
2. Escreve em arquivo `.nbt.tmp`
3. Computa checksum SHA-256
4. Move atomicamente para `.nbt`
5. Só então altera a party ativa

Estrutura do snapshot NBT:
```
schemaVersion: 1
sessionId: UUID
playerUuid: UUID
createdAt: long
partySize: int
selection: int[]
checksum: string (SHA-256)
party: List<CompoundTag> (Pokémon completos)
```

## Restauração

A restauração usa exclusivamente os dados do snapshot em disco:

- Verifica sessionId correspondente
- Verifica playerUuid correspondente
- Valida checksum SHA-256
- Reconstrói Pokémon do NBT
- Substitui party ativa
- Só remove snapshot após restauração confirmada

## Finalização Idempotente

`TournamentBattleFinalizationService` garante:

- Lock por sessão (synchronized)
- Compare-and-set de estado para `RESTORE_PENDING`
- Restaura ambos os jogadores
- Transição para `RESTORED` apenas após sucesso
- Snapshot removido apenas após `RESTORED`
- Chamadas paralelas são rejeitadas se estado já terminal

Caminhos que convergem para finalização:
- Vitória, derrota, fuga, timeout, desconexão, cancelamento admin,
  erro de início, crash recovery, shutdown, batalha inexistente
