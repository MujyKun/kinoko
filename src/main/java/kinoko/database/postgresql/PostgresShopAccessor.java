package kinoko.database.postgresql;

import com.zaxxer.hikari.HikariDataSource;
import kinoko.database.ShopAccessor;
import kinoko.database.postgresql.type.ShopDao;
import kinoko.database.postgresql.util.SQLBooleanAction;
import kinoko.server.dialog.miniroom.ShopItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * PostgreSQL implementation of ShopAccessor for the Hired Merchant / Fredrick system.
 */
public final class PostgresShopAccessor extends PostgresAccessor implements ShopAccessor {

    public PostgresShopAccessor(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<ShopItem> getShopItemsByCharacterId(int characterId) {
        try (Connection conn = getConnection()) {
            return ShopDao.getShopItemsByCharacterId(conn, characterId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public List<ShopItem> getUnsoldItemsByCharacterId(int characterId) {
        try (Connection conn = getConnection()) {
            return ShopDao.getUnsoldItemsByCharacterId(conn, characterId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public List<ShopItem> getSoldItemsByCharacterId(int characterId) {
        try (Connection conn = getConnection()) {
            return ShopDao.getSoldItemsByCharacterId(conn, characterId);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasItemsInFredrick(int characterId) {
        try (Connection conn = getConnection()) {
            return ShopDao.hasItemsInFredrick(conn, characterId);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean saveUnsoldItem(ShopItem shopItem) {
        return withTransaction((SQLBooleanAction) conn -> ShopDao.saveUnsoldItem(conn, shopItem));
    }

    @Override
    public boolean saveSoldItem(ShopItem shopItem) {
        return withTransaction((SQLBooleanAction) conn -> ShopDao.saveSoldItem(conn, shopItem));
    }

    @Override
    public boolean deleteShopItem(long shopItemId) {
        return withTransaction((SQLBooleanAction) conn -> ShopDao.deleteShopItem(conn, shopItemId));
    }

    @Override
    public boolean deleteAllShopItems(int characterId) {
        return withTransaction((SQLBooleanAction) conn -> ShopDao.deleteAllShopItems(conn, characterId));
    }

    @Override
    public long getTotalMesosToCollect(int characterId) {
        try (Connection conn = getConnection()) {
            return ShopDao.getTotalMesosToCollect(conn, characterId);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
