package kinoko.database.postgresql;

import kinoko.database.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.TimeZone;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kinoko.database.postgresql.setup.SchemaUpdater;
import kinoko.server.ServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PostgresConnector implements DatabaseConnector {
    private static final Logger log = LogManager.getLogger(PostgresConnector.class);
    private static final String INIT_SQL_PATH = "src/main/java/kinoko/database/postgresql/setup/init.sql";

    private HikariDataSource dataSource;
    private IdAccessor idAccessor;
    private AccountAccessor accountAccessor;
    private CharacterAccessor characterAccessor;
    private FriendAccessor friendAccessor;
    private GuildAccessor guildAccessor;
    private GiftAccessor giftAccessor;
    private MemoAccessor memoAccessor;
    private ItemAccessor itemAccessor;
    private FamilyAccessor familyAccessor;
    private ShopAccessor shopAccessor;

    @Override
    public void initialize() {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            // First, check if the database exists and create it if it doesn't
            ensureDatabaseExists();

            // Now connect to the actual database
            String DATABASE_URL = String.format(
                    "jdbc:postgresql://%s:%s/%s",
                    ServerConstants.DATABASE_HOST,
                    ServerConstants.DATABASE_PORT,
                    ServerConstants.DATABASE_NAME
            );

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DATABASE_URL);
            config.setUsername(ServerConstants.DATABASE_USER);
            config.setPassword(ServerConstants.DATABASE_PASSWORD);
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(5000); // 5s
            config.setIdleTimeout(60000); // 60s
            config.setMaxLifetime(1800000); // 30min
            config.setLeakDetectionThreshold(5000L);

            dataSource = new HikariDataSource(config);

            try (Connection connection = dataSource.getConnection()) {
                SchemaUpdater.run(connection);
            }

            // Create Accessors
            idAccessor = new PostgresIdAccessor(dataSource);
            accountAccessor = new PostgresAccountAccessor(dataSource);
            characterAccessor = new PostgresCharacterAccessor(dataSource);
            friendAccessor = new PostgresFriendAccessor(dataSource);
            guildAccessor = new PostgresGuildAccessor(dataSource);
            giftAccessor = new PostgresGiftAccessor(dataSource);
            memoAccessor = new PostgresMemoAccessor(dataSource);
            itemAccessor = new PostgresItemAccessor(dataSource);
            familyAccessor = new PostgresFamilyAccessor(dataSource);
            shopAccessor = new PostgresShopAccessor(dataSource);



        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize PostgresConnector", e);
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close(); // safely closes all pooled connections
            dataSource = null;
        }
    }

    @Override public IdAccessor getIdAccessor() { return idAccessor; }
    @Override public AccountAccessor getAccountAccessor() { return accountAccessor; }
    @Override public CharacterAccessor getCharacterAccessor() { return characterAccessor; }
    @Override public FriendAccessor getFriendAccessor() { return friendAccessor; }
    @Override public GuildAccessor getGuildAccessor() { return guildAccessor; }
    @Override public GiftAccessor getGiftAccessor() { return giftAccessor; }
    @Override public MemoAccessor getMemoAccessor() { return memoAccessor; }
    @Override public ItemAccessor getItemAccessor() {return itemAccessor; }
    @Override public FamilyAccessor getFamilyAccessor() {return familyAccessor; }
    @Override public ShopAccessor getShopAccessor() {return shopAccessor; }

    /**
     * Ensures the database exists. If it doesn't, creates it and runs the init.sql script.
     */
    private void ensureDatabaseExists() throws Exception {
        // Connect to the default 'postgres' database to check/create our database
        String postgresUrl = String.format(
                "jdbc:postgresql://%s:%s/postgres",
                ServerConstants.DATABASE_HOST,
                ServerConstants.DATABASE_PORT
        );

        try (Connection adminConnection = DriverManager.getConnection(
                postgresUrl,
                ServerConstants.DATABASE_USER,
                ServerConstants.DATABASE_PASSWORD)) {

            // Check if the database exists
            boolean databaseExists = false;
            try (Statement stmt = adminConnection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT 1 FROM pg_database WHERE datname = '" + ServerConstants.DATABASE_NAME + "'")) {
                databaseExists = rs.next();
            }

            if (!databaseExists) {
                log.info("Database '{}' does not exist. Creating it now...", ServerConstants.DATABASE_NAME);

                // Create the database
                try (Statement stmt = adminConnection.createStatement()) {
                    stmt.execute("CREATE DATABASE " + ServerConstants.DATABASE_NAME);
                }
                log.info("Database '{}' created successfully.", ServerConstants.DATABASE_NAME);

                // Now connect to the new database and run init.sql
                String newDbUrl = String.format(
                        "jdbc:postgresql://%s:%s/%s",
                        ServerConstants.DATABASE_HOST,
                        ServerConstants.DATABASE_PORT,
                        ServerConstants.DATABASE_NAME
                );

                try (Connection dbConnection = DriverManager.getConnection(
                        newDbUrl,
                        ServerConstants.DATABASE_USER,
                        ServerConstants.DATABASE_PASSWORD)) {

                    // Read and execute init.sql
                    Path initPath = Path.of(INIT_SQL_PATH);
                    if (Files.exists(initPath)) {
                        log.info("Running init.sql to set up database schema...");
                        String sql = Files.readString(initPath);
                        try (Statement stmt = dbConnection.createStatement()) {
                            stmt.execute(sql);
                        }
                        log.info("Database schema initialized successfully.");
                    } else {
                        log.warn("init.sql not found at {}. Database may not be properly initialized.", INIT_SQL_PATH);
                    }
                }
            } else {
                log.info("Database '{}' already exists.", ServerConstants.DATABASE_NAME);
            }
        }
    }
}
