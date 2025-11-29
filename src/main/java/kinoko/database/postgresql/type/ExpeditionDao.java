package kinoko.database.postgresql.type;


import java.sql.*;
import java.util.Optional;

public class ExpeditionDao {
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
