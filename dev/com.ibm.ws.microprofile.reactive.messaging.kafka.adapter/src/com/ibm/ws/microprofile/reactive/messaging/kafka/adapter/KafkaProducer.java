/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
     * @param producerRecord
     * @param callback
     */
    void send(ProducerRecord<K, V> producerRecord, Callback callback);

    void close(Duration timeout);

}
