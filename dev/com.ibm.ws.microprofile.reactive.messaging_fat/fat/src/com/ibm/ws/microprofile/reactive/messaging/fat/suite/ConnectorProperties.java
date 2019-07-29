/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.testcontainers.containers.KafkaContainer;

/**
 * Shrinkwrap asset for an MP reactive connector config
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * ConnectorProperties sourceConfig = new ConnectorProperties(INCOMING, "mySource")
 *              .addProperty("serverUrl", "localhost:1234")
 *              .addProperty("userId", "testUser");
 *
 * ConnectorProperties sinkConfig = new ConnectorProperties(OUTGOING, "mySink")
 *              .addProperty("serverUrl", "localhost:1234")
 *              .addProperty("userId", "testUser2");
 *
 * PropertiesAsset config = new PropertiesAsset()
 *              .include(sourceConfig)
 *              .include(sinkConfig);
 *
 * WebArchive war = ShrinkWrap.create(WebArchive.class)
 *              .addPackage(MyClass.class.getPackage())
 *              .addAsResource(config, "META-INF/microprofile-config.properties");
 * </code>
 * </pre>
 */
public class ConnectorProperties extends PropertiesAsset {

    /**
     * Creates a simple configuration for a channel sending to a topic of the same name
     * <p>
     * The message type is String
     *
     * @param kafka       the kafka container
     * @param channelName the channel and topic name
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleOutgoingChannel(KafkaContainer kafka, String channelName) {
        return simpleOutgoingChannel(kafka.getBootstrapServers(), channelName);
    }

    /**
     * Creates a simple configuration for a channel sending to a topic of the same name
     * <p>
     * The message type is String
     *
     * @param kafkaBootstrapServers the kafka bootstrap server config
     * @param channelName           the channel and topic name
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleOutgoingChannel(String kafkaBoostrapServers, String channelName) {
        return new ConnectorProperties(Direction.OUTGOING, channelName)
                        .addProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBoostrapServers)
                        .addProperty("connector", "io.openliberty.kafka")
                        .addProperty("topic", channelName)
                        .addProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
                        .addProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    }

    /**
     * Creates a simple configuration for a channel receiving from a topic of the same name
     * <p>
     * The message type is String
     *
     * @param kafka       the kafka container
     * @param channelName the channel and topic name
     * @param groupId     the reader group id (used to commit message offsets)
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleIncomingChannel(KafkaContainer kafka, String channelName, String groupId) {
        return simpleIncomingChannel(kafka.getBootstrapServers(), channelName, groupId);
    }

    /**
     * Creates a simple configuration for a channel receiving from a topic of the same name
     * <p>
     * The message type is String
     *
     * @param kafkaBootstrapServers the kafka bootstrap server config
     * @param channelName           the channel and topic name
     * @param groupId               the reader group id (used to commit message offsets)
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleIncomingChannel(String kafkaBootstrapServers, String channelName, String groupId) {
        return new ConnectorProperties(Direction.INCOMING, channelName)
                        .addProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
                        .addProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
                        .addProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
                        .addProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId)
                        .addProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                        .addProperty("connector", "io.openliberty.kafka")
                        .addProperty("topics", channelName);
    }

    private final String prefix;

    public ConnectorProperties(Direction direction, String channelName) {
        prefix = "mp.messaging." + direction.getValue() + "." + channelName + ".";
    }

    @Override
    public ConnectorProperties addProperty(String key, String value) {
        super.addProperty(prefix + key, value);
        return this;
    }

    public enum Direction {
        INCOMING("incoming"),
        OUTGOING("outgoing"),
        CONNECTOR("connector");

        private String value;

        private Direction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
