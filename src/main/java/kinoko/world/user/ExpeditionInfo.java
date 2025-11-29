package kinoko.world.user;

import kinoko.server.packet.InPacket;
import kinoko.server.packet.OutPacket;
import kinoko.util.Encodable;

public final class ExpeditionInfo implements Encodable {
    public static final ExpeditionInfo EMPTY = new ExpeditionInfo(0, 0, false);
    private final int expeditionId;
    private final int memberIndex;
    private final boolean master;

    public ExpeditionInfo(int expeditionId, int memberIndex, boolean master) {
        this.expeditionId = expeditionId;
        this.master = master;
        this.memberIndex = memberIndex;
    }

    public int getExpeditionId() {
        return expeditionId;
    }

    public int getMemberIndex() {
        return memberIndex;
    }

    public boolean isMaster() {
        return master;
    }

    @Override
    public String toString() {
        return "ExpeditionInfo{" +
                "expeditionId=" + expeditionId +
                ", memberIndex=" + memberIndex +
                ", master=" + master +
                '}';
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(expeditionId);
        outPacket.encodeByte(memberIndex);
        outPacket.encodeByte(master);
    }

    public static ExpeditionInfo decode(InPacket inPacket) {
        final int expeditionId = inPacket.decodeInt();
        final int memberIndex = inPacket.decodeByte();
        final boolean master = inPacket.decodeBoolean();
        return new ExpeditionInfo(expeditionId, memberIndex, master);
    }
}