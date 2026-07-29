package starlightcarnival;
// ==========================================================
// DatabaseManager
// ==========================================================
// This class manages all communication between the Java
// application and the MySQL database. It handles connecting
// to the database, executing queries and updates, validating
// the required schema, managing transactions, and maintaining
// the currently connected user's role and permissions.
// The class follows the Singleton design pattern to ensure
// only one database connection manager exists throughout
// the application.
// ==========================================================

import java.sql.*;
import java.util.*;

// Our Database Manager to get the .SQL connected to the Front End
public class DatabaseManager {

    // Interface used for running database operations inside transactions
    public interface TransactionWork<T> {
        T run() throws SQLException;
    }

    // Singleton instance of DatabaseManager
    private static DatabaseManager instance;

    // Database connection object
    private Connection conn;

    // Stores currently connected username
    private String connectedUser = "";

    // Stores the connected user's role
    private String connectedRole = "GUEST";

    // Database configuration values
    // Uses system properties if available, otherwise defaults are used
    static final String HOST = System.getProperty("db.host", "localhost");
    static final String PORT = System.getProperty("db.port", "3306");
    static final String DBNAME = System.getProperty("db.name", "starlight_carnival");
    static final String USER = System.getProperty("db.user", "starlight_guest");
    static final String PASSWORD = System.getProperty("db.password", "guest123");

    // Private constructor for Singleton pattern
    private DatabaseManager() {}

    // Getting the instance of the Database to ensure that it has been added correctly
    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // Connect using default guest credentials
    public void connect() throws Exception {
        connect(USER, PASSWORD);
    }

    // Passing in the User and Password to get access to the database
    // When passed in, we check if the neccessary Dependencies have been added
    // such as the database driver as well if the database has been added
    // all of which throw exceptions that display on the login screen.
    public void connect(String user, String password) throws Exception {

        // Load MySQL JDBC Driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Create database connection URL
        String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DBNAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        // Establish database connection
        conn = DriverManager.getConnection(url, user, password);

        // Enable automatic commits after each query
        conn.setAutoCommit(true);

        // Store connected username
        connectedUser = user;

        // Determine and store user role
        connectedRole = roleForUser(user);

        // Ensure required tables exist
        validateSchema();
    }

    // Returns the username currently connected to the database
    public String getConnectedUser() {
        return connectedUser;
    }

    // Returns the role of the connected user
    public String getConnectedRole() {
        return connectedRole;
    }

    // Assigning roles for our database, we are matching in the
    // input case to the one in the database sql
    private String roleForUser(String user) {

        // Prevent null pointer errors and convert to lowercase
        String name = user == null ? "" : user.toLowerCase(Locale.ROOT);

        // Determine role based on username
        if (name.contains("admin") || name.equals("root")) return "ADMIN";
        if (name.contains("sales")) return "SALES";
        if (name.contains("maintenance") || name.contains("maint")) return "MAINTENANCE";

        // Default role
        return "GUEST";
    }

    // Validates whether the required tables exist in the database.
    // Uses a direct SELECT rather than information_schema, because
    // information_schema.tables only shows tables the user has privileges on —
    // which makes it unreliable for restricted roles.
    private void validateSchema() throws SQLException {

        // Pick one table each role is guaranteed to have SELECT on.
        String tableToCheck;
        switch (connectedRole) {
            case "SALES":       tableToCheck = "CUSTOMER";   break;
            case "MAINTENANCE": tableToCheck = "ATTRACTION"; break;
            default:            tableToCheck = "LOCATION";   break; // ADMIN and GUEST
        }

        try {
            // A LIMIT 0 query is near-instant and confirms the table exists and is accessible.
            query("SELECT 1 FROM " + tableToCheck + " LIMIT 0");
        } catch (SQLException e) {
            throw new SQLException("Required tables were not found. Run starlight_carnival.sql first.");
        }
    }

    // Executes SELECT queries and returns results as a List of Maps
    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {

        // Stores all rows returned from the query
        List<Map<String, Object>> rows = new ArrayList<>();

        // Create prepared statement
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // Insert parameter values into SQL query
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);

            // Execute query
            ResultSet rs = ps.executeQuery();

            // Get metadata about columns
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            // Read every row returned
            while (rs.next()) {

                // Store column-value pairs for a row
                Map<String, Object> row = new LinkedHashMap<>();

                // Loop through all columns
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }

                // Add row to list
                rows.add(row);
            }
        }

        return rows;
    }

    // Executes INSERT, UPDATE, or DELETE queries
    // Returns generated ID if available
    public long update(String sql, Object... params) throws SQLException {

        // Create prepared statement capable of returning generated keys
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Insert parameter values into SQL query
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);

            // Execute update query
            ps.executeUpdate();

            // Retrieve generated keys
            try (ResultSet keys = ps.getGeneratedKeys()) {

                // Return generated ID if available
                if (keys.next()) return keys.getLong(1);
            }
        }

        // Return -1 if no generated key exists
        return -1;
    }

    // Inserts data and manually retrieves the last generated ID
    public long insertAndReturnGeneratedId(String sql, Object... params) throws SQLException {

        // Execute insert query
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // Insert parameter values into SQL query
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);

            // Execute insert
            ps.executeUpdate();
        }

        // Retrieve the last auto-generated ID
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID() AS generated_id")) {

            // Return generated ID
            if (rs.next()) return rs.getLong("generated_id");
        }

        // Throw error if ID could not be retrieved
        throw new SQLException("Could not read generated ID for insert.");
    }

    // Runs multiple database operations inside a transaction
    public <T> T inTransaction(TransactionWork<T> work) throws SQLException {

        // Store original auto-commit setting
        boolean originalAutoCommit = conn.getAutoCommit();

        try {

            // Disable auto-commit for transaction control
            conn.setAutoCommit(false);

            // Execute transaction work
            T result = work.run();

            // Save all changes permanently
            conn.commit();

            return result;

        } catch (SQLException e) {

            // Undo all changes if SQL error occurs
            conn.rollback();
            throw e;

        } catch (RuntimeException e) {

            // Undo all changes if runtime error occurs
            conn.rollback();
            throw e;

        } finally {

            // Restore original auto-commit setting
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    // Checks if database connection is still active
    public boolean isConnected() {

        try {
            return conn != null && !conn.isClosed();
        }

        // Return false if an SQL error occurs
        catch (SQLException e) {
            return false;
        }
    }

    // Closes database connection
    public void close() {

        try {

            // Close connection if it exists
            if (conn != null) conn.close();

        } catch (SQLException ignored) {}
    }
}