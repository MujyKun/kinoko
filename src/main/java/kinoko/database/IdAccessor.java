package kinoko.database;

import kinoko.world.item.Item;

import java.util.Optional;

public interface IdAccessor {
    default Optional<Integer> nextAccountId(){
        if (!DatabaseManager.isRelational()){
            throw new UnsupportedOperationException("nextAccountId needs to be implemented for this database.");
        }
        return Optional.of(-1);
    }

    default Optional<Integer> nextCharacterId(){
        if (!DatabaseManager.isRelational()){
            throw new UnsupportedOperationException("nextCharacterId needs to be implemented for this database.");
        }
        return Optional.of(-1);
    }

    Optional<Integer> nextPartyId();

    default Optional<Integer> nextGuildId(){
        if (!DatabaseManager.isRelational()){
            throw new UnsupportedOperationException("nextGuildId needs to be implemented for this database.");
        }
        return Optional.of(-1);
    }

    Optional<Integer> nextExpedId();

    default Optional<Integer> nextMemoId(){
        if (!DatabaseManager.isRelational()){
            throw new UnsupportedOperationException("nextMemoId needs to be implemented for this database.");
        }
        return Optional.of(-1);
    }

    default boolean generateItemSn(Item item){
        if (DatabaseManager.isRelational()){
            throw new UnsupportedOperationException("generateItemSn() needs to be implemented for this database.");
        }
        return true;
    }
}
