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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.testcontainers.containers.KafkaContainer;

import com.ibm.websphere.simplicity.PropertiesAsset;

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

    public static final String DEFAULT_CONNECTOR_ID = "liberty-kafka";

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
     * Creates a simple configuration for a channel sending to a topic of the given name
     * <p>
     * The message type is String
     *
     * @param kafka       the kafka container
     * @param channelName the channel and topic name
     * @param topic       the topic name
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleOutgoingChannel(KafkaContainer kafka, String channelName, String topic) {
        return simpleOutgoingChannel(kafka, DEFAULT_CONNECTOR_ID, channelName, topic);
    }

    /**
     * Creates a simple configuration for a channel sending to a topic of the same name
     * <p>
     * The message type is String
     *
     * @param kafka       the kafka container
     * @param connectorID the connector ID
     * @param channelName the channel and topic name
     * @param topic       the topic name
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleOutgoingChannel(KafkaContainer kafka, String connectorID, String channelName, String topic) {
        return simpleOutgoingChannel(Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()), connectorID, channelName, topic);
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
        return simpleOutgoingChannel(Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBoostrapServers), channelName);
    }

    /**
     * Creates a simple configuration for a channel sending to a topic of the same name
     * <p>
     * The message type is String
     *
     * @param connectionProperties the properties required to connect to the kafka broker
     * @param channelName          the channel and topic name
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleOutgoingChannel(Map<? extends String, ?> connectionProperties, String channelName) {
        return new ConnectorProperties(Direction.OUTGOING, channelName)
                        .addAll(connectionProperties)
                        .addProperty("connector", DEFAULT_CONNECTOR_ID);
    }

    /**
     * Creates a simple configuration for a channel sending to a topic of the same name
     * <p>
     * The message type is String
     *
     * @param connectionProperties the properties required to connect to the kafka broker
     * @param connectorID          the name of the connector
     * @param channelName          the channel name
     * @param topic                the name of the topic
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleOutgoingChannel(Map<? extends String, ?> connectionProperties, String connectorID, String channelName, String topic) {
        return simpleOutgoingChannel(connectionProperties, channelName)
                        .addProperty("connector", connectorID)
                        .addProperty("topic", topic);
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
        return simpleIncomingChannel(Collections.singletonMap(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers), channelName, groupId);
    }

    /**
     * Creates a simple configuration for a channel receiving from a topic of the same name
     * <p>
     * The message type is String
     *
     * @param connectionProperties the properties required to connect to the kafka broker
     * @param channelName          the channel and topic name
     * @param groupId              the reader group id (used to commit message offsets)
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleIncomingChannel(Map<? extends String, ?> connectionProperties, String channelName, String groupId) {
        return new ConnectorProperties(Direction.INCOMING, channelName)
                        .addAll(connectionProperties)
                        .addProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId)
                        .addProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                        .addProperty("connector", DEFAULT_CONNECTOR_ID);
    }

    /**
     * Creates a simple configuration for a channel
     * <p>
     * The message type is String
     *
     * @param connectionProperties the properties required to connect to the kafka broker
     * @param connectorID          the connector ID
     * @param channelName          the channel name
     * @param groupId              the reader group id (used to commit message offsets)
     * @param topic                the topic names
     * @return the ConnectorProperties to add to the app configuration
     */
    public static ConnectorProperties simpleIncomingChannel(Map<? extends String, ?> connectionProperties, String connectorID, String channelName, String groupId, String topic) {
        return simpleIncomingChannel(connectionProperties, channelName, groupId)
                        .addProperty("connector", connectorID)
                        .addProperty("topic", topic);
    }

    private final String prefix;
    private final Direction direction;

    public ConnectorProperties(Direction direction, String channelName) {
        this.direction = direction;
        prefix = "mp.messaging." + direction.getValue() + "." + channelName + ".";
    }

    @Override
    public ConnectorProperties addProperty(String key, String value) {
        super.addProperty(prefix + key, value);
        return this;
    }

    @Override
    public ConnectorProperties removeProperty(String key) {
        super.removeProperty(prefix + key);
        return this;
    }

    public ConnectorProperties addAll(Map<?, ?> properties) {
        for (Entry<?, ?> entry : properties.entrySet()) {
            addProperty((String) entry.getKey(), (String) entry.getValue());
        }
        return this;
    }

    public ConnectorProperties topic(String topic) {
        switch (direction) {
            case INCOMING:
                addProperty("topic", topic);
                break;
            case OUTGOING:
                addProperty("topic", topic);
                break;
            default:
                throw new IllegalArgumentException("topic not allowed");
        }
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
