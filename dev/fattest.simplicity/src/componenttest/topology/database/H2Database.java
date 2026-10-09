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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
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
 * <p>
 * Supported security mechanisms:
 * </p>
 * <ul>
 * <li>Username / password authentication via {@link #create(String, String)} and {@link #withUser(String, String)}</li>
 * <li>File encryption via {@link #withCipher(CIPHER, String)} (URL parameter <code>CIPHER</code>, implies in-file mode)</li>
 * <li>Pre-hashed passwords via {@link #withPasswordHash()} (URL parameter <code>PASSWORD_HASH=TRUE</code>)</li>
 * <li>SQL literal restrictions via {@link #withAllowLiterals(String)} (SQL statement <code>SET ALLOW_LITERALS</code>)</li>
 * <li>File locking modes via {@link #withFileLock(String)} (URL parameter <code>FILE_LOCK</code>, requires in-file mode)</li>
 * </ul>
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

    /**
     * Enum that represents the file encryption algorithms supported by H2
     * - AES - Advanced Encryption Standard
     * - XTEA - Extended Tiny Encryption Algorithm
     * - FOG - pseudo-encryption, only obfuscates data (not secure)
     */
    public enum CIPHER {
        AES,
        XTEA,
        FOG
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

    // File encryption - not configured by default
    private CIPHER cipher = null;
    private String filePassword = null;

    // Pre-hashed password - disabled by default
    private boolean passwordHash = false;

    // SQL literal restriction mode - not configured by default
    private String allowLiterals = null;

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
     * Enable file encryption using the given cipher.
     *
     * NOTE: encryption requires in-file mode, this will switch modes from in-memory to in-file.
     *
     * Produces the URL parameter <code>CIPHER=&lt;algorithm&gt;</code>.
     * Connections created via {@link #createConnection(String, Properties)} will pass the
     * password as <code>"&lt;filePassword&gt; &lt;userPassword&gt;"</code>, while
     * {@link #getAdminPassword()} continues to return the plain user password.
     *
     * @param  cipher       the encryption algorithm
     * @param  filePassword the file encryption password
     * @return              this
     */
    public H2Database withCipher(CIPHER cipher, String filePassword) {
        Objects.requireNonNull(cipher);
        Objects.requireNonNull(filePassword);

        if (filePassword.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("filePassword must not contain whitespace (H2 CIPHER protocol splits on space)");
        }

        if (mode.equals(MODE.IN_MEMORY)) {
            Log.info(c, "withCipher", "WARNING: CIPHER requires in-file mode, switching from in-memory to in-file mode.");
            withFileMode();
        }     

        this.cipher = cipher;
        this.filePassword = filePassword;
        additionalConfig.put("CIPHER", cipher.name());
        return this;
    }

    /**
     * Enable pre-hashed passwords.
     *
     * Produces the URL parameter <code>PASSWORD_HASH=TRUE</code>. Works in either mode.
     * Connections created via {@link #createConnection(String, Properties)} will pass the
     * lowercase hex SHA-256 of the UTF-16LE bytes of
     * <code>"@" + adminUser.toUpperCase() + adminPassword</code> instead of the plain password.
     *
     * When combined with {@link #withCipher(CIPHER, String)} only the user password portion
     * is hashed, the file password portion is passed as plain text.
     *
     * @return this
     */
    public H2Database withPasswordHash() {
        this.passwordHash = true;
        additionalConfig.put("PASSWORD_HASH", "TRUE");
        return this;
    }

    /**
     * Restrict the use of literals in SQL statements.
     *
     * Valid modes (case-insensitive): <code>ALL</code>, <code>NUMBERS</code>, <code>NONE</code>.
     *
     * Does not produce a URL parameter. Instead, the SQL statement
     * <code>SET ALLOW_LITERALS &lt;MODE&gt;</code> is executed by an admin connection in
     * {@link #before()} (after creating any additional users). Requires in-file mode for
     * the statement to be executed, since {@link #before()} does nothing for in-memory databases.
     *
     * @param  mode the literal restriction mode
     * @return      this
     */
    public H2Database withAllowLiterals(String mode) {
        String m = mode == null ? null : mode.toUpperCase(Locale.ROOT);
        if (!"ALL".equals(m) && !"NUMBERS".equals(m) && !"NONE".equals(m)) {
            throw new IllegalArgumentException("Invalid ALLOW_LITERALS mode '" + mode + "', must be one of: ALL, NUMBERS, NONE");
        }
        this.allowLiterals = m;
        if (this.mode.equals(MODE.IN_MEMORY)) {
            Log.info(c, "withAllowLiterals", "ALLOW_LITERALS requires in-file mode, switching from in-memory to in-file mode.");
            withFileMode();
        }
        return this;
    }

    /**
     * Configure the file locking method.
     *
     * Valid methods (case-insensitive): <code>FILE</code>, <code>SOCKET</code>, <code>NO</code>, <code>FS</code>.
     *
     * NOTE: requires in-file mode, call {@link #withFileMode()} (or another method that implies it) first.
     *
     * Produces the URL parameter <code>FILE_LOCK=&lt;method&gt;</code>.
     *
     * @param  method the file locking method
     * @return        this
     */
    public H2Database withFileLock(String method) {
        String m = method == null ? null : method.toUpperCase(Locale.ROOT);
        if (!"FILE".equals(m) && !"SOCKET".equals(m) && !"NO".equals(m) && !"FS".equals(m)) {
            throw new IllegalArgumentException("Invalid FILE_LOCK method '" + method + "', must be one of: FILE, SOCKET, NO, FS");
        }
        if (mode.equals(MODE.IN_MEMORY)) {
            throw new IllegalStateException("FILE_LOCK cannot be used with an in-memory database, call withFileMode() first");
        }
        additionalConfig.put("FILE_LOCK", m);
        return this;
    }

    /**
     * Configure additional config options to append to the URL
     * See: http://www.h2database.com/html/features.html#database_url
     *
     * NOTE: the keys CIPHER, PASSWORD_HASH, ALLOW_LITERALS, and FILE_LOCK are not allowed,
     * use the dedicated methods instead.
     *
     * @param  key   the config key
     * @param  value the config value
     * @return
     */
    public H2Database withConfig(String key, String value) {
        String normalizedKey = key == null ? null : key.toUpperCase(Locale.ROOT);

        if ("CIPHER".equals(normalizedKey)) {
            throw new IllegalArgumentException("CIPHER cannot be set via withConfig, use withCipher(CIPHER, String) instead");
        }

        if ("PASSWORD_HASH".equals(normalizedKey)) {
            throw new IllegalArgumentException("PASSWORD_HASH cannot be set via withConfig, use withPasswordHash() instead");
        }

        if ("ALLOW_LITERALS".equals(normalizedKey)) {
            throw new IllegalArgumentException("ALLOW_LITERALS cannot be set via withConfig, use withAllowLiterals(String) instead");
        }

        if ("FILE_LOCK".equals(normalizedKey)) {
            throw new IllegalArgumentException("FILE_LOCK cannot be set via withConfig, use withFileLock(String) instead");
        }

        if ("AUTO_SERVER".equals(normalizedKey)) {
            Log.info(c, "withConfig", "AUTO_SERVER config ignored, " +
                                      "set to true automatically when running in-file mode.");
            return this;
        }

        if ("DB_CLOSE_DELAY".equals(normalizedKey)) {
            Log.info(c, "withConfig", "DB_CLOSE_DELAY config ignored, " +
                                      "set to -1 automatically when running in-memory mode.");
            return this;
        }

        additionalConfig.put(normalizedKey, value);
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

        if (allowLiterals != null) {
            try (Connection con = createConnection("");
                            Statement stmt = con.createStatement()) {
                stmt.execute("SET ALLOW_LITERALS " + allowLiterals);
            } catch (SQLException e) {
                throw new RuntimeException("Could not set ALLOW_LITERALS " + allowLiterals, e);
            }
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
        i.put("password", getConnectionPassword());

        Properties logged = new Properties();
        logged.putAll(i);
        logged.put("password", "****");
        Log.info(c, "createConnection", "Creating a connection using URL=" + getURL() + " queryString=" + q + " and properties=" + logged);

        return getDriverInstance().connect(getURL() + q, i);
    }

    /**
     * Get the password to pass to the H2 driver, taking into account
     * file encryption and pre-hashed password configuration.
     */
    String getConnectionPassword() {
        String userPassword = passwordHash ? hashPassword(adminUser, adminPassword) : adminPassword;
        return cipher != null ? filePassword + " " + userPassword : userPassword;
    }

    /**
     * Compute the lowercase hex SHA-256 of the UTF-16LE bytes of ("@" + user.toUpperCase() + password).
     *
     * Known vector: user="secuser", password="secpwd" -> input "@SECUSERsecpwd" ->
     * c8fb70828d3e11d0989b9e1e80599ad35f68c7772a6501ce22298d43cc0b1293
     */
    static String hashPassword(String user, String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                            .digest(("@" + user.toUpperCase(Locale.ROOT) + password).getBytes(StandardCharsets.UTF_16LE));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
