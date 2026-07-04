# Team Preview Recovery — BigBang Tournaments

## Crash / Restart Recovery

### Identificação de Sessões Pendentes

No boot do servidor, `TournamentBattleService.handleRestartRecovery()`:

1. Lista sessões em `world/serverconfig/bigbang_tournaments/team_preview_sessions/`
2. Para cada sessão com estado não-terminal:
   - Se `isPartyModified()`: tenta finalizar (restaurar parties) se ambos jogadores online,
     ou marca como `RESTORE_PENDING` e aguarda login.
   - Se não houve modificação de party: transiciona para `CANCELLED` e limpa.

### Recovery no Login do Jogador (Restauração Independente)

`TournamentBattleService.handleLogin()`:

1. Tenta restaurar party do disco antigo (`original_parties/*.dat`) para compatibilidade.
2. Lista sessões ativas no armazenamento.
3. Se o jogador tem sessão pendente ou com party modificada (`isPartyModified()`, `RESTORE_PENDING`, `RESTORING`, `FAILED`):
   - **Restauração Imediata e Independente**: O jogador é restaurado imediatamente, sem exigir que o oponente esteja online.
   - O arquivo de snapshot individual do jogador (`<playerUuid>.nbt`) é deletado logo após o sucesso da sua restauração, prevenindo dupla-restauração ou sobrescrita de party caso o outro jogador logue futuramente.
   - Ao final, se ambos foram restaurados, a sessão transiciona para `RESTORED` e o diretório da sessão é removido.
   - Remove referências da sessão em `ACTIVE_SESSION_BY_PLAYER`.

### Política de Disconnect e Fim de Batalha (Doubles & Singles)

- **Busca de Sessão Robusta (Doubles)**: No término de batalha (`BATTLE_VICTORY`, `BATTLE_FLED`, `DISCONNECT`), o sistema verifica todos os UUIDs envolvidos (vencedores, perdedores e atores em campo), garantindo a identificação correta da sessão mesmo em batalhas em duplas (onde há 4 slots em campo).
- **Restauração e Cura**: Ao restaurar a party original de 6 Pokémon de volta para o jogador via `TeamPreviewPartySwapService`, o sistema aplica `party.heal()`, garantindo que os Pokémon retornem saudáveis após o término da batalha.
- **Fallback de Servidor**: A referência do `MinecraftServer` é mantida de forma segura em fallback para que a finalização ocorra sem erros mesmo se a lista de jogadores da batalha estiver vazia no momento do evento de desconexão.

### Política de Limpeza de Arquivos

- Cada snapshot individual (`<playerUuid>.nbt`) é removido imediatamente assim que a party daquele jogador específico é restaurada com sucesso.
- O diretório da sessão e metadados (`session.json`) são removidos quando a sessão atinge o estado `RESTORED`.
- Sessões `FAILED` (onde algum jogador offline ainda não logou para restaurar) mantêm os snapshots pendentes salvos para quando o jogador logar ou para investigação.

### Estrutura de Diretórios

```
world/serverconfig/bigbang_tournaments/
  team_preview_sessions/
    <sessionId>/
      session.json        ← metadados da sessão (Gson)
      <player1Uuid>.nbt    ← snapshot party jogador 1 (removido após restauração do p1)
      <player2Uuid>.nbt    ← snapshot party jogador 2 (removido após restauração do p2)
  original_parties/       ← (legado, apenas leitura)
    <playerUuid>.dat
```

### Thread Safety e Resiliência

1. **Party offline durante recovery**: Se um jogador estiver offline no término da batalha ou no restart, sua party não é perdida; o snapshot é mantido e a restauração ocorre automaticamente no seu próximo login sem afetar o outro jogador.
2. **Snapshot corrompido**: Se o checksum SHA-256 do snapshot não corresponder, a restauração é abortada e registrada em log para evitar perda ou duplicação indevida de dados.
3. **Thread safety**: Toda manipulação de party (snapshot, swap, restore) ocorre de forma sincronizada (`SESSION_LOCKS` e `ConcurrentHashMap`), evitando concorrência em eventos simultâneos de vitória e desconexão.
