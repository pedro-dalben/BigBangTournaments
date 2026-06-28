# Tournament Battle State Machine

## Estados

| Estado | Descrição | Terminal | Party Locked |
|--------|-----------|----------|-------------|
| `CREATED` | Sessão criada, aguardando iniciar preview | Não | Não |
| `TEAM_PREVIEW` | Preview sendo exibido para ambos jogadores | Não | Sim |
| `PLAYER_ONE_SELECTED` | Jogador 1 confirmou seleção | Não | Sim |
| `PLAYER_TWO_SELECTED` | Jogador 2 confirmou seleção | Não | Sim |
| `PREPARING_PARTIES` | Salvando snapshots e trocando parties | Não | Sim |
| `PARTIES_SWAPPED` | Parties temporárias aplicadas | Não | Sim |
| `BATTLE_STARTING` | Iniciando batalha Cobblemon | Não | Sim |
| `COUNTDOWN` | Contagem regressiva antes da batalha | Não | Sim |
| `ACTIVE` | Batalha em andamento | Não | Sim |
| `RESTORE_PENDING` | Aguardando restauração das parties | Não | Sim |
| `RESTORING` | Restaurando parties ativamente | Não | Sim |
| `RESTORED` | Parties restauradas com sucesso | Sim | Não |
| `CANCELLED` | Sessão cancelada sem modificação de party | Sim | Não |
| `FAILED` | Erro durante execução (mantém snapshots) | Não | Sim (se parties modificadas) |
| `FINISHED` | Batalha concluída com vencedor | Sim | Não |
| `MANUAL_RESULT_REQUIRED` | Staff precisa registrar resultado manualmente | Não | Não |
| `INTERRUPTED` | Batalha interrompida por restart | Não | Não |

## Transições Válidas

```
CREATED
  → TEAM_PREVIEW
  → CANCELLED
  → FAILED

TEAM_PREVIEW
  → PLAYER_ONE_SELECTED
  → PLAYER_TWO_SELECTED
  → CANCELLED
  → FAILED

PLAYER_ONE_SELECTED
  → PLAYER_TWO_SELECTED
  → PREPARING_PARTIES
  → CANCELLED
  → FAILED

PLAYER_TWO_SELECTED
  → PLAYER_ONE_SELECTED
  → PREPARING_PARTIES
  → CANCELLED
  → FAILED

PREPARING_PARTIES
  → PARTIES_SWAPPED
  → RESTORE_PENDING
  → FAILED

PARTIES_SWAPPED
  → BATTLE_STARTING
  → RESTORE_PENDING
  → FAILED

BATTLE_STARTING
  → COUNTDOWN
  → ACTIVE
  → RESTORE_PENDING
  → FAILED

COUNTDOWN
  → ACTIVE
  → RESTORE_PENDING
  → CANCELLED
  → FAILED

ACTIVE
  → RESTORE_PENDING
  → FINISHED
  → MANUAL_RESULT_REQUIRED
  → CANCELLED
  → FAILED

RESTORE_PENDING
  → RESTORING
  → CANCELLED
  → FAILED

RESTORING
  → RESTORED
  → FAILED

FAILED
  → RESTORE_PENDING
  → CANCELLED

MANUAL_RESULT_REQUIRED
  → RESTORE_PENDING
  → FINISHED
  → CANCELLED
```

## Regras de Idempotência

1. `RESTORED` é terminal — nenhuma transição é permitida
2. `CANCELLED` é terminal — usado quando nenhuma party foi modificada
3. `FAILED` não é terminal — permite transição para RESTORE_PENDING para tentar recovery
4. Uma sessão não pode ter duas batalhas ativas simultâneas
5. Um jogador não pode participar de duas sessões simultâneas (verificado via `ACTIVE_SESSION_BY_PLAYER`)
6. A mesma sessão não pode trocar party duas vezes
7. A mesma sessão não pode restaurar duas vezes (transição RESTORE_PENDING → RESTORED → bloqueio)
