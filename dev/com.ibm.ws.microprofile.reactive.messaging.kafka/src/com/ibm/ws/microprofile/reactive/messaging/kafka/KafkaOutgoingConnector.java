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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaProducer;

@Connector("io.openliberty.kafka")
@ApplicationScoped
public class KafkaOutgoingConnector implements OutgoingConnectorFactory {

    @Inject
    private KafkaAdapterFactory kafkaAdapterFactory;

    @Override
    public SubscriberBuilder<Message<Object>, Void> getSubscriberBuilder(Config config) {
        // Pass the config directly through to the kafkaProducer
        Map<String, Object> producerConfig = StreamSupport.stream(config.getPropertyNames().spliterator(),
                                                                  false).collect(Collectors.toMap(Function.identity(), (k) -> config.getValue(k, String.class)));

        KafkaProducer<String, Object> kafkaProducer = this.kafkaAdapterFactory.newKafkaProducer(producerConfig);
        KafkaOutput<String, Object> kafkaOutput = new KafkaOutput<>(config.getValue("topic", String.class), kafkaProducer);

        return ReactiveStreams.<Message<Object>> builder().to(kafkaOutput.getSubscriber());
    }

}
