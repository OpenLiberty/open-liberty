/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.rules.ExternalResource;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.RepeatTestFilter;

/**
 * A JUnit ExternalResource for starting and stopping a H2 database.
 *
 * <p>
 * This class can be used in three different ways:
 * </p>
 *
 * 1. Manually
 *
 * <pre>
 * H2Database db = H2Database.create("user", "pass");
 *
 * &#64;BeforeClass
 * public static void setup() {
 *     db.before();
 * }
 *
 * &#64;AfterClass
 * public static void tearDown() {
 *     db.after();
 * }
 * </pre>
 *
 * 2. As a rule
 *
 * <pre>
 * &#64;ClassRule
 * H2Database db = H2Database.create("user", "pass");
 * </pre>
 *
 * 3. Passed to container
 *
 * <pre>
 * H2Database db = H2Database.create("user", "pass");
 *
 * &#64;ClassRule
 * H2Container dbContainer = new H2Container().withDatabase(db);
 * </pre>
 *
 */
public class H2Database extends ExternalResource {

    //Logging Constants
    private static final Class<H2Database> c = H2Database.class;

    //In file embedded parent path
    private static final Path IN_FILE_PATH = Paths.get("results", "h2").toAbsolutePath().normalize();

    /**
     * Enum that represents the two different embedded modes H2 can run in
     * - in-memory
     * - in-file
     */
    public enum MODE {
        IN_MEMORY("mem:"),
        IN_FILE("file:");

        public final String prefix;

        MODE(final String prefix) {
            this.prefix = prefix;
        }
    };

    /**
     * Enum that represents the different trace levels supported by H2
     * 0 - off
     * 1 - error
     * 2 - info
     * 3 - debug
     */
    public enum TRACE_LEVEL {
        OFF(0),
        ERROR(1),
        INFO(2),
        DEBUG(3);

        public final int value;

        TRACE_LEVEL(final int value) {
            this.value = value;
        }

        public String getValue() {
            return Integer.toString(value);
        }
    }

    // Admin user
    private final String adminUser;
    private final String adminPassword;

    // Additional user(s)
    private final Map<String, String> additionalUsers = new HashMap<>();

    // Additional configuration(s)
    private final Map<String, String> additionalConfig = new HashMap<>();

    // Mode - In memory by default
    private MODE mode = MODE.IN_MEMORY;

    // Name of database - Random UUID by default
    private String databaseName = UUID.randomUUID().toString();

    // Cache the driver
    private final AtomicReference<Driver> driver = new AtomicReference<>();

    /**
     * Builder class do not allow construction outside this class
     *
     * @param user     the admin user name
     * @param password the admin password
     */
    private H2Database(String user, String password) {
        this.adminUser = user;
        this.adminPassword = password;
    }

    /**
     * Creates a new H2 database with the given admin user and password.
     *
     * @param  user     the admin user name
     * @param  password the admin password
     * @return          the H2 database
     */
    public static H2Database create(String user, String password) {
        // Verify username and password
        Objects.requireNonNull(user);
        Objects.requireNonNull(password);

        if (password.startsWith("{xor}") || //
            password.startsWith("{aes}") || //
            password.startsWith("{aes-128}") || //
            password.startsWith("{hash}")) {
            throw new IllegalStateException("Admin password cannot be encoded because " +
                                            "the initial connection may need to be created " +
                                            "via test client to create additional users.");
        }

        return new H2Database(user, password);
    }

    /**
     * Add an additional user needed for testing
     *
     * NOTE: this will switch modes from in-memory to in-file
     *
     * @param  user     the username
     * @param  password the password
     * @return          this
     */
    public H2Database withUser(String user, String password) {
        additionalUsers.put(user, password);
        return withFileMode();
    }

    /**
     * Typically, file mode is reserved for when multiple users are required,
     * but you can force file mode by calling this method.
     *
     * Useful, when you need to execute a DDL or create tables prior to running a test.
     *
     * @return this
     */
    public H2Database withFileMode() {
        mode = MODE.IN_FILE;
        return this;
    }

    /**
     * Configure a database name to use for either in-memory or in-file modes
     *
     * NOTE: the database name will be augmented with the repeat action name
     * so buckets that repeat will not use the same database between runs.
     *
     * @param  databaseName the database name
     * @return              this
     */
    public H2Database withDatabaseName(String databaseName) {
        // Verify valid name
        if (databaseName.length() < 3) {
            throw new IllegalStateException("Database name must be at least 3 characters long");
        }
        if (databaseName.contains(";")) {
            throw new IllegalStateException("Database name cannot contain a semicolon (;)");
        }

        this.databaseName = databaseName;
        return this;
    }

    /**
     * Configure the system out trace level.
     * By default system out trace is disabled.
     *
     * @param  level the trace level
     * @return       this
     */
    public H2Database withSysOutTrace(TRACE_LEVEL level) {
        return withConfig("TRACE_LEVEL_SYSTEM_OUT", level.getValue());
    }

    /**
     * Configure the file trace level.
     * By default file trace is disabled.
     *
     * @param  level the trace level
     * @return       this
     */
    public H2Database withFileTrace(TRACE_LEVEL level) {
        return withConfig("TRACE_LEVEL_FILE", level.getValue());
    }

    /**
     * Configure additional config options to append to the URL
     * See: http://www.h2database.com/html/features.html#database_url
     *
     * @param  key   the config key
     * @param  value the config value
     * @return
     */
    public H2Database withConfig(String key, String value) {
        if ("AUTO_SERVER".equals(key)) {
            Log.info(c, "withConfig", "AUTO_SERVER config ignored, " +
                                      "set to true automatically when running in-file mode.");
            return this;
        }

        if ("DB_CLOSE_DELAY".equals(key)) {
            Log.info(c, "withConfig", "DB_CLOSE_DELAY config ignored, " +
                                      "set to -1 automatically when running in-memory mode.");
            return this;
        }

        additionalConfig.put(key, value);
        return this;
    }

    /**
     * Lifecycle method: called by @Rule or @ClassRule as part of JUnit lifecycle
     */
    @Override
    public void before() {
        Log.info(c, "before", "Start using H2 database with URL: " + getURL());

        if (mode.equals(MODE.IN_MEMORY)) {
            return;
        }

        Log.info(c, "before", "Adding the following users to the database " + additionalUsers);

        try (Connection con = createConnection("");
                        Statement stmt = con.createStatement()) {
            for (Map.Entry<String, String> e : additionalUsers.entrySet()) {
                stmt.executeUpdate("create user if not exists " + e.getKey() +
                                   " password '" + e.getValue() + "' admin;");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create new users", e);
        }

        if (additionalConfig.containsKey("TRACE_LEVEL_SYSTEM_OUT")) {
            try (Connection con = createConnection("");
                            Statement stmt = con.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.USERS;")) {
                    while (rs.next()) {
                        String userName = rs.getString("USER_NAME");
                        boolean isAdmin = rs.getBoolean("IS_ADMIN");

                        Log.info(c, "before", "Found user in H2: " + userName + " isAdmin? " + isAdmin);
                    }
                }
            } catch (SQLException e) {
                Log.error(c, "before", e);
            }
        }
    }

    /**
     * Lifecycle method: called by @Rule or @ClassRule as part of JUnit lifecycle
     */
    @Override
    public void after() {
        // NOTE: no need to cleanup IN_FILE database,
        // as every test SHOULD have a unique database name.
        Log.info(c, "after", "Stop using H2 database with URL: " + getURL());
    }

    /**
     * Get admin user
     */
    public String getAdminUser() {
        return adminUser;
    }

    /**
     * Get admin password
     */
    public String getAdminPassword() {
        return adminPassword;
    }

    /**
     * Get database name augmented with repeat action string
     */
    public String getDatabaseName() {
        return databaseName + RepeatTestFilter.getRepeatActionsAsString();
    }

    /**
     * Get URL based on mode (in-memory or in-file) with additional configurations
     */
    public String getURL() {
        String configString = additionalConfig.entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce("", (partial, entry) -> partial.isEmpty() ? ";" + entry : partial + ";" + entry);

        switch (mode) {
            case IN_FILE:
                return "jdbc:h2:" + mode.prefix + IN_FILE_PATH.resolve(getDatabaseName()) + configString + ";AUTO_SERVER=TRUE";
            case IN_MEMORY:
                return "jdbc:h2:" + mode.prefix + getDatabaseName() + configString + ";DB_CLOSE_DELAY=-1";
            default:
                throw new IllegalStateException("Could not generate URL without knowing mode");
        }
    }

    /**
     * Get driver class name
     */
    public String getDriverClassName() {
        return "org.h2.Driver";
    }

    /**
     * Get driver instance
     */
    public Driver getDriverInstance() {
        return driver.updateAndGet(existing -> {
            if (Objects.nonNull(existing)) {
                return existing;
            }
            try {
                Class<?> c = Class.forName(this.getDriverClassName());
                Driver d = (Driver) c.getDeclaredConstructor().newInstance();
                Log.info(c, "getDriverInstance", "Driver loaded from: " + c.getProtectionDomain().getCodeSource().getLocation());
                return d;
            } catch (Exception e) {
                throw new RuntimeException("Could not get Driver", e);
            }
        });
    }

    /**
     * Get test query string
     */
    public String getTestQueryString() {
        return "SELECT 1";
    }

    public Connection createConnection(String queryString) throws SQLException {
        return createConnection(queryString, null);
    }

    public Connection createConnection(String queryString, Properties info) throws SQLException {
        String q = queryString.isEmpty() ? queryString : //
                        queryString.startsWith(";") ? queryString : ";" + queryString;

        Properties i = info == null ? new Properties() : info;
        i.put("user", getAdminUser());
        i.put("password", getAdminPassword());

        Log.info(c, "createConnection", "Creating a connection using URL=" + getURL() + " queryString=" + q + " and properties=" + i);

        return getDriverInstance().connect(getURL() + q, i);
    }
}
