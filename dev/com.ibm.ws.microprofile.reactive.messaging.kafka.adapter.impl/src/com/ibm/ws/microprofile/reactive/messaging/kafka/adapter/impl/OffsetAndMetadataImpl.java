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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl;

import java.util.Optional;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.OffsetAndMetadata;

/**
 *
 */
public class OffsetAndMetadataImpl extends AbstractKafkaAdapter<org.apache.kafka.clients.consumer.OffsetAndMetadata> implements OffsetAndMetadata {

    /**
     * @param commitOffset
     * @param leaderEpoch
     * @param metadata
     */
    public OffsetAndMetadataImpl(long commitOffset, Optional<Integer> leaderEpoch, String metadata) {
        this(new org.apache.kafka.clients.consumer.OffsetAndMetadata(commitOffset, leaderEpoch, metadata));
    }

    /**
     * @param delegateOffsetAndMetadata
     */
    public OffsetAndMetadataImpl(org.apache.kafka.clients.consumer.OffsetAndMetadata delegateOffsetAndMetadata) {
        super(delegateOffsetAndMetadata);
    }

    @Override
    public long offset() {
        return this.getDelegate().offset();
    }
}
