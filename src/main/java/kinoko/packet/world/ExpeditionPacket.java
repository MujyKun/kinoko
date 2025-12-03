package kinoko.packet.world;

import kinoko.server.expedition.Expedition;
import kinoko.server.expedition.ExpeditionResultType;
import kinoko.server.header.OutHeader;
import kinoko.server.packet.OutPacket;
import kinoko.server.party.Party;
import kinoko.server.user.RemoteUser;

public class ExpeditionPacket {
    // CWvsContext::OnExpedtionResult ----------------------------------------------------------------------------------

    public static OutPacket loadExpedDone(Expedition expedition) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.LoadDone);
        expedition.encode(outPacket);
        return outPacket;
    }

    public static OutPacket loadExpedFail() { return ExpeditionPacket.of(ExpeditionResultType.LoadFail); }

    public static OutPacket createNewExpedDone(Expedition expedition) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.CreateNewDone);
        expedition.encode(outPacket);
        return outPacket;
    }

    public static OutPacket withdrawExpedDone(Expedition exped, RemoteUser member) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.WithdrawDone);
        outPacket.encodeString(member.getCharacterName());
        return outPacket;
    }

    public static OutPacket withdrewExped(RemoteUser member) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.YouWithdrew);
        outPacket.encodeString(member.getCharacterName());
        return outPacket;
    }

    public static OutPacket joinExpedDone(Expedition expedition, RemoteUser remoteUser) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.JoinDone);
        outPacket.encodeString(remoteUser.getCharacterName());
        return outPacket;
    }

    public static OutPacket youJoinedExped(Expedition expedition, RemoteUser remoteUser) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.YouJoined);
        expedition.encode(outPacket);
        return outPacket;
    }

    public static OutPacket youJoinedExped2() { return ExpeditionPacket.of(ExpeditionResultType.YouJoined2); }

    public static OutPacket joinExpedFail() { return ExpeditionPacket.of(ExpeditionResultType.JoinFail); }

    public static OutPacket kickExpedDone(RemoteUser member) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.KickDone);
        outPacket.encodeString(member.getCharacterName());
        return outPacket;
    }

    public static OutPacket changeMaster(int newMasterId) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.MasterChanged);
        outPacket.encodeInt(newMasterId);
        return outPacket;
    }

    public static OutPacket modified(Expedition exped, Party party) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.Modified);
        outPacket.encodeInt(exped.getMasterPartyIndex()); // nMasterPartyIndex
        outPacket.encodeInt(party.getPartyId()); // nPartyIndex
        party.encodeForExped(outPacket); // p_partyMember
        return outPacket;
    }

    public static OutPacket inviteExped(RemoteUser remoteUser, int partyQuestId) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.Invite);
        int level = remoteUser.getLevel();
        int job = remoteUser.getJob();
        String name = remoteUser.getCharacterName();

        outPacket.encodeInt(level);
        outPacket.encodeInt(job);
        outPacket.encodeString(name);
        outPacket.encodeInt(partyQuestId);

        return outPacket;
    }

    public static OutPacket responseInvite(RemoteUser remoteUser, int response) {
        final OutPacket outPacket = ExpeditionPacket.of(ExpeditionResultType.ResponseInvite);

        outPacket.encodeInt(response); //nResponse
        outPacket.encodeString(remoteUser.getCharacterName()); //p_sName

        return outPacket;
    }

    public static OutPacket of(ExpeditionResultType resultType) {
        final OutPacket outPacket = OutPacket.of(OutHeader.ExpeditionNoti);
        outPacket.encodeByte(resultType.getValue());
        return outPacket;
    }
}