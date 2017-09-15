/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jms;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/*
 * For the JMSContext created within the transaction we need to keep track for the entire transaction
 * scope.  If there is another injected object with the same annotations ( same connection factory, user name etc)
 * then we should return the same object.  We store those in the TransactionSynchronizationRegistry
 * 
 * In case of non transacted context,  we keep a local instance of the same  ( internalJMSContext)  and that
 * will get closed when the @Disposes event is received from CDI by the JMSContextProducerBean
 */

class JMSContextInjected implements JMSContext {

    // because we use a pack-info.java for trace options our group and message file is already there
    // We just need to register the class here
    private static final TraceComponent tc = Tr.register(JMSContextInjected.class);

    private static final String TSR_LOOKUP_NAME = "java:comp/TransactionSynchronizationRegistry";
    private JMSContext internalJMSContext = null;
    private final JMSContextInfo jmsContextInfo;
    private boolean inTransaction = false;

    JMSContextInjected(JMSContextInfo info) {
        this.jmsContextInfo = info;
    }

    /*
     * Create the JMSContext from the supplied configuration
     */
    private JMSContext createJMSContext(JMSContextInfo info, boolean tranIsActive) throws NamingException {
        inTransaction = tranIsActive;

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup(info.getConnectionFactoryString());

        if (inTransaction && cf instanceof XAConnectionFactory) {

            XAJMSContext xaContext = ((XAConnectionFactory) cf).createXAContext(info.getUserName(), info.getPassword());
            return xaContext.getContext();

        } else {

            return cf.createContext(info.getUserName(), info.getPassword(), info.getAcknowledgeMode());
        }
    }

    /*
     * Close the internal JMSContext object
     */
    synchronized void closeInternalJMSContext() {
        if (internalJMSContext != null && !inTransaction) {
            internalJMSContext.close();
            internalJMSContext = null;
        }
    }

    /*
     * If there is a transaction in progress, then create a new JMSContext add to Transaction registry and return the same
     * If there is no transaction, then create a new context and return the JMSContext
     */
    private synchronized JMSContext getInternalJMSContext() {
        TransactionSynchronizationRegistry tranSyncRegistry = null;
        try {

            boolean tranIsActive = false;

            tranSyncRegistry = (TransactionSynchronizationRegistry) new InitialContext().lookup(TSR_LOOKUP_NAME);

            if (tranSyncRegistry != null)
            {
                tranIsActive = (tranSyncRegistry.getTransactionStatus() == Status.STATUS_ACTIVE);
            }

            if (tranIsActive)
            {

                Object resource = tranSyncRegistry.getResource(jmsContextInfo);

                if (resource != null) {

                    return (JMSContext) resource;

                } else {

                    final JMSContext transactedContext = createJMSContext(jmsContextInfo, tranIsActive);

                    //Once the new JMSContext is created,  add it to the transaction registry to 
                    //retrieve it later if the new JMSContext is requested for the same configuration
                    //but in same transactional context
                    tranSyncRegistry.putResource(jmsContextInfo, transactedContext);

                    //Register a Synchronization object in transaction registry for a call back
                    //when the transaction is complete,  we will close the JMSContext once the 
                    //Transaction is complete
                    tranSyncRegistry.registerInterposedSynchronization(new Synchronization() {
                        @Override
                        public void beforeCompletion() {}

                        @Override
                        public synchronized void afterCompletion(int status) {
                            transactedContext.close();
                            inTransaction = false;
                        }
                    });

                    return transactedContext;
                }
            } else {

                //Non transacted,  create a new JMSContext and return it
                if (internalJMSContext == null) {
                    internalJMSContext = createJMSContext(jmsContextInfo, tranIsActive);
                }
                return internalJMSContext;
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    //Override all the methods of JMSContext and call the real object methods.

    @Override
    public void acknowledge() {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "acknowledge"));
    }

    @Override
    public void close() {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "acknowledge"));

    }

    @Override
    public void commit() {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "commit"));

    }

    @Override
    public QueueBrowser createBrowser(Queue queue) {
        return getInternalJMSContext().createBrowser(queue);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) {

        return getInternalJMSContext().createBrowser(queue, messageSelector);
    }

    @Override
    public BytesMessage createBytesMessage() {
        return getInternalJMSContext().createBytesMessage();
    }

    @Override
    public JMSConsumer createConsumer(Destination destination) {
        return getInternalJMSContext().createConsumer(destination);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector) {
        return getInternalJMSContext().createConsumer(destination, messageSelector);
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) {
        return getInternalJMSContext().createConsumer(destination, messageSelector, noLocal);
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return getInternalJMSContext().createContext(sessionMode);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name) {
        return getInternalJMSContext().createDurableConsumer(topic, name);
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) {
        return getInternalJMSContext().createDurableConsumer(topic, name, messageSelector, noLocal);
    }

    @Override
    public MapMessage createMapMessage() {
        return getInternalJMSContext().createMapMessage();
    }

    @Override
    public Message createMessage() {
        return getInternalJMSContext().createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() {
        return getInternalJMSContext().createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) {
        return getInternalJMSContext().createObjectMessage(object);
    }

    @Override
    public JMSProducer createProducer() {
        return getInternalJMSContext().createProducer();
    }

    @Override
    public Queue createQueue(String queueName) {
        return getInternalJMSContext().createQueue(queueName);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) {
        return getInternalJMSContext().createSharedConsumer(topic, sharedSubscriptionName);
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) {
        return getInternalJMSContext().createSharedConsumer(topic, sharedSubscriptionName, messageSelector);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name) {
        return getInternalJMSContext().createSharedDurableConsumer(topic, name);
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) {
        return getInternalJMSContext().createSharedDurableConsumer(topic, name, messageSelector);
    }

    @Override
    public StreamMessage createStreamMessage() {
        return getInternalJMSContext().createStreamMessage();
    }

    @Override
    public TemporaryQueue createTemporaryQueue() {
        return getInternalJMSContext().createTemporaryQueue();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() {
        return getInternalJMSContext().createTemporaryTopic();
    }

    @Override
    public TextMessage createTextMessage() {
        return getInternalJMSContext().createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) {
        return getInternalJMSContext().createTextMessage(text);
    }

    @Override
    public Topic createTopic(String topicName) {
        return getInternalJMSContext().createTopic(topicName);
    }

    @Override
    public boolean getAutoStart() {
        return getInternalJMSContext().getAutoStart();
    }

    @Override
    public String getClientID() {
        return getInternalJMSContext().getClientID();
    }

    @Override
    public ExceptionListener getExceptionListener() {
        return getInternalJMSContext().getExceptionListener();
    }

    @Override
    public ConnectionMetaData getMetaData() {
        return getInternalJMSContext().getMetaData();
    }

    @Override
    public int getSessionMode() {
        return getInternalJMSContext().getSessionMode();
    }

    @Override
    public boolean getTransacted() {
        return getInternalJMSContext().getTransacted();
    }

    @Override
    public void recover() {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "recover"));

    }

    @Override
    public void rollback() {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "rollback"));

    }

    @Override
    public void setAutoStart(boolean arg0) {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "setAutoStart"));

    }

    @Override
    public void setClientID(String arg0) {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "setClientID"));

    }

    @Override
    public void setExceptionListener(ExceptionListener arg0) {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "setExceptionListener"));

    }

    @Override
    public void start() {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "start"));

    }

    @Override
    public void stop() {
        throw new IllegalStateRuntimeException(Tr.formatMessage(tc, "JMSCONTEXT_INJECTED_CWSIA0512", "stop"));

    }

    @Override
    public void unsubscribe(String name) {
        getInternalJMSContext().unsubscribe(name);

    }

}
