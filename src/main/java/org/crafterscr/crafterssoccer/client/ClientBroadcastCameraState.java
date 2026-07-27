package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.network.BroadcastCameraStatePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientBroadcastCameraState {

    private static boolean available;
    private static boolean allowed;
    private static boolean watching;
    private static int cameraEntityId = -1;
    private static String cameraName = "";

    private static boolean previousHideGui;
    private static boolean savedHideGuiState;

    private ClientBroadcastCameraState() {
    }

    public static void apply(
            BroadcastCameraStatePayload payload
    ) {
        boolean wasWatching = watching;

        available = payload.available();
        allowed = payload.allowed();
        watching = payload.watching();
        cameraEntityId = payload.cameraEntityId();
        cameraName = payload.cameraName();

        if (!wasWatching && watching) {
            enterTelevisionMode();
        } else if (wasWatching && !watching) {
            leaveTelevisionMode();
        }
    }

    public static void clientTick(
            Minecraft minecraft
    ) {
        if (!watching) {
            return;
        }

        minecraft.options.hideGui = true;

        if (minecraft.level == null
                || minecraft.player == null
                || cameraEntityId < 0) {
            return;
        }

        Entity operator =
                minecraft.level.getEntity(
                        cameraEntityId
                );

        if (operator != null
                && minecraft.getCameraEntity() != operator) {
            minecraft.setCameraEntity(
                    operator
            );
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean isAllowed() {
        return allowed;
    }

    public static boolean isWatching() {
        return watching;
    }

    public static String getCameraName() {
        return cameraName;
    }

    public static void reset() {
        leaveTelevisionMode();

        available = false;
        allowed = false;
        watching = false;
        cameraEntityId = -1;
        cameraName = "";
    }

    private static void enterTelevisionMode() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (!savedHideGuiState) {
            previousHideGui =
                    minecraft.options.hideGui;

            savedHideGuiState = true;
        }

        minecraft.options.hideGui = true;
    }

    private static void leaveTelevisionMode() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.setCameraEntity(
                    minecraft.player
            );
        }

        if (savedHideGuiState) {
            minecraft.options.hideGui =
                    previousHideGui;

            savedHideGuiState = false;
        }
    }
}
