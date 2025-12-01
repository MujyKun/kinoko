package kinoko.database;

import kinoko.server.dialog.miniroom.ShopItem;

import java.util.List;

/**
 * Database accessor for the player.shop table (Hired Merchant / Fredrick system).
 */
public interface ShopAccessor {

    /**
     * Get all shop items (sold and unsold) for a character.
     * Used by Fredrick NPC to show retrievable items.
     *
     * @param characterId the character ID
     * @return list of shop items
     */
    List<ShopItem> getShopItemsByCharacterId(int characterId);

    /**
     * Get only unsold items for a character.
     *
     * @param characterId the character ID
     * @return list of unsold shop items
     */
    List<ShopItem> getUnsoldItemsByCharacterId(int characterId);

    /**
     * Get only sold items (with mesos to collect) for a character.
     *
     * @param characterId the character ID
     * @return list of sold shop items
     */
    List<ShopItem> getSoldItemsByCharacterId(int characterId);

    /**
     * Check if a character has any items in Fredrick (shop table).
     * Used to prevent opening a new shop before claiming items.
     *
     * @param characterId the character ID
     * @return true if character has items to retrieve
     */
    boolean hasItemsInFredrick(int characterId);

    /**
     * Save an unsold item to the shop table (when shop closes/expires).
     *
     * @param shopItem the shop item to save
     * @return true if successful
     */
    boolean saveUnsoldItem(ShopItem shopItem);

    /**
     * Save a sold item record to the shop table (when item is purchased).
     *
     * @param shopItem the sold item record
     * @return true if successful
     */
    boolean saveSoldItem(ShopItem shopItem);

    /**
     * Delete a shop item by ID (after player retrieves it from Fredrick).
     *
     * @param shopItemId the shop item ID
     * @return true if successful
     */
    boolean deleteShopItem(long shopItemId);

    /**
     * Delete all shop items for a character (after claiming all from Fredrick).
     *
     * @param characterId the character ID
     * @return true if successful
     */
    boolean deleteAllShopItems(int characterId);

    /**
     * Get total mesos to collect from sold items.
     *
     * @param characterId the character ID
     * @return total mesos
     */
    long getTotalMesosToCollect(int characterId);
}
