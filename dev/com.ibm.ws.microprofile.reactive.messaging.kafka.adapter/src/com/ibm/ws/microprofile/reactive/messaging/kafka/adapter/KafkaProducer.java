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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter;

import java.time.Duration;

/**
 *
 */
public interface KafkaProducer<K, V> extends KafkaAdapter {

    /**
     * @param topic
     * @param channelName
     * @param value
     * @param callback
     */
    void send(String topic, String channelName, V value, Callback callback);

    void close(Duration timeout);

}
