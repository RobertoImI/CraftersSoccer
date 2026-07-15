package org.crafterscr.crafterssoccer.match;

public enum SoccerTeamSide {

    RED("ROJO"),
    BLUE("AZUL");

    private final String displayName;

    SoccerTeamSide(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SoccerTeamSide opposite() {
        return this == RED
                ? BLUE
                : RED;
    }
}