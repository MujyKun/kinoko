package kinoko.database.postgresql.type;


import java.sql.*;
import java.util.Optional;

public class ExpeditionDao {

    /**
     * Generates the next available expedition ID from the expedition_id_seq sequence.
     *
     * Executes a SELECT nextval(...) query to obtain the next sequence value,
     * safely casts it to an integer, and returns it wrapped in an Optional.
     * If the sequence value exceeds the integer range, a SQLException is thrown.
     *
     * @param conn an active SQL connection used to execute the query
     * @return Optional containing the next expedition ID, or empty if the query returned no value
     * @throws SQLException if a database error occurs or the sequence value exceeds integer range
     */
    public static Optional<Integer> create(Connection conn) throws SQLException {
        String sql = "SELECT nextval('expedition_id_seq')";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                long id = rs.getLong(1);

                if (id > Integer.MAX_VALUE) {  // ensure it's safe to cast
                    throw new SQLException("Sequence value exceeds Integer range: " + id);
                }

                return Optional.of((int) id);
            }
            return Optional.empty();
        }
    }
}
