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
package org.test.config.jmsadapter;

import java.lang.reflect.Proxy;
import java.util.Enumeration;

import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

public class JMSTopicConnectionImpl implements TopicConnection, ConnectionMetaData {
    private final JMSTopicConnectionFactoryImpl tcf;

    JMSTopicConnectionImpl(JMSTopicConnectionFactoryImpl tcf) {
        this.tcf = tcf;
    }

    @Override
    public void close() throws JMSException {
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages) {
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

    @Override
    public Session createSession() throws JMSException {
        return (TopicSession) Proxy.newProxyInstance(TopicSession.class.getClassLoader(),
                                                     new Class[] { TopicSession.class },
                                                     new NoOpSessionImpl());
    }

    @Override
    public Session createSession(int sessionMode) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return createTopicSession(transacted, acknowledgeMode);
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
        return (TopicSession) Proxy.newProxyInstance(TopicSession.class.getClassLoader(),
                                                     new Class[] { TopicSession.class },
                                                     new NoOpSessionImpl());
    }

    @Override
    public String getClientID() throws JMSException {
        return tcf.mcf.getClientId();
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
        return "TestConfig Messaging Provider";
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
        return 88;
    }

    @Override
    public int getProviderMinorVersion() throws JMSException {
        return 105;
    }

    @Override
    public String getProviderVersion() throws JMSException {
        return "88.105.137";
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
