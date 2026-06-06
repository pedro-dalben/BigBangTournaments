package com.bigbang_tournaments.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public final class TournamentMessages {
    public static final String PREFIX = "[CAMPEONATO] ";

    private TournamentMessages() {
    }

    public static Component text(String message) {
        return Component.literal(PREFIX + message);
    }

    public static void broadcast(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(text(message), false);
    }

    public static void send(ServerPlayer player, String message) {
        player.sendSystemMessage(text(message));
    }

    public static void sendFailure(CommandSourceStack source, String message) {
        source.sendFailure(text(message));
    }

    public static void sendSuccess(CommandSourceStack source, String message, boolean broadcastToOps) {
        source.sendSuccess(() -> text(message), broadcastToOps);
    }

    public static void broadcastInvalidTeam(MinecraftServer server, String playerName) {
        broadcast(server, "O jogador " + playerName + " possui Pokemon, item ou regra invalida no time. Ele tem 5 minutos para corrigir.");
    }

    public static void sendInvalidTeam(ServerPlayer player, List<String> reasons) {
        send(player, "Seu time esta invalido: " + String.join("; ", reasons) + ". Corrija em ate 5 minutos.");
    }

    public static void broadcastPrepared(MinecraftServer server, String playerName) {
        broadcast(server, playerName + " esta validado e pronto para o campeonato.");
    }

    public static void broadcastAllPrepared(MinecraftServer server) {
        broadcast(server, "Todos os jogadores estao preparados. O campeonato pode comecar.");
    }

    public static void sendRosterLocked(ServerPlayer player) {
        send(player, "Seu time esta travado para o campeonato. Voce nao pode alterar Pokemon, itens, moves, habilidades ou formas agora.");
    }

    public static void logRosterChange(Logger logger, String playerName, String detail) {
        logger.warn("{} tentou alterar o roster travado: {}", playerName, detail);
    }

    public static void broadcastNextBattle(MinecraftServer server, String player1, String player2) {
        broadcast(server, "Proxima batalha: " + player1 + " vs " + player2 + ".");
    }

    public static void broadcastBattleCountdown(MinecraftServer server, String player1, String player2, int seconds) {
        broadcast(server, "Batalha entre " + player1 + " e " + player2 + " comecara em " + seconds + " segundos.");
    }

    public static void broadcastBattleStarted(MinecraftServer server, String player1, String player2) {
        broadcast(server, "A batalha comecou! " + player1 + " vs " + player2);
    }

    public static void broadcastBattleWin(MinecraftServer server, String winner, String loser) {
        broadcast(server, winner + " venceu a batalha contra " + loser + ".");
    }

    public static void broadcastManualWin(MinecraftServer server, String winner) {
        broadcast(server, "Resultado registrado manualmente: " + winner + " venceu.");
    }

    public static void broadcastDisconnect(MinecraftServer server, String player, String opponent) {
        broadcast(server, player + " desconectou durante a batalha contra " + opponent + ". A staff ira avaliar a situacao.");
    }

    public static void broadcastInterruptedRestart(MinecraftServer server) {
        broadcast(server, "Partida ativa foi interrompida por restart. Resultado deve ser definido manualmente.");
    }

    public static void sendArenaIncomplete(CommandSourceStack source) {
        sendFailure(source, "Arena incompleta. Configure pos1, pos2 e arquibancada antes de iniciar batalhas.");
    }

    public static void sendSpectatorTeleported(ServerPlayer player) {
        send(player, "Voce foi enviado para a arquibancada.");
    }

    public static void sendSpectatorMissing(ServerPlayer player) {
        send(player, "A arquibancada ainda nao foi configurada.");
    }

    public static void sendArenaBoundary(ServerPlayer player) {
        send(player, "Voce nao pode sair da area da batalha.");
    }

    public static List<String> toReasonMessages(List<?> violations) {
        return violations.stream().map(Object::toString).collect(Collectors.toList());
    }
}
