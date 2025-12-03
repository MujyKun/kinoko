package kinoko.server.expedition;

import kinoko.server.packet.OutPacket;
import kinoko.server.party.Party;
import kinoko.server.user.RemoteTownPortal;
import kinoko.server.user.RemoteUser;
import kinoko.util.Encodable;
import kinoko.util.Lockable;
import kinoko.world.GameConstants;
import kinoko.world.user.ExpeditionInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class Expedition implements Encodable, Lockable<Expedition> {
    private static final RemoteUser EMPTY_MEMBER = new RemoteUser(0, 0, "", 0, 0, GameConstants.CHANNEL_OFFLINE, GameConstants.UNDEFINED_FIELD_ID, 0, 0, 0, 0, RemoteTownPortal.EMPTY);
    private static final Party EMPTY_PARTY = new Party(0, EMPTY_MEMBER);
    private final Lock lock = new ReentrantLock();
    private final int expedId;
    private final int expedQuestId;
    private final List<Party> expedParties;
    private int expedMasterId;
    private int expedMasterPartyIndex;
    private int expedMinLevel;
    private int expedMaxLevel;
    private int expedMaxMembers;
    private final Map<Integer, Integer> expedInvites; // invitee ID -> inviter ID

    private static final Logger log = LogManager.getLogger(Expedition.class);

    public Expedition(int expedId, int questId, RemoteUser remoteUser) {
        this.expedId = expedId;
        this.expedQuestId = questId;
        this.expedParties = new ArrayList<>(GameConstants.EXPEDITION_MAX); // Max of 5 parties in an expedition
        this.expedInvites = new HashMap<>();
        this.expedMasterId = remoteUser.getCharacterId();
        this.expedMasterPartyIndex = remoteUser.getPartyId();
    }

    public int getExpeditionId() {
        return expedId;
    }

    public int getQuestId() {
        return expedQuestId;
    }

    public int getFreeParty(RemoteUser remoteUser) {


        for (Party party : expedParties) {
            log.debug(party.getPartyId());
            if (party.getPartyBossId() == remoteUser.getCharacterId() || party.canAddMember(remoteUser)) {
                return party.getPartyId();//expedParties.indexOf(party);
            }
        }

        return -1;
    }

    public List<Party> getParties() {
        return expedParties;
    }

    public int getMasterId() { return this.expedMasterId; }

    public boolean setMasterId(int currentMasterId, int newMasterId) {
        if (expedMasterId != 0 && expedMasterId != currentMasterId) {
            log.debug("expedMasterId: {} | currentMasterId: {}", expedMasterId, currentMasterId);
            return false;
        }
        if (!hasMember(newMasterId)) {
            log.debug("Expedition {} does not have member {}", expedId, newMasterId);
            return false;
        }
        this.expedMasterId = newMasterId;
        log.debug("Successfully set expedMasterId to {}", expedMasterId);
        return true;
    }

    public int getMasterPartyIndex() { return this.expedMasterPartyIndex; }

    public void setMasterPartyIndex(int masterPartyIndex) {
        this.expedMasterPartyIndex = masterPartyIndex;
    }

    public int getMinLevel() { return this.expedMinLevel; }

    public void setMinLevel(int level) { this.expedMinLevel = level; }

    public int getMaxLevel() { return this.expedMaxLevel; }

    public void setMaxLevel(int level) { this.expedMaxLevel = level; }

    public int getMaxMembers() { return this.expedMaxMembers; }

    public void setMaxMembers(int members) { this.expedMaxMembers = members; }

    public Party getParty(int partyIndex) {
        return expedParties.get(partyIndex);
        //return expedParties.stream().filter(party -> party.getPartyId() == partyId).findFirst().orElse(null);
    }

    public boolean canAddMember(RemoteUser remoteUser) {

        int i = 0;
        for (Party party : expedParties) {
            if (party.canAddMember(remoteUser)) {
                return true;
            }

            if (i == expedParties.size()) {
                return expedParties.size() < GameConstants.EXPEDITION_MAX;
            }
            i++;
        }

        return false;
    }

    public boolean addMember(RemoteUser remoteUser) {

        int partyId = getFreeParty(remoteUser);
        if (partyId == 0) {
            log.debug("Tried to add user to full expedition.");
            return false;
        }

        for (Party party : expedParties) {
            if (party.getPartyId() == partyId) {
                party.addMember(remoteUser);
                return true;
            }
        }
        return false;
    }

    public boolean removeMember(RemoteUser remoteUser) {
        for (Party party : expedParties) {
            if (party.hasMember(remoteUser.getCharacterId())) {
                return party.removeMember(remoteUser);
            }
        }
        return false;
    }

    public boolean canAddParty(Party party) {
        if (expedParties.size() >= GameConstants.PARTY_MAX) {
            return false;
        }
        for (Party expedParty : expedParties) {
            if (expedParty.getPartyId() == party.getPartyId()) {
                return false;
            }
        }
        return true;
    }

    public boolean addParty(Party party) {
        if (!canAddParty(party)) {
            return false;
        }
        expedParties.add(party);
        return true;
    }

    public void removeParty(Party party) {
        expedParties.remove(party);
    }

    public void registerInvite(int inviterId, int targetId) {
        expedInvites.put(targetId, inviterId);
    }

    public boolean unregisterInvite(int inviterId, int targetId) {
        return expedInvites.remove(targetId) == inviterId;
    }

    public int getPartyBossId(RemoteUser remoteUser, int partyId) {

        Party party = new Party(partyId, remoteUser);

        int bossId = party.getPartyBossId();
        log.debug(bossId);
        return bossId;
    }

    public Optional<RemoteUser> getMember(int characterId) {
        return expedParties.stream()
                .filter((party) -> party.hasMember(characterId))
                .map((party) -> party.getMember(characterId))
                .findFirst()
                .orElse(Optional.empty());
    }

    public int getMemberIndex(RemoteUser remoteUser) {
        for (int i = 0; i < GameConstants.EXPEDITION_MAX; i++) {
            if (i >= expedParties.size()) {
                break;
            }
            //if (expedParties.get(i).getMember(remoteUser.getCharacterId()).isPresent()) {
            //    return i + 1; // used for affectedMemberBitMap
            //}
            if (expedParties.get(i).getMemberIndex(remoteUser) != 0) {
                return expedParties.get(i).getMemberIndex(remoteUser);
            }
        }
        return 0;
    }

    public int getPartyIndex(RemoteUser remoteUser) {
        for (int i = 0; i < GameConstants.EXPEDITION_MAX; i++) {
            if (i >= expedParties.size()) {
                break;
            }
            if (expedParties.get(i).getMemberIndex(remoteUser) != 0) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasMember(int characterId) {
        return getMember(characterId).isPresent();
    }

    public void updateMember(RemoteUser remoteUser) {
        for (Party party : expedParties) {
            if (party.hasMember(remoteUser.getCharacterId())) {
                party.updateMember(remoteUser);
            }
        }
    }

    public ExpeditionInfo createInfo(RemoteUser remoteUser) {
        return new ExpeditionInfo(expedId, getMemberIndex(remoteUser), expedParties.get(expedMasterPartyIndex).getPartyId() == remoteUser.getPartyId());
    }

    public void forEachParty(Consumer<Party> consumer) {
        for (Party party : expedParties) {
            consumer.accept(party);
        }
    }

    private void forEachPartyForExpedData(Consumer<Party> consumer) {
        for (int i = 0; i < GameConstants.EXPEDITION_MAX; i++) {
            if (i < expedParties.size()) {
                consumer.accept(expedParties.get(i));
            } else {
                consumer.accept(EMPTY_PARTY);
            }
        }
    }

    @Override
    public String toString() {
        return "Expedition{" +
                "expedId=" + expedId +
                ", expedParties=" + expedParties +
                ", expedMasterId=" + expedMasterId +
                ", expedMasterPartyId=" + expedMasterPartyIndex +
                '}';
    }

    @Override
    public void encode(OutPacket outPacket) {
        // EXPEDITION::Decode (900) (0x384)
        outPacket.encodeInt(expedQuestId); // nPartyQuestID
        outPacket.encodeInt(expedMasterPartyIndex); // nMasterPartyIndex
        forEachPartyForExpedData((party) -> party.encodeForExped(outPacket));
        outPacket.encodeByte(0); // undefined
        outPacket.encodeByte(0); // undefined
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

}