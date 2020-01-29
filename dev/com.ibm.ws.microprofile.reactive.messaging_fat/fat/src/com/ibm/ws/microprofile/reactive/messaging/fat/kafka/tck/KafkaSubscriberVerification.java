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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaTestConstants;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.suite.PlaintextTests;
import com.ibm.ws.microprofile.reactive.messaging.kafka.KafkaOutput;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaProducer;

/**
 * Reactive Streams TCK test for the Kafka outgoing connector
 */
public class KafkaSubscriberVerification extends SubscriberWhiteboxVerification<Message<String>> {

    private final KafkaTestClient kafkaTestClient = new KafkaTestClient(PlaintextTests.kafkaContainer.getBootstrapServers());
    private final KafkaAdapterFactory kafkaAdapterFactory = new TestKafkaAdapterFactory();

    private int testNo = 0;
    private String testName;
    private final List<KafkaOutput<?, ?>> kafkaOutputs = new ArrayList<>();

    @BeforeMethod
    public void handleTestMethodName(Method method) {
        testName = method.getName();
    }

    @AfterMethod
    public void cleanup() {
        kafkaTestClient.cleanUp();
        for (KafkaOutput<?, ?> output : kafkaOutputs) {
            try {
                output.shutdown(KafkaTestConstants.KAFKA_ENVIRONMENT_TIMEOUT);
            } catch (Exception e) {
            }
        }
        kafkaOutputs.clear();
    }

    /**
     * @param env
     */
    public KafkaSubscriberVerification() {
        super(new TestEnvironment(KafkaTestConstants.KAFKA_ENVIRONMENT_TIMEOUT.toMillis()));
    }

    @Override
    public Subscriber<Message<String>> createSubscriber(WhiteboxSubscriberProbe<Message<String>> probe) {
        // Pick a kafka topic name
        String topicName = "kafka-subscriber-test-" + ++testNo + "-" + testName;

        // Create the subscriber
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.CLIENT_ID_CONFIG, "kafka-publisher-verification-consumer-" + testNo);
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, PlaintextTests.kafkaContainer.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        KafkaProducer<String, String> kafkaProducer = kafkaAdapterFactory.newKafkaProducer(config);
        KafkaOutput<String, String> kafkaOutput = new KafkaOutput<>(kafkaAdapterFactory, topicName, "fakeChannelName", kafkaProducer);
        kafkaOutputs.add(kafkaOutput);
        return new VerificationSubscriber<>(probe, kafkaOutput);
    }

    @Override
    public Message<String> createElement(int element) {
        return Message.of("test-message-" + element);
    }

    public static class VerificationSubscriber<T> implements Subscriber<Message<T>> {

        private final WhiteboxSubscriberProbe<Message<T>> probe;
        private final Subscriber<Message<T>> delegate;

        public VerificationSubscriber(WhiteboxSubscriberProbe<Message<T>> probe, KafkaOutput<?, T> kafkaOutput) {
            super();
            this.probe = probe;
            this.delegate = kafkaOutput.getSubscriber().build();
        }

        @Override
        public void onSubscribe(final Subscription s) {
            delegate.onSubscribe(s);

            // register a successful Subscription, and create a Puppet,
            // for the WhiteboxVerification to be able to drive its tests:
            probe.registerOnSubscribe(new SubscriberPuppet() {

                @Override
                public void triggerRequest(long elements) {
                    // Do nothing, our subscriber always requests more
                }

                @Override
                public void signalCancel() {
                    // We can't prompt KafkaInput to cancel on demand so just do it directly
                    // (It will cancel on next message, but that's not good enough for the test)
                    s.cancel();
                }
            });
        }

        @Override
        public void onNext(Message<T> element) {
            // in addition to normal Subscriber work that you're testing, register onNext with the probe
            delegate.onNext(element);
            probe.registerOnNext(element);
        }

        @Override
        public void onError(Throwable cause) {
            // in addition to normal Subscriber work that you're testing, register onError with the probe
            delegate.onError(cause);
            probe.registerOnError(cause);
        }

        @Override
        public void onComplete() {
            // in addition to normal Subscriber work that you're testing, register onComplete with the probe
            delegate.onComplete();
            probe.registerOnComplete();
        }
    }

}
