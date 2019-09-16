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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public abstract class KafkaAdapterFactory {

    private static final String KAFKA_CONSUMER_IMPL = "com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl.KafkaConsumerImpl";
    private static final Class<?>[] KAFKA_CONSUMER_ARG_TYPES = { Map.class };

    private static final String KAFKA_PRODUCER_IMPL = "com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl.KafkaProducerImpl";
    private static final Class<?>[] KAFKA_PRODUCER_ARG_TYPES = { Map.class };

    private static final String TOPIC_PARTITION_IMPL = "com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl.TopicPartitionImpl";
    private static final Class<?>[] TOPIC_PARTITION_ARG_TYPES = { String.class, int.class };

    private static final String OFFSET_AND_METADATA_IMPL = "com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl.OffsetAndMetadataImpl";
    private static final Class<?>[] OFFSET_AND_METADATA_ARG_TYPES = { long.class, Optional.class, String.class };

    private static final String COMMIT_FAILED_EXCEPTION = "org.apache.kafka.clients.consumer.CommitFailedException";
    private static final Class<?>[] COMMIT_FAILED_EXCEPTION_ARG_TYPES = {};

    protected abstract ClassLoader getClassLoader();

    /**
     * @param <K>
     * @param <V>
     * @param consumerConfig
     * @return
     */
    public <K, V> KafkaConsumer<K, V> newKafkaConsumer(Map<String, Object> consumerConfig) {
        @SuppressWarnings("unchecked")
        KafkaConsumer<K, V> kafkaConsumer = getInstance(getClassLoader(), KafkaConsumer.class, KAFKA_CONSUMER_IMPL, KAFKA_CONSUMER_ARG_TYPES, consumerConfig);
        return kafkaConsumer;
    }

    /**
     * @param producerConfig
     * @return
     */
    public <K, V> KafkaProducer<K, V> newKafkaProducer(Map<String, Object> producerConfig) {
        @SuppressWarnings("unchecked")
        KafkaProducer<K, V> kafkaProducer = getInstance(getClassLoader(), KafkaProducer.class, KAFKA_PRODUCER_IMPL, KAFKA_PRODUCER_ARG_TYPES, producerConfig);
        return kafkaProducer;
    }

    /**
     * @param topic
     * @param partition
     * @return
     */
    public TopicPartition newTopicPartition(String topic, int partition) {
        TopicPartition topicPartition = getInstance(getClassLoader(), TopicPartition.class, TOPIC_PARTITION_IMPL, TOPIC_PARTITION_ARG_TYPES, topic, partition);
        return topicPartition;
    }

    /**
     * @param commitOffset
     * @param leaderEpoch
     * @param metadata
     * @return
     */
    public OffsetAndMetadata newOffsetAndMetadata(long commitOffset, Optional<Integer> leaderEpoch, String metadata) {
        OffsetAndMetadata offsetAndMetadata = getInstance(getClassLoader(), OffsetAndMetadata.class, OFFSET_AND_METADATA_IMPL, OFFSET_AND_METADATA_ARG_TYPES,
                                                          commitOffset, leaderEpoch, metadata);
        return offsetAndMetadata;
    }

    /**
     * @return
     */
    public Exception newCommitFailedException() {
        Exception e = getInstance(getClassLoader(), Exception.class, COMMIT_FAILED_EXCEPTION, COMMIT_FAILED_EXCEPTION_ARG_TYPES);
        return e;
    }

    protected static final <T> T getInstance(ClassLoader classloader, Class<T> interfaceClass, String implClassName, Class<?>[] parameterTypes, Object... parameters) {
        Class<? extends T> implClass = getImplClass(classloader, interfaceClass, implClassName);
        T instance = getInstance(implClass, parameterTypes, parameters);
        return instance;
    }

    protected static final <T> T getInstance(Class<T> implClass, Class<?>[] parameterTypes, Object[] parameters) {
        Constructor<T> xtor = getConstructor(implClass, parameterTypes);

        T instance = null;
        try {
            instance = xtor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new KafkaAdapterException(e);
        }
        return instance;
    }

    protected static final <T> Constructor<T> getConstructor(Class<T> implClass, Class<?>... parameterTypes) {
        Constructor<T> xtor = null;
        try {
            xtor = implClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new KafkaAdapterException(e);
        }
        return xtor;
    }

    @SuppressWarnings("unchecked")
    protected static final <T> Class<? extends T> getImplClass(ClassLoader classloader, Class<T> interfaceClass, String implClassName) {
        Class<? extends T> implClass = null;
        try {
            implClass = (Class<? extends T>) classloader.loadClass(implClassName);
        } catch (ClassNotFoundException e) {
            throw new KafkaAdapterException(e);
        }
        return implClass;
    }
}
