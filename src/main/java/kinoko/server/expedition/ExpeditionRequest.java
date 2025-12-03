package kinoko.server.expedition;

import kinoko.server.header.CentralHeader;
import kinoko.server.packet.InPacket;
import kinoko.server.packet.OutPacket;
import kinoko.server.party.Party;
import kinoko.util.Encodable;

/**
 * Utility class for {@link CentralHeader#ExpeditionRequest}
 */
public final class ExpeditionRequest implements Encodable {
    private final ExpeditionRequestType requestType;
    private int expeditionId;
    private int selectQuestId;
    private String characterName;
    private int characterId;
    private int response;
    private int masterPartyId;
    private int partyId;
    private Party party;
    private boolean isDisconnect;

    ExpeditionRequest(ExpeditionRequestType requestType) {//, int selectQuestId) {
        this.requestType = requestType;
        //this.selectQuestId = selectQuestId;
    }

    public ExpeditionRequestType getRequestType() {
        return requestType;
    }

    public int getExpeditionId() {
        return expeditionId;
    }

    public int getSelectQuestId() {
        return selectQuestId;
    }

    public int getCharacterId() {
        return characterId;
    }

    public int getResponse() {
        return response;
    }

    public String getCharacterName() {
        return characterName;
    }

    public int getMasterPartyId() {
        return masterPartyId;
    }

    public int getPartyId() {
        return partyId;
    }

    public boolean isDisconnect() {
        return isDisconnect; // TODO: Unsure if needed
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(requestType.getValue());
        switch (requestType) {
            case Load -> {
                outPacket.encodeInt(expeditionId);
            }
            case CreateNew -> {
                outPacket.encodeInt(selectQuestId);
            }
            case Withdraw -> {
                // no encodes
            }
            case Response -> {
                outPacket.encodeString(characterName);
                outPacket.encodeInt(response);
            }
            case Kick -> {
                outPacket.encodeInt(characterId);
            }
            case Invite -> {
                outPacket.encodeString(characterName);
            }
            case RelocateMember -> {
                outPacket.encodeInt(partyId);
                outPacket.encodeInt(characterId);
            }
            case ChangeMaster -> {
                outPacket.encodeInt(characterId);
            }
            case ChangePartyBoss -> {
                outPacket.encodeInt(characterId);
                outPacket.encodeByte(isDisconnect);

            }
        }
    }

    public static ExpeditionRequest decode(InPacket inPacket) {
        final int type = inPacket.decodeByte();
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.getByValue(type));
        switch (request.requestType) {
            case Load -> {
                request.expeditionId = inPacket.decodeInt();
            }
            case CreateNew -> {
                request.selectQuestId = inPacket.decodeInt();
            }
            case Kick -> {
                request.characterId = inPacket.decodeInt();
            }
            case Response -> {
                request.characterName = inPacket.decodeString();
                request.response = inPacket.decodeInt(); // 9 - (bAccept != 0)
            }
            case Invite -> {
                request.characterName = inPacket.decodeString();
            }
            case ChangeMaster -> {
                request.characterId = inPacket.decodeInt();
            }
            case ChangePartyBoss -> {
                request.characterId = inPacket.decodeInt();
                request.isDisconnect = inPacket.decodeBoolean();
            }
            case RelocateMember -> {
                request.partyId = inPacket.decodeInt();
                request.characterId = inPacket.decodeInt();
            }
            case Withdraw -> {
                //No decodes
            }
            case null -> {
                throw new IllegalStateException(String.format("Unknown expedition request type %d", type));
            }
            default -> {
                throw new IllegalStateException(String.format("Unhandled expedition request type %d", type));
            }
        }
        return request;
    }

    public static ExpeditionRequest loadExpedition(int expeditionId) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.Load);
        request.expeditionId = expeditionId;
        return request;
    }

    public static ExpeditionRequest createNewExpedition(int selectQuestId) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.CreateNew);
        request.selectQuestId = selectQuestId;
        return request;
    }

    public static ExpeditionRequest withdrawExpedition() {
        return new ExpeditionRequest(ExpeditionRequestType.Withdraw);
    }

    public static ExpeditionRequest response(String characterName, int response) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.Response);
        request.characterName = characterName;
        request.response = response;
        return request;
    }

    public static ExpeditionRequest invite(String characterName) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.Invite);
        request.characterName = characterName;
        return request;
    }

    public static ExpeditionRequest kickExpedition(int targetId) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.Kick);
        request.characterId = targetId;
        return request;
    }

    public static ExpeditionRequest changeMaster(int targetId, boolean isDisconnect) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.ChangeMaster);
        request.characterId = targetId;
        return request;
    }

    public static ExpeditionRequest changePartyBoss(int targetId, boolean isDisconnect) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.ChangePartyBoss);
        request.characterId = targetId;
        request.isDisconnect = isDisconnect;
        return request;
    }

    public static ExpeditionRequest relocateMember(int targetId, int partyId) {
        final ExpeditionRequest request = new ExpeditionRequest(ExpeditionRequestType.RelocateMember);
        request.characterId = targetId;
        request.partyId = partyId;
        return request;
    }
}