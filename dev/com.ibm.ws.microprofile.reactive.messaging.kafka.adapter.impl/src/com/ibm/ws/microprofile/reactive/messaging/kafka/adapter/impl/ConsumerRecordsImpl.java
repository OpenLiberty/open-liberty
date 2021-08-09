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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecord;
import com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.ConsumerRecords;

/**
 *
 */
public class ConsumerRecordsImpl<K, V> extends AbstractKafkaAdapter<org.apache.kafka.clients.consumer.ConsumerRecords<K, V>> implements ConsumerRecords<K, V> {

    private static final String CLAZZ = ConsumerRecordsImpl.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLAZZ);

    /**
     * @param delegateRecords
     */
    public ConsumerRecordsImpl(org.apache.kafka.clients.consumer.ConsumerRecords<K, V> delegateRecords) {
        super(delegateRecords);
    }

    /**
     * @return
     */
    @Override
    public boolean isEmpty() {
        return getDelegate().isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<ConsumerRecord<K, V>> iterator() {
        Iterator<ConsumerRecord<K, V>> itr = new DelegateIterator(getDelegate().iterator());
        return itr;
    }

    private class DelegateIterator implements Iterator<ConsumerRecord<K, V>> {

        private final Iterator<org.apache.kafka.clients.consumer.ConsumerRecord<K, V>> delegate;

        /**
         * @param iterator
         */
        public DelegateIterator(Iterator<org.apache.kafka.clients.consumer.ConsumerRecord<K, V>> delegate) {
            this.delegate = delegate;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public ConsumerRecord<K, V> next() {
            org.apache.kafka.clients.consumer.ConsumerRecord<K, V> delegateNext = this.delegate.next();
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLAZZ + ".DelegateIterator", "next", "ConsumerRecord: {0}", delegateNext);
            }
            ConsumerRecord<K, V> next = new ConsumerRecordImpl<>(delegateNext);
            return next;
        }

    }

}
