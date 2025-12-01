package kinoko.server.dialog.miniroom;

import kinoko.world.item.Item;

public final class PlayerShopItem {
    private final Item item;      // Item with quantity = perBundle
    private final int price;      // Price per bundle
    public short bundles;         // Number of bundles available (public like reference)


    public PlayerShopItem(Item item, short bundles, int price) {
        this.item = item;
        this.bundles = bundles;
        this.price = price;
    }

    public Item getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    /**
     * Get per-bundle quantity (stored in item.getQuantity())
     */
    public int getSetSize() {
        return item.getQuantity();
    }

    /**
     * Get number of bundles available
     */
    public int getSetCount() {
        return bundles;
    }
}
