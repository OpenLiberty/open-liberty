/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaProducer;

@Connector(KafkaConnectorConstants.CONNECTOR_NAME)
@ApplicationScoped
public class KafkaOutgoingConnector implements OutgoingConnectorFactory {

    private static final TraceComponent tc = Tr.register(KafkaOutgoingConnector.class);

    @Inject
    private KafkaAdapterFactory kafkaAdapterFactory;

    private final List<KafkaOutput<?, ?>> kafkaOutputs = Collections.synchronizedList(new ArrayList<>());

    @Override
    public SubscriberBuilder<Message<Object>, Void> getSubscriberBuilder(Config config) {

        String channelName = config.getValue(ConnectorFactory.CHANNEL_NAME_ATTRIBUTE, String.class);

        try {
            // Configure our defaults
            Map<String, Object> producerConfig = new HashMap<>();

            //default the key and value serializers to String
            producerConfig.put(KafkaConnectorConstants.KEY_SERIALIZER, KafkaConnectorConstants.STRING_SERIALIZER);
            producerConfig.put(KafkaConnectorConstants.VALUE_SERIALIZER, KafkaConnectorConstants.STRING_SERIALIZER);

            // Pass the config directly through to the kafkaProducer
            producerConfig.putAll(StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                                               .filter(k -> !KafkaConnectorConstants.NON_KAFKA_PROPS.contains(k))
                                               .collect(Collectors.toMap(Function.identity(), (k) -> config.getValue(k, String.class))));

            KafkaProducer<String, Object> kafkaProducer = this.kafkaAdapterFactory.newKafkaProducer(producerConfig);

            String configuredTopic = config.getOptionalValue(KafkaConnectorConstants.TOPIC, String.class).orElse(null);

            KafkaOutput<String, Object> kafkaOutput = new KafkaOutput<>(this.kafkaAdapterFactory, configuredTopic, channelName, kafkaProducer);
            this.kafkaOutputs.add(kafkaOutput);

            return ReactiveStreams.<Message<Object>> builder().to(kafkaOutput.getSubscriber());
        } catch (Exception e) {
            throw new KafkaConnectorException(Tr.formatMessage(tc, "kafka.create.outgoing.error.CWMRX1008E", channelName, e.getMessage()), e);
        }
    }

    @PreDestroy
    private void shutdown() {
        synchronized (kafkaOutputs) {
            for (KafkaOutput<?, ?> kafkaOutput : kafkaOutputs) {
                kafkaOutput.shutdown(Duration.ZERO);
            }
        }
    }

}
