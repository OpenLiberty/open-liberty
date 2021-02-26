/*******************************************************************************
 * Copyright (c) 2012, 2015, 2020 IBM Corporation and others.
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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.BytesMessage;
import javax.jms.CompletionListener;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.IllegalStateException;
import javax.jms.InvalidDestinationException;
import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
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
import javax.jms.TopicSubscriber;
import javax.jms.TransactionRolledBackException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsFactoryFactory;
import com.ibm.websphere.sib.api.jms.JmsMsgConsumer;
import com.ibm.websphere.sib.api.jms.JmsMsgProducer;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.JmsSession;
import com.ibm.ws.sib.api.jms.JmsTemporaryDestinationInternal;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.Distribution;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SIInvalidDestinationPrefixException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

public class JmsSessionImpl implements JmsSession, ApiJmsConstants, JmsInternalConstants
{

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsSessionImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // **************************** STATE VARIABLES ******************************

    /**
     * Properties map that originates from the connection factory (it is passed
     * through to the Session's producers and consumers).
     */
    private final Map passThruProps;

    /**
     * The core connection that this session is associated with (used when
     * creating temporary destinations, producers, consumers).
     */
    private final SICoreConnection coreConnection;

    /**
     * The JMS connection that this session is associated with (holds properties
     * and sets state that affects this session).
     */
    private final JmsConnectionImpl connection;

    /**
     * Used to obtain and manage transactions.
     */
    private final JmsJcaSession jcaSession;

    /**
     * A flag indicating if the session is locally transacted (may be overridden
     * by configuring a global XA transaction if the session is in a managed
     * environment).
     */
    private final boolean transacted;

    /**
     * If the session is not transacted, this value indicates whether the session
     * is to be used in client, auto or dups-ok acknowledge mode.
     */
    private final int acknowledgeMode;

    /**
     * Flag indictaing if the session is running in a managed environment (i.e.
     * in the application server).
     */
    private final boolean isManaged;

    /**
     * Number of messages received since the last commit call.
     */
    private int uncommittedReceiveCount = 0;

    /**
     * Number of messages that should be received before the internal transaction
     * controlling a dups-ok session is committed.
     */
    private final int dupsCommitThreshold;

    /**
     * Current session state (stopped, started or closed).
     */
    private int state = STOPPED;

    /**
     * Synchronization object for state (all acesses to the state value must
     * synchronize on this object).
     */
    private final Object stateLock = new Object();

    /**
     * Object used to serialize asynchronous message delivery (i.e. calls to
     * onMessage).
     */
    private final Object asyncDeliveryLock = new Object();

    /**
     * Object used to serialize concurrent calls to vulnerable methods
     * (e.g. send/receive/commit etc)
     */
    private final Object sessionSyncLock = new Object();

    /**
     * List of producers under this session.
     */
    private final List producers;

    /**
     * List of synchronous consumers under this session.
     */
    private final List syncConsumers;

    /**
     * List of asynchronous consumers under this session.
     */
    private final List asyncConsumers;

    /**
     * lock for grouping actions on the sync/async consumer lists
     */
    private final Object consumerListsLock = new Object();

    /**
     * List of browsers under this session.
     */
    private final List browsers;

    /**
     * This byte array is used to allocate messageIDs as messages are
     * sent. It contains two parts (in this order);
     * i) 12 bytes of the session uniqueID as obtained from the coreSPI.
     * ii) 8 bytes (long) counter ID that is incremented per message.
     * 
     * The first part is set up in the class constructor, leaving
     * space for the 8 byte counter to be filled in by the createMessageID
     * method each time a messageID is requested.
     * 
     */
    private final byte[] currentMessageID;

    /**
     * An orderingContext for use with consumers and producers.
     */
    private OrderingContext orderingContext;

    /**
     * The unique prefix supplied by the core connection is not
     * of a sufficient length to fill the remainder of the 24 bytes
     * after we add the unique counter part so we need to arbitrary
     * bytes to insert.
     * Define 16 pad bytes, as the absoule maximum which could be needed.
     */
    private static final byte[] MSG_ID_PAD_BYTES = new byte[] { 0x11, 0x0A, 0x13, 0x4F, 0x21, 0x20, 0x4A, 0x4D,
                                                               0x53, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    /**
     * PK60857: Flag used to work around the problem of some components calling
     * rollback after receiving TransactionRolledBackException during commit.
     * They shouldn't do this, but do. Without this check they would receive a
     * SIIncorrectCallException from commit.
     */
    private boolean rolledBackDueToConnectionFailure = false;

    /**
     * Threshold at which opening a new producer or consumer will cause a warning message
     * to be output to the console (indicating to the application that it may
     * be leaking resources).
     */
    private static final int PROD_CONS_WARNING_THRESHOLD = 100;

    private static final int DEFAULT_DUPS_THRESHOLD = 20;

    /**
     * _asyncSendQueue is Queue which holds AysnSend operations of all message producers which are created through it.
     */
    private final LinkedBlockingQueue<AysncSendDetails> _asyncSendQueue = new LinkedBlockingQueue<AysncSendDetails>();

    /**
     * Each session would have a single thread named asyncRunThread .. which would take care of executing
     * AsyncSend operations.
     */
    private Thread _asyncSendRunThread = null;

    /**
     * A dummy AysncSendDetails is added when session is closed.. this dummy AsyncSendDetails is used
     * as self destroy command by asyncSendRunThread.
     */
    private static final int AsyncSendKillCommand = 7777;

    /**
     * asyncThreadLocal stores the session object which owns the async send operation.
     */
    static final ThreadLocal<JmsSessionImpl> asyncThreadLocal = new ThreadLocal<JmsSessionImpl>();

    /**
     * current running async send details object. Before giving asyncSendQueue empty notification, this is made null
     */
    private volatile AysncSendDetails currentAsyncSendObject = null;

    /**
     * asyncReceiverLocal stores the session object which owns the async recv operation.
     */
    private static final ThreadLocal<JmsSessionImpl> asyncReceiverThreadLocal = new ThreadLocal<JmsSessionImpl>();

    // ***************************** CONSTRUCTORS ********************************

    JmsSessionImpl(boolean transacted, int acknowledgeMode, SICoreConnection coreConnection, JmsConnectionImpl connection, JmsJcaSession jcaSession) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsSessionImpl", new Object[] { transacted, acknowledgeMode, coreConnection, connection, jcaSession });

        // These should be the first thing of any importance that happens to avoid
        // confusion with scope of variable names.
        this.transacted = transacted;
        this.acknowledgeMode = acknowledgeMode;
        this.connection = connection;
        this.coreConnection = coreConnection;
        this.jcaSession = jcaSession;
        this.isManaged = connection.isManaged();
        this.passThruProps = connection.getPassThruProps();
        this.dupsCommitThreshold = DEFAULT_DUPS_THRESHOLD;
        this.producers = Collections.synchronizedList(new ArrayList());
        this.syncConsumers = Collections.synchronizedList(new ArrayList());
        this.asyncConsumers = Collections.synchronizedList(new ArrayList());
        this.browsers = Collections.synchronizedList(new ArrayList());

        // Add an extra check to ensure that the coreConnection is present (should have
        // been caught further up - but doesn't hurt to be on the safe side).
        if (coreConnection == null) {
            // d238447 FFDC review. Generate FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "JCA_RESOURCE_EXC_CWSIA0005",
                                                            null,
                                                            null,
                                                            "JmsSessionImpl.constructor#2",
                                                            this,
                                                            tc
                            );
        }

        // Calculate the message ID template for this JMS Session. (see javadoc
        // for the currentMessageID variable for details).
        try {
            // Get the unique stem for this Session
            byte[] messageIDStem = coreConnection.createUniqueId();
            int messageIDStemLength = messageIDStem.length;

            // Now convert this into the right message ID template for this session.
            // The template starts with the stem, and then leaves space for a long
            // increment counter. It must be 24 bytes in length so that it matches
            // the MQ messageID format. We add some padding bytes between the unique
            // stem and the counter to fill up any space.
            currentMessageID = new byte[24];
            System.arraycopy(messageIDStem, 0, currentMessageID, 0, messageIDStemLength);

            // How many characters are required to fill up the space? In general
            // we will need 4 extra bytes, unless they change the unique prefix
            // length again!
            int charsNeeded = 16 - messageIDStemLength;
            if (charsNeeded > 0) {
                System.arraycopy(MSG_ID_PAD_BYTES, 0, currentMessageID, messageIDStemLength, charsNeeded);
            }
        } catch (SIException sice) {
            // No FFDC code needed
            // d238447 FFDC review. Various connectLost exceptions and Resource exception. Should already
            //   have generated FFDCs, so don't FFDC again here.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { sice, "JmsSessionImpl.<constructor>" },
                                                            sice,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc);
        }

        // obtain an ordering context
        try {
            orderingContext = connection.allocateOrderingContext();
        } catch (SIException sice) {
            // No FFDC code needed
            // Not expecting errors from createOrderingContext, so exception received is ok.
            // d238447 FFDC review. Declared exceptions are:
            //   SIConnectionUnavailableException, SIConnectionDroppedException. Should already have
            //   been FFDC'd, so don't FFDC again here.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { sice, "JmsSessionImpl.<constructor>" },
                                                            sice,
                                                            null, // null probeId = no FFDC.
                                                            this,
                                                            tc);
        }

        // Now we have done the initialization, pick up the start/stopped state
        // of the Connection.
        if (connection.getState() == STARTED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "start session because connection was started");
            start();
        }
        else {
            stop();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsSessionImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * This method creates proxy for the given message object. This is used only in Async Send..
     * Hence this method is called only by client environments.
     * 
     * @param msgObject
     * @return Message interface implementation of SIBus.
     */
    private Message createMessageProxy(Message msgObject) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createMessageProxy", msgObject);

        Message retObj;
        try {

            retObj = (Message) Proxy.newProxyInstance(msgObject.getClass().getClassLoader(),
                                                      msgObject.getClass().getInterfaces(),
                                                      new MessageProxyInvocationHandler(msgObject));
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Exception caught while trying to createMessageProxy ", e);

            //if any exception .. return the same msgObject.
            retObj = msgObject;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createMessageProxy", retObj);

        return retObj;

    }

    /**
     * @see javax.jms.Session#createBytesMessage()
     */
    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBytesMessage");

        checkNotClosed();
        JmsBytesMessageImpl bm = new JmsBytesMessageImpl();
        setMessageProperties(bm);

        //create proxy message object only in non-managed environments
        if (!isManaged) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createBytesMessage", bm.getClass() + "@" + System.identityHashCode(bm));

            return (BytesMessage) createMessageProxy(bm);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createBytesMessage", bm.getClass() + "@" + System.identityHashCode(bm));
        return bm;
    }

    /**
     * @see javax.jms.Session#createMapMessage()
     */
    @Override
    public MapMessage createMapMessage() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createMapMessage");

        checkNotClosed();
        MapMessage msg = new JmsMapMessageImpl();

        //create proxy message object only in non-managed environments
        if (!isManaged) {
            msg = (MapMessage) createMessageProxy(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createMapMessage", msg.getClass() + "@" + System.identityHashCode(msg));
        return msg;
    }

    /**
     * @see javax.jms.Session#createMessage()
     */
    @Override
    public Message createMessage() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createMessage");

        checkNotClosed();
        Message msg = new JmsMessageImpl();

        //create proxy message object only in non-managed environments
        if (!isManaged) {
            msg = createMessageProxy(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createMessage", msg.getClass() + "@" + System.identityHashCode(msg));
        return msg;
    }

    /**
     * @see javax.jms.Session#createObjectMessage()
     */
    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createObjectMessage");

        checkNotClosed();
        ObjectMessage msg = createObjectMessage(null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createObjectMessage", msg.getClass() + "@" + System.identityHashCode(msg));
        return msg;
    }

    /**
     * @see javax.jms.Session#createObjectMessage(Serializable)
     */
    @Override
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createObjectMessage", (object == null ? "null" : object.getClass()));

        checkNotClosed();
        JmsObjectMessageImpl om = new JmsObjectMessageImpl(object);
        setMessageProperties(om);

        //create proxy message object only in non-managed environments
        if (!isManaged) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createObjectMessage", om.getClass() + "@" + System.identityHashCode(om));
            return (ObjectMessage) createMessageProxy(om);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createObjectMessage", om.getClass() + "@" + System.identityHashCode(om));
        return om;
    }

    /**
     * @see javax.jms.Session#createStreamMessage()
     */
    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createStreamMessage");

        checkNotClosed();
        StreamMessage msg = new JmsStreamMessageImpl();

        //create proxy message object only in non-managed environments
        if (!isManaged) {
            msg = (StreamMessage) createMessageProxy(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createStreamMessage");
        return msg;
    }

    /**
     * @see javax.jms.Session#createTextMessage()
     */
    @Override
    public TextMessage createTextMessage() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTextMessage");

        checkNotClosed();
        TextMessage msg = new JmsTextMessageImpl();

        //create proxy message object only in non-managed environments
        if (!isManaged) {
            msg = (TextMessage) createMessageProxy(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createTextMessage", msg.getClass() + "@" + System.identityHashCode(msg));
        return msg;
    }

    /**
     * @see javax.jms.Session#createTextMessage(String)
     */
    @Override
    public TextMessage createTextMessage(String text) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTextMessage", text);

        checkNotClosed();
        TextMessage msg = new JmsTextMessageImpl(text);

        //create proxy message object only in non-managed environments
        if (!isManaged) {
            msg = (TextMessage) createMessageProxy(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createTextMessage", msg.getClass() + "@" + System.identityHashCode(msg));
        return msg;
    }

    /**
     * @see javax.jms.Session#getTransacted()
     */
    @Override
    public boolean getTransacted() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTransacted");
        checkNotClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTransacted", transacted);
        return transacted;
    }

    /**
     * @see javax.jms.Session#getAcknowledgeMode()
     */
    @Override
    public int getAcknowledgeMode() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAcknowledgeMode");
        checkNotClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getAcknowledgeMode", acknowledgeMode);
        return acknowledgeMode;
    }

    /**
     * @see javax.jms.Session#commit()
     */
    @Override
    public void commit() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commit");

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");

            // throw an exception if the session is closed.
            checkNotClosed();
            // throw an exception if the session is async
            checkSynchronousUsage("commit");
            // throw an exception if the session is not transacted.
            if (acknowledgeMode != Session.SESSION_TRANSACTED) {
                throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(
                                                                                   javax.jms.IllegalStateException.class,
                                                                                   "INVALID_OP_FOR_NONTRANS_SESSION_CWSIA0042",
                                                                                   new Object[] { "commit" },
                                                                                   tc);
            }

            //if it is non-managed environment.. we have to check that this call should not have made from owning context
            // and has to wait till all Async sends are resolved.
            if (!isManaged) {

                validateCloseCommitRollback("commit");

                //wait till all Async sends are resolved on this Session. 
                waitForAsyncSendCompletion();
            }

            // commit the current transaction.
            commitTransaction();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commit");
    }

    /**
     * @see javax.jms.Session#rollback()
     */
    @Override
    public void rollback() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rollback");

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");

            // throw an exception if the session is closed.
            checkNotClosed();
            // throw an exception if the session is async
            checkSynchronousUsage("rollback");
            // throw an exception if the session is not transacted.
            if (acknowledgeMode != Session.SESSION_TRANSACTED) {
                throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(
                                                                                   javax.jms.IllegalStateException.class,
                                                                                   "INVALID_OP_FOR_NONTRANS_SESSION_CWSIA0042",
                                                                                   new Object[] { "rollback" },
                                                                                   tc);
            }

            //if it is non-managed environment.. we have to check that this call should not have made from owning context
            // and has to wait till all Async sends are resolved.
            if (!isManaged) {

                validateCloseCommitRollback("rollback");

                //wait till all Async sends are resolved on this Session. 
                waitForAsyncSendCompletion();
            }

            // rollback the current transaction.
            rollbackTransaction();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "rollback");
    }

    /**
     * @see javax.jms.Session#close()
     */
    @Override
    public void close() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");
        close(false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    void close(final boolean tidyUp) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close", new Object[] { tidyUp });

        if (!isManaged) {
            //check whether the close call has come from CompletionListner onComplete/onException
            //or from MessageListner onMessage()... which is
            //driven by the this session. In that case as per JMS 2.0 spec we have to throw
            //IllegalStateException.
            validateCloseCommitRollback("close");
            validateStopCloseForMessageListener("close");
        }

        if (getState() != CLOSED) {

            //if it is non-managed environment.. we have to check that this call should not have made from owning context
            // and has to wait till all Async sends are resolved
            // (don't bother if the async send thread isn't running)
            if (!isManaged && null!=_asyncSendRunThread && _asyncSendRunThread.isAlive()) {
                addtoAsysncSendQueue(null,
                                     null,
                                     null,
                                     AsyncSendKillCommand, // kill command code for Async send thread.
                                     null);
                //wait until all the async sends have completed
                waitForAsyncSendCompletion();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "session " + this + " closing sync consumers.");

            synchronized (syncConsumers) {
                Object[] cons = syncConsumers.toArray();
                for (int i = 0; i < cons.length; i++) {
                    ((MessageConsumer) cons[i]).close();
                }
                syncConsumers.clear();
            }

            stop(); // This BLOCKS until onMessage completes
            setState(CLOSED);

            // Handle any transaction currently in effect.
            switch (acknowledgeMode) {
                case Session.SESSION_TRANSACTED:
                    // must rollback because client code must explicitly commit,
                    // or a configured XA transaction is in use (in which case may
                    // commit or rollback depending on configuration); all cases
                    // handled by jca code.
                    break;
                case Session.CLIENT_ACKNOWLEDGE:
                    // must rollback because client code must explicitly commit;
                    // rollback handled by jca code.
                    break;
                case Session.DUPS_OK_ACKNOWLEDGE:
                    // commit any messages delivered in the last incomplete batch.
                    commitTransaction();
                    break;
                case Session.AUTO_ACKNOWLEDGE:
                    // no transaction to handle, or a configured XA transaction is in
                    // use (in which case may commit or rollback depending on
                    // configuration); latter case handled by jca code.
                    break;
                default:
                    // this should never happen.
                    break;
            }

            // The code that follows closes attached producers, consumers and queue
            // browsers. An array copy of each list is obtained, and close issued
            // on the members of the array. Each member removes itself from the
            // list on close() (since we have an array copy, this code is
            // unaffected). The list clear() call should be spurious, as when it is
            // issued all members should have removed themselves from the list.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "session closing producers.");

            synchronized (producers) {
                Object[] prods = producers.toArray();
                for (int i = 0; i < prods.length; i++) {
                    ((JmsMsgProducerImpl) prods[i]).close(tidyUp);
                }
                producers.clear();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "session closing async consumers.");

            synchronized (asyncConsumers) {
                Object[] cons = asyncConsumers.toArray();
                for (int i = 0; i < cons.length; i++) {
                    ((MessageConsumer) cons[i]).close();
                }
                asyncConsumers.clear();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "session closing queue browsers.");

            synchronized (browsers) {
                Object[] brow = browsers.toArray();
                for (int i = 0; i < brow.length; i++) {
                    ((QueueBrowser) brow[i]).close();
                }
                browsers.clear();
            }

            // close jca session.
            try {
                jcaSession.close();
            } catch (SIException sice) {
                // No FFDC code needed
                // d222942 review
                // Not expecting errors from closing jcaSession, so exception received is ok.
                // d238447 FFDC review. Need to consider:
                // SIIncorrectCallException, SIResourceException, SIErrorException
                // I think we can reasonably generate FFDCs for all of these.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0053",
                                                                new Object[] { sice, "JmsSessionImpl.close" },
                                                                sice,
                                                                "JmsSessionImpl.close#1",
                                                                this,
                                                                tc
                                );
            }

            // Remove Session from Connection list.
            connection.removeSession(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /**
     * @see javax.jms.Session#recover()
     */
    @Override
    public void recover() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "recover");

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");

            // throw an exception if the session is closed.
            checkNotClosed();
            // throw an exception if the session is async
            checkSynchronousUsage("recover");

            // perform appropriate action depending on session mode.
            switch (acknowledgeMode) {
                case (Session.CLIENT_ACKNOWLEDGE):
                    // must rollback any uncommitted messages.
                    if (uncommittedReceiveCount > 0) {
                        rollbackTransaction();
                    }
                    break;
                case (Session.DUPS_OK_ACKNOWLEDGE):
                    // behaviour is somewhat unspecified - may as well commit any
                    // uncommitted messages, because user app doesn't have control over
                    // the batch.
                    if (uncommittedReceiveCount > 0) {
                        commitTransaction();
                    }
                    break;
                case (Session.SESSION_TRANSACTED):
                    // cannot invoke recover() on a transacted session.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "recover");
                    throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(
                                                                                       javax.jms.IllegalStateException.class,
                                                                                       "INVALID_OP_FOR_TRANS_SESSION_CWSIA0050",
                                                                                       new Object[] { "recover" },
                                                                                       tc);
                    // break;
                case Session.AUTO_ACKNOWLEDGE:
                    // This is done so that we can tell when an application onMessage
                    // calls recover under an auto_ack session.
                    uncommittedReceiveCount = 0;
                    break;
                default:
                    // do nothing.
                    break;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "recover");
    }

    /**
     * @see javax.jms.Session#getMessageListener()
     */
    @Override
    public MessageListener getMessageListener() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageListener");

        checkNotClosed();
        if (isManaged) {
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "MGD_ENV_CWSIA0052",
                                                                               new Object[] { "Session.getMessageListener" },
                                                                               tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "getMessageListener() optional, not implemented");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessageListener", null);
        return null;
    }

    /**
     * @see javax.jms.Session#setMessageListener(MessageListener)
     */
    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMessageListener", listener);

        checkNotClosed();
        if (isManaged) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setMessageListener", listener);
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "MGD_ENV_CWSIA0052",
                                                                               new Object[] { "Session.setMessageListener" },
                                                                               tc
                            );
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "setMessageListener(MessageListener) optional, not implemented");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMessageListener", listener);
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "UNSUPPORTED_OPERATION_CWSIA0045",
                                                                           new Object[] { "Session.setMessageListener" },
                                                                           tc
                        );
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // For use by AppServers
        // optional facility, not implemented by Jetstream.

        // J2EE 1.4 spec includes this in the list of non-permitted methods in a
        // managed environment. However, the following sentence seems to preclude
        // us doing anything:
        // "A J2EE container may throw a JMSException (if allowed by the method) if
        // the application component violates these restrictions."

        // Under the circumstances, slilent fail seems as reasonable as anything
        // else. JBK.
    }

    /**
     * @see javax.jms.Session#createProducer(Destination)
     */
    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createProducer", destination);

        checkNotClosed();

        // instantiate a MessageProducer (or subclass if overridden).
        JmsMsgProducer producer = instantiateProducer(destination);
        producers.add(producer);

        // d353701 - output a warning message if there are 'lots' of producers active.
        if (producers.size() % PROD_CONS_WARNING_THRESHOLD == 0) {
            // We wish to tell the user which line of their application created the session,
            // so we must obtain a line of stack trace from their application.
            String errorLocation = JmsErrorUtils.getFirstApplicationStackString();
            SibTr.warning(tc, "MANY_PRODUCERS_WARNING_CWSIA0055", new Object[] { "" + producers.size(), errorLocation });
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createProducer", producer);
        return producer;
    }

    /**
     * @see javax.jms.Session#createConsumer(Destination)
     */
    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumer", destination);

        checkNotClosed();
        MessageConsumer messageConsumer = createConsumer(destination, null, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumer", messageConsumer);
        return messageConsumer;
    }

    /**
     * @see javax.jms.Session#createConsumer(Destination, String)
     */
    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumer", new Object[] { destination, messageSelector });

        checkNotClosed();
        MessageConsumer messageConsumer = createConsumer(destination, messageSelector, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumer", messageConsumer);
        return messageConsumer;
    }

    /**
     * @see javax.jms.Session#createConsumer(Destination, String, boolean)
     */
    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumer", new Object[] { destination, null, messageSelector, noLocal });

        try {
            return createConsumer(destination, null, messageSelector, noLocal,
                                  false //is JMS2.0 shared non durable subscriber
            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createConsumer");
        }
    }

    // this method is called from two flows
    // one flow is: from JMS old APIs and JMS 2.0 API createConsumer - for Queue and non 'durable non shared'
    // The second flow is: from JMS 2.0 API createSharedConsumer - for 'non durable shared' 
    private MessageConsumer createConsumer(Destination destination, String name, String messageSelector, boolean noLocal,
                                           boolean isSharedNonDurable) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumer", new Object[] { destination, messageSelector, noLocal, isSharedNonDurable });
        JmsMsgConsumer consumer = null;

        checkNotClosed();
        if (destination == null) {
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "INVALID_VALUE_CWSIA0048",
                                                                           new Object[] { "Destination", "null" },
                                                                           tc
                            );
        }

        if (isSharedNonDurable) {
            if ((name == null) || ("".equals(name))) {
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "INVALID_VALUE_CWSIA0048",
                                                                new Object[] { "sharedSubscriptionName", name },
                                                                tc
                                );
            }
        }

        // Check that this is not a foreign implementation.
        // Note that in some situations this method call can return a _different_
        // destination than the one passed in as the parameter (e.g. Spring).
        JmsDestinationImpl nativeDest = JmsDestinationImpl.checkNativeInstance(destination);

        // Check that this destination is not blocked from creating consumers
        JmsDestinationImpl.checkBlockedStatus(nativeDest);

        // create the consumer properties object filling in as much information
        // as we have available at the moment.
        ConsumerProperties newProps = new ConsumerProperties(nativeDest,
                        null, // DestType                filled in later
                        messageSelector, // Selector
                        null, // Reliability             filled in later
                        false, // ReadAhead               filled in later
                        true, // RecovExpress            filled in later
                        noLocal, // NoLocal
                        name, // SubName  .. with JMS 2.0, the value is set for all cases
                        connection.getClientID(), // ClientID .. with JMS 2.0, the value is set for all cases
                        isSharedNonDurable, // SupportMultiple .. with JMS 2.0, the value is set for all cases         
                        null // DurableSubscriptionHome n/a for non-durable consumers
        );

        // Hold the state lock while the msg consumer constructor is called to prevent
        // it from reading invalid start/stopped state while start is in progress. This
        // will basically cause and createConsumer calls on this session to wait until
        // session.start is complete.
        synchronized (stateLock) {
            consumer = instantiateConsumer(newProps);
            // newly created consumers must be synchronous
            syncConsumers.add(consumer);

            // d353701 - output a warning message if there are 'lots' of producers active.
            int totalConsumers = syncConsumers.size() + asyncConsumers.size();
            if (totalConsumers % PROD_CONS_WARNING_THRESHOLD == 0) {
                // We wish to tell the user which line of their application created the session,
                // so we must obtain a line of stack trace from their application.
                String errorLocation = JmsErrorUtils.getFirstApplicationStackString();
                SibTr.warning(tc, "MANY_CONSUMERS_WARNING_CWSIA0059", new Object[] { "" + totalConsumers, errorLocation });
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumer", consumer);
        return consumer;
    }

    /**
     * @see javax.jms.Session#createQueue(String)
     */
    @Override
    public Queue createQueue(String queueName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createQueue", queueName);

        checkNotClosed();
        Queue queue = JmsFactoryFactory.getInstance().createQueue(queueName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createQueue", queue);
        return queue;
    }

    /**
     * @see javax.jms.Session#createTopic(String)
     */
    @Override
    public javax.jms.Topic createTopic(String topicName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTopic", topicName);

        checkNotClosed();
        Topic topic = JmsFactoryFactory.getInstance().createTopic(topicName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createTopic", topic);
        return topic;
    }

    /**
     * @see javax.jms.Session#createDurableSubscriber(Topic, String)
     */
    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableSubscriber", new Object[] { topic, name });

        checkNotClosed();
        TopicSubscriber durableSubscriber = createDurableSubscriber(topic, name, null, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createDurableSubscriber", durableSubscriber);
        return durableSubscriber;
    }

    /**
     * @see javax.jms.Session#createDurableSubscriber(Topic, String, String, boolean)
     */
    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableSubscriber", new Object[] { topic, name, messageSelector, noLocal });

        try {
            return createDurableSubscriber(topic, name, messageSelector, noLocal,
                                           false, //is JMS2.0 shared durable subscriber
                                           true //is JMS2.0 non-shared durable subscriber
            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createDurableSubscriber");
        }
    }

    //this function is called by both classic and traditional i.e
    // from createDurableSubscriber with selector and without selector
    // from createDurableConsumer with selector and without selector
    // from createSharedDurableConsumer with selector and without selector
    private TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal, boolean isSharedDurable, boolean isNonSharedDurable) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableSubscriber", new Object[] { topic, name, messageSelector, noLocal, isSharedDurable, isNonSharedDurable });

        checkNotClosed();
        if (topic == null) {
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "INVALID_VALUE_CWSIA0048",
                                                                           new Object[] { "Topic", "null" },
                                                                           tc
                            );
        }

        if ((name == null) || ("".equals(name))) {
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "INVALID_VALUE_CWSIA0048",
                                                                           new Object[] { "name", name },
                                                                           tc
                            );
        }

        // Check that this is not a foreign implementation.
        // Note that in some situations this method call can return a _different_
        // destination than the one passed in as the parameter (e.g. Spring).
        JmsDestinationImpl nativeDest = JmsDestinationImpl.checkNativeInstance(topic);

        // Check that this destination is not blocked
        JmsDestinationImpl.checkBlockedStatus(nativeDest);

        //client ID check should be there for only for non-shared. For shared client ID should be optional
        String clientID = connection.getClientID();
        if (isNonSharedDurable) {
            if ((clientID == null) || ("".equals(clientID))) {
                throw (IllegalStateException) JmsErrorUtils.newThrowable(IllegalStateException.class,
                                                                         "INVALID_VALUE_CWSIA0048",
                                                                         new Object[] { "clientID", clientID },
                                                                         tc
                                );
            }
        }

        // Look up the subscription home that was set on the ConnectionFactory
        // at the point when the Connection was created.
        String durSubHome = (String) passThruProps.get(JmsraConstants.DURABLE_SUB_HOME);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "DurableSubHome : " + durSubHome);

        // We demand that if durable subscriptions are being used then the user
        // must have specified a durable subscription home on the ConnectionFactory
        // before they create the connection.
        if ((durSubHome == null) || ("".equals(durSubHome))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "User Error - No durableSubscriptionHome was specified.");
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "DURABLE_SUB_HOME_NOT_SPECIFIED_CWSIA0056",
                                                                               null,
                                                                               tc
                            );
        }

        // Now determine whether or not we are running in a cloned environment.
        String sharedSubs = (String) passThruProps.get(JmsInternalConstants.SHARE_DSUBS);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "shareDurableSubs: " + sharedSubs);
        boolean supportsMultiple = false;
        if ((sharedSubs == null)
            || ("".equals(sharedSubs))
            || (ApiJmsConstants.SHARED_DSUBS_IN_CLUSTER.equals(sharedSubs))) {
            // The application asked us to work it out for ourselves (default)
            boolean isCloned = JmsConnectionImpl.isClonedServer();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "isClonedServer: " + isCloned);
            // Pass the cloned state in as the instruction to support
            // multiple consumers on this durable subscription.
            supportsMultiple = isCloned;
        }
        else if (ApiJmsConstants.SHARED_DSUBS_ALWAYS.equals(sharedSubs)) {
            // The application stated explicitly it wants to share.
            supportsMultiple = true;
        }
        else {
            // The safe behaviour is to not support multiple consumers.
            supportsMultiple = false;
        }

        //if the call is made from createSharedDurableConsumer, set supportsMultiple to true
        //otherwise leaving the property as it is
        if (isSharedDurable)
            supportsMultiple = true;

        //if the call is made from CreateSharedConsumer i.e non-shared durable then
        //set supportsMultiple to false, otherwise leaving the property as it is
        if (isNonSharedDurable)
            supportsMultiple = false;

        // Create the consumer properties object filling in as much information
        // as we have available at the moment.
        ConsumerProperties newProps = new ConsumerProperties(nativeDest,
                        null, // DestType                filled in later
                        messageSelector, // Selector
                        null, // Reliability             filled in later
                        false, // ReadAhead               filled in later
                        true, // RecovExpress            filled in later
                        noLocal, // NoLocal
                        name, // SubName
                        clientID, // ClientID
                        supportsMultiple,// SupportMultiple
                        durSubHome // DurableSubscriptionHome
        );

        TopicSubscriber durableSubscriber = new JmsDurableSubscriberImpl(coreConnection, this, newProps);
        // newly created consumer is synchronous
        syncConsumers.add(durableSubscriber);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createDurableSubscriber", durableSubscriber);
        return durableSubscriber;
    }

    /**
     * @see javax.jms.Session#createBrowser(Queue)
     */
    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowser", queue);

        checkNotClosed();
        QueueBrowser queueBrowser = createBrowser(queue, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createBrowser", queueBrowser);
        return queueBrowser;
    }

    /**
     * @see javax.jms.Session#createBrowser(Queue, String)
     */
    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowser", new Object[] { queue, messageSelector });

        checkNotClosed();
        if (queue == null) {
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "INVALID_VALUE_CWSIA0048",
                                                                           new Object[] { "Queue", "null" },
                                                                           tc
                            );
        }

        // Check that this is not a foreign implementation.
        // Note that in some situations this method call can return a _different_
        // destination than the one passed in as the parameter (e.g. Spring).
        JmsDestinationImpl jmsDestination = JmsDestinationImpl.checkNativeInstance(queue);
        // Check that the destination is not blocked
        JmsDestinationImpl.checkBlockedStatus(jmsDestination);

        QueueBrowser queueBrowser = new JmsQueueBrowserImpl(this, jmsDestination, messageSelector);
        browsers.add(queueBrowser);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createBrowser", queueBrowser);
        return queueBrowser;
    }

    /**
     * @see javax.jms.Session#createTemporaryQueue()
     */
    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryQueue");
        JmsTemporaryDestinationInternal temporaryQueue = null;

        checkNotClosed();

        synchronized (stateLock) {
            String prefix = (String) passThruProps.get(JmsraConstants.TEMP_QUEUE_NAME_PREFIX);
            SIDestinationAddress temporaryQueueAddr = createTemporaryDestination(Distribution.ONE, prefix);
            temporaryQueue = new JmsTemporaryQueueImpl(temporaryQueueAddr, this);
            connection.addTemporaryDestination(temporaryQueue);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createTemporaryQueue", temporaryQueue);
        return (TemporaryQueue) temporaryQueue;
    }

    /**
     * @see javax.jms.Session#createTemporaryTopic()
     */
    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryTopic");
        JmsTemporaryDestinationInternal temporaryTopic = null;

        checkNotClosed();

        synchronized (stateLock) {
            String prefix = (String) passThruProps.get(JmsraConstants.TEMP_TOPIC_NAME_PREFIX);
            SIDestinationAddress temporaryTopicAddr = createTemporaryDestination(Distribution.ALL, prefix);
            temporaryTopic = new JmsTemporaryTopicImpl(temporaryTopicAddr, this);
            connection.addTemporaryDestination(temporaryTopic);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createTemporaryTopic", temporaryTopic);
        return (TemporaryTopic) temporaryTopic;
    }

    /**
     * @see javax.jms.Session#unsubscribe(String)
     */
    @Override
    public void unsubscribe(String subName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unsubscribe", subName);

        checkNotClosed();

        // check for null and empty subscription names.
        if ((subName == null) || ("".equals(subName))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "unsubscribe", subName);
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "INVALID_VALUE_CWSIA0048",
                                                                           new Object[] { "name", subName },
                                                                           tc
                            );
        }

        // obtain the current client id.
        String clientID = connection.getClientID();

        // concatenate it with the subscription name to form the core subscription name
        String subscriptionName = JmsInternalsFactory.getSharedUtils().getCoreDurableSubName(clientID, subName);
        String durableSubHome = (String) passThruProps.get(JmsraConstants.DURABLE_SUB_HOME);
        if (JmsDurableSubscriberImpl.DEVT_DEBUG) {
            System.out.println("UNSUBSCRIBE : " + subscriptionName);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "subscriptionName: " + subscriptionName + "  durableSubHome: " + durableSubHome);

        // We demand that if durable subscriptions are being used then the user
        // must have specified a durable subscription home on the ConnectionFactory
        // before they create the connection.
        if ((durableSubHome == null) || ("".equals(durableSubHome))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "User Error - No durableSubscriptionHome was specified.");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "unsubscribe", subName);
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "DURABLE_SUB_HOME_NOT_SPECIFIED_CWSIA0056",
                                                                               null,
                                                                               tc
                            );
        }

        try {
            coreConnection.deleteDurableSubscription(subscriptionName, durableSubHome);
        } catch (SIConnectionUnavailableException sioce) {
            // No FFDC code needed
            // Method invoked on a closed connection
            // This could happen if connection.close was called on another thread during
            // this method invocation.
            // d238447 FFDC review. Since this can happen 'normally', don't generate FFDC.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "CONN_CLOSED_CWSIA0041",
                                                                               null,
                                                                               sioce,
                                                                               null, // null probeId = no FFDC.
                                                                               this,
                                                                               tc
                            );
        } catch (SIDestinationLockedException sidle) {
            // No FFDC code needed
            // Destination is locked - probably means that there
            // is an active subscriber for this durable subscription
            // d238447 FFDC review. Don't generate FFDC.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "DSUB_LOCKED_CWSIA0043",
                                                                               null,
                                                                               sidle,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                            );
        } catch (SIDurableSubscriptionNotFoundException sidnfe) {
            // No FFDC code needed
            // This durable subscription does not exist - the user is trying to
            // unsubscribe a durable subscription which they have not created
            // (combination of client id and subacription name has not been used
            // before to create a durable subscription).
            // d238447 FFDC review. Don't generate FFDC.
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "DURABLE_SUB_DOES_NOT_EXIST_CWSIA0054",
                                                                           new Object[] { subscriptionName },
                                                                           sidnfe,
                                                                           null, // null probeId = no FFDC
                                                                           this,
                                                                           tc
                            );
        } catch (SINotAuthorizedException sinae) {
            // No FFDC code needed
            // d238447 FFDC review. NotAuth is an app/config error. Don't generate FFDC.
            throw (JMSSecurityException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                                    "NOT_AUTH_CWSIA0044",
                                                                    null,
                                                                    sinae,
                                                                    null, // null probeId = no FFDC
                                                                    this,
                                                                    tc
                            );
        } catch (SIException sice) {
            // No FFDC code needed
            // Misc other exception (Store, Comms, Core).

            // d222942 review - ok
            // d238447 FFDC review. Need to consider:
            //   SIConnectionUnavailableException, SIConnectionDroppedException,      \
            //   SIDestinationLockedException, SIDurableSubscriptionNotFoundException  > Handled above
            //   SINotAuthorizedException,                                            /
            // SIResourceException, SIConnectionLostException, \ Generate FFDCs for these.
            // SIIncorrectCallException,                       /
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { sice, "JmsSessionImpl.unsubscribe (#5)" },
                                                            sice,
                                                            "JmsSessionImpl.unsubscribe#5",
                                                            this,
                                                            tc
                            );
        }
        finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "unsubscribe", subName);
        }
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * Indicates if the session is managed.<p>
     * (i.e. running in the application server).
     */
    boolean isManaged() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isManaged");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isManaged", isManaged);
        return isManaged;
    }

    /**
     * Get the state of the session (stopped, started or closed).
     */
    int getState() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getState");
        int currentState;

        synchronized (stateLock) {
            currentState = state;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getState", currentState);
        return currentState;
    }

    /**
     * Sets the state.
     * 
     * @param state The new state to set
     */
    private void setState(int newState) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setState", newState);
        int oldState = -1;

        if ((newState == CLOSED) || (newState == STARTED) || (newState == STOPPED)) {
            synchronized (stateLock) {
                oldState = state;
                state = newState;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setState", oldState);
    }

    /**
     * This method is called at the beginning of every method that should not
     * work if the Session has been closed. It prevents further execution by
     * throwing a JMSException with a suitable "I'm closed" message.
     */
    void checkNotClosed() throws JMSException {
        int currentState;
        synchronized (stateLock) {
            currentState = state;
        }
        if (currentState == CLOSED) {
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "SESSION_CLOSED_CWSIA0049",
                                                                               null,
                                                                               tc
                            );
        }
    }

    /**
     * Return the passThruProps for this Session.
     */
    Map getPassThruProps() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPassThruProps");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPassThruProps", passThruProps);
        return passThruProps;
    }

    /**
     * Returns the asyncDeliveryLock for this Session.
     */
    Object getAsyncDeliveryLock() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAsyncDeliveryLock");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getAsyncDeliveryLock", asyncDeliveryLock);
        return asyncDeliveryLock;
    }

    /**
     * This method is used by the JmsMsgConsumerImpl.Consumer constructor to
     * obtain a reference to the exception listener target.
     * 
     * Note that this method should _not_ be used to pass properties through from
     * Connection to Producer or Consumer objects - instead use the
     * getPassThruProps method.
     */
    Connection getConnection() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnection");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConnection", connection);
        return connection;
    }

    /**
     * Returns the coreConnection for this Connection
     */
    SICoreConnection getCoreConnection() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCoreConnection");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCoreConnection", coreConnection);
        return coreConnection;
    }

    /**
     * This method is overriden by subclasses in order to instantiate an instance
     * of the MsgProducer class. This means that the QueueSession.createSender
     * method can delegate straight to Session.createProducer, and still get back
     * an instance of a QueueSender, rather than a vanilla MessageProducer.
     * 
     * Note, since this method is over-ridden by Queue and Topic specific classes,
     * updates to any one of these methods will require consideration of the
     * other two versions.
     */
    JmsMsgProducer instantiateProducer(Destination jmsDestination) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateProducer", jmsDestination);
        JmsMsgProducer messageProducer = new JmsMsgProducerImpl(jmsDestination, coreConnection, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateProducer", messageProducer);
        return messageProducer;
    }

    /**
     * This package method allows producers and consumers to obtain the
     * appropriate ordering context.<p>
     * 
     * @return the orderingContext for this session
     */
    OrderingContext getOrderingContext() {
        return orderingContext;
    }

    /**
     * This method is overriden by subclasses in order to instantiate an instance
     * of the MsgConsumer class. This means that the QueueSession.createReceiver
     * method can delegate straight to Session.createConsumer, and still get back
     * an instance of a QueueReceiver, rather than a vanilla MessageConsumer.
     * 
     * Note, since this method is over-ridden by Queue and Topic specific classes,
     * updates to any one of these methods will require consideration of the
     * other two versions.
     */
    JmsMsgConsumer instantiateConsumer(ConsumerProperties consumerProperties) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateConsumer", consumerProperties);
        JmsMsgConsumer messageConsumer = new JmsMsgConsumerImpl(coreConnection, this, consumerProperties);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateConsumer", messageConsumer);
        return messageConsumer;
    }

    /**
     * Start (consumers).
     * See the comment inside the method below for details of how we deal with
     * an exception being thrown by one of the consumers.
     */
    void start() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "start");

        synchronized (stateLock) {

            // We will start each consumer inside its own catch block so that
            // the failing of a single consumer does not adversely affect any
            // other. The first exception that is received is stored in this
            // variable and will be passed back up to the Connection when all
            // the starts are complete.
            JMSException excReceived = null;

            // Start all the synchronous consumers

            // Take a copy of the list of synchronous consumers. A sync block in
            // createConsumer means that you cannot add anything to this list until
            // session.start completes, however consumers can still be removed (as
            // a result of the consumer.close operation), so by the time we get to
            // calling start on the elements in this list, some of them may already
            // be closed (pathalogical scenario). Calling start on a closed consumer
            // has no effect, so this shouldn't cause a problem.
            Object[] syncList = syncConsumers.toArray();

            for (int i = 0; i < syncList.length; i++) {
                try {
                    ((JmsMsgConsumerImpl) syncList[i]).start();
                } catch (JMSException e) {
                    // No FFDC code needed
                    // Cache the exception to be thrown back at the end of the method,
                    // but don't take any other action here. The exception will already
                    // have been logged in JmsMsgConsumerImpl.start.
                    if (excReceived == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "JMSException received at Session.start. Will continue trying to start the remaining consumers.");
                        // Cache this exception for later use.
                        excReceived = e;
                    }
                }
            }

            // Start all the asynchronous consumers.
            Iterator asyncList = asyncConsumers.iterator();
            while (asyncList.hasNext()) {
                try {
                    ((JmsMsgConsumerImpl) asyncList.next()).start();
                } catch (JMSException e) {
                    // No FFDC code needed

                    // Cache the exception to be thrown back at the end of the method,
                    // but don't take any other action here. The exception will already
                    // have been logged in JmsMsgConsumerImpl.start.
                    if (excReceived == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "JMSException received at Session.start. Will continue trying to start the remaining asynch consumers.");
                        // Cache this exception for later use.
                        excReceived = e;
                    }
                }
            }

            // Check whether an exception was thrown at any point during this method.
            if (excReceived != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "An exception was received during Session.start. Propogate to Connection.");
                throw excReceived;
            }

            // Flag that we have started everything.
            setState(STARTED);

        } // Release the lock

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "start");
    }

    /**
     * Stop (consumers).
     * 
     * Here be DRAGONS
     * We need to iterate over the list of async consumers and call stop on each one.
     * If a consumer is currently running, then the core consumer session stop call will
     * block until it completes.
     * However, the onMessage code may make calls which require access to the asyncConsumers
     * List object, so if we hold the lock whilst we block, the system will dead lock.
     * It would be possible for the calls made from onMessage to modify the asyncConsumers List,
     * so it is not safe for us not to synchronize access to it here.
     * We will need to copy the List before iterating.
     * Risk: if the list is modified whilst we are iterating, then we will have called stop on
     * a non-current set of consumers.
     * Items can only be added to the list by registerAsyncConsumer, called from
     * MessageConsumer.setMessageListener. This would result in the consumer moving from the
     * syncConsumers list to the asyncConsumers list. This should be ok, because we have already
     * stop'd the syncConsumers.
     * Items can be removed from the list by registerSyncConsumer and close. Close should be ok
     * because the consumer is destined for the garbage, and an extra stop call won't hurt.
     * registerSyncConsumer results from MessageConsumer.setMessageListener(null). It won't matter
     * if it comes after we take a copy of the asyncConsumers list, but it's not nice if it comes
     * after we release the syncConsumers lock and before we copy asyncConsumers, as we then get
     * a consumer moved to the syncConsumers list which is potentially in the wrong state. To
     * defend against this, I've introduced a new lock to cover actions on both consumer lists.
     * 
     * This method used to hold the stateLock for most of its duration.
     * Session.close blocks in this method whilst onMessage is
     * running (due to core behaviour when calling consumerSession.stop).
     * This is useful behaviour.
     * If the state lock is held for the duration, then most other methods
     * such as commit/recover etc will dead lock when called from onMessage,
     * as they need to aquire the stateLock for the checkClosed method.
     */
    void stop() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stop");
        Object[] cons;

        //for non-managed Message Listener onMessage() should not call on stop on its own context/connection.
        if (!isManaged) {
            validateStopCloseForMessageListener("stop");
        }

        setState(STOPPED);

        // We will start each consumer inside its own catch block so that
        // the failing of a single consumer does not adversely affect any
        // other. The first exception that is received is stored in this
        // variable and will be passed back up to the Connection when all
        // the starts are complete.
        JMSException excReceived = null;

        // Aquire consumerListsLock to ensure that sync and async lists remain in step
        // *** READ THE JAVADOC ABOVE BEFORE CHANGING THESE SYNC BLOCKS ***
        synchronized (consumerListsLock) {
            synchronized (syncConsumers) {

                Iterator i = syncConsumers.iterator();
                while (i.hasNext()) {
                    try {
                        ((JmsMsgConsumerImpl) i.next()).stop();
                    } catch (JMSException e) {
                        // No FFDC code needed
                        // Cache the exception to be thrown back at the end of the method,
                        // but don't take any other action here. The exception will already
                        // have been logged in JmsMsgConsumerImpl.stop.
                        if (excReceived == null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "JMSException received at Session.stop. Will continue trying to stop the remaining consumers.");
                            // Cache this exception for later use.
                            excReceived = e;
                        }
                    }
                }

            } // release syncConsumers lock

            // toArray gives us a copy of the list
            cons = asyncConsumers.toArray();

        } // release consumerListsLock here, before we risk blocking in individual stop calls

        // now iterate over copy of async list
        for (int i = 0; i < cons.length; i++) {
            try {
                ((JmsMsgConsumerImpl) cons[i]).stop(); // may block in the stop call if onMessage is running
            } catch (JMSException e) {
                // No FFDC code needed
                // Cache the exception to be thrown back at the end of the method,
                // but don't take any other action here. The exception will already
                // have been logged in JmsMsgConsumerImpl.stop.
                if (excReceived == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "JMSException received at Session.stop. Will continue trying to stop the remaining async consumers.");
                    // Cache this exception for later use.
                    excReceived = e;
                }
            }
        }

        // Check whether an exception was thrown at any point during this method.
        if (excReceived != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "An exception was received during Session.stop. Propogate to Connection.");
            throw excReceived;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "stop");
    }

    /**
     * Return a count of the number of Producers currently open on this Session.
     */
    public int getProducerCount() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProducerCount");
        int producerCount = producers.size();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProducerCount", producerCount);
        return producerCount;
    }

    /**
     * Return a count of the number of Consumers currently open on this Session.
     */
    public int getConsumerCount() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerCount");
        int consumerCount = syncConsumers.size();
        consumerCount += asyncConsumers.size();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerCount", consumerCount);
        return consumerCount;
    }

    /**
     * This method is called by a JmsMsgProducer in order to remove itself from
     * the list of Producers held by the Session.
     * 
     * @param JmsMsgProducer The Producer which is calling this method.
     */
    void removeProducer(JmsMsgProducerImpl producer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeProducer", producer);
        producers.remove(producer);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeProducer");
    }

    /**
     * This method is called by a JmsMsgConsumer in order to remove itself from
     * the list of consumers held by the Session.
     * 
     * @param JmsMsgConsumer The Consumer which is calling the method.
     */
    void removeConsumer(JmsMsgConsumerImpl consumer) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeConsumer", consumer);

        // if DUPS_OK_ACKNOWLEDGE is in use, commit any messages delivered in the
        // last incomplete batch (if the session is closing it will already have
        // done this and the count of uncommitted messages will be zero)
        if ((acknowledgeMode == Session.DUPS_OK_ACKNOWLEDGE) && (uncommittedReceiveCount > 0)) {
            commitTransaction();
        }
        syncConsumers.remove(consumer);
        asyncConsumers.remove(consumer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeConsumer");
    }

    /**
     * This method is called by a JmsQueueBrowser in order to remove itself from
     * the list of Browsers held by the Session.
     * 
     * @param JmsQueueBrowser The Browser which is calling this method.
     */
    void removeBrowser(JmsQueueBrowserImpl browser) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeBrowser", browser);
        browsers.remove(browser);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeBrowser");
    }

    /**
     * This method should be called when a message has been received by JMS
     * consumer code, but not yet delivered to user code.
     * 
     * This occurs when a message is obtained using the core api before calling
     * an asynchronous consumer's onMessage() method.
     * 
     * If the message is handled under a local transaction (transacted session,
     * client ack mode, dups ok ack mode, but not auto ack mode or globally
     * transacted (XA) sessions), the count of uncommitted received messages is
     * incremented.
     * 
     * This count is required by a dups ok session so that it can commit every
     * n messages. It is not required by any other type of session, but may be
     * useful in debug scenarios. Global XA transactions do not drive commits
     * through JMS code, and auto ack mode sessions do not use transactions, so
     * the count is not set for these types of session.
     * 
     * Note that the session mode is overriden if an XA transaction is
     * configured, i.e. session mode may be set to transacted, or auto ack, but
     * an XA transaction may be used anyway. The acknowledgeMode cannot be used
     * to distinguish between these states, so the transaction in use is passed
     * to this method.
     * 
     * @param transaction
     *            the transaction under which the message will be consumed (null if no
     *            transaction is in use).
     * 
     * @see #notifyMessagePostConsume()
     * @see #notifyMessageConsumed(SITransaction)
     */
    void notifyMessagePreConsume(SITransaction transaction) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "notifyMessagePreConsume", transaction);

        if (!(transaction instanceof SIXAResource)) {
            // We wish to increment this count even under auto_ack, since we use
            // it to determine whether the onMessage calls recover.
            uncommittedReceiveCount++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && (uncommittedReceiveCount % 100 == 0)) {
                SibTr.debug(this, tc, "session " + this + " uncommittedReceiveCount : " + uncommittedReceiveCount);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "notifyMessagePreConsume");
    }

    /**
     * This method should be called when a previously received message has been
     * delivered by JMS consumer code to user code.
     * 
     * This occurs on return from an asynchronous consumer's onMessage() method.
     * 
     * @see #notifyMessagePreConsume(SITransaction)
     * @see #notifyMessageConsumed(SITransaction)
     */
    void notifyMessagePostConsume() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "notifyMessagePostConsume");
        if ((acknowledgeMode == Session.DUPS_OK_ACKNOWLEDGE) && (uncommittedReceiveCount >= dupsCommitThreshold)) {
            commitTransaction();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "notifyMessagePostConsume");
    }

    /**
     * This method should be called when a message is received by JMS consumer
     * code, and is just about to be delivered to user code (and there is nowhere
     * to insert post delivery processing).
     * 
     * This occurs when returning a message to a synchronous consumer from a
     * receive method.
     * 
     * @param transaction
     *            the transaction under which the message will be consumed (null if no
     *            transaction is in use).
     * 
     * @see #notifyMessagePreConsume(SITransaction)
     * @see #notifyMessagePostConsume()
     */
    void notifyMessageConsumed(SITransaction transaction) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "notifyMessageConsumed", transaction);
        notifyMessagePreConsume(transaction);
        notifyMessagePostConsume();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "notifyMessageConsumed");
    }

    /**
     * Return the transaction that is currently in effect for this session.
     * An auto-ack mode session has no transaction, and so returns null. In a
     * managed environment (i.e. in WAS) the session may be locally transacted,
     * or in auto-ack mode. It may also be configured to run under an XA global
     * transaction, in which case the local transaction flag and ack mode flag
     * are ignored. This means that an attempt to obtain a transaction from the
     * jcaSession must always be made, as it knows the true transaction mode.
     * 
     * @return current transaction object for this session (or null if one
     *         doesn't exist).
     */
    SITransaction getTransaction() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTransaction");
        SITransaction transaction = null;

        try {
            transaction = jcaSession.getCurrentTransaction();
        } catch (javax.resource.spi.IllegalStateException ise) {
            // No FFDC code needed
            // d238447 FFDC review. Possible result of a race condition. No FFDC.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "EXCEPTION_RECEIVED_CWSIA0053",
                                                                               new Object[] { ise, "JmsSessionImpl.getTransaction (#1)" },
                                                                               ise,
                                                                               null, // null probeId = no FFDC.
                                                                               this,
                                                                               tc
                            );
        } catch (Exception e) {
            // No FFDC code needed
            // d238447 FFDC review. Need to consider:
            //   IllegalStateException - handled above.
            //   ResourceException, SIException, SIErrorException - generate FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { e, "JmsSessionImpl.getTransaction (#2)" },
                                                            e,
                                                            "JmsSessionImpl.getTransaction#2",
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTransaction", transaction);
        return transaction;
    }

    /**
     * Commit the transaction that is currently in effect for this session.
     * Note, if there is no transaction (e.g. in auto ack mode), an exception
     * will be thrown.
     */
    void commitTransaction() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitTransaction");

        try {
            jcaSession.commitLocalTransaction();
            uncommittedReceiveCount = 0;
        } catch (javax.resource.spi.IllegalStateException ise) {
            // No FFDC code needed
            // d238447 FFDC review. Generate FFDC
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "EXCEPTION_RECEIVED_CWSIA0053",
                                                                               new Object[] { ise, "JmsSessionImpl.commitTransaction (#1)" },
                                                                               ise,
                                                                               "JmsSessionImpl.commitTransaction#1",
                                                                               this,
                                                                               tc
                            );
        } catch (javax.resource.spi.LocalTransactionException lte) {
            // No FFDC code needed
            // d238447 FFDC Review. LocalT thrown if no transaction in progress. Wouldn't expect
            //   us to call commit if this was the case, so generate FFDC.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "EXCEPTION_RECEIVED_CWSIA0053",
                                                                               new Object[] { lte, "JmsSessionImpl.commitTransaction (#2)" },
                                                                               lte,
                                                                               "JmsSessionImpl.commitTransaction#2",
                                                                               this,
                                                                               tc
                            );
        } catch (SIConnectionDroppedException e) { //PK60857  connection no longer available so tran will timeout and rollback on the server side
            // No FFDC code needed
            // Generate FFDC.
            rolledBackDueToConnectionFailure = true;
            throw (TransactionRolledBackException) JmsErrorUtils.newThrowable(
                                                                              TransactionRolledBackException.class, // subclass of JMSException
                                                                              "EXCEPTION_RECEIVED_CWSIA0053",
                                                                              new Object[] { e, "JmsSessionImpl.commitTransaction (#4)" },
                                                                              e,
                                                                              "JmsSessionImpl.commitTransaction#4",
                                                                              this,
                                                                              tc
                            );

        } catch (SIConnectionLostException e) { //PK60857  connection failed during the commit, result unknown.  This is a risk of 1PC, assume rollback.
            // No FFDC code needed
            // Generate FFDC.
            rolledBackDueToConnectionFailure = true;
            throw (TransactionRolledBackException) JmsErrorUtils.newThrowable(
                                                                              TransactionRolledBackException.class, // subclass of JMSException
                                                                              "EXCEPTION_RECEIVED_CWSIA0053",
                                                                              new Object[] { e, "JmsSessionImpl.commitTransaction (#5)" },
                                                                              e,
                                                                              "JmsSessionImpl.commitTransaction#5",
                                                                              this,
                                                                              tc
                            );
        }

        catch (Exception e) {
            // No FFDC code needed
            // d238447 FFDC review. Includes ResourceException, SIException, SIErrorException
            // Generate FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { e, "JmsSessionImpl.commitTransaction (#3)" },
                                                            e,
                                                            "JmsSessionImpl.commitTransaction#3",
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitTransaction");
    }

    /**
     * Rollback the transaction that is currently in effect for this session.
     * Note, if there is no transaction (e.g. in auto ack mode), an exception
     * will be thrown.
     */
    void rollbackTransaction() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "rollbackTransaction");

        try {
            if (rolledBackDueToConnectionFailure) { //PK60857
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "rolledBackDueToConnectionFailure = true");
                }
                // No-op'ing the rollback because it has already happened during the commit.
                // This means rollback should never have been called, but some components insist on calling rollback after getting a TransactionRolledBackException.
                return; // exit trace handled by the finally block
            }

            jcaSession.rollbackLocalTransaction();
            uncommittedReceiveCount = 0;
        } catch (javax.resource.spi.IllegalStateException ise) {
            // No FFDC code needed
            // d238447 FFDC review. IllegalState is thrown if the jcaSession is closed, which
            //   might occur in a badly coded app due to concurrent race. No FFDC.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "EXCEPTION_RECEIVED_CWSIA0053",
                                                                               new Object[] { ise, "JmsSessionImpl.rollbackTransaction (#1)" },
                                                                               ise,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                            );
        } catch (javax.resource.spi.LocalTransactionException lte) {
            // No FFDC code needed
            // d222942 review - ok
            // d238447 FFDC review. LTE thrown when no transaction is in progress. JMS layer shouldn't
            //   get this, so generate FFDC to indicate something has gone wrong.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "EXCEPTION_RECEIVED_CWSIA0053",
                                                                               new Object[] { lte, "JmsSessionImpl.rollbackTransaction (#2)" },
                                                                               lte,
                                                                               "JmsSessionImpl.rollbackTransaction#2",
                                                                               this,
                                                                               tc
                            );
        } catch (Exception e) {
            // No FFDC code needed
            // d238447 FFDC review - generate FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { e, "JmsSessionImpl.rollbackTransaction (#3)" },
                                                            e,
                                                            "JmsSessionImpl.rollbackTransaction#3",
                                                            this,
                                                            tc
                            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "commitTransaction");
        }
    }

    /**
     * This method exists for the sole purpose of querying whether an application
     * onMessage called recover under an auto_ack session. It returns the current
     * value of the commit count, and then zeros the value.
     */
    int getAndResetCommitCount() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAndResetCommitCount");
        int currentUncommittedReceiveCount = uncommittedReceiveCount;
        uncommittedReceiveCount = 0;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getAndResetCommitCount", currentUncommittedReceiveCount);
        return currentUncommittedReceiveCount;
    }

    /**
     * Creates a new messageID.
     * Use the stem (supplied by core) and add a session unique counter to
     * the end. Return as a byte[] representing '<hex>' format. (note it
     * does not include the 'ID:' part.<br>
     * 
     * Note that as part of 188046.1 we can set the messageID onto the message
     * directly as a byte array so we can avoid converting the messageID to
     * a string and back again.
     * 
     * Following 189874 we know that MFP will take a copy of the messageID byte[]
     * so we would like not to have to take one here. A problem would occur if
     * a second thread called this method between the time it returned the ID
     * and the time MFP took a copy of it. We know this cannot arise because the
     * session locking guarantees that you cannot send two messages simaltaneously.
     * (the odds of it happening if this were not the case are extremely small!)
     * 
     * @return a new messageID
     */
    byte[] createMessageID() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createMessageID");

        // Note there is no need to take a lock around this update piece of
        // code because the synchronisation code guarantees we will be single
        // threaded through this method.

        // Increment the least significant 8 bytes by 1 to provide the counter
        // index.
        int currentOffset = currentMessageID.length;
        int counterEndPosition = currentMessageID.length - 8;

        do {
            // Look at the next position to the left (the first time around this brings
            // the offset inside the width of the array).
            currentOffset--;
            // Increment the current position.
            currentMessageID[currentOffset]++;
            // If the increment caused a rollover then we want to move on to the
            // next position, unless we have already reached the end of the long.
        } while ((currentMessageID[currentOffset] == 0) && (currentOffset > counterEndPosition));

        // The next instructions that happen on this thread will be MFP copying the byte[].
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createMessageID", currentMessageID);
        return currentMessageID;
    }

    /**
     * Handles the call to core connection in which the temporary destination
     * gets created.
     * 
     * @param name
     *            the model destination name on the core connection
     * @param prefix The destination prefix
     * 
     * @return
     *         the string returned from the core connection
     */
    private SIDestinationAddress createTemporaryDestination(Distribution destType, String prefix) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryDestination", new Object[] { destType, prefix });
        SIDestinationAddress da = null;

        try {
            da = getCoreConnection().createTemporaryDestination(destType, prefix);
        } catch (SIInvalidDestinationPrefixException siidpe) {
            // No FFDC code needed
            // d238447 FFDC review. App error, no FFDC required.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INVALID_VALUE_CWSIA0048",
                                                            new Object[] { "TempDestPrefix", prefix },
                                                            siidpe,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        } catch (SIConnectionLostException sice) {
            // No FFDC code needed
            // d238447 FFDC review. ConnectionLost is external and probably already FFDCd.
            // Don't generate FFDC at this level.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { sice, "JmsSessionImpl.createTemporaryDestination (#2)" },
                                                            sice,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        } catch (SINotAuthorizedException sinae) {
            // No FFDC code needed
            String userID = "<unknown>";
            try {
                userID = coreConnection.getResolvedUserid();
            } catch (SIException e) {
                // No FFDC code needed
                // things are obviously not good, better to proceed with the original
                // error message than to start complaining about this new problem, but
                // we can add a debug output
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "failed to get resovledUserId: " + e);
            }
            // d238447 FFDC Review. NotAuth is app/config. Don't FFDC.
            throw (JMSSecurityException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                                    "AUTHORIZATION_FAILED_CWSIA0057",
                                                                    new Object[] { userID },
                                                                    sinae,
                                                                    null, // null probeId = no FFDC
                                                                    this,
                                                                    tc
                            );
        } catch (SIConnectionUnavailableException sioce) {
            // No FFDC code needed
            // d238447 FFDC review. App error? The createTemp methods in this class call checkClosed,
            //   so it could be argued that this is an internal error. However, we don't defend
            //   concurrent close during the createTemp calls, so you probably could get to here
            //   with a badly coded app'. No FFDC required.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "CONNECTION_CLOSED_CWSIA0051",
                                                                               null,
                                                                               sioce,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                            );
        } catch (SIException sice) {
            // No FFDC code needed
            // d238447 FFDC review. Exceptions to be considered:
            //   SIInvalidDestinationPrefixException                               \
            //   SIConnectionUnavailableException, SIConnectionDroppedException,    > Handled above
            //   SINotAuthorizedException, SIConnectionLostException               /
            // SIResourceException, -- ? anyone's guess, but probably already FFDC'd in lower level.
            // SILimitExceededException -- architected limit, no FFDC
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { sice, "JmsSessionImpl.createTemporaryDestination (#5)" },
                                                            sice,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        } catch (Exception e) {
            // No FFDC code needed
            // d238447 FFDC review. Generate FFDC
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { e, "JmsSessionImpl.createTemporaryDestination (#6)" },
                                                            e,
                                                            "JmsSessionImpl.createTemporaryDestination#6",
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createTemporaryDestination", da);
        return da;
    }

    /**
     * Handles the call to CoreConnection to delete a temporary destination.
     * 
     * @param name
     *            the name of the temporary destination to be deleted.
     * 
     */
    protected void deleteTemporaryDestination(SIDestinationAddress dest) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deleteTemporaryDestination", dest);

        try {
            getCoreConnection().deleteTemporaryDestination(dest);
        } catch (SITemporaryDestinationNotFoundException e) {
            // No FFDC code needed
            // d238447 FFDC Review. In theory the tempQ/tempT delete methods defend against multiple
            //   delete calls, but I suspect you could get here via a race, or even after session.close.
            //   Treat this as an app error - no FFDC.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "DESTINATION_DOES_NOT_EXIST_CWSIA0052",
                                                                               new Object[] { dest.getDestinationName() },
                                                                               e,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                            );
        } catch (SIDestinationLockedException sidle) {
            // No FFDC code needed
            // d238447 FFDC review. DestLocked is the normal exception if the temp dest still has
            //   consumers attached. No FFDC required.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "DEST_LOCKED_CWSIA0058",
                                                                               null,
                                                                               sidle,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                            );
        } catch (SINotAuthorizedException sinae) {
            // No FFDC code needed
            String userID = "<unknown>";
            try {
                userID = coreConnection.getResolvedUserid();
            } catch (SIException e) {
                // No FFDC code needed
                // things are obviously not good, better to proceed with the original
                // error message than to start complaining about this new problem, but
                // we can add a debug output
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "failed to get resovledUserId: " + e);
            }
            // d238447 FFDC review. NotAuth is app/config error - no FFDC.
            throw (JMSSecurityException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                                    "AUTHORIZATION_FAILED_CWSIA0057",
                                                                    new Object[] { userID },
                                                                    sinae,
                                                                    null, // null probeId = no FFDC.
                                                                    this,
                                                                    tc
                            );
        } catch (SIConnectionLostException sice) {
            // No FFDC code needed
            // d238447 FFDC review. External/already FFDC'd. Don't FFDC at this level
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { sice, "JmsSessionImpl.deleteTemporaryDestination (#3)" },
                                                            sice,
                                                            null, // null probeId = no FFDC.
                                                            this,
                                                            tc
                            );
        } catch (SIConnectionUnavailableException sioce) {
            // No FFDC code needed
            // d238447 FFDC review. External/already FFDC'd. Don't FFDC at this level
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "CONNECTION_CLOSED_CWSIA0051",
                                                                               null,
                                                                               sioce,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                            );
        } catch (SIException sice) {
            // No FFDC code needed
            // d238447 FFDC review. Need to consider:
            //   SIDestinationLockedException, SITemporaryDestinationNotFoundException, \
            //   SINotAuthorizedException, SIConnectionLostException                     > Handled above
            //   SIConnectionUnavailableException, SIConnectionDroppedException         /
            // SIResourceException, SIIncorrectCallException - no info on when these are thrown, generate an FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { sice, "JmsSessionImpl.deleteTemporaryDestination (#6)" },
                                                            sice,
                                                            "JmsSessionImpl.deleteTemporaryDestination#6",
                                                            this,
                                                            tc
                            );
        } catch (Exception e) {
            // No FFDC code needed
            // d238447 FFDC review. Generate FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0053",
                                                            new Object[] { e, "JmsSessionImpl.deleteTemporaryDestination (#7)" },
                                                            e,
                                                            "JmsSessionImpl.deleteTemporaryDestination#7",
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deleteTemporaryDestination");
    }

    /**
     * Test to see if any of the consumers of this session are using
     * async message delivery.
     * 
     * @return true if the session is being used for async delivery
     */
    boolean isAsync() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isAsync");
        boolean isAsync = !asyncConsumers.isEmpty() && getState() == STARTED;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isAsync", isAsync);
        return isAsync;
    }

    /**
     * Utility method to generate JMSExceptions if a synchronous method is
     * called on an asynchronous session.
     * This method should be called at the beginning of every method that should not
     * work if the owning session is being used for asynchronous receipt.
     * 
     * @param methodName the name of the calling method, to be inserted in the nls error string.
     */
    void checkSynchronousUsage(String methodName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "checkSynchronousUsage", methodName);
        // Generate an exception if the session is in async mode, and the call
        // has not originated from within onMessage
        if (isAsync() && !Thread.holdsLock(asyncDeliveryLock)) {
            throw (JMSException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                            "ASYNC_IN_PROGRESS_CWSIA0082",
                                                            new Object[] { methodName },
                                                            tc
                            );
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "checkSynchronousUsage");
    }

    /**
     * Place the specified consumer in the synchronous list
     * This will be called from the MessageConsumer.setMessageListener()
     * method when the application specifies a null message listener.
     * This method will tolerate multiple calls specifying the same consumer.
     * 
     * @param consumer The MessageConsumer instance that asserts it is
     *            synchronous.
     */
    void registerSyncConsumer(MessageConsumer consumer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "registerSyncConsumer", consumer);

        // ensure that movement between lists is atomic
        synchronized (consumerListsLock) {
            // remove the consumer from the async list
            asyncConsumers.remove(consumer);
            // add the consumer to the sync list if it's not
            // already present
            if (!syncConsumers.contains(consumer)) {
                syncConsumers.add(consumer);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "registerSyncConsumer");
    }

    /**
     * Place the specified consumer in the asynchronous list
     * This will be called from the MessageConsumer.setMessageListener()
     * method when the application specifies a non-null message listener.
     * This method will tolerate multiple calls specifying the same
     * consumer.
     * 
     * @param consumer The MessageConsumer instance that asserts it is
     *            asynchronous.
     */
    void registerAsyncConsumer(MessageConsumer consumer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "registerAsyncConsumer", consumer);

        // ensure that movement between lists is atomic
        synchronized (consumerListsLock) {
            // remove the consumer from the sync list
            syncConsumers.remove(consumer);
            // add the consumer to the async list if it's not
            // already present
            if (!asyncConsumers.contains(consumer)) {
                asyncConsumers.add(consumer);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "registerAsyncConsumer");
    }

    /**
     * Accessor method for sessionSyncLock.
     * 
     * @return sessionSyncLock
     */
    Object getSessionSyncLock() {
        return sessionSyncLock;
    }

    /**
     * Use to propagate session/connection properties to a JMS message.
     * 
     * @param msg
     */
    private void setMessageProperties(JmsMessageImpl msg) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMessageProperties", msg.getClass() + "@" + System.identityHashCode(msg));

        // Get properties from the pass through properties
        String prodProp = (String) passThruProps.get(JmsraConstants.PRODUCER_DOES_NOT_MODIFY_PAYLOAD_AFTER_SET);
        String consProp = (String) passThruProps.get(JmsraConstants.CONSUMER_DOES_NOT_MODIFY_PAYLOAD_AFTER_GET);

        // Check property values & assign message attributes accordingly
        msg.producerWontModifyPayloadAfterSet = prodProp.equalsIgnoreCase(ApiJmsConstants.WILL_NOT_MODIFY_PAYLOAD);
        msg.consumerWontModifyPayloadAfterGet = consProp.equalsIgnoreCase(ApiJmsConstants.WILL_NOT_MODIFY_PAYLOAD);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMessageProperties");
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createDurableConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name) throws InvalidDestinationException, IllegalStateException, JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableConsumer", new Object[] { topic, name });
        MessageConsumer messageConsumer = null;
        try {
            messageConsumer = createDurableSubscriber(
                                                      topic, name,
                                                      null, //messageSelctor
                                                      false, //nLocal
                                                      false, //is JMS2.0 shared durable subscriber
                                                      true //is JMs2.0 non-shared durable subscriber
            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createDurableConsumer", messageConsumer);
        }
        return messageConsumer;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createDurableConsumer(javax.jms.Topic, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) throws InvalidDestinationException, InvalidSelectorException, IllegalStateException, JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableConsumer", new Object[] { topic, name, messageSelector, noLocal });
        MessageConsumer messageConsumer = null;
        try {
            messageConsumer = createDurableSubscriber(topic, name, messageSelector, noLocal,
                                                      false, //is JMS2.0 shared durable subscriber
                                                      true //is JMs2.0 non-shared durable subscriber
            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createDurableConsumer", messageConsumer);
        }
        return messageConsumer;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSException, InvalidDestinationException, InvalidSelectorException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedConsumer", new Object[] { topic, sharedSubscriptionName });
        MessageConsumer messageConsumer = null;
        try {
            messageConsumer = createConsumer(
                                             topic,
                                             sharedSubscriptionName,
                                             null, //messageSelector
                                             false, //nLocal
                                             true //is JMS2.0 shared non durable subscriber
            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedConsumer", messageConsumer);
        }
        return messageConsumer;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedConsumer(javax.jms.Topic, java.lang.String, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException, InvalidDestinationException, InvalidSelectorException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedConsumer", new Object[] { topic, sharedSubscriptionName, messageSelector });
        MessageConsumer messageConsumer = null;
        try {
            messageConsumer = createConsumer(
                                             topic,
                                             sharedSubscriptionName,
                                             messageSelector,
                                             false, //nLocal
                                             true //is JMS2.0 shared non durable subscriber

            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedConsumer", messageConsumer);
        }
        return messageConsumer;

    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedDurableConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException, InvalidDestinationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedDurableConsumer", new Object[] { topic, name });
        MessageConsumer messageConsumer = null;
        try {
            messageConsumer = createDurableSubscriber(
                                                      topic, name,
                                                      null, //messageSelector
                                                      false, //nLocal
                                                      true, //is JMS2.0 shared durable subscriber
                                                      false //is JMs2.0 non-shared durable subscriber
            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedDurableConsumer", messageConsumer);
        }
        return messageConsumer;
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedDurableConsumer(javax.jms.Topic, java.lang.String, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) throws InvalidDestinationException, IllegalStateException, JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedDurableConsumer", new Object[] { topic, name, messageSelector });
        MessageConsumer messageConsumer = null;
        try {
            messageConsumer = createDurableSubscriber(
                                                      topic, name, messageSelector,
                                                      false, //nLocal
                                                      true, //is JMS2.0 shared durable subscriber
                                                      false //is JMs2.0 non-shared durable subscriber
            );
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSharedDurableConsumer", messageConsumer);
        }
        return messageConsumer;
    }

    /**
     * A message listener must not attempt to stop its own connection or context as this would lead to deadlock.
     * IllegalStateException has to be thrown in case if it calls stop.
     * THIS FUNCTION is called only in non-managed environments.
     */
    void validateStopCloseForMessageListener(String functionCall) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "validateStopForMessageListener");

        //Before triggering onMessage .. SIB pushes "owning session object" to Thread Local, 
        //Now obtain the Session object from Thread Local and validate with 'this'. 
        if ((JmsSessionImpl.asyncReceiverThreadLocal.get() != null) && (JmsSessionImpl.asyncReceiverThreadLocal.get() == this)) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Message Listener onMessage called stop on its own Context/connection.");

            if (functionCall.equalsIgnoreCase("stop")) {
                //stop is called.

                //the stop call is driven from this JmsSessionImpl object only.. throw JMSException
                throw (IllegalStateException) JmsErrorUtils.newThrowable(IllegalStateException.class,
                                                                         "INVALID_METHOD_CWSIA0517",
                                                                         new Object[] { this },
                                                                         tc
                                );
            } else {
                //close is called
                //the stop call is driven from this JmsSessionImpl object only.. throw JMSException
                throw (IllegalStateException) JmsErrorUtils.newThrowable(IllegalStateException.class,
                                                                         "INVALID_METHOD_CWSIA0518",
                                                                         new Object[] { this },
                                                                         tc
                                );

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "validateStopForMessageListener");

    }

    /**
     * Put the session object in thread local object
     * 
     * @param sess
     */
    static void pushMsgListenerSessionToThreadLocal(JmsSession sess) {
        JmsSessionImpl.asyncReceiverThreadLocal.set((JmsSessionImpl) sess);

    }

    /**
     * Remove the session object from thread local
     */
    static void removeMsgListenerSessionFromThreadLocal() {
        JmsSessionImpl.asyncReceiverThreadLocal.remove();
    }

    /**
     * This function blocks till all Async sends are resolved.
     */
    private void waitForAsyncSendCompletion() {
        boolean success = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "waitForAsyncSendCompletion");

        while (!_asyncSendQueue.isEmpty() || currentAsyncSendObject != null) {
            // if the async thread isn't running, no point in waiting...
            if (null==_asyncSendRunThread || !_asyncSendRunThread.isAlive()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(tc, "waitForAsyncSendCompletion: async thread not running, aborting wait");
                }
                success = false;
                break;
            }
            try {
                synchronized (_asyncSendQueue) {
                    // after twenty second.. again check is made if the async send queue is empty.
                    //the sleep time is precautinary measure.. to avoid a futuristic scenario in which notification would not come.
                    _asyncSendQueue.wait(1000 * 20);
                }
            } catch (InterruptedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "waitForAsyncSendCompletion got interrupted", e);
                success = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "waitForAsyncSendCompletion",new Boolean(success));
    }

    /**
     * CompletionListener calls (i.e OnComplete and onException) should not attempt
     * rollback/commit/close on its own Context/Session/connection.
     */
    void validateCloseCommitRollback(String functionCall) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "validateCloseCommitRollback", functionCall);

        //Before triggering onCompletion/onException .. SIB pushes "owning session object" to Thread Local, 
        //Now obtain the Session object from Thread Local and validate with 'this'. 
        if ((JmsSessionImpl.asyncThreadLocal.get() != null) && (JmsSessionImpl.asyncThreadLocal.get() == this)) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "OnComplete/onException called roolback/commit/close on its own Context/Session.");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "validateCloseCommitRollback");

            //the call is driven from this JmsSessionImpl object only.. throw JMSException
            throw (IllegalStateException) JmsErrorUtils.newThrowable(IllegalStateException.class,
                                                                     "INVALID_METHOD_CWSIA0515",
                                                                     new Object[] { functionCall,
                                                                                   this },
                                                                     tc
                            );

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "validateCloseCommitRollback");

    }

    /**
     * Adds AsynSend operation details to the Queue.
     * This function would get called only in case of non-managed environements.
     * 
     * @param msgProducer MessageProdcuder object which triggered AsyncSend
     * @param cListner CompletionListener of AsyncSend
     * @param msg
     */
    void addtoAsysncSendQueue(JmsMsgProducerImpl msgProducer, CompletionListener cListner, Message msg, int sendMethodType, Object[] params) {
        boolean startThread = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addtoAsysncSendQueue", new Object[] { msgProducer, cListner, cListner, sendMethodType, params });

        if (null==_asyncSendRunThread||!_asyncSendRunThread.isAlive()) {
            // initialize asyncSendRunThread
            _asyncSendRunThread = new Thread(new AsyncSendTask());
            _asyncSendRunThread.setDaemon(true);
            startThread = true;
        }

        //when AsyncSend is in progess, the message object should not get accessed by anyone other than SIB
        // inusebyAsyncSend and AsyncThreadId are used to restrict access for Message object.
        if ((msg != null) && msg instanceof JmsMessageImpl) {
            ((JmsMessageImpl) msg).setAsyncSendInProgress(true);
        }

        // queue the async command only after possibly marking the message - so that the setting async-in-progress to false can't
        // happen out of order with the setting to true
        _asyncSendQueue.offer(new AysncSendDetails(msgProducer, cListner, msg, sendMethodType, params));

        if (startThread) {
            // new async thread, start it...
            try {
                _asyncSendRunThread.start();
            } catch (Exception e) {
                //absorb the exception and add to trace. 
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "addtoAsysncSendQueue: starting asyncRunThread failed", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addtoAsysncSendQueue");
    }

    /**
     * Runnable method for all Async Send calls which are queued in _asyncSendQueue.
     */
    private class AsyncSendTask implements Runnable {

        @Override
        public void run() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "AsyncSendTask$run");

            try {
                /**
                 * This thread is dedicated to service the Async Send back ground work. As and when application calls
                 * Async send, JMSSession puts the Async send details object into _asyncSendQueue.
                 * This thread continusouly picks one by one Async send details object from the above queue and performs
                 * the real send work.
                 */

                if (_asyncSendQueue.isEmpty()) {
                    //Queue is null.. 
                    //notify to commit/rollback/complete to inform that Queue is empty.
                    synchronized (_asyncSendQueue) {
                        currentAsyncSendObject = null;
                        _asyncSendQueue.notifyAll();
                    }
                }

                while (((currentAsyncSendObject = _asyncSendQueue.take()) != null)) //blocking here .. if the queue is empty.
                {

                    try {
                        if (currentAsyncSendObject.getSendMethodType() == AsyncSendKillCommand) {
                            //correpsond session got closed.. hence exiting from this thread.
                            //Before clear _asyncSendQueue and give notification to waiting threads 
                            //System.out.println("Before clear count: " + _asyncSendQueue.size());

                            _asyncSendQueue.clear();
                            synchronized (_asyncSendQueue) {
                                currentAsyncSendObject = null;
                                _asyncSendQueue.notifyAll();
                            }

                            break;
                        }

                        //store the owner JMSSession object in thread local for each and every Async Send operation.
                        //in case if subsequent onComplete/onException calls commit/rollback/clsoe on its own Seesion/connection/context
                        //then IllegalStateException has to be thrown. 
                        JmsSessionImpl.asyncThreadLocal.set(JmsSessionImpl.this);

                        currentAsyncSendObject.getMsgProducer().sendCalledFromAsynsSendRunThread(currentAsyncSendObject.getMsg(),
                                                                                                 currentAsyncSendObject.getSendMethodType(),
                                                                                                 currentAsyncSendObject.getParams(),
                                                                                                 currentAsyncSendObject.getCListner());

                        //deleting the owning JMSSession object as async send operation (with the return of onCompletion/onException)
                        //has been completed.
                        JmsSessionImpl.asyncThreadLocal.remove();

                    } catch (Throwable e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Caught exception in AsyncRun thread ", e);
                    } finally {
                        //System.out.println("COMING TO CHK FOR EMPTY QUEUE");

                        if (_asyncSendQueue.isEmpty()) {
                            //Queue is null.. 
                            //notify to commit/rollback/complete to inform that Queue is empty.
                            synchronized (_asyncSendQueue) {
                                currentAsyncSendObject = null;
                                _asyncSendQueue.notifyAll();
                            }
                        }

                        //deleting the owning JMSSession object as async send operation (with the return of onCompletion/onException)
                        //has been completed.
                        JmsSessionImpl.asyncThreadLocal.remove();
                    }

                }

            } catch (InterruptedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Async thread got interrupted ", e);
            } catch (Throwable e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Caught exception in AsyncRun thread ", e);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "AsyncSendTask$run");
        } //run()
    }//class AsyncSendTask
}//class JmsSessionImpl

/**
 * This class contains details about individual AsyncSend call details.
 */
class AysncSendDetails {
    private final JmsMsgProducerImpl msgProducer;
    private final CompletionListener cListner;
    private final Message msg;
    private final int sendMthdType;
    private final Object[] params;

    AysncSendDetails(JmsMsgProducerImpl msgProducer, CompletionListener cListner, Message msg, int sendMethodType, Object[] params) {
        this.msgProducer = msgProducer;
        this.cListner = cListner;
        this.msg = msg;
        this.sendMthdType = sendMethodType;
        this.params = params;

    }

    /**
     * @return the _msgProducer
     */
    public JmsMsgProducerImpl getMsgProducer() {
        return msgProducer;
    }

    /**
     * @return the _cListner
     */
    public CompletionListener getCListner() {
        return cListner;
    }

    /**
     * @return the _msg
     */
    public Message getMsg() {
        return msg;
    }

    /**
     * @return the _sendMethodType
     */
    public int getSendMethodType() {
        return sendMthdType;
    }

    /**
     * @return the _params
     */
    public Object[] getParams() {
        return params;
    }

}// class AysncSendDetails

