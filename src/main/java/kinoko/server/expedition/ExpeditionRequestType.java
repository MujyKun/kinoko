package kinoko.server.expedition;

public enum ExpeditionRequestType {
    // ExpedReq
    Load(48),
    CreateNew(49),
    Invite(50),
    Response(51),
    Withdraw(52),
    Kick(53),
    ChangeMaster(54),
    ChangePartyBoss(55),
    RelocateMember(56);

    private final int value;

    ExpeditionRequestType(int value) { this.value = value; }

    public final int getValue() { return value; }

    public static ExpeditionRequestType getByValue(int value) {
        for (ExpeditionRequestType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}