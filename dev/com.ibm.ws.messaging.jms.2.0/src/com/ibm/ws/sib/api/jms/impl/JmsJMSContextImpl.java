/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.IllegalStateException;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.InvalidClientIDException;
import javax.jms.InvalidClientIDRuntimeException;
import javax.jms.InvalidDestinationException;
import javax.jms.InvalidDestinationRuntimeException;
import javax.jms.InvalidSelectorException;
import javax.jms.InvalidSelectorRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TransactionRolledBackException;
import javax.jms.TransactionRolledBackRuntimeException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.JmsJMSContext;
import com.ibm.ws.sib.utils.ras.SibTr;

//JMS2.0 - New class

/**
 * This class encapsulates JmsConnectionImpl and JmsSessionImpl.
 * <p>
 * For every subsequent creation of context using {@link JmsJMSContext#createContext(int)} a
 * new session is created and the conneciton is shared
 * <p>
 * The jsmConnection will be closed only when the last {@link JmsJMSContextImpl#close()} is invoked
 */
public class JmsJMSContextImpl implements JmsJMSContext {
    private final JmsConnectionImpl jmsConnection;
    private JmsSessionImpl jmsSession;

    // Specifies whether the underlying connection used by this JMSContext 
    // will be started automatically when a consumer is created.
    // This is the default behaviour, and it may be disabled by 
    //calling setAutoStart(boolean) with a value of false.
    private boolean autoStart = true;

    private static TraceComponent tc = SibTr.register(JmsJMSContextImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    /**
     * While creating a session, transacted is set as true only if the sessionMode is {@link JMSContext#SESSION_TRANSACTED} and in all
     * the other cases the transacted is set as false
     * 
     * @param jmsConnection The jmsConnection
     * @param sessionMode Mode of the session can be {@link JMSContext#AUTO_ACKNOWLEDGE}, {@link JMSContext#CLIENT_ACKNOWLEDGE}, {@link JMSContext#DUPS_OK_ACKNOWLEDGE},
     *            {@link JMSContext#SESSION_TRANSACTED}
     * @throws JMSRuntimeException
     */
    JmsJMSContextImpl(JmsConnectionImpl jmsConnection, int sessionMode, boolean fixClientID) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsJMSContextImpl", new Object[] { jmsConnection, sessionMode });

        this.jmsConnection = jmsConnection;

        try {

            if (sessionMode == JMSContext.SESSION_TRANSACTED)
                jmsSession = (JmsSessionImpl) jmsConnection.createSession(true, sessionMode);
            else
                jmsSession = (JmsSessionImpl) jmsConnection.createSession(false, sessionMode);

            //As per JMS spec, if any method is called on javax.jms.Connection object, then user will not be able to make setClientId() call. If doing so, will throw IllegalStateException.
            //While creating the JmsJMSContextImpl object, we make a call to javax.jms.Connection.createSession(), so invoking the setClientId() 
            //on the javax.jms.JMSContext will throw the  IllegalStateException which is not expected behavior since we should allow the user to call setClient() on JMSContext once.
            //Hence, we are unfixClientID if the clientId in connection object is null or its a default client Id i.e clientID 
            if (fixClientID && (jmsConnection.getClientID() == null || jmsConnection.isDefaultClientId(jmsConnection.getClientID()))) {
                jmsConnection.unfixClientID();
            }
        } catch (JMSException e) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(e, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "JmsJMSContextImpl", new Object[] { this.jmsConnection, this.jmsSession });
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#acknowledge()
     */
    @Override
    public void acknowledge() throws IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "acknowledge");

        try {

            if (jmsSession == null) {
                IllegalStateException ise = (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                                                         "INVALID_FOR_UNCONSUMED_MSG_CWSIA0110",
                                                                                                         new Object[] { "acknowledge" },
                                                                                                         tc
                                );
                throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
            }

            synchronized (jmsSession.getSessionSyncLock()) { // lock on the jmssession's sessionSyncLock
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "got lock");

                // Throw an exception if the session is closed.
                jmsSession.checkNotClosed();

                // throw an exception if the acknowledge method conflicts with async usage
                jmsSession.checkSynchronousUsage("acknowledge");

                // Perform the appropriate action for session's ack mode. The action for
                // a dups ok session in JMS1.1 was somewhat unspecified, so choose to commit.
                // But in JMS2.0 clearly commit is only for the client ack mode
                int sessAck = jmsSession.getAcknowledgeMode();
                if ((sessAck == Session.CLIENT_ACKNOWLEDGE)) {
                    jmsSession.commitTransaction();
                }
            }

        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);

        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "acknowledge");
        }

    }

    /*
     * Users can create multiple JMSContext using {@link JmsJMSContextImpl#createContext(int)},
     * and close the context using {@link JmsJMSContextImpl#close()}.
     * The last call to {@link JmsJMSContextImpl#close()} will ensure that the jmsConnection is closed
     * 
     * @see javax.jms.JMSContext#close()
     */
    @Override
    public void close() throws IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");

        try {

            jmsSession.close();// close the session
            if (jmsConnection.getSessionCount() == 0)// if its a last JMSContext, close the connection itself
                jmsConnection.close();
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "close");
        }
    }

    /*
     * Commit the transaction that is currently in effect for this session.
     * Note, if there is no transaction (e.g. in auto ack mode), an exception
     * will be thrown.
     * 
     * @see javax.jms.JMSContext#commit()
     */
    @Override
    public void commit() throws IllegalStateRuntimeException, TransactionRolledBackRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commit");

        try {
            jmsSession.commit();
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (TransactionRolledBackException trbe) {
            throw (TransactionRolledBackRuntimeException) JmsErrorUtils.getJMS2Exception(trbe, TransactionRolledBackRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "commit");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createBrowser(javax.jms.Queue)
     */
    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSRuntimeException, InvalidDestinationRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowser", queue);

        QueueBrowser queueBrowser = null;
        try {
            queueBrowser = jmsSession.createBrowser(queue);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createBrowser");
        }

        return queueBrowser;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createBrowser(javax.jms.Queue, java.lang.String)
     */
    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSRuntimeException, InvalidDestinationRuntimeException, InvalidSelectorRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowser", new Object[] { queue, messageSelector });

        QueueBrowser queueBrowser = null;

        try {
            queueBrowser = jmsSession.createBrowser(queue, messageSelector);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (InvalidSelectorException ise) {
            throw (InvalidSelectorRuntimeException) JmsErrorUtils.getJMS2Exception(ise, InvalidSelectorRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createBrowser");
        }

        return queueBrowser;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createBytesMessage()
     */
    @Override
    public BytesMessage createBytesMessage() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBytesMessage");

        BytesMessage bytesMessage = null;

        try {
            bytesMessage = jmsSession.createBytesMessage();
        } catch (JMSException e) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(e, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createBytesMessage");
        }
        return bytesMessage;
    }

    /**
     * By default {@link autoStart} is true, i,e connection is started as soon as the first consumer is created.
     * This behaviour can be changed by user by setting {@link autoStart} to false
     */
    private void autoStartConsumer() throws JMSException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "autoStartConsumer", new Object[] { autoStart, jmsConnection.getState() });

        try {
            //check if autoStart is true and connection is not started.
            //If its already started, it implies the first consumer has already started the connection
            if (autoStart && (jmsConnection.getState() == JmsInternalConstants.STOPPED)) {
                // start the connection
                jmsConnection.start();
            }
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "autoStartConsumer");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createConsumer(javax.jms.Destination)
     */
    @Override
    public JMSConsumer createConsumer(Destination destination) throws JMSRuntimeException, InvalidDestinationRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumer", new Object[] { destination });

        JMSConsumer jmsConsumer = null;
        try {
            MessageConsumer messageConsumer = jmsSession.createConsumer(destination);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createConsumer(javax.jms.Destination, java.lang.String)
     */
    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector) throws JMSRuntimeException, InvalidDestinationRuntimeException, InvalidSelectorRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumer", new Object[] { destination, messageSelector });

        JMSConsumer jmsConsumer = null;
        try {
            MessageConsumer messageConsumer = jmsSession.createConsumer(destination, messageSelector);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (InvalidSelectorException ise) {
            throw (InvalidSelectorRuntimeException) JmsErrorUtils.getJMS2Exception(ise, InvalidSelectorRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createConsumer(javax.jms.Destination, java.lang.String, boolean)
     */
    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSRuntimeException, InvalidDestinationRuntimeException, InvalidSelectorRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumer", new Object[] { destination, messageSelector, noLocal });

        JMSConsumer jmsConsumer = null;
        try {
            MessageConsumer messageConsumer = jmsSession.createConsumer(destination, messageSelector, noLocal);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (InvalidSelectorException ise) {
            throw (InvalidSelectorRuntimeException) JmsErrorUtils.getJMS2Exception(ise, InvalidSelectorRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;
    }

    /*
     * For every {@link JmsJMSContextImpl#createContext(int)} the same jmsConnection
     * is used but a new jmsSession is created.
     * 
     * @see javax.jms.JMSContext#createContext(int)
     */
    @Override
    public JMSContext createContext(int sessionMode) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createContext", new Object[] { sessionMode });

        JmsJMSContextImpl jmsContext = null;

        // If the context created is container-managed, then JMSRuntimeException must be thrown
        if (jmsConnection.isManaged()) {
            JMSException jmse = (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                          "CREATECONTEXT_IN_CONTAINER_CWSIA0513",
                                                                          null, tc);
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        }

        try {
            //CTS test failure was reported when the session mode was passed as -1,  so adding the validation
            switch (sessionMode)
            {
                case JMSContext.AUTO_ACKNOWLEDGE:
                case JMSContext.CLIENT_ACKNOWLEDGE:
                case JMSContext.DUPS_OK_ACKNOWLEDGE:
                case JMSContext.SESSION_TRANSACTED: {
                    break;
                }
                default:
                    throw (JMSRuntimeException) JmsErrorUtils.newThrowable(JMSRuntimeException.class,
                                                                           "INVALID_ACKNOWLEDGE_MODE_CWSIA0514",
                                                                           new Object[] {},
                                                                           tc
                                    );
            }

            jmsContext = new JmsJMSContextImpl(jmsConnection, sessionMode, false);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createContext", new Object[] { jmsContext });
        }
        return jmsContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createDurableConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name) throws InvalidDestinationRuntimeException, IllegalStateRuntimeException, JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableConsumer", new Object[] { topic, name });

        JMSConsumer jmsConsumer = null;

        try {
            MessageConsumer messageConsumer = jmsSession.createDurableConsumer(topic, name);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createDurableConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createDurableConsumer(javax.jms.Topic, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) throws InvalidDestinationRuntimeException, InvalidSelectorRuntimeException, IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableConsumer", new Object[] { topic, name, messageSelector, noLocal });

        JMSConsumer jmsConsumer = null;

        try {
            MessageConsumer messageConsumer = jmsSession.createDurableConsumer(topic, name, messageSelector, noLocal);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (InvalidSelectorException ise) {
            throw (InvalidSelectorRuntimeException) JmsErrorUtils.getJMS2Exception(ise, InvalidSelectorRuntimeException.class);
        } catch (IllegalStateException istatee) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(istatee, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createDurableConsumer", new Object[] { jmsConsumer });

        }

        return jmsConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createMapMessage()
     */
    @Override
    public MapMessage createMapMessage() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createMapMessage");

        MapMessage mapMessage = null;

        try {
            mapMessage = jmsSession.createMapMessage();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createMapMessage", new Object[] { mapMessage });
        }

        return mapMessage;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createMessage()
     */
    @Override
    public Message createMessage() throws JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createMessage");

        Message message = null;

        try {
            message = jmsSession.createMessage();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createMessage", new Object[] { message });
        }

        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createObjectMessage()
     */
    @Override
    public ObjectMessage createObjectMessage() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createObjectMessage");

        ObjectMessage objectMessage = null;

        try {
            objectMessage = jmsSession.createObjectMessage();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createObjectMessage", new Object[] { objectMessage });
        }

        return objectMessage;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createObjectMessage(java.io.Serializable)
     */
    @Override
    public ObjectMessage createObjectMessage(Serializable object) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createObjectMessage", new Object[] { object });

        ObjectMessage objectMessage = null;

        try {
            objectMessage = jmsSession.createObjectMessage(object);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createObjectMessage", new Object[] { objectMessage });
        }

        return objectMessage;

    }

    /*
     * According to JMS 2.0 spec jmsexception is not throwm back
     * to the caller.Hence any exception will be absorbed and FFDC
     * will be logged
     * 
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createProducer()
     */
    @Override
    public JMSProducer createProducer() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createProducer");

        JMSProducer jmsProducer = null;
        try {
            // Create messageproducer first, since we reuse most in jmsproducer
            // destination is passed as null because during send,destination is passed
            // jmssession.createproducer() is not used becuase it stores the list of producers
            // which is not required for simplified API's
            MessageProducer msgProducer = jmsSession.instantiateProducer(null);
            jmsProducer = new JmsJMSProducerImpl(msgProducer);
        } catch (JMSException e) {
            // should never have got here because we pass destination as null so there is no chance 
            // that instantiateProducer() will throw any exception

            FFDCFilter.processException(e,
                                        JmsJMSContextImpl.class.toString() + ".createProducer",
                                        "598", this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createProducer");

        return jmsProducer;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createQueue(java.lang.String)
     */
    @Override
    public Queue createQueue(String queueName) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createQueue", new Object[] { queueName });

        Queue queue = null;
        try {
            queue = jmsSession.createQueue(queueName);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createQueue", new Object[] { queue });
        }

        return queue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createSharedConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSRuntimeException, InvalidDestinationRuntimeException, InvalidSelectorRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedConsumer", new Object[] { topic, sharedSubscriptionName });

        JMSConsumer jmsConsumer = null;
        try {

            MessageConsumer messageConsumer = jmsSession.createSharedConsumer(topic, sharedSubscriptionName);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (InvalidSelectorException ise) {
            throw (InvalidSelectorRuntimeException) JmsErrorUtils.getJMS2Exception(ise, InvalidSelectorRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createSharedConsumer(javax.jms.Topic, java.lang.String, java.lang.String)
     */
    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSRuntimeException, InvalidDestinationRuntimeException, InvalidSelectorRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedConsumer", new Object[] { topic, sharedSubscriptionName, messageSelector });

        JMSConsumer jmsConsumer = null;
        try {

            MessageConsumer messageConsumer = jmsSession.createSharedConsumer(topic, sharedSubscriptionName, messageSelector);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (InvalidSelectorException ise) {
            throw (InvalidSelectorRuntimeException) JmsErrorUtils.getJMS2Exception(ise, InvalidSelectorRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createSharedDurableConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name) throws InvalidDestinationRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedDurableConsumer", new Object[] { topic, name });

        JMSConsumer jmsConsumer = null;
        try {

            MessageConsumer messageConsumer = jmsSession.createSharedDurableConsumer(topic, name);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedDurableConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createSharedDurableConsumer(javax.jms.Topic, java.lang.String, java.lang.String)
     */
    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) throws InvalidDestinationRuntimeException, InvalidSelectorRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedDurableConsumer", new Object[] { topic, name, messageSelector });

        JMSConsumer jmsConsumer = null;
        try {

            MessageConsumer messageConsumer = jmsSession.createSharedDurableConsumer(topic, name, messageSelector);
            jmsConsumer = new JmsJMSConsumerImpl(messageConsumer);
            autoStartConsumer();
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (InvalidSelectorException ise) {
            throw (InvalidSelectorRuntimeException) JmsErrorUtils.getJMS2Exception(ise, InvalidSelectorRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedDurableConsumer", new Object[] { jmsConsumer });
        }

        return jmsConsumer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createStreamMessage()
     */
    @Override
    public StreamMessage createStreamMessage() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createStreamMessage");

        StreamMessage streamMessage = null;

        try {
            streamMessage = jmsSession.createStreamMessage();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createStreamMessage", new Object[] { streamMessage });
        }

        return streamMessage;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createTemporaryQueue()
     */
    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryQueue");

        TemporaryQueue tempQueue = null;
        try {
            tempQueue = jmsSession.createTemporaryQueue();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createTemporaryQueue", new Object[] { tempQueue });
        }

        return tempQueue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createTemporaryTopic()
     */
    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryTopic");

        TemporaryTopic temptopic = null;
        try {
            temptopic = jmsSession.createTemporaryTopic();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createTemporaryTopic", new Object[] { temptopic });
        }

        return temptopic;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createTextMessage()
     */
    @Override
    public TextMessage createTextMessage() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTextMessage");

        TextMessage textMessage = null;

        try {
            textMessage = jmsSession.createTextMessage();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createTextMessage", new Object[] { textMessage });
        }

        return textMessage;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createTextMessage(java.lang.String)
     */
    @Override
    public TextMessage createTextMessage(String text) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTextMessage", new Object[] { text });

        TextMessage textMessage = null;

        try {
            textMessage = jmsSession.createTextMessage(text);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createTextMessage", new Object[] { textMessage });
        }

        return textMessage;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#createTopic(java.lang.String)
     */
    @Override
    public Topic createTopic(String topicName) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTopic", new Object[] { topicName });

        Topic topic = null;

        try {
            topic = jmsSession.createTopic(topicName);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createTopic", new Object[] { topic });
        }

        return topic;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#getAutoStart()
     */
    @Override
    public boolean getAutoStart() {
        return autoStart;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#getClientID()
     */
    @Override
    public String getClientID() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getClientID");

        String clientId = null;
        try {
            clientId = jmsConnection.getClientID();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getClientID", new Object[] { clientId });
        }

        return clientId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#getExceptionListener()
     */
    @Override
    public ExceptionListener getExceptionListener() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getExceptionListener");

        ExceptionListener exceptionListener = null;

        try {
            exceptionListener = jmsConnection.getExceptionListener();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getExceptionListener", new Object[] { exceptionListener });
        }

        return exceptionListener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#getMetaData()
     */
    @Override
    public ConnectionMetaData getMetaData() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMetaData");

        ConnectionMetaData connectionMetaData = null;
        try {
            connectionMetaData = jmsConnection.getMetaData();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getMetaData", new Object[] { connectionMetaData });
        }

        return connectionMetaData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#getSessionMode()
     */
    @Override
    public int getSessionMode() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getSessionMode");

        int sessionMode = -1;
        try {
            sessionMode = jmsSession.getAcknowledgeMode();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getSessionMode", new Object[] { sessionMode });
        }

        return sessionMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#getTransacted()
     */
    @Override
    public boolean getTransacted() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTransacted");

        boolean transacted = false;
        try {
            transacted = jmsSession.getTransacted();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getTransacted", new Object[] { transacted });
        }

        return transacted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#recover()
     */
    @Override
    public void recover() throws IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "recover");

        try {
            jmsSession.recover();
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "recover");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#rollback()
     */
    @Override
    public void rollback() throws IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rollback");

        try {
            jmsSession.rollback();
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "rollback");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#setAutoStart(boolean)
     */
    @Override
    public void setAutoStart(boolean autoStart) throws IllegalStateRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setAutoStart", new Object[] { autoStart });

        try {
            this.autoStart = autoStart;
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setAutoStart", new Object[] { this.autoStart });
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#setClientID(java.lang.String)
     */
    @Override
    public void setClientID(String clientID) throws InvalidClientIDRuntimeException, IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setClientID", new Object[] { clientID });

        try {
            jmsConnection.setClientID(clientID);
        } catch (InvalidClientIDException icie) {
            throw (InvalidClientIDRuntimeException) JmsErrorUtils.getJMS2Exception(icie, InvalidClientIDRuntimeException.class);
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setClientID");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#setExceptionListener(javax.jms.ExceptionListener)
     */
    @Override
    public void setExceptionListener(ExceptionListener eListener) throws IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setExceptionListener", new Object[] { eListener });
        try {
            jmsConnection.setExceptionListener(eListener);
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setExceptionListener");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#start()
     */
    @Override
    public void start() throws IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "start");
        try {
            jmsConnection.start();
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "start");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#stop()
     */
    @Override
    public void stop() throws IllegalStateRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stop");

        try {
            jmsConnection.stop();
        } catch (IllegalStateException ise) {
            throw (IllegalStateRuntimeException) JmsErrorUtils.getJMS2Exception(ise, IllegalStateRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "stop");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSContext#unsubscribe(java.lang.String)
     */
    @Override
    public void unsubscribe(String subName) throws JMSRuntimeException, InvalidDestinationRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unsubscribe", new Object[] { subName });
        try {
            jmsSession.unsubscribe(subName);
        } catch (InvalidDestinationException ide) {
            throw (InvalidDestinationRuntimeException) JmsErrorUtils.getJMS2Exception(ide, InvalidDestinationRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "unsubscribe");
        }
    }

}
