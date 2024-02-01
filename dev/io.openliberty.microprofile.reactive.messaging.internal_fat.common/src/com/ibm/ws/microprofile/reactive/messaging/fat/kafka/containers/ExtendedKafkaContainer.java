/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2019 Richard North
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * This class is a modification of the <code>org.testcontainers.containers.KafkaContainer</code> class
 * to allow configuration of TLS and SASL.
 */
public class ExtendedKafkaContainer extends GenericContainer<ExtendedKafkaContainer> {

    public static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.1.1");
    public static final int KAFKA_PORT = 9093;
    public static final int ZOOKEEPER_PORT = 2181;

    private static final String KEYSTORE_PASSWORD = "kafka-teststore";

    private static final String SECRETS_PREFIX = "/etc/kafka/secrets/";
    private static final String KEYSTORE_FILENAME = "kafkakey.jks";
    private static final String KEYSTORE_FILENAME2 = "kafkakey2.jks";
    private static final String KEYSTORE_FILEPATH = SECRETS_PREFIX + KEYSTORE_FILENAME;
    private static final String KEYSTORE_FILEPATH2 = SECRETS_PREFIX + KEYSTORE_FILENAME2;
    private static final String KEYSTORE_FILENAME_COMBINED = "kafkakey_combined.jks";
    private static final String KEYSTORE_FILEPATH_COMBINED = SECRETS_PREFIX + KEYSTORE_FILENAME_COMBINED;
    private static final String KEYSTORE_PASSWORD_FILENAME = "kafkakey-pass";
    private static final String KEYSTORE_PASSWORD_FILEPATH = SECRETS_PREFIX + KEYSTORE_PASSWORD_FILENAME;

    private static final String KAFKA_JAAS_CONF_FILENAME = "/etc/kafka/kafka_server_jaas.conf";
    private static final String ZOOKEEPER_JAAS_CONF_FILENAME = "/etc/kafka/zookeeper_server_jaas.conf";
    private static final String ZOOKEEPER_PROPERTIES_FILENAME = "/etc/kafka/zookeeper.properties";

    private String listenerScheme = "PLAINTEXT";
    private boolean tls = false;
    private boolean mtls = false;
    private boolean mergeKeystores = false;
    private String kafkaJaasConfig = null;
    private String zookeeperJaasConfig = null;

    public ExtendedKafkaContainer() {
        super(KAFKA_IMAGE);

        withExposedPorts(KAFKA_PORT, ZOOKEEPER_PORT);
        withEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:2181");

        withEnv("KAFKA_BROKER_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
    }

    private static String getZookeeperProperties() {
        StringBuilder builder = new StringBuilder();
        builder.append("clientPort=");
        builder.append(ZOOKEEPER_PORT);
        builder.append("\n");
        builder.append("dataDir=/var/lib/zookeeper/data\n");
        builder.append("dataLogDir=/var/lib/zookeeper/log\n");
        String conf = builder.toString();

        return conf;
    }

    public ExtendedKafkaContainer withTls() {
        this.tls = true;
        withListenerScheme("SSL");
        return this;
    }

    /**
     * Enable MTLS
     *
     * This also enables TLS and all related TLS objects are created.
     *
     * @return
     */
    public ExtendedKafkaContainer withMTls(){
        this.mtls = true;
        withTls();
        return this;
    }

    /**
     * Set that keystore 1 and 2 are to be merged into a separate single truststore
     *
     * By Default the primary key/truststore will only contain the contents of keystore 1, not keystore 2
     * So if a server uses the contents of 2 it should be rejected by the other side for not being trusted
     *
     * the merge store is for use as a truststore only.
     *
     * @return
     */
    public ExtendedKafkaContainer mergeKeyStores(){
        this.mergeKeystores = true;
        return this;
    }

    public ExtendedKafkaContainer withListenerScheme(String listenerScheme) {
        this.listenerScheme = listenerScheme;
        return this;
    }

    public String getListenerScheme(){
        return this.listenerScheme;
    }

    public ExtendedKafkaContainer withZookeeperJaasConfig(String zookeeperJaasConfig) {
        this.zookeeperJaasConfig = zookeeperJaasConfig;
        return this;
    }

    public ExtendedKafkaContainer withKafkaJaasConfig(String kafkaJaasConfig) {
        this.kafkaJaasConfig = kafkaJaasConfig;
        return this;
    }

    /**
     * Get Primary Keystore file
     *
     * If TLS has not been enabled returns null
     *
     * The contents of this keystore is used as the basis of trust and key stores for both Liberty and Kafka
     * therefore its contents should always be trusted if used on either side of the communication
     *
     * This will return null if at least TLS is not enabled on the kafka definition
     *
     * @return
     */
    public File getKeystoreFile() {
        if(tls) {
            return new File(KEYSTORE_FILENAME);
        } else {
            return null;
        }
    }

    /**
     * Gets a secondary Keystore file if MTLS has been enabled, otherwise returns null
     *
     * This should be used for Client keystores such as RM Channels or connector
     *
     * By default, this keystore is not included in the truststores of either Liberty or Kafka. If used as the keystore
     * for either server or client sides should result in a SSL Handshake error
     *
     * If you want to include the contents of this keystore in the truststore used for kafka, then `mergeKeyStores()` must be used
     * to created a merged server truststore.
     *
     * @return
     */
    public File getKeystoreFile2() {
        if(mtls) {
            return new File(KEYSTORE_FILENAME2);
        } else {
            return null;
        }
    }

    public String getKeystorePassword() {
        return KEYSTORE_PASSWORD;
    }

    public String getBootstrapServers() {
        return String.format("%s://%s:%s", listenerScheme, getHost(), getMappedPort(KAFKA_PORT));
    }

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        super.configure();

        withEnv("KAFKA_LISTENERS", listenerScheme + "://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT," + listenerScheme + ":" + listenerScheme);
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
        withEnv("KAFKA_ADVERTISED_LISTENERS", listenerScheme + "://localhost:" + KAFKA_PORT + ",BROKER://localhost:9092"); // Note: this is updated later once the container has started

        List<String> subCommands = new ArrayList<>();
        if (tls) {
            subCommands.add(getCertGenerationCommand(KEYSTORE_FILEPATH, KEYSTORE_PASSWORD, getHost()));
            subCommands.add("echo " + KEYSTORE_PASSWORD + " > " + KEYSTORE_PASSWORD_FILEPATH);
            withEnv("KAFKA_SSL_KEYSTORE_FILENAME", KEYSTORE_FILENAME);
            withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
            withEnv("KAFKA_SSL_KEY_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
            withEnv("KAFKA_SSL_TRUSTSTORE_FILENAME", KEYSTORE_FILENAME);
            withEnv("KAFKA_SSL_TRUSTSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME);
            withEnv("KAFKA_SSL_ENABLED_PROTOCOLS", "TLSv1.2");
            // MTLS only works if TLS is enabled.
            if (mtls) {
                // Enable Client Authentication - by default this is `requested`, which allows non-cert based requests to succeed
                withEnv("KAFKA_SSL_CLIENT_AUTH", "required");
                // Generate Second Keystore
                // Second key store contents is not included in the Kafka server truststore so is by default rejected
                subCommands.add(getCertGenerationCommand(KEYSTORE_FILEPATH2, KEYSTORE_PASSWORD, getHost()));
                // Combine the keys into a single store that can be used by the kafka server
                if (mergeKeystores) {
                    subCommands.add(mergeKeyStoresCommand(KEYSTORE_FILEPATH, KEYSTORE_FILEPATH_COMBINED, "kafka-testcontainers", KEYSTORE_PASSWORD));
                    subCommands.add(mergeKeyStoresCommand(KEYSTORE_FILEPATH2, KEYSTORE_FILEPATH_COMBINED, "kafka-testcontainers2", KEYSTORE_PASSWORD));
                    // Change Kafka Server TrustStore location to combined store
                    withEnv("KAFKA_SSL_TRUSTSTORE_FILENAME", KEYSTORE_FILENAME_COMBINED);
                }
            }
        }

        subCommands.add("printf '" + getZookeeperProperties() + "' > " + ZOOKEEPER_PROPERTIES_FILENAME);

        if (kafkaJaasConfig != null) {
            subCommands.add("printf '" + kafkaJaasConfig + "' > " + KAFKA_JAAS_CONF_FILENAME);
            withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=" + KAFKA_JAAS_CONF_FILENAME);
            withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN");
        }

        if (zookeeperJaasConfig != null) {
            subCommands.add("printf '" + zookeeperJaasConfig + "' > " + ZOOKEEPER_JAAS_CONF_FILENAME);
            subCommands.add("(EXTRA_ARGS=-Djava.security.auth.login.config=" + ZOOKEEPER_JAAS_CONF_FILENAME + " zookeeper-server-start " + ZOOKEEPER_PROPERTIES_FILENAME + " &)");
        } else {
            subCommands.add("(zookeeper-server-start " + ZOOKEEPER_PROPERTIES_FILENAME + " &)");
        }

        subCommands.add("/etc/confluent/docker/run");

        withCommand("sh", "-c", String.join(" && ", subCommands));
    }

    /** {@inheritDoc} */
    @Override
    protected void doStart() {
        super.doStart();
        if (tls) {
            copyFileFromContainer(KEYSTORE_FILEPATH, KEYSTORE_FILENAME);
        }
        if (mtls) {
            copyFileFromContainer(KEYSTORE_FILEPATH2, KEYSTORE_FILENAME2);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        String brokerAdvertisedListener = brokerAdvertisedListener(containerInfo);
        ExecResult result;
        try {
            result = execInContainer("kafka-configs",
                                     "--alter",
                                     "--bootstrap-server", brokerAdvertisedListener,
                                     "--entity-type", "brokers",
                                     "--entity-name", getEnvMap().get("KAFKA_BROKER_ID"),
                                     "--add-config",
                                     "advertised.listeners=[" + String.join(",", getBootstrapServers(), brokerAdvertisedListener) + "]");
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(result.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String brokerAdvertisedListener(InspectContainerResponse containerInfo) {
        return String.format("BROKER://%s:%s", containerInfo.getConfig().getHostName(), "9092");
    }

    private String getCertGenerationCommand(String filepath, String password, String ipAddress) {
        List<String> sans = new ArrayList<>();
        if (looksLikeIp(ipAddress)) {
            sans.add("IP:" + ipAddress);
            try {
                // Attempt a reverse name lookup on the IP address to get the hostname.
                // The Kafka client seems to sometimes to find the hostname even if we
                // only give it the IP address and in that case the certificate needs
                // the IP address and the hostname
                String hostname = InetAddress.getByName(ipAddress).getCanonicalHostName();
                if (!looksLikeIp(hostname)) {
                    sans.add("DNS:" + hostname);
                }
            } catch (Exception e) {
                // Don't care
            }
        } else {
            sans.add("DNS:" + ipAddress);
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
                                         "-ext", "SAN=" + String.join(",", sans),
                                         "-sigalg", "SHA256withRSA",
                                         "-keyalg", "RSA",
                                         "-keysize", "4096");
        return String.join(" ", cmd);
    }

    /**
     * Combine Keystore files into a single one to use as a truststore
     * @param keystoreFilePath
     * @param outputFilePath
     * @param password will be used for both the extraction and for the output store
     * @return
     */
    private String mergeKeyStoresCommand(String keystoreFilePath, String outputFilePath, String destAlias, String password){
        List <String> cmd = Arrays.asList("keytool",
                "-importkeystore",
                "-srckeystore", keystoreFilePath,
                "-destkeystore", outputFilePath,
                "-srcalias", "kafka-testcontainers",
                "-destalias", destAlias,
                "-srcstorepass", password,
                "-deststorepass", password
                );
        return String.join(" ", cmd);
    }

    private boolean looksLikeIp(String maybeIp) {
        return maybeIp.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }

}
