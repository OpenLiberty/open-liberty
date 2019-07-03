package com.ibm.ws.jdbc.fat.db2;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

public class DB2Container<SELF extends DB2Container<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String IMAGE = "ibmcom/db2";
    public static final String DEFAULT_TAG = "11.5.0.0";

    public static final Integer DB2_PORT = 50000;
    private String databaseName = "test";
    private String username = "db2inst1";
    private String password = "test";
    private boolean licenseAccepted = false;

    public DB2Container() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public DB2Container(final String dockerImageName) {
        super(dockerImageName);
        this.waitStrategy = new LogMessageWaitStrategy()
                        .withRegEx(".*Setup has completed\\..*")
                        .withStartupTimeout(Duration.of(5, MINUTES));
    }

    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(DB2_PORT));
    }

    @Override
    protected void configure() {
        addExposedPort(DB2_PORT);
        addEnv("DBNAME", databaseName);
        addEnv("DB2INSTANCE", username);
        addEnv("DB2INST1_PASSWORD", password);
        if (!licenseAccepted && !getEnvMap().containsKey("LICENSE")) {
            throw new ContainerLaunchException("Must accept the license in order to use this image." +
                                               " Call acceptLicense() or set the 'LICENSE=accept' in the env");
        }
        if (licenseAccepted)
            addEnv("LICENSE", "accept");
        if (!isPrivilegedMode())
            throw new ContainerLaunchException("The DB2 containers must be started in privileged mode in order to work properly.");
    }

    @Override
    public String getDriverClassName() {
        return "com.ibm.db2.jcc.DB2Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:db2://" + getContainerIpAddress() + ":" + getMappedPort(DB2_PORT) + "/" + databaseName;
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
        return "SELECT 1 FROM sysibm.sysdummy1";
    }

    @Override
    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    @Override
    public SELF withPassword(final String password) {
        this.password = password;
        return self();
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    public SELF acceptLicense() {
        licenseAccepted = true;
        return self();
    }
}
