# Team Preview VGC — BigBang Tournaments

## Visão Geral

O Team Preview é um fluxo para torneios de duplas (`doubles` e `regulation_i_doubles`).
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

## Interface e Comandos

Quando a fase de Team Preview se inicia, além da mensagem no chat, o jogo abre automaticamente uma **Interface Gráfica (Menu de Baú 9x6)** para ambos os jogadores.
- **Linha Superior (Slots 1 a 6):** Lãs coloridas representando os 6 Pokémon do oponente com informações detalhadas (espécie, nível, item e habilidade, conforme configuração).
- **Linha Intermediária (Slots 19 a 24):** Seus 6 Pokémon da party.
- **Linha Indicadora (Slots 28 a 33):** Lãs vermelhas (não selecionado) que mudam para verde ao clicar (selecionado).
- **Centro Inferior (Slot 49):** Botão "CONFIRMAR" (Bloco de Esmeralda) que é habilitado assim que exatamente 4 Pokémon são escolhidos.
- **Bloqueio de Fechamento:** Se o jogador tentar fechar o menu sem confirmar (ESC ou E), o menu é reaberto instantaneamente ("na cara da pessoa"). Após confirmar, o menu pode ser fechado livremente.

| Comando | Descrição |
|---------|-----------|
| `/tournament duel <p1> <p2>` | Inicia batalha (Doubles/VGC ou padrão conforme formato) |
| `/tournament menu` (ou `preview`, `gui`) | Abre o menu visual interativo de seleção de Pokémon |
| `/tournament select <s1> <s2> <s3> <s4>` | Seleciona 4 slots (1-6) via chat durante o preview |
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
- Substitui party ativa e aplica cura completa (`party.heal()`)
- Remove o snapshot individual (`<playerUuid>.nbt`) imediatamente após a restauração bem-sucedida do jogador, garantindo independência e resiliência na recuperação de dados

## Finalização Idempotente

`TournamentBattleFinalizationService` garante:

- Lock por sessão (synchronized)
- Compare-and-set de estado para `RESTORE_PENDING`
- Restaura os jogadores de forma independente (se um jogador estiver offline no término da batalha, o jogador online é restaurado imediatamente sem erros ou bloqueios)
- Transição para `RESTORED` apenas após sucesso de todos os participantes
- Snapshot removido individualmente por jogador logo após sua restauração e diretório da sessão limpo em `RESTORED`
- Chamadas paralelas são rejeitadas se estado já terminal

Caminhos que convergem para finalização:
- Vitória, derrota, fuga, timeout, desconexão, cancelamento admin,
  erro de início, crash recovery, shutdown, batalha inexistente
