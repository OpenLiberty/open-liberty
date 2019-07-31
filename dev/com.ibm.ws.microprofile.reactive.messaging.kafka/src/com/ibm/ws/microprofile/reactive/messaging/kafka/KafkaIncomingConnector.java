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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;

@Connector("io.openliberty.kafka")
@ApplicationScoped
public class KafkaIncomingConnector implements IncomingConnectorFactory {

    private static final TraceComponent tc = Tr.register(KafkaIncomingConnector.class);

    //properties extracted from config
    private static final String TOPICS = "topics";
    private static final String UNACKED_LIMIT = "unacked.limit";
    //Kafka property
    private static final String ENABLE_AUTO_COMMIT = "enable.auto.commit";

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
        List<String> topics = Arrays.asList(config.getValue(TOPICS, String.class).split(" *, *", -1));
        int unackedLimit = config.getOptionalValue(UNACKED_LIMIT, Integer.class).orElse(20);

        // Pass the rest of the config directly through to the kafkaConsumer
        Map<String, Object> consumerConfig = new HashMap<>(StreamSupport.stream(config.getPropertyNames().spliterator(),
                                                                                false).collect(Collectors.toMap(Function.identity(), (k) -> config.getValue(k, String.class))));

        // Set the config values which we hard-code
        consumerConfig.put(ENABLE_AUTO_COMMIT, "false"); // Connector handles commit in response to ack()
                                                         // automatically

        // Create the kafkaConsumer
        KafkaConsumer<String, Object> kafkaConsumer = this.kafkaAdapterFactory.newKafkaConsumer(consumerConfig);

        // Create our connector around the kafkaConsumer
        KafkaInput<String, Object> kafkaInput = new KafkaInput<>(this.kafkaAdapterFactory, kafkaConsumer, this.executor, topics, unackedLimit);
        kafkaInputs.add(kafkaInput);

        return kafkaInput.getPublisher();
    }

}
