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
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;

/**
 *
 */
public class KafkaConnectorConstants {

    //Our own custom properties that we extract from config for the connector
    //The producer topic
    public static final String TOPIC = "topic";
    //The consumer topics
    public static final String TOPICS = "topics";
    //The limit on on the number of un-acknowledged messages
    public static final String UNACKED_LIMIT = "unacked.limit";

    //The set of properties which should NOT be passed through to the Kafka client
    public static final Set<String> NON_KAFKA_PROPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new String[] { TOPIC, TOPICS, ConnectorFactory.CONNECTOR_ATTRIBUTE,
                                                                                                                             ConnectorFactory.CHANNEL_NAME_ATTRIBUTE })));

    //Kafka property - org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG
    public static final String ENABLE_AUTO_COMMIT = "enable.auto.commit";

    //The unique name of this MicroProfile Reactive Messaging Connector for Kafka
    public static final String CONNECTOR_NAME = "io.openliberty.kafka";

}
