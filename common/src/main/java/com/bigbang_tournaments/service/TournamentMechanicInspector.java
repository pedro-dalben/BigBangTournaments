package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentSpecialMechanic;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.yajatkaul.mega_showdown.tag.MegaShowdownTags;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.EquipmentChecking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public final class TournamentMechanicInspector {
    private TournamentMechanicInspector() {
    }

    public static Set<TournamentSpecialMechanic> detectMechanics(ServerPlayer player, Collection<Pokemon> party) {
        EnumSet<TournamentSpecialMechanic> mechanics = EnumSet.noneOf(TournamentSpecialMechanic.class);

        for (Pokemon pokemon : party) {
            if (pokemon == null) {
                continue;
            }

            ItemStack heldItem = pokemon.heldItem();
            if (!heldItem.isEmpty()) {
                if (heldItem.is(MegaShowdownTags.Items.MEGA_STONE)) {
                    mechanics.add(TournamentSpecialMechanic.MEGA_EVOLUTION);
                }
                if (heldItem.is(MegaShowdownTags.Items.Z_CRYSTAL)) {
                    mechanics.add(TournamentSpecialMechanic.Z_MOVE);
                }
            }

            if (pokemon.getGmaxFactor() || pokemon.getDmaxLevel() > 0) {
                mechanics.add(TournamentSpecialMechanic.DYNAMAX);
            }
        }

        AccessoriesCapability.getOptionally(player).ifPresent(capability -> {
            for (SlotEntryReference equipped : capability.getAllEquipped(true)) {
                ItemStack stack = equipped.stack();
                if (stack.isEmpty()) {
                    continue;
                }

                if (stack.is(MegaShowdownTags.Items.MEGA_BRACELET)) {
                    mechanics.add(TournamentSpecialMechanic.MEGA_EVOLUTION);
                }
                if (stack.is(MegaShowdownTags.Items.Z_RING)) {
                    mechanics.add(TournamentSpecialMechanic.Z_MOVE);
                }
                if (stack.is(MegaShowdownTags.Items.TERA_ORB)) {
                    mechanics.add(TournamentSpecialMechanic.TERASTALLIZATION);
                }
                if (stack.is(MegaShowdownTags.Items.DYNAMAX_BAND)) {
                    mechanics.add(TournamentSpecialMechanic.DYNAMAX);
                }
                if (stack.is(MegaShowdownTags.Items.OMNI_RING)) {
                    mechanics.add(TournamentSpecialMechanic.MEGA_EVOLUTION);
                    mechanics.add(TournamentSpecialMechanic.Z_MOVE);
                    mechanics.add(TournamentSpecialMechanic.TERASTALLIZATION);
                    mechanics.add(TournamentSpecialMechanic.DYNAMAX);
                }
            }
        });

        return mechanics;
    }
}
