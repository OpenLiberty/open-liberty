/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsDestination;
import com.ibm.websphere.sib.api.jms.JmsMsgConsumer;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.JmsConnInternals;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;
import com.ibm.wsspi.sib.core.trm.SibTrmConstants;
import com.ibm.wsspi.sib.pacing.MessagePacingControl;
import com.ibm.wsspi.sib.pacing.MessagePacingControlFactory;

public class JmsMsgConsumerImpl implements JmsMsgConsumer, ApiJmsConstants, JmsInternalConstants {

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsMsgConsumerImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // **************************** STATE VARIABLES ******************************

    private Consumer consumer;
    private ConsumerProperties props = null;
    private ConsumerSession coreConsumerSession = null;
    private SICoreConnection coreConn = null;
    private JmsSessionImpl session = null;

    private boolean closed = false;
    private final Object closedLock = new Object();

    /**
     * This variable is initialized in the constructor to reflect the acknowledge
     * mode value of the Session.
     */
    private final int sessionAckMode;

    /**
     * initialised in the constructor to reference the lock held in the session.
     * This lock is used to prevent concurrent calls to send/receive/commit etc.
     */
    private final Object sessionSyncLock;

    /**
     * Factory to create objects used on calls that require a selector.
     */
    SelectionCriteriaFactory selectionCriteriaFactory = null;

    /**
     * Used for XD pacing if busname is not set in the SIDestinationAddress
     */
    private String defaultBusName = null;

    // ***************************** CONSTRUCTORS ********************************

    /**
     * Constructor.
     * 
     * @param coreConnection The core connection through which this consumer will operate.
     * @param newSession The Session with which this consumer is associated.
     * @param newProps Object containing the properties required for creating the core consumer.
     * 
     * @throws javax.jms.JMSException
     */
    protected JmsMsgConsumerImpl(SICoreConnection coreConnection, JmsSessionImpl newSession, ConsumerProperties newProps) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsMsgConsumerImpl", new Object[] { coreConnection, newSession });
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "theDest : " + newProps.getJmsDestination() + " sel : " + newProps.getSelector() + " noL : " + newProps.noLocal());

        // Grab the busname for use by XD pacing
        defaultBusName = (String) newSession.getPassThruProps().get(SibTrmConstants.BUSNAME);

        // Keep hold of the consumer properties object in case we need
        // it later.
        props = newProps;

        // Store the core connection for later use.
        coreConn = coreConnection;
        session = newSession;
        sessionSyncLock = session.getSessionSyncLock();

        // Method local reference to the destination
        JmsDestination dest = props.getJmsDestination();

        // Get a selection criteria factory for later use.
        try {
            selectionCriteriaFactory = JmsServiceFacade.getSelectionCriteriaFactory();
        } catch (SIErrorException e) {
            // No FFDC code needed
            // d222942 review. SIErrorException are "should never happen" cases, so default message ok.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0085",
                                                            new Object[] { e, "JmsMsgConsumerImpl.constructor" },
                                                            e,
                                                            "JmsMsgConsumerImpl.constructor#1",
                                                            this,
                                                            tc
                            );
        }

        // If the destination is a TOPIC....
        if (dest instanceof Topic) {
            props.setDestinationType(DestinationType.TOPICSPACE);
            // The default value for readAhead with non-durable consumers
            // is true, meaning that messages can be streamed to a consumer
            // before they are requested. The exception to this statement is
            // if we are supporting multiple consumers (for example shared
            // durable subscribers in a cloned application server) in which
            // case the default should be false to prevent problems.
            // Users can override the default by explicitly specifying the
            // readAhead value on the CF or Destination.
            // The value for supportsMultiple was configured when the
            // ConsumerProperties object was created, so we can query it now
            // to determine the correct value.
            if (props.supportsMultipleConsumers()) {
                // This prevents odd behaviour for shared subscriptions.
                props.setReadAhead(false);
            }
            else {
                // This is the default for non-cloned subscribers
                props.setReadAhead(true);
            }
        }

        // If the destination is a QUEUE...
        else {
            props.setDestinationType(DestinationType.QUEUE);
            // Default value for readAhead when consuming from a queue type
            // destination.
            props.setReadAhead(false);
            // Configure the cluster control properties (application to queues only).
            JmsQueue jmsQueue = (JmsQueue) dest;
            String gatherStr = jmsQueue.getGatherMessages();
            if (ApiJmsConstants.GATHER_MESSAGES_ON.equals(gatherStr))
                props.setGatherMessages(true);
        }

        // Now see if the user requested an override of the readAhead property.
        // Look at the destination - it will have a value of AS_CONN, ON or OFF.
        String readAheadDestVal = dest.getReadAhead();

        // The readAhead is to be inherited from the connection....
        if (ApiJmsConstants.READ_AHEAD_AS_CONNECTION.equals(readAheadDestVal)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Inherit readAhead from Connection");
            // Look up the value specified on the CF at the time the connection was created.
            Map passThru = session.getPassThruProps();
            String cfVal = (String) passThru.get(JmsraConstants.READ_AHEAD);
            // If not sent to 'default', deliberately set to on or off.
            if (!ApiJmsConstants.READ_AHEAD_DEFAULT.equals(cfVal)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Connection explicitly set readAhead: " + readAheadDestVal);
                if (ApiJmsConstants.READ_AHEAD_ON.equals(cfVal)) {
                    props.setReadAhead(true);
                }
                else {
                    props.setReadAhead(false);
                }
            }
        }

        // ... else, the readAhead value was explicitly set on the Destination.
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Destination explicitly set readAhead: " + readAheadDestVal);
            // Deliberately set to on or off.
            if (ApiJmsConstants.READ_AHEAD_ON.equals(readAheadDestVal)) {
                props.setReadAhead(true);
            }
            else {
                props.setReadAhead(false);
            }
        }

        // Retrieve a reference to the acknowledge mode for use during receives.
        sessionAckMode = session.getAcknowledgeMode();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "acknowledge mode: " + sessionAckMode);

        // Set up the QoS we will use to consume messages. For the moment, we
        // assume that this is maximum possible in order to ensure that we can
        // receive both P and NP messages.
        props.setReliability(Reliability.ASSURED_PERSISTENT);

        /*
         * Determine whether or not to set recoverableExpress (175384)
         * 
         * JMS mode recoverableExpress
         * ------------------------------------------------------------
         * TRANSACTED true
         * AUTO_ACK false (they already have the message)
         * DUPS_OK false (no rollback mechanism anyway)
         * CLIENT_ACK true (session.recover to rollback)
         */
        boolean recovExpress = true;
        if ((sessionAckMode == Session.AUTO_ACKNOWLEDGE) || (sessionAckMode == Session.DUPS_OK_ACKNOWLEDGE)) {
            // Give the MP/Comms guys the chance to optimise in these cases.
            recovExpress = false;
        }
        props.setRecovExpress(recovExpress);

        // Create and start the consumer in part as a means to ensure that we do
        // ACL and existence checks up front.
        coreConsumerSession = createCoreConsumer(coreConn, props);

        // Make the consumerSession state match that of the JMS Session.
        if (session.getState() == STARTED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Start consumer because connection was already started.");
            start();
        }
        else {
            stop();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsMsgConsumerImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * @see javax.jms.MessageConsumer#getMessageSelector()
     */
    @Override
    public String getMessageSelector() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageSelector");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessageSelector", props.getSelector());
        return props.getSelector();
    }

    /**
     * @see javax.jms.MessageConsumer#getMessageListener()
     */
    @Override
    public MessageListener getMessageListener() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageListener");
        checkClosed();
        if (session.isManaged()) {
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "MGD_ENV_CWSIA0084",
                                                                               new Object[] { "MessageConsumer.getMessageListener" },
                                                                               tc);
        }
        MessageListener ml = null;
        if (consumer != null) {
            ml = consumer.getMessageListener();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessageListener", ml);
        return ml;
    }

    /**
     * assign a MessageListener to this consumer for async message delivery.
     * 
     * @param listener an instance of MessageListener supplied by an application for
     *            the purpose of consuming messages. If null, remove any previously assigned
     *            MessageListener.
     * @see javax.jms.MessageConsumer#setMessageListener(MessageListener)
     */
    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        // pass control to the internal method
        boolean checkManaged = true;
        _setMessageListener(listener, checkManaged);
    }

    /**
     * @see javax.jms.MessageConsumer#receive()
     */
    @Override
    public Message receive() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receive");
        Message msg = null;

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");
            checkClosed();
            session.checkSynchronousUsage("receive");
            msg = receiveInboundMessage(true, 0);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "receive", msg);
        return msg;
    }

    /**
     * @see javax.jms.MessageConsumer#receive(long)
     */
    @Override
    public Message receive(long timeout) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receive", timeout);
        Message msg = null;

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");
            checkClosed();
            session.checkSynchronousUsage("receive");
            msg = receiveInboundMessage(true, timeout);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "receive", msg);
        return msg;
    }

    /**
     * @see javax.jms.MessageConsumer#receiveNoWait()
     */
    @Override
    public Message receiveNoWait() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveNoWait");
        Message msg = null;

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");
            checkClosed();
            session.checkSynchronousUsage("receiveNoWait");
            if (session.getState() == STARTED) {
                msg = receiveInboundMessage(false, -1);
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "consumer not started");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "receiveNoWait", msg);
        return msg;
    }

    /**
     * @see javax.jms.MessageConsumer#close()
     */
    @Override
    public void close() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");
        boolean originallyClosed = false;

        // synchronizing on closedLock serves a dual purpose: it ensures
        // that we always see the uptodate value of the closed variable,
        // and it protects the body of this method from concurrent access.
        synchronized (closedLock) {
            // Save the state of the closed flag before we update it here. If
            // this is the first time that close has been called then we want
            // to carry out the logic, otherwise silently complete with no
            // further action.
            originallyClosed = closed;
            // set the closed flag
            closed = true;
        }

        if (!originallyClosed) {
            // Stop the consumer gracefully before we close it.
            stop();

            // Close the core object
            if (coreConsumerSession != null) {
                try {
                    coreConsumerSession.close();
                } catch (SIException sice) {
                    // No FFDC code needed
                    // d238447 FFDC Review. The exceptions are connectionLost/Dropped and ResourceE.
                    //   These have probably already been FFDCd if necessary, and are externalish, so
                    //   no FFDC here.
                    throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                    "EXCEPTION_RECEIVED_CWSIA0085",
                                                                    new Object[] { sice, "JmsMsgConsumerImpl.close" },
                                                                    sice,
                                                                    null, // null probeId = no FFDC
                                                                    this,
                                                                    tc);
                }
                coreConsumerSession = null;
                consumer = null;
            }
            // Flag the session that we are closed. Note that this will
            // commit the dups transaction if there are outstanding gets
            // on this transaction.
            session.removeConsumer(this);
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "already closed");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /**
     * @see javax.jms.MessageProducer#getDestination()
     */
    @Override
    public Destination getDestination() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDestination");
        checkClosed();
        JmsDestination d = props.getJmsDestination();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDestination", d);
        return d;
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * Called in the constructor to create an up front connection to the Destination.
     * 
     * Note: This method is overridden in the JmsDurableSubscribeImpl class
     * to create a connection to a durable subscription. Care should
     * be taken when changing parameters to this method.
     */
    protected ConsumerSession createCoreConsumer(SICoreConnection _coreConn, ConsumerProperties _props) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createCoreConsumer", new Object[] { _coreConn });
        ConsumerSession cs = null;

        JmsDestinationImpl dest = (JmsDestinationImpl) _props.getJmsDestination();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "destName: " + dest.getConsumerDestName() +
                                  " type: " + _props.getDestinationType() +
                                  " discrim: " + dest.getDestDiscrim() +
                                  " selector: " + _props.getSelector() +
                                  " reliability: " + _props.getReliability() +
                                  " noLocal: " + _props.noLocal() +
                                  " unrecovRel: " + _props.getUnrecovReliability() +
                                  " gatherMsgs: " + _props.isGatherMessages());
        try {
            // Need to create a selection criteria object for create consumer call.
            SelectionCriteria selectionCriteria = null;
            SIDestinationAddress consumerSIDA = null;
            try {
                selectionCriteria = selectionCriteriaFactory.createSelectionCriteria(dest.getDestDiscrim(), // discriminator (topic)
                                                                                     _props.getSelector(), // selector string
                                                                                     SelectorDomain.JMS // selector domain
                );
                // The MessageProcessor resolves the bus field of the SIDA object that is passed
                // in to createConsumerSession, which can lead to unusual behaviour since we have
                // been passing this by reference rather than copying it. Taking a copy here
                // avoids the 'back-door' populating of the object, and is still outside the mainline
                // send path.
                SIDestinationAddress originalConsumerProps = dest.getConsumerSIDestinationAddress();
                consumerSIDA = JmsMessageImpl.destAddressFactory.createSIDestinationAddress(originalConsumerProps.getDestinationName(),
                                                                                            ((JsDestinationAddress) originalConsumerProps).isLocalOnly(),
                                                                                            originalConsumerProps.getBusName());
            } catch (SIErrorException sice) {
                // No FFDC code needed
                // d222942 review. SIErrorException are "should never happen" cases, so default message ok.
                // d238447 FFDC review. Generate FFDC.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0085",
                                                                new Object[] { sice, "JmsMsgConsumerImpl.createCoreConsumer" },
                                                                sice,
                                                                "JmsMsgConsumerImpl.createCoreConsumer#1",
                                                                this,
                                                                tc
                                );
            }

            if (_props.getSubName() != null) {
                //Getting the final non durable subscription id by calling getCoreDurableSubName
                // if subname is 'nondurSub1' and clientid is 'cid', the method getCoreDurableSubName 
                // returns subscriber id as 'nondurSub1##cid'
                String subscriptionName = JmsInternalsFactory.getSharedUtils().getCoreDurableSubName(_props.getClientID(), _props.getSubName());

                //createSharedConsumerSession is newly introduced SPI for non-durable shared subscriptions.
                cs = _coreConn.createSharedConsumerSession(subscriptionName,
                                                           consumerSIDA,
                                                           _props.getDestinationType(),
                                                           selectionCriteria,
                                                           _props.getReliability(),
                                                           _props.readAhead(),
                                                           true,
                                                           _props.noLocal(),
                                                           _props.getUnrecovReliability(), /* recoverableExpress */
                                                           false, // bifurcatable
                                                           null, // alternateUserID
                                                           true, // ignoreInitialIndoubts
                                                           _props.isGatherMessages(),
                                                           null // msg control props
                );
            } else {

                cs = _coreConn.createConsumerSession(consumerSIDA,
                                                     _props.getDestinationType(),
                                                     selectionCriteria,
                                                     _props.getReliability(),
                                                     _props.readAhead(),
                                                     _props.noLocal(),
                                                     _props.getUnrecovReliability(), /* recoverableExpress */
                                                     false, // bifurcatable
                                                     null, // alternateUserID
                                                     true, // ignoreInitialIndoubts
                                                     _props.isGatherMessages(),
                                                     null // msg control props
                );
            }
        } catch (SISelectorSyntaxException e) {
            // No FFDC code needed
            // d238447 FFDC review. Bad selector is an app' error, so no FFDC.
            throw (InvalidSelectorException) JmsErrorUtils.newThrowable(InvalidSelectorException.class,
                                                                        "INVALID_SELECTOR_CWSIA0083",
                                                                        null,
                                                                        e,
                                                                        null, // null probeId = no FFDC.
                                                                        this,
                                                                        tc
                            );
        } catch (SINotAuthorizedException sidnfe) {
            // No FFDC code needed
            // d238447 FFDC review: Not auth is an app/config error, so no FFDC.
            throw (JMSSecurityException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                                    "CONSUMER_AUTH_ERROR_CWSIA0090",
                                                                    new Object[] { dest.getDestName() },
                                                                    sidnfe,
                                                                    null, // null probeId = no FFDC
                                                                    this,
                                                                    tc
                            );
        } catch (SINotPossibleInCurrentConfigurationException dwte) {
            // No FFDC code needed
            // This was SIDestinationWrongTypeException, now also gets DestNotFound
            // d238447 FFDC review. Both are app/config errors, so no FFDC.
            String msgKey = "MC_CREATE_FAILED_CWSIA0086";
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           msgKey,
                                                                           new Object[] { dest },
                                                                           dwte,
                                                                           null, // null probeId = no FFDC
                                                                           this,
                                                                           tc
                            );
        } catch (SITemporaryDestinationNotFoundException e) {
            // No FFDC code needed
            // d238447 FFDC reveiw. App error, no FFDC.
            String msgKey = "MC_CREATE_FAILED_CWSIA0086";
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           msgKey,
                                                                           new Object[] { dest },
                                                                           e,
                                                                           null, // null probeId = no FFDC
                                                                           this,
                                                                           tc
                            );
        } catch (SIIncorrectCallException e) {
            // No FFDC code needed
            // d222942 review. Default message ok.
            // d238447 FFDC review. Incorrect call would seem to be an internal error, so generate FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0085",
                                                            new Object[] { e, "JmsMsgConsumerImpl.createCoreConsumer" },
                                                            e,
                                                            "JmsMsgConsumerImpl.createCoreConsumer#6",
                                                            this,
                                                            tc
                            );
        } catch (SIException sice) {
            // No FFDC code needed
            // d222942 review.
            // Exceptions thrown from createConsumerSession which can be handled with default message:
            //SIConnectionUnavailableException, SIConnectionDroppedException,
            //SIResourceException, SIConnectionLostException, SILimitExceededException.
            // d238447 FFDC Review. These are either external errors, or should already have been FFDCd,
            //   so don't FFDC here.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0085",
                                                            new Object[] { sice, "JmsMsgConsumerImpl.createCoreConsumer" },
                                                            sice,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        } catch (SIErrorException sie) {
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0085",
                                                            new Object[] { sie, "JmsMsgConsumerImpl.createCoreConsumer" },
                                                            sie,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createCoreConsumer", cs);
        return cs;

    }

    /**
     * Method start.
     */
    protected void start() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "start");

        // We call start regardless of whether this is synchronous or async
        if (coreConsumerSession != null) {
            try {

                synchronized (closedLock) {
                    if (!closed) {
                        // Do not deliver message immediately on start.
                        coreConsumerSession.start(false);
                    }
                    else {
                        // This condition could be caused by tight looping closing of consumers at
                        // the same time as calling connection.start. This is a less intrusive solution
                        // than adding the locking that would otherwise be required.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Avoided starting a consumer that has been closed.");
                    }
                }

            } catch (SIException sice) {
                // No FFDC code needed
                // d222942 review. Default message ok.
                // This exception will be propogated back up to the Session, which will
                // cache the first exception thrown. Once all the consumers have been
                // started (or stopped) the first caught exception is then propogated
                // to the Connection for delivery to the application.
                // d238447 FFDC Review. Either external or already FFDCd, so don't FFDC here.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0085",
                                                                new Object[] { sice, "JmsMsgConsumerImpl.start" },
                                                                sice,
                                                                null, // null probeId = no FFDC
                                                                this,
                                                                tc);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "start");
    }

    /**
     * Method stop.
     */
    protected void stop() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stop");

        if (coreConsumerSession != null) {
            try {
                coreConsumerSession.stop();
            } catch (SIException sice) {
                // No FFDC code needed
                // d222942 review - default message ok.
                // This exception will be propogated back up to the Session, which will
                // cache the first exception thrown. Once all the consumers have been
                // started (or stopped) the first caught exception is then propogated
                // to the Connection for delivery to the application.
                // d238447 FFDC Review. The following exceptions apply:
                //          SISessionUnavailableException, SISessionDroppedException,
                //          SIConnectionUnavailableException, SIConnectionDroppedException,
                //          SIResourceException, SIConnectionLostException
                //  I'm assuming these are either external errors and/or will have been FFDCd in
                //  the lower levels. No FFDC here.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0085",
                                                                new Object[] { sice, "JmsMsgConsumerImpl.stop" },
                                                                sice,
                                                                null, // null probeId = no FFDC
                                                                this,
                                                                tc);
            }
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "coreConsumerSession is null, already closed ?");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "stop");
    }

    /**
     * This method is called at the beginning of every method that should not
     * work if the consumer has been closed. It prevents further execution by
     * throwing a JMSException with a suitable "I'm closed" message.
     */
    void checkClosed() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "checkClosed");

        synchronized (closedLock) {
            if (closed) {
                throw (JMSException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                "CONSUMER_CLOSED_CWSIA0081",
                                                                null,
                                                                tc);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "checkClosed");
    }

    /**
     * assign a MessageListener to this consumer for async message delivery.
     * This version supports use from the MessageListenerSetter class
     * (checkManaged=false), as well as from the normal setMessageListener
     * method (checkManaged=true)
     * 
     * @param listener an instance of MessageListener supplied by an application for
     *            the purpose of consuming messages. If null, remove any previously assigned
     *            MessageListener.
     * @param checkManaged flag to indicate whether or not to restrict the use of this method
     *            in managed environments.
     * @throws JMSException
     * @see javax.jms.MessageConsumer#setMessageListener(MessageListener)
     */
    void _setMessageListener(MessageListener listener, boolean checkManaged) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_setMessageListener", new Object[] { listener, checkManaged });

        synchronized (sessionSyncLock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "got lock");

            // throw exception if closed
            checkClosed();

            // throw exception if running in a managed environment
            if (checkManaged && session.isManaged()) {
                boolean exceptionRequired = true;
                // To support async beans, we suppress the exception if:
                // the session is non-transacted, auto-ack, and
                // the listener is a Proxy, and
                // the invocation handler is from the com.ibm tree.
                if (sessionAckMode == Session.AUTO_ACKNOWLEDGE && listener instanceof Proxy) {
                    InvocationHandler handler = Proxy.getInvocationHandler(listener);
                    String name = handler.getClass().getName();
                    if (name.startsWith("com.ibm")) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "async beans, listener accepted");
                        exceptionRequired = false;
                    }
                }
                if (exceptionRequired) {
                    throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                                       "MGD_ENV_CWSIA0084",
                                                                                       new Object[] { "MessageConsumer.setMessageListener" },
                                                                                       tc);
                }
            }

            if (listener == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "application has asked to deregister listener");
                removeAsyncListener();
                session.registerSyncConsumer(this);
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "application has supplied a listener to register");
                setAsyncListener(listener);
                session.registerAsyncConsumer(this);
            }

        } // releases sessionSyncLock

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_setMessageListener");
    }

    /**
     * internal method for assigning a message listener
     */
    private void setAsyncListener(MessageListener listener) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setAsyncListener", listener);

        consumer = new Consumer(listener, session, sessionAckMode);
        try {
            // ConsumerSession must be stopped to register the callback
            coreConsumerSession.stop();
            int batchSize = 1;
            boolean deliverImmediately = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Registering async callback for consumer" + consumer);
            // the register call will replace any existing callback, so no need for
            // separate deregister call.
            coreConsumerSession.registerAsynchConsumerCallback(consumer,
                                                               0, // maxActiveMessages - no limit
                                                               0, // messageLockExpiry - no expiry
                                                               batchSize,
                                                               session.getOrderingContext()
                            );

            // Deal with the case where the listener is set after the connection
            // is started. Note that this will handle the restarting of the core
            // consumer session if we paused it earlier in this block.
            if (session.getState() == STARTED)
                coreConsumerSession.start(deliverImmediately);
        } catch (SIIncorrectCallException sie) {
            // No FFDC code needed
            // d222942 review. Default message ok.
            // d238447 FFDC Review. FFDC here, see comment in next catch for more detail.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0085",
                                                            new Object[] { sie, "JmsMsgConsumerImpl.setAsyncListener" },
                                                            sie,
                                                            "JmsMsgConsumerImpl.setAsyncListener#1",
                                                            this,
                                                            tc);
        } catch (SIException sie) {
            // No FFDC code needed
            // The exceptions thrown by stop, registerAsynchConsumerCallback and start are mostly
            // the "connection lost" family. registerAsynchConsumerCallback can also throw an
            // SIIncorrectCallException if the coreConsumerSession hasn't been stopped, but that
            // shouldn't be possible in the code above. Default message ok.
            // d238447 FFDC Review. SIIncorrectCall separated into its own catch block above,
            //   no FFDCs for this group.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0085",
                                                            new Object[] { sie, "JmsMsgConsumerImpl.setAsyncListener" },
                                                            sie,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setAsyncListener");
    }

    /**
     * This method is used to remove an existing asynchronous consumer
     * from the core consumer object, and reset the JMS consumer back to
     * sychronous receipt mode.
     * 
     * 13/01/04 Modified so that it can be called repeatedly, and restores
     * the started state of the core consumer session after running.
     */
    private void removeAsyncListener() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeAsyncListener");

        // if the consumer is null, there is nothing to do
        if (consumer != null) {
            try {
                // stop the coreConsumerSession
                coreConsumerSession.stop();
                // remove the callback
                coreConsumerSession.deregisterAsynchConsumerCallback();
                // restart the coreConsumerSession if necessary
                if (session.getState() == STARTED) {
                    // Do not deliver message immediately on start.
                    coreConsumerSession.start(false);
                }
                // null out our consumer instance
                consumer = null;
            } catch (SIIncorrectCallException sice) {
                // No FFDC code needed
                // stop/start methods ok for default message. deregisterAsynchConsumerCallback can throw
                // an SIIincorrectCallException is session is not stopped, but this shouldn't be possible
                // with the code above.
                // d238447 FFDC review. See comment in next catch block. Generate FFDC.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0085",
                                                                new Object[] { sice, "JmsMsgConsumerImpl.removeAsyncListener" },
                                                                sice,
                                                                "JmsMsgConsumerImpl.removeASyncListener#1",
                                                                this,
                                                                tc);
            } catch (SIException sice) {
                // No FFDC code needed
                // stop/start methods ok for default message. deregisterAsynchConsumerCallback can throw
                // an SIIincorrectCallException is session is not stopped, but this shouldn't be possible
                // with the code above.
                // d238447 FFDC review. We are dealing with:
                //          SISessionUnavailableException, SISessionDroppedException,
                //          SIConnectionUnavailableException, SIConnectionDroppedException,
                //          SIResourceException, SIConnectionLostException;
                //          SIIncorrectCallException
                // Of these SIIncorrectCallE is probably the only one that warrents FFDC at this level,
                // so a new catch block for IncorrectCallE added above, no FFDCs here.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0085",
                                                                new Object[] { sice, "JmsMsgConsumerImpl.removeAsyncListener" },
                                                                sice,
                                                                null, // null probeId = no FFDC.
                                                                this,
                                                                tc);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeAsyncListener");
    }

    /**
     * Get the noLocal flag.
     * 
     * @return noLocal
     */
    boolean getNoLocalFlag() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getNoLocalFlag");
        checkClosed();
        boolean nl = props.noLocal();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getNoLocalFlag", nl);
        return nl;
    }

    /**
     * This method is called by each of the flavours of receive in order to
     * get a message. This method handles the semantics required by transacted,
     * and the three untransacted types of Session.
     * 
     * @return Message
     */
    private Message receiveInboundMessage(boolean waiting, long timeout) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveInboundMessage", new Object[] { waiting, timeout });
        Message msg = null;
        String bus = null;
        String destName = null;
        SIBusMessage coreMsg = null;

        SITransaction st = session.getTransaction();

        // 304501.api Check for XD pacing
        MessagePacingControl mpc = MessagePacingControlFactory.getInstance();
        if (mpc != null && mpc.isActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "about to call preSynchReceive");
            Object context = coreConsumerSession;
            bus = coreConsumerSession.getDestinationAddress().getBusName();
            if (bus == null)
                bus = defaultBusName; // Default to the value from the connection factory
            destName = coreConsumerSession.getDestinationAddress().getDestinationName();
            if (waiting) {
                timeout = mpc.preSynchReceive(bus, destName, context, timeout);
            }
            else {
                mpc.preSynchReceive(bus, destName, context, -1);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "preSynchReceive complete");
        }

        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(this, tc, "About to call receive method; transaction: " + st + (waiting ? " timeout: " + timeout : ""));
                // Put a big warning in if they have called receive without starting
                // the connection.
                // This won't cause a problem if the connection is started before the timeout
                // elapses, but in 90% of cases is caused by a rookie programming error because
                // they have forgotton to start the application at all - then they wonder why
                // no messages are received. (Note that you can still send messages - just not
                // receive them).
                if (session.getState() != STARTED) {
                    SibTr.debug(this, tc, "WARNING - APPLICATION CALLED RECEIVE BUT CONNECTION IS NOT STARTED");
                }
            }

            // We now branch depending on which of the two core receive
            // calls we are interested in.
            if (waiting) {
                // This is a get with timeout, or infinite wait
                coreMsg = coreConsumerSession.receiveWithWait(st, timeout);

                // d248044 Provide a warning if receive(wait) returns null on a stopped connection
                if (coreMsg == null && session.getState() != STARTED) {
                    // Find the stack entry of the caller of the receive method.
                    // d386440 - in some cases it seems that the first line of the stack can
                    //   be for the Throwable constructor, which throws off the fixed-offset
                    //   approach, and means we quote a line reference the JmsMsgConsumerImpl
                    //   class. This approach checks when we leave the JMS package.
                    String caller = JmsErrorUtils.getFirstApplicationStackString();
                    SibTr.warning(tc, "MC_CONN_STOPPED_CWSIA0087", caller);
                }
            }
            else {
                // This is a get with no wait.
                coreMsg = coreConsumerSession.receiveNoWait(st);

            }

            // Convert the core message to a JMS version - pass the session's 'pass thru props' so certain
            // properties will be associated with the message (SIB0121)
            msg = JmsInternalsFactory.getSharedUtils().inboundMessagePath(coreMsg, session, session.getPassThruProps());

            if (msg != null) {
                if (((JmsMessageImpl) msg).getMsgReference().getJmsDestination() == null) {
                    // The received message did not come from JMS (or had the JmsDestination field
                    // deliberately excluded).
                    JmsDestinationImpl dest = (JmsDestinationImpl) props.getJmsDestination();
                    if (!dest._getInhibitJMSDestination()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "This message was not sent by a JMS client, and so has no JmsDestination field set. Setting it to: "
                                                  + props.getJmsDestination().getDestName());
                        msg.setJMSDestination(dest);
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "sessionAckMode : " + sessionAckMode);

            // This handles the commit of transaction for dups ok, and keeping a
            // tally of the number uncommitted messages received.
            // Optimise to call only if transaction is non-null, because we know
            // that it does nothing if it is null.
            if ((msg != null) && (st != null)) {
                session.notifyMessageConsumed(st);
            }
        } catch (SISessionUnavailableException oce) {
            // No FFDC code needed
            // This is normal if blocking receive is interrupted by close().
            // Trace and discard, will return null from this method.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "caught SISessionUnavailableException: " + oce);
            // We do need to ensure this consumer is marked as closed, as we
            // may also enter this block if the ME becomes unavailable (but the
            // server stays active). PK53368
            close();

            // There is one known case where an SISessionUnavailableException
            // exception is thrown for a reason that we do need to report
            // to the user. This is when the receiver is created against a
            // destination that is stricly ordered, and there is an indoubt
            // transaction on that destination that prevents the consumer
            // from consuming messages.
            // The only way we have to test for this special case is using
            // the NLS message identifier
            String exceptionMsg = oce.getMessage();
            if (exceptionMsg != null &&
                (exceptionMsg.contains("CWSIP0180E") ||
                exceptionMsg.contains("CWSIP0194E"))) {
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "INBOUND_MSG_ERROR_CWSIA0103",
                                                                new Object[] { oce },
                                                                oce,
                                                                null, // null probeId = no FFDC
                                                                this,
                                                                tc);
            }
        } catch (SIException sice) {
            // No FFDC code needed

            // d238447 FFDC Review. Need to group the following into those requiring FFDC and those that don't:
            // Not much doc' available, I suspect that all of these are either external/app errors and/or
            // will already have been FFDCd. Don't generate FFDC here.
            //        SISessionUnavailableException, SISessionDroppedException,
            //        SIConnectionUnavailableException, SIConnectionDroppedException,
            //        SIResourceException, SIConnectionLostException, SILimitExceededException,
            //        SINotAuthorizedException,
            //        SIIncorrectCallException - thrown when in use for async delivery
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "INBOUND_MSG_ERROR_CWSIA0103",
                                                            new Object[] { sice },
                                                            sice,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc);
        }
        // 304501.api XD pacing
        if (mpc != null && mpc.isActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "about to call postSynchReceive");
            Object context = coreConsumerSession;
            boolean msgReceived = msg != null;
            mpc.postSynchReceive(bus, destName, context, msgReceived);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "postSynchReceive complete");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "receiveInboundMessage", msg);
        return msg;
    }

    /**
     * Provides a minimalist abrupt shutdown of the consumer.
     * This method is used from the async delivery code, and its
     * primary purpose is to prevent further message loss in the
     * face of unrecoverable errors during message delivery.
     */
    private void emergencyClose() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "emergencyClose");

        try {
            if (coreConsumerSession != null)
                coreConsumerSession.close();
        } catch (SIException e) {
            // No FFDC code needed
            // d238447 FFDC review - ok
            FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.impl.JmsMsgConsumerImpl", "emergencyClose#1", this);
        } finally {
            // d245254 - set the consumer session to null so that subsequent stop/close calls
            // don't generate exceptions.
            coreConsumerSession = null;
            // set the closed flag so that the majority of methods calls will generate an appopriate exception
            // Note that this is not synchronized on the closeLock as we don't want to risk this blocking.
            closed = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "emergencyClose");
    }

    // ***************************** INNER CLASSES *******************************

    /**
     * An instance of this class acts as the endpoint for the core API
     * asynchronous consumer codepath.
     */
    private class Consumer implements AsynchConsumerCallback
    {
        private MessageListener listener = null;
        private Object sessionLock = null;
        private JmsConnInternals excTarget = null;
        private JmsSessionImpl session = null;
        private int sessionAckMode = -1;

        /**
         * @param newListener JMS MessageListener object supplied by the
         *            user application to which messages will be delivered on arrival.
         * @param theSession The JMS Session with which we are associated.
         * @param theSessionAckMode The acknowledge mode from the Session. We pass this in as a
         *            parameter to avoid having to do lookup inside a try block again.
         * @param QOS The quality of service to use for this receiver.
         */
        public Consumer(MessageListener newListener, JmsSessionImpl theSession, int theSessionAckMode) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "Consumer", new Object[] { newListener, theSession, theSessionAckMode });

            listener = newListener;
            sessionLock = theSession.getAsyncDeliveryLock();
            excTarget = (JmsConnInternals) theSession.getConnection();
            session = theSession;
            sessionAckMode = theSessionAckMode;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "Consumer", sessionLock);
        }

        /**
         * This method is called by the core layer when a message becomes available
         * for this consumer.
         */
        @Override
        public void consumeMessages(LockedMessageEnumeration lme) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "consumeMessages", lme);

            // Get the next locked message
            Message jmsMsg = obtainMessage(lme);

            if (jmsMsg != null) {
                // Use the sessionLock to ensure that we don't concurrently deliver
                // messages to multiple consumers on this Session.
                // This sync block must extend over all transaction related operations
                // (including the error cleanup) to prevent the transaction from being
                // ripped from under us.
                synchronized (sessionLock) {
                    SITransaction st;

                    if (session.isManaged()) {
                        // async delivery in a managed environment implies Async Beans support.
                        // Don't request a transaction, use null instead.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "async beans: using null trans");
                        st = null;
                    }
                    else {
                        try {
                            // Get the transaction currently in use by the session, this will
                            // be null if the ack mode is auto ack.
                            st = session.getTransaction();
                        } catch (JMSException e) {
                            // No FFDC code needed
                            // d238447 FFDC review. FFDCs generated in getTransaction, so don't call processThrowable here.
                            // Really shouldn't have got an exception, if we have, then there is no transaction under which to proceed, so give up.
                            // Assume this is non-recoverable and close this message consumer down
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "unrecoverable error, attempting to close this messageConsumer");
                            // 202458 covers MP causing this to deadlock...
                            emergencyClose();
                            // Pass this on to the async exception listener
                            if (excTarget != null)
                                excTarget.reportException(e);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(this, tc, "consumeMessages", "early return");
                            return; // EARLY RETURN
                        }
                    } // end of if isManaged

                    try {
                        // Delete before calling onMessage if a transaction is in effect
                        // so that any call to commit, rollback, acknowledge or recover
                        // from inside onMessage will work correctly.
                        if (st != null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Delete under transaction: " + st);
                            lme.deleteCurrent(st);
                        }
                        // Notify the session that a message has been obtained and
                        // will be consumed by calling notifyMessagePreConsume().
                        // We must call this method for all transaction types because we use
                        // the underlying commit-count to determine if the onMessage calls recover.
                        session.notifyMessagePreConsume(st);
                        // Call the application onMessage.
                        boolean onMessageSuccessful = true;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Invoke app onMessage");

                        try {
                            //store the owner JMSSession object in thread local for each and every Async receive operation.
                            //in case if subsequent onMessage calls stop on its own connection/context
                            //then IllegalStateException has to be thrown. 
                            JmsSessionImpl.pushMsgListenerSessionToThreadLocal(session);
                            listener.onMessage(jmsMsg);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "app onMessage complete");
                        } catch (RuntimeException re) {
                            // No FFDC code needed
                            // The user code has thrown a runtime exception.
                            onMessageSuccessful = false;
                            dealWithRuntimeExceptionFromOnMessage(lme, st, re);
                        } finally {
                            //deleting the owning JMSSession object as async recv operation (with the return of onMessage)
                            //has been completed.
                            JmsSessionImpl.removeMsgListenerSessionFromThreadLocal();
                        }
                        // If onMessage returned normally, post on message code
                        if (onMessageSuccessful) {
                            // Tell the session that the message was successfully consumed.
                            // Optimise to call only if the session is a dups ok ack mode
                            // session, because we know that it does nothing if it isn't.
                            if (sessionAckMode == Session.DUPS_OK_ACKNOWLEDGE) {
                                session.notifyMessagePostConsume();
                            }
                            // If no transaction is in effect we cannot delete before the call
                            // to onMessage, because if an exception is thrown within onMessage
                            // the message must remain available.
                            deleteOrMakeAvailable(lme, st);
                        }
                    }

                    catch (Exception e) {
                        // No FFDC code needed
                        // d238447 review. Too broad a brush to analyse in detail. Generate FFDC to be on the safe side.
                        // (used to call processThrowable, don't need to do it twice, use newThrowable below)
                        // d222942 review - default message ok
                        JMSException jmse = (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                                      "EXCEPTION_RECEIVED_CWSIA0085",
                                                                                      new Object[] { e, "Consumer.consumeMessages" },
                                                                                      e,
                                                                                      "JmsMsgConsumerImpl.Consumer.consumeMessages#6",
                                                                                      this,
                                                                                      tc);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "closing");
                        emergencyClose();
                        // Pass this on to the async exception listener
                        if (excTarget != null)
                            excTarget.reportException(jmse);
                    }

                } // releases sessionLock

            }
            stopConsumerIfAppropriate();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "consumeMessages");
        }

        /**
         * getMessageListener
         */
        public MessageListener getMessageListener() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "getMessageListener");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getMessageListener", listener);
            return listener;
        }

        // IMPLEMENTATION METHODS

        /**
         * Obtain the next locked message
         * 
         * @param lme The current LockedMessageEnumeration
         * 
         * @return The message, if one was obtained
         */
        private final Message obtainMessage(LockedMessageEnumeration lme) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "obtainMessage", lme);
            Message jmsMsg = null;

            try {
                // This method assumes we have only been given one message
                // in the lme, so check this is true.
                int mc = lme.getRemainingMessageCount();
                if (mc != 1) {
                    // In theory this should never occur because we register the async
                    // callback object with a batchsize of 1, however it is not unheard
                    // of for the lme to contain zero, or more than 1 messages when MP
                    // goes wrong!
                    String key = "INTERNAL_ERROR_CWSIA0499";
                    throw (java.lang.IllegalStateException) JmsErrorUtils.newThrowable(java.lang.IllegalStateException.class,
                                                                                       key,
                                                                                       new Object[] { "lme.messageCount", mc },
                                                                                       tc);
                }
                SIBusMessage msg = lme.nextLocked();
                // We take this SIBusMessage and turn it into the JMS representation - pass the sessions 'pass thru props'
                // so certain properties will be associated with the message (SIB0121)
                jmsMsg = JmsInternalsFactory.getSharedUtils().inboundMessagePath(msg, session, session.getPassThruProps());
            }

            catch (SIException e) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception obtaining message from ConsumerSession (async)", e);
                // unlock all to ensure redelivery (and therefore poison msg handling)
                unlockConsumerSession(lme, false);
                // d222942 review - default message ok. d238447 review - FFDC generation ok
                JMSException jmse = (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                              "EXCEPTION_RECEIVED_CWSIA0085",
                                                                              new Object[] { e, "Consumer.consumeMessages" },
                                                                              e,
                                                                              "JmsMsgConsumerImpl.Consumer.consumeMessages#1",
                                                                              this,
                                                                              tc);
                // Pass this on to the async exception listener
                if (excTarget != null)
                    excTarget.reportException(jmse);
            }

            catch (JMSException jmse) {
                // No FFDC code needed
                // d238447 FFDC review. These JMSExceptions should already have caused FFDCs, so don't call processThrowable here
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "exception caught ", jmse);
                // unlock all to ensure redelivery (and therefore poison msg handling)
                unlockConsumerSession(lme, true);
                // Pass this on to the async exception listener
                if (excTarget != null)
                    excTarget.reportException(jmse);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "obtainMessage", jmsMsg);
            return jmsMsg;
        }

        /**
         * unlockConsumerSession
         * 
         * Unlock all to ensure redelivery (and therefore poison msg handling)
         * 
         * @param lme Current LockedMessageEnumeration
         * @param ffdcIfException Whether to FFDC if an Exception is caught by the method
         */
        private final void unlockConsumerSession(LockedMessageEnumeration lme, boolean ffdcIfException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "unlockConsumerSession", new Object[] { lme, ffdcIfException });
            try {
                ConsumerSession cs = lme.getConsumerSession();
                if (cs != null) {
                    cs.unlockAll();
                }
                else {
                    // If we can't get the ConsumerSession to do the unlockAll then we risk message loss, so close down.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "couldn't get ConsumerSession, closing");
                    emergencyClose();
                }
            } catch (SIException e1) {
                // No FFDC code needed
                // d238447 Should generate an FFDC here if called from catch of JMSException
                if (ffdcIfException) {
                    FFDCFilter.processException(e1, "JmsMsgConsumerImpl.Consumer.consumeMessages", "consumeMessages#2", this);
                }
                // time to throw in the towel
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception trying to unlock the lme, closing", e1);
                emergencyClose();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "unlockConsumerSession");
        }

        /**
         * dealWithRuntimeExceptionFromOnMessage
         * 
         * Deal with any RuntimeException thrown by the call to onMessage
         * 
         * @param lme The current LockedMessageEnumeration
         * @param st The current SITransaction
         * @param re The RuntimeException to be dealt with
         */
        private final void dealWithRuntimeExceptionFromOnMessage(LockedMessageEnumeration lme, SITransaction st, RuntimeException re) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "dealWithRuntimeExceptionFromOnMessage", new Object[] { lme, st, re });
            try {
                // JMS spec requires auto & dups_ok to redeliver,
                // whilst client_ack and transacted don't (unless app' specifically
                // does recover/rollback). auto_ack wasn't got under transaction, so
                // will be redelivered after unlockAll. Dups_ok was, so we need to roll
                // it back.
                if (sessionAckMode == Session.DUPS_OK_ACKNOWLEDGE) {
                    session.rollbackTransaction();
                }
                // d327401 JMXDeliveryCount incremented twice for DupsOK client apps
                // If we have not deleted the message under a transaction then we want to
                // unlock it here so that it can be redelivered.
                if (st == null) {
                    ConsumerSession cs = lme.getConsumerSession();
                    if (cs != null) {
                        cs.unlockAll();
                    }
                    else {
                        // If we can't get the ConsumerSession to do the unlockAll then we risk message loss,
                        // so close down.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "couldn't get ConsumerSession, closing");
                        close();
                    }
                }
                // create exception, generate FFDC
                JMSException jmse = (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                              "ML_THREW_EXCPTN_CWSIA0089",
                                                                              null,
                                                                              re,
                                                                              "JmsMsgConsumerImpl.Consumer.consumeMessages#4",
                                                                              this,
                                                                              tc);
                // Pass this on to the async exception listener
                if (excTarget != null)
                    excTarget.reportException(jmse);
            } catch (Exception e1) {
                FFDCFilter.processException(e1, "JmsMsgConsumerImpl.Consumer.consumeMessages", "consumeMessages#7", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception in tidyup after failed onMessage, closing", e1);
                emergencyClose();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "dealWithRuntimeExceptionFromOnMessage");
        }

        /**
         * deleteOrMakeAvailable
         * 
         * @param lme The current LockedMessageEnumeration
         * @param st The current SITransaction
         * 
         * @exception SIException description of exception
         */
        private void deleteOrMakeAvailable(LockedMessageEnumeration lme, SITransaction st) throws SIException {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "deleteOrMakeAvailable", new Object[] { lme, st });
            // We must check whether the onMessage called recover, which must also cause
            // the message to  be redelivered (in the same way as exception being thrown).
            if (st == null) {
                // Retrieve the current commit count, and then set it to zero.
                int tempCommitCount = session.getAndResetCommitCount();
                if (tempCommitCount == 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "onMessage called recover");
                    // onMessage called recover, so we must make the message available
                    // for redelivery.
                    lme.getConsumerSession().unlockAll();
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Delete without transaction");
                    // This is the mainline path, where onMessage did not call recover.
                    lme.deleteCurrent(null);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "deleteOrMakeAvailable");
        }

        /**
         * stopConsumerIfAppropriate
         * 
         * Optimisation for closing busy sessions. If the session is closed or stopped,
         * call stop on this consumer to stem flow of messages. Note that the closed or
         * stopped states may indicate that the session is in the process of closing or
         * stopping, and it is NOT safe to call this.close() as it may deadlock.
         * this.stop is safe, but we have to be prepared to catch an exception during
         * close as the coreConsumerSession may already have been closed (although it
         * shouldn't have whilst this consumeMessages method was running).
         */
        private final void stopConsumerIfAppropriate() {
            int s = session.getState();
            switch (s) {
                case CLOSED:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "session closed/closing, stopping this async consumer");
                    try {
                        stop();
                    } catch (JMSException e) {
                        // No FFDC code needed
                        // swallow this problem, we're closing anyway, and hopefully things
                        // will sort themselves out
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "ignoring exception thrown by stop during close optimisation", e);
                    }
                    break;
                case STOPPED:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "session stopped, so stopping this async consumer");
                    try {
                        stop();
                    } catch (JMSException jmse) {
                        // No FFDC code needed
                        // This is potentially more serious than close since this doesn't have
                        // the "we're closing anyway" defence. Log this exception to the exception
                        // listener.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Exception thrown by stop during stop optimisation", jmse);
                        // Pass this on to the async exception listener
                        if (excTarget != null)
                            excTarget.reportException(jmse);
                    }
                    break;
            }
        }
    }
}
