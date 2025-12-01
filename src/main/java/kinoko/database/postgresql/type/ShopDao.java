package kinoko.database.postgresql.type;

import kinoko.server.dialog.miniroom.ShopItem;
import kinoko.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the player.shop table.
 */
public class ShopDao {
    private static final Logger log = LogManager.getLogger(ShopDao.class);

    /**
     * Get all shop items for a character.
     */
    public static List<ShopItem> getShopItemsByCharacterId(Connection conn, int characterId) throws SQLException {
        return getShopItems(conn, characterId, null);
    }

    /**
     * Get unsold items for a character.
     */
    public static List<ShopItem> getUnsoldItemsByCharacterId(Connection conn, int characterId) throws SQLException {
        return getShopItems(conn, characterId, false);
    }

    /**
     * Get sold items for a character.
     */
    public static List<ShopItem> getSoldItemsByCharacterId(Connection conn, int characterId) throws SQLException {
        return getShopItems(conn, characterId, true);
    }

    /**
     * Internal method to get shop items with optional sold filter.
     */
    private static List<ShopItem> getShopItems(Connection conn, int characterId, Boolean sold) throws SQLException {
        List<ShopItem> items = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT s.id, s.character_id, s.item_sn, s.price, s.bundles, s.sold, s.mesos, s.buyer_name,
                   i.item_id, i.quantity, i.attribute, i.title, i.date_expire
            FROM player.shop s
            LEFT JOIN item.items i ON s.item_sn = i.item_sn
            WHERE s.character_id = ?
            """);

        if (sold != null) {
            sql.append(" AND s.sold = ?");
        }
        sql.append(" ORDER BY s.id");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, characterId);
            if (sold != null) {
                ps.setBoolean(2, sold);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item item = null;
                    long itemSn = rs.getLong("item_sn");
                    if (!rs.wasNull() && itemSn > 0) {
                        int itemId = rs.getInt("item_id");
                        short quantity = rs.getShort("quantity");
                        item = new Item(itemId, quantity);
                        item.setItemSn(itemSn);
                        item.setAttribute(rs.getShort("attribute"));
                        item.setTitle(rs.getString("title"));
                        Timestamp dateExpire = rs.getTimestamp("date_expire");
                        if (dateExpire != null) {
                            item.setDateExpire(dateExpire.toInstant());
                        }
                    }

                    ShopItem shopItem = new ShopItem(
                            rs.getLong("id"),
                            rs.getInt("character_id"),
                            item,
                            rs.getInt("price"),
                            (short) rs.getInt("bundles"),
                            rs.getBoolean("sold"),
                            rs.getLong("mesos"),
                            rs.getString("buyer_name")
                    );
                    items.add(shopItem);
                }
            }
        }
        return items;
    }

    /**
     * Check if a character has any items in the shop table.
     */
    public static boolean hasItemsInFredrick(Connection conn, int characterId) throws SQLException {
        String sql = "SELECT 1 FROM player.shop WHERE character_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Save an unsold item to the shop table.
     * The item must already exist in item.items table.
     */
    public static boolean saveUnsoldItem(Connection conn, ShopItem shopItem) throws SQLException {
        // First ensure the item exists in item.items
        if (shopItem.getItem() != null && shopItem.getItem().getItemSn() <= 0) {
            ItemDao.createNewItem(conn, shopItem.getItem());
        }

        String sql = """
            INSERT INTO player.shop (character_id, item_sn, price, bundles, sold, mesos, buyer_name)
            VALUES (?, ?, ?, ?, FALSE, 0, NULL)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopItem.getCharacterId());
            if (shopItem.getItem() != null) {
                ps.setLong(2, shopItem.getItem().getItemSn());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setInt(3, shopItem.getPrice());
            ps.setShort(4, shopItem.getBundles());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Save a sold item record to the shop table.
     */
    public static boolean saveSoldItem(Connection conn, ShopItem shopItem) throws SQLException {
        // First ensure the item exists in item.items if present
        if (shopItem.getItem() != null && shopItem.getItem().getItemSn() <= 0) {
            ItemDao.createNewItem(conn, shopItem.getItem());
        }

        String sql = """
            INSERT INTO player.shop (character_id, item_sn, price, bundles, sold, mesos, buyer_name)
            VALUES (?, ?, ?, ?, TRUE, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopItem.getCharacterId());
            if (shopItem.getItem() != null) {
                ps.setLong(2, shopItem.getItem().getItemSn());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setInt(3, shopItem.getPrice());
            ps.setShort(4, shopItem.getBundles());
            ps.setLong(5, shopItem.getMesos());
            ps.setString(6, shopItem.getBuyerName());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Delete a shop item by ID.
     */
    public static boolean deleteShopItem(Connection conn, long shopItemId) throws SQLException {
        String sql = "DELETE FROM player.shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shopItemId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Delete all shop items for a character.
     */
    public static boolean deleteAllShopItems(Connection conn, int characterId) throws SQLException {
        String sql = "DELETE FROM player.shop WHERE character_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            return ps.executeUpdate() >= 0; // 0 is valid if no items exist
        }
    }

    /**
     * Get total mesos to collect from sold items.
     */
    public static long getTotalMesosToCollect(Connection conn, int characterId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(mesos), 0) FROM player.shop WHERE character_id = ? AND sold = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }
}
