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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;

@Connector(KafkaConnectorConstants.CONNECTOR_NAME)
@ApplicationScoped
public class KafkaIncomingConnector implements IncomingConnectorFactory {

    private static final TraceComponent tc = Tr.register(KafkaIncomingConnector.class);

    ManagedScheduledExecutorService executor;

    @Inject
    KafkaAdapterFactory kafkaAdapterFactory;

    private final List<KafkaInput<?, ?>> kafkaInputs = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    private void postConstruct() {
        Bundle b = FrameworkUtil.getBundle(KafkaIncomingConnector.class);
        ServiceReference<ManagedScheduledExecutorService> mgdSchedExecSvcRef = b.getBundleContext().getServiceReference(ManagedScheduledExecutorService.class);
        this.executor = b.getBundleContext().getService(mgdSchedExecSvcRef);

        if (this.executor == null) {
            String msg = Tr.formatMessage(tc, "internal.kafka.connector.error.CWMRX1000E", "The Managed Scheduled Executor Service could not be found.");
            throw new IllegalStateException(msg);
        }
    }

    @PreDestroy
    private void shutdown() {
        synchronized (kafkaInputs) {
            for (KafkaInput<?, ?> kafkaInput : kafkaInputs) {
                try {
                    kafkaInput.shutdown();
                } catch (Exception e) {
                    // Ensures we attempt to shutdown all inputs
                    // and also that we get an FFDC for any errors
                }
            }
        }
    }

    @Override
    public PublisherBuilder<Message<Object>> getPublisherBuilder(Config config) {

        // Extract our config
        String channelName = config.getValue(ConnectorFactory.CHANNEL_NAME_ATTRIBUTE, String.class);

        Optional<String> groupID = config.getOptionalValue(KafkaConnectorConstants.GROUP_ID, String.class);
        if (!groupID.isPresent()) {
            String msg = Tr.formatMessage(tc, "kafka.groupid.not.set.CWMRX1005E", ConnectorFactory.INCOMING_PREFIX + channelName + "." + KafkaConnectorConstants.GROUP_ID);
            throw new IllegalArgumentException(msg);
        }

        String topic = config.getOptionalValue(KafkaConnectorConstants.TOPIC, String.class).orElse(channelName);
        int maxPollRecords = config.getOptionalValue(KafkaConnectorConstants.MAX_POLL_RECORDS, Integer.class).orElse(500);
        int unackedLimit = config.getOptionalValue(KafkaConnectorConstants.UNACKED_LIMIT, Integer.class).orElse(maxPollRecords);

        // Configure our defaults
        Map<String, Object> consumerConfig = new HashMap<>();
        // Default behaviour is that connector handles commit in response to ack()
        consumerConfig.put(KafkaConnectorConstants.ENABLE_AUTO_COMMIT, "false");
        //default the key and value deserializers to String
        consumerConfig.put(KafkaConnectorConstants.KEY_DESERIALIZER, KafkaConnectorConstants.STRING_DESERIALIZER);
        consumerConfig.put(KafkaConnectorConstants.VALUE_DESERIALIZER, KafkaConnectorConstants.STRING_DESERIALIZER);

        // Pass the rest of the config directly through to the kafkaConsumer
        consumerConfig.putAll(StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                                           .filter(k -> !KafkaConnectorConstants.NON_KAFKA_PROPS.contains(k))
                                           .collect(Collectors.toMap(Function.identity(), (k) -> config.getValue(k, String.class))));

        boolean enableAutoCommit = "true".equalsIgnoreCase((String) consumerConfig.get(KafkaConnectorConstants.ENABLE_AUTO_COMMIT));

        // Create the kafkaConsumer
        KafkaConsumer<String, Object> kafkaConsumer = this.kafkaAdapterFactory.newKafkaConsumer(consumerConfig);

        PartitionTrackerFactory partitionTrackerFactory = new PartitionTrackerFactory();
        partitionTrackerFactory.setExecutor(executor);
        partitionTrackerFactory.setAdapterFactory(kafkaAdapterFactory);
        partitionTrackerFactory.setAutoCommitEnabled(enableAutoCommit);

        if (enableAutoCommit) {
            unackedLimit = 0;
        }

        // Create our connector around the kafkaConsumer
        KafkaInput<String, Object> kafkaInput = new KafkaInput<>(this.kafkaAdapterFactory, partitionTrackerFactory, kafkaConsumer, this.executor,
                                                                 topic, unackedLimit);
        kafkaInputs.add(kafkaInput);

        return kafkaInput.getPublisher();
    }

}
