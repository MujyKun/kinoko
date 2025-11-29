package kinoko.database.postgresql;

import com.zaxxer.hikari.HikariDataSource;
import kinoko.database.IdAccessor;
import kinoko.database.postgresql.type.ExpeditionDao;
import kinoko.database.postgresql.type.ItemDao;
import kinoko.database.postgresql.type.PartyDao;
import kinoko.world.item.Item;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public final class PostgresIdAccessor extends PostgresAccessor implements IdAccessor {

    public PostgresIdAccessor(HikariDataSource dataSource) {
        super(dataSource);
    }

    /**
     * Generates the next available party ID using the party sequence.
     *
     * Opens a new database connection, requests the next value from the
     * party_id_seq sequence, and returns it wrapped in an Optional.
     * If a database error occurs, Optional.empty() is returned.
     *
     * @return Optional containing the next party ID, or empty on failure
     */
    public Optional<Integer> nextPartyId() {
        try (Connection conn = getConnection()) {
            return PartyDao.create(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Generates the next available expedition ID using the expedition sequence.
     *
     * Opens a new database connection, requests the next value from the
     * expedition_id_seq sequence, and returns it wrapped in an Optional.
     * If a database error occurs, Optional.empty() is returned.
     *
     * @return Optional containing the next expedition ID, or empty on failure
     */
    public Optional<Integer> nextExpedId() {
        try (Connection conn = getConnection()) {
            return ExpeditionDao.create(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Generates a new item SN for the given item if it does not already have one.
     * If the item already has a serial number, this method returns true immediately.
     * Otherwise, it creates a new item entry in the database and assigns the generated ID.
     *
     * @param item the item for which to generate an ID
     * @return true if the item already had an ID or was successfully assigned one, false if an error occurred
     */
    @Override
    public boolean generateItemSn(Item item) {
        if (!item.hasNoSN()){
            return true;
        }

        try {
            return withTransaction(getConnection(), c -> ItemDao.createNewItem(c, item));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
