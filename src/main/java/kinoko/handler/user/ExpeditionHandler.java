package kinoko.handler.user;

import kinoko.handler.Handler;
import kinoko.packet.world.ExpeditionPacket;
import kinoko.packet.world.PartyPacket;
import kinoko.server.expedition.ExpeditionRequest;
import kinoko.server.expedition.ExpeditionRequestType;
import kinoko.server.expedition.ExpeditionResultType;
import kinoko.server.header.InHeader;
import kinoko.server.packet.InPacket;
import kinoko.world.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ExpeditionHandler {

    private static final Logger log = LogManager.getLogger(ExpeditionHandler.class);

    @Handler(InHeader.ExpeditionRequest)
    public static void handleExpeditionRequest(User user, InPacket inPacket) {

        byte type = inPacket.decodeByte();
        final ExpeditionRequestType requestType = ExpeditionRequestType.getByValue(type);

        switch(requestType) {
            case CreateNew -> {
                //ExpeditionIntermediary::SendExpCreatePacket
                int nSelectQuestID = inPacket.decodeInt();

                if (user.hasExpedition()) {
                    user.write(ExpeditionPacket.of(ExpeditionResultType.JoinFail));
                    return;
                }
                //Submit expedition request
                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.createNewExpedition(nSelectQuestID));
            }
            case Invite -> {
                //ExpeditionIntermediary::SendExpInvitePacket
                String sTargetName = inPacket.decodeString();

                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.invite(sTargetName));

            }
            case Response -> {
                //ExpeditionIntermediary::SendResponseInvitePacket
                String sCharacterName = inPacket.decodeString();
                int response = inPacket.decodeInt(); // 9 - (bAccept != 0)
                log.debug("name: {} | response: {}", sCharacterName, response);
                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.response(sCharacterName, response));
            }
            case Withdraw -> {
                //ExpeditionIntermediary::SendWithdrawPacket
                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.withdrawExpedition());
            }
            case Kick -> {
                int charID = inPacket.decodeInt();
                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.kickExpedition(charID));
            }
            case ChangeMaster -> {
                // CField::SendChangePartyBossMsg
                if (!user.isExpeditionMaster()) {
                    log.debug("User {} tried to change expedition leader.", user.getCharacterName());
                    return;
                }
                final int targetId = inPacket.decodeInt();
                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.changeMaster(targetId, false));
            }
            case ChangePartyBoss -> {
                // CField::SendChangePartyBossMsg
                if (!user.isPartyBoss()) {
                    user.write(PartyPacket.serverMsg("You are not the leader of the party."));
                    return;
                }
                final int targetId = inPacket.decodeInt();
                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.changePartyBoss(targetId, false));
            }
            case RelocateMember -> {
                int toIndex = inPacket.decodeInt();
                int charId = inPacket.decodeInt();

                user.getConnectedServer().submitExpeditionRequest(user, ExpeditionRequest.relocateMember(charId, toIndex));
            }
            case null -> {
                log.error("Unknown expedition request type : {}", type);
            }
            default -> {
                log.error("Unhandled expedition request type : {}", requestType);
            }
        }







        /*

        //ExpeditionIntermediary::OnPacketExpNoti_Invite
        //case invite (packetfail)
        //case invite
        byte idk = inPacket.decodeByte();
        String idk2 = inPacket.decodeString();
        int idk3 = inPacket.decodeInt();
        //Submit expedition invite*/

    }


}