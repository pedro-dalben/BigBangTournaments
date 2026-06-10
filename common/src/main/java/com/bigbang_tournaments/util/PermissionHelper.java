package com.bigbang_tournaments.util;

import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class PermissionHelper {
    private PermissionHelper() {
    }

    public static boolean hasPermission(ServerPlayer player, String permission) {
        // 1. Try LuckPerms via reflection to avoid direct compilation dependencies
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = providerClass.getMethod("get").invoke(null);
            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUUID());
            if (user != null) {
                Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
                
                // Try simple checkPermission(String) first
                try {
                    Object checkResult = permissionData.getClass().getMethod("checkPermission", String.class).invoke(permissionData, permission);
                    if (checkResult != null) {
                        return (boolean) checkResult.getClass().getMethod("asBoolean").invoke(checkResult);
                    }
                } catch (NoSuchMethodException e) {
                    // Fall back to context-based check
                    Object contextManager = lp.getClass().getMethod("getContextManager").invoke(lp);
                    Object queryOptions = contextManager.getClass().getMethod("getQueryOptions", player.getClass()).invoke(contextManager, player);
                    Class<?> queryOptionsClass = Class.forName("net.luckperms.api.query.QueryOptions");
                    Object checkResult = permissionData.getClass().getMethod("checkPermission", String.class, queryOptionsClass).invoke(permissionData, permission, queryOptions);
                    if (checkResult != null) {
                        return (boolean) checkResult.getClass().getMethod("asBoolean").invoke(checkResult);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 2. Fallback: check if player is OP or has permission level 2 (admins can do anything)
        if (player.hasPermissions(2)) {
            return true;
        }

        return false;
    }

    public static int getMaxRolls(ServerPlayer player, int defaultValue) {
        // Check permissions tournament.roll.100 down to 1
        for (int i = 100; i >= 1; i--) {
            if (hasPermission(player, "tournament.roll." + i)) {
                return i;
            }
        }
        return defaultValue;
    }
}
