/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testjms.web.impl;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Enumeration;

public final class JmsTestFramework implements AutoCloseable {
    private static final String JNDI_QUEUE_NAME = "jms/libertyQ";
    private static final long RECEIVE_TIMEOUT_MS = 5000L;

    private final Connection conn;
    public final Queue queue;
    public final Session session;

    JmsTestFramework(ConnectionFactoryType factoryType) throws Exception {
        final Context context = new InitialContext();
        queue = (Queue)context.lookup(JNDI_QUEUE_NAME);

        ConnectionFactory cf = (ConnectionFactory)context.lookup(factoryType.jndiName);
        conn = cf.createConnection();
        conn.start();

        session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        clearQueue(queue);
    }

    public <T extends Message> T sendAndReceive(T message, Class<T> type, Destination destination) throws Exception {
        send(message, destination);
        return receive(type, destination);
    }

    public <T extends Message> T receive(Class<T> type, Destination destination) throws Exception {
        try (final MessageConsumer consumer = session.createConsumer(destination)) {
            return type.cast(consumer.receive(RECEIVE_TIMEOUT_MS));
        }
    }

    public <T extends Message> void send(T message, Destination destination) throws Exception {
        try (final MessageProducer producer = session.createProducer(destination)) {
            producer.send(message);
        }
    }

    public int clearQueue(final Queue queue) throws Exception {
        int elements = 0;
        try (final MessageConsumer consumer = session.createConsumer(queue)) {
            while (null != consumer.receiveNoWait()) {
                elements++;
            }
        }
        return elements;
    }

    public void close() throws Exception {
        try (final Connection c = conn) {
             try (final Session s = session) {
                 clearQueue(queue);
             }
        }
    }
}
