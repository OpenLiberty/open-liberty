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
 * This class is a modification of the <code>org.testcontainers.containers.KafkaContainer</code> class to add TLS support
 */
public class KafkaTlsContainer extends GenericContainer<KafkaTlsContainer> {

    private final Network network;

    private static final String KEYSTORE_PASSWORD = "kafka-teststore";

    private static final String SECRETS_PREFIX = "/etc/kafka/secrets/";
    private static final String KEYSTORE_FILENAME = "kafkakey.jks";
    private static final String KEYSTORE_FILEPATH = SECRETS_PREFIX + KEYSTORE_FILENAME;
    private static final String KEYSTORE_PASSWORD_FILENAME = "kafkakey-pass";
    private static final String KEYSTORE_PASSWORD_FILEPATH = SECRETS_PREFIX + KEYSTORE_PASSWORD_FILENAME;

    public static final int KAFKA_PORT = 9093;

    public static final int ZOOKEEPER_PORT = 2181;

    protected SocatContainer proxy;

    public KafkaTlsContainer() {
        this("4.0.0");
    }

    /**
     * @param confluentPlatformVersion
     */
    public KafkaTlsContainer(String confluentPlatformVersion) {
        super(TestcontainersConfiguration.getInstance().getKafkaImage() + ":" + confluentPlatformVersion);

        network = Network.newNetwork();

        withNetwork(network);
        withNetworkAliases("kafka-" + Base58.randomString(6));
        withExposedPorts(KAFKA_PORT, ZOOKEEPER_PORT);
        withEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:2181");

        withEnv("KAFKA_LISTENERS", "SSL://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

        withEnv("KAFKA_BROKER_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

        // Not standard properties, the confluent docker container has its own ideas about how SSL config should be provided
        withEnv("KAFKA_SSL_KEYSTORE_FILENAME", KEYSTORE_FILENAME);
        withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
        withEnv("KAFKA_SSL_KEY_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
        withEnv("KAFKA_SSL_TRUSTSTORE_FILENAME", KEYSTORE_FILENAME);
        withEnv("KAFKA_SSL_TRUSTSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
        withEnv("KAFKA_SSL_ENABLED_PROTOCOLS", "TLSv1.2");
    }

    public String getBootstrapServers() {
        return String.format("SSL://%s:%s", proxy.getContainerIpAddress(), proxy.getFirstMappedPort());
    }

    public File getKeystoreFile() {
        return new File(KEYSTORE_FILENAME);
    }

    public String getKeystorePassword() {
        return KEYSTORE_PASSWORD;
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
                    getCertGenerationCommand(KEYSTORE_FILEPATH, KEYSTORE_PASSWORD, proxy.getContainerIpAddress()) + // Create Cert
                          "&& echo " + KEYSTORE_PASSWORD + " > " + KEYSTORE_PASSWORD_FILEPATH + // Create password file
                          " && printf 'clientPort=2181\ndataDir=/var/lib/zookeeper/data\ndataLogDir=/var/lib/zookeeper/log' > /zookeeper.properties" + // Create zookeeper config
                          " && (zookeeper-server-start /zookeeper.properties &)" + // Run zookeeper in the background
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
