package kinoko.server.expedition;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ExpeditionStorage {
    private final ConcurrentHashMap<Integer, Expedition> expeditionMap = new ConcurrentHashMap<>();

    public void addExpedition(Expedition expedition) {
        expeditionMap.put(expedition.getExpeditionId(), expedition);
    }

    public boolean removeExpedition(Expedition expedition) {
        return expeditionMap.remove(expedition.getExpeditionId(), expedition);
    }

    public Optional<Expedition> getExpeditionById(int expeditionId) {
        return Optional.ofNullable(expeditionMap.get(expeditionId));
    }
}