package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentBattleSession;
import com.bigbang_tournaments.model.TournamentBattleStatus;
import com.bigbang_tournaments.storage.TournamentBattleSessionStorage;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public class TeamPreviewPartySwapService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeamPreviewPartySwapService.class);
    private static final int SCHEMA_VERSION = 1;

    public enum SwapResult {
        SUCCESS,
        SESSION_NOT_FOUND,
        INVALID_STATE,
        PLAYER_NOT_IN_SESSION,
        NO_SELECTION,
        SNAPSHOT_FAILED,
        SWAP_FAILED,
        ROLLED_BACK,
        ALREADY_SWAPPED
    }

    public static SwapResult saveSnapshotAndSwap(MinecraftServer server, TournamentBattleSession session) {
        if (session == null) return SwapResult.SESSION_NOT_FOUND;
        if (session.getState() != TournamentBattleStatus.PREPARING_PARTIES) {
            return SwapResult.INVALID_STATE;
        }

        UUID sessionId = session.getSessionId();

        try {
            ServerPlayer player1 = server.getPlayerList().getPlayer(session.getPlayerOneUuid());
            ServerPlayer player2 = server.getPlayerList().getPlayer(session.getPlayerTwoUuid());

            if (player1 == null || player2 == null) {
                session.transitionTo(TournamentBattleStatus.FAILED);
                session.setFinalizationReason("player_offline_during_swap");
                TournamentBattleSessionStorage.saveSession(server, session);
                return SwapResult.SWAP_FAILED;
            }

            List<Pokemon> p1Original = PokemonTeamService.listPartyPokemon(player1);
            List<Pokemon> p2Original = PokemonTeamService.listPartyPokemon(player2);

            String p1Checksum = saveAtomicSnapshot(server, sessionId, session.getPlayerOneUuid(), p1Original);
            if (p1Checksum == null) {
                session.transitionTo(TournamentBattleStatus.FAILED);
                session.setFinalizationReason("snapshot_write_failed_p1");
                TournamentBattleSessionStorage.saveSession(server, session);
                return SwapResult.SNAPSHOT_FAILED;
            }
            session.setPlayerOneSnapshotPath(TournamentBattleSessionStorage.getSnapshotFile(server, sessionId, session.getPlayerOneUuid()).toString());
            session.setPlayerOneSnapshotChecksum(p1Checksum);
            TournamentBattleSessionStorage.saveSession(server, session);

            String p2Checksum = saveAtomicSnapshot(server, sessionId, session.getPlayerTwoUuid(), p2Original);
            if (p2Checksum == null) {
                LOGGER.error("Snapshot failed for player 2 after player 1 snapshot was saved. Rolling back player 1.");
                restorePlayerFromSnapshot(server, session, session.getPlayerOneUuid(), p1Original);
                session.transitionTo(TournamentBattleStatus.FAILED);
                session.setFinalizationReason("snapshot_write_failed_p2");
                TournamentBattleSessionStorage.saveSession(server, session);
                return SwapResult.ROLLED_BACK;
            }
            session.setPlayerTwoSnapshotPath(TournamentBattleSessionStorage.getSnapshotFile(server, sessionId, session.getPlayerTwoUuid()).toString());
            session.setPlayerTwoSnapshotChecksum(p2Checksum);
            TournamentBattleSessionStorage.saveSession(server, session);

            if (!applyFilteredParty(player1, p1Original, session.getPlayerOneSelection())) {
                restorePlayerFromSnapshot(server, session, session.getPlayerOneUuid(), p1Original);
                restorePlayerFromSnapshot(server, session, session.getPlayerTwoUuid(), p2Original);
                session.transitionTo(TournamentBattleStatus.FAILED);
                session.setFinalizationReason("party_swap_failed_p1");
                TournamentBattleSessionStorage.saveSession(server, session);
                return SwapResult.ROLLED_BACK;
            }

            if (!applyFilteredParty(player2, p2Original, session.getPlayerTwoSelection())) {
                restorePlayerFromSnapshot(server, session, session.getPlayerOneUuid(), p1Original);
                restorePlayerFromSnapshot(server, session, session.getPlayerTwoUuid(), p2Original);
                session.transitionTo(TournamentBattleStatus.FAILED);
                session.setFinalizationReason("party_swap_failed_p2");
                TournamentBattleSessionStorage.saveSession(server, session);
                return SwapResult.ROLLED_BACK;
            }

            if (!session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED)) {
                restorePlayerFromSnapshot(server, session, session.getPlayerOneUuid(), p1Original);
                restorePlayerFromSnapshot(server, session, session.getPlayerTwoUuid(), p2Original);
                session.setState(TournamentBattleStatus.FAILED);
                session.setFinalizationReason("state_transition_failed");
                TournamentBattleSessionStorage.saveSession(server, session);
                return SwapResult.ROLLED_BACK;
            }

            TournamentBattleSessionStorage.saveSession(server, session);
            return SwapResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Fatal error during party swap for session {}", session.getSessionId(), e);
            try {
                forceRollback(server, session);
            } catch (Exception rollbackError) {
                LOGGER.error("Rollback also failed for session {}", session.getSessionId(), rollbackError);
            }
            return SwapResult.SWAP_FAILED;
        }
    }

    public static boolean restorePlayerFromSnapshot(MinecraftServer server, TournamentBattleSession session, UUID playerUuid, List<Pokemon> originalParty) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) {
                LOGGER.warn("Cannot restore party for offline player {} during session {}", playerUuid, session.getSessionId());
                return false;
            }
            PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (party == null) return false;

            for (int i = party.size() - 1; i >= 0; i--) {
                Pokemon p = party.get(i);
                if (p != null) {
                    party.remove(p);
                }
            }
            for (Pokemon p : originalParty) {
                party.add(p);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to restore party for player {} in session {}", playerUuid, session.getSessionId(), e);
            return false;
        }
    }

    public static boolean restorePlayerFromDisk(MinecraftServer server, TournamentBattleSession session, UUID playerUuid) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) {
                LOGGER.warn("Cannot restore party from disk for offline player {} in session {}", playerUuid, session.getSessionId());
                return false;
            }

            Path snapshotFile = TournamentBattleSessionStorage.getSnapshotFile(server, session.getSessionId(), playerUuid);
            if (!Files.exists(snapshotFile)) {
                LOGGER.warn("No snapshot file found for player {} in session {}", playerUuid, session.getSessionId());
                return false;
            }

            String expectedChecksum = playerUuid.equals(session.getPlayerOneUuid())
                    ? session.getPlayerOneSnapshotChecksum()
                    : session.getPlayerTwoSnapshotChecksum();

            if (expectedChecksum != null && !expectedChecksum.isEmpty()) {
                String actualChecksum = computeFileChecksum(snapshotFile);
                if (!expectedChecksum.equals(actualChecksum)) {
                    LOGGER.error("Checksum mismatch for player {} snapshot in session {}. Expected: {}, Actual: {}",
                            playerUuid, session.getSessionId(), expectedChecksum, actualChecksum);
                    return false;
                }
            }

            CompoundTag root;
            try (InputStream in = Files.newInputStream(snapshotFile)) {
                root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            }

            int fileSchema = root.getInt("schemaVersion");
            if (fileSchema != SCHEMA_VERSION) {
                LOGGER.warn("Snapshot schema version mismatch for player {}: expected {} got {}", playerUuid, SCHEMA_VERSION, fileSchema);
            }

            String fileSessionId = root.getString("sessionId");
            if (!session.getSessionId().toString().equals(fileSessionId)) {
                LOGGER.error("Snapshot sessionId mismatch for player {}: expected {} got {}", playerUuid, session.getSessionId(), fileSessionId);
                return false;
            }

            String filePlayerUuid = root.getString("playerUuid");
            if (!playerUuid.toString().equals(filePlayerUuid)) {
                LOGGER.error("Snapshot playerUuid mismatch: expected {} got {}", playerUuid, filePlayerUuid);
                return false;
            }

            ListTag listTag = root.getList("party", 10);
            if (listTag.size() != root.getInt("partySize")) {
                LOGGER.warn("Party size mismatch in snapshot for player {}: header says {} but list has {}",
                        playerUuid, root.getInt("partySize"), listTag.size());
            }

            List<Pokemon> originalParty = new ArrayList<>();
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag pTag = listTag.getCompound(i);
                Pokemon p = Pokemon.Companion.loadFromNBT(server.registryAccess(), pTag);
                originalParty.add(p);
            }

            return restorePlayerFromSnapshot(server, session, playerUuid, originalParty);
        } catch (Exception e) {
            LOGGER.error("Failed to restore player {} from disk snapshot in session {}", playerUuid, session.getSessionId(), e);
            return false;
        }
    }

    private static String saveAtomicSnapshot(MinecraftServer server, UUID sessionId, UUID playerUuid, List<Pokemon> party) {
        try {
            Path snapshotDir = TournamentBattleSessionStorage.getSessionDirPath(server, sessionId);
            if (!Files.exists(snapshotDir)) {
                Files.createDirectories(snapshotDir);
            }

            Path targetPath = TournamentBattleSessionStorage.getSnapshotFile(server, sessionId, playerUuid);
            Path tempPath = snapshotDir.resolve(playerUuid.toString() + ".nbt.tmp");

            CompoundTag root = new CompoundTag();
            root.putInt("schemaVersion", SCHEMA_VERSION);
            root.putString("sessionId", sessionId.toString());
            root.putString("playerUuid", playerUuid.toString());
            root.putLong("createdAt", System.currentTimeMillis());
            root.putInt("partySize", party.size());

            ListTag selectionTag = new ListTag();
            root.put("selection", selectionTag);
            root.putInt("numSelection", 0);

            ListTag listTag = new ListTag();
            for (Pokemon p : party) {
                CompoundTag pTag = new CompoundTag();
                p.saveToNBT(server.registryAccess(), pTag);
                listTag.add(pTag);
            }
            root.put("party", listTag);

            try (OutputStream out = Files.newOutputStream(tempPath)) {
                NbtIo.writeCompressed(root, out);
                out.flush();
            }

            String checksum = computeFileChecksum(tempPath);
            root.putString("checksum", checksum);

            try (OutputStream out = Files.newOutputStream(tempPath)) {
                NbtIo.writeCompressed(root, out);
                out.flush();
            }

            try {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return checksum;
        } catch (Exception e) {
            LOGGER.error("Failed to save atomic snapshot for player {} in session {}", playerUuid, sessionId, e);
            return null;
        }
    }

    private static String computeFileChecksum(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(path);
            byte[] hash = md.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            LOGGER.error("Failed to compute checksum for {}", path, e);
            return "";
        }
    }

    private static boolean applyFilteredParty(ServerPlayer player, List<Pokemon> original, List<Integer> selectedSlots) {
        try {
            PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (int i = party.size() - 1; i >= 0; i--) {
                Pokemon p = party.get(i);
                if (p != null) {
                    party.remove(p);
                }
            }
            for (int slot : selectedSlots) {
                if (slot >= 1 && slot <= original.size()) {
                    Pokemon p = original.get(slot - 1);
                    party.add(p);
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to apply filtered party for player {}", player.getUUID(), e);
            return false;
        }
    }

    private static void forceRollback(MinecraftServer server, TournamentBattleSession session) {
        try {
            restorePlayerFromDisk(server, session, session.getPlayerOneUuid());
        } catch (Exception e) {
            LOGGER.error("Force rollback failed for player 1 in session {}", session.getSessionId(), e);
        }
        try {
            restorePlayerFromDisk(server, session, session.getPlayerTwoUuid());
        } catch (Exception e) {
            LOGGER.error("Force rollback failed for player 2 in session {}", session.getSessionId(), e);
        }
        session.setState(TournamentBattleStatus.FAILED);
        session.setFinalizationReason("force_rollback");
    }
}
