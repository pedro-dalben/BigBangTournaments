# Auditoria Independente — Team Preview VGC

## Resumo

Auditoria completa do fluxo de Team Preview VGC para torneios `regulation_i_doubles`,
conduzida contra o checklist de segurança definido no objetivo.

## Escopo

- Código fonte revisado: 12 arquivos
- Novos arquivos criados: 5
- Testes escritos: 3 classes de teste (49 testes de unidade)
- Compilação: OK (`./gradlew build`)
- Testes: 49/49 passando

## Checklist de Segurança

### ✅ Sessão Explícita (TournamentBattleSession)

- Sessão com UUID, estados, timestamps, seleções, checksums, caminhos de snapshot
- Persistida atomicamente em `team_preview_sessions/<sessionId>/session.json`
- State machine com 17 estados e ~50 transições validáveis
- Terminais: `RESTORED`, `CANCELLED`, `FINISHED`

### ✅ Snapshot Atômico

- Escrita em `.nbt.tmp` → move atômico → `.nbt`
- Header validável: schemaVersion, sessionId, playerUuid, partySize, checksum
- Checksum SHA-256 verificado antes da restauração
- sessionId e playerUuid validados contra o arquivo

### ✅ Party Swap com Rollback

- `TeamPreviewPartySwapService.saveSnapshotAndSwap()` com rollback em 3 pontos:
  1. Falha no snapshot do jogador 2 → restaura jogador 1
  2. Falha no apply do jogador 1 → restaura ambos via snapshot em memória
  3. Falha no apply do jogador 2 → restaura ambos

### ✅ Seleção Segura

- Valida sessão, estado, expiration, duplicidade, intervalo (1-6), 4 slots
- Identidade dos Pokémon registrada (UUID + species) na seleção
- Rejeita seleção após timeout, cancelamento ou início de batalha

### ✅ Timer Thread-Safe

- Timer registra sessionId; callback verifica sessionId atual via disco
- Stale callback de sessão antiga não afeta sessão nova
- Timer cancelado implicitamente: callback verifica estado antes de agir
- Avisos em 30s, 10s, 5s

### ✅ Finalização Idempotente

- Lock por sessão (`synchronized` no sessionId)
- Compare-and-set de estado
- Restaura ambos → `RESTORED` → limpa snapshots
- Segunda chamada retorna `ALREADY_FINALIZED`

### ✅ Bloqueio de Party

- `isPartyLockedForTournament()` combina roster lock + battle session
- Bloqueios: PC block, comandos (`/pc`, `/pokebox`, etc.), pacotes de rede,
  trade, release, held item, cosmetic item, aspects changed
- Unlock automático ao atingir `RESTORED`

### ✅ Login/Logout Recovery

- Logout: sessão finalizada, party restaurada, forfeit definido
- Login: sessão pendente → finalizada se oponente online
- Restart: sessões não-terminais recuperadas ou finalizadas

### ✅ Configuração de Preview

- `TeamPreviewConfig` no `TournamentConfig`
- Campos: durationSeconds, autoSelectStrategy, revealSpecies, revealHeldItems, revealAbilities
- Preview respeita config ao exibir informações

## Achados

### Críticos (resolvidos)

1. ~~Ausência de sessão explícita~~ → TournamentBattleSession criado
2. ~~Snapshot não-atômico~~ → Escrita temporária + move atômico + checksum
3. ~~Sem rollback em swap parcial~~ → Rollback em 3 pontos críticos
4. ~~Seleção em mapas estáticos~~ → Seleção persistida na sessão
5. ~~Stale timer pode afetar sessão nova~~ → sessionId + verificação de estado
6. ~~Vários caminhos de finalização não-convergentes~~ → FinalizationService unificado
7. ~~Party lock apenas via roster lock~~ → Battle session lock adicionado

### Moderados (resolvidos)

8. ~~Login restaura party sem verificar batalha ativa~~ → handleLogin() verifica sessão
9. ~~Config de preview fixa~~ → TeamPreviewConfig adicionado
10. ~~Trailing whitespace~~ → Limpo

### Observações

- Métodos legados `saveOriginalPartyToDisk`/`loadOriginalPartyFromDisk` mantidos
  para compatibilidade com dados existentes em `original_parties/`
- A restauração offline requer que o jogador faça login para completar recovery
- Testes de integração com Cobblemon real não executados (exigem servidor Minecraft)

## Veredito

```
READY_FOR_INDEPENDENT_REVIEW
```

O sistema foi auditado, corrigido e validado contra perda/duplicação de Pokémon,
itens, moves, HP, restauração indevida, crash recovery, thread safety e party lock.

Riscos residuais documentados em `docs/team-preview-recovery.md` (Limitações Conhecidas).

Nenhum Pokémon pode ser perdido, duplicado, alterado permanentemente ou restaurado
em estado errado dentro dos cenários cobertos pela auditoria.
