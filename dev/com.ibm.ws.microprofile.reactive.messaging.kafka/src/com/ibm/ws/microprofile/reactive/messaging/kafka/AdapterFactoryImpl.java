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

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.Callback;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRebalanceListener;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecords;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapter;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterException;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaAdapterFactory;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaConsumer;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.KafkaProducer;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetAndMetadata;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetCommitCallback;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.RecordMetadata;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.TopicPartition;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.WakeupException;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl.AbstractKafkaAdapter;
import com.ibm.ws.microprofile.reactive.messaging.kafka.classloader.AppLibraryClassLoader;

@ApplicationScoped
public class AdapterFactoryImpl extends KafkaAdapterFactory {

    private ClassLoader appLibLoader;

    @PostConstruct
    private void init() {
        this.appLibLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            List<Class<?>> interfaces = Arrays.asList(KafkaAdapterFactory.class,
                                                      Callback.class,
                                                      ConsumerRebalanceListener.class,
                                                      ConsumerRecord.class,
                                                      ConsumerRecords.class,
                                                      KafkaAdapter.class,
                                                      KafkaAdapterException.class,
                                                      KafkaConsumer.class,
                                                      KafkaProducer.class,
                                                      OffsetAndMetadata.class,
                                                      OffsetCommitCallback.class,
                                                      RecordMetadata.class,
                                                      TopicPartition.class,
                                                      WakeupException.class);

            URL[] urls = new URL[] { AbstractKafkaAdapter.class.getProtectionDomain().getCodeSource().getLocation() };// URL for the adapter impl bundle

            return new AppLibraryClassLoader(urls, interfaces, Thread.currentThread().getContextClassLoader());
        });
    }

    @Override
    protected ClassLoader getClassLoader() {
        return this.appLibLoader;
    }

}
