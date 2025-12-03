package kinoko.server.expedition;

public enum ExpeditionResultType {
    // ExpeditionNoti
    LoadDone(57),
    LoadFail(58),
    CreateNewDone(59),
    JoinDone(60),
    YouJoined(61),
    YouJoined2(62),
    JoinFail(63),
    WithdrawDone(64),
    YouWithdrew(65),
    KickDone(66),
    YouKicked(67),
    Removed(68),
    MasterChanged(69),
    Modified(70),
    Modified2(71),
    Invite(72),
    ResponseInvite(73);

    private final int value;

    ExpeditionResultType(int value) { this.value = value; }

    public final int getValue() { return value; }

    public static ExpeditionResultType getByValue(int value) {
        for (ExpeditionResultType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}