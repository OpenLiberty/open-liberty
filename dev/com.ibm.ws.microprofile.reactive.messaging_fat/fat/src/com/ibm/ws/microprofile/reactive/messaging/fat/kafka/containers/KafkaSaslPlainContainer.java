package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * This class is a modification of the <code>org.testcontainers.containers.KafkaContainer</code> class to add SASL+TLS support
 */
public class KafkaSaslPlainContainer extends GenericContainer<KafkaSaslPlainContainer> {

    private final Network network;

    private static final String KEYSTORE_PASSWORD = "kafka-teststore";
    private static final String NETWORK_ALIAS = generateSecret("kafka");
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_SECRET = generateSecret(ADMIN_USER);
    private static final String TEST_USER = "test";
    private static final String TEST_SECRET = generateSecret(TEST_USER);
    private static final String SPECIAL_USER = "specialchars";
    private static final String SPECIAL_SECRET = generateSecret("{test}^&*+=-$(");

    private static final String SECRETS_PREFIX = "/etc/kafka/secrets/";
    private static final String KEYSTORE_FILENAME = "kafkakey.jks";
    private static final String KEYSTORE_FILEPATH = SECRETS_PREFIX + KEYSTORE_FILENAME;
    private static final String KEYSTORE_PASSWORD_FILENAME = "kafkakey-pass";
    private static final String KEYSTORE_PASSWORD_FILEPATH = SECRETS_PREFIX + KEYSTORE_PASSWORD_FILENAME;

    private static final String KAFKA_JAAS_CONF_FILENAME = "/etc/kafka/kafka_server_jaas.conf";
    private static final String ZOOKEEPER_JAAS_CONF_FILENAME = "/etc/kafka/zookeeper_server_jaas.conf";
    private static final String ZOOKEEPER_PROPERTIES_FILENAME = "/etc/kafka/zookeeper.properties";

    public static final int KAFKA_PORT = 9093;
    public static final int ZOOKEEPER_PORT = 2181;

    protected SocatContainer proxy;

    public KafkaSaslPlainContainer() {
        this("4.0.0");
    }

    /**
     * @param confluentPlatformVersion
     */
    public KafkaSaslPlainContainer(String confluentPlatformVersion) {
        super(TestcontainersConfiguration.getInstance().getKafkaImage() + ":" + confluentPlatformVersion);

        network = Network.newNetwork();

        withNetwork(network);
        withNetwork(Network.newNetwork());
        withNetworkAliases(NETWORK_ALIAS);
        withExposedPorts(KAFKA_PORT, ZOOKEEPER_PORT);
        withEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:2181");

        withEnv("KAFKA_LISTENERS", "SASL_SSL://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SASL_SSL:SASL_SSL");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

        withEnv("KAFKA_BROKER_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

        withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=" + KAFKA_JAAS_CONF_FILENAME);

        // Not standard properties, the confluent docker container has its own ideas about how SSL config should be provided
        withEnv("KAFKA_SSL_KEYSTORE_FILENAME", KEYSTORE_FILENAME);
        withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
        withEnv("KAFKA_SSL_KEY_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
        withEnv("KAFKA_SSL_TRUSTSTORE_FILENAME", KEYSTORE_FILENAME);
        withEnv("KAFKA_SSL_TRUSTSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
        withEnv("KAFKA_SSL_ENABLED_PROTOCOLS", "TLSv1.2");

        withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN");

    }

    public static String generateSecret(String prefix) {
        return prefix + "-" + Base58.randomString(6);
    }

    public String getBootstrapServers() {
        return String.format("SASL_SSL://%s:%s", proxy.getContainerIpAddress(), proxy.getFirstMappedPort());
    }

    public File getKeystoreFile() {
        return new File(KEYSTORE_FILENAME);
    }

    public String getKeystorePassword() {
        return KEYSTORE_PASSWORD;
    }

    public String getAdminUser() {
        return ADMIN_USER;
    }

    public String getAdminSecret() {
        return ADMIN_SECRET;
    }

    public String getTestUser() {
        return TEST_USER;
    }

    public String getTestSecret() {
        return TEST_SECRET;
    }

    public String getSpecialCharsUser() {
        return SPECIAL_USER;
    }

    public String getSpecialCharsSecret() {
        return SPECIAL_SECRET;
    }

    @SuppressWarnings("resource")
    @Override
    protected void doStart() {
        /*
         * Largely copied from KafkaContainer with added generation of certificate
         */
        String networkAlias = getNetworkAliases().get(0);
        proxy = new SocatContainer()
                        .withNetwork(getNetwork())
                        .withTarget(KAFKA_PORT, networkAlias)
                        .withTarget(ZOOKEEPER_PORT, networkAlias);

        proxy.start();
        withEnv("KAFKA_ADVERTISED_LISTENERS", "BROKER://" + networkAlias + ":9092" + "," + getBootstrapServers());

        withCommand(
                    "sh",
                    "-c",
                    // Use command to create the file to avoid file mounting (useful when you run your tests against a remote Docker daemon)
                    getCertGenerationCommand(KEYSTORE_FILEPATH, getKeystorePassword(), proxy.getContainerIpAddress()) + // Create Cert
                          "&& echo " + KEYSTORE_PASSWORD + " > " + KEYSTORE_PASSWORD_FILEPATH + // Create password file
                          " && printf '" + getKafkaJaasConf() + "' > " + KAFKA_JAAS_CONF_FILENAME + // Create Kafka JAAS config
                          " && printf '" + getZookeeperJaasConf() + "' > " + ZOOKEEPER_JAAS_CONF_FILENAME + // Create Zookeeper JAAS config
                          " && printf '" + getZookeeperProperties() + "' > " + ZOOKEEPER_PROPERTIES_FILENAME + // Create zookeeper config
                          " && (EXTRA_ARGS=-Djava.security.auth.login.config=" + ZOOKEEPER_JAAS_CONF_FILENAME + " zookeeper-server-start " + ZOOKEEPER_PROPERTIES_FILENAME + " &)" + // Run zookeeper in the background
                          " && /etc/confluent/docker/run");

        super.doStart();

        copyFileFromContainer(KEYSTORE_FILEPATH, KEYSTORE_FILENAME);
    }

    @Override
    public void stop() {
        // Shut down both the proxy and the container
        proxy.stop();
        super.stop();
        network.close();
    }

    private String getZookeeperProperties() {
        StringBuilder builder = new StringBuilder();
        builder.append("clientPort=");
        builder.append(ZOOKEEPER_PORT);
        builder.append("\n");
        builder.append("dataDir=/var/lib/zookeeper/data\n");
        builder.append("dataLogDir=/var/lib/zookeeper/log\n");
        String conf = builder.toString();

        return conf;
    }

    private String getKafkaJaasConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("KafkaServer {\n");
        builder.append("   org.apache.kafka.common.security.plain.PlainLoginModule required\n");
        builder.append("   username=\"");
        builder.append(getAdminUser());
        builder.append("\"\n");
        builder.append("   password=\"");
        builder.append(getAdminSecret());
        builder.append("\"\n");

        builder.append("   user_");
        builder.append(getAdminUser());
        builder.append("=\"");
        builder.append(getAdminSecret());
        builder.append("\"\n");

        builder.append("   user_");
        builder.append(getTestUser());
        builder.append("=\"");
        builder.append(getTestSecret());
        builder.append("\"\n");

        builder.append("   user_");
        builder.append(getSpecialCharsUser());
        builder.append("=\"");
        builder.append(getSpecialCharsSecret());
        builder.append("\";\n");

        builder.append("};\n\n");

        builder.append("Client {\n");
        builder.append("   org.apache.kafka.common.security.plain.PlainLoginModule required\n");
        builder.append("   username=\"");
        builder.append(getAdminUser());
        builder.append("\"\n");
        builder.append("   password=\"");
        builder.append(getAdminSecret());
        builder.append("\";\n");

        builder.append("};");
        String conf = builder.toString();

        return conf;
    }

    private String getZookeeperJaasConf() {
        StringBuilder builder = new StringBuilder();
        builder.append("Server {\n");
        builder.append("   org.apache.zookeeper.server.auth.DigestLoginModule required\n");
        builder.append("   user_");
        builder.append(getAdminUser());
        builder.append("=\"");
        builder.append(getAdminSecret());
        builder.append("\";\n");

        builder.append("};\n");
        String conf = builder.toString();

        return conf;
    }

    private String getCertGenerationCommand(String filepath, String password, String ipAddress) {
        String san;
        if (ipAddress.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            san = "SAN=IP:" + ipAddress;
        } else {
            san = "SAN=DNS:" + ipAddress;
        }
        List<String> cmd = Arrays.asList("keytool",
                                         "-genkey",
                                         "-alias", "kafka-testcontainers",
                                         "-keystore", filepath,
                                         "-storetype", "jks",
                                         "-storepass", password,
                                         "-keypass", password,
                                         "-validity", "30",
                                         "-dname", "CN=kafka-testcontainers",
                                         "-ext", san,
                                         "-sigalg", "SHA256withRSA",
                                         "-keyalg", "RSA",
                                         "-keysize", "4096");
        return String.join(" ", cmd);
    }

}
