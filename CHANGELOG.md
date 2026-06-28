# Changelog

## [Unreleased]

### Added
- TournamentBattleSession: sessão explícita de batalha com state machine de 17 estados
- TournamentBattleSessionStorage: persistência atômica de sessões em disco
- TeamPreviewPartySwapService: swap atômico de party com rollback em 3 pontos
- TournamentBattleFinalizationService: finalização idempotente com lock por sessão
- TeamPreviewConfig: configuração de duração, auto-select e visibilidade (species, item, ability)
- Party lock durante sessão de batalha (combina roster lock + battle session)
- Testes de state machine: 49 testes de unidade para TournamentBattleSession
- Docs: team-preview-vgc.md, team-preview-recovery.md, tournament-battle-state-machine.md,
  manual-test-plan.md, team-preview-vgc-independent-audit.md

### Changed
- TournamentBattleStatus: expandido de 6 para 17 estados com métodos isTerminal/isPartyLocked
- TournamentBattleService: refatorado para usar TournamentBattleSession em vez de maps estáticos
- TournamentEventsHandler: usa isPartyLockedForTournament em vez de isRosterLocked
- TournamentCobblemonEventRegistrar: bloqueios de party agora consideram battle sessions
- TournamentStateService: adicionado isPartyLockedForTournament
- TournamentConfig: adicionado campo teamPreview (TeamPreviewConfig)

### Fixed
- Snapshot de party agora é atômico (temp file + atomic move + checksum)
- Rollback em swap parcial: se jogador 2 falha, jogador 1 é restaurado
- Timer não pode afetar sessão errada após stale callback
- Login não restaura party sem verificar batalha ativa
- Múltiplos caminhos de finalização agora convergem para um único serviço
