package kinoko.database;

public interface DatabaseConnector {
    IdAccessor getIdAccessor();

    AccountAccessor getAccountAccessor();

    CharacterAccessor getCharacterAccessor();

    FriendAccessor getFriendAccessor();

    GuildAccessor getGuildAccessor();

    GiftAccessor getGiftAccessor();

    MemoAccessor getMemoAccessor();

    ItemAccessor getItemAccessor();

    FamilyAccessor getFamilyAccessor();

    ShopAccessor getShopAccessor();

    void initialize();

    void shutdown();
}
