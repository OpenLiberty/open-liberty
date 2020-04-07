/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.CompletionListener;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsMsgProducer;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.websphere.sib.api.jms.XctJmsConstants;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.api.jms.EncodingLevel;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.PersistenceType;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

public class JmsMsgProducerImpl implements JmsMsgProducer, ApiJmsConstants, JmsInternalConstants
{

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsMsgProducerImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // **************************** STATE VARIABLES ******************************

    private SICoreConnection coreConnection;
    private ProducerSession prod;
    private int defaultDeliveryMode = DeliveryMode.PERSISTENT;
    private int defaultPriority = 4;
    private long defaultTimeToLive = 0;
    private long defaultDeliveryDelay = 0;
    private JmsDestinationImpl dest = null;
    private JmsSessionImpl session = null;
    private boolean closed = false;
    private final Object closedLock = new Object();
    private boolean disableTimestamp = false;
    private boolean disableMessageID = false;

    /**
     * initialised in the constructor to reference the lock object held by the
     * session. Used to serialise concurrent calls to send/receive/commit etc.
     */
    private Object sessionSyncLock;

    /**
     * This object is used by the efficient send method to determine which properties
     * should be set into the message. For identified producers most of the information
     * it contains is basically fixed at the time the producer is created.
     * 
     * This object is NOT used by unidentified producers, where a different object
     * is created on the fly.
     */
    private ProducerProperties producerProperties = null;

    /**
     * Copy of the non-persistent mapping defined by our owning Connection Factory,
     * converted to a Reliability for efficiency.
     * This is needed for updating the replyReliability per send call.
     */
    private Reliability nonPersistentReliability;

    /**
     * Copy of the persistent mapping defined by our owning Connection Factory,
     * converted to a Reliability for efficiency.
     * This is needed for updating the replyReliability per send call.
     */
    private Reliability persistentReliability;

    /**
     * This property is used to flag when the previous identified send on
     * this producer used the multi-argument send call. In this case we will
     * have overwritten the dm, pri, ttl values in the ProducerProps object so
     * that it may not match the defaults in the producer.
     * 
     * By checking this flag when send(Message) is called we can avoid having
     * to re-set all the producer values in the ProducerProperties in the mainline
     * path where the user always uses the 1-arg send.
     */
    private boolean propsOverriden = false;

    /**
     * The userID obtained from the SICoreConnection, used
     * for setting JMSXUserID
     */
    private String resolvedUserID;

    private OrderingContext orderingContext;

    /**
     * The busname of the bus to which we are connected.
     * Used during the send call to resolve unset busname in replyTo.
     */
    private String busName;

    /**
     * keep tracks of number of AsynsSends in progress.No need to synchronize .. as
     * only one thread access this at a time.
     */
    private final AtomicLong _inProgressAysncSends = new AtomicLong(0);

    private final static int PRODUCER_SEND_WITH_ONLY_MESSAGE = 1;
    private final static int PRODUCER_SEND_MESSAGE_WITH_PARAMETERS = 3;
    private final static int CONNECTION_SEND_MESSAGE_WITH_PARAMETERS = 4;

    // ***************************** CONSTRUCTORS ********************************

    private JmsMsgProducerImpl() {}

    JmsMsgProducerImpl(Destination theDest, SICoreConnection coreConnection, JmsSessionImpl newSession) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsMsgProducerImpl", new Object[] { theDest, coreConnection, newSession });

        if (theDest != null) {
            // Note that in some situations this method call can return a _different_
            // destination than the one passed in as the parameter (e.g. Spring).
            this.dest = JmsDestinationImpl.checkNativeInstance(theDest);

            // check that the destination is not blocked from consuming
            JmsDestinationImpl.checkBlockedStatus(this.dest);
        }

        this.coreConnection = coreConnection;
        this.prod = null;
        this.session = newSession;
        sessionSyncLock = session.getSessionSyncLock();

        // store the userID
        try {
            resolvedUserID = coreConnection.getResolvedUserid();
        } catch (SIException sice) {
            // No FFDC code needed
            // d222942 review - default exception ok
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0067",
                                                            new Object[] { sice, "JmsMsgProducerImpl.<constructor>" },
                                                            sice,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc
                            );
        }

        // get the orderingContext from the session
        orderingContext = session.getOrderingContext();
        // Grab the passThrough properties
        Map passThruProps = session.getPassThruProps();
        // d272113 Store the busname for use during send calls.
        busName = (String) passThruProps.get(JmsInternalConstants.BUS_NAME);

        // setup the reliability fields
        String connPropVal = (String) passThruProps.get(JmsraConstants.NON_PERSISTENT_MAP);
        if (connPropVal != null) {
            nonPersistentReliability = ProducerProperties.lookupReliability(connPropVal);
        }
        else {
            // We expect the value to always be in the props, since the default is defined in
            // the JmsJcaManagedConnectionFactoryImpl constructor.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "WARNING: no non-persistent mapping in passThruProps");
            // pick a reasonable default
            nonPersistentReliability = Reliability.EXPRESS_NONPERSISTENT;
        }
        connPropVal = (String) passThruProps.get(JmsraConstants.PERSISTENT_MAP);
        if (connPropVal != null) {
            persistentReliability = ProducerProperties.lookupReliability(connPropVal);
        }
        else {
            // We expect the value to always be in the props, since the default is defined in
            // the JmsJcaManagedConnectionFactoryImpl constructor.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "WARNING: no persistent mapping in passThruProps");
            // pick a reasonable default
            persistentReliability = Reliability.RELIABLE_PERSISTENT;
        }

        if (dest != null) {
            // Set up the ProducerProperties object for this producer if it is not
            // an unidentified producer.
            producerProperties = new ProducerProperties(dest, this, passThruProps, persistentReliability, nonPersistentReliability);

            try {
                DestinationType dt = null;
                boolean producerBind = false;
                boolean preferLocalQP = true;

                // Check whether we can actually validate the type of the destination. In general
                // we can, but not if there is a forward routing path set on the destination.
                if (dest.isProducerTypeCheck()) {
                    if (theDest instanceof Queue) {
                        dt = DestinationType.QUEUE;
                        // Set up the producer bind property value to be passed to the CoreSPI.
                        String producerBindStr = ((JmsQueue) dest).getProducerBind();
                        if ((producerBindStr != null) && (ApiJmsConstants.PRODUCER_BIND_ON.equals(producerBindStr)))
                            producerBind = true;
                        // Set up the prefer local QP property value to be passed to the CoreSPI.
                        String preferLocalQPStr = ((JmsQueue) dest).getProducerPreferLocal();
                        if ((preferLocalQPStr != null) && (ApiJmsConstants.PRODUCER_PREFER_LOCAL_OFF.equals(preferLocalQPStr)))
                            preferLocalQP = false;
                    }
                    else {
                        dt = DestinationType.TOPICSPACE;
                    }
                }

                SIDestinationAddress sida = dest.getProducerSIDestinationAddress();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Creating producer session - destAddr: " + sida + " type: " + dt + " bind: " + producerBind + " preferLocl: " + preferLocalQP);

                // As a result of feature 190973.4, we can pass in the destination
                // discriminator on the call to createProducerSession. This discrim
                // is allowed to be null, so we don't need to check for that here.
                String destDiscrim = dest.getDestDiscrim();
                prod = coreConnection.createProducerSession(sida,
                                                            destDiscrim,
                                                            dt,
                                                            orderingContext,
                                                            null, // alternate userID
                                                            producerBind, // fixed queue point
                                                            preferLocalQP // prefer local queue point
                );
            } catch (SINotAuthorizedException sinae) {
                // No FFDC code needed
                // d238447 FFDC Review. NotAuth is an app/config error, no FFDC.
                throw (JMSSecurityException) JmsErrorUtils.newThrowable(
                                                                        JMSSecurityException.class,
                                                                        "PRODUCER_AUTH_ERROR_CWSIA0069",
                                                                        null,
                                                                        sinae,
                                                                        null, // null probeId = no FFDC
                                                                        this,
                                                                        tc
                                );
            } catch (SINotPossibleInCurrentConfigurationException dwte) {
                // No FFDC code needed
                // The destination was either not there or the wrong type.
                String msgKey = "MP_CREATE_FAILED_CWSIA0062";
                // d238447 FFDC Review. App or config error - no FFDC.
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(
                                                                               InvalidDestinationException.class,
                                                                               msgKey,
                                                                               new Object[] { theDest },
                                                                               dwte,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                                );
            } catch (SITemporaryDestinationNotFoundException tdnf) {
                // No FFDC code needed
                // The destination was either not there or the wrong type.
                String msgKey = "MP_CREATE_FAILED_CWSIA0062";
                // d238447 FFDC Review. App or config error - no FFDC.
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(
                                                                               InvalidDestinationException.class,
                                                                               msgKey,
                                                                               new Object[] { theDest },
                                                                               tdnf,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                                );
            } catch (SIIncorrectCallException e) {
                // No FFDC code needed
                // d222942 review - default exception ok (was combined with SIException block below)
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { e, "JmsMsgProducerImpl.<constructor>" },
                                                                e,
                                                                "JmsMsgProducerImpl#4",
                                                                this,
                                                                tc
                                );
            } catch (SIException sice) {
                // No FFDC code needed
                // d238447 FFDC Review. Remaining Exceptions are:
                //  SIConnectionUnavailableException, SIConnectionDroppedException, \- external, no FFDC
                //  SIConnectionLostException                                       /
                //  SIDiscriminatorSyntaxException - App error, no FFDC
                //  SILimitExceededException - Described as an architected limit, so no FFDC
                //  SIResourceException, SIIncorrectCallException - No info on when these are thrown,
                //                                                  generate FFDC for incorrect call (moved above)
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { sice, "JmsMsgProducerImpl.<constructor>" },
                                                                sice,
                                                                null, // null probeId = no FFDC
                                                                this,
                                                                tc
                                );
            } catch (Exception e) {
                // No FFDC code needed
                // d222942 review - default exception ok
                // d238447 FFDC review. Obviously have no idea what these might be, so better generate FFDC.
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { e, "JmsMsgProducerImpl.<constructor>" },
                                                                e,
                                                                "JmsMsgProducerImpl#5",
                                                                this,
                                                                tc
                                );
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsMsgProducerImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * @see javax.jms.MessageProducer#setDisableMessageID(boolean)
     */
    @Override
    public void setDisableMessageID(boolean value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDisableMessageID", value);
        checkClosed();
        disableMessageID = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDisableMessageID");
    }

    /**
     * @see javax.jms.MessageProducer#getDisableMessageID()
     */
    @Override
    public boolean getDisableMessageID() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDisableMessageID");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDisableMessageID", disableMessageID);
        return disableMessageID;
    }

    /**
     * @see javax.jms.MessageProducer#setDisableMessageTimestamp(boolean)
     */
    @Override
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDisableMessageTimestamp", value);
        checkClosed();
        disableTimestamp = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDisableMessageTimestamp");
    }

    /**
     * @see javax.jms.MessageProducer#getDisableMessageTimestamp()
     */
    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDisableMessageTimestamp");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDisableMessageTimestamp", disableTimestamp);
        return disableTimestamp;
    }

    /**
     * @see javax.jms.MessageProducer#setDeliveryMode(int)
     */
    @Override
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryMode", deliveryMode);

        checkClosed();
        // Throws an exception if this is not a valid delivery mode.
        validateDeliveryMode(deliveryMode);
        // By this point we know it is valid.
        this.defaultDeliveryMode = deliveryMode;
        // Need to drive any required updates to the ProducerProperties
        // state. Note that producerProperties will be null if this is
        // an unidentified producer.
        if (producerProperties != null) {
            producerProperties.setInDeliveryMode(deliveryMode);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDeliveryMode");
    }

    /**
     * @see javax.jms.MessageProducer#getDeliveryMode()
     */
    @Override
    public int getDeliveryMode() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeliveryMode");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDeliveryMode", defaultDeliveryMode);
        return defaultDeliveryMode;
    }

    /**
     * @see javax.jms.MessageProducer#setPriority(int)
     */
    @Override
    public void setPriority(int x) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPriority", x);

        checkClosed();
        // Throws an exception if this is not a valid priority.
        validatePriority(x);
        // At this point we know it is a valid priority.
        defaultPriority = x;
        // Need to drive any required updates to the ProducerProperties
        // state. Note that producerProperties will be null if this is
        // an unidentified producer.
        if (producerProperties != null) {
            producerProperties.setInPriority(x);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPriority");
    }

    /**
     * @see javax.jms.MessageProducer#getPriority()
     */
    @Override
    public int getPriority() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPriority");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPriority", defaultPriority);
        return defaultPriority;
    }

    /**
     * @see javax.jms.MessageProducer#setTimeToLive(long)
     */
    @Override
    public void setTimeToLive(long timeToLive) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTimeToLive", timeToLive);

        checkClosed();
        // Throws an exception if this is not a valid time to live.
        validateTimeToLive(timeToLive);
        // We know by this point that it must be valid.
        this.defaultTimeToLive = timeToLive;
        // Need to drive any required updates to the ProducerProperties
        // state. Note that producerProperties will be null if this is
        // an unidentified producer.
        if (producerProperties != null) {
            producerProperties.setInTTL(timeToLive);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTimeToLive");
    }

    /**
     * @see javax.jms.MessageProducer#getTimeToLive()
     */
    @Override
    public long getTimeToLive() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTimeToLive");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTimeToLive", defaultTimeToLive);
        return defaultTimeToLive;
    }

    /**
     * @see javax.jms.MessageProducer#getDestination()
     */
    @Override
    public Destination getDestination() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDestination");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDestination", dest);
        return dest;
    }

    /**
     * @see javax.jms.MessageProducer#close()
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
        boolean originallyClosed = false;

        //check for non-managed i.e client container and thin client
        if (!session.isManaged()) {

            //validate the close call is generated from its own completion listener.. 
            //in that case .. IllegalStateException is thrown.
            session.validateCloseCommitRollback("close");

            //close has to be blocked till all async sends are resolved.
            waitForAsyncSendsResolution();
        }

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
            if (prod != null) {
                // Carry out whatever close delegation we have to the core model.
                try {
                    // Only delegate a close when we are not tidying up during a session or connection close
                    if (!tidyUp) {
                        prod.close();
                    }
                } catch (SIException sice) {
                    // No FFDC code needed
                    // d238447 FFDC Review. Exceptions are:
                    //  SIResourceException, SIConnectionLostException,
                    //  SIConnectionDroppedException
                    // Don't generate FFDC for these at this level.
                    throw (JMSException) JmsErrorUtils.newThrowable(
                                                                    JMSException.class,
                                                                    "EXCEPTION_RECEIVED_CWSIA0067",
                                                                    new Object[] { sice, "JmsMsgProducerImpl.close" },
                                                                    sice,
                                                                    null, // null probeId = no FFDC
                                                                    this,
                                                                    tc);
                }
            }
            // Point out to the Session that we have closed.
            session.removeProducer(this);
        }

        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "already closed");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    private void send_internal(Message message, CompletionListener cListner) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send_internal(Message,CompletionListener)", new Object[] { message, cListner });

        try {
            synchronized (sessionSyncLock) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "got lock");

                //validates if message is null.. if that is case throws MessageFormatException
                validateMessageForNull(message);

                // if this is an unidentified producer, throw an
                // UnsupportedOperationException as this is an identified producer method.
                if (this.dest == null) {
                    throw (UnsupportedOperationException) JmsErrorUtils.newThrowable(
                                                                                     UnsupportedOperationException.class,
                                                                                     "NO_DEST_SPECIFIED_ON_SEND_CWSIA0065",
                                                                                     null,
                                                                                     tc);
                }

                //in case of non-managed environments.. the message may be a proxy.. then get the actual message implementation
                //if the message owned by SIBus
                if (!session.isManaged()) {
                    message = getMessageFromProxy(message);
                }

                if (cListner == null) {
                    // Sync send... blocking till all previous async sends are resolved.
                    waitForAsyncSendsResolution();

                    //all async sends are resolved.. can be performed Sync send
                    //call internal method to actually start sending
                    sendUsingProducerSession(message);

                } else {
                    //Async Send

                    if (session.isManaged()) {
                        throw (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                                  "MGD_ENV_CWSIA0084",
                                                                                  new Object[] { "MessageProducer.send" },
                                                                                  tc);
                    }

                    //increment the async send usage counter.
                    _inProgressAysncSends.incrementAndGet();

                    session.addtoAsysncSendQueue(this, cListner, message, PRODUCER_SEND_WITH_ONLY_MESSAGE, null);
                    return;

                }

            } // releases the lock
        } finally {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(this, tc, "send_internal(Message,CompletionListener)");
        }
    }

    /**
     * returns the actual message implementation object if msg is
     * Proxy ... this method ignores any message is owned by other than SIB.
     */
    private Message getMessageFromProxy(Message msg) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageFromProxy", msg);

        try {
            if (msg instanceof Proxy) {
                //check whether proxy is type of our InvocationHanlder.
                InvocationHandler handler = Proxy.getInvocationHandler(msg);

                //deal only with our handlers.. other Proxy handlers.. leave as it is..
                if (handler instanceof MessageProxyInvocationHandler) {
                    msg = ((MessageProxyInvocationHandler) handler).getMessage();

                }
            }

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Exception in getMessageFromProxy .. returning msg as it is", e);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessageFromProxy", msg);

        return msg;

    }

    /**
     * This method is internal method which would send message to ME on top
     * of producer session. This is called from Sync send and Async Send. In case of Sync send
     * it would have guarded with monitor sessionSyncLock.
     * 
     * @param msg
     */
    private void sendUsingProducerSession(Message message) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendUsingProducerSession", new Object[] { message });

        checkClosed();
        // check for sync/async conflicts
        session.checkSynchronousUsage("send");
        try {
          // if the supplied message is null, throw a jms MessageFormatException.
          if (message == null) {
              throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                        MessageFormatException.class,
                                                                        "INVALID_VALUE_CWSIA0068",
                                                                        new Object[] { "message", null },
                                                                        tc);
          }
          if (propsOverriden) {
              // This block will be invoked if the user is flipping back and forth
              // between the multi-arg identified send, and this 1-arg send. In this
              // case we need to re-instate the dm, pri, ttl values from the producer.
              producerProperties.setInDeliveryMode(defaultDeliveryMode);
              producerProperties.setInPriority(defaultPriority);
              producerProperties.setInTTL(defaultTimeToLive);

              // Mark that we are back with the defaults so we don't do this set the
              // next time around.
              propsOverriden = false;
          }

          // Delegate to the internal send method.
          sendMessage(producerProperties, message, dest);
        } finally {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "sendUsingProducerSession");
        }
    }

    private void send_internal(Message message, int deliveryMode, int priority, long timeToLive, CompletionListener cListner) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this
                       ,tc
                       ,"send_internal(Message,int,int,long,CompletionListener)"
                       ,new Object[] { message, deliveryMode, priority, timeToLive, cListner }
                       );

        try {
            synchronized (sessionSyncLock) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "got lock");

                //validates if message is null.. if that is case throws MessageFormatException
                validateMessageForNull(message);

                //validates priority,deliveeryMode and timetoLive
                validatePriority(priority);
                validateDeliveryMode(deliveryMode);
                validateTimeToLive(timeToLive);

                // if this is an unidentified producer, throw an
                // UnsupportedOperationException this is an identified producer method.
                if (this.dest == null) {
                    throw (UnsupportedOperationException) JmsErrorUtils.newThrowable(
                                                                                     UnsupportedOperationException.class,
                                                                                     "NO_DEST_SPECIFIED_ON_SEND_CWSIA0065",
                                                                                     null,
                                                                                     tc);
                }

                //in case of non-managed environments.. the message may be a proxy.. then get the actual message implementation
                //if the message owned by SIBus
                if (!session.isManaged()) {
                    message = getMessageFromProxy(message);
                }

                if (cListner == null) {
                    // Sync send... blocking till all previous Async sends are resolved.
                    waitForAsyncSendsResolution();

                    //all async sends are resolved.. call internal method to actually start sending
                    sendUsingProducerSession(message, deliveryMode, priority, timeToLive);

                } else {
                    //Async Send

                    if (session.isManaged()) {
                        throw (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                                  "MGD_ENV_CWSIA0084",
                                                                                  new Object[] { "MessageProducer.send" },
                                                                                  tc);
                    }

                    //increment the async send usage counter.
                    _inProgressAysncSends.incrementAndGet();

                    session.addtoAsysncSendQueue(this, cListner, message, PRODUCER_SEND_MESSAGE_WITH_PARAMETERS, new Object[] { deliveryMode, priority, timeToLive });
                    return;

                }
            } // releases the lock
        } finally {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(this, tc, "send_internal(Message,int,int,long,CompletionListener)");
        }
    }

    /**
     * This method is internal method which would send message to ME on top
     * of producer session. This is called from Sync send and Async Send. In case of Sync send
     * it would have guarded with monitor sessionSyncLock.
     * 
     * @param message
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @throws JMSException
     */
    private void sendUsingProducerSession(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendUsingProducerSession", new Object[] { message, deliveryMode, priority, timeToLive });

        // if the producer is closed throw a JMSException.
        checkClosed();
        // check for sync/async conflicts
        session.checkSynchronousUsage("send");
        // if the supplied message is null, throw a jms MessageFormatException.
        if (message == null) {
            throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                      MessageFormatException.class,
                                                                      "INVALID_VALUE_CWSIA0068",
                                                                      new Object[] { "message", null },
                                                                      tc);
        }

        // Mark that we have overriden the previous properties in the PP
        // object in case the next send call is using the 1-arg send.
        propsOverriden = true;

        // Set the parameter values into the producer properties object. The
        // producerProperties object contains sufficient intelligence that
        // if any of these particular values has already been set then it
        // does not recalculate anything.
        producerProperties.setInDeliveryMode(deliveryMode);
        producerProperties.setInPriority(priority);
        producerProperties.setInTTL(timeToLive);

        // Delegate to the internal send method.
        sendMessage(producerProperties, message, dest);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendUsingProducerSession");

    }

    void send_internal(Destination destination, Message message, CompletionListener cListner) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this
                       ,tc
                       ,"send_internal(Destination,Message,CompletionListener)"
                       ,new Object[] { destination, message, cListner }
                       );

        // Defer straight through to the many arg version (which will do all the locking and checking).
        send_internal(destination, message, defaultDeliveryMode, defaultPriority, defaultTimeToLive, cListner);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send_internal(Destination,Message,CompletionListener)");
    }

    private void send_internal(Destination destination
                              ,Message message
                              ,int deliveryMode
                              ,int priority
                              ,long timeToLive
                              ,CompletionListener cListner
                              ) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this
                       ,tc
                       ,"send_internal(Destination,Message,int,int,long,CompletionListener)"
                       ,new Object[] { destination, message, deliveryMode, priority, timeToLive, cListner }
                       );

        try {
            synchronized (sessionSyncLock) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "got lock");

                //validates if message is null.. if that is case throws MessageFormatException
                validateMessageForNull(message);

                //validates priority,deliveeryMode and timetoLive
                validatePriority(priority);
                validateDeliveryMode(deliveryMode);
                validateTimeToLive(timeToLive);

                //validated destination
                validateDestination(destination);

                //in case of non-managed environments.. the message may be a proxy.. then get the actual message implementation
                //if the message owned by SIBus
                if (!session.isManaged()) {
                    message = getMessageFromProxy(message);
                }

                if (cListner == null) {
                    // Sync send... blocking till all previous async sends are resolved.
                    waitForAsyncSendsResolution();

                    //all async sends are resolved.. call internal method to actually start sending
                    sendUsingConnection(destination, message, deliveryMode, priority, timeToLive);

                } else {
                    //Async Send

                    if (session.isManaged()) {
                        throw (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                                  "MGD_ENV_CWSIA0084",
                                                                                  new Object[] { "MessageProducer.send" },
                                                                                  tc);
                    }

                    //increment the async send usage counter.
                    _inProgressAysncSends.incrementAndGet();

                    session.addtoAsysncSendQueue(this, cListner, message, CONNECTION_SEND_MESSAGE_WITH_PARAMETERS, new Object[] { deliveryMode, priority, timeToLive, destination });
                    return;

                }
            } // releases the lock
        } finally {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(this, tc, "send_internal(Destination,Message,int,int,long,CompletionListener)");
        }
    }

    /**
     * This method is internal method which would send message to ME on top
     * of connection. This is called from Sync send and Async Send. In case of Sync send
     * it would have guarded with monitor sessionSyncLock.
     * 
     * @param destination
     * @param message
     * @param deliveryMode
     * @param priority
     * @param timeToLive
     * @throws JMSException
     */
    private void sendUsingConnection(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendUsingConnection", new Object[] { destination, message, deliveryMode, priority, timeToLive });

        // if the producer is closed throw a JMSException.
        checkClosed();
        // check for sync/async conflicts
        session.checkSynchronousUsage("send");

        // if the supplied message is null, throw a jms MessageFormatException.
        if (message == null) {
            throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                      MessageFormatException.class,
                                                                      "INVALID_VALUE_CWSIA0068",
                                                                      new Object[] { "message", null },
                                                                      tc);
        }

        //validates destination and if it is successful gives native destination.
        JmsDestinationImpl nativeDest = validateDestination(destination);

        // Create a new ProducerProps on the fly for these parameters.
        ProducerProperties pp = new ProducerProperties(nativeDest, this, session.getPassThruProps(), null, null);
        pp.setInDeliveryMode(deliveryMode);
        pp.setInPriority(priority);
        pp.setInTTL(timeToLive);

        // Delegate to the internal send method.
        sendMessage(pp, message, nativeDest);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendUsingConnection");

    }

    /**
     * This method validates destination and returns native destination if validation is proper
     * 
     * @param destination
     * @return
     * @throws JMSException
     */
    private JmsDestinationImpl validateDestination(Destination destination) throws JMSException {

        // if this is an identified producer, throw an
        // UnsupportedOperationException as this is an unidentified producer
        // method.

        if (this.dest != null) {
            if (destination == null) {
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class
                                                                               , "INVALID_VALUE_CWSIA0281"
                                                                               , new Object[] { "Destination", null }
                                                                               , tc);
            }

            throw (UnsupportedOperationException) JmsErrorUtils.newThrowable(
                                                                             UnsupportedOperationException.class,
                                                                             "DEST_SPECIFIED_ON_SEND_CWSIA0066",
                                                                             null,
                                                                             tc);
        }

        // if the supplied destination is set to null, or it isn't a jetstream
        // destination, throw a jms InvalidDestinationException.
        // Note that in some situations this method call can return a _different_
        // destination than the one passed in as the parameter (e.g. Spring).
        JmsDestinationImpl nativeDest = JmsDestinationImpl.checkNativeInstance(destination);

        // for an identified producer, this check will have been done at constructor time.
        if (this.dest == null) {
            // Check that this destination is not blocked from creating consumers
            JmsDestinationImpl.checkBlockedStatus(nativeDest);
        }

        return nativeDest;

    }

    /**
     * This method is called from back ground thread (which is maintained in JmsSessionImpl).
     * It calls appropriate send method by supplying proper parameters.
     * Depending on success/failure.. it calls either onCompletion or onException.
     * 
     * @param message
     * @param SendMethodType
     * @param params
     * @param CompletionListener
     */
    void sendCalledFromAsynsSendRunThread(Message message, int SendMethodType, Object[] params, CompletionListener cListener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendCalledFromAsynsSendRunThread", new Object[] { message, SendMethodType, params, cListener });

        try {
            if (SendMethodType == PRODUCER_SEND_WITH_ONLY_MESSAGE) {
                sendUsingProducerSession(message);
            } else if (SendMethodType == PRODUCER_SEND_MESSAGE_WITH_PARAMETERS) {
                sendUsingProducerSession(message,
                                         (Integer) params[0], //deliveryMode
                                         (Integer) params[1], //priority
                                         (Long) params[2]);//timeToLive
            } else if (SendMethodType == CONNECTION_SEND_MESSAGE_WITH_PARAMETERS) {
                sendUsingConnection((Destination) params[3], //destination
                                    message,
                                    (Integer) params[0], //deliveryMode
                                    (Integer) params[1], //priority
                                    (Long) params[2]); //timeToLive
            }

            //send method successful.. call onCompletion.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "calling onCompletion ",
                            new Object[] { message, cListener, message });

            try {
                //AsyncSend is successfully completed. So remove the restriction on message object ... so that
                //application can access message object.This we have to do only for SIB messages.
                if (message instanceof JmsMessageImpl) {
                    ((JmsMessageImpl) message).setAsyncSendInProgress(false);
                }

                cListener.onCompletion(message);
            } catch (Throwable e) {
                //Exception in onCompletion method. Should be handled by user. add to trace. 
                SibTr.debug(tc, "Caught exception in onCompletion method", new Object[] { message, cListener, message, e });

            }

        } catch (Exception e) {

            //exception in send method... call onException and return.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Caught exception in executing send method .. calling onException",
                            new Object[] { message, cListener, message, e });

            try {
                //AsyncSend is successfully completed. So remove the restriction on message object ... so that
                //application can access message object.This we have to do only for SIB messages.
                if (message instanceof JmsMessageImpl) {
                    ((JmsMessageImpl) message).setAsyncSendInProgress(false);
                }

                cListener.onException(message, e);
            } catch (Throwable ex) {
                //Exception in onCompletion method. Should be handled by user. add to trace. 
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Caught exception in onException method", new Object[] { message, cListener, message, ex });

            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "sendCalledFromAsynsSendRunThread");
            return;
        } finally {
            //decrement the async send count..and then notify if the count is zero.
            // any blocking Sync send thread would get woken up.
            synchronized (_inProgressAysncSends) {

                if (_inProgressAysncSends.get() > 0)
                    _inProgressAysncSends.decrementAndGet();

                if (_inProgressAysncSends.get() <= 0) {
                    _inProgressAysncSends.notifyAll();
                }

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendCalledFromAsynsSendRunThread");

    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * This method is called at the beginning of every method that should not work
     * if the producer has been closed. It prevents further execution by throwing
     * a JMSException with a suitable "I'm closed" message.
     */
    void checkClosed() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "checkClosed");
        synchronized (closedLock) {
            if (closed) {
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                javax.jms.IllegalStateException.class,
                                                                "PRODUCER_CLOSED_CWSIA0061",
                                                                null,
                                                                tc);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "checkClosed");
    }

    /**
     * Internal method used for all varieties of send.<p>
     * 
     * f 188046.3 Efficiency of sendMessage
     * This method provides a more efficient version of the original sendMessage
     * method that interacts directly with the MFP object and obtains its
     * information from a ProducerProperties instance.
     * 
     * --------
     * This is the internal method that all the external JMS api send methods
     * resolve to (and so both identified and unidentified producers call this
     * method).
     * <p>
     * 
     * A non-null <code>ProducerSession</code> member variable indicates that
     * this is an identified producer, and the <code>ProducerSession</code> is
     * used to send the message.
     * <p>
     * 
     * A null <code>ProducerSession</code> member variable indicates tha this is
     * an unidentified producer, and the <code>SIMPConnection</code> is used to
     * send the message.
     * <p>
     * 
     * This method handles sending both native and foreign messages. If the
     * supplied message is foreign, a native message is constructed from it and
     * sent. Any changed values are then copied back into the foreign message.
     * <p>
     * 
     * @param ProducerProperties The object containing parameters for the
     *            createProducerSession call.
     * @param Message The message to be sent (may be a foreign message at this point)
     * @param JmsDestinationImpl The JMS destination object to which the message will
     *            be sent.
     * @throws JMSException if a message constructor fails when handling a foreign message, if
     *             getting / setting a message's fields fails, if the delivery mode is
     *             non-persistent and the non-persistent mapping on the
     *             connection is invalid, if the delivery mode supplied is invalid, or if
     *             invoking <code>send()</code> on the <code>ProducerSession</code> or
     *             <code>SIMPConnection</code> creates a <code>SIMPException</code>.
     */
    void sendMessage(ProducerProperties props, Message message, JmsDestinationImpl destRef) throws JMSException {

        PersistenceType deliveryMode;
        Integer priority;
        long timeToLive;
        JmsMessageImpl jmsMIref;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "sendMessage(ProducerProps, Message, Destination)");
        }

        // This is the core MFP message object contained in the supplied Jetstream message.
        JsJmsMessage jsJmsMessage = null;

        // if the message is a jetstream message then cast it, otherwise
        // construct a jetstream message from it.
        JmsMessageImpl foreignMsgHelper = null;
        if (message instanceof JmsMessageImpl) {
            jmsMIref = (JmsMessageImpl) message;
            jsJmsMessage = jmsMIref.getMsgReference();
            // clear the special case properties from the message to be sent
            jmsMIref.clearLocalProperties();
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Create a JS message object for foreign message");
            foreignMsgHelper = JmsMessageImpl.messageToJmsMessageImpl(message);
            jmsMIref = foreignMsgHelper;
            jsJmsMessage = foreignMsgHelper.getMsgReference();
        }

        // The rest of this method (with the exception of the block at the bottom
        // that sets values back into the foreign message should use the MFP message
        // directly - not through the JmsMessageImpl instance.
        // Except for the call to updateReplyReliability which is JmsMessageImpl specific. Yuk.

        // check for admin overrides of deliveryMode, priority
        deliveryMode = props.getEffectiveDeliveryMode();
        jsJmsMessage.setJmsDeliveryMode(deliveryMode);
        priority = props.getEffectivePriority();
        jsJmsMessage.setPriority(priority.intValue());
        timeToLive = props.getEffectiveTTL();

        // validate the timeToLive and deliveryDelay 
        validateTTLAndDD(timeToLive);

        // Set up the forward and reverse routing paths on the message.
        List frpPath = props.getConvertedFRP();
        if (frpPath != null)
            jsJmsMessage.uncheckedSetForwardRoutingPath(frpPath);

        List rrpPath = props.getConvertedRRP_Part();
        if (rrpPath != null) {
            // If the destination contained a reverse routing path then we have
            // to be careful how this interacts with any replyTo dest already
            // set on the message. The replyTo dest should end up at the end of
            // the routing path.
            List existingRRP = jsJmsMessage.getReverseRoutingPath();
            if (existingRRP.size() > 0) {
                // Append each element of the existing RRP into the list obtained
                // from the destination. It is likely to be only one element in size
                // (for the replyTo dest).
                for (int i = 0; i < existingRRP.size(); i++) {
                    rrpPath.add(existingRRP.get(i));
                }
            }
            jsJmsMessage.uncheckedSetReverseRoutingPath(rrpPath);
            // This step copies the discriminator from the sending destination to the
            // reply discriminator in case the destination is being used for request/reply
            String discrim = props.getDiscriminator();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Using discriminator from destination: " + discrim);
            if ((discrim != null) && (!discrim.equals(jsJmsMessage.getReplyDiscriminator()))) {
                jsJmsMessage.setReplyDiscriminator(discrim);
            }
        }

        // d317373 perf. Only set the busname in rrp if we need to
        if ((message instanceof JmsMessageImpl)
            && ((JmsMessageImpl) message).isRrpBusNameNeedsUpdating()) {
            // d272113 If the busname is null, resolve it to the local bus so that
            // replies will still work if the msg travels to a non-local bus.
            boolean changed = false;
            List rrp = jsJmsMessage.getReverseRoutingPath();
            if (rrp.size() > 0) {
                for (int i = 0; i < rrp.size(); i++) {
                    JsDestinationAddress addr = (JsDestinationAddress) rrp.get(i); // addr is byValue, not byRef.
                    if (addr.getBusName() == null) {
                        addr.setBusName(busName);
                        rrp.set(i, addr); // it's not enough to just change addr, we have to set it back into the list.
                        changed = true;
                    }
                }
            }
            // If we've updated any values then reflect those updates back into the message.
            if (changed)
                jsJmsMessage.uncheckedSetReverseRoutingPath(rrp);
            // no need to do this again unless the message tells us to by resetting this flag
            ((JmsMessageImpl) message).setRrpBusNameNeedsUpdating(false);
        }

        // set up the on-send header properties.

        // d317373 the mesage now maintains the MSG_TYPE_PROPERTY
        // so no need to update it here
        if (!destRef._getInhibitJMSDestination()) {
            byte[] destBytes = destRef.encodeToBytes(EncodingLevel.FULL);
            jsJmsMessage.setJmsDestination(destBytes);
            // Set the destination reference cache in the message for the benefit of the sender.
            // (separate code handles the foreign message case at the end of this message)
            // This method sets the reference without touching the coreMsg object.
            if (message instanceof JmsMessageImpl) {
                ((JmsMessageImpl) message).setDestReference(destRef);
            }
        }

        if (!disableMessageID) {
            jsJmsMessage.setApiMessageIdAsBytes(session.createMessageID());
        }

        // Tag this message as originating in WPM. (uses a byte field to save space,
        // expands the byte to the required text when get is called).
        jsJmsMessage.setJmsxAppId(MfpConstants.WPM_JMSXAPPID);

        // Set JMSXUserID, using api specific call
        jsJmsMessage.setApiUserId(resolvedUserID);

        // Only insert a default value for the groupID if this message hasn't previously been sent.
        if (!jsJmsMessage.alreadySent()) {
            // Check for JMSXGroupSeq set, but JMSXGroupID not set, which
            // triggers automatic creation of JMSXGroupID. Using getObjectProperty
            // instead of getIntProperty avoids the need to catch an exception on
            // a common case.
            Object gSeq = jsJmsMessage.getJMSXGroupSeq();
            if (gSeq != null) {
                Object gID = jsJmsMessage.getObjectProperty("JMSXGroupID");
                if (gID == null) {
                    // create a groupID by requesting a new messageID, and converting it
                    // to the 'ID:xxxxxx' form.
                    byte[] idBytes = session.createMessageID();
                    StringBuffer sb = new StringBuffer("ID:");
                    HexString.binToHex(idBytes, 0, idBytes.length, sb);
                    String newGroupID = sb.toString();
                    jsJmsMessage.setObjectProperty("JMSXGroupID", newGroupID);
                }
            }
        }

        // The expiration field is based on the current timestamp and so will
        // not be common across invocations unless the value being set is 0.
        timeToLive = props.getEffectiveTTL();
        long timeNow = 0;
        if ((timeToLive != 0) || (!disableTimestamp)) {
            timeNow = System.currentTimeMillis();
        }
        if (timeToLive == 0) {
            jsJmsMessage.setJmsExpiration(0);
        }
        else {
            jsJmsMessage.setJmsExpiration(timeNow + timeToLive);
        }
        jsJmsMessage.uncheckedSetTimeToLive(timeToLive);

        //if timeNow is 0 then get the current time
        if (timeNow == 0)
            timeNow = System.currentTimeMillis();
        //set delivery time and the delivery delay
        jsJmsMessage.setJmsDeliveryTime(timeNow + this.defaultDeliveryDelay);
        jsJmsMessage.uncheckedSetDeliveryDelay(this.defaultDeliveryDelay);

        if (!disableTimestamp) {
            // The timestamp will always be different - not a lot we can do there.
            jsJmsMessage.setTimestamp(timeNow);
        }
        else {
            // d305036.2 must explicitly set to 0 now that the SIB default is -1
            jsJmsMessage.setTimestamp(0L); // JMS spec says timestamp must be 0 when disabled
        }

        // set up jetstream message reliability field.
        // d317816 need to use the replyReliability if it is set in the destination
        Reliability rel = destRef.getReplyReliability();
        if (rel == null) {
            // this is the normal case
            rel = props.getEffectiveReliability();
        }
        jsJmsMessage.setReliability(rel);

        // Give the message class a change to update the replyReliability field
        jmsMIref.updateReplyReliability(nonPersistentReliability, persistentReliability);

        // Set the topic on the message based on the discrim. Note that for
        // queue type destinations the behaviour of this is not defined, but
        // should not affect the behaviour.
        String discrim = props.getDiscriminator();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Setting discrim as '" + discrim + "'");
            if (prod != null) {
                // If we have already created the producer then we should
                // be guaranteed that this is a valid discriminator by
                // this point (sending a message).
                jsJmsMessage.uncheckedSetDiscriminator(discrim);
            }
            else {
                // For unidentified producer this still has some value.
                jsJmsMessage.setDiscriminator(discrim);
            }
        } catch (IllegalArgumentException iae) {
            // No FFDC code needed
            // This will occur if the user is trying to publish to a topic that contains wildcards..
            // d238447 FFDC review. App error, no FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0068",
                                                            new Object[] { "discriminator", discrim },
                                                            iae,
                                                            null, // null probeId = no FFDC
                                                            this,
                                                            tc);
        }

        // We want the routing destination to be null (normal case).
        jsJmsMessage.setRoutingDestination(null);

        // Obtain transaction under which to send the message, if required.
        SITransaction transaction;
        String acknowledgeMode;
        switch (session.getAcknowledgeMode()) {
            case Session.SESSION_TRANSACTED:
                // If locally transacted, need a transaction.
                // If an XA transaction is configured (overriding local transaction
                // flag), need a transaction.
                transaction = session.getTransaction();
                acknowledgeMode = XctJmsConstants.XCT_ACK_MODE_TRANSACTED;
                break;
            case Session.CLIENT_ACKNOWLEDGE:
                // Must be client ack, as cannot override with XA (since XA is only
                // available if a managed environment is in use, in which case
                // client ack is illegal).
                // Ack mode transactions should only be used on receives, so must not
                // ask for one.
                transaction = null;
                acknowledgeMode = XctJmsConstants.XCT_ACK_MODE_CLIENT;
                break;
            case Session.DUPS_OK_ACKNOWLEDGE:
                // Must be dups ok ack, as cannot override with XA (since XA is only
                // available if a managed environment is in use, in which case
                // dups ok ack is illegal).
                // Ack mode transactions should only be used on receives, so must not
                // ask for one.
                transaction = null;
                acknowledgeMode = XctJmsConstants.XCT_ACK_MODE_DUPS_OK;
                break;
            case Session.AUTO_ACKNOWLEDGE:
                // If auto ack, no transaction is used.
                // If an XA transaction is configured (overriding auto ack flag),
                // need a transaction.
                // Ask for a transaction in either case, if session is really auto
                // ack, null will be returned.
                transaction = session.getTransaction();
                acknowledgeMode = XctJmsConstants.XCT_ACK_MODE_AUTO;
                break;
            default:
                // This should never happen.
                transaction = session.getTransaction();
                acknowledgeMode = XctJmsConstants.XCT_ACK_MODE_NONE;
                break;
        }

        // d265587  Msg toString not reset on send
        // Now invalidate the message toString so that the changes we have just made
        // on the underlying MFP message object are reflected the next time the user
        // calls toString.
        jmsMIref.invalidateToStringCache();

        // Try to actually send it if this is an identified producer
        if (prod != null) {
            // send it using the previously constructed producer session.
            try {
                prod.send(jsJmsMessage, transaction);
            } catch (SINotAuthorizedException sinae) {
                // No FFDC code needed
                // d238447 FFDC Review. NotAuth is app/config error, no FFDC.
                throw (JMSSecurityException) JmsErrorUtils.newThrowable(
                                                                        JMSSecurityException.class,
                                                                        "PRODUCER_AUTH_ERROR_CWSIA0069",
                                                                        null,
                                                                        sinae,
                                                                        null, // null probeId = no FFDC
                                                                        this,
                                                                        tc
                                );
            } catch (SINotPossibleInCurrentConfigurationException dwte) {
                // No FFDC code needed
                // Dest not there or wrong type
                String msgKey = "SEND_FAILED_CWSIA0063";
                // d238447 FFDC Review. App or config error, no FFDC.
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(
                                                                               InvalidDestinationException.class,
                                                                               msgKey,
                                                                               new Object[] { destRef },
                                                                               dwte,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                                );
            } catch (SIIncorrectCallException e) {
                // No FFDC code needed
                // d222942 review - default exception ok (was in SIException block below)
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { e, "JmsMsgProducerImpl.sendMessage (#4)" },
                                                                e,
                                                                "JmsMsgProducerImpl.sendMessage#4",
                                                                this,
                                                                tc
                                );
            } catch (SIException simpe) {
                // No FFDC code needed
                // d238447 FFDC review. Exceptions from the send method are:
                //   SINotAuthorizedException,                     \ Handled above.
                //   SINotPossibleInCurrentConfigurationException  /
                //   SIIncorrectCallException        -- FFDC required, moved above
                //   SISessionUnavailableException, SISessionDroppedException,                 \
                //   SIConnectionUnavailableException, SIConnectionDroppedException,            > No FFDC
                //   SIResourceException, SIConnectionLostException, SILimitExceededException, /
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { simpe, "JmsMsgProducerImpl.sendMessage (#4)" },
                                                                simpe,
                                                                null, // null probeId = no FFDC
                                                                this,
                                                                tc
                                );
            }

        }

        // if this is an unidentified producer, send it using the 'put-one'
        // method on the core connection.
        else {

            try {
                DestinationType dt = null;

                // Check whether we can validate the type of the destination to which the
                // producer is attaching. In the simple case this is always true, however
                // is false if there is a forward routing path on this destination.
                if (destRef.isProducerTypeCheck()) {
                    if (destRef instanceof Queue) {
                        dt = DestinationType.QUEUE;
                    }
                    else if (destRef instanceof Topic) {
                        dt = DestinationType.TOPICSPACE;
                    }
                }

                SIDestinationAddress sida = destRef.getProducerSIDestinationAddress();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "About to do core unidentified send - transaction: " + transaction + " destAddr: " + sida + " destType: " + dt);

                // CoreSPI unidentified put method (put1)
                coreConnection.send(jsJmsMessage, transaction, sida, dt, orderingContext, null);
            }

            catch (SINotAuthorizedException sinae) {
                // No FFDC code needed
                // d238447 FFDC review. NotAuth is an app/config error, no FFDC.
                throw (JMSSecurityException) JmsErrorUtils.newThrowable(
                                                                        JMSSecurityException.class,
                                                                        "PRODUCER_AUTH_ERROR_CWSIA0069",
                                                                        null,
                                                                        sinae,
                                                                        null, // null probeId = no FFDC
                                                                        this,
                                                                        tc
                                );
            } catch (SINotPossibleInCurrentConfigurationException dwte) {
                // No FFDC code needed
                // Dest not there or wrong type
                String msgKey = "SEND_FAILED_CWSIA0063";
                // d38447 FFDC review. App/config error, no FFDC.
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(
                                                                               InvalidDestinationException.class,
                                                                               msgKey,
                                                                               new Object[] { destRef },
                                                                               dwte,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                                );
            } catch (SITemporaryDestinationNotFoundException tdnf) {
                // No FFDC code needed
                // Dest not there or wrong type
                String msgKey = "SEND_FAILED_CWSIA0063";
                // d238447 FFDC review. App error, no FFDC.
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(
                                                                               InvalidDestinationException.class,
                                                                               msgKey,
                                                                               new Object[] { destRef },
                                                                               tdnf,
                                                                               null, // null probeId = no FFDC
                                                                               this,
                                                                               tc
                                );
            } catch (SIIncorrectCallException sice) {
                // No FFDC code needed
                // d222942 review - default exception ok (was in SIException block below)
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { sice, "JmsMsgProducerImpl.sendMessage (#7)" },
                                                                sice,
                                                                "JmsMsgProducerImpl.sendMessage#7",
                                                                this,
                                                                tc
                                );
            } catch (SIException sice) {
                // No FFDC code needed
                // d238447 FFDC review. Need to consider:
                //   SINotAuthorizedException, SINotPossibleInCurrentConfigurationException \ Covered above
                //   SITemporaryDestinationNotFoundException,                               /
                //   SIIncorrectCallException,                                            - Requires FFDC, moved above.
                //   SIConnectionUnavailableException, SIConnectionDroppedException,           \ No FFDC required
                //   SIResourceException, SIConnectionLostException, SILimitExceededException, /
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { sice, "JmsMsgProducerImpl.sendMessage (#7)" },
                                                                sice,
                                                                null, // null probeId = no FFDC
                                                                this,
                                                                tc
                                );
            } catch (Exception e) {
                // No FFDC code needed
                // d238447 FFDC review - no idea what this covers, so generate FFDC.
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0067",
                                                                new Object[] { e, "JmsMsgProducerImpl.sendMessage (#8)" },
                                                                e,
                                                                "JmsMsgProducerImpl.sendMessage#8",
                                                                this,
                                                                tc
                                );
            }

        }

        // if the message isn't a jetstream message then copy the changed values
        // back into the original message.
        if (!(message instanceof JmsMessageImpl)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Setting properties back into foreign message");
            message.setJMSDeliveryMode(foreignMsgHelper.getJMSDeliveryMode());
            message.setJMSDestination(foreignMsgHelper.getJMSDestination());
            message.setJMSExpiration(foreignMsgHelper.getJMSExpiration());
            message.setJMSMessageID(foreignMsgHelper.getJMSMessageID());
            message.setJMSPriority(foreignMsgHelper.getJMSPriority());
            message.setJMSTimestamp(foreignMsgHelper.getJMSTimestamp());
            Enumeration propertyNames = foreignMsgHelper.getPropertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                if ((name.startsWith("JMSX")) || (name.startsWith("JMS_IBM"))) {
                    Object value = foreignMsgHelper.getObjectProperty(name);
                    try {
                        message.setObjectProperty(name, value);
                    } catch (JMSException e) {
                        // No FFDC code needed
                        // This can occur if the foreign message doesn't support setting
                        // a particular JMSX, or if it polices the use of JMS_* other than
                        // it's own vendor specific set (like we do).
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "foreign message refused setObjProperty(" + name + "): " + e);
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendMessage(ProducerProps, Message, Destination)");
    }

    /**
     * This method is something to do with PMI and Request Metrics.
     * 
     * @param props
     * @param message
     * @param destRef
     * @param sibPmiRm
     * @param sucessful
     * @param sibPmiRmCallbackObject
     */
    /*
     * private void produceMessageUnblock(ProducerProperties props, Message message, JmsDestinationImpl destRef, SIBPmiRm sibPmiRm, boolean sucessful, Object
     * sibPmiRmCallbackObject) {
     * 
     * if (sibPmiRm.isActiveOnThread()) {
     * boolean makeProduceCall = true;
     * boolean collectDetail = true;
     * Properties traceDetailInfo = null;
     * 
     * int traceLevel = sibPmiRm.getTransactionTraceLevel(SIBPmiRm.JMS_COMPONENT_ID);
     * if (traceLevel == SIBPmiRm.TRAN_DETAIL_LEVEL_NONE) {
     * //We don't want to collect info or make the call
     * makeProduceCall = false;
     * collectDetail = false;
     * }
     * else if (traceLevel == SIBPmiRm.TRAN_DETAIL_LEVEL_PERF) {
     * //We don't want to collect the info but we do want to make the call
     * makeProduceCall = true;
     * collectDetail = false;
     * }
     * 
     * if (collectDetail) {
     * JMSTransactionTraceDetail transactionTraceDetail = new JMSTransactionTraceDetail();
     * transactionTraceDetail.setBasicTraceDetail(destRef, message );
     * if (traceLevel == SIBPmiRm.TRAN_DETAIL_LEVEL_EXTENDED) {
     * transactionTraceDetail.addExtendedTraceDetail(message, props.getEffectiveTTL());
     * }
     * traceDetailInfo = transactionTraceDetail.getTraceDetail();
     * }
     * 
     * // The makeProduceCall should only be false if the trace level is none
     * if (makeProduceCall) {
     * sibPmiRm.produceMessageUnblock(sibPmiRmCallbackObject
     * ,(sucessful ? SIBPmiRm.STATUS_GOOD : SIBPmiRm.STATUS_FAILED)
     * ,SIBPmiRm.JMS_COMPONENT_ID
     * ,traceDetailInfo
     * ,SIBPmiRm.REQSCOPE_INPROCESS
     * );
     * }
     * }
     * }
     */

    /**
     * Validates the deliveryMode property, throwing an exception if there is a problem.
     */
    private void validateDeliveryMode(int deliveryMode) throws JMSException {
        switch (deliveryMode) {
            case DeliveryMode.NON_PERSISTENT:
            case DeliveryMode.PERSISTENT:
                // OK - no problems.
                break;
            default:
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "INVALID_VALUE_CWSIA0068",
                                                                new Object[] { "JMSDeliveryMode", "" + deliveryMode },
                                                                tc
                                );
        }
    }

    /**
     * This method carries out validation on the priority field.
     * 
     * @param x
     */
    private void validatePriority(int x) throws JMSException {
        // Priority must be in the range 0-9
        if ((x < 0) || (x > 9)) {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0068",
                                                            new Object[] { "JMSPriority", "" + x },
                                                            tc
                            );
        }
    }

    /**
     * Validates the timeToLive field and throws an exception if there is a problem.
     * 
     * @param timeToLive
     * @throws JMSException
     */
    private void validateTimeToLive(long timeToLive) throws JMSException {
        if ((timeToLive < 0) || (timeToLive > MfpConstants.MAX_TIME_TO_LIVE)) {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0068",
                                                            new Object[] { "timeToLive", "" + timeToLive },
                                                            tc
                            );
        }

    }

    /**
     * Validates the deliveryDelayTime field and throws an exception if there is a problem.
     * 
     * @param timeToLive
     * @throws JMSException
     */
    private void validateDeliveryDelayTime(long deliveryDelayTime) throws JMSException {
        if ((deliveryDelayTime < 0) || (deliveryDelayTime > MfpConstants.MAX_DELIVERY_DELAY)) {
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            JMSException.class,
                                                            "INVALID_VALUE_CWSIA0068",
                                                            new Object[] { "deliveryDelay", "" + deliveryDelayTime },
                                                            tc
                            );
        }

    }

    /**
     * Validates timetoLive and deliverydelay values.Throws exception if there is a problem
     * 
     * @param timeToLive
     * @throws JMSException
     */
    private void validateTTLAndDD(long timeToLive) throws JMSException {
        // There is no point continuting when expiry/timetolive time is earlier than the deliveryDelay
        if (getDeliveryDelay() > 0 && timeToLive > 0) {//only if its non default value
            if (timeToLive <= getDeliveryDelay()) {
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "INVALID_VALUE_CWSIA0070",
                                                                new Object[] { getDeliveryDelay(), timeToLive },
                                                                tc
                                );
            }
        }
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageProducer#send(javax.jms.Message, javax.jms.CompletionListener)
     */
    @Override
    public void send(Message msg) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", msg);

        //Defer straight through to the version with completion listner (which will do all the locking and checking).
        send_internal(msg, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageProducer#send(javax.jms.Destination, javax.jms.Message, javax.jms.CompletionListener)
     */
    @Override
    public void send(Destination dst, Message msg) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { msg, dst });

        //Defer straight through to the version with completion listner (which will do all the locking and checking).
        send_internal(dst, msg, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long, javax.jms.CompletionListener)
     */
    @Override
    public void send(Message msg, int deliveryMode, int priority, long timeToLive) throws JMSException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { msg, deliveryMode, priority, timeToLive });

        //Defer straight through to the version with completion listner (which will do all the locking and checking).
        send_internal(msg, deliveryMode, priority, timeToLive, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageProducer#send(javax.jms.Destination, javax.jms.Message, int, int, long, javax.jms.CompletionListener)
     */
    @Override
    public void send(Destination dst, Message msg, int deliveryMode, int priority, long timeToLive) throws JMSException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dst, msg, deliveryMode, priority, timeToLive });

        //Defer straight through to the version with completion listner (which will do all the locking and checking).
        send_internal(dst, msg, deliveryMode, priority, timeToLive, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    /**
     * Validates if CompletionListener is NULL. If it is null throws IllegalArgumentException
     * 
     * @param cListener
     * @throws IllegalArgumentException
     */
    private void validateCompletionListernerForNull(CompletionListener cListener) throws IllegalArgumentException {

        if (null == cListener) {

            throw (IllegalArgumentException) JmsErrorUtils.newThrowable(
                                                                        IllegalArgumentException.class,
                                                                        "INVALID_VALUE_CWSIA0068",
                                                                        new Object[] { "CompletionListener", null },
                                                                        null,
                                                                        null, // null probeId = no FFDC
                                                                        this,
                                                                        tc);
        }
    }

    /**
     * Validates if Message is NULL. If it is null throws MessageFormatException
     * 
     * @param cListener
     * 
     * @throws IllegalArgumentException
     */
    private void validateMessageForNull(Message msg) throws MessageFormatException {

        // if the supplied message is null, throw a jms MessageFormatException.
        if (msg == null) {
            throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                      MessageFormatException.class,
                                                                      "INVALID_VALUE_CWSIA0068",
                                                                      new Object[] { "message", null },
                                                                      tc);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void send(Message msg, CompletionListener cListener)
                    throws JMSException, MessageFormatException, InvalidDestinationException, IllegalArgumentException, UnsupportedOperationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { msg, cListener });

        validateCompletionListernerForNull(cListener);

        send_internal(msg, cListener);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");

    }

    /** {@inheritDoc} */
    @Override
    public void send(Destination dst, Message msg, CompletionListener cListener)
                    throws JMSException, MessageFormatException, InvalidDestinationException, IllegalArgumentException, UnsupportedOperationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dst, msg, cListener });

        validateCompletionListernerForNull(cListener);

        send_internal(dst, msg, cListener);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    /** {@inheritDoc} */
    @Override
    public void send(Message msg, int deliveryMode, int priority, long timeToLive, CompletionListener cListener)
                    throws JMSException, MessageFormatException, InvalidDestinationException, IllegalArgumentException, UnsupportedOperationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { msg, deliveryMode, priority, timeToLive, cListener });

        validateCompletionListernerForNull(cListener);

        send_internal(msg, deliveryMode, priority, timeToLive, cListener);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    /** {@inheritDoc} */
    @Override
    public void send(Destination dst, Message msg, int deliveryMode, int priority, long timeToLive, CompletionListener cListener)
                    throws JMSException, MessageFormatException, InvalidDestinationException, IllegalArgumentException, UnsupportedOperationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send", new Object[] { dst, msg, deliveryMode, priority, timeToLive, cListener });

        validateCompletionListernerForNull(cListener);

        send_internal(dst, msg, deliveryMode, priority, timeToLive, cListener);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageProducer#setDeliveryDelay(long)
     */
    @Override
    public void setDeliveryDelay(long deliveryDelayTime) throws JMSException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryDelay", deliveryDelayTime);

        checkClosed();
        // Throws an exception if this is not a valid time to live.
        validateDeliveryDelayTime(deliveryDelayTime);
        // We know by this point that it must be valid.
        this.defaultDeliveryDelay = deliveryDelayTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDeliveryDelay");

    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageProducer#getDeliveryDelay()
     */
    @Override
    public long getDeliveryDelay() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeliveryDelay");
        checkClosed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDeliveryDelay", defaultDeliveryDelay);
        return this.defaultDeliveryDelay;
    }

    boolean isManaged() {
        return session.isManaged();
    }

    /**
     * This function blocks till all Async sends are resolved.
     */
    private void waitForAsyncSendsResolution() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "waitForAsyncSendsResolution");

        //AsyncSend is only for non-managed environments.. no need to wait for managed environment
        if (!isManaged()) {

            synchronized (_inProgressAysncSends) {
                while (_inProgressAysncSends.get() > 0L) {
                    try {
                        //wait for 20 seconds.. and again check for condition..if no notification comes before then.
                        _inProgressAysncSends.wait(1000 * 20);
                    } catch (InterruptedException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "waitForAsyncSendsResolution got interrupted", e);
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "waitForAsyncSendsResolution");
    }

}
