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

package com.ibm.ws.sib.api.jms.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.InvalidDestinationException;
import javax.jms.InvalidSelectorException;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsFactoryFactory;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.JmsConnInternals;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.JmsTemporaryDestinationInternal;
import com.ibm.ws.sib.api.jms.ute.UTEHelperFactory;
import com.ibm.ws.sib.api.jmsra.JmsJcaConnection;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ThreadPool;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

public class JmsConnectionImpl implements Connection, JmsConnInternals, ApiJmsConstants, JmsInternalConstants
{

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsConnectionImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // ******************************* CONSTANTS *********************************

    /**
     * Threshold at which opening a new Session will cause a warning message
     * to be output to the console (indicating to the application that it may
     * be leaking resources).
     */
    private static final int SESSION_WARNING_THRESHOLD = 100;

    // **************************** STATE VARIABLES ******************************

    private final JmsJcaConnection jcaConnection;
    private SICoreConnection coreConnection;
    private boolean clientIDFixed;
    private final boolean isManaged;

    /**
     * A map containing the properties we wish to pass through to producer and/or
     * consumer objects. Things like the clientID and nonPerMapping are stored here.
     */
    private Map passThruProps = null;

    /**
     * This list contains a reference to all of the Sessions created from this Connection
     * Note that all access to this object should be carried out inside a sync(stateLock)
     * block to prevent array index problems from occuring if Sessions are being started,
     * stopped and closed at the same time (165050).
     */
    private final List sessions;

//174200.1
    /**
     * This list contains a reference to all of the TemporaryQueues and TemporaryTopics.
     * Although they are created on the sessions, they have the same life time as the
     * Connection. For this reason the Connection maintains a list of them so that they
     * may be closed when the Connection is closed.
     */
    private final List temporaryDestinations;

    /**
     * A list of available ordering context objects, that were created by sessions
     * that are no longer used.
     * This is required because comms allocates an id to each ordering context
     * object created. This ID is only released if at least one producer/consumer
     * session is successfully created that references the ordering context, and
     * then closed. This means that if a producer/consumer is never created from
     * a JMS session, then we will leak an ID in the table (eventually
     * running out of IDs and failing to create a session).
     */
    private final List<OrderingContext> unusedOrderingContexts = new ArrayList<OrderingContext>(1);

    /**
     * Variables to administer the connection state.
     * Note that the state lock is used for the following purposes as well as state;
     * Synchronizing the clientID set/get methods.
     * Serializing access to the ExceptionListener if one is set.
     */
    private int state = STOPPED;
    private final Object stateLock = new Object();

    /**
     * The optional ExceptionListener for this Connection (Use AtomicReference to ensure we see
     * an up-to-date view of the exception listener across all threads
     */
    private final AtomicReference<ExceptionListener> elRef = new AtomicReference<ExceptionListener>(null);

    /**
     * The ConnectionListenerImpl instance used to route core notifications
     * to the ExceptionListener.
     */
    private ConnectionListenerImpl connListener = null;

    /**
     * This static variable holds the state for whether we are running
     * in a cloned app server or not. It will be set by querying the
     * WLM runtime objects. (but can be fiddled by tests!)
     */
    private static Boolean isCloned = null;

    /**
     * PK59962 A threadpool for handling JMS exception listener calls - one per process
     */
    private static ThreadPool exceptionThreadPool = null;

    /**
     * PK59962 Ensure once-only creation of the threadpool for the whole process
     */
    private static Object exceptionTPCreateSync = new Object();

    /**
     * PK59962 Our instance of the JMS Exception handler - one per JMS connection.
     */
    private final JmsAsyncExceptionTask asyncExceptionTask = new JmsAsyncExceptionTask();

    // ***************************** CONSTRUCTORS ********************************

    /**
     * Constructor used by the ConnectionFactory to instantiate this Connection
     * object.
     * 
     * @param isManaged
     *            Is the conneciton factory running in a managed environment (such as WAS).
     * @param jcaConnection
     *            The associated jca connection object.
     * @param _passThruProps Map containing the properties that must be passed through
     *            to producer or consumer objects (things like clientID, nonPerMapping etc).
     */
    JmsConnectionImpl(JmsJcaConnection jcaConnection, boolean isManaged, Map _passThruProps) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsConnectionImpl", new Object[] { jcaConnection, isManaged, _passThruProps });

        passThruProps = _passThruProps;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc,
                        "clientId : " + passThruProps.get(JmsraConstants.CLIENT_ID
                                                          + "  nonPersistentMapping : " + passThruProps.get(JmsraConstants.NON_PERSISTENT_MAP)));
        this.jcaConnection = jcaConnection;

        // Make sure that if we do see empty string then we treat it the same as null.
        // This shouldn't be an issue after the change to JmsMCF.getClientID.
        String tempClientID = (String) passThruProps.get(JmsraConstants.CLIENT_ID);
        if ((tempClientID != null) && (!"".equals(tempClientID))) {

            //If the clientID is not default ID, then prevent the further override of clientID. 
            if (!isDefaultClientId(tempClientID)) {
                fixClientID();
                addClientId(tempClientID);
            }
        }

        this.isManaged = isManaged;
        sessions = new ArrayList();
        temporaryDestinations = new ArrayList();
        setState(STOPPED);

        // get the core connection from the jca connection
        try {
            this.coreConnection = jcaConnection.getSICoreConnection();
            String durableSubsHome = (String) passThruProps.get(JmsraConstants.DURABLE_SUB_HOME);

            //if user/cf does not set value, this would be null in thin client environments.
            if (null == durableSubsHome) {
                durableSubsHome = "defaultME";
            }
            // defect 119176
            if (durableSubsHome.equals("defaultME"))
            {
                // defect 251013 to make durable subscription points to ME name as durable subscription home while connecting ME instead of defaultME
                passThruProps.put(JmsraConstants.DURABLE_SUB_HOME, coreConnection.getMeName());
            }
            // Check that were given something we can work with.
            if (this.coreConnection == null) {
                JMSException e = (JMSException) JmsErrorUtils.newThrowable(
                                                                           JMSException.class,
                                                                           "JCA_RESOURCE_EXC_CWSIA0005",
                                                                           null,
                                                                           tc);
                // d238447 FFDC review. Failing to get the coreConnection warrents an FFDC.
                FFDCFilter.processException(e, "JmsConnectionImpl", "<init>#1", this, new Object[] { jcaConnection, isManaged, _passThruProps });
                throw e;
            }
        } catch (IllegalStateException ise) {
            // No FFDC code needed
            // d222942 review. getSICoreConnection throws IllegalState if the connection is closed.
            // Since we wouldn't expect the connection to be closed at this stage, default message ok.
            // d238447 FFDC review. Generate FFDC.
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(
                                                                               javax.jms.IllegalStateException.class,
                                                                               "EXCEPTION_RECEIVED_CWSIA0022",
                                                                               new Object[] { ise, "JmsConnectionImpl.getSICoreConnection" },
                                                                               ise,
                                                                               "JmsConnectionImpl#2",
                                                                               this,
                                                                               tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "JmsConnectionImpl" +
                           "(JmsJcaConnection, boolean, Map)");
        }
    }

    // *************************** INTERFACE METHODS *****************************

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSession", new Object[] { transacted, acknowledgeMode });

        JmsSessionImpl jmsSession = null;

        // throw an exception if the connection is closed.
        checkClosed();

        // mark that the client id cannot be changed.
        fixClientID();

        // enforce consistency on transacted flag and acknowledge mode.
        if (transacted) {
            acknowledgeMode = Session.SESSION_TRANSACTED;
        }
        if (!transacted && isManaged) {
            if (acknowledgeMode == Session.CLIENT_ACKNOWLEDGE) {
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "INVALID_ACKNOWLEDGE_MODE_CWSIA0514",
                                                                new Object[] { acknowledgeMode },
                                                                tc
                                );
            }
            // if the ackmode is dups_ok then set it to dups_ok, for all the other combination 
            // set it to auto_ack
            if (acknowledgeMode == Session.DUPS_OK_ACKNOWLEDGE)
                acknowledgeMode = Session.DUPS_OK_ACKNOWLEDGE;
            else
                acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
        }

        // create the jca session.
        boolean internallyTransacted = (transacted
                                        || acknowledgeMode == Session.CLIENT_ACKNOWLEDGE
                                        || acknowledgeMode == Session.DUPS_OK_ACKNOWLEDGE);
        JmsJcaSession jcaSession = createJcaSession(internallyTransacted);

        // Lock here so that another thread using the same connection doesn't reset the 
        // coreConnection to something else as as we are instantiating the Session 
        synchronized (this)
        {
            try
            {
                // We potentially have a new coreConnection becuase the creation of the JcaSession
                // detected that the coreConnection is now invalid and so a new managedConnection
                // and coreConnection was created. So reset the coreConnection for this JmsConnection
                // to the new one or we just set the old one again.
                coreConnection = jcaSession.getSICoreConnection();
            } catch (IllegalStateException e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.impl.JmsConnectionImpl", "createSession#2", this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "createSession", jmsSession);

                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "JCA_CREATE_SESS_CWSIA0024",
                                                                null,
                                                                e,
                                                                "JmsConnectionImpl.createSession#2",
                                                                this,
                                                                tc);
            }
            // instantiate the jms session, and add it to this connection's session list.
            jmsSession = instantiateSession(transacted, acknowledgeMode, coreConnection, jcaSession);
        }
        synchronized (stateLock) {
            sessions.add(jmsSession);
            // d353701 - output a warning message if there are 'lots' of sessions active.
            if (sessions.size() % SESSION_WARNING_THRESHOLD == 0) {
                // We wish to tell the user which line of their application created the session,
                // so we must obtain a line of stack trace from their application.
                String errorLocation = JmsErrorUtils.getFirstApplicationStackString();
                SibTr.warning(tc, "MANY_SESSIONS_WARNING_CWSIA0027", new Object[] { "" + sessions.size(), errorLocation });
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createSession", jmsSession);
        return jmsSession;
    }

    /**
     * @see javax.jms.Connection#getClientID()
     */
    @Override
    public String getClientID() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getClientID");

        // Throw an exception if this connection is closed.
        checkClosed();

        String cl = null;
        synchronized (stateLock) {
            cl = (String) passThruProps.get(JmsraConstants.CLIENT_ID);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getClientID", cl);
        return cl;
    }

    /**
     * @see javax.jms.Connection#setClientID(String)
     */
    @Override
    public void setClientID(String clID) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setClientID", clID);

        // Prevent use of this method if the Connection is closed.
        checkClosed();

        if (isManaged) {
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "MGD_ENV_CWSIA0025",
                                                                               new Object[] { "setClientID" }, tc);
        }

        synchronized (stateLock) {
            if ((clID == null) || ("".equals(clID))) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Ignoring null or empty clientID - does not affect fixed state.");
            }
            else if (!clientIDFixed) {

                /** If the given client Id is a default Id and the same was set previously on this connection, then just call fixClientID & return. */
                if (isDefaultClientId(clID) && clID.equals(getClientID())) {
                    fixClientID();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "The client Id " + clID
                                              + " being set is a default client Id and this connection already has the default client Id set");

                    return;
                } else if (clientIdExists(clID)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, " The client ID " + clID + " is invalid as a connection with the same client ID is already running.");
                    throw (javax.jms.InvalidClientIDException) JmsErrorUtils.newThrowable(javax.jms.InvalidClientIDException.class,
                                                                                          "CLIENT_ID_ALREADY_EXISTS_CWSIA0028", new Object[] { clID }, tc);
                }

                passThruProps.put(JmsraConstants.CLIENT_ID, clID);
                addClientId(clID);
                fixClientID();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "clientID set to " + clID);
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Attempted to set clientID after it has been fixed.");
                throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                                   "CLIENT_ID_FIXED_CWSIA0023", null, tc);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setClientID");
    }

    /**
     * @see javax.jms.Connection#getMetaData()
     */
    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMetaData");

        checkClosed();
        ConnectionMetaData cmd = JmsFactoryFactory.getInstance().getMetaData();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMetaData", cmd);
        return cmd;
    }

//    public String getMename() throws JMSException
//    {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//            SibTr.entry(this, tc, "getMename");
//        return this.Mename;
//    }

    /**
     * @see javax.jms.Connection#getExceptionListener()
     */
    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getExceptionListener");

        checkClosed();

        // Retrieve the most current snapshot of the exception listener
        ExceptionListener el = elRef.get();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getExceptionListener", el);
        return el;
    }

    /**
     * @see javax.jms.Connection#setExceptionListener(ExceptionListener)
     */
    @Override
    public void setExceptionListener(ExceptionListener eListener) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setExceptionListener", eListener);

        checkClosed();
        fixClientID();

        if (isManaged) {

            boolean exceptionRequired = true;

            // To support async beans, we suppress the exception if:
            // the listener is a Proxy, and
            // the invocation handler is from the com.ibm tree.
            if (eListener instanceof Proxy) {
                InvocationHandler handler = Proxy.getInvocationHandler(eListener);
                String name = handler.getClass().getName();
                if (name.startsWith("com.ibm")) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "async beans: exceptionListener accepted");
                    exceptionRequired = false;
                }
            }

            if (exceptionRequired) {
                throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(
                                                                                   javax.jms.IllegalStateException.class,
                                                                                   "MGD_ENV_CWSIA0025",
                                                                                   new Object[] { "setExceptionListener" },
                                                                                   tc);
            }
        }

        // Store the listener.
        elRef.set(eListener);

        // Do we need to create a ConnectionListener to hook in core events?
        if (eListener != null && connListener == null) {
            connListener = new ConnectionListenerImpl(this);
            try {
                coreConnection.addConnectionListener(connListener);
            } catch (SIConnectionUnavailableException e) {
                // No FFDC code needed
                // d222942 review - default message ok
                // d238447 FFDC review. Don't generate FFDC for connection loss at this level, as
                //   this is likely to be either an external error, or have been already FFDCd by
                //   comms/core spi.
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0022",
                                                                new Object[] { e, "JmsConnectionImpl.setExceptionListener" },
                                                                e,
                                                                null, // null probeId = no FFDC.
                                                                this,
                                                                tc);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setExceptionListener");
    }

    /**
     * @see javax.jms.Connection#start()
     */
    @Override
    public void start() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "start");

        // Mark that the clientID cannot now be changed.
        fixClientID();

        synchronized (stateLock) {

            int st = getState();
            if (st == STOPPED) {
                Object[] sessionCopy = sessions.toArray();
                int sessLength = 0;
                if (sessionCopy != null) {
                    sessLength = sessionCopy.length;
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "sessionCopy is null.");
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "About to start " + sessLength + " sessions.");
                for (int i = 0; i < sessLength; i++) {
                    ((JmsSessionImpl) sessionCopy[i]).start();
                }
                setState(STARTED);
            }
            else if (st == CLOSED) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "start");
                checkClosed();
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "unknown state: " + st);
            }

        } // end of synchronized block

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "start");
    }

    /**
     * @see javax.jms.Connection#stop()
     */
    @Override
    public void stop() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "stop");

        // I can't find anywhere that we call this method ourselves, but if we
        // do, we'll probably need to implement a private _stop method that doesn't
        // implement the following check.
        if (isManaged) {
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "MGD_ENV_CWSIA0025",
                                                                               new Object[] { "stop" }, tc);
        }

        // Mark that the clientID cannot now be changed.
        fixClientID();

        synchronized (stateLock) {

            int st = getState();
            if ((st == STARTED) || (st == STOPPED)) {
                Object[] sessionCopy = sessions.toArray();
                int sessLength = 0;
                if (sessionCopy != null) {
                    sessLength = sessionCopy.length;
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "sessionCopy is null.");
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "About to stop " + sessLength + " sessions.");
                for (int i = 0; i < sessLength; i++) {
                    ((JmsSessionImpl) sessionCopy[i]).stop();
                }
                setState(STOPPED);
            }
            else if (st == CLOSED) {
                checkClosed();
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Unknown state: " + st);
            }

        } // end of synchronized block

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "stop");
    }

    /**
     * @see javax.jms.Connection#close()
     */
    @Override
    public void close() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");

        //in case of non-managed environments, check for validity of close calls.
        if (!isManaged) {
            synchronized (stateLock) {
                //check whether the close call has come from CompletionListner onComplete/onException
                //or from MessageListner onMessage()... which is
                //driven by the this connection's sessions. In that case as per JMS 2.0 spec we have tp throw
                //IllegalStateException. loop through all the sessions and validate.
                Object[] sess = sessions.toArray();
                for (int i = 0; i < sess.length; i++) {
                    ((JmsSessionImpl) sess[i]).validateCloseCommitRollback("close");
                    ((JmsSessionImpl) sess[i]).validateStopCloseForMessageListener("close");
                }

            }
        }

        boolean needToClose = (getState() != CLOSED);

        try {

            if (needToClose) {
                removeClientId(getClientID());
                setState(CLOSED);
                fixClientID();
                // cascade the close operation down to the Sessions.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "connection aquiring sessions lock");

                synchronized (stateLock) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "connection acquired sessions lock");

                    Object[] sess = sessions.toArray();
                    for (int i = 0; i < sess.length; i++) {
                        ((JmsSessionImpl) sess[i]).close(true); // Indicate that this is a tidy up close so that some optimisaton may be performed lower down
                    }
                    // should be nothing left to clear as session close removes from list
                    sessions.clear();

                    // Clear our list of ordering contexts associated with those sessions
                    unusedOrderingContexts.clear();

                    //174200.1
                    //delete all the temporary destinations on the connection
                    // Temp dests will be destroyed when connection closes anyway, so this is
                    // somewhat redundant. Therefore, synchronizing on temporaryDestinations object
                    // not worth the risk of deadlock.
                    Object[] tempDests = temporaryDestinations.toArray();
                    for (int index = 0; index < tempDests.length; index++) {
                        ((JmsTemporaryDestinationInternal) tempDests[index]).delete();
                    }
                    //By this point there should be nothing left to clear
                    temporaryDestinations.clear();
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "connection released sessions lock");

                // Do we need to remove a ConnectionListener?
                // (It seems odd that the core doesn't do this as part of the
                // coreConnection.close() processing - perhaps worth checking).
                if (connListener != null && coreConnection != null) {
                    try {
                        coreConnection.removeConnectionListener(connListener);
                    } catch (SIConnectionUnavailableException e) {
                        // No FFDC code needed
                        // d222942 review - default message ok
                        // d238447 FFDC review. ConnectionUnavailable/connectionDropped aren't internal errors
                        //   at this level, so no FFDC.
                        throw (JMSException) JmsErrorUtils.newThrowable(
                                                                        JMSException.class,
                                                                        "EXCEPTION_RECEIVED_CWSIA0022",
                                                                        new Object[] { e, "JmsConnectionImpl.close" },
                                                                        e,
                                                                        null, // null probeId = no FFDC
                                                                        this,
                                                                        tc
                                        );
                    }
                }
            }

        } finally {
            // d260707 Close the jcaConnection to ensure that jca objects get cleaned up.
            // The jcaConnection's close() closes the coreConnection, so we must not close
            // it ourselves.
            try {
                if (needToClose) {
                    jcaConnection.close();
                }
            } catch (SIException e) {
                // No FFDC code needed
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0022",
                                                                new Object[] { e, "JmsConnectionImpl.close" },
                                                                e,
                                                                "JmsConnectionImpl.close#2",
                                                                this,
                                                                tc
                                );
            } finally {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "close");
            }
        }

    }

    /**
     * @see javax.jms.Connection#createConnectionConsumer(Destination, String, ServerSessionPool, int)
     */
    @Override
    public ConnectionConsumer createConnectionConsumer(Destination dest, String selector, ServerSessionPool ssp, int maxMessages) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConnectionConsumer");

        if (isManaged) {
            throw (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                      "MGD_ENV_CWSIA0025",
                                                                      new Object[] { "JmsConnectionImpl.createConnectionConsumer" }, tc);
        }

        // This is an ASF function that we do not support in Jetstream
        throw (JMSException) JmsErrorUtils.newThrowable(
                                                        JMSException.class,
                                                        "UNSUPPORTED_FUNC_CWSIA0026",
                                                        new Object[] { "JmsConnectionImpl.createConnectionConsumer()" },
                                                        tc
                        );
    }

    /**
     * @see javax.jms.Connection#createDurableConnectionConsumer(Topic, String, String, ServerSessionPool, int)
     */
    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subName, String selector, ServerSessionPool ssp, int maxMessages) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableConnectionConsumer");

        if (isManaged) {
            throw (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                      "MGD_ENV_CWSIA0025",
                                                                      new Object[] { "createDurableConnectionConsumer" }, tc);
        }

        // This is an ASF function that we do not support in Jetstream
        throw (JMSException) JmsErrorUtils.newThrowable(
                                                        JMSException.class,
                                                        "UNSUPPORTED_FUNC_CWSIA0026",
                                                        new Object[] { "JmsConnectionImpl.createDurableConnectionConsumer()" },
                                                        tc
                        );
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jms.JmsConnection#reportException(javax.jms.JMSException)
     */
    @Override
    public void reportException(JMSException e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "reportException", e);
        if (elRef.get() != null) {
            asyncExceptionTask.handleException(e);
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No exception listener is currently set for this connection.");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "reportException");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jms.JmsConnInternals#getConnectedMEName()
     */
    @Override
    public String getConnectedMEName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnectedMEName");
        String meName = coreConnection.getMeName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConnectedMEName", meName);
        return meName;
    }

    // ***************************** TEST METHODS *****************************

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jms.JmsConnInternals#createDestination(javax.jms.Destination)
     */
    public void createDestination(Destination dest) throws JMSException {
        UTEHelperFactory.getHelperInstance().createDestination(dest);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jms.JmsConnInternals#deleteDestination(javax.jms.Destination)
     */
    public void deleteDestination(Destination dest) throws JMSException {
        UTEHelperFactory.getHelperInstance().deleteDestination(dest);
    }

    /**
     * Returns a count of the number of Sessions that are currently open against
     * this Connection.
     */
    public int getSessionCount() {
        synchronized (stateLock) {
            return sessions.size();
        }
    }

    /**
     * Returns the number of temporary destinations owned by this connection.
     * Package vis method for unit test.
     * 
     * @return
     */
    int getTempDestCount() {
        // don't worry about synchronization unless unit tests fail
        return temporaryDestinations.size();
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * This method is used to create a JMS session object.<p>
     * 
     * It is overriden by
     * subclasses so that the appropriate session type is returned whenever
     * this method is called (e.g. from within createSession()).
     */
    JmsSessionImpl instantiateSession(boolean transacted, int acknowledgeMode, SICoreConnection coreConnection, JmsJcaSession jcaSession) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateSession", new Object[] { transacted, acknowledgeMode, coreConnection, jcaSession });
        JmsSessionImpl jmsSession = new JmsSessionImpl(transacted, acknowledgeMode, coreConnection, this, jcaSession);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateSession", jmsSession);
        return jmsSession;
    }

    /**
     * This method is used to remove a Session from the list of sessions held by
     * the Connection.
     */
    void removeSession(JmsSessionImpl sess) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeSession", sess);

        synchronized (stateLock) {
            // Remove the Session
            // Note that this is a synchronized collection.
            boolean res = sessions.remove(sess);

            // Release the ordering context associated with the session
            unusedOrderingContexts.add(sess.getOrderingContext());

            if (!res && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "session not found in list");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeSession");
    }

    /**
     * Returns the state.
     * 
     * @return int
     */
    protected int getState() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getState");
        int tempState;
        synchronized (stateLock) {
            tempState = state;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getState", state);
        return tempState;
    }

    /**
     * Sets the state.
     * 
     * @param state The state to set
     */
    protected void setState(int newState) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setState", newState);

        synchronized (stateLock) {
            if ((newState == JmsInternalConstants.CLOSED)
                || (newState == JmsInternalConstants.STOPPED)
                || (newState == JmsInternalConstants.STARTED)) {
                state = newState;
                stateLock.notifyAll();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setState");
    }

    /**
     * This method is called at the beginning of every method that should not work
     * if the Connection has been closed. It prevents further execution by throwing
     * a JMSException with a suitable "I'm closed" message.
     */
    protected void checkClosed() throws JMSException {
        if (getState() == CLOSED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "This Connection is closed.");
            throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                               "CONNECTION_CLOSED_CWSIA0021",
                                                                               null, tc);
        }
    }

    /**
     * Provide access to the isManaged state.
     * 
     */
    boolean isManaged() {
        return isManaged;
    }

    /**
     * This method is called in order to mark that the clientID may not now change.
     */
    protected void fixClientID() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "fixClientID");

        synchronized (stateLock) {
            clientIDFixed = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "fixClientID");
    }

    /**
     * This method is called in order to mark that the clientID may now change.
     */
    void unfixClientID() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unfixClientID");

        synchronized (stateLock) {
            clientIDFixed = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unfixClientID");
    }

    /**
     * Create a JCA Session.
     * If this Connection contains a JCA Connection, then use it to create a
     * JCA Session.
     * 
     * @return The new JCA Session
     * @throws JMSException if the JCA runtime fails to create the JCA Session
     */
    protected JmsJcaSession createJcaSession(boolean transacted) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createJcaSession", transacted);
        JmsJcaSession jcaSess = null;

        // If we have a JCA connection, then make a JCA session
        if (jcaConnection != null) {
            try {
                jcaSess = jcaConnection.createSession(transacted);
            } catch (Exception e) { // ResourceE, IllegalStateE, SIE, SIErrorE
                // No FFDC code needed
                // d238447 Generate FFDC for these cases.
                throw (JMSException) JmsErrorUtils.newThrowable(
                                                                JMSException.class,
                                                                "JCA_CREATE_SESS_CWSIA0024",
                                                                null,
                                                                e,
                                                                "JmsConnectionImpl.createSession#1",
                                                                this,
                                                                tc);
            }
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "jcaConnection is null, returning null jcaSess");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createJcaSession", jcaSess);
        return jcaSess;
    }

    /**
     * Used to retrieve the pass through properties for things defined on the CF,
     * that are to be used by producer or consumer objects.
     * 
     * @return Map
     */
    protected Map getPassThruProps() {
        return passThruProps;
    }

//174200.1
    /**
     * Add a TemporaryDestination to the list of temporary destinations
     * created by sessions under this connection
     * 
     * @param tempDest - the temporary destination
     */
    protected void addTemporaryDestination(JmsTemporaryDestinationInternal tempDest) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addTemporaryDestination", System.identityHashCode(tempDest));

        synchronized (temporaryDestinations) { // synchronize against removeTemporaryDestination
            temporaryDestinations.add(tempDest);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addTemporaryDestination");
    }

    /**
     * Remove a TemporaryDestination from the list of temporary destinations
     * created by sessions under this connection
     * 
     * @param tempDest - the temporary destination to be removed from the List
     */
    protected void removeTemporaryDestination(JmsTemporaryDestinationInternal tempDest) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeTemporaryDestination", System.identityHashCode(tempDest));

        synchronized (temporaryDestinations) { // synchronize against addTemporaryDestination
            temporaryDestinations.remove(tempDest);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeTemporaryDestination");
    }

    /**
     * This method is used to determine whether we are running in a cloned appServer.<p>
     * 
     * It queries the WLM runtime objects (indirectly) to find this out.
     * 
     * @return boolean
     */
    public static boolean isClonedServer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isClonedServer");

        // If we have not yet found out this information.
        if (isCloned == null) {
            isCloned = Boolean.valueOf(RuntimeInfo.isClusteredServer());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isClonedServer", isCloned);
        return isCloned.booleanValue();
    }

    /**
     * This method exists for the benefit of testability, allowing
     * us to fake the flag that says we are running in a cloned
     * appserver.<p>
     * 
     * It should not be exposed to customer apps.
     */
    public static void setIsClonedServer(Boolean newVal) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setIsClonedServer", newVal);
        isCloned = newVal;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setIsClonedServer");
    }

    /**
     * Called by each session when it is created to get an ordering context for that session.
     * 
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     */
    public OrderingContext allocateOrderingContext() throws SIConnectionDroppedException, SIConnectionUnavailableException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "allocateOrderingContext");

        // Synchronize on the state lock, and either re-use an existing
        // object or create a new one
        OrderingContext oc;
        synchronized (stateLock) {
            if (unusedOrderingContexts.isEmpty()) {
                oc = coreConnection.createOrderingContext();
            }
            else {
                // Re-use an existing object.
                // It is most efficient to remove the last entry in the array list.
                oc = unusedOrderingContexts.remove(unusedOrderingContexts.size() - 1);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "allocateOrderingContext", oc);
        return oc;
    }

    /**
     * Provide a toString containing useful information
     * This toString provides the objectId of the connection instance, the
     * name of the connected ME, and the number of sessions and temp dests
     * associated with the connection.
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer("ConnectionId: " + Integer.toHexString(System.identityHashCode(this)));
        result.append(", MEName: " + coreConnection.getMeName());
        result.append(", Sessions: " + sessions.size());
        result.append(", TemporaryDestinations: " + temporaryDestinations.size()); // don't worry about synchronization
        return result.toString();
    }

    // **************************************** Exception thread ***********************************************

    /**
     * PK59962 Ensure the existence of a single thread pool within the process
     */
    private static void initExceptionThreadPool() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initExceptionThreadPool");

        // Do a synchronous check to ensure once-only creation
        synchronized (exceptionTPCreateSync) {
            if (exceptionThreadPool == null) {
                // Get the maximum size for the thread pool, defaulting if
                // not specified or if the value cannot be parsed
                int maxThreads = Integer.parseInt(ApiJmsConstants.EXCEPTION_MAXTHREADS_DEFAULT_INT);
                String maxThreadsStr = RuntimeInfo.getProperty(ApiJmsConstants.EXCEPTION_MAXTHREADS_NAME_INT,
                                                               ApiJmsConstants.EXCEPTION_MAXTHREADS_DEFAULT_INT);
                try {
                    maxThreads = Integer.parseInt(maxThreadsStr);
                } catch (NumberFormatException e) {
                    // No FFDC code needed, but we will exception this
                    SibTr.exception(tc, e);
                }

                // Trace the value we will use
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, ApiJmsConstants.EXCEPTION_MAXTHREADS_NAME_INT + ": " + Integer.toString(maxThreads));

                // Create the thread pool
                exceptionThreadPool = new ThreadPool(ApiJmsConstants.EXCEPTION_THREADPOOL_NAME_INT, 0, maxThreads);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initExceptionThreadPool");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.api.jms.JmsConnection#reportException(javax.jms.JMSException)
     * This method directly invokes any registered exception listener.
     * No locks are taken by this method, so it is the caller's responsibility to
     * take any locks that may be needed.
     */
    private void callExceptionListener(JMSException e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "callExceptionListener");

        // Get the latest version of the ExceptionListener (after this point we're
        // going to call that one regardless of whether it changes
        ExceptionListener elLocal = elRef.get();
        if (elLocal != null) {
            // Protect our code from badly behaved exception listeners.
            try {
                // Trace the class and hashcode of the exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    SibTr.debug(this, tc, "Exception handler class: " + elLocal.getClass().getName());
                    SibTr.debug(this, tc, "Exception: ", e);
                }

                // Pass the exception on.
                elLocal.onException(e);
            } catch (RuntimeException exc) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "User ExceptionListener threw exception", exc);
            }
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No exception listener is currently set for this connection.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "callExceptionListener");
    }

    // ************ Inner Class for exception listener calling ********

    /**
     * PK59962 This runnable is used to call JMS exception listeners, on
     * a separate thread to the failure (to avoid locking issues).
     * The code processes all exceptions which are outstanding for this
     * JMS connection, in order in which they occurred. It then ends.
     * Exceptions for different JMS connections can be handled on different
     * threads, using a pool.
     */
    private class JmsAsyncExceptionTask implements Runnable {
        /**
         * Linked list of exceptions which require a callback for this
         * JMS connection.
         */
        private final LinkedList<JMSException> exceptionQueue = new LinkedList<JMSException>();

        /**
         * Set when the thread for this JMS connection is in starting,
         * or started state. Unset once the thread has chosen to terminate.
         */
        private boolean active = false;

        /**
         * Run method for the thread
         */
        @Override
        public void run() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "run");

            // Loop until all exceptions have been processed
            boolean moreWork = true;
            while (moreWork) {
                // Synchronize while checking the depth of the linked list
                JMSException jmse = null;
                synchronized (this) {
                    int size = exceptionQueue.size();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Exceptions queued for handling: " + size);
                    active = moreWork = (size > 0);
                    if (moreWork) {
                        jmse = exceptionQueue.removeFirst();
                    }
                }

                // Do we have an exception to pass to a listener?
                if (jmse != null) {
                    // We do not need any synchronization around the call to the exception listener,
                    // as only one thread can ever be actively calling onException for a connection.
                    // The logic in handleException (inc. careful synchronization around
                    // enqueue/dequeue to exceptionQueue & the active flag) ensures this.
                    // Variable visability is not a justification for locking, as setExceptionListener
                    // does not hold any locks when assigning 'el' (and callExceptionListener takes
                    // a local snapshot of whatever this thread sees the value to be).
                    callExceptionListener(jmse);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "run");
        }

        /**
         * Provides handling for this exception.
         * One of three things can happen:
         * 1) A thread (dedicated to this JMS connection) is started to
         * handle this exception. This thread will end when it runs
         * out of exceptions to handle.
         * 2) The exception is queued for the existing thread.
         * 3) A failure occurs starting a thread, so the exception is handled
         * on this thread.
         * 
         * @param jmse The JMSException to be handled
         */
        private void handleException(JMSException jmse) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "handleException", jmse);

            // Synchronization ensures active is true if a thread is currently running
            // for this JMS connection (processing exceptions in order).
            // If it is false, then we can be sure either no thread is running,
            // or the thread currently running is assured to end without processing
            // any more exceptions.
            boolean threadAvailable; // Local var to hold result out of lock.
            synchronized (this) {
                threadAvailable = active;

                // Do we need to try and start a thread?
                if (!threadAvailable) {
                    try {
                        // Initialize the thread pool (if not already done)
                        if (exceptionThreadPool == null)
                            initExceptionThreadPool();

                        // Kick off a thread to call listeners on this connection
                        exceptionThreadPool.execute(this);

                        // Mark that we successfully activated. Synchronization ensures
                        // the started thread won't check this until we unlock.
                        threadAvailable = active = true;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Activated exception handling thread for connection");
                    } catch (Throwable ue) {
                        // No FFDC code needed (nor trace) for thread death
                        if (ue instanceof ThreadDeath) {
                            throw (ThreadDeath) ue;
                        }
                        // FFDC for any other unexpected failure
                        FFDCFilter.processException(ue, "com.ibm.ws.sib.api.jms.impl.JmsConnectionImpl", "handleException#2", this);
                    }
                }

                // If we have a thread at this point, just add some work
                if (threadAvailable) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Queuing exception for handling thread");
                    exceptionQueue.addLast(jmse);
                }
            }

            // Otherwise, invoke the exception listener on this thread.
            // - It is dangerous to try and take any lock here, so we don't.
            //   This is also dangerous if the exception listener itself makes
            //   JMS calls. However, it is the pre-PK59962 behaviour and
            //   on balance better than swallowing the exception.
            if (!threadAvailable) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Calling exception handler in-line.");
                callExceptionListener(jmse);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "handleException");
        }
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Connection#createSession()
     */
    @Override
    public Session createSession() throws JMSException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSession");

        Session session = null;
        try {
            session = createSession(false, Session.AUTO_ACKNOWLEDGE);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSession");
        }

        return session;
    }

    // JMS2.0
    /*
     * if the sessionMode is SESSION_TRANSACTED, we create a transacted session
     * 
     * @see javax.jms.Connection#createSession(int)
     */
    @Override
    public Session createSession(int sessionMode) throws JMSException {

        Session session = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSession", new Object[] { sessionMode });
        try {
            if (sessionMode == JMSContext.SESSION_TRANSACTED)
                session = createSession(true, sessionMode);
            else
                session = createSession(false, sessionMode);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "createSession");
        }
        return session;

    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Connection#createSharedConnectionConsumer(javax.jms.Topic, java.lang.String, java.lang.String, javax.jms.ServerSessionPool, int)
     */
    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws javax.jms.IllegalStateException, InvalidDestinationException, InvalidSelectorException, JMSException {

        if (isManaged) {
            throw (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                      "MGD_ENV_CWSIA0025",
                                                                      new Object[] { "createSharedConnectionConsumer" }, tc);
        }
        // This is an ASF function that we do not support in Jetstream
        throw (JMSException) JmsErrorUtils.newThrowable(
                                                        JMSException.class,
                                                        "UNSUPPORTED_FUNC_CWSIA0026",
                                                        new Object[] { "JmsConnectionImpl.createSharedConnectionConsumer()" },
                                                        tc
                        );
    }

    // JMS2.0
    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Connection#createSharedDurableConnectionConsumer(javax.jms.Topic, java.lang.String, java.lang.String, javax.jms.ServerSessionPool, int)
     */
    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws javax.jms.IllegalStateException, InvalidDestinationException, InvalidSelectorException, JMSException {

        if (isManaged) {
            throw (javax.jms.JMSException) JmsErrorUtils.newThrowable(javax.jms.JMSException.class,
                                                                      "MGD_ENV_CWSIA0025",
                                                                      new Object[] { "createSharedDurableConnectionConsumer" }, tc);
        }
        // This is an ASF function that we do not support in Jetstream
        throw (JMSException) JmsErrorUtils.newThrowable(
                                                        JMSException.class,
                                                        "UNSUPPORTED_FUNC_CWSIA0026",
                                                        new Object[] { "JmsConnectionImpl.createSharedDurableConnectionConsumer()" },
                                                        tc
                        );
    }

    /**
     * If there is no entry for the given Client Id in the "clientIdTable", then add it with count as "1".
     * If its already exists then just increment the counter value by "1".
     * 
     * @param clientId
     */
    private void addClientId(String clientId) {
        //Sometimes the client Id will be null. For instance in the case QueueConnectionFactory, there is no default client Id specified in metatype.xml.
        if (clientId == null)
            return; //do nothing

        ConcurrentHashMap<String, Integer> clientIdTable = JmsFactoryFactoryImpl.getClientIdTable();

        if (clientIdTable.containsKey(clientId)) {
            clientIdTable.put(clientId, clientIdTable.get(clientId).intValue() + 1);
        } else {
            clientIdTable.put(clientId, 1);
        }

    }

    /**
     * To remove the client id from client table.
     * Whenever this method is called, the counter of respective HashMap entry is decremented by one. If the count reaches "0", then that entry wil be removed from the
     * clientIdTable.
     * 
     * @param clientId
     */
    private void removeClientId(String clientId) {
        //Sometimes the client Id will be null. For instance in the case QueueConnectionFactory, there is no default client Id specified in metatype.xml. 
        if (clientId == null)
            return; //do nothing

        ConcurrentHashMap<String, Integer> clientIdTable = JmsFactoryFactoryImpl.getClientIdTable();

        if (clientIdTable.containsKey(clientId)) {
            int referenceCount = clientIdTable.get(clientId).intValue();
            if (referenceCount == 1) {
                clientIdTable.remove(clientId);
            } else {
                clientIdTable.put(clientId, clientIdTable.get(clientId).intValue() - 1);
            }
        }
    }

    /**
     * @param clientId - Client Identifier.
     * @return true if the client Id exists in the "clientIdTable"
     */
    private boolean clientIdExists(String clientId) {
        //Sometimes the client Id will be null. For instance in the case QueueConnectionFactory, there is no default client Id specified in metatype.xml. 
        if (clientId == null)
            return false; //do nothing

        ConcurrentHashMap<String, Integer> clientIdTable = JmsFactoryFactoryImpl.getClientIdTable();
        return clientIdTable.containsKey(clientId);
    }

    /**
     * To check if the given client id is a default client Id of this messaging system or not.
     * 
     * @param clientId - Client Identifier.
     * @return true if the given client Id is a default client Id.
     */
    boolean isDefaultClientId(String clientId) {
        return JmsraConstants.DEFAULT_ADMIN_CONFIG_CLIENT_ID.equals(clientId);

    }
}