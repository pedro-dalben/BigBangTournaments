# Plano de Teste Manual — Team Preview VGC

## Pré-requisitos

- Servidor dedicado com BigBang Tournaments instalado
- Dois jogadores de teste online (Player A e Player B)
- Torneio `regulation_i_doubles` configurado
- Cada jogador com party de 6 Pokémon válidos

## Roteiro

### 1. Fluxo Básico

| # | Passo | Resultado Esperado |
|---|-------|-------------------|
| 1 | Criar party de 6 Pokémon para cada jogador | — |
| 2 | Registrar NBT original e checksum de cada party | — |
| 3 | `/tournament duel <A> <B>` | Preview exibido para ambos |
| 4 | Confirmar que espécies, itens e habilidades são exibidos | Informação correta |
| 5 | A seleciona `/tournament select 1 2 3 5` | Confirmação para A |
| 6 | B seleciona `/tournament select 2 3 4 6` | Batalha inicia em 5s |
| 7 | Batalha começa 4v4 Doubles | Formato correto |
| 8 | Encerrar por vitória | Parties restauradas |
| 9 | Comparar NBT da party restaurada com snapshot original | Idêntico |

### 2. Validações de Seleção

| # | Teste | Resultado Esperado |
|---|-------|-------------------|
| 10 | Slot repetido: `select 1 1 2 3` | Rejeitado |
| 11 | Slot inválido: `select 1 2 3 7` | Rejeitado |
| 12 | Menos de 4 slots: `select 1 2 3` | Rejeitado (sintaxe) |
| 13 | Selecionar após confirmar | Rejeitado |
| 14 | Selecionar sem estar em preview | Rejeitado |

### 3. Timeout

| # | Teste | Resultado Esperado |
|---|-------|-------------------|
| 15 | A não seleciona, espera 60s | Seleção automática (slots 1-4) |
| 16 | B não seleciona, espera 60s | Avisos em 30s, 10s, 5s |
| 17 | Selecionar após timeout | Rejeitado |

### 4. Bloqueio de Party

| # | Teste | Resultado Esperado |
|---|-------|-------------------|
| 18 | Tentar usar PC block durante preview | Bloqueado |
| 19 | Tentar `/pc` durante preview | Bloqueado |
| 20 | Tentar trocar Pokémon via PC durante batalha | Bloqueado |
| 21 | Tentar trade durante batalha | Bloqueado |
| 22 | Tentar release durante batalha | Bloqueado |
| 23 | Verificar que PC funciona após restore | Liberado |

### 5. Desconexão

| # | Teste | Resultado Esperado |
|---|-------|-------------------|
| 24 | A desconecta durante preview | Sessão finalizada, parties ok |
| 25 | A desconecta durante batalha | Sessão finalizada, parties restauradas |
| 26 | B desconecta, A ainda online | A restaura, B pendente |
| 27 | B reconecta | B é restaurado se sessão pendente |

### 6. Crash/Restart

| # | Teste | Resultado Esperado |
|---|-------|-------------------|
| 28 | Parar servidor durante preview | Sessão CANCELLED no restart |
| 29 | Parar servidor após snapshot, antes do swap | Sessão RESTORE_PENDING |
| 30 | Parar servidor após swap, antes da batalha | Sessão RESTORE_PENDING |
| 31 | Parar servidor durante batalha | Sessão RESTORE_PENDING |
| 32 | Parar servidor durante restore | Restauração tentada novamente |
| 33 | Reiniciar servidor | Recovery executado |
| 34 | Verificar parties após recovery | Idênticas aos snapshots |

### 7. Integridade de Dados

| # | Teste | Resultado Esperado |
|---|-------|-------------------|
| 35 | Snapshot só é apagado após RESTORED | Verificar diretório |
| 36 | Ausência de duplicação em party e PC | `./pokemon list` consistente |
| 37 | HP original preservado | Comparar NBT |
| 38 | Itens originais preservados | Comparar NBT |
| 39 | Moves originais preservados | Comparar NBT |
| 40 | Logs sem exceções | `latest.log` limpo |

### 8. Configuração de Preview

| # | Teste | Resultado Esperado |
|---|-------|-------------------|
| 41 | `revealSpecies: false` | Espécies ocultas (???) |
| 42 | `revealHeldItems: false` | Itens ocultos |
| 43 | `revealAbilities: false` | Habilidades ocultas |
| 44 | `durationSeconds: 30` | Timeout em 30s |
