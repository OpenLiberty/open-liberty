package componenttest.topology.database.container;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.concurrent.Future;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.HttpsRequest;

@SuppressWarnings("restriction")
public class CouchDBContainer extends GenericContainer<CouchDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("kyleaure/couchdb-ssl");
    private static final String DEFAULT_TAG = "2.0";

    public static final int PORT = 5984;
    public static final int PORT_SECURE = 6984;

    private String user = "dbuser";
    private String pass = "dbpass";

    private final HashSet<String> databases = new HashSet<String>();

    public CouchDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public CouchDBContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CouchDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    public CouchDBContainer(Future<String> dockerImageName) {
        super(dockerImageName);
    }

    public CouchDBContainer withUser(String user) {
        this.user = user;
        return this;
    }

    public String getUser() {
        return user;
    }

    public CouchDBContainer withPassword(String pass) {
        this.pass = pass;
        return this;
    }

    public String getPassword() {
        return pass;
    }

    public CouchDBContainer withDatabase(String databaseName) {
        this.databases.add(databaseName);
        return this;
    }

    public CouchDBContainer withDatabases(String... databaseNames) {
        this.databases.addAll(Arrays.asList(databaseNames));
        return this;
    }

    public int getDatabasePort() {
        return getMappedPort(PORT);
    }

    public int getDatabaseSSLPort() {
        return getMappedPort(PORT_SECURE);
    }

    public String getEncodedAuthData() {
        return "Basic " + Base64.getEncoder().encodeToString((user + ':' + pass).getBytes());
    }

    @Override
    protected void configure() {
        withEnv("COUCHDB_USER", user);
        withEnv("COUCHDB_PASSWORD", pass);
        withExposedPorts(PORT, PORT_SECURE);
        waitingFor(Wait.forHttp("/").forPort(PORT));
        databases.addAll(Arrays.asList("_users", "_replicator", "_global_changes"));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        for (String database : databases) {
            try {
                createDb(database);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create database [" + database + "]", e);
            }
        }
    }

    public String getURL(boolean secure) {
        return secure ? //
                        "https://" + getHost() + ':' + getDatabaseSSLPort() : //
                        "http://" + getHost() + ':' + getDatabasePort();
    }

    private String createDb(String dbName) throws Exception {
        Log.info(getClass(), "createDb", "Creating DB " + dbName);
        String auth = getEncodedAuthData();

        String response = new HttpsRequest(getURL(false) + "/" + dbName)
                        .method("PUT")
                        .allowInsecure()
                        .requestProp("Authorization", auth)
                        .requestProp("Accept", "application/json")
                        .requestProp("Content-type", "application/json")
                        .requestProp("User-Agent", "java-couchdb/unknown")
                        .expectCode(201) // HTTP 201/202 mean create successfully
                        .expectCode(202)
                        .run(String.class);
        Log.info(getClass(), "createDb", "Create DB response: " + response);
        return response;
    }
}
