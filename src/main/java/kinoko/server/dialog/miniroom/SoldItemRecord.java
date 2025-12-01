package kinoko.server.dialog.miniroom;

import kinoko.world.item.Item;

/**
 * Record of a sold item in the hired merchant.
 * Displayed in the owner's UI and saved to Fredrick when shop closes.
 */
public final class SoldItemRecord {
    private final int itemId;           // The item ID that was sold
    private final short quantity;       // Quantity sold
    private final int totalPrice;       // Total mesos earned
    private final String buyerName;     // Who bought it
    private final Item originalItem;    // Reference to original item (for saving to Fredrick)

    public SoldItemRecord(int itemId, short quantity, int totalPrice, String buyerName, Item originalItem) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.buyerName = buyerName;
        this.originalItem = originalItem;
    }

    public int getItemId() {
        return itemId;
    }

    public short getQuantity() {
        return quantity;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public Item getOriginalItem() {
        return originalItem;
    }
}
