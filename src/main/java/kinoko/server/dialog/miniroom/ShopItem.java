package kinoko.server.dialog.miniroom;

import kinoko.world.item.Item;

/**
 * Represents an item stored in the player.shop table for Fredrick retrieval.
 * Can be either an unsold item (sold=false) or a sold item record (sold=true).
 */
public final class ShopItem {
    private final long id;              // Primary key from player.shop
    private final int characterId;      // Owner character ID
    private final Item item;            // The actual item (null for sold records after item is delivered)
    private final int price;            // Price per bundle
    private final short bundles;        // Number of bundles
    private final boolean sold;         // false = unsold item, true = sold record
    private final long mesos;           // Mesos earned (only for sold = true)
    private final String buyerName;     // Buyer name (only for sold = true)

    public ShopItem(long id, int characterId, Item item, int price, short bundles, boolean sold, long mesos, String buyerName) {
        this.id = id;
        this.characterId = characterId;
        this.item = item;
        this.price = price;
        this.bundles = bundles;
        this.sold = sold;
        this.mesos = mesos;
        this.buyerName = buyerName;
    }

    /**
     * Create an unsold shop item (for saving when shop closes)
     */
    public static ShopItem unsold(int characterId, Item item, int price, short bundles) {
        return new ShopItem(0, characterId, item, price, bundles, false, 0, null);
    }

    /**
     * Create a sold shop item record (for sale history)
     */
    public static ShopItem sold(int characterId, Item item, int price, short bundles, long mesos, String buyerName) {
        return new ShopItem(0, characterId, item, price, bundles, true, mesos, buyerName);
    }

    public long getId() {
        return id;
    }

    public int getCharacterId() {
        return characterId;
    }

    public Item getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public short getBundles() {
        return bundles;
    }

    public boolean isSold() {
        return sold;
    }

    public long getMesos() {
        return mesos;
    }

    public String getBuyerName() {
        return buyerName;
    }
}
