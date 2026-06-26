package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentArenaState;
import com.bigbang_tournaments.model.TournamentBattleRecord;
import com.bigbang_tournaments.model.TournamentBattleStatus;
import com.bigbang_tournaments.model.TournamentParticipantRecord;
import com.bigbang_tournaments.model.TournamentPosition;
import com.bigbang_tournaments.model.TournamentRuleViolation;
import com.bigbang_tournaments.model.TournamentState;
import com.bigbang_tournaments.util.TournamentMessages;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import com.cobblemon.mod.common.net.serverhandling.battle.SpectateBattleHandler;
import kotlin.Unit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TournamentBattleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentBattleService.class);
    private static final Map<UUID, AreaLock> AREA_LOCKS = new HashMap<>();

    private TournamentBattleService() {
    }

    public static void setArenaPos1(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        arena.setPos1(capturePosition(player));
        TournamentStateService.saveState(server);
        TournamentMessages.send(player, "Posicao 1 da arena definida.");
    }

    public static void setArenaPos2(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        arena.setPos2(capturePosition(player));
        TournamentStateService.saveState(server);
        TournamentMessages.send(player, "Posicao 2 da arena definida.");
    }

    public static void setArenaSpectator(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        arena.setSpectator(capturePosition(player));
        TournamentStateService.saveState(server);
        TournamentMessages.send(player, "Arquibancada definida.");
    }

    public static String arenaInfo(MinecraftServer server) {
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        return "pos1=" + formatPosition(arena.getPos1()) + ", pos2=" + formatPosition(arena.getPos2()) + ", spectator=" + formatPosition(arena.getSpectator());
    }

    public static int startBattle(CommandSourceStack source, ServerPlayer player1, ServerPlayer player2) {
        MinecraftServer server = source.getServer();
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        if (!arena.isComplete()) {
            TournamentMessages.sendArenaIncomplete(source);
            return 0;
        }

        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle != null && activeBattle.getStatus() != TournamentBattleStatus.FINISHED) {
            TournamentMessages.sendFailure(source, "Ja existe uma partida ativa ou pendente de resolucao.");
            return 0;
        }

        Optional<TournamentParticipantRecord> record1 = TournamentStateService.getParticipant(server, player1.getUUID());
        Optional<TournamentParticipantRecord> record2 = TournamentStateService.getParticipant(server, player2.getUUID());
        if (record1.isEmpty() || !record1.get().isPrepared()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player1.getGameProfile().getName() + " ainda nao esta preparado.");
            return 0;
        }
        if (record2.isEmpty() || !record2.get().isPrepared()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player2.getGameProfile().getName() + " ainda nao esta preparado.");
            return 0;
        }
        if (!record1.get().isRosterLocked()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player1.getGameProfile().getName() + " nao esta com o roster travado.");
            return 0;
        }
        if (!record2.get().isRosterLocked()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player2.getGameProfile().getName() + " nao esta com o roster travado.");
            return 0;
        }

        List<TournamentRuleViolation> player1Violations = TournamentRulesValidator.validatePlayer(player1, record1.get().getPreparedLevel(), true);
        if (!player1Violations.isEmpty()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player1.getGameProfile().getName() + " esta com o time invalido: " + String.join("; ", TournamentRulesValidator.toReasonList(player1Violations)) + ".");
            return 0;
        }

        List<TournamentRuleViolation> player2Violations = TournamentRulesValidator.validatePlayer(player2, record2.get().getPreparedLevel(), true);
        if (!player2Violations.isEmpty()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player2.getGameProfile().getName() + " esta com o time invalido: " + String.join("; ", TournamentRulesValidator.toReasonList(player2Violations)) + ".");
            return 0;
        }

        PokemonTeamService.healPlayerTeam(player1);
        PokemonTeamService.healPlayerTeam(player2);
        teleportToPosition(player1, arena.getPos1());
        teleportToPosition(player2, arena.getPos2());
        enableAreaLock(server, player1, arena.getPos1());
        enableAreaLock(server, player2, arena.getPos2());

        TournamentBattleRecord battleRecord = new TournamentBattleRecord();
        battleRecord.setPlayer1Uuid(player1.getUUID());
        battleRecord.setPlayer1Name(player1.getGameProfile().getName());
        battleRecord.setPlayer2Uuid(player2.getUUID());
        battleRecord.setPlayer2Name(player2.getGameProfile().getName());
        battleRecord.setStatus(TournamentBattleStatus.COUNTDOWN);
        battleRecord.setCreatedAt(System.currentTimeMillis());
        battleRecord.setUpdatedAt(System.currentTimeMillis());
        TournamentStateService.setActiveBattle(server, battleRecord);

        TournamentMessages.broadcastNextBattle(server, player1.getGameProfile().getName(), player2.getGameProfile().getName());
        TournamentMessages.broadcastBattleCountdown(server, player1.getGameProfile().getName(), player2.getGameProfile().getName(), 10);
        scheduleCountdown(server, battleRecord, player1, player2, 5);
        scheduleCountdown(server, battleRecord, player1, player2, 3);
        scheduleCountdown(server, battleRecord, player1, player2, 2);
        scheduleCountdown(server, battleRecord, player1, player2, 1);
        com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(10F, () -> {
            beginCobblemonBattle(server, player1, player2);
            return Unit.INSTANCE;
        });
        return 1;
    }

    public static int registerManualWin(CommandSourceStack source, ServerPlayer winner) {
        MinecraftServer server = source.getServer();
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            TournamentMessages.sendFailure(source, "Nao ha batalha ativa para registrar resultado.");
            return 0;
        }

        UUID winnerUuid = winner.getUUID();
        if (!winnerUuid.equals(activeBattle.getPlayer1Uuid()) && !winnerUuid.equals(activeBattle.getPlayer2Uuid())) {
            TournamentMessages.sendFailure(source, "O jogador informado nao participa da batalha ativa.");
            return 0;
        }

        activeBattle.setWinnerUuid(winnerUuid);
        activeBattle.setWinnerName(winner.getGameProfile().getName());
        boolean isPlayer1 = winnerUuid.equals(activeBattle.getPlayer1Uuid());
        activeBattle.setLoserUuid(isPlayer1 ? activeBattle.getPlayer2Uuid() : activeBattle.getPlayer1Uuid());
        activeBattle.setLoserName(isPlayer1 ? activeBattle.getPlayer2Name() : activeBattle.getPlayer1Name());
        activeBattle.setManualResult(true);
        activeBattle.setStatus(TournamentBattleStatus.FINISHED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        releaseAreaLocks(activeBattle);
        sendPlayersToSpectatorArea(server, activeBattle);
        TournamentMessages.broadcastManualWin(server, activeBattle.getWinnerName());
        TournamentStateService.archiveActiveBattle(server);
        return 1;
    }

    public static void spectate(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        TournamentPosition spectatorPos = TournamentStateService.getState(server).getArena().getSpectator();
        if (spectatorPos == null) {
            TournamentMessages.sendSpectatorMissing(player);
            return;
        }

        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        boolean shouldTeleport = activeBattle == null
                || activeBattle.getStatus() != TournamentBattleStatus.ACTIVE
                || !isWithinSpectatorRange(player, spectatorPos, server);

        if (shouldTeleport) {
            teleportToPosition(player, spectatorPos);
            TournamentMessages.sendSpectatorTeleported(player);
        }

        if (activeBattle == null || activeBattle.getStatus() != TournamentBattleStatus.ACTIVE) {
            return;
        }

        ServerPlayer target = server.getPlayerList().getPlayer(activeBattle.getPlayer1Uuid());
        if (target == null || target.getUUID().equals(player.getUUID())) {
            target = server.getPlayerList().getPlayer(activeBattle.getPlayer2Uuid());
        }
        if (target != null && !target.getUUID().equals(player.getUUID())) {
            SpectateBattleHandler.INSTANCE.spectateBattle(target, player);
        }
    }

    public static void handlePlayerTick(ServerPlayer player) {
        AreaLock areaLock = AREA_LOCKS.get(player.getUUID());
        if (areaLock == null) {
            return;
        }

        if (!player.level().dimension().location().toString().equals(areaLock.position().getDimension())) {
            teleportToPosition(player, areaLock.position());
            return;
        }

        double dx = player.getX() - areaLock.position().getX();
        double dy = player.getY() - areaLock.position().getY();
        double dz = player.getZ() - areaLock.position().getZ();
        double maxDistanceSq = areaLock.radius() * areaLock.radius();
        if ((dx * dx) + (dy * dy) + (dz * dz) > maxDistanceSq) {
            teleportToPosition(player, areaLock.position());
            long now = System.currentTimeMillis();
            if (now - areaLock.lastWarningAt() >= 2000L) {
                TournamentMessages.sendArenaBoundary(player);
                AREA_LOCKS.put(player.getUUID(), new AreaLock(areaLock.position(), areaLock.radius(), now));
            }
        }
    }

    public static void handleBattleStarted(PokemonBattle battle) {
        if (!battle.isPvP()) {
            return;
        }
        MinecraftServer server = battle.getPlayers().isEmpty() ? null : battle.getPlayers().get(0).getServer();
        if (server == null) {
            return;
        }
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            return;
        }
        if (containsPlayers(activeBattle, battle.getPlayerUUIDs())) {
            activeBattle.setBattleId(battle.getBattleId().toString());
            activeBattle.setStatus(TournamentBattleStatus.ACTIVE);
            activeBattle.setUpdatedAt(System.currentTimeMillis());
            TournamentStateService.setActiveBattle(server, activeBattle);
        }
    }

    public static void handleBattleVictory(BattleVictoryEvent event) {
        PokemonBattle battle = event.getBattle();
        MinecraftServer server = battle.getPlayers().isEmpty() ? null : battle.getPlayers().get(0).getServer();
        if (server == null) {
            return;
        }

        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null || !containsPlayers(activeBattle, battle.getPlayerUUIDs())) {
            return;
        }

        UUID winnerUuid = null;
        UUID loserUuid = null;
        for (var winner : event.getWinners()) {
            winnerUuid = winner.getUuid();
            break;
        }
        for (var loser : event.getLosers()) {
            loserUuid = loser.getUuid();
            break;
        }

        if (winnerUuid == null || loserUuid == null) {
            activeBattle.setStatus(TournamentBattleStatus.MANUAL_RESULT_REQUIRED);
            activeBattle.setUpdatedAt(System.currentTimeMillis());
            TournamentStateService.setActiveBattle(server, activeBattle);
            releaseAreaLocks(activeBattle);
            TournamentMessages.broadcast(server, "A batalha entre " + activeBattle.getPlayer1Name() + " e " + activeBattle.getPlayer2Name() + " terminou, mas o vencedor nao pode ser detectado automaticamente. Staff deve registrar o resultado manualmente.");
            return;
        }

        activeBattle.setWinnerUuid(winnerUuid);
        activeBattle.setLoserUuid(loserUuid);
        activeBattle.setWinnerName(winnerUuid.equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer1Name() : activeBattle.getPlayer2Name());
        activeBattle.setLoserName(loserUuid.equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer1Name() : activeBattle.getPlayer2Name());
        activeBattle.setBattleId(battle.getBattleId().toString());
        activeBattle.setStatus(TournamentBattleStatus.FINISHED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        battle.saveBattleLog();
        releaseAreaLocks(activeBattle);
        sendPlayersToSpectatorArea(server, activeBattle);
        TournamentMessages.broadcastBattleWin(server, activeBattle.getWinnerName(), activeBattle.getLoserName());
        TournamentStateService.archiveActiveBattle(server);
    }

    public static void handleBattleFled(BattleFledEvent event) {
        ServerPlayer player = event.getPlayer().getEntity();
        if (player == null || player.getServer() == null) {
            return;
        }
        markManualResultRequired(player.getServer(), player.getUUID(), "fuga");
    }

    public static void handleDisconnect(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            return;
        }
        if (!player.getUUID().equals(activeBattle.getPlayer1Uuid()) && !player.getUUID().equals(activeBattle.getPlayer2Uuid())) {
            return;
        }

        UUID opponentUuid = player.getUUID().equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer2Uuid() : activeBattle.getPlayer1Uuid();
        String opponentName = player.getUUID().equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer2Name() : activeBattle.getPlayer1Name();
        activeBattle.setStatus(TournamentBattleStatus.MANUAL_RESULT_REQUIRED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        TournamentStateService.setActiveBattle(server, activeBattle);
        releaseAreaLocks(activeBattle);
        TournamentMessages.broadcastDisconnect(server, player.getGameProfile().getName(), opponentName);
        LOGGER.warn("Player {} disconnected during tournament battle against {}", player.getUUID(), opponentUuid);
    }

    public static void handleRestartRecovery(MinecraftServer server) {
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle != null && activeBattle.isInterruptedByRestart()) {
            TournamentMessages.broadcastInterruptedRestart(server);
        }
    }

    private static void scheduleCountdown(MinecraftServer server, TournamentBattleRecord activeBattle, ServerPlayer player1, ServerPlayer player2, int secondsLeft) {
        float delay = 10F - secondsLeft;
        com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(delay, () -> {
            TournamentBattleRecord current = TournamentStateService.getActiveBattle(server);
            if (current != null && current.getStatus() == TournamentBattleStatus.COUNTDOWN
                    && current.getPlayer1Uuid().equals(activeBattle.getPlayer1Uuid())
                    && current.getPlayer2Uuid().equals(activeBattle.getPlayer2Uuid())) {
                TournamentMessages.broadcastBattleCountdown(server, player1.getGameProfile().getName(), player2.getGameProfile().getName(), secondsLeft);
            }
            return Unit.INSTANCE;
        });
    }

    private static void beginCobblemonBattle(MinecraftServer server, ServerPlayer player1, ServerPlayer player2) {
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null || activeBattle.getStatus() != TournamentBattleStatus.COUNTDOWN) {
            return;
        }

        TournamentState state = TournamentStateService.getState(server);
        com.cobblemon.mod.common.battles.BattleFormat format = com.cobblemon.mod.common.battles.BattleFormat.Companion.getGEN_9_SINGLES();
        if (state != null) {
            String modeId = TournamentModeRegistry.resolve(state.getTournamentType()).id();
            if ("doubles".equals(modeId) || "regulation_i_doubles".equals(modeId)) {
                format = com.cobblemon.mod.common.battles.BattleFormat.Companion.getGEN_9_DOUBLES();
            }
        }

        BattleStartResult result = BattleBuilder.INSTANCE.pvp1v1(player1, player2, null, null, format, false, false);
        if (result instanceof SuccessfulBattleStart successfulBattleStart) {
            PokemonBattle battle = successfulBattleStart.getBattle();
            activeBattle.setBattleId(battle.getBattleId().toString());
            activeBattle.setStatus(TournamentBattleStatus.ACTIVE);
            activeBattle.setUpdatedAt(System.currentTimeMillis());
            TournamentStateService.setActiveBattle(server, activeBattle);
            TournamentMessages.broadcastBattleStarted(server, player1.getGameProfile().getName(), player2.getGameProfile().getName());
            return;
        }

        LOGGER.warn("Cobblemon PvP battle start failed between {} and {}", player1.getGameProfile().getName(), player2.getGameProfile().getName());
        releaseAreaLocks(activeBattle);
        TournamentStateService.archiveActiveBattle(server);
    }

    private static void markManualResultRequired(MinecraftServer server, UUID participantUuid, String reason) {
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            return;
        }
        if (!participantUuid.equals(activeBattle.getPlayer1Uuid()) && !participantUuid.equals(activeBattle.getPlayer2Uuid())) {
            return;
        }
        activeBattle.setStatus(TournamentBattleStatus.MANUAL_RESULT_REQUIRED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        TournamentStateService.setActiveBattle(server, activeBattle);
        releaseAreaLocks(activeBattle);
        LOGGER.warn("Tournament battle marked for manual result because of {}", reason);
    }

    private static TournamentPosition capturePosition(ServerPlayer player) {
        return new TournamentPosition(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    private static String formatPosition(TournamentPosition position) {
        if (position == null) {
            return "unset";
        }
        return position.getDimension() + "@" + position.getX() + "," + position.getY() + "," + position.getZ();
    }

    private static void teleportToPosition(ServerPlayer player, TournamentPosition position) {
        if (player.getServer() == null || position == null) {
            return;
        }
        ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(position.getDimension())));
        if (level == null) {
            return;
        }
        player.teleportTo(level, position.getX(), position.getY(), position.getZ(), position.getYaw(), position.getPitch());
    }

    private static boolean isWithinSpectatorRange(ServerPlayer player, TournamentPosition spectatorPos, MinecraftServer server) {
        if (spectatorPos == null || player.getServer() == null) {
            return false;
        }

        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(spectatorPos.getDimension())));
        if (level == null || !player.level().dimension().location().equals(level.dimension().location())) {
            return false;
        }

        double dx = player.getX() - spectatorPos.getX();
        double dy = player.getY() - spectatorPos.getY();
        double dz = player.getZ() - spectatorPos.getZ();
        double maxDistance = Math.max(24.0D, TournamentStateService.getConfig(server).getArenaRadius() * 6.0D);
        return (dx * dx) + (dy * dy) + (dz * dz) <= maxDistance * maxDistance;
    }

    private static void enableAreaLock(MinecraftServer server, ServerPlayer player, TournamentPosition center) {
        AREA_LOCKS.put(player.getUUID(), new AreaLock(center, TournamentStateService.getConfig(server).getArenaRadius(), 0L));
    }

    private static void releaseAreaLocks(TournamentBattleRecord battleRecord) {
        AREA_LOCKS.remove(battleRecord.getPlayer1Uuid());
        AREA_LOCKS.remove(battleRecord.getPlayer2Uuid());
    }

    private static void sendPlayersToSpectatorArea(MinecraftServer server, TournamentBattleRecord battleRecord) {
        TournamentPosition spectatorPos = TournamentStateService.getState(server).getArena().getSpectator();
        if (spectatorPos == null) {
            return;
        }

        com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(1F, () -> {
            ServerPlayer player1 = server.getPlayerList().getPlayer(battleRecord.getPlayer1Uuid());
            ServerPlayer player2 = server.getPlayerList().getPlayer(battleRecord.getPlayer2Uuid());
            if (player1 != null) {
                teleportToPosition(player1, spectatorPos);
            }
            if (player2 != null) {
                teleportToPosition(player2, spectatorPos);
            }
            return Unit.INSTANCE;
        });
    }

    private static boolean containsPlayers(TournamentBattleRecord activeBattle, Iterable<UUID> playerUuids) {
        boolean hasPlayer1 = false;
        boolean hasPlayer2 = false;
        for (UUID playerUuid : playerUuids) {
            if (playerUuid.equals(activeBattle.getPlayer1Uuid())) {
                hasPlayer1 = true;
            }
            if (playerUuid.equals(activeBattle.getPlayer2Uuid())) {
                hasPlayer2 = true;
            }
        }
        return hasPlayer1 && hasPlayer2;
    }

    private record AreaLock(TournamentPosition position, int radius, long lastWarningAt) {
    }
}
