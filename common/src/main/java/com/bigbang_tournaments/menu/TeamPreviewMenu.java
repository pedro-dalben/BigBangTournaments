package com.bigbang_tournaments.menu;

import com.bigbang_tournaments.model.TeamPreviewConfig;
import com.bigbang_tournaments.model.TournamentBattleSession;
import com.bigbang_tournaments.service.PokemonTeamService;
import com.bigbang_tournaments.service.TournamentBattleService;
import com.bigbang_tournaments.util.TournamentMessages;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public class TeamPreviewMenu {

    public static void open(ServerPlayer viewer, ServerPlayer opponent, TournamentBattleSession session, TeamPreviewConfig config) {
        viewer.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§e§lTEAM PREVIEW - Time de " + opponent.getGameProfile().getName());
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new TeamPreviewChestMenu(syncId, playerInventory, viewer, opponent, session, config);
            }
        });
    }

    private static class TeamPreviewChestMenu extends ChestMenu {
        private final ServerPlayer viewer;
        private final ServerPlayer opponent;
        private final TournamentBattleSession session;
        private final TeamPreviewConfig config;
        private final SimpleContainer container;
        private final List<Integer> selectedSlots = new ArrayList<>(); // 1 to 6

        public TeamPreviewChestMenu(int syncId, Inventory playerInventory, ServerPlayer viewer, ServerPlayer opponent, TournamentBattleSession session, TeamPreviewConfig config, SimpleContainer container) {
            super(MenuType.GENERIC_9x6, syncId, playerInventory, container, 6);
            this.viewer = viewer;
            this.opponent = opponent;
            this.session = session;
            this.config = config;
            this.container = container;

            boolean isPlayerOne = viewer.getUUID().equals(session.getPlayerOneUuid());
            List<Integer> existingSelection = isPlayerOne ? session.getPlayerOneSelection() : session.getPlayerTwoSelection();
            if (existingSelection != null) {
                selectedSlots.addAll(existingSelection);
            }

            updateInventory();
        }

        public TeamPreviewChestMenu(int syncId, Inventory playerInventory, ServerPlayer viewer, ServerPlayer opponent, TournamentBattleSession session, TeamPreviewConfig config) {
            this(syncId, playerInventory, viewer, opponent, session, config, new SimpleContainer(54));
        }

        public void updateInventory() {
            boolean isConfirmed = viewer.getUUID().equals(session.getPlayerOneUuid())
                    ? (session.getPlayerOneSelection() != null && !session.getPlayerOneSelection().isEmpty())
                    : (session.getPlayerTwoSelection() != null && !session.getPlayerTwoSelection().isEmpty());

            List<Pokemon> enemyTeam = PokemonTeamService.listPartyPokemon(opponent);
            List<Pokemon> myTeam = PokemonTeamService.listPartyPokemon(viewer);

            Item[] enemyWools = {Items.RED_WOOL, Items.ORANGE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL, Items.LIGHT_BLUE_WOOL, Items.PURPLE_WOOL};
            Item[] myWools = {Items.CYAN_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.GREEN_WOOL, Items.MAGENTA_WOOL, Items.PINK_WOOL};

            // Row 0: Enemy Team (slots 1 to 6, border in 0, 7, 8)
            container.setItem(0, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));
            for (int i = 0; i < 6; i++) {
                if (i < enemyTeam.size()) {
                    Pokemon p = enemyTeam.get(i);
                    String speciesName = p.getSpecies() != null ? p.getSpecies().getName() : "Unknown";
                    String title = "§c§l[INIMIGO] §e" + (config.isRevealSpecies() ? speciesName : "???");
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Slot: §f" + (i + 1));
                    lore.add("§7Nível: §f" + (config.isRevealSpecies() ? String.valueOf(p.getLevel()) : "???"));
                    if (config.isRevealHeldItems()) {
                        ItemStack held = p.heldItem();
                        String heldId = PokemonTeamService.getHeldItemId(held);
                        if (!heldId.isBlank()) {
                            String display = heldId.contains(":") ? heldId.split(":")[1] : heldId;
                            lore.add("§7Item: §b" + capitalizeWord(display.replace("_", " ")));
                        }
                    }
                    if (config.isRevealAbilities()) {
                        String ability = p.getAbility() != null ? p.getAbility().getName() : "";
                        if (!ability.isEmpty()) {
                            lore.add("§7Habilidade: §d" + ability);
                        }
                    }
                    container.setItem(i + 1, createItem(enemyWools[i], title, lore));
                } else {
                    container.setItem(i + 1, createPane(Items.GRAY_STAINED_GLASS_PANE, "§7[ Vazio ]"));
                }
            }
            container.setItem(7, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));
            container.setItem(8, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));

            // Row 1: Divider (slots 9 to 17)
            for (int i = 9; i <= 17; i++) {
                container.setItem(i, createPane(Items.BLACK_STAINED_GLASS_PANE, "§8§l[ Divisória - Seu Time Abaixo ]"));
            }

            // Row 2: Your Team (slots 19 to 24)
            container.setItem(18, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));
            for (int i = 0; i < 6; i++) {
                if (i < myTeam.size()) {
                    Pokemon p = myTeam.get(i);
                    String speciesName = p.getSpecies() != null ? p.getSpecies().getName() : "Unknown";
                    String title = "§a§l[SEU TIME] §f" + speciesName;
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Slot: §f" + (i + 1));
                    lore.add("§7Nível: §f" + p.getLevel());
                    ItemStack held = p.heldItem();
                    String heldId = PokemonTeamService.getHeldItemId(held);
                    if (!heldId.isBlank()) {
                        String display = heldId.contains(":") ? heldId.split(":")[1] : heldId;
                        lore.add("§7Item: §b" + capitalizeWord(display.replace("_", " ")));
                    }
                    String ability = p.getAbility() != null ? p.getAbility().getName() : "";
                    if (!ability.isEmpty()) {
                        lore.add("§7Habilidade: §d" + ability);
                    }
                    lore.add("");
                    if (selectedSlots.contains(i + 1)) {
                        lore.add("§a§l✔ SELECIONADO PARA BATALHA ✔");
                        lore.add("§eClique para deselecionar");
                    } else {
                        lore.add("§c§l✖ NÃO SELECIONADO (NO BANCO) ✖");
                        lore.add("§eClique para selecionar");
                    }
                    container.setItem(19 + i, createItem(myWools[i], title, lore));
                } else {
                    container.setItem(19 + i, createPane(Items.GRAY_STAINED_GLASS_PANE, "§7[ Vazio ]"));
                }
            }
            container.setItem(25, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));
            container.setItem(26, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));

            // Row 3: Selection Indicators (slots 28 to 33)
            container.setItem(27, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));
            for (int i = 0; i < 6; i++) {
                if (i < myTeam.size()) {
                    if (selectedSlots.contains(i + 1)) {
                        List<String> lore = List.of("§7Slot " + (i + 1) + ": §f" + myTeam.get(i).getSpecies().getName(), "§aEste Pokémon batalhará!", "§eClique aqui ou acima para deselecionar.");
                        container.setItem(28 + i, createItem(Items.GREEN_WOOL, "§a§l✔ [ SELECIONADO ] ✔", lore));
                    } else {
                        List<String> lore = List.of("§7Slot " + (i + 1) + ": §f" + myTeam.get(i).getSpecies().getName(), "§cEste Pokémon ficará no banco.", "§eClique aqui ou acima para selecionar.");
                        container.setItem(28 + i, createItem(Items.RED_WOOL, "§c§l✖ [ NÃO SELECIONADO ] ✖", lore));
                    }
                } else {
                    container.setItem(28 + i, createPane(Items.GRAY_STAINED_GLASS_PANE, "§7[ Vazio ]"));
                }
            }
            container.setItem(34, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));
            container.setItem(35, createPane(Items.BLACK_STAINED_GLASS_PANE, "§7§m---"));

            // Row 4: Divider (slots 36 to 44)
            for (int i = 36; i <= 44; i++) {
                container.setItem(i, createPane(Items.BLACK_STAINED_GLASS_PANE, "§8§m---------------------------"));
            }

            // Row 5: Bottom Row (slots 45 to 53)
            List<String> bookLore = List.of("§7Regras da batalha:", "§f1. Analise o time inimigo acima", "§f2. Escolha exatamente 4 Pokémon", "§f3. Clique em CONFIRMAR ao centro", "§7Quando ambos confirmarem, a batalha inicia!");
            container.setItem(45, createItem(Items.BOOK, "§e§lINSTRUÇÕES VGC DOUBLES", bookLore));

            for (int i = 46; i <= 53; i++) {
                if (i != 49) {
                    container.setItem(i, createPane(Items.GRAY_STAINED_GLASS_PANE, "§7§m---"));
                }
            }

            // Slot 49: Confirm Button
            if (isConfirmed) {
                List<String> lore = List.of("§aSua equipe já foi confirmada!", "§7Aguardando o oponente...");
                container.setItem(49, createItem(Items.GOLD_BLOCK, "§6§l★ SELEÇÃO CONFIRMADA ★", lore));
            } else if (selectedSlots.size() == 4) {
                List<String> lore = List.of("§7Você selecionou 4 Pokémon!", "§eClique aqui para confirmar sua equipe!");
                container.setItem(49, createItem(Items.EMERALD_BLOCK, "§a§l✔ CLIQUE PARA CONFIRMAR ✔", lore));
            } else {
                List<String> lore = List.of("§7Você selecionou: §e" + selectedSlots.size() + " §7/ §a4 §7Pokémon.", "§cSelecione exatamente 4 Pokémon para confirmar.");
                container.setItem(49, createItem(Items.RED_CONCRETE, "§c§l[ CONFIRMAR - INDISPONÍVEL ]", lore));
            }
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (slotId < 0) {
                return;
            }
            if (slotId >= 0 && slotId < 54) {
                handleMenuClick(slotId);
                this.sendAllDataToRemote();
                return;
            }
            if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP) {
                this.sendAllDataToRemote();
                return;
            }
            super.clicked(slotId, button, clickType, player);
        }

        private void handleMenuClick(int slotId) {
            boolean isConfirmed = viewer.getUUID().equals(session.getPlayerOneUuid())
                    ? (session.getPlayerOneSelection() != null && !session.getPlayerOneSelection().isEmpty())
                    : (session.getPlayerTwoSelection() != null && !session.getPlayerTwoSelection().isEmpty());
            if (isConfirmed) {
                TournamentMessages.send(viewer, "§cVocê já confirmou seu time! Aguarde o oponente.");
                return;
            }

            int pokemonSlot = -1; // 1 to 6
            if (slotId >= 19 && slotId <= 24) {
                pokemonSlot = (slotId - 19) + 1;
            } else if (slotId >= 28 && slotId <= 33) {
                pokemonSlot = (slotId - 28) + 1;
            }

            if (pokemonSlot != -1) {
                List<Pokemon> myTeam = PokemonTeamService.listPartyPokemon(viewer);
                if (pokemonSlot <= myTeam.size()) {
                    if (selectedSlots.contains(pokemonSlot)) {
                        selectedSlots.remove(Integer.valueOf(pokemonSlot));
                        viewer.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.MASTER, 0.5f, 0.8f);
                    } else {
                        if (selectedSlots.size() >= 4) {
                            TournamentMessages.send(viewer, "§cVocê já selecionou o máximo de 4 Pokémon! Deselecione um primeiro.");
                            viewer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 0.5f, 1.0f);
                        } else {
                            selectedSlots.add(pokemonSlot);
                            viewer.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.MASTER, 0.5f, 1.2f);
                        }
                    }
                    updateInventory();
                }
            } else if (slotId == 49) {
                if (selectedSlots.size() != 4) {
                    TournamentMessages.send(viewer, "§cVocê deve selecionar exatamente 4 Pokémon para confirmar!");
                    viewer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 0.5f, 1.0f);
                } else {
                    viewer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.7f, 1.0f);
                    int s1 = selectedSlots.get(0);
                    int s2 = selectedSlots.get(1);
                    int s3 = selectedSlots.get(2);
                    int s4 = selectedSlots.get(3);
                    TournamentBattleService.selectTeam(viewer, s1, s2, s3, s4);
                    updateInventory();
                }
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return session.getState().isPreviewActive() && System.currentTimeMillis() <= session.getPreviewExpiresAt();
        }

        @Override
        public void removed(Player player) {
            super.removed(player);

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            boolean isConfirmed = serverPlayer.getUUID().equals(session.getPlayerOneUuid())
                    ? (session.getPlayerOneSelection() != null && !session.getPlayerOneSelection().isEmpty())
                    : (session.getPlayerTwoSelection() != null && !session.getPlayerTwoSelection().isEmpty());

            if (!session.getState().isPreviewActive() || System.currentTimeMillis() > session.getPreviewExpiresAt() || isConfirmed) {
                return;
            }

            TournamentMessages.send(serverPlayer, "§cVocê não pode fechar o menu até confirmar os seus 4 Pokémon!");
            serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 0.5f, 1.0f);

            com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(0.1F, () -> {
                com.bigbang_tournaments.model.TournamentBattleSession latestSession = com.bigbang_tournaments.storage.TournamentBattleSessionStorage.loadSession(serverPlayer.getServer(), session.getSessionId());
                if (latestSession != null && latestSession.getState().isPreviewActive() && System.currentTimeMillis() <= latestSession.getPreviewExpiresAt()) {
                    boolean stillUnconfirmed = serverPlayer.getUUID().equals(latestSession.getPlayerOneUuid())
                            ? (latestSession.getPlayerOneSelection() == null || latestSession.getPlayerOneSelection().isEmpty())
                            : (latestSession.getPlayerTwoSelection() == null || latestSession.getPlayerTwoSelection().isEmpty());
                    if (stillUnconfirmed) {
                        open(serverPlayer, opponent, latestSession, config);
                    }
                }
                return kotlin.Unit.INSTANCE;
            });
        }

        private static ItemStack createPane(Item item, String name) {
            return createItem(item, name, null);
        }

        private static ItemStack createItem(Item item, String name, List<String> lore) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(Component.literal(line));
                }
                stack.set(DataComponents.LORE, new ItemLore(loreComponents));
            }
            return stack;
        }

        private static String capitalizeWord(String str) {
            if (str == null || str.isEmpty()) {
                return str;
            }
            StringBuilder sb = new StringBuilder();
            for (String word : str.split(" ")) {
                if (!word.isEmpty()) {
                    sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
                }
            }
            return sb.toString().trim();
        }
    }
}
