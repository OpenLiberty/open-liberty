package componenttest.topology.database.container;

import java.time.Duration;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.util.concurrent.Future;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

//TODO this class can be deleted once this issue is resolved in TestContainers: https://github.com/testcontainers/testcontainers-java/issues/1292
public class OracleContainer extends JdbcDatabaseContainer<OracleContainer> {

    public static final String NAME = "oracle";

    private static final int ORACLE_PORT = 1521;
    private static final int APEX_HTTP_PORT = 8080;

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 120;

    private String username = "system";
    private String password = "oracle";

    private static String resolveImageName() {
        String image = TestcontainersConfiguration.getInstance()
            .getProperties().getProperty("oracle.container.image");

        if (image == null) {
            throw new IllegalStateException("An image to use for Oracle containers must be configured. " +
                "To do this, please place a file on the classpath named `testcontainers.properties`, " +
                "containing `oracle.container.image=IMAGE`, where IMAGE is a suitable image name and tag.");
        }
        return image;
    }

    public OracleContainer() {
        this(resolveImageName());
    }

    public OracleContainer(String dockerImageName) {
        super(dockerImageName);
        this.waitStrategy = Wait.forLogMessage(".*DATABASE IS READY TO USE!.*", 1)
                .withStartupTimeout(Duration.of(DEFAULT_STARTUP_TIMEOUT_SECONDS, SECONDS));
        withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    }

    public OracleContainer(Future<String> dockerImageName) {
        super(dockerImageName);
        this.waitStrategy = Wait.forLogMessage(".*DATABASE IS READY TO USE!.*", 1)
                .withStartupTimeout(Duration.of(DEFAULT_STARTUP_TIMEOUT_SECONDS, SECONDS));
        withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        addExposedPorts(ORACLE_PORT, APEX_HTTP_PORT);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(ORACLE_PORT);
    }

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:oracle:thin:" + getUsername() + "/" + getPassword() + "@" + getContainerIpAddress() + ":" + getOraclePort() + ":" + getSid();
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
    public OracleContainer withUsername(String username) {
        this.username = username;
        return self();
    }

    @Override
    public OracleContainer withPassword(String password) {
        this.password = password;
        return self();
    }

    @SuppressWarnings("SameReturnValue")
    public String getSid() {
        return "xe";
    }

    public Integer getOraclePort() {
        return getMappedPort(ORACLE_PORT);
    }

    @SuppressWarnings("unused")
    public Integer getWebPort() {
        return getMappedPort(APEX_HTTP_PORT);
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1 FROM DUAL";
    }
    
    @Override
    protected void waitUntilContainerStarted() {
    	getWaitStrategy().waitUntilReady(this);
    }
}
