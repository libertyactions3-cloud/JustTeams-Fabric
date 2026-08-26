package eu.kotori.justTeams.team;

/**
 * Seven-step member rank ladder requested for the Fabric team member system.
 * Leader is reserved for the team owner; the legacy TeamRole remains for
 * compatibility with existing ownership/elevated-permission checks.
 */
public enum TeamRank {
    LEADER("Leader"),
    CO_LEADER("Co-Leader"),
    OFFICER("Officer"),
    UNDEROFFICER("Underofficer"),
    ASSOCIATE("Associate"),
    MEMBER("Member"),
    INITIATE("Initiate");

    private final String displayName;

    TeamRank(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TeamRank promote() {
        return switch (this) {
            case INITIATE -> MEMBER;
            case MEMBER -> ASSOCIATE;
            case ASSOCIATE -> UNDEROFFICER;
            case UNDEROFFICER -> OFFICER;
            case OFFICER -> CO_LEADER;
            case CO_LEADER, LEADER -> this;
        };
    }

    public TeamRank demote() {
        return switch (this) {
            case LEADER, INITIATE -> this;
            case CO_LEADER -> OFFICER;
            case OFFICER -> UNDEROFFICER;
            case UNDEROFFICER -> ASSOCIATE;
            case ASSOCIATE -> MEMBER;
            case MEMBER -> INITIATE;
        };
    }
}
