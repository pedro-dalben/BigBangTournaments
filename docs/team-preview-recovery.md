# Team Preview Recovery — BigBang Tournaments

## Crash / Restart Recovery

### Identificação de Sessões Pendentes

No boot do servidor, `TournamentBattleService.handleRestartRecovery()`:

1. Lista sessões em `world/serverconfig/bigbang_tournaments/team_preview_sessions/`
2. Para cada sessão com estado não-terminal:
   - Se `isPartyModified()`: tenta finalizar (restaurar parties) se ambos jogadores online,
     ou marca como `RESTORE_PENDING` e aguarda login
   - Se não houve modificação de party: transiciona para `CANCELLED` e limpa

### Recovery no Login do Jogador

`TournamentBattleService.handleLogin()`:

1. Tenta restaurar party do disco antigo (`original_parties/*.dat`) para compatibilidade
2. Lista sessões ativas
3. Se jogador tem sessão pendente em `RESTORE_PENDING`/`RESTORING`/`FAILED`:
   - Se oponente online ou sessão já `FAILED`: executa finalização
   - Remove locks de sessão ativa

### Política de Disconnect

- `handleDisconnect()`: finaliza sessão com motivo `player_disconnected`
- Remove locks de sessão ativa
- Marca batalha como `MANUAL_RESULT_REQUIRED` no record legado

### Política de Limpeza de Arquivos

- Snapshot NBT só é removido após sessão atingir `RESTORED`
- Sessão `FAILED` mantém snapshots para investigação
- Administrador pode remover snapshots manualmente de sessões `FAILED`
- Limpeza manual: deletar diretório `team_preview_sessions/<sessionId>/`

### Estrutura de Diretórios

```
world/serverconfig/bigbang_tournaments/
  team_preview_sessions/
    <sessionId>/
      session.json        ← metadados da sessão (Gson)
      <player1Uuid>.nbt    ← snapshot party jogador 1
      <player2Uuid>.nbt    ← snapshot party jogador 2
  original_parties/       ← (legado, apenas leitura)
    <playerUuid>.dat
```

### Limitações Conhecidas

1. **Party offline durante recovery**: Se ambos os jogadores estiverem offline
   no restart, a sessão fica em `RESTORE_PENDING` até que um deles faça login.

2. **Snapshot corrompido**: Se o checksum SHA-256 do snapshot não corresponder,
   a restauração falha e a sessão vai para `FAILED`. O administrador deve
   restaurar manualmente usando backups.

3. **Formato não-VGC**: Apenas torneios `regulation_i_doubles` usam sessão.
   Outros formatos (standard, doubles) usam o fluxo COUNTDOWN direto.

4. **Thread safety**: Toda manipulação de party (snapshot, swap, restore)
   ocorre em callbacks do `ServerTaskTracker` (main thread). O `ConcurrentHashMap`
   de sessões ativas e locks por sessão previnem concorrência.
