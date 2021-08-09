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
package org.test.validator.jmsadapter;

import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;

import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

public class JMSConnectionImpl implements QueueConnection, TopicConnection, ConnectionMetaData {
    private final String user;

    public JMSConnectionImpl(String user) {
        this.user = user;
    }

    @Override
    public void close() throws JMSException {
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Queue queue, String messageSelector, ServerSessionPool sessionPool, int maxSessions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Topic topic, String messageSelector, ServerSessionPool sessionPool, int maxMessages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) {
        throw new UnsupportedOperationException();
    }

    private <T extends Session> T createNoOpSession(Class<T> sessionInterface) throws JMSException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<T>) //
            () -> sessionInterface.cast(Proxy.newProxyInstance(sessionInterface.getClassLoader(),
                                                               new Class[] { sessionInterface },
                                                               new NoOpSessionImpl())));
        } catch (PrivilegedActionException x) {
            throw (JMSException) new JMSException(x.getCause().getMessage()).initCause(x.getCause());
        }
    }

    @Override
    public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return createNoOpSession(QueueSession.class);
    }

    @Override
    public Session createSession() throws JMSException {
        return createNoOpSession(Session.class);
    }

    @Override
    public Session createSession(int sessionMode) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return createNoOpSession(Session.class);
    }

    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String messageSelector, String sessionPool, ServerSessionPool arg3, int maxMessages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TopicSession createTopicSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return createNoOpSession(TopicSession.class);
    }

    @Override
    public String getClientID() throws JMSException {
        return user;
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return null;
    }

    @Override
    public int getJMSMajorVersion() throws JMSException {
        return 2;
    }

    @Override
    public int getJMSMinorVersion() throws JMSException {
        return 0;
    }

    @Override
    public String getJMSProviderName() throws JMSException {
        return "TestValidation Messaging Provider";
    }

    @Override
    public String getJMSVersion() throws JMSException {
        return "2.0";
    }

    @Override
    public Enumeration<?> getJMSXPropertyNames() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getProviderMajorVersion() throws JMSException {
        return 60;
    }

    @Override
    public int getProviderMinorVersion() throws JMSException {
        return 221;
    }

    @Override
    public String getProviderVersion() throws JMSException {
        return "60.221.229";
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return this;
    }

    @Override
    public void setClientID(String value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws JMSException {
    }

    @Override
    public void stop() throws JMSException {
    }
}