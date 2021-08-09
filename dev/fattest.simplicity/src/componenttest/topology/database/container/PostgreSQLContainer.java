package componenttest.topology.database.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * This class is a replacement for the regular <code>org.testcontainers.containers.PostgreSQLContainer</code> class.
 * This custom class is needed for 2 reasons:
 * 1. To add a ctor that accepts a <code>Future<String></code> parameter so we can mount SSL certificates in the image
 * 2. To fix the ordering of configure() so that we can set config options such as max_connections=200
 */
public class PostgreSQLContainer extends JdbcDatabaseContainer<PostgreSQLContainer> {
	
    public static final String NAME = "postgresql";
    public static final String IMAGE = "postgres";
    public static final String DEFAULT_TAG = "9.6.8";

    public static final Integer POSTGRESQL_PORT = 5432;
    private String databaseName = "test";
    private String username = "test";
    private String password = "test";
    private final Map<String, String> options = new HashMap<>();

    public PostgreSQLContainer(String img) {
        super(img);
    }

    public PostgreSQLContainer(final Future<String> image) {
        super(image);
    }

    /**
     * Add additional configuration options that should be used for this container.
     *
     * @param key   The PostgreSQL configuration option key. For example: "max_connections"
     * @param value The PostgreSQL configuration option value. For example: "200"
     * @return this
     */
    public PostgreSQLContainer withConfigOption(String key, String value) {
        if (key == null) {
            throw new java.lang.NullPointerException("key marked @NonNull but is null");
        }
        if (value == null) {
            throw new java.lang.NullPointerException("value marked @NonNull but is null");
        }
        options.put(key, value);
        return self();
    }

    @Override
    protected void configure() {
        addExposedPort(POSTGRESQL_PORT);
        addEnv("POSTGRES_DB", databaseName);
        addEnv("POSTGRES_USER", username);
        addEnv("POSTGRES_PASSWORD", password);
        if (!options.containsKey("fsync"))
            withConfigOption("fsync", "off");
        if (!options.containsKey("max_prepared_transactions"))
        	withConfigOption("max_prepared_transactions", "2");
        List<String> command = new ArrayList<>();
        for (Entry<String, String> e : options.entrySet()) {
            command.add("-c");
            command.add(e.getKey() + '=' + e.getValue());
        }
        setCommand(command.toArray(new String[command.size()]));
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(POSTGRESQL_PORT));
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:postgresql://" + getContainerIpAddress() + ":" + getMappedPort(POSTGRESQL_PORT) + "/" + databaseName;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public PostgreSQLContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public PostgreSQLContainer withUsername(final String username) {
        this.username = username;
        return self();
    }

    @Override
    public PostgreSQLContainer withPassword(final String password) {
        this.password = password;
        return self();
    }
    
    /**
     * Sets the necessary config options for enabling SSL for the container. Assumes there is
     * a server.crt and server.key file under /var/lib/postgresql/ in the container.
     * An easy way to use this is to combine it with the <code>aguibert/postgresql-ssl:1.0</code>
     * or similar base image
     */
    public PostgreSQLContainer withSSL() {
        withConfigOption("ssl", "on");
        withConfigOption("ssl_cert_file", "/var/lib/postgresql/server.crt");
        withConfigOption("ssl_key_file", "/var/lib/postgresql/server.key");
        return this;
    }

    @Override
    protected void waitUntilContainerStarted() {
        // by Testcontainers waits for being able to establish a JDBC connection
        // use the default wait strategy instead (necessary for the SSL path)
        getWaitStrategy().waitUntilReady(this);
    }

}
