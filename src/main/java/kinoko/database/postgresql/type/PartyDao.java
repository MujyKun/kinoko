package kinoko.database.postgresql.type;


import java.sql.*;
import java.util.Optional;

public class PartyDao {

    /**
     * Generates the next available party ID from the party_id_seq sequence.
     *
     * Executes a SELECT nextval(...) query to obtain the next sequence value,
     * safely casts it to an integer, and returns it wrapped in an Optional.
     * If the sequence value exceeds the integer range, a SQLException is thrown.
     *
     * @param conn an active SQL connection used to execute the query
     * @return Optional containing the next party ID, or empty if no value was returned
     * @throws SQLException if a database error occurs or the sequence value is too large
     */
    public static Optional<Integer> create(Connection conn) throws SQLException {
        String sql = "SELECT nextval('party_id_seq')";

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
