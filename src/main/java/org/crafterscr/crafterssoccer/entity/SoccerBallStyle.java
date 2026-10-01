package org.crafterscr.crafterssoccer.entity;

import java.util.Locale;

/**
 * Apariencias disponibles para el balón.
 *
 * El valor networkId viaja sincronizado con cada entidad para que todos
 * los clientes rendericen exactamente el mismo diseño.
 */
public enum SoccerBallStyle {

    DEFAULT(
            0,
            "default"
    ),

    POKEBALL(
            1,
            "pokeball"
    ),

    PIKACHU(
            2,
            "pikachu"
    );

    private final int networkId;
    private final String commandName;

    SoccerBallStyle(
            int networkId,
            String commandName
    ) {
        this.networkId = networkId;
        this.commandName = commandName;
    }

    public int getNetworkId() {
        return networkId;
    }

    public String getCommandName() {
        return commandName;
    }

    public static SoccerBallStyle fromNetworkId(
            int networkId
    ) {
        for (SoccerBallStyle style : values()) {
            if (style.networkId == networkId) {
                return style;
            }
        }

        return DEFAULT;
    }

    public static SoccerBallStyle fromCommandName(
            String value
    ) {
        if (value == null) {
            return DEFAULT;
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        for (SoccerBallStyle style : values()) {
            if (style.commandName.equals(
                    normalized
            )) {
                return style;
            }
        }

        return DEFAULT;
    }
}
