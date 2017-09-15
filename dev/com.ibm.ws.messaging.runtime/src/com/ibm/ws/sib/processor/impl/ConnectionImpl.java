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
package com.ibm.ws.sib.processor.impl;

import java.io.Serializable;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.messaging.security.Authentication;
import com.ibm.ws.messaging.security.Authorization;
import com.ibm.ws.messaging.security.MessagingSecurityConstants;
import com.ibm.ws.messaging.security.MessagingSecurityException;
import com.ibm.ws.messaging.security.RuntimeSecurityService;
import com.ibm.ws.messaging.security.authorization.MessagingAuthorizationException;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.internal.JsMainAdminComponentImpl;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.processor.CommandHandler;
import com.ibm.ws.sib.processor.MPCoreConnection;
import com.ibm.ws.sib.processor.MPSubscription;
import com.ibm.ws.sib.processor.MulticastProperties;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.TransactionalCommandHandler;
import com.ibm.ws.sib.processor.exceptions.SIMPConnectionUnavailableException;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationAlreadyExistsException;
import com.ibm.ws.sib.processor.exceptions.SIMPIncorrectCallException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotAuthorizedException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.processor.exceptions.SIMPTemporaryDestinationNotFoundException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPIConnection;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream;
import com.ibm.ws.sib.processor.utils.DestinationSessionUtils;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.security.auth.AuthUtils;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.trm.TrmMeMain;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationConfiguration;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.Distribution;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.SICoreUtils;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SICommandInvocationFailedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SIInvalidDestinationPrefixException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * @author tevans
 */
public final class ConnectionImpl implements MPCoreConnection, TransactionCallback
{
    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls_cwsir =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIR_RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls_cwsik =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    private static final TraceComponent tc =
                    SibTr.register(
                                   ConnectionImpl.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private final SIMPTransactionManager _txManager;
    private final List<SICoreConnectionListener> _connectionListeners;
    private boolean _closed;
    private final List<ProducerSession> _producers;
    private final List<ConsumerSessionImpl> _consumers;
    private final List<BifurcatedConsumerSessionImpl> _bifurcatedConsumers;
    private final List<BrowserSession> _browsers;
    private final List<String> _temporaryDestinations;
    private final List<LocalTransaction> _ownedTransactions; // Transactions scoped by this connection
    //The unique id of this connection
    private final SIBUuid12 _uuid;
    private TrmMeMain _trmMeMain;

    /** Security subject associated with logged-on principal. */
    private Subject _subject = null;

    /** Used to check if messages should be copied when they are sent */
    private boolean _copyMessagesWhenSent = true;
    /** Used to check if messages should be copied when received */
    private boolean _copyMessagesWhenReceived = true;
    /** Used to check if msg wait time should be set when received */
    private boolean _setWaitTimeInMessage = true; //see defect 250838

    private final MessageProcessor _messageProcessor;
    private final DestinationManager _destinationManager;
    /** Support for destination access control */
    private AccessChecker _accessChecker;

    /** reference to the properties associated with the connection */
    private Map _connectionProperties = null;

    // Security Changes for Liberty Messaging: Sharath Start
    private final RuntimeSecurityService runtimeSecurityService = RuntimeSecurityService.SINGLETON_INSTANCE;
    private final Authentication _authentication;
    private final Authorization _authorization;
    private boolean _isBusSecure = false;
    // Security changes for Liberty Messaging: Sharath End

    private int connectionType = SIMPConstants.MP_INPROCESS_CONNECTION;

    /**
     * Create a new ConnectionImpl connected to the given Message Processor.
     * 
     * @param messageProcessor The MessageProcessor to connect to
     */
    ConnectionImpl(MessageProcessor messageProcessor, Subject subject, Map connectionProperties)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            //Don't call toString on subject as that can cause problems if Java 2 security is enabled.
            String report = "<null>";
            if (subject != null)
            {
                report = "subject(" + messageProcessor.
                                getAuthorisationUtils().
                                getUserName(subject) + ")";
            }
            SibTr.entry(tc, "ConnectionImpl", new Object[] { messageProcessor, report, connectionProperties });
        }

        _messageProcessor = messageProcessor;
        _subject = subject;
        _connectionProperties = connectionProperties;

        // Security Changes for Liberty Messaging: Sharath Start
        _authentication = messageProcessor.getAuthentication();
        _authorization = messageProcessor.getAuthorization();
        // Security Changes for Liberty Messaging: Sharath End

        // Set the security enablement flag
        _isBusSecure = messageProcessor.isBusSecure();
        if (_isBusSecure)
            _accessChecker = messageProcessor.getAccessChecker();

        _txManager = messageProcessor.getTXManager();
        _destinationManager = messageProcessor.getDestinationManager();

        //Initialize some lists...
        //Access to these will generally be sequential
        //so LinkedLists are used in preference to an ArrayList which implements
        //it's iterator using random access methods. Note that these are not
        //the synchronized forms of LinkedList and thus concurrent modification
        //needs to be guarded against.
        _producers = new LinkedList<ProducerSession>();
        _consumers = new LinkedList<ConsumerSessionImpl>();
        _bifurcatedConsumers = new LinkedList<BifurcatedConsumerSessionImpl>();
        _browsers = new LinkedList<BrowserSession>();
        _connectionListeners = new LinkedList<SICoreConnectionListener>();
        //ArrayList used for temporary Destinations
        _temporaryDestinations = new LinkedList<String>();
        // ArrayList of transactions created under this connection and therefore
        // scoped by the lifetime of this connection (if the connection is closed
        // any of these transactions still active will be rolled back).
        _ownedTransactions = new LinkedList<LocalTransaction>();

        //Create a new unique id for this connection. Using an Uuid may turn out
        //to be overkill but it'll do for now.
        _uuid = new SIBUuid12();

        //A boolean to indicate if this connection has been closed. 
        _closed = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ConnectionImpl", this);
    }

    /**
     * Check if this connection has been closed. If it has, a SIObjectClosedException
     * is thrown.
     * 
     * @throws SIConnectionUnavailableException
     */
    private void checkNotClosed() throws SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkNotClosed");

        //Synchronize on the closed object to prevent it being changed while we check it.
        synchronized (this)
        {
            if (_closed)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkNotClosed", "Connection Closed exception");
                SIMPConnectionUnavailableException e = new SIMPConnectionUnavailableException(
                                nls_cwsik.getFormattedMessage(
                                                              "DELIVERY_ERROR_SIRC_22", // OBJECT_CLOSED_ERROR_CWSIP0091
                                                              new Object[] { _messageProcessor.getMessagingEngineName() },
                                                              null));

                e.setExceptionReason(SIRCConstants.SIRC0022_OBJECT_CLOSED_ERROR);
                e.setExceptionInserts(new String[] { _messageProcessor.getMessagingEngineName() });

                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkNotClosed");
    }

    /**
     * Check if the Message Processor is started. If it is not a {@link SIConnectionUnavailableException} is thrown.
     * 
     * @throws SIConnectionUnavailableException
     */
    private void checkMPStarted() throws SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkMPStarted");

        //Synchronize on the closed object to prevent it being changed while we check it.
        synchronized (this)
        {
            if (!_messageProcessor.isStarted())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkMPStarted", "Message Processor not started exception");
                SIMPConnectionUnavailableException e = new SIMPConnectionUnavailableException(
                                nls_cwsik.getFormattedMessage(
                                                              "DELIVERY_ERROR_SIRC_22", // OBJECT_CLOSED_ERROR_CWSIP0091
                                                              new Object[] { _messageProcessor.getMessagingEngineName() },
                                                              null));

                e.setExceptionReason(SIRCConstants.SIRC0022_OBJECT_CLOSED_ERROR);
                e.setExceptionInserts(new String[] { _messageProcessor.getMessagingEngineName() });

                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkMPStarted");
    }

    /**
     * Check if the given expected destination type matches the type we are trying
     * to use. If not we throw an exception.
     * 
     * @param expectedDestType
     * @param destination - The actual destination we are trying to use
     * @throws SINotSupportedByConfigurationException If the expected type isn't the real type
     */
    private void checkDestinationType(DestinationType expectedDestType,
                                      SIDestinationAddress destAddr,
                                      DestinationHandler destination,
                                      boolean system)
                    throws SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkDestinationType",
                        new Object[] { expectedDestType, destAddr, destination, destination.getDestinationType(), system });

        boolean correctType = true;

        // 178865 Check that the destination type is the expected type
        if (expectedDestType != null)
        {
            correctType = false;

            // If the destination is foreign or a foreign bus, then skip the check
            if (destination.isForeign() || destination.isForeignBus() ||
                (destination.isAlias() && destination.getDestinationType() == DestinationType.UNKNOWN))
            {
                correctType = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Correct Type as destination is Foreign");
            }
            else
            {
                // For Remote Temporary topicspaces we send to the _PSIMP.TDRECEIVER queue.  This means that
                // remote topicspaces drop through this code.
                if (expectedDestType == destination.getDestinationType())
                    correctType = true;
                // If we're sending to a remote temporary topic space then we
                // can't rely on the type of the destination (as it's the _PSIMP.TDRECEIVER queue)
                // so check the prefix on the original name instead
                else if ((expectedDestType == DestinationType.TOPICSPACE) &&
                         (destAddr.getDestinationName().startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX)))
                    correctType = true;
            }
        }

        // Irrespective of type checking, system queues can only be accessed
        // by system SPI users and vice versa.
        if (system ^ destination.isSystem())
        {
            if (destination.isSystem())
            {
                // The exception to this rule is when we use the TDRECEIVER queue to
                // send messages remotely (which is a system queue being used for
                // normal messages).
                SIBUuid8 encodedME = SIMPUtils.parseME(destAddr.getDestinationName());

                if ((encodedME != null) && !(encodedME.equals(_messageProcessor.getMessagingEngineUuid())))
                    correctType = true;
            }
        }

        if (!correctType)
        {

            String type = "SYSTEM";
            if (expectedDestType != null)
            {
                type = expectedDestType.toString();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "checkDestinationType",
                           "Destination is of wrong type " + type + ":" + destination.getDestinationType().toString());
            // Get the string for the message inserts
            String msg = locateCorrectMessage(destination.getDestinationType(), expectedDestType);
            SIMPNotPossibleInCurrentConfigurationException e = null;
            if (!msg.equals("DELIVERY_ERROR_SIRC_14"))
            {
                e = new SIMPNotPossibleInCurrentConfigurationException(
                                nls_cwsik.getFormattedMessage(
                                                              msg,
                                                              new Object[] { destination.getName(),
                                                                            _messageProcessor.getMessagingEngineName() },
                                                              null));

                e.setExceptionReason(Integer.parseInt(msg.replaceFirst("DELIVERY_ERROR_SIRC_", "")));
                e.setExceptionInserts(new String[] { destination.getName(),
                                                    _messageProcessor.getMessagingEngineName() });
            }
            else
            {
                e = new SIMPNotPossibleInCurrentConfigurationException(
                                nls_cwsik.getFormattedMessage(
                                                              msg,
                                                              new Object[] { destination.getName(),
                                                                            _messageProcessor.getMessagingEngineName(),
                                                                            destination.getDestinationType().toString(),
                                                                            type },
                                                              null));

                e.setExceptionReason(SIRCConstants.SIRC0014_INCORRECT_DESTINATION_USAGE_ERROR);
                e.setExceptionInserts(new String[] { destination.getName(),
                                                    _messageProcessor.getMessagingEngineName(),
                                                    destination.getDestinationType().toString(),
                                                    type });
            }

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDestinationType");
    }

    /**
     * 
     * @return
     */
    private String locateCorrectMessage(DestinationType realType, DestinationType expectedType)
    {
        if (realType == DestinationType.QUEUE)
        {
            if (expectedType == DestinationType.TOPICSPACE)
                return "DELIVERY_ERROR_SIRC_2"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0159
            else if (expectedType == DestinationType.SERVICE)
                return "DELIVERY_ERROR_SIRC_3"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0160
            else if (expectedType == DestinationType.PORT)
                return "DELIVERY_ERROR_SIRC_4"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0161
            else
                return "DELIVERY_ERROR_SIRC_14"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0168
        }
        else if (realType == DestinationType.TOPICSPACE)
        {
            if (expectedType == DestinationType.QUEUE)
                return "DELIVERY_ERROR_SIRC_11"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0154
            else if (expectedType == DestinationType.SERVICE)
                return "DELIVERY_ERROR_SIRC_12"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0155
            else if (expectedType == DestinationType.PORT)
                return "DELIVERY_ERROR_SIRC_13"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0158 
            else
                return "DELIVERY_ERROR_SIRC_14"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0168     
        }
        else if (realType == DestinationType.SERVICE)
        {
            if (expectedType == DestinationType.QUEUE)
                return "DELIVERY_ERROR_SIRC_5"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0162
            else if (expectedType == DestinationType.TOPICSPACE)
                return "DELIVERY_ERROR_SIRC_6"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0163
            else if (expectedType == DestinationType.PORT)
                return "DELIVERY_ERROR_SIRC_7"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0164
            else
                return "DELIVERY_ERROR_SIRC_14"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0168
        }
        else if (realType == DestinationType.PORT)
        {
            if (expectedType == DestinationType.QUEUE)
                return "DELIVERY_ERROR_SIRC_8"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0165
            else if (expectedType == DestinationType.TOPICSPACE)
                return "DELIVERY_ERROR_SIRC_9"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0166
            else if (expectedType == DestinationType.SERVICE)
                return "DELIVERY_ERROR_SIRC_10"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0167
            else
                return "DELIVERY_ERROR_SIRC_14"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0168     
        }

        return "DELIVERY_ERROR_SIRC_14"; // INCORRECT_DESTINATION_USAGE_ERROR_CWSIP0168     
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#close()
     */
    @Override
    public void close()
                    throws SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "close", this);

        // Lock on the connections lock object to stop Message Processor from 
        // closing us
        _messageProcessor.getConnectionLockManager().lock();
        try
        {
            // Close the connection and remove the connection listeners.
            _close(true);
        } finally
        {
            _messageProcessor.getConnectionLockManager().unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "close");
    }

    /* PM39926-Start */
    @Override
    public void close(boolean bForceFlag) throws SIConnectionLostException, SIResourceException, SIErrorException {
        close();
    }

    /* PM39926-End */
    /**
     * Closes the object and removes all associated state.
     * 
     * @param removeConnectionListeners If this is set to true we remove the
     *            connection listeners. (Called by the regular connection.close method).
     *            The _close method is called directly by the stop method and doesn't
     *            want the connection listeners to be removed.
     * 
     */
    protected void _close(boolean removeConnectionListeners)
                    throws SIConnectionLostException, SIResourceException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "_close", Boolean.valueOf(removeConnectionListeners));
        // Synchronize on the closed object so that we can't change it while
        // someone else is trying to check it. This will also block all operations
        // until we have finished cleaning up.
        synchronized (this)
        {
            if (_closed)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "_close", "Returning as already closed");
                return;
            }

            _closed = true;

            // as the connection is now closed we need to log the user out.

            String busName = _messageProcessor.getBus().getName();

            _messageProcessor.getAuthorisationUtils().logout(busName, _subject);

            // Work down the list of active transactions owned by this connection
            // and roll them back.
            synchronized (_ownedTransactions)
            {
                Iterator<LocalTransaction> itr = _ownedTransactions.iterator();
                while (itr.hasNext())
                {
                    LocalTransaction tran = itr.next();
                    itr.remove();
                    try
                    {
                        tran.rollback(); // Rollback the transaction
                    } catch (SIException e)
                    {
                        // No FFDC code needed

                        // It is possible that the MS has had to rollback this transaction
                        // already so we shouldn't worry if we see this exception
                    }
                }
            }

            //Sequentially iterate over all of the sessions, closing them.
            //The internal _close() method is called in preference to close() on the API.
            //The standard close method will try to remove it's self from these lists
            //before actually closing. This would invalidate our iterators.
            //We will remove them from the lists ourselves using the specialized remove method
            //on the iterator.

            //synchronize on the list of producers etc. We might not actually need to
            //do this since no one else should be able to do anything while we have
            //the closed lock.
            synchronized (_producers)
            {
                Iterator<ProducerSession> itr = _producers.iterator();
                while (itr.hasNext())
                {
                    ProducerSessionImpl ps = (ProducerSessionImpl) itr.next();
                    itr.remove();
                    ps._close();
                }
            }

            // Close all the bifurcated consumers on this connection 
            // before closing all the consumers.
            synchronized (_bifurcatedConsumers)
            {
                Iterator<BifurcatedConsumerSessionImpl> itr = _bifurcatedConsumers.iterator();
                while (itr.hasNext())
                {
                    BifurcatedConsumerSessionImpl bcs = itr.next();
                    itr.remove();
                    bcs._close();
                }
            }

            synchronized (_consumers)
            {
                Iterator<ConsumerSessionImpl> itr = _consumers.iterator();
                while (itr.hasNext())
                {
                    ConsumerSessionImpl cs = itr.next();
                    itr.remove();
                    _messageProcessor.removeConsumer(cs);
                    cs._close();
                }
            }
            synchronized (_browsers)
            {
                Iterator<BrowserSession> itr = _browsers.iterator();
                while (itr.hasNext())
                {
                    BrowserSessionImpl bs = (BrowserSessionImpl) itr.next();
                    itr.remove();
                    bs._close();
                }
            }
            synchronized (_temporaryDestinations)
            {
                Iterator<String> itr = _temporaryDestinations.iterator();
                while (itr.hasNext())
                {
                    String destinationName = itr.next();
                    try
                    {
                        _destinationManager.deleteTemporaryDestination(SIMPUtils.createJsDestinationAddress(destinationName,
                                                                                                            _messageProcessor.getMessagingEngineUuid()));
                    } catch (SIException e)
                    {
                        // No FFDC code needed

                        SibTr.exception(tc, e);

                        // This destination will be deleted at restart.
                    }
                    itr.remove();
                }
            }

            if (removeConnectionListeners)
            {
                synchronized (_connectionListeners)
                {
                    Iterator<SICoreConnectionListener> itr = _connectionListeners.iterator();
                    while (itr.hasNext())
                    {
                        // Iterate through each of the connection listeners and remove them
                        itr.next();
                        itr.remove();
                    }
                }
            }

            // Close processing for consumer set monitors
            try
            {
                _messageProcessor.
                                getMessageProcessorMatching().removeConsumerSetMonitors(this);
            } catch (SINotPossibleInCurrentConfigurationException e)
            {
                // FFDC
                FFDCFilter.processException(e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl._close",
                                            "1:745:1.347.1.25",
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "_close", e);
                // Thrown if callback couldn't be found
                throw new SIErrorException(nls.getFormattedMessage(
                                                                   "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                   new Object[] { "ConnectionImpl",
                                                                                 "1:754:1.347.1.25" },
                                                                   null), e);
            }

            // Remove any destination listeners associated with this connection
            _destinationManager.removeDestinationListener(this);

            // Now the close is complete, remove this connection.
            _messageProcessor.removeConnection(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_close");
    }

    /**
     * This method simply removes a Browser Session from our list. It is generally
     * called by the Browser Session as it is closing down.
     * 
     * @param browser
     */
    void removeBrowserSession(BrowserSessionImpl browser)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeBrowserSession", browser);

        synchronized (_browsers)
        {
            _browsers.remove(browser);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeBrowserSession");
    }

    /**
     * This method simply removes a Consumer Session from our list. It is generally
     * called by the Consumer Session as it is closing down.
     * 
     * @param consumer
     */
    void removeConsumerSession(ConsumerSessionImpl consumer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeConsumerSession", consumer);

        synchronized (_consumers)
        {
            _consumers.remove(consumer);
        }

        _messageProcessor.removeConsumer(consumer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeConsumerSession");
    }

    /**
     * This method simply removes a Producer Session from our list. It is generally
     * called by the Producer Session as it is closing down.
     * 
     * @param producer
     */
    void removeProducerSession(ProducerSessionImpl producer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeProducerSession");
        synchronized (_producers)
        {
            _producers.remove(producer);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeProducerSession");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createUncoordinatedTransaction()
     */
    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createUncoordinatedTransaction");
        SIUncoordinatedTransaction tran = createUncoordinatedTransaction(true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createUncoordinatedTransaction", tran);
        return tran;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createUncoordinatedTransaction(boolean)
     */
    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction(boolean allowSubordinateResources)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createUncoordinatedTransaction",
                        new Object[] { this, Boolean.valueOf(allowSubordinateResources) });

        LocalTransaction tran = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the transaction.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();
            checkMPStarted();

            //Get a LocalTransaction from the message store (via the MP).
            tran = _txManager.createLocalTransaction(!allowSubordinateResources);

            synchronized (_ownedTransactions)
            {
                // Add this transaction to the connections list in case we need to
                // clean them up at close time.
                _ownedTransactions.add(tran);
                tran.registerCallback(this);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createUncoordinatedTransaction", tran);

        return tran;
    }

    private void checkProducerSessionNullParameters(SIDestinationAddress destAddr) throws SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkProducerSessionNullParameters", destAddr);

        if (destAddr == null)
        {
            SIMPIncorrectCallException e =
                            new SIMPIncorrectCallException(
                                            nls_cwsir.getFormattedMessage(
                                                                          "CREATE_PRODUCER_CWSIR0051",
                                                                          null,
                                                                          null));

            e.setExceptionReason(SIRCConstants.SIRC0900_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ConnectionImpl.checkProducerSessionNullParameters",
                                                "1:898:1.347.1.25" });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkProducerSessionNullParameters", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkProducerSessionNullParameters", destAddr);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public ProducerSession createProducerSession(
                                                 SIDestinationAddress destAddr,
                                                 String discriminator,
                                                 DestinationType destType,
                                                 OrderingContext extendedMessageOrderingContext,
                                                 String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SITemporaryDestinationNotFoundException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException
    {
        ProducerSession session =
                        createProducerSession(destAddr,
                                              discriminator,
                                              destType,
                                              extendedMessageOrderingContext,
                                              alternateUser,
                                              false,
                                              false);

        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public ProducerSession createProducerSession(
                                                 SIDestinationAddress destAddr,
                                                 String discriminator,
                                                 DestinationType destType,
                                                 OrderingContext extendedMessageOrderingContext,
                                                 String alternateUser,
                                                 boolean fixedQueuePoint,
                                                 boolean preferLocal)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SITemporaryDestinationNotFoundException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException
    {
        ProducerSession session =
                        createProducerSession(destAddr,
                                              discriminator,
                                              destType,
                                              extendedMessageOrderingContext,
                                              alternateUser,
                                              fixedQueuePoint,
                                              preferLocal,
                                              true);

        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public ProducerSession createProducerSession(
                                                 SIDestinationAddress destAddr,
                                                 String discriminator,
                                                 DestinationType destType,
                                                 OrderingContext extendedMessageOrderingContext,
                                                 String alternateUser,
                                                 boolean fixedQueuePoint,
                                                 boolean preferLocal,
                                                 boolean clearPubSubFingerprints)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SITemporaryDestinationNotFoundException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createProducerSession",
                        new Object[] { destAddr,
                                      discriminator,
                                      destType,
                                      extendedMessageOrderingContext,
                                      alternateUser,
                                      Boolean.valueOf(fixedQueuePoint),
                                      Boolean.valueOf(preferLocal),
                                      Boolean.valueOf(clearPubSubFingerprints),
                                      this });

        // See if this connection has been closed
        checkNotClosed();

        checkProducerSessionNullParameters(destAddr);

        SecurityContext secContext = null;

        // If security is enabled then we need to set up a security context
        if (_isBusSecure)
        {
            // Feature 219101: Add the alternate user string to the security context
            secContext = new SecurityContext(this._subject,
                            alternateUser,
                            discriminator,
                            _messageProcessor.getAuthorisationUtils());
        }
        boolean keepSecurityUserid = false;
        ProducerSession session = internalCreateProducerSession(destAddr,
                                                                destType,
                                                                false,
                                                                secContext,
                                                                keepSecurityUserid,
                                                                fixedQueuePoint,
                                                                preferLocal,
                                                                clearPubSubFingerprints,
                                                                discriminator);

        // Set the style of discriminator access for the producer session
        if (discriminator != null && session != null)
            ((ProducerSessionImpl) session).disableDiscriminatorAccessCheckAtSend(discriminator);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createProducerSession", session);
        return session;
    }

    /**
     * Method that creates the producer session.
     * 
     * @param destAddress
     * @param destinationType
     * @param system
     * @param discriminator
     * @param testDiscrimAtCreate
     * @param fixedMessagePoint
     * @param preferLocal
     * @return
     * @throws SINotAuthorizedException
     * @throws SIDestinationNotFoundException
     * @throws SIDestinationWrongTypeException
     * @throws SIDestinationLockedException
     * @throws SIObjectClosedException
     * @throws SICoreException
     */
    private ProducerSession internalCreateProducerSession(
                                                          SIDestinationAddress destAddress,
                                                          DestinationType destinationType,
                                                          boolean system,
                                                          SecurityContext secContext,
                                                          boolean keepSecurityUserid,
                                                          boolean fixedMessagePoint,
                                                          boolean preferLocal,
                                                          boolean clearPubSubFingerprints,
                                                          String discriminator)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException,
                    SITemporaryDestinationNotFoundException, SIIncorrectCallException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "internalCreateProducerSession",
                        new Object[] { destAddress,
                                      destinationType,
                                      system,
                                      secContext,
                                      keepSecurityUserid,
                                      fixedMessagePoint,
                                      preferLocal,
                                      clearPubSubFingerprints });

        String destinationName = destAddress.getDestinationName();
        String busName = destAddress.getBusName();
        DestinationHandler destination =
                        _destinationManager.getDestination(destinationName, busName, false, true);

        // Check the destination type
        checkDestinationType(destinationType, destAddress, destination, system);

        // Check authority to produce to destination
        // If security is disabled then we'll bypass the check
        // Security changes for Liberty Messaging: Sharath Start
        // Remove the If condition, since the proxy class handles it
        checkDestinationAuthority(destination, MessagingSecurityConstants.OPERATION_TYPE_SEND, discriminator);
        // Security changes for Liberty Messaging: Sharath End

        ProducerSession producer = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the producer.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            producer =
                            new ProducerSessionImpl(destAddress,
                                            destination,
                                            this,
                                            secContext,
                                            keepSecurityUserid,
                                            fixedMessagePoint,
                                            preferLocal,
                                            clearPubSubFingerprints);

            synchronized (_producers)
            {
                //store a reference to that producer session so that we can close
                //it again later if needed
                _producers.add(producer);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalCreateProducerSession", producer);
        return producer; //169892
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public ProducerSession createProducerSession(SIDestinationAddress destAddr,
                                                 DestinationType destinationType,
                                                 OrderingContext extendedMessageOrderingContext,
                                                 String alternateUser)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SITemporaryDestinationNotFoundException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createProducerSession",
                        new Object[] { destAddr,
                                      destinationType,
                                      extendedMessageOrderingContext,
                                      alternateUser,
                                      this });

        // See if this connection has been closed
        checkNotClosed();

        checkProducerSessionNullParameters(destAddr);

        SecurityContext secContext = null;
        // If security is enabled then we need to set up a security context
        if (_isBusSecure)
        {
            // Feature 219101: Add the alternate user string to the security context
            secContext = new SecurityContext(_subject,
                            alternateUser,
                            null,
                            _messageProcessor.getAuthorisationUtils());
        }

        boolean keepSecurityUserid = false;

        ProducerSession producer =
                        internalCreateProducerSession(destAddr,
                                                      destinationType,
                                                      false,
                                                      secContext,
                                                      keepSecurityUserid,
                                                      false,
                                                      true,
                                                      true,
                                                      null);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createProducerSession", producer);
        //Destination is on the local bus
        return producer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#createMQInterOpConsumerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria, com.ibm.websphere.sib.Reliability, boolean, boolean, com.ibm.websphere.sib.Reliability, boolean, java.lang.String, boolean)
     */
    @Override
    public ConsumerSession createMQInterOpConsumerSession(SIDestinationAddress destAddress,
                                                          DestinationType destinationType,
                                                          SelectionCriteria criteria,
                                                          Reliability reliability,
                                                          boolean enableReadAhead,
                                                          boolean nolocal,
                                                          Reliability unrecoverableReliability,
                                                          boolean bifurcatable,
                                                          String alternateUser,
                                                          boolean forwardScanning,
                                                          boolean system)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createMQInterOpConsumerSession",
                        new Object[] { this,
                                      destAddress,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(nolocal),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      alternateUser,
                                      Boolean.valueOf(forwardScanning),
                                      Boolean.valueOf(system) });

        // See if this connection has been closed
        checkNotClosed();

        checkConsumerSessionNullParameters(destAddress);

        ConsumerSession session =
                        internalCreateConsumerSession(destAddress,
                                                      alternateUser,
                                                      destinationType,//type    
                                                      criteria,
                                                      reliability,
                                                      enableReadAhead,
                                                      nolocal,
                                                      forwardScanning,
                                                      system,
                                                      unrecoverableReliability,
                                                      bifurcatable,
                                                      true, // mqinterop
                                                      true,
                                                      false // gatherMessages
                        );

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createMQInterOpConsumerSession", session);

        return session;
    }

    /**
     * Checks to see if the destination is temporary and the connection
     * used to create the temp destination is the same as the one trying to
     * access it.
     * 
     * Checking skipped if this is a mqinterop request
     * 
     * @param destination
     * @param mqinterop
     * @throws SITemporaryDestinationNotFoundException If the temp destination wasn't
     *             created using the current connection.
     */
    private void checkTemporary(DestinationHandler destination, boolean mqinterop)

                    throws SITemporaryDestinationNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkTemporary", new Object[] { destination, Boolean.valueOf(mqinterop) });

        // If a Temporary Destination, ensure it is on this connection unless mqinterop
        if (destination.isTemporary()
            && !mqinterop
            && (_temporaryDestinations.indexOf(destination.getName()) == -1))
        {
            SIMPTemporaryDestinationNotFoundException e =
                            new SIMPTemporaryDestinationNotFoundException(
                                            nls.getFormattedMessage(
                                                                    "TEMPORARY_DESTINATION_CONNECTION_ERROR_CWSIP0099",
                                                                    new Object[] { destination.getName() },
                                                                    null));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "checkTemporary", e);
            }
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkTemporary");
    }

    /**
     * Internal method for creating the consumer.
     * 
     * @param destAddr
     * @param alternateUser
     * @param destinationType
     * @param discriminator
     * @param selector
     * @param reliability
     * @param enableReadAhead
     * @param nolocal
     * @param forwardScanning
     * @param system
     * @param unrecoverableReliability
     * @param bifurcatable
     * @param mqinterop
     * @return
     */
    private ConsumerSession internalCreateConsumerSession(
                                                          SIDestinationAddress destAddr,
                                                          String alternateUser,
                                                          DestinationType destinationType,
                                                          SelectionCriteria criteria,
                                                          Reliability reliability,
                                                          boolean enableReadAhead,
                                                          boolean nolocal,
                                                          boolean forwardScanning,
                                                          boolean system,
                                                          Reliability unrecoverableReliability,
                                                          boolean bifurcatable,
                                                          boolean mqinterop,
                                                          boolean ignoreInitialIndoubts,
                                                          boolean gatherMessages)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SINotPossibleInCurrentConfigurationException, SITemporaryDestinationNotFoundException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "internalCreateConsumerSession",
                        new Object[] {
                                      destAddr,
                                      alternateUser,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(nolocal),
                                      Boolean.valueOf(forwardScanning),
                                      Boolean.valueOf(system),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      Boolean.valueOf(mqinterop),
                                      Boolean.valueOf(ignoreInitialIndoubts),
                                      Boolean.valueOf(gatherMessages) });

        try {
            return internalCreateConsumerSession(null,
                                                 destAddr,
                                                 alternateUser,
                                                 destinationType,
                                                 criteria,
                                                 reliability,
                                                 enableReadAhead,
                                                 nolocal,
                                                 forwardScanning,
                                                 system,
                                                 unrecoverableReliability,
                                                 bifurcatable,
                                                 mqinterop,
                                                 ignoreInitialIndoubts,
                                                 gatherMessages);

        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "internalCreateConsumerSession");
        }
    }

    private ConsumerSession internalCreateConsumerSession(String subscriptionName,
                                                          SIDestinationAddress destAddr,
                                                          String alternateUser,
                                                          DestinationType destinationType,
                                                          SelectionCriteria criteria,
                                                          Reliability reliability,
                                                          boolean enableReadAhead,
                                                          boolean nolocal,
                                                          boolean forwardScanning,
                                                          boolean system,
                                                          Reliability unrecoverableReliability,
                                                          boolean bifurcatable,
                                                          boolean mqinterop,
                                                          boolean ignoreInitialIndoubts,
                                                          boolean gatherMessages)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SINotPossibleInCurrentConfigurationException, SITemporaryDestinationNotFoundException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "internalCreateConsumerSession",
                        new Object[] {
                                      subscriptionName,
                                      destAddr,
                                      alternateUser,
                                      destinationType,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(nolocal),
                                      Boolean.valueOf(forwardScanning),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      Boolean.valueOf(mqinterop),
                                      Boolean.valueOf(ignoreInitialIndoubts),
                                      Boolean.valueOf(gatherMessages) });

        String destName = destAddr.getDestinationName();

        // Get the destination. If it is a remote queue, then we may need
        // to get its definition from Admin
        // (Despite not being able to consume from a foreign bus the bus name
        // should be used just incase this is an alias to a local bus destination) 
        DestinationHandler destination = _destinationManager.getDestination(destName, destAddr.getBusName(), false);

        // We may be using an alias, resolve to the actual destination
        DestinationHandler resolvedDestination = destination.getResolvedDestinationHandler();

        // Check that the destination they're trying to consume from is in the local
        // bus - we only allow consumption from destinations in this bus
        if (!resolvedDestination.getBus().equals(_messageProcessor.getMessagingEngineBus()))
        {
            SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "CONSUME_FROM_FOREIGN_BUS_ERROR_CWSIP0030",
                                                    new Object[] { resolvedDestination.getName(), resolvedDestination.getBus(), _messageProcessor.getMessagingEngineBus() },
                                                    null));

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateConsumerSession", e);

            throw e;
        }

        // Check the destination type
        checkDestinationType(destinationType, destAddr, destination, system);

        if ((destination.getDestinationType() == DestinationType.SERVICE))
        {
            //Cant create a consumer to a service destination
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateConsumerSession", "cannot browse a service destination");

            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "INVALID_DESTINATION_USAGE_ERROR_CWSIP0021",
                                                    new Object[] { destination.getName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        checkTemporary(destination, mqinterop);

        String topicName = null;
        if (criteria != null)
            topicName = criteria.getDiscriminator();

        // Check authority to receive from destination
        // If security is disabled then we'll bypass the check
        checkDestinationAuthority(destination, MessagingSecurityConstants.OPERATION_TYPE_RECEIVE, topicName);

        //checkQOS(destination, reliability);

        ConsumerSessionImpl consumer = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the consumer.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            //create a state object for this consumer session
            //In this basic form it is just a wrapper for the discriminator and selector 
            ConsumerDispatcherState state = new ConsumerDispatcherState(
                            subscriptionName,
                            destination.getUuid(),
                            criteria,
                            nolocal,
                            null, // null durableHome ok since we're not trying to create a durable subscription
                            destination.getName(),
                            destination.getBus());

            // Set the user into the state for security checks
            state.setUser(getResolvedUserid(), isSIBServerSubject());

            //set clone id to true if subscriptionName is not null i.e in case of shared non-durable
            if (null != subscriptionName)
                state.setIsCloned(true);

            try
            {
                //create a new ConsumerSession
                consumer =
                                new ConsumerSessionImpl(
                                                destination,
                                                destAddr,
                                                state,
                                                this,
                                                enableReadAhead,
                                                forwardScanning,
                                                unrecoverableReliability,
                                                bifurcatable,
                                                ignoreInitialIndoubts,
                                                gatherMessages);
            } catch (SIDurableSubscriptionNotFoundException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalCreateConsumerSession",
                                            "1:1502:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:1513:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:1522:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SIDurableSubscriptionMismatchException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalCreateConsumerSession",
                                            "1:1534:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:1545:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:1554:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SINonDurableSubscriptionMismatchException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalCreateConsumerSession",
                                            "1:7704:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:7715:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:7724:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SISessionUnavailableException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalCreateConsumerSession",
                                            "1:1566:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:1577:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:1586:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            }

            synchronized (_consumers)
            {
                //store a reference
                _consumers.add(consumer);
            }

            _messageProcessor.addConsumer(consumer);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalCreateConsumerSession", consumer);

        return consumer;
    }

    /*
     * (non-Javadoc)
     * This method is introduced newly in V9 for JMS 2.0 non-durable shared consumer.
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createSharedConsumerSession(java.lang.String, com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType, com.ibm.wsspi.sib.core.SelectionCriteria, com.ibm.websphere.sib.Reliability, boolean, boolean, boolean,
     * com.ibm.websphere.sib.Reliability, boolean, java.lang.String, boolean, boolean, java.util.Map)
     */
    @Override
    public ConsumerSession createSharedConsumerSession(String subscriptionName, SIDestinationAddress destAddr, DestinationType destType, SelectionCriteria criteria,
                                                       Reliability reliability, boolean enableReadAhead, boolean supportsMultipleConsumers, boolean nolocal,
                                                       Reliability unrecoverableReliability, boolean bifurcatable, String alternateUser, boolean ignoreInitialIndoubts,
                                                       boolean allowMessageGathering, Map<String, String> messageControlProperties)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SINotPossibleInCurrentConfigurationException, SITemporaryDestinationNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "createSharedConsumerSession",
                        new Object[] {
                                      subscriptionName,
                                      destAddr,
                                      criteria,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal),
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      alternateUser });

        ConsumerSession session =
                        internalCreateConsumerSession(
                                                      subscriptionName,
                                                      destAddr,
                                                      alternateUser,
                                                      destType,
                                                      criteria,
                                                      reliability,
                                                      enableReadAhead,
                                                      nolocal,
                                                      false,
                                                      false,
                                                      unrecoverableReliability,
                                                      bifurcatable,
                                                      false,
                                                      ignoreInitialIndoubts,
                                                      false // gatherMessages
                        );

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createSharedConsumerSession", session);
        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createDurableSubscription(java.lang.String, java.lang.String, com.ibm.websphere.sib.SIDestinationAddress, java.lang.String,
     * java.lang.String, boolean, boolean)
     */
    @Override
    public void createDurableSubscription(String subscriptionName,
                                          String durableSubscriptionHome,
                                          SIDestinationAddress destinationAddress,
                                          SelectionCriteria criteria,
                                          boolean supportsMultipleConsumers,
                                          boolean nolocal,
                                          String alternateUser)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDurableSubscriptionAlreadyExistsException
    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "createDurableSubscription",
                        new Object[] {
                                      this,
                                      subscriptionName,
                                      durableSubscriptionHome,
                                      destinationAddress,
                                      alternateUser,
                                      criteria,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal) });

        SelectionCriteria[] selectionCriteriaList = null;
        // if(criteria != null)
        //{
        selectionCriteriaList = new SelectionCriteria[] { criteria };
        // }
        internalCreateDurableSubscription(subscriptionName,
                                          durableSubscriptionHome,
                                          destinationAddress,
                                          supportsMultipleConsumers,
                                          nolocal,
                                          alternateUser,
                                          selectionCriteriaList, // selectionCriteria array
                                          null); // userData 

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createDurableSubscription");

    }

    /**
     * An extended version of SICoreConnection.createDurableSubscription which includes
     * a list of selection criteria and a map for user data (these abilities are also available
     * independently from the create, see below)
     * 
     * Use of either of the above two parameters is only valid for locally homed subscriptions
     */
    @Override
    public void createDurableSubscription(
                                          String subscriptionName,
                                          String durableSubscriptionHome,
                                          SIDestinationAddress destinationAddress,
                                          SelectionCriteria[] criteriaList,
                                          boolean supportsMultipleConsumers,
                                          boolean nolocal,
                                          String alternateUser,
                                          Map userData)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDurableSubscriptionAlreadyExistsException

    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "createDurableSubscription",
                        new Object[] {
                                      this,
                                      subscriptionName,
                                      durableSubscriptionHome,
                                      destinationAddress,
                                      alternateUser,
                                      criteriaList,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal) });

        internalCreateDurableSubscription(subscriptionName,
                                          durableSubscriptionHome,
                                          destinationAddress,
                                          supportsMultipleConsumers,
                                          nolocal,
                                          alternateUser,
                                          criteriaList,
                                          (HashMap) userData);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createDurableSubscription");

    }

    /* 
   * 
   */
    public void internalCreateDurableSubscription(String subscriptionName,
                                                  String durableSubscriptionHome,
                                                  SIDestinationAddress destinationAddress,
                                                  boolean supportsMultipleConsumers,
                                                  boolean nolocal,
                                                  String alternateUser,
                                                  SelectionCriteria[] criteriaList,
                                                  HashMap userData)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDurableSubscriptionAlreadyExistsException
    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "internalCreateDurableSubscription",
                        new Object[] {
                                      this,
                                      subscriptionName,
                                      durableSubscriptionHome,
                                      destinationAddress,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal),
                                      alternateUser,
                                      criteriaList,
                                      userData });

        // Check that the destination information is correct.
        SIBUuid8 durableHomeID =
                        checkDurableSubscriptionInformation(subscriptionName,
                                                            durableSubscriptionHome,
                                                            destinationAddress,
                                                            supportsMultipleConsumers,
                                                            nolocal,
                                                            false,
                                                            false);

        //get the destination
        DestinationHandler destination = null;
        try
        {
            destination =
                            _destinationManager.getDestination(
                                                               (JsDestinationAddress) destinationAddress,
                                                               false);
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateDurableSubscription", "SINotPossibleInCurrentConfigurationException");
            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "DURABLE_SUBSCRIPTION_USAGE_ERROR_CWSIP0098",
                                                    new Object[] { subscriptionName, destinationAddress.getDestinationName() },
                                                    null));
        }

        // Check the destination is a topicSpace
        if (!destination.isPubSub())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateDurableSubscription", "Destination not a topicspace");

            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "DESTINATION_USEAGE_ERROR_CWSIP0141",
                                                    new Object[] { destinationAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName(),
                                                                  subscriptionName },
                                                    null));
        }
        else if (destination.isTemporary())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateDurableSubscription", "Destination is Temporary");

            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "DURABLE_SUBSCRIPTION_USAGE_ERROR_CWSIP0098",
                                                    new Object[] { subscriptionName, destinationAddress.getDestinationName() },
                                                    null));
        }

        //The state for this subscription
        ConsumerDispatcherState subState = null;

        boolean hasAtLeastOneTopic = false;

        SecurityContext secContext = null; //this will be set if security is enabled

        if (_isBusSecure)
        {
            // Feature 219101: Add the alternate user string to the security context
            secContext = new SecurityContext(_subject,
                            alternateUser,
                            null,
                            _messageProcessor.getAuthorisationUtils());

            // Set the discriminator to null in this check, so that we check access to
            // the destination only. We break out the discriminator check into a 
            // separate loop below so that allowable discriminators can be processed while
            // those that are disallowed will be discarded.                                     
        }

        String topicName = null;
        if (criteriaList != null) {
            SelectionCriteria criteria = criteriaList[0];
            if (criteria != null)
                topicName = criteria.getDiscriminator();
        }

        checkDestinationAuthority(destination, MessagingSecurityConstants.OPERATION_TYPE_RECEIVE, topicName);

        //Need to check every discriminator if we have an array of multiple criteria
        //We are checking for duplicate criterias (see defect 355556) and if
        //security is enabled we need to check each individual topic too.
        if (criteriaList != null)
        {
            // A string of disallowed discriminators
            String failedDiscriminators = null;
            for (int i = 0; i < (criteriaList.length); i++)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "examining  selection criteria " + criteriaList[i]);

                boolean topicIsAllowed = true; //we assume the topic is allowed
                if (_isBusSecure)
                {
                    //Security is enabled so we need to check if this topic is allowed
                    String discriminator = null;
                    if (criteriaList[i] != null)
                    {
                        discriminator = criteriaList[i].getDiscriminator();
                    }

                    //NOTE: we do not check permission for null discriminators
                    if (discriminator != null)
                    {
                        topicIsAllowed =
                                        checkConsumerDiscriminatorAccess(destination,
                                                                         discriminator,
                                                                         secContext);
                    }

                    if (!topicIsAllowed)
                    {
                        // Assemble a string of disallowed discriminators for use in logging
                        if (failedDiscriminators == null)
                        {
                            failedDiscriminators = discriminator;
                        }
                        else
                        {
                            failedDiscriminators = failedDiscriminators + ", " + discriminator;
                        }
                    }

                }//end if(_isBusSecure)

                if (topicIsAllowed)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "adding selection criteria " + criteriaList[i]);

                    if (subState == null)
                    {
                        hasAtLeastOneTopic = true;
                        //This is the first selection criteria in the list that we are allowing
                        //so create a new state object representing the subscription and
                        //this allowed selection criteria. 
                        subState =
                                        new ConsumerDispatcherState(
                                                        subscriptionName,
                                                        destination.getUuid(),
                                                        nolocal,
                                                        durableSubscriptionHome,
                                                        destination.getName(),
                                                        destination.getBus(),
                                                        new SelectionCriteria[] { criteriaList[i] },
                                                        userData);
                    }
                    else
                    {
                        //add this new selection criteria to the list - this will
                        //check for duplicates
                        subState.addSelectionCriteria(criteriaList[i]); //NOTE: this will ignore null selection criterias
                                                                        //This might be a bug
                    }
                }
                else
                {
                    // This is a null selection criteria - ignore it
                    // but output a debug
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "null selection criteria");
                }
            } // eof loop round list of SelectionCriteria

            if (_isBusSecure && !hasAtLeastOneTopic)
            {
                // Thrown if NO discriminator in the set was allowed access
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "internalCreateDurableSubscription",
                               "SINotAuthorizedException - allowedCritList.isEmpty()");
                throw new SINotAuthorizedException(
                                nls.getFormattedMessage(
                                                        "USER_NOT_AUTH_RECEIVE_ERROR_CWSIP0310",
                                                        new Object[] { destination.getName(),
                                                                      failedDiscriminators,
                                                                      secContext.getUserName(false) },
                                                        null));
            }
        }

        if (destination.isOrdered() && supportsMultipleConsumers)
        {
            supportsMultipleConsumers = false;
            // Override and warn if ordered destination
            SibTr.warning(tc, "CLONED_SUBSCRIBER_OVERRIDE_CWSIP0028",
                          new Object[] { subscriptionName, destination.getName() });
        }

        subState.setIsCloned(supportsMultipleConsumers);

        // Set the user into the state for security checks
        // we'll set the alternate user if that is supplied
        if (alternateUser == null)
            subState.setUser(getResolvedUserid(), isSIBServerSubject());
        else
            subState.setUser(alternateUser, false);

        // Is durableHome local?
        if (durableHomeID.equals(_messageProcessor.getMessagingEngineUuid()))
            // We're directly attached to the durable home
            destination.createDurableSubscription(subState, null);
        else
            // Durable home is not local so send off the request
            DurableInputHandler.createRemoteDurableSubscription(
                                                                _messageProcessor,
                                                                subState,
                                                                durableHomeID,
                                                                destination.getUuid());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalCreateDurableSubscription");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSessionForDurableSubscription(java.lang.String, java.lang.String, com.ibm.websphere.sib.SIDestinationAddress,
     * java.lang.String, java.lang.String, boolean, boolean, com.ibm.websphere.sib.Reliability, boolean, com.ibm.websphere.sib.Reliability, com.ibm.wsspi.sib.core.OrderingContext)
     */
    @Override
    public ConsumerSession createConsumerSessionForDurableSubscription(String subscriptionName,
                                                                       String durableSubscriptionHome,
                                                                       SIDestinationAddress destinationAddress,
                                                                       SelectionCriteria criteria,
                                                                       boolean supportsMultipleConsumers,
                                                                       boolean nolocal,
                                                                       Reliability reliability,
                                                                       boolean enableReadAhead,
                                                                       Reliability unrecoverableReliability,
                                                                       boolean bifurcatable,
                                                                       String alternateUser)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException,
                    SIDurableSubscriptionNotFoundException, SIDurableSubscriptionMismatchException,
                    SIDestinationLockedException, SIIncorrectCallException, SIResourceException, SINotAuthorizedException
    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "createConsumerSessionForDurableSubscription",
                        new Object[] {
                                      this,
                                      subscriptionName,
                                      durableSubscriptionHome,
                                      destinationAddress,
                                      criteria,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal),
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      alternateUser });

        ConsumerSession session =
                        internalCreateConsumerSessionForDurableSubscription(subscriptionName,
                                                                            durableSubscriptionHome,
                                                                            destinationAddress,
                                                                            criteria,
                                                                            supportsMultipleConsumers,
                                                                            nolocal,
                                                                            reliability,
                                                                            enableReadAhead,
                                                                            unrecoverableReliability,
                                                                            bifurcatable,
                                                                            alternateUser,
                                                                            false);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createConsumerSessionForDurableSubscription", session);
        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSessionForDurableSubscription(java.lang.String, com.ibm.websphere.sib.SIDestinationAddress, boolean,
     * com.ibm.websphere.sib.Reliability, boolean)
     */
    @Override
    public ConsumerSession createConsumerSessionForDurableSubscription(String subscriptionName,
                                                                       boolean enableReadAhead,
                                                                       Reliability unrecoverableReliability,
                                                                       boolean bifurcatable)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException,
                    SIDurableSubscriptionNotFoundException, SIDurableSubscriptionMismatchException,
                    SIDestinationLockedException, SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "createConsumerSessionForDurableSubscription",
                        new Object[] {
                                      this,
                                      subscriptionName,
                                      Boolean.valueOf(enableReadAhead),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable) });

        // Do not need to check that the destination information is correct.
        // As we are using the subscription data straight from the named subscription

        // Find the ConsumerDispatcher for this subscription
        // Note that it must be homed on this ME
        HashMap durableSubs = _destinationManager.getDurableSubscriptionsTable();

        ConsumerDispatcher cd = null;
        synchronized (durableSubs)
        {
            //Look up the consumer dispatcher for this subId in the system durable subs list
            cd =
                            (ConsumerDispatcher) durableSubs.get(subscriptionName);

            // Check that the durable subscription exists
            if (cd == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createConsumerSessionForDurableSubscription");

                throw new SIDurableSubscriptionNotFoundException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
                                                        new Object[] { subscriptionName,
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }
        }

        //Get the destinationHandler from the ConsumerDispatcher
        //Note that we know it is PubSub  
        DestinationHandler destination = cd.getDestination();
        SIDestinationAddress destinationAddress = SIMPUtils.createJsDestinationAddress(
                                                                                       destination.getName(),
                                                                                       _messageProcessor.getMessagingEngineUuid());

        //Get the state object representing the subscription from the ConsumerDispatcher
        ConsumerDispatcherState subState = cd.getConsumerDispatcherState();

        ConsumerSessionImpl consumer = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the subscription.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            //create the consumer session for this subscription
            try
            {
                consumer =
                                new ConsumerSessionImpl(destination,
                                                destinationAddress,
                                                subState,
                                                this,
                                                enableReadAhead,
                                                false,
                                                unrecoverableReliability,
                                                bifurcatable,
                                                true,
                                                false);
            } catch (SINotPossibleInCurrentConfigurationException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createConsumerSessionForDurableSubscription", "SIErrorException");
                throw new SIErrorException(e);
            } catch (SISessionUnavailableException e)
            {
                // FFDC
                FFDCFilter
                                .processException(
                                                  e,
                                                  "com.ibm.ws.sib.processor.impl.ConnectionImpl.createConsumerSessionForDurableSubscription",
                                                  "1:2129:1.347.1.25",
                                                  this);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:1809:1.285",
                                          e });

                // This should never be thrown
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createConsumerSessionForDurableSubscription", "SIErrorException");
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:2146:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SITemporaryDestinationNotFoundException e)
            {
                // FFDC
                FFDCFilter
                                .processException(
                                                  e,
                                                  "com.ibm.ws.sib.processor.impl.ConnectionImpl.createConsumerSessionForDurableSubscription",
                                                  "1:2159:1.347.1.25",
                                                  this);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:2165:1.347.1.25",
                                          e });

                // This should never be thrown
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createConsumerSessionForDurableSubscription", "SIErrorException");
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:2176:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SINonDurableSubscriptionMismatchException e)
            {
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:1953:1.347.1.25",
                                          e });

                // This should never be thrown
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createConsumerSessionForDurableSubscription", "SIErrorException");
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:1964:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            }

            synchronized (_consumers)
            {
                //store a reference
                _consumers.add(consumer);
            }

            _messageProcessor.addConsumer(consumer);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createConsumerSessionForDurableSubscription", consumer);
        return consumer;
    }

    /**
     * Internal method that creates the durable subscription
     */
    private ConsumerSession
                    internalCreateConsumerSessionForDurableSubscription(String subscriptionName,
                                                                        String durableSubscriptionHome,
                                                                        SIDestinationAddress destinationAddress,
                                                                        SelectionCriteria criteria,
                                                                        boolean supportsMultipleConsumers,
                                                                        boolean nolocal,
                                                                        Reliability reliability,
                                                                        boolean enableReadAhead,
                                                                        Reliability unrecoverableReliability,
                                                                        boolean bifurcatable,
                                                                        String alternateUser,
                                                                        boolean forwardScanning)
                                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                                    SIIncorrectCallException,
                                    SIDurableSubscriptionNotFoundException, SIDurableSubscriptionMismatchException,
                                    SIDestinationLockedException, SINotAuthorizedException

    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "internalCreateConsumerSessionForDurableSubscription",
                        new Object[] { subscriptionName,
                                      durableSubscriptionHome,
                                      destinationAddress,
                                      alternateUser,
                                      criteria,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal),
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      unrecoverableReliability });

        // Check that the destination information is correct.
        SIBUuid8 durableHomeID =
                        checkDurableSubscriptionInformation(subscriptionName,
                                                            durableSubscriptionHome,
                                                            destinationAddress,
                                                            supportsMultipleConsumers,
                                                            nolocal,
                                                            false,
                                                            true);

        // Check that the destination name of the real destination and the 
        // Destination supplied on the attachToDurable Sub match.
        HashMap durableSubs = _destinationManager.getDurableSubscriptionsTable();

        ConsumerDispatcher cd = null;
        // Is durableHome local?
        if (durableHomeID.equals(_messageProcessor.getMessagingEngineUuid()))
        {
            synchronized (durableSubs)
            {
                //Look up the consumer dispatcher for this subId in the system durable subs list
                cd =
                                (ConsumerDispatcher) durableSubs.get(subscriptionName);

                // Check that the durable subscription exists
                if (cd == null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription");

                    throw new SIDurableSubscriptionNotFoundException(
                                    nls.getFormattedMessage(
                                                            "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
                                                            new Object[] { subscriptionName,
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));
                }
                else if (cd.getConsumerDispatcherState().getTargetDestination() != null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription");

                    throw new SIDurableSubscriptionMismatchException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_SUBSCRIPTION_ACCESS_DISALLOWED_CWSIP0147",
                                                            new Object[] { subscriptionName,
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));
                }
            }
        }

        //get the destination
        DestinationHandler destination = null;
        try
        {
            destination =
                            _destinationManager.getDestination(
                                                               (JsDestinationAddress) destinationAddress,
                                                               false);
        } catch (SINotPossibleInCurrentConfigurationException e)
        {
            // No FFDC code needed 

            // This error means that the destination name of the destination supplied 
            // and the destination of the consumer don't match.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", "SIDurableSubscriptionMismatchException");
            throw new SIDurableSubscriptionMismatchException(
                            nls.getFormattedMessage(
                                                    "DURABLE_MISMATCH_ERROR_CWSIP0025",
                                                    new Object[] {
                                                                  destinationAddress.getDestinationName(),
                                                                  ((cd.getDestination() == null) ? null : cd.getDestination().getName()),
                                                                  subscriptionName,
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));

        } catch (SITemporaryDestinationNotFoundException e)
        {
            // No FFDC code needed

            // This error means that the destination name of the destination supplied 
            // and the destination of the consumer don't match.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", "SIDurableSubscriptionMismatchException");
            throw new SIDurableSubscriptionMismatchException(
                            nls.getFormattedMessage(
                                                    "DURABLE_MISMATCH_ERROR_CWSIP0025",
                                                    new Object[] {
                                                                  destinationAddress.getDestinationName(),
                                                                  ((cd.getDestination() == null) ? null : cd.getDestination().getName()),
                                                                  subscriptionName,
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        // Check the destination is a topicSpace
        if (!destination.isPubSub())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", "Destination not a topicspace");

            throw new SIDurableSubscriptionMismatchException(
                            nls.getFormattedMessage(
                                                    "DESTINATION_USEAGE_ERROR_CWSIP0141",
                                                    new Object[] { destinationAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName(),
                                                                  subscriptionName },
                                                    null));
        }

        String topicName = null;
        if (criteria != null)
            topicName = criteria.getDiscriminator();

        checkDestinationAuthority(destination, MessagingSecurityConstants.OPERATION_TYPE_RECEIVE, topicName);

        //create a state object representing a subscription
        ConsumerDispatcherState subState =
                        new ConsumerDispatcherState(
                                        subscriptionName,
                                        destination.getUuid(),
                                        criteria,
                                        nolocal,
                                        durableSubscriptionHome,
                                        destination.getName(),
                                        destination.getBus());

        if (destination.isOrdered() && supportsMultipleConsumers)
        {
            supportsMultipleConsumers = false;
            // Override and warn if ordered destination
            SibTr.warning(tc, "CLONED_SUBSCRIBER_OVERRIDE_CWSIP0028",
                          new Object[] { subscriptionName, destination.getName() });
        }

        subState.setIsCloned(supportsMultipleConsumers);

        // Set the user into the state for security checks
        // we'll set the alternate user if that is supplied
        if (alternateUser == null)
        {
            subState.setUser(getResolvedUserid(), isSIBServerSubject());
        }
        else
        {
            subState.setUser(alternateUser, false);
        }

        ConsumerSessionImpl consumer = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the subscription.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            //create the consumer session for this subscription
            try
            {
                consumer =
                                new ConsumerSessionImpl(destination,
                                                destinationAddress,
                                                subState,
                                                this,
                                                enableReadAhead,
                                                forwardScanning,
                                                unrecoverableReliability,
                                                bifurcatable,
                                                true,
                                                false);
            } catch (SINotPossibleInCurrentConfigurationException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", "SIErrorException");
                throw new SIErrorException(e);
            } catch (SISessionUnavailableException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalCreateConsumerSessionForDurableSubscription",
                                            "1:2417:1.347.1.25",
                                            this);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:2423:1.347.1.25",
                                          e });

                // This should never be thrown
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", "SIErrorException");
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:2434:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SITemporaryDestinationNotFoundException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalCreateConsumerSessionForDurableSubscription",
                                            "1:2446:1.347.1.25",
                                            this);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:2452:1.347.1.25",
                                          e });

                // This should never be thrown
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", "SIErrorException");
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:2463:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SINonDurableSubscriptionMismatchException e) {
                //this exception not possible for durable and should be never thrown. So absorb here and trace it.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", "SINonDurableSubscriptionMismatchException");

                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:2248:1.347.1.25",
                                                                      e },
                                                        null),
                                e);

            }

            // TODO Check the recoverableExpress bit here
            synchronized (_consumers)
            {
                //store a reference
                _consumers.add(consumer);
            }

            _messageProcessor.addConsumer(consumer);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalCreateConsumerSessionForDurableSubscription", consumer);

        return consumer;
    }

    @Override
    public ConsumerSession
                    createMQInterOpConsumerSessionForDurableSubscription(String subscriptionName,
                                                                         String durableSubscriptionHome,
                                                                         SIDestinationAddress destinationAddress,
                                                                         SelectionCriteria criteria,
                                                                         boolean supportsMultipleConsumers,
                                                                         boolean nolocal,
                                                                         Reliability reliability,
                                                                         boolean enableReadAhead,
                                                                         Reliability unrecoverableReliability,
                                                                         boolean bifurcatable,
                                                                         String alternateUser,
                                                                         boolean forwardScanning)
                                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                                    SIIncorrectCallException,
                                    SIDurableSubscriptionNotFoundException, SIDurableSubscriptionMismatchException,
                                    SIDestinationLockedException, SINotAuthorizedException
    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "createMQInterOpConsumerSessionForDurableSubscription",
                        new Object[] {
                                      this,
                                      subscriptionName,
                                      durableSubscriptionHome,
                                      destinationAddress,
                                      criteria,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal),
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      alternateUser,
                                      Boolean.valueOf(forwardScanning) });

        ConsumerSession session =
                        internalCreateConsumerSessionForDurableSubscription(subscriptionName,
                                                                            durableSubscriptionHome,
                                                                            destinationAddress,
                                                                            criteria,
                                                                            supportsMultipleConsumers,
                                                                            nolocal,
                                                                            reliability,
                                                                            enableReadAhead,
                                                                            unrecoverableReliability,
                                                                            bifurcatable,
                                                                            alternateUser,
                                                                            forwardScanning);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(
                       CoreSPIConnection.tc,
                       "createMQInterOpConsumerSessionForDurableSubscription",
                       session);

        return session;
    }

    /**
     * Checks made for durable subscription support.
     * 
     * returns the uuid of the durable sub home
     * 
     * Checks that the connection isn't closed
     * Checks that the subscription name isn't null
     * Checks that the destination address isn't null
     * Checks that the supports multiple consumers and noLocal aren't both set.
     */
    private SIBUuid8 checkDurableSubscriptionInformation(String subscriptionName,
                                                         String durableSubscriptionHome,
                                                         SIDestinationAddress destinationAddress,
                                                         boolean supportsMultipleConsumers,
                                                         boolean nolocal,
                                                         boolean delete,
                                                         boolean createForDurSub)

                    throws SIIncorrectCallException, SIConnectionUnavailableException
    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkDurableSubscriptionInformation",
                        new Object[] { subscriptionName,
                                      destinationAddress,
                                      Boolean.valueOf(supportsMultipleConsumers),
                                      Boolean.valueOf(nolocal) });

        // See if this connection has been closed
        checkNotClosed();

        if (subscriptionName == null)
        {
            String exText = "CREATE_DURABLE_SUB_CWSIR0042";

            if (createForDurSub)
                exText = "CREATE_DURABLE_SUB_CWSIR0032";
            else if (delete)
                exText = "DELETE_DURABLE_SUB_CWSIR0061";
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls_cwsir.getFormattedMessage(
                                                                          exText,
                                                                          null,
                                                                          null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "checkDurableSubscriptionInformation", e);
            }
            throw e;
        }

        if (destinationAddress == null && !delete)
        {
            String exText = "CREATE_DURABLE_SUB_CWSIR0041";
            if (createForDurSub)
                exText = "CREATE_DURABLE_SUB_CWSIR0031";

            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls_cwsir.getFormattedMessage(
                                                                          exText,
                                                                          null,
                                                                          null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "checkDurableSubscriptionInformation", e);
            }
            throw e;
        }

        // Convert the durable subscription home to a UUID and figure out whether
        // this is a local or remote create.
        SIBUuid8 durableHomeID = _messageProcessor.mapMeNameToUuid(durableSubscriptionHome);
        if (durableHomeID == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkDurableSubscriptionInformation", "SIMENotFoundException");
            // Lookup failed, throw an excepiton
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "REMOTE_ME_MAPPING_ERROR_CWSIP0156",
                                                    new Object[] { durableSubscriptionHome },
                                                    null));
        }

        // noLocal on a cloned subscription is not supported.
        if (nolocal && supportsMultipleConsumers)
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls.getFormattedMessage(
                                                                    "INVALID_PARAMETER_COMBINATION_ERROR_CWSIP0100",
                                                                    new Object[] { subscriptionName,
                                                                                  destinationAddress.getDestinationName(),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "checkDurableSubscriptionInformation", e);
            }
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDurableSubscriptionInformation", durableHomeID);

        return durableHomeID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#deleteDurableSubscription(java.lang.String, java.lang.String)
     */
    @Override
    public void deleteDurableSubscription(String subscriptionName,
                                          String durableSubscriptionHome)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException
    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "deleteDurableSubscription",
                        new Object[] { this, subscriptionName, durableSubscriptionHome });

        deleteDurableSubscription(subscriptionName, durableSubscriptionHome, null);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "deleteDurableSubscription");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#deleteDurableSubscription(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void deleteDurableSubscription(String subscriptionName,
                                          String durableSubscriptionHome,
                                          String alternateUser)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException
    {

        //liberty code change : chetan
        //Since there is no ME-ME communication the durableSubscriptionHome is always the local ME 
        durableSubscriptionHome = _messageProcessor.getMessagingEngineName();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "deleteDurableSubscription",
                        new Object[] { this, subscriptionName, durableSubscriptionHome, alternateUser });

        // Synchronize on the connection object, we don't want the connection closing
        // while we try to delete the subscription.
        synchronized (this)
        {
            SIBUuid8 durableHomeID =
                            checkDurableSubscriptionInformation(subscriptionName,
                                                                durableSubscriptionHome,
                                                                null,
                                                                false,
                                                                false,
                                                                true,
                                                                false);

            HashMap durableSubs = _destinationManager.getDurableSubscriptionsTable();

            // Is durableHome local?
            if (durableHomeID.equals(_messageProcessor.getMessagingEngineUuid()))
            {
                // Yes, then there should be an existing consumer dispatcher which we'll
                // invoke directly
                synchronized (durableSubs)
                {
                    //Look up the consumer dispatcher for this subId in the system durable subs list
                    ConsumerDispatcher cd =
                                    (ConsumerDispatcher) durableSubs.get(subscriptionName);

                    // Check that the durable subscription existed
                    if (cd == null)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                            SibTr.exit(CoreSPIConnection.tc, "deleteDurableSubscription");

                        throw new SIDurableSubscriptionNotFoundException(
                                        nls.getFormattedMessage(
                                                                "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
                                                                new Object[] { subscriptionName,
                                                                              _messageProcessor.getMessagingEngineName() },
                                                                null));
                    }

                    //Obtain the destination from the consumer dispatcher
                    DestinationHandler destination = cd.getDestination();

                    // Check that the destination is a TopicSpace
                    if (!destination.isPubSub())
                    {
                        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                            SibTr.exit(CoreSPIConnection.tc, "deleteDurableSubscription", "SIDestinationWrongTypeException");

                        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                    new Object[] {
                                                  "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                  "1:2759:1.347.1.25" });

                        throw new SIErrorException(
                                        nls.getFormattedMessage(
                                                                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                new Object[] { "ConnectionImpl",
                                                                              "1:2765:1.347.1.25" },
                                                                null));
                    }

                    // decalre variables used in security checks
                    AuthUtils sibAuthUtils = _messageProcessor.getAuthorisationUtils();
                    String topicName = null;
                    String topicSpaceName = null;
                    String theUser = null;

                    // If security is disabled then we'll bypass the check
                    if (_isBusSecure)
                    {
                        // get the consumer dispatcher state from the consumer dispatcher
                        ConsumerDispatcherState subState = cd.getConsumerDispatcherState();

                        topicName = subState.getTopic();
                        topicSpaceName = subState.getTopicSpaceName();

                        // Check that the user who is attempting to delete this durable subscription
                        // matches that set in the CD state when the subscription was created           
                        boolean isPriv = false;
                        if (alternateUser == null)
                        {
                            theUser = getResolvedUserid();
                            isPriv = isSIBServerSubject();
                        }
                        else
                        {
                            theUser = alternateUser;
                        }

                        boolean userMatch = subState.equalUser(theUser, isPriv);

                        // Throw not auth exception if the user who is trying to delete the durable
                        // subscription isn't the same user that created the subscription
                        if (!userMatch)
                        {
                            // ensure that a security audit event takes place for this authorization failure
                            sibAuthUtils.deleteDurSubAuthorizationFailed(theUser, topicName, topicSpaceName, 0L);

                            // trace the exception
                            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                                SibTr.exit(CoreSPIConnection.tc, "deleteDurableSubscription", "SINotAuthorizedException");

                            // throw the exception
                            throw new SINotAuthorizedException(
                                            nls.getFormattedMessage(
                                                                    "USER_NOT_AUTH_DELETE_ERROR_CWSIP0311",
                                                                    new Object[] { theUser, subscriptionName, destination.getName() },
                                                                    null));
                        }
                    }

                    //Call the deleteDurableSubscription method on the destination
                    destination.deleteDurableSubscription(subscriptionName, durableSubscriptionHome);

                    if (_isBusSecure)
                    {
                        // ensure that a security audit event takes place for this successful dur sub delete
                        sibAuthUtils.deleteDurSubAuthorizationPassed(theUser, topicName, topicSpaceName, 0L);
                    }
                }
            }
            else
            {
                // No, issue a remote request
                DurableInputHandler.deleteRemoteDurableSub(_messageProcessor,
                                                           subscriptionName,
                                                           getResolvedUserid(),
                                                           durableHomeID);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "deleteDurableSubscription");

    }

    private void checkSendNullParameters(SIDestinationAddress destAddr,
                                         SIBusMessage msg) throws SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkSendNullParameters", new Object[] { destAddr, msg });

        if (destAddr == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkSendNullParameters", "SIIncorrectCallException - null destAddr");
            SIMPIncorrectCallException e = new SIMPIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "SEND_CWSIR0111",
                                                          null,
                                                          null));

            e.setExceptionReason(SIRCConstants.SIRC0900_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ConnectionImpl.checkSendNullParameters",
                                                "1:2864:1.347.1.25" });
            throw e;
        }

        if (msg == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkSendNullParameters", "SIIncorrectCallException - null msg");
            SIMPIncorrectCallException e = new SIMPIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "SEND_CWSIR0112",
                                                          null,
                                                          null));
            e.setExceptionReason(SIRCConstants.SIRC0900_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ConnectionImpl.checkSendNullParameters",
                                                "1:2879:1.347.1.25" });
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkSendNullParameters");
    }

    private void internalSend(SIBusMessage msg,
                              SITransaction tran,
                              SIDestinationAddress destAddr,
                              DestinationType destinationType,
                              OrderingContext extendedMessageOrderingContext,
                              String alternateUser,
                              boolean system)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SITemporaryDestinationNotFoundException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "internalSend",
                        new Object[] { msg, tran, destAddr, destinationType, alternateUser, extendedMessageOrderingContext, Boolean.valueOf(system) });

        checkNotClosed();

        checkSendNullParameters(destAddr, msg);

        if (tran != null && !((TransactionCommon) tran).isAlive())
        {
            SIMPIncorrectCallException e = new SIMPIncorrectCallException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_16", // TRANSACTION_SEND_USAGE_ERROR_CWSIP0093
                                                          new Object[] { destAddr },
                                                          null));

            e.setExceptionReason(SIRCConstants.SIRC0016_TRANSACTION_SEND_USAGE_ERROR);
            e.setExceptionInserts(new String[] { destAddr.toString() });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalSend", e);

            throw e;
        }

        SecurityContext secContext = null;
        // If security is enabled then we need to set up a security context
        if (_isBusSecure)
        {
            // Feature 219101: Add the alternate user string to the security context
            secContext = new SecurityContext(_subject,
                            alternateUser,
                            null,
                            _messageProcessor.getAuthorisationUtils());
        }

        //TODO there may be some optimization we can do here to just send one message
        //Create a new producer session
        ProducerSession session =
                        internalCreateProducerSession(destAddr,
                                                      destinationType,
                                                      system,
                                                      secContext,
                                                      false, // keepSecurityUserid
                                                      false,
                                                      true,
                                                      true,
                                                      msg.getDiscriminator());
        try
        {
            //send one message
            session.send(msg, tran);
            //close the session again
            session.close();
        } catch (SISessionUnavailableException e)
        {
            // No FFDC code needed     
            SibTr.exception(tc, e);

            // If any of these calls failed due to being closed the connection
            // must have been closed. Call checkNotClosed() to force out the
            // correct exception.
            checkNotClosed();
            // Just in case the connection isn't closed we better re-throw the
            // original exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalSend", e);

            throw new SIConnectionUnavailableException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_22", // OBJECT_CLOSED_ERROR_CWSIP0091 
                                                          new Object[] { _messageProcessor.getMessagingEngineName() },
                                                          null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalSend");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#send(com.ibm.wsspi.sib.core.SIBusMessage, com.ibm.wsspi.sib.core.SITransaction, com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType, com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public void send(SIBusMessage msg,
                     SITransaction tran,
                     SIDestinationAddress destAddr,
                     DestinationType destinationType,
                     OrderingContext extendedMessageOrderingContext,
                     String alternateUser)

                    throws SIConnectionDroppedException, SIConnectionUnavailableException, SITemporaryDestinationNotFoundException, SIErrorException, SIIncorrectCallException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "send",
                        new Object[] {
                                      this,
                                      msg,
                                      tran,
                                      destAddr,
                                      destinationType,
                                      extendedMessageOrderingContext,
                                      alternateUser });

        internalSend(msg,
                     tran,
                     destAddr,
                     destinationType,
                     extendedMessageOrderingContext,
                     alternateUser,
                     false);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "send");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#receiveNoWait(com.ibm.wsspi.sib.core.SITransaction, com.ibm.websphere.sib.Reliability,
     * com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType, java.lang.String, java.lang.String, com.ibm.websphere.sib.Reliability,
     * com.ibm.wsspi.sib.core.OrderingContext)
     * Added M7 Core SPI
     */
    @Override
    public SIBusMessage receiveNoWait(SITransaction tran,
                                      Reliability unrecoverableReliability,
                                      SIDestinationAddress destAddr,
                                      DestinationType destinationType,
                                      SelectionCriteria criteria,
                                      Reliability reliability,
                                      String alternateUser)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "receiveNoWait",
                        new Object[] {
                                      this,
                                      tran,
                                      unrecoverableReliability,
                                      destAddr,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      alternateUser });

        SIBusMessage msg =
                        internalReceiveNoWait(tran,
                                              unrecoverableReliability,
                                              destAddr,
                                              destinationType,
                                              criteria,
                                              reliability,
                                              alternateUser,
                                              false);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "receiveNoWait", msg);
        return msg;
    }

    /**
     * Internal implementation for receiving no wait
     */
    private SIBusMessage internalReceiveNoWait(SITransaction tran,
                                               Reliability unrecoverableReliability,
                                               SIDestinationAddress destAddr,
                                               DestinationType destinationType,
                                               SelectionCriteria criteria,
                                               Reliability reliability,
                                               String alternateUser,
                                               boolean system)

                    throws SIConnectionDroppedException, SIConnectionUnavailableException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "internalReceiveNoWait",
                        new Object[] { destAddr,
                                      alternateUser,
                                      destinationType,
                                      criteria,
                                      tran,
                                      reliability,
                                      unrecoverableReliability,
                                      Boolean.valueOf(system) });

        if (destAddr == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalReceiveNoWait", "SIIncorrectCallException - null destAddr");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "RECEIVE_NO_WAIT_CWSIR0091",
                                                          null,
                                                          null));
        }

        SIBusMessage message = null;

        if (tran != null && !((TransactionCommon) tran).isAlive())
        {
            SIIncorrectCallException e = new SIIncorrectCallException(nls.getFormattedMessage(
                                                                                              "TRANSACTION_RECEIVE_USAGE_ERROR_CWSIP0777",
                                                                                              new Object[] { destAddr },
                                                                                              null));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalReceiveNoWait", e);

            throw e;
        }

        try
        {
            //TODO there may be some optimization we can do here to just receive one message
            //Create a consumer session 
            ConsumerSession session =
                            internalCreateConsumerSession(
                                                          destAddr,
                                                          alternateUser,
                                                          destinationType,
                                                          criteria,
                                                          reliability,
                                                          false, //enableReadAhead
                                                          false, //noLocal
                                                          false, //forwardScanning
                                                          system,
                                                          unrecoverableReliability,
                                                          false, // bifurcatable
                                                          false, // mqInterop
                                                          true, // ignoreIntialIndoubts
                                                          false // gatherMessages
                            );

            session.start(false);

            //receive one message
            message = session.receiveNoWait(tran);
            //close the session
            session.close();
        } catch (SISessionUnavailableException e)
        {
            // No FFDC code needed     
            SibTr.exception(tc, e);

            // If any of these calls failed due to being closed the connection
            // must have been closed. Call checkNotClosed() to force out the
            // correct exception.
            checkNotClosed();
            // Just in case the connection isn't closed we better re-throw the
            // original exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalReceiveNoWait", e);

            throw new SIMPConnectionUnavailableException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_22", // OBJECT_CLOSED_ERROR_CWSIP0091 
                                                          new Object[] { _messageProcessor.getMessagingEngineName() },
                                                          null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalReceiveNoWait", message);

        return message;
    }

    /**
     * Internal implementation for receiving with wait
     */
    private SIBusMessage internalReceiveWithWait(SITransaction tran,
                                                 Reliability unrecoverableReliability,
                                                 SIDestinationAddress destAddr,
                                                 DestinationType destinationType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 long timeout,
                                                 String alternateUser,
                                                 boolean system)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException

    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "internalReceiveWithWait",
                        new Object[] {
                                      tran,
                                      unrecoverableReliability,
                                      destAddr,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Long.valueOf(timeout),
                                      alternateUser,
                                      Boolean.valueOf(system) });

        if (tran != null && !((TransactionCommon) tran).isAlive())
        {
            SIIncorrectCallException e = new SIIncorrectCallException(nls.getFormattedMessage(
                                                                                              "TRANSACTION_RECEIVE_USAGE_ERROR_CWSIP0777",
                                                                                              new Object[] { destAddr },
                                                                                              null));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalReceiveWithWait", e);

            throw e;
        }

        SIBusMessage message = null;

        try
        {
            //TODO there may be some optimization we can do here to just receive one message
            //create a consumer session
            ConsumerSession session =
                            internalCreateConsumerSession(
                                                          destAddr,
                                                          alternateUser,
                                                          destinationType,
                                                          criteria,
                                                          reliability,
                                                          false, //enableReadAhead
                                                          false, //noLocal
                                                          false, //forwardScanning
                                                          system,
                                                          unrecoverableReliability,
                                                          false, // bifurcatable
                                                          false, // mqInterop
                                                          true, //ignoreInitialIndoubts
                                                          false // gatherMessages
                            );

            session.start(false);

            //receive one message                                                    
            message = session.receiveWithWait(tran, timeout);
            //close the session
            session.close();
        } catch (SISessionUnavailableException e)
        {
            // No FFDC code needed     
            SibTr.exception(tc, e);

            // If any of these calls failed due to being closed the connection
            // must have been closed. Call checkNotClosed() to force out the
            // correct exception.
            checkNotClosed();
            // Just in case the connection isn't closed we better re-throw the
            // original exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalReceiveWithWait", e);

            throw new SIMPConnectionUnavailableException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_22", // OBJECT_CLOSED_ERROR_CWSIP0091
                                                          new Object[] { _messageProcessor.getMessagingEngineName() },
                                                          null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalReceiveWithWait", message);

        return message;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#receiveWithWait(com.ibm.wsspi.sib.core.SITransaction, com.ibm.websphere.sib.Reliability,
     * com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType, java.lang.String, java.lang.String, com.ibm.websphere.sib.Reliability, long,
     * com.ibm.wsspi.sib.core.OrderingContext)
     * 
     * Method Added in M7 Core SPI
     */
    @Override
    public SIBusMessage receiveWithWait(SITransaction tran,
                                        Reliability unrecoverableReliability,
                                        SIDestinationAddress destAddr,
                                        DestinationType destinationType,
                                        SelectionCriteria criteria,
                                        Reliability reliability,
                                        long timeout,
                                        String alternateUser)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "receiveWithWait",
                        new Object[] {
                                      this,
                                      Long.valueOf(timeout),
                                      destAddr,
                                      alternateUser,
                                      destinationType,
                                      criteria,
                                      tran,
                                      reliability,
                                      unrecoverableReliability });

        if (destAddr == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConnection.tc, "receiveWithWait", "SIIncorrectCallException - null destAddr");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "RECEIVE_WITH_WAIT_CWSIR0101",
                                                          null,
                                                          null));
        }

        SIBusMessage message = internalReceiveWithWait(tran, unrecoverableReliability, destAddr, destinationType, criteria, reliability, timeout, alternateUser, false);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "receiveWithWait", message);

        return message;
    }

    private void checkBrowserSessionNullParameters(SIDestinationAddress destinationAddress) throws SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkBrowserSessionNullParameters", destinationAddress);

        if (destinationAddress == null)
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls_cwsir.getFormattedMessage(
                                                                          "BROWSE_METHOD_CWSIR0001",
                                                                          null,
                                                                          null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkBrowserSessionNullParameters", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkBrowserSessionNullParameters");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createBrowserSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType, java.lang.String,
     * boolean)
     * 
     * Method Added M7 Core SPI
     */
    @Override
    public BrowserSession createBrowserSession(SIDestinationAddress destinationAddress,
                                               DestinationType destinationType,
                                               SelectionCriteria criteria,
                                               String alternateUser)

                    throws SIConnectionDroppedException, SIConnectionUnavailableException, SITemporaryDestinationNotFoundException, SIErrorException, SIIncorrectCallException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createBrowserSession",
                        new Object[] { this, destinationAddress, destinationType, criteria });

        // See if this connection has been closed
        checkNotClosed();
        // Check for null parameters    
        checkBrowserSessionNullParameters(destinationAddress);

        BrowserSession session = createBrowserSession(destinationAddress,
                                                      destinationType,
                                                      criteria,
                                                      false,
                                                      alternateUser,
                                                      false);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createBrowserSession", session);
        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createBrowserSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType, java.lang.String,
     * boolean)
     */
    @Override
    public BrowserSession createBrowserSession(SIDestinationAddress destinationAddress,
                                               DestinationType destinationType,
                                               SelectionCriteria criteria,
                                               String alternateUser,
                                               boolean gatherMessages)

                    throws SIConnectionDroppedException, SIConnectionUnavailableException, SITemporaryDestinationNotFoundException, SIErrorException, SIIncorrectCallException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createBrowserSession",
                        new Object[] { this, destinationAddress, destinationType, criteria, alternateUser, Boolean.valueOf(gatherMessages) });

        // See if this connection has been closed
        checkNotClosed();
        // Check for null parameters    
        checkBrowserSessionNullParameters(destinationAddress);

        BrowserSession session = createBrowserSession(destinationAddress,
                                                      destinationType,
                                                      criteria,
                                                      false,
                                                      alternateUser,
                                                      gatherMessages);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createBrowserSession", session);
        return session;
    }

    /**
     * Creates the Browser session.
     * 
     * @param destName
     * @param destinationAddress
     * @param discriminator
     * @param selector
     * @param system
     * @return
     * @throws SIDiscriminatorSyntaxException
     * @throws SISelectorSyntaxException
     */
    private BrowserSession createBrowserSession(
                                                SIDestinationAddress destinationAddress,
                                                DestinationType destinationType,
                                                SelectionCriteria criteria,
                                                boolean system,
                                                String alternateUser,
                                                boolean gatherMessages)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException, SISelectorSyntaxException, SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createBrowserSession",
                        new Object[] { destinationAddress, destinationType, criteria, Boolean.valueOf(gatherMessages) });

        // Finding a destination could take some time so we don't have the
        // connection locked (on closed) when we do this.
        DestinationHandler destination = _destinationManager.getDestination(
                                                                            (JsDestinationAddress) destinationAddress, false);

        // Check that this is the correct destination type
        checkDestinationType(destinationType, destinationAddress, destination, system);

        if (destination.getDestinationType() == DestinationType.SERVICE)
        {
            //Cant browse a service destination
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createBrowserSession", "cannot browse a service destination");

            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "INVALID_DESTINATION_USAGE_ERROR_CWSIP0022",
                                                    new Object[] { destination.getName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        // Check that this temporary destination was created by this connection.
        checkTemporary(destination, false);

        // Check authority to browse this destination
        // If security is disabled then we'll bypass the check
        checkDestinationAuthority(destination, MessagingSecurityConstants.OPERATION_TYPE_BROWSE, null);

        //if the destination turns out to be pub-sub then that doesn't make sense
        //so we'll just set it back to null
        if (destination.isPubSub())
        {
            destination = null;
        }

        BrowserSession browser = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the browser.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            //create a browser session with the given destination
            //if the destination was null - it was pub sub - this will create a browser
            //session which never returns any messages
            browser = new BrowserSessionImpl(destination,
                            criteria,
                            this,
                            destinationAddress,
                            gatherMessages);
            synchronized (_browsers)
            {
                //store a reference
                _browsers.add(browser);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createBrowserSession", browser);

        return browser;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#addConnectionListener(com.ibm.wsspi.sib.core.SICoreConnectionListener)
     */
    @Override
    public void addConnectionListener(SICoreConnectionListener listener)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "addConnectionListener",
                        new Object[] { this, listener });

        checkNotClosed();

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the listener.
        synchronized (_connectionListeners)
        {
            //store a reference to the listener
            _connectionListeners.add(listener);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "addConnectionListener");
    }

    /**
     * Gets the Message processor to which this is a connection to
     * 
     * @return MessageProcessor
     */
    MessageProcessor getMessageProcessor()
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getMessageProcessor");
            SibTr.exit(tc, "getMessageProcessor", _messageProcessor);
        }

        return _messageProcessor;
    }

    /**
     * Returns the unique id of this connection.
     * 
     * @return SIBUuid12 of the connection
     */
    SIBUuid12 getUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getUuid");
            SibTr.exit(tc, "getUuid", _uuid);
        }

        return _uuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createTemporaryDestination(com.ibm.ws.sib.common.Distribution, java.lang.String)
     */
    @Override
    public SIDestinationAddress createTemporaryDestination(Distribution distribution,
                                                           String destinationPrefix)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIInvalidDestinationPrefixException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createTemporaryDestination",
                        new Object[] { this, destinationPrefix, distribution });

        // See if this connection has been closed
        checkNotClosed();

        // Check to make sure that the destination prefix is valid. 
        // If it's not, then we can't continue.
        String result = SICoreUtils.isDestinationPrefixValid(destinationPrefix);
        if (!result.equals(SICoreUtils.VALID)) // if its not valid there is something wrong
        {
            if (result.equals(SICoreUtils.MAX_LENGTH_EXCEEDED)) {// prefix length might have been exceeded
                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                    SibTr.exit(CoreSPIConnection.tc, "createTemporaryDestination", "SIInvalidDestinationPrefixException");
                throw new SIInvalidDestinationPrefixException(
                                nls.getFormattedMessage(
                                                        "INVALID_DESTINATION_PREFIX_MAX_LENGTH_ERROR_CWSIP0039",
                                                        null,
                                                        null));
            } else { // an invalid character is there in the prefix 

                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                    SibTr.exit(CoreSPIConnection.tc, "createTemporaryDestination", "SIInvalidDestinationPrefixException");
                throw new SIInvalidDestinationPrefixException(
                                nls.getFormattedMessage(
                                                        "INVALID_DESTINATION_PREFIX_CHAR_ERROR_CWSIP0040",
                                                        new Object[] { destinationPrefix, result },
                                                        null));
            }
        }

        SIDestinationAddress address = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the temporary destination.
        synchronized (this)
        {
            // Check authority to create destination
            // If security is disabled then we'll bypass the check
            checkTempDestinationCreation((destinationPrefix == null ? "" : destinationPrefix), distribution);

            // Synchronize on the temporaryDestinations list object so that we can't collide
            // on creations. This will also ensure list integrity.
            synchronized (_temporaryDestinations)
            {
                //pass this on to the destination manager
                try
                {
                    address =
                                    _destinationManager.createTemporaryDestination(distribution, destinationPrefix);
                } catch (SIMPDestinationAlreadyExistsException e)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.ConnectionImpl.createTemporaryDestination",
                                                "1:3712:1.347.1.25",
                                                this);

                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                              "1:3718:1.347.1.25",
                                              e });

                    // This should never be thrown
                    if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                        SibTr.exit(CoreSPIConnection.tc, "createTemporaryDestination", "SIErrorException");
                    throw new SIErrorException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                          "1:3729:1.347.1.25",
                                                                          e },
                                                            null),
                                    e);
                }

                //add to list
                _temporaryDestinations.add(address.getDestinationName());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createTemporaryDestination", address);

        return address;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#deleteTemporaryDestination(com.ibm.websphere.sib.SIDestinationAddress)
     */
    @Override
    public void deleteTemporaryDestination(SIDestinationAddress destAddr)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException,
                    SIDestinationLockedException, SITemporaryDestinationNotFoundException, SIResourceException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "deleteTemporaryDestination",
                        new Object[] { this, destAddr });

        // Synchronize on the close object, we don't want the connection closing
        // while we try to delete the destination.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            if (destAddr == null)
            {
                SIIncorrectCallException e =
                                new SIIncorrectCallException(
                                                nls_cwsir.getFormattedMessage(
                                                                              "CREATE_PRODUCER_CWSIR0071",
                                                                              null,
                                                                              null));
                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "deleteTemporaryDestination", e);

                throw e;
            }

            // Synchronize on the temporaryDestinations list object so that we can't collide
            // on creations. This will also ensure list integrity.
            synchronized (_temporaryDestinations)
            {
                //verify this temporary destination is in current list
                int index = _temporaryDestinations.indexOf(destAddr.getDestinationName());
                if (index == -1)
                {
                    if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                        SibTr.exit(tc, "deleteTemporaryDestination", "SITemporaryDestinationNotFoundException");
                    throw new SIMPTemporaryDestinationNotFoundException(
                                    nls.getFormattedMessage(
                                                            "TEMPORARY_DESTINATION_NAME_ERROR_CWSIP0097",
                                                            new Object[] { destAddr },
                                                            null));
                }

                //pass to the destination manager
                _destinationManager.deleteTemporaryDestination((JsDestinationAddress) destAddr);

                //remove entry from list
                _temporaryDestinations.remove(index);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "deleteTemporaryDestination");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getSIXAResource()
     */
    @Override
    public SIXAResource getSIXAResource()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getSIXAResource", this);
        // See if this connection has been closed
        checkNotClosed();
        checkMPStarted();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getSIXAResource");
        //sanjay liberty change transaction
        return _txManager.createXAResource(true);
    }

    /**
     * @return an XA resource implementation for the Message
     *         Store.
     */
    @Override
    public SIXAResource getMSSIXAResource()
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getMSSIXAResource", this);
        // See if this connection has been closed
        checkNotClosed();
        checkMPStarted();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getMSSIXAResource");
        return _txManager.createXAResource(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#cloneConnection()
     */
    @Override
    public SICoreConnection cloneConnection()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "cloneConnection", this);

        SICoreConnection clone = null;

        // take a lock on  the MP _connections lock manager (required to maintain lock consistency)

        _messageProcessor.getConnectionLockManager().lock();

        try
        {
            // See if this connection has been closed
            checkNotClosed();

            //Create a new connection instance
            // The definition of a cloned connection is that it is connected to the
            // same ME. This is always true in the MP layer.
            clone = _messageProcessor.createConnection(_subject, false, _connectionProperties);
        } finally
        {
            _messageProcessor.getConnectionLockManager().unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "cloneConnection", clone);
        return clone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#isEquivalentTo(com.ibm.wsspi.sib.core.SICoreConnection)
     */
    @Override
    public boolean isEquivalentTo(SICoreConnection rhs)
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "isEquivalentTo", new Object[] { this, rhs });

        //if any one of these checks fails, the connections are not equivalent
        boolean result = false;

        //there are three checks for an equivalent connection...
        //it has to be of the same class
        if (rhs instanceof ConnectionImpl)
        {
            final ConnectionImpl conImpl = (ConnectionImpl) rhs;
            //it has to be a connection to the same message processor
            if (conImpl.getMessageProcessor() == getMessageProcessor())
            {
                // now check that the connection's credentials are the same too
                // Calling .equals() on a subject object requires a doPrivileged()
                Boolean subjectResult = (Boolean) AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                            SibTr.entry(tc, "run", new Object[] { _subject, conImpl.getSecuritySubject() });

                        boolean result = ((_subject == null) ? (conImpl.getSecuritySubject() == null) :
                                        (_subject.equals(conImpl.getSecuritySubject())));

                        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                            SibTr.exit(tc, "run", Boolean.valueOf(result));

                        return new Boolean(result);
                    }
                });
                result = subjectResult.booleanValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "isEquivalentTo", Boolean.valueOf(result));

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getMeName()
     */
    @Override
    public String getMeName()
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getMeName", this);
        //get the name of the Message Processor
        String name = _messageProcessor.getMessagingEngineName();
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getMeName", name);
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getMeUuid()
     */
    @Override
    public String getMeUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getMeUuid", this);

        // get the meuuid from the messaging engine instead of messageprocessor
        // When ME is stopping connection events are sent to MDB and MDB tries to connect 
        // to ME. Earlier messageprocessor.getmessagingengineuuid() was being called but
        // leads to NPE because messageprocessor.getmessagingengineuuid() gets the uuid from persistent
        // store is stopped(null) by now
        SIBUuid8 meUUID = new SIBUuid8(_messageProcessor.getMessagingEngine().getUuid());

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getMeUuid", meUUID);

        return meUUID.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#removeConnectionListener(com.ibm.wsspi.sib.core.SICoreConnectionListener)
     */
    @Override
    public void removeConnectionListener(SICoreConnectionListener listener)
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "removeConnectionListener",
                        new Object[] { this, listener });

        // Synchronize on the _connectionListeners object, 
        synchronized (_connectionListeners)
        {
            _connectionListeners.remove(listener);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "removeConnectionListener");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getConnectionListeners()
     * 
     * The synchronization of this method is purely around the
     * connectionListeners object. This is because of a timing problem
     * between an asynch consumer processing an error and the server shutting
     * down. As this doesn't have any close checking, not synchronizing on the
     * *this* object won't matter.
     */
    @Override
    public SICoreConnectionListener[] getConnectionListeners()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getConnectionListeners", this);

        checkNotClosed();

        SICoreConnectionListener[] array = null;
        synchronized (_connectionListeners)
        {
            //convert our linked list to an array
            //create a new array of the correct runtime type
            SICoreConnectionListener[] resultArray =
                            new SICoreConnectionListener[_connectionListeners.size()];
            //populate a new array of the right type
            array =
                            _connectionListeners.toArray(
                                            resultArray);
        }
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getConnectionListeners", array);

        return array;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getApiLevelDescription()
     */
    @Override
    public String getApiLevelDescription()
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getApiLevelDescription", this);

        //Get the API Level Description from the MP
        String description = _messageProcessor.getApiLevelDescription();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getApiLevelDescription", description);

        return description;
    }

    @Override
    public long getApiMajorVersion()
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getApiMajorVersion", this);

        //Get the API Levelfrom the MP
        long level = _messageProcessor.getApiMajorVersion();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getApiMajorVersion", Long.valueOf(level));

        return level;
    }

    @Override
    public long getApiMinorVersion()
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getApiMinorVersion", this);

        //Get the API Levelfrom the MP
        long level = _messageProcessor.getApiMinorVersion();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getApiMinorVersion", Long.valueOf(level));

        return level;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void beforeCompletion(TransactionCommon transaction)
    {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.Transaction, boolean)
     */
    @Override
    public void afterCompletion(TransactionCommon transaction, boolean committed)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "afterCompletion", new Object[] { transaction, Boolean.valueOf(committed) });

        synchronized (this)
        {
            // If the connection is already closed this transaction
            // will have been removed.
            if (!_closed)
            {
                synchronized (_ownedTransactions)
                {
                    // Remove the transaction from the connections list
                    _ownedTransactions.remove(transaction);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "afterCompletion");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createUniqueId()
     */
    @Override
    public byte[] createUniqueId()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createUniqueId", this);

        //See if this connection has been closed
        checkNotClosed();

        SIBUuid12 id = new SIBUuid12();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createUniqueId", id);
        //SIBUuid extends UUID which is 128bit == 16bytes
        return id.toByteArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getDestinationConfiguration(com.ibm.websphere.sib.SIDestinationAddress)
     */
    @Override
    public DestinationConfiguration getDestinationConfiguration(SIDestinationAddress destAddr)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getDestinationConfiguration",
                        new Object[] { this, destAddr });

        // See if this connection has been closed
        checkNotClosed();

        if (destAddr == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConnection.tc, "getDestinationConfiguration", "SIIncorrectCallException - null destAddr");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "GET_DEST_CONFIG_CWSIR0081",
                                                          null,
                                                          null));
        }

        DestinationConfiguration dc = null;

        DestinationHandler dh = _destinationManager.getDestination((JsDestinationAddress) destAddr, false);

        // If the destination is temporary then create a destionationConfiguration with
        //  what we have and know.
        if (destAddr.isTemporary())
        {
            DestinationType destinationType = DestinationType.QUEUE;
            if (destAddr.getDestinationName().startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX))
            {
                destinationType = DestinationType.TOPICSPACE;
            }

            // Handle the case where the destination is foreign temporary
            boolean foreignTempDestination = false;
            String busName = destAddr.getBusName();
            if ((busName != null) &&
                !(busName.equals(_messageProcessor.getMessagingEngineBus())))
            {
                foreignTempDestination = true;
            }

            // Check authority to inquire on the destination
            // If security is disabled then we'll bypass the check
            if (_isBusSecure)
            {
                SecurityContext secContext = new SecurityContext(this._subject,
                                null, // alternateUser 
                                null,
                                _messageProcessor.getAuthorisationUtils());

                // Check authority to inquire on the temp destination
                if (!foreignTempDestination)
                {
                    checkInquireAuthority(dh,
                                          destAddr.getDestinationName(),
                                          null, // home bus,
                                          secContext,
                                          true);
                }
                else
                {
                    // Need foreign bus style processing
                    checkInquireAuthority(dh,
                                          null, // null for this style of access check
                                          busName, // home bus,
                                          secContext,
                                          false);
                }
            }

            dc =
                            new DestinationConfigurationImpl(0,
                                            Reliability.EXPRESS_NONPERSISTENT,
                                            null,
                                            null,
                                            destinationType,
                                            null,
                                            0,
                                            Reliability.EXPRESS_NONPERSISTENT,
                                            destAddr.getDestinationName(),
                                            null,
                                            false,
                                            true,
                                            true,
                                            true,
                                            null,
                                            null,
                                            false);
        }
        else if (dh.isForeignBus())
        {
            // Check authority to inquire on the destination
            // If security is disabled then we'll bypass the check
            if (_isBusSecure)
            {
                SecurityContext secContext = new SecurityContext(this._subject,
                                null, // alternateUser 
                                null,
                                _messageProcessor.getAuthorisationUtils());

                String busName = dh.getName();

                // Check authority to inquire on the foreign bus
                checkInquireAuthority(dh,
                                      null, // null for this style of access check
                                      busName,
                                      secContext,
                                      false);

            }

            // If this is a foreign bus - then there will be no definition associated with the 
            // destination handler, so construct one based on the BusHandler.
            dc =
                            new DestinationConfigurationImpl(dh.getDefaultPriority(),
                                            dh.getDefaultReliability(),
                                            dh.getDescription(),
                                            ((BusHandler) dh).getDestinationContext(),
                                            dh.getDestinationType(),
                                            dh.getExceptionDestination(),
                                            dh.getMaxFailedDeliveries(),
                                            dh.getMaxReliability(),
                                            dh.getName(),
                                            dh.getUuid().toString(),
                                            dh.isOverrideOfQOSByProducerAllowed(),
                                            dh.isReceiveAllowed(),
                                            dh.isReceiveExclusive(),
                                            dh.isSendAllowed(),
                                            dh.getDefaultForwardRoutingPath(),
                                            dh.getReplyDestination(),
                                            dh.isOrdered());

        }
        else
        {
            // For non-local destinations, will throw SIDestinationNotFoundException
            // In later milestone, Admin will provide i/f for us to query both
            // local and remove destination configurations  
            BaseDestinationDefinition baseDefinition = dh.getDefinition();
            if (dh.isAlias())
            {
                //see defect 296430
                //we resolve alias destinations to their real destinations
                baseDefinition = ((AliasDestinationHandler) dh).getResolvedDestinationHandler().getDefinition();
            }
            DestinationDefinition definition = (DestinationDefinition) baseDefinition;

            // Check authority to inquire on the destination
            // If security is disabled then we'll bypass the check
            if (_isBusSecure)
            {
                SecurityContext secContext = new SecurityContext(this._subject,
                                null, // alternateUser 
                                null,
                                _messageProcessor.getAuthorisationUtils());

                String destinationName = destAddr.getDestinationName();

                // Check authority to inquire on this destination
                checkInquireAuthority(dh,
                                      destinationName,
                                      null, // home bus
                                      secContext,
                                      false);
            }

            dc =
                            new DestinationConfigurationImpl(definition.getDefaultPriority(),
                                            definition.getDefaultReliability(),
                                            definition.getDescription(),
                                            definition.getDestinationContext(),
                                            definition.getDestinationType(),
                                            definition.getExceptionDestination(),
                                            definition.getMaxFailedDeliveries(),
                                            definition.getMaxReliability(),
                                            definition.getName(),
                                            definition.getUUID().toString(),
                                            definition.isOverrideOfQOSByProducerAllowed(),
                                            definition.isReceiveAllowed(),
                                            definition.isReceiveExclusive(),
                                            definition.isSendAllowed(),
                                            dh.getDefaultForwardRoutingPath(),
                                            dh.getReplyDestination(),
                                            dh.isOrdered());
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getDestinationConfiguration", dc);
        return dc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#createSystemBrowserSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationFilter,
     * java.lang.String, java.lang.String)
     */
    @Override
    public BrowserSession createSystemBrowserSession(SIDestinationAddress destAddress,
                                                     DestinationType destinationType,
                                                     SelectionCriteria criteria)

                    throws SIConnectionDroppedException, SIConnectionUnavailableException, SIErrorException, SIIncorrectCallException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSystemBrowserSession",
                        new Object[] { destAddress, destinationType, criteria });

        // See if this connection has been closed
        checkNotClosed();

        checkBrowserSessionNullParameters(destAddress);

        if (!destAddress.getDestinationName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemBrowserSession", "SIIncorrectCallException");
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "SYSTEM_DESTINATION_USAGE_ERROR_CWSIP0024",
                                                    new Object[] { destAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        BrowserSession session = null;
        try
        {
            session =
                            createBrowserSession(destAddress,
                                                 destinationType,
                                                 criteria,
                                                 true,
                                                 null, // alternateUser 
                                                 false);
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // Temporary destination not found should not happen for a System destination
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConnectionImpl.createSystemBrowserSession",
                                        "1:4363:1.347.1.25",
                                        this);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                      "1:4369:1.347.1.25",
                                      e });

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemBrowserSession", "SIErrorException");

            throw new SIErrorException(nls.getFormattedMessage(
                                                               "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                               new Object[] { "ConnectionImpl",
                                                                             "1:4380:1.347.1.25",
                                                                             e },
                                                               null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSystemBrowserSession", session);

        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#createSystemConsumerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationFilter,
     * java.lang.String, java.lang.String, com.ibm.websphere.sib.Reliability, boolean, boolean, com.ibm.websphere.sib.Reliability)
     */
    @Override
    public ConsumerSession createSystemConsumerSession(SIDestinationAddress destAddress,
                                                       DestinationType destinationType,
                                                       SelectionCriteria criteria,
                                                       Reliability reliability,
                                                       boolean enableReadAhead,
                                                       boolean nolocal,
                                                       Reliability unrecoverableReliability,
                                                       boolean bifurcatable)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSystemConsumerSession",
                        new Object[] { destAddress,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(nolocal),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable) });

        // See if this connection has been closed
        checkNotClosed();

        checkConsumerSessionNullParameters(destAddress);

        if (!destAddress.getDestinationName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemConsumerSession", "SIIncorrectCallException");
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "SYSTEM_DESTINATION_USAGE_ERROR_CWSIP0024",
                                                    new Object[] { destAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        ConsumerSession session = null;
        try
        {
            session =
                            internalCreateConsumerSession(
                                                          destAddress,
                                                          null, // alternateUser
                                                          destinationType,
                                                          criteria,
                                                          reliability,
                                                          enableReadAhead,
                                                          nolocal,
                                                          false,
                                                          true, //system
                                                          unrecoverableReliability,
                                                          bifurcatable,
                                                          false,
                                                          true,
                                                          false // gatherMessages
                            );
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // Temporary destination not found should not happen for a System destination
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConnectionImpl.createSystemConsumerSession",
                                        "1:4468:1.347.1.25",
                                        this);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                      "1:4474:1.347.1.25",
                                      e });

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemConsumerSession", "SIErrorException");

            throw new SIErrorException(nls.getFormattedMessage(
                                                               "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                               new Object[] { "ConnectionImpl",
                                                                             "1:4485:1.347.1.25",
                                                                             e },
                                                               null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSystemConsumerSession", session);

        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#createSystemProducerSession(com.ibm.websphere.sib.SIDestinationAddress, java.lang.String,
     * com.ibm.wsspi.sib.core.DestinationFilter, com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public ProducerSession createSystemProducerSession(SIDestinationAddress destAddress,
                                                       String discriminator,
                                                       DestinationType destFilter,
                                                       OrderingContext context,
                                                       String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException
    {
        return createSystemProducerSession(destAddress,
                                           discriminator,
                                           destFilter,
                                           context,
                                           alternateUser,
                                           true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#createSystemProducerSession(com.ibm.websphere.sib.SIDestinationAddress, java.lang.String,
     * com.ibm.wsspi.sib.core.DestinationFilter, com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public ProducerSession createSystemProducerSession(SIDestinationAddress destAddress,
                                                       String discriminator,
                                                       DestinationType destFilter,
                                                       OrderingContext context,
                                                       String alternateUser,
                                                       boolean clearPubSubFingerprints)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSystemProducerSession",
                        new Object[] { destAddress, discriminator, destFilter, context, alternateUser, clearPubSubFingerprints });

        // See if this connection has been closed
        checkNotClosed();

        checkProducerSessionNullParameters(destAddress);

        if (!destAddress.getDestinationName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemProducerSession", "SIIncorrectCallException");
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "SYSTEM_DESTINATION_USAGE_ERROR_CWSIP0024",
                                                    new Object[] { destAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        SecurityContext secContext = null;
        // If security is enabled then we need to set up a security context
        if (_isBusSecure)
        {
            // Feature 219101: Add the alternate user string to the security context
            secContext = new SecurityContext(this._subject,
                            alternateUser,
                            null,
                            _messageProcessor.getAuthorisationUtils());
        }

        //create a producer session
        ProducerSession session = null;
        try
        {
            session =
                            internalCreateProducerSession(
                                                          destAddress,
                                                          destFilter,
                                                          true,
                                                          secContext,
                                                          false, // keepSecurityUserid
                                                          false, // fixedQueuePoint
                                                          true, // preferLocal
                                                          clearPubSubFingerprints,
                                                          null);
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // Temporary destination not found should not happen for a System destination
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConnectionImpl.createSystemProducerSession",
                                        "1:4590:1.347.1.25",
                                        this);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                      "1:4596:1.347.1.25",
                                      e });

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSystemProducerSession", "SIErrorException");

            throw new SIErrorException(nls.getFormattedMessage(
                                                               "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                               new Object[] { "ConnectionImpl",
                                                                             "1:4607:1.347.1.25",
                                                                             e },
                                                               null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSystemProducerSession", session);
        return session;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#systemReceiveNoWait(com.ibm.wsspi.sib.core.SITransaction, com.ibm.websphere.sib.Reliability,
     * com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationFilter, java.lang.String, java.lang.String, com.ibm.websphere.sib.Reliability)
     */
    @Override
    public SIBusMessage systemReceiveNoWait(SITransaction tran,
                                            Reliability unrecoverableReliability,
                                            SIDestinationAddress destAddress,
                                            DestinationType destinationType,
                                            SelectionCriteria criteria,
                                            Reliability reliability)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "systemReceiveNoWait",
                        new Object[] { tran,
                                      unrecoverableReliability,
                                      destAddress,
                                      destinationType,
                                      criteria,
                                      reliability });

        if (destAddress == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemReceiveNoWait", "SIIncorrectCallException - null destAddr");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "RECEIVE_NO_WAIT_CWSIR0091",
                                                          null,
                                                          null));
        }

        if (!destAddress.getDestinationName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemReceiveNoWait", "SIIncorrectCallException");
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "SYSTEM_DESTINATION_USAGE_ERROR_CWSIP0024",
                                                    new Object[] { destAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        SIBusMessage msg = null;
        try
        {
            msg =
                            internalReceiveNoWait(tran,
                                                  unrecoverableReliability,
                                                  destAddress,
                                                  destinationType,
                                                  criteria,
                                                  reliability,
                                                  null,
                                                  true);
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConnectionImpl.systemReceiveNoWait",
                                        "1:4687:1.347.1.25",
                                        this);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                      "1:4693:1.347.1.25" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemReceiveNoWait", "SIErrorException");
            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                    new Object[] { "ConnectionImpl",
                                                                  "1:4701:1.347.1.25" },
                                                    null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "systemReceiveNoWait", msg);

        return msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#systemReceiveWithWait(com.ibm.wsspi.sib.core.SITransaction, com.ibm.websphere.sib.Reliability,
     * com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationFilter, java.lang.String, java.lang.String, com.ibm.websphere.sib.Reliability, long)
     */
    @Override
    public SIBusMessage systemReceiveWithWait(SITransaction tran,
                                              Reliability unrecoverableReliability,
                                              SIDestinationAddress destAddress,
                                              DestinationType destinationType,
                                              SelectionCriteria criteria,
                                              Reliability reliability,
                                              long timeout)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "systemReceiveWithWait",
                        new Object[] { tran,
                                      unrecoverableReliability,
                                      destAddress,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Long.valueOf(timeout) });

        if (destAddress == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemReceiveWithWait", "SIIncorrectCallException - null destAddr");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "RECEIVE_NO_WAIT_CWSIR0091",
                                                          null,
                                                          null));
        }

        if (!destAddress.getDestinationName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemReceiveWithWait", "SIDestinationWrongTypeException");
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "SYSTEM_DESTINATION_USAGE_ERROR_CWSIP0024",
                                                    new Object[] { destAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        SIBusMessage msg = null;
        try
        {
            msg =
                            internalReceiveWithWait(tran,
                                                    unrecoverableReliability,
                                                    destAddress,
                                                    destinationType,
                                                    criteria,
                                                    reliability,
                                                    timeout,
                                                    null,
                                                    true);
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConnectionImpl.systemReceiveWithWait",
                                        "1:4783:1.347.1.25",
                                        this);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                      "1:4789:1.347.1.25" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemReceiveWithWait", "SIErrorException");
            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                    new Object[] { "ConnectionImpl",
                                                                  "1:4797:1.347.1.25" },
                                                    null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "systemReceiveWithWait", msg);

        return msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#systemSend(com.ibm.wsspi.sib.core.SIBusMessage, com.ibm.wsspi.sib.core.SITransaction,
     * com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationFilter, com.ibm.wsspi.sib.core.OrderingContext, boolean)
     */
    @Override
    public void systemSend(SIBusMessage msg,
                           SITransaction tran,
                           SIDestinationAddress destAddress,
                           DestinationType destinationType,
                           OrderingContext context,
                           boolean keepSecurityContext)

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "systemSend",
                        new Object[] { msg, tran, destAddress, destinationType, context, Boolean.valueOf(keepSecurityContext) });

        if (!destAddress.getDestinationName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemSend", "SIDestinationWrongTypeException");
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "SYSTEM_DESTINATION_USAGE_ERROR_CWSIP0024",
                                                    new Object[] { destAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        try
        {
            internalSend(msg, tran, destAddress, destinationType, context, null, true);
        } catch (SITemporaryDestinationNotFoundException e)
        {
            // Temporary destination not found should not happen for a System destination
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConnectionImpl.systemSend",
                                        "1:4851:1.347.1.25",
                                        this);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                      "1:4857:1.347.1.25",
                                      e });

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "systemSend", "SIErrorException");

            throw new SIErrorException(nls.getFormattedMessage(
                                                               "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                               new Object[] { "ConnectionImpl",
                                                                             "1:4868:1.347.1.25",
                                                                             e },
                                                               null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "systemSend");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#createSystemDestination(java.lang.String)
     */
    @Override
    public JsDestinationAddress createSystemDestination(String prefix)
                    throws SIResourceException, SIMPDestinationAlreadyExistsException, SIIncorrectCallException
    {
        //prefixes should be unique, 24 chars or less and
        //can contain the characters a-z, A-Z, 0-9, ., /, and %.
        boolean valid = isDestinationPrefixValid(prefix);
        if (!valid)
        {
            throw new SIInvalidDestinationPrefixException(
                            nls.getFormattedMessage(
                                                    "INVALID_DESTINATION_PREFIX_ERROR_CWSIP0023",
                                                    new Object[] { prefix },
                                                    null));
        }

        return _destinationManager.createSystemDestination(prefix);
    }

    /**
     * Determines whether a destination prefix for a System destination is valid or not.
     * 
     * <p>If the destination prefix has more than 24
     * characters, then it is invalid.
     * <p>The destination prefix is invalid if it contains any characters not in the following
     * list:
     * <ul>
     * <li>a-z (lower-case alphas)</li>
     * <li>A-Z (upper-case alphas)</li>
     * <li>0-9 (numerics)</li>
     * <li>. (period)</li>
     * <li>/ (slash)</li>
     * <li>% (percent)</li>
     * </ul>
     * <p>null and empty string values for a destination prefix are valid, and
     * simply indicate an empty prefix.
     * 
     * @param destinationPrefix The destination prefix to which the validity
     *            check is applied.
     * @return true if the destination prefix is valid, false if the destination prefix is
     *         invalid.
     */
    private static final boolean isDestinationPrefixValid(String destinationPrefix)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isDestinationPrefixValid", destinationPrefix);
        boolean isValid = true; // Assume the prefix is valid until we know otherwise.

        // null indicates that no destination prefix is being used.
        if (null != destinationPrefix)
        {
            // Check for the length first.
            int len = destinationPrefix.length();

            if (len > 24)
            {
                isValid = false;
            }
            else
            {
                // Cycle through each character in the prefix until we find an invalid character, 
                // or until we come to the end of the string.
                int along = 0;

                while ((along < len) && isValid)
                {
                    char c = destinationPrefix.charAt(along);
                    if (!(('A' <= c) && ('Z' >= c)))
                    {
                        if (!(('a' <= c) && ('z' >= c)))
                        {
                            if (!(('0' <= c) && ('9' >= c)))
                            {
                                if ('.' != c && '/' != c && '%' != c)
                                {
                                    // This character isn't a valid one...  
                                    isValid = false;
                                }
                            }
                        }
                    }
                    // Move along to the next character in the string.
                    along += 1;
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isDestinationPrefixValid", Boolean.valueOf(isValid));
        return isValid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#deleteSystemDestination(com.ibm.ws.sib.mfp.JsDestinationAddress)
     */
    @Override
    public void deleteSystemDestination(JsDestinationAddress destAddress)
                    throws SINotPossibleInCurrentConfigurationException,
                    SIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteSystemDestination", destAddress);

        // See if this connection has been closed
        checkNotClosed();

        if (!destAddress.getDestinationName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteSystemDestination", "SIIncorrectCallException");
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "SYSTEM_DESTINATION_USAGE_ERROR_CWSIP0024",
                                                    new Object[] { destAddress.getDestinationName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        _destinationManager.deleteSystemDestination(destAddress);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteSystemDestination");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#deleteSystemDestination(com.ibm.ws.sib.mfp.JsDestinationAddress)
     */
    @Override
    public void deleteSystemDestination(String prefix)
                    throws SINotPossibleInCurrentConfigurationException,
                    SIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteSystemDestination", prefix);
        // See if this connection has been closed
        checkNotClosed();

        _destinationManager.deleteSystemDestination(
                        SIMPUtils.createJsSystemDestinationAddress(prefix,
                                                                   _messageProcessor.getMessagingEngineUuid()));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteSystemDestination");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#setMessageCopiedWhenSent(boolean)
     * 
     * Attribute on the MPCoreConnection. If set, a producer send will not copy
     * the message.
     */
    @Override
    public void setMessageCopiedWhenSent(boolean copied)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setMessageCopiedWhenSent", Boolean.valueOf(copied));
        _copyMessagesWhenSent = copied;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setMessageCopiedWhenSent");
    }

    boolean getMessageCopiedWhenSent()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getMessageCopiedWhenSent");
            SibTr.exit(tc, "getMessageCopiedWhenSent", Boolean.valueOf(_copyMessagesWhenSent));
        }
        return _copyMessagesWhenSent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#setMessageCopiedWhenReceived(boolean)
     * 
     * Attribute on the MPCoreConnection. If set, a consumer receive will not copy
     * the message.
     */
    @Override
    public void setMessageCopiedWhenReceived(boolean copied)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setMessageCopiedWhenReceived", Boolean.valueOf(copied));
        _copyMessagesWhenReceived = copied;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setMessageCopiedWhenReceived");
    }

    boolean getMessageCopiedWhenReceived()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getMessageCopiedWhenReceived");
            SibTr.exit(tc, "getMessageCopiedWhenReceived", Boolean.valueOf(_copyMessagesWhenReceived));
        }
        return _copyMessagesWhenReceived;
    }

    private void checkConsumerSessionNullParameters(SIDestinationAddress destAddr) throws SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkConsumerSessionNullParameters", destAddr);

        if (destAddr == null)
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls_cwsir.getFormattedMessage(
                                                                          "CREATE_CONSUMER_CWSIR0021",
                                                                          null,
                                                                          null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkConsumerSessionNullParameters", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkConsumerSessionNullParameters");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType, java.lang.String,
     * java.lang.String, com.ibm.websphere.sib.Reliability, boolean, boolean, com.ibm.websphere.sib.Reliability, com.ibm.wsspi.sib.core.OrderingContext)
     * 
     * Added in the M7 Core SPI updates.
     */
    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddr,
                                                 DestinationType destinationType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createConsumerSession",
                        new Object[] { this,
                                      destAddr,
                                      alternateUser,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(nolocal),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable) });
        // See if this connection has been closed
        checkNotClosed();

        checkConsumerSessionNullParameters(destAddr);

        ConsumerSession session =
                        internalCreateConsumerSession(destAddr,
                                                      alternateUser,
                                                      destinationType,
                                                      criteria,
                                                      reliability,
                                                      enableReadAhead,
                                                      nolocal,
                                                      false,
                                                      false,
                                                      unrecoverableReliability,
                                                      bifurcatable,
                                                      false,
                                                      true,
                                                      false // gatherMessages
                        );

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createConsumerSession", session);
        return session;
    }

    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddr,
                                                 DestinationType destinationType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser,
                                                 boolean ignoreInitialIndoubts)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createConsumerSession",
                        new Object[] { this,
                                      destAddr,
                                      alternateUser,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(nolocal),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      Boolean.valueOf(ignoreInitialIndoubts) });

        // See if this connection has been closed
        checkNotClosed();

        checkConsumerSessionNullParameters(destAddr);

        ConsumerSession session =
                        internalCreateConsumerSession(destAddr,
                                                      alternateUser,
                                                      destinationType,
                                                      criteria,
                                                      reliability,
                                                      enableReadAhead,
                                                      nolocal,
                                                      false,
                                                      false,
                                                      unrecoverableReliability,
                                                      bifurcatable,
                                                      false,
                                                      ignoreInitialIndoubts,
                                                      false // gatherMessages
                        );

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createConsumerSession", session);

        return session;
    }

    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddr,
                                                 DestinationType destinationType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser,
                                                 boolean ignoreInitialIndoubts,
                                                 boolean gatherMessages,
                                                 Map<String, String> messageControlProperties)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createConsumerSession",
                        new Object[] { this,
                                      destAddr,
                                      alternateUser,
                                      destinationType,
                                      criteria,
                                      reliability,
                                      Boolean.valueOf(enableReadAhead),
                                      Boolean.valueOf(nolocal),
                                      unrecoverableReliability,
                                      Boolean.valueOf(bifurcatable),
                                      Boolean.valueOf(ignoreInitialIndoubts),
                                      Boolean.valueOf(gatherMessages) });

        // See if this connection has been closed
        checkNotClosed();

        checkConsumerSessionNullParameters(destAddr);

        ConsumerSession session =
                        internalCreateConsumerSession(destAddr,
                                                      alternateUser,
                                                      destinationType,
                                                      criteria,
                                                      reliability,
                                                      enableReadAhead,
                                                      nolocal,
                                                      false,
                                                      false,
                                                      unrecoverableReliability,
                                                      bifurcatable,
                                                      false,
                                                      ignoreInitialIndoubts,
                                                      gatherMessages // gatherMessages
                        );

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createConsumerSession", session);

        return session;
    }

    /**
     * This method accepts the same arguments as the Core SPI equivalent
     * and returns the same class objects. The difference is that this
     * method will only work against MQLink destinations. If a non-mqlink
     * destination is supplied an SIDestinationWrongTypeException is thrown.
     * 
     * @see com.ibm.wsspi.sib.core.SICoreException#createConsumerSession(SIBUuid12, boolean)
     * 
     * @param mqLinkUuid
     * @param selector
     * @param unrecoverableReliability
     * @return
     * @throws SIDestinationWrongTypeException
     * @throws SIDestinationNotFoundException
     * @throws SIDestinationLockedException
     * @throws SISelectorSyntaxException
     * @throws SIObjectClosedException
     * @throws SICoreException
     */
    @Override
    public ConsumerSession createMQLinkConsumerSession(
                                                       String mqLinkUuidStr,
                                                       SelectionCriteria criteria,
                                                       Reliability unrecoverableReliability)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(
                        CoreSPIConnection.tc,
                        "createMQLinkConsumerSession",
                        new Object[] {
                                      mqLinkUuidStr,
                                      criteria,
                                      unrecoverableReliability });

        MQLinkHandler mqLinkHandler = null;
        boolean forwardScanning = false;

        // See if this connection has been closed
        checkNotClosed();

        if (mqLinkUuidStr == null)
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls.getFormattedMessage(
                                                                    "MISSING_PARAM_ERROR_CWSIP0029",
                                                                    new Object[] { "1:5347:1.347.1.25" },
                                                                    null));
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", e);
            }
            throw e;
        }

        SIBUuid8 mqLinkUuid = new SIBUuid8(mqLinkUuidStr);

        // Get the destination. 
        mqLinkHandler = _destinationManager.getMQLinkLocalization(mqLinkUuid, false);

        // Check that it is an MQLink Handler

        if (mqLinkHandler == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", "SINotPossibleInCurrentConfigurationException");

            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "MQLINK_ERROR_CWSIP0026",
                                                    new Object[] {
                                                                  mqLinkUuid,
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        ConsumerSessionImpl consumer = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we try to add the consumer.
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            //create a state object for this consumer session
            //In this basic form it is just a wrapper for the discriminator and selector 
            ConsumerDispatcherState state = new ConsumerDispatcherState(
                            null,
                            mqLinkHandler.getUuid(),
                            criteria,
                            false,
                            "", // null durableHome ok here
                            mqLinkHandler.getName(),
                            mqLinkHandler.getBusName());

            // Create a destination address
            SIDestinationAddress destAddress = DestinationSessionUtils.createJsDestinationAddress(mqLinkHandler);

            try
            {
                //create a new ConsumerSession
                consumer =
                                new ConsumerSessionImpl(
                                                mqLinkHandler,
                                                destAddress,
                                                state,
                                                this,
                                                false,
                                                forwardScanning,
                                                unrecoverableReliability,
                                                false,
                                                true,
                                                false);
            } catch (SIDurableSubscriptionMismatchException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.createMQLinkConsumerSession",
                                            "1:5424:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                    SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:5435:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:5444:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SINonDurableSubscriptionMismatchException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.createMQLinkConsumerSession",
                                            "1:5318:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                    SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:5329:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:5338:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SIDurableSubscriptionNotFoundException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.createMQLinkConsumerSession",
                                            "1:5455:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                    SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:5466:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:5475:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SISessionUnavailableException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.createMQLinkConsumerSession",
                                            "1:5486:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                    SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:5497:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:5506:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            } catch (SITemporaryDestinationNotFoundException e)
            {
                // This exception should not be thrown so FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.createMQLinkConsumerSession",
                                            "1:5517:1.347.1.25",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                    SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                          "1:5528:1.347.1.25",
                                          e });

                // This should never be thrown
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConnectionImpl",
                                                                      "1:5537:1.347.1.25",
                                                                      e },
                                                        null),
                                e);
            }

            synchronized (_consumers)
            {
                //store a reference
                _consumers.add(consumer);
            }

            _messageProcessor.addConsumer(consumer);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createMQLinkConsumerSession", consumer);

        return consumer;
    }

    /**
     * Retrieves the MQLink's PubSubBridge ItemStream
     * 
     * @param name of the MQLink
     */
    @Override
    public ItemStream getMQLinkPubSubBridgeItemStream(String mqLinkUuidStr)
                    throws SIException
    {
        MQLinkHandler mqLinkHandler = null;
        ItemStream mqLinkPubSubBridgeItemStream = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "getMQLinkPubSubBridgeItemStream", mqLinkUuidStr);

        // See if this connection has been closed
        checkNotClosed();

        if (mqLinkUuidStr == null)
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls.getFormattedMessage(
                                                                    "MISSING_PARAM_ERROR_CWSIP0029",
                                                                    new Object[] { "1:5583:1.347.1.25" },
                                                                    null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "getMQLinkPubSubBridgeItemStream", e);
            }
            throw e;
        }

        SIBUuid8 mqLinkUuid = new SIBUuid8(mqLinkUuidStr);

        // Get the destination. It must be localised on this ME
        mqLinkHandler = _destinationManager.getMQLinkLocalization(mqLinkUuid, false);

        // Check the destination type
        if (mqLinkHandler == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getMQLinkPubSubBridgeItemStream", "SINotPossibleInCurrentConfigurationException");
            throw new SINotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage(
                                                    "MQLINK_PSB_ERROR_CWSIP0027",
                                                    new Object[] {
                                                                  mqLinkUuid,
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
        }

        // Got the right kind of handler, now get its itemstream
        mqLinkPubSubBridgeItemStream = mqLinkHandler.getMqLinkPubSubBridgeItemStream();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMQLinkPubSubBridgeItemStream", mqLinkPubSubBridgeItemStream);

        return mqLinkPubSubBridgeItemStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createOrderingContext()
     */
    @Override
    public OrderingContext createOrderingContext()

                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createOrderingContext", this);

        // Check that the connection isn't closed.
        checkNotClosed();

        OrderingContext context = new OrderingContextImpl();

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createOrderingContext", context);

        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getResolvedUserid()
     */
    @Override
    public String getResolvedUserid()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "getResolvedUserid", this);

        // See if this connection has been closed
        checkNotClosed();

        String userName = null;
        try {
            if (_subject != null)
                userName = runtimeSecurityService.getUniqueUserName(_subject);
        } catch (MessagingSecurityException mse) {
            // No FFDC Needed here, just log an exception
            if (CoreSPIConnection.tc.isDebugEnabled())
                SibTr.exception(tc, mse);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "getResolvedUserid", userName);

        return userName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createBifurcatedConsumerSession(long)
     */
    @Override
    public BifurcatedConsumerSession createBifurcatedConsumerSession(long id)

                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "createBifurcatedConsumerSession",
                        new Object[] { this, Long.valueOf(id) });

        // Check that the connection isn't closed.
        checkNotClosed();

        ConsumerSessionImpl session = findConsumerSession(id);

        // If the consumer session is null, throw a SISessionNotFoundException
        if (session == null)
        {
            // Add correct NLS trace
            SISessionUnavailableException e =
                            new SISessionUnavailableException(
                                            nls.getFormattedMessage(
                                                                    "CREATE_CONSUMER_ERROR_CWSIP0092",
                                                                    new Object[] { Long.valueOf(id),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));

            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConnection.tc, "createBifurcatedConsumerSession", e);
            throw e;
        }

        if (!session.isBifurcatable())
        {
            SIIncorrectCallException e =
                            new SIIncorrectCallException(
                                            nls.getFormattedMessage(
                                                                    "CONSUMER_NOT_BIFURCATABLE_CWSIP0680",
                                                                    new Object[] { Long.valueOf(id) },
                                                                    null));

            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConnection.tc, "createBifurcatedConsumerSession", e);
            throw e;
        }

        // Now we have the ConsumerSession, create a Bifurcated Consumer
        BifurcatedConsumerSessionImpl consumerSession = new BifurcatedConsumerSessionImpl(this, session);

        // Add the new Bifurcated consumer to the list.
        synchronized (_bifurcatedConsumers)
        {
            _bifurcatedConsumers.add(consumerSession);
        }

        // if security is enabled, audit the creation of the new session
        if (_isBusSecure)
        {
            AuthUtils sibAuthUtils = _messageProcessor.getAuthorisationUtils();

            // get the destination name from the session
            String destination = consumerSession.getDestinationAddress().getDestinationName();

            // call through to audit event for successful creation
            sibAuthUtils.createBifurcatedConsumerSessionAuthorizationPassed(_subject, destination, id);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "createBifurcatedConsumerSession", consumerSession);

        return consumerSession;
    }

    /**
     * Remove a bifurcated consumer from the connection list.
     * 
     * @param bifurcatedConsumer
     */
    void removeBifurcatedConsumerSession(BifurcatedConsumerSessionImpl bifurcatedConsumer)
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "removeBifurcatedConsumerSession",
                        new Object[] { this, bifurcatedConsumer });

        // Remove the consumer from the list.
        synchronized (_bifurcatedConsumers)
        {
            _bifurcatedConsumers.remove(bifurcatedConsumer);
        }

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "removeBifurcatedConsumerSession");
    }

    /**
     * Searches through the local connection, then through any other connections
     * looking for the matching consumer session for the given id.
     * 
     * @param id
     * @return
     * @throws SINotAuthorizedException
     */
    private ConsumerSessionImpl findConsumerSession(long id) throws SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "findConsumerSession", Long.valueOf(id));

        ConsumerSessionImpl sessionInternal = _messageProcessor.getConsumer(id);

        if (sessionInternal != null)
        {
            // Check if this is the right Consumer.
            if (sessionInternal.getIdInternal() == id)
            {
                if (sessionInternal.getConnectionInternal() != this)
                {
                    // Defect 346001. Check that the subjects match, but only if bus security is enabled
                    if (_isBusSecure)
                    {
                        // get the bus security interface in order to make audit calls
                        AuthUtils sibAuthUtils = _messageProcessor.getAuthorisationUtils();

                        // get the name of the destination the consumer is connected to (used for auditing)
                        String destinationName = sessionInternal.getDestinationAddress().getDestinationName();

                        // Check that the subjects match.
                        Subject connsSubject = ((ConnectionImpl) sessionInternal.getConnectionInternal()).getSecuritySubject();

                        if (connsSubject != null)
                        {
                            boolean subjectsDiffer = false;

                            // Check null subject
                            if (_subject == null)
                            {
                                subjectsDiffer = true;
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "Subjects differ - base subject is null");
                            }

                            // Check for privileged SIBServerSubject
                            if (!subjectsDiffer && isSIBServerSubject())
                            {
                                // Connected user is privileged SIBServerSubject, check that the
                                // bifurcated session has same degree of privilege
                                if (!_messageProcessor.getAuthorisationUtils().isSIBServerSubject(connsSubject))
                                {
                                    subjectsDiffer = true;
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(tc, "Subjects differ - base subject is privileged");
                                }
                            }

                            if (!subjectsDiffer)
                            {
                                // Compare the resolved string userids
                                String resolvedConnUserid = _messageProcessor.getAuthorisationUtils().getUserName(connsSubject);
                                String resolvedUserid = _messageProcessor.getAuthorisationUtils().getUserName(_subject);

                                // NB Sib.security, ensures that resolved Userids are non-null                                 
                                if (!resolvedConnUserid.equals(resolvedUserid))
                                {
                                    subjectsDiffer = true;
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(tc, "Subjects differ by userid - base " +
                                                        resolvedUserid + ", connected " +
                                                        resolvedConnUserid);
                                }
                            }

                            // Throw exception
                            if (subjectsDiffer)
                            {
                                // audit the authorization failure
                                sibAuthUtils.createBifurcatedConsumerSessionAuthorizationFailed(connsSubject, destinationName, id);

                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "findConsumerSession", "SINotAuthorizedException - not same");
                                throw new SINotAuthorizedException(
                                                nls.getFormattedMessage(
                                                                        "CREATE_BIFURCATED_CONSUMER_ERROR_CWSIP0094",
                                                                        new Object[] { Long.valueOf(id),
                                                                                      _messageProcessor.getMessagingEngineName() },
                                                                        null));
                            }
                        }
                        else if (_subject != null)
                        {
                            // audit the authorization failure  
                            sibAuthUtils.createBifurcatedConsumerSessionAuthorizationFailed(connsSubject, destinationName, id);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "findConsumerSession", "SINotAuthorizedException - null");

                            throw new SINotAuthorizedException(
                                            nls.getFormattedMessage(
                                                                    "CREATE_BIFURCATED_CONSUMER_ERROR_CWSIP0094",
                                                                    new Object[] { Long.valueOf(id),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));
                        }
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "findConsumerSession", sessionInternal);
        return sessionInternal;
    }

    final Subject getSecuritySubject()
    {
        return _subject;
    }

    /**
     * Checks the authority of a producer to produce to a destination
     * 
     */
    // Need to remove this for Liberty Messaging (Holding it till review happens)
    @Deprecated
    private void checkProducerAuthority(SIDestinationAddress destAddr,
                                        DestinationHandler destination,
                                        String destinationName,
                                        String busName,
                                        SecurityContext secContext,
                                        boolean system)
                    throws SIDiscriminatorSyntaxException, SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkProducerAuthority",
                        new Object[] { destAddr,
                                      destination,
                                      destinationName,
                                      busName,
                                      secContext,
                                      Boolean.valueOf(system) });

        // Check authority to produce to destination unless we're sending to a system
        // queue in the local bus.
        if (!system || (destAddr.getBusName() == null) || destAddr.getBusName().equals(_messageProcessor.getMessagingEngineBus()))
        {
            // Perform the alternate user check first. If an alternateUser was set then we 
            // need to determine whether the connected subject has the authority to perform
            // alternate user checks.

            if (secContext.isAlternateUserBased())
            {
                if (!destination.checkDestinationAccess(secContext,
                                                        OperationType.IDENTITY_ADOPTER))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "checkProducerAuthority", "not authorized to perform alternate user checks on this destination");

                    // Get the username
                    String userName = secContext.getUserName(true);
                    // Build the message for the Exception and the Notification
                    String nlsMessage =
                                    nls_cwsik.getFormattedMessage("DELIVERY_ERROR_SIRC_18", // USER_NOT_AUTH_SEND_ERROR_CWSIP0306
                                                                  new Object[] { destinationName,
                                                                                userName },
                                                                  null);

                    // Fire a Notification if Eventing is enabled
                    _accessChecker.
                                    fireDestinationAccessNotAuthorizedEvent(destinationName,
                                                                            userName,
                                                                            OperationType.IDENTITY_ADOPTER,
                                                                            nlsMessage);

                    // Thrown if user denied access to destination
                    SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(nlsMessage);

                    e.setExceptionReason(SIRCConstants.SIRC0018_USER_NOT_AUTH_SEND_ERROR);
                    e.setExceptionInserts(new String[] { destinationName, secContext.getUserName(true) });

                    throw e;
                }
            }

            if (!destAddr.isTemporary()) // Non-temorary destination access check
            {
                if (!destination.checkDestinationAccess(secContext,
                                                        OperationType.SEND))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "checkProducerAuthority", "not authorized to produce to this destination");

                    // Get the username
                    String userName = secContext.getUserName(false);
                    // Build the message for the Exception and the Notification
                    String nlsMessage =
                                    nls_cwsik.getFormattedMessage("DELIVERY_ERROR_SIRC_18", // USER_NOT_AUTH_SEND_ERROR_CWSIP0306
                                                                  new Object[] { destinationName,
                                                                                userName },
                                                                  null);

                    // Fire a Notification if Eventing is enabled
                    _accessChecker.
                                    fireDestinationAccessNotAuthorizedEvent(destinationName,
                                                                            userName,
                                                                            OperationType.SEND,
                                                                            nlsMessage);

                    // Thrown if user denied access to destination
                    SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(nlsMessage);

                    e.setExceptionReason(SIRCConstants.SIRC0018_USER_NOT_AUTH_SEND_ERROR);
                    e.setExceptionInserts(new String[] { destinationName, secContext.getUserName(false) });

                    throw e;
                }
            }
            else // Check authority to produce to temp topic
            {
                // get the temp prefix from the destination name
                String destinationPrefix = SIMPUtils.parseTempPrefix(destinationName);
                if (!_accessChecker.checkTemporaryDestinationAccess(secContext,
                                                                    destinationName, // name of destination  
                                                                    (destinationPrefix == null) ? "" : destinationPrefix, // sib.security wants empty string if prefix is null
                                                                    OperationType.SEND))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "checkProducerAuthority", "not authorized to produce to temporary destination");

                    // Get the username
                    String userName = secContext.getUserName(false);
                    // Build the message for the Exception and the Notification
                    String nlsMessage =
                                    nls_cwsik.getFormattedMessage("DELIVERY_ERROR_SIRC_19", // USER_NOT_AUTH_SEND_ERROR_CWSIP0306
                                                                  new Object[] { destinationPrefix,
                                                                                userName },
                                                                  null);

                    // Fire a Notification if Eventing is enabled
                    _accessChecker.
                                    fireDestinationAccessNotAuthorizedEvent(destinationPrefix,
                                                                            userName,
                                                                            OperationType.SEND,
                                                                            nlsMessage);

                    // Thrown if user denied access to destination
                    SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(nlsMessage);

                    e.setExceptionReason(SIRCConstants.SIRC0019_USER_NOT_AUTH_SEND_ERROR);
                    e.setExceptionInserts(new String[] { destinationPrefix, secContext.getUserName(false) });

                    throw e;
                }
            }

            // Check authority to produce to discriminator
            if (secContext.testDiscriminatorAtCreate())
            {
                if (!destination.checkDiscriminatorAccess(secContext,
                                                          OperationType.SEND))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "checkProducerAuthority", "not authorized to produce to this destination's discriminator");
                    // Write an audit record if access is denied
                    SibTr.audit(tc, nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
                                                                  new Object[] { destination.getName(),
                                                                                secContext.getDiscriminator(),
                                                                                secContext.getUserName(false) },
                                                                  null));

                    // Thrown if user denied access to destination
                    SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(
                                    nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
                                                                  new Object[] { destination.getName(),
                                                                                secContext.getDiscriminator(),
                                                                                secContext.getUserName(false) },
                                                                  null));

                    e.setExceptionReason(SIRCConstants.SIRC0020_USER_NOT_AUTH_SEND_ERROR);
                    e.setExceptionInserts(new String[] { destination.getName(),
                                                        secContext.getDiscriminator(),
                                                        secContext.getUserName(false) });
                    throw e;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkProducerAuthority");
    }

    /**
     * Checks the authority of a consumer to consume from a destination
     * 
     */
    @Deprecated
    private void checkConsumerAuthority(DestinationHandler destination,
                                        String destinationName,
                                        String discriminator,
                                        SecurityContext secContext,
                                        boolean system)

                    throws SIDiscriminatorSyntaxException, SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkConsumerAuthority",
                        new Object[] { destination,
                                      destinationName,
                                      discriminator,
                                      secContext,
                                      Boolean.valueOf(system) });

        // Check authority to consume from a destination
        if (!destination.isTemporary() && !system)
        {
            boolean allowed = true;
            boolean failingOpisIdentityAdopter = false;
            // Perform the alternate user check first. If an alternateUser was set then we 
            // need to determine whether the connected subject has the authority to perform
            // alternate user checks.

            if (secContext.isAlternateUserBased())
            {
                if (!destination.checkDestinationAccess(secContext,
                                                        OperationType.IDENTITY_ADOPTER))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "checkConsumerAuthority", "not authorized to perform alternate user checks on this destination");

                    allowed = false;
                    failingOpisIdentityAdopter = true;
                }
            }

            if (allowed) // ok so far
            {
                // Check if its the default exc dest
                if (destinationName.startsWith(SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX))
                {
                    //If its a def exc dest see if we have access to the prefix.
                    if (!_messageProcessor.
                                    getAccessChecker().
                                    checkDestinationAccess(secContext,
                                                           null, // home bus  
                                                           SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX,
                                                           OperationType.RECEIVE))
                    {
                        allowed = false;
                    }
                }
                else
                {
                    if (!destination.checkDestinationAccess(secContext,
                                                            OperationType.RECEIVE))
                    {
                        allowed = false;
                    }
                }
            }

            if (!allowed)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkConsumerAuthority", "not authorized to consume from this destination");

                // Get the username
                String userName = secContext.getUserName(failingOpisIdentityAdopter);
                OperationType operationType =
                                failingOpisIdentityAdopter ? OperationType.IDENTITY_ADOPTER :
                                                OperationType.RECEIVE;

                // Build the message for the Exception and the Notification
                String nlsMessage =
                                nls.getFormattedMessage("USER_NOT_AUTH_RECEIVE_ERROR_CWSIP0309",
                                                        new Object[] { destination.getName(),
                                                                      userName },
                                                        null);

                // Fire a Notification if Eventing is enabled
                _accessChecker.
                                fireDestinationAccessNotAuthorizedEvent(destination.getName(),
                                                                        userName,
                                                                        operationType,
                                                                        nlsMessage);

                // Thrown if user denied access to destination
                throw new SINotAuthorizedException(nlsMessage);
            }
        }

        // If the discriminator is non-wildcarded, we can check authority to consume on
        // the discriminator.
        if (discriminator != null &&
            !_messageProcessor.getMessageProcessorMatching().isWildCarded(discriminator))
        {
            // set the discriminator into the security context
            secContext.setDiscriminator(discriminator);
            if (!destination.checkDiscriminatorAccess(secContext,
                                                      OperationType.RECEIVE))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkConsumerAuthority", "not authorized to consume from this destination's discriminator");
                // Write an audit record if access is denied
                SibTr.audit(tc, nls.getFormattedMessage(
                                                        "USER_NOT_AUTH_RECEIVE_ERROR_CWSIP0310",
                                                        new Object[] { destination.getName(),
                                                                      discriminator,
                                                                      secContext.getUserName(false) },
                                                        null));

                // Thrown if user denied access to destination
                throw new SINotAuthorizedException(
                                nls.getFormattedMessage(
                                                        "USER_NOT_AUTH_RECEIVE_ERROR_CWSIP0310",
                                                        new Object[] { destination.getName(),
                                                                      discriminator,
                                                                      secContext.getUserName(false) },
                                                        null));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkConsumerAuthority");
    }

    // Security changes for messaging security: Sharath Start
    /**
     * 
     * @param destination
     * @param operationType
     * @param descriminator
     * @throws SINotAuthorizedException
     */
    private void checkDestinationAuthority(DestinationHandler destination, String operationType, String descriminator) throws SINotAuthorizedException {

        SibTr.entry(tc, "checkDestinationAuthority", new Object[] { destination, operationType, descriminator });
        DestinationType destinationType = destination.getDestinationType();
        String destinationName = destination.getName();

        try {
            if (destination.isTemporary()) {
                /*
                 * In case of Creating a Temporary Destination, we will get its prefix, comparing is straight forward
                 * In case of other operations such as SEND or RECEIVE, we will get complete destination name, we have to parse it
                 * and check for the prefix in that destination name.
                 * 
                 * Extract prefix from the destination name
                 * Get the substring of the destination from by knocking of the specific prefix which was added for Queue or Topic
                 */
                String destinationPrefix = destinationName;
                if (destinationName.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX)) {
                    destinationPrefix = destinationName.substring(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX.length());
                } else if (destinationName.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX)) {
                    destinationPrefix = destinationName.substring(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX.length());
                }
                _authorization.checkTemporaryDestinationAccess(_subject, destinationPrefix, operationType);
            }

            else if (destination.isAlias()) {
                destination = destination.getResolvedDestinationHandler();
                int type = 0;
                if (destinationType == DestinationType.QUEUE)
                    type = MessagingSecurityConstants.DESTINATION_TYPE_QUEUE;
                else if (destinationType == DestinationType.TOPICSPACE)
                    type = MessagingSecurityConstants.DESTINATION_TYPE_TOPICSPACE;
                _authorization.checkAliasAccess(_subject, destination.getName(), destinationName, type, operationType);
            }

            else if (destination.isPubSub()) {
                _authorization.checkTopicAccess(_subject, destinationName, descriminator, operationType);
            }

            else if (destinationType.equals(DestinationType.QUEUE)) {
                _authorization.checkQueueAccess(_subject, destinationName, operationType);
            }

        } catch (MessagingAuthorizationException mae) {
            throw new SINotAuthorizedException(mae.getMessage());
        }

        SibTr.exit(tc, "checkDestinationAuthority");

    }

    private void checkTempDestinationCreation(String destinationPrefix, Distribution distribution) throws SINotAuthorizedException {
        SibTr.entry(tc, "checkTempDestinationCreation", new Object[] { destinationPrefix, distribution });

        try {
            _authorization.checkTemporaryDestinationAccess(_subject, destinationPrefix, MessagingSecurityConstants.OPERATION_TYPE_CREATE);
        } catch (MessagingAuthorizationException mae) {
            throw new SINotAuthorizedException(mae.getMessage());
        }

        SibTr.exit(tc, "checkTempDestinationCreation");
    }

    // Security changes for messaging security: Sharath End

    /**
     * Checks the authority of a consumer to consume from a discriminator
     * 
     */
    private boolean checkConsumerDiscriminatorAccess(DestinationHandler destination,
                                                     String discriminator,
                                                     SecurityContext secContext)

                    throws SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkConsumerDiscriminatorAccess",
                        new Object[] { destination,
                                      discriminator,
                                      secContext });

        boolean allowed = true;
        // If the discriminator is non-wildcarded, we can check authority to consume on
        // the discriminator.
        if (discriminator != null &&
            !_messageProcessor.getMessageProcessorMatching().isWildCarded(discriminator))
        {
            // set the discriminator into the security context
            secContext.setDiscriminator(discriminator);
            if (!destination.checkDiscriminatorAccess(secContext,
                                                      OperationType.RECEIVE))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkConsumerDiscriminatorAccess", "not authorized to consume from this destination's discriminator");
                // Write an audit record if access is denied
                SibTr.audit(tc, nls.getFormattedMessage(
                                                        "USER_NOT_AUTH_RECEIVE_ERROR_CWSIP0310",
                                                        new Object[] { destination.getName(),
                                                                      discriminator,
                                                                      secContext.getUserName(false) },
                                                        null));

                allowed = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkConsumerDiscriminatorAccess", Boolean.valueOf(allowed));
        return allowed;
    }

    /**
     * Checks the authority of a consumer to consume from a destination
     * 
     */
    @Deprecated
    private void checkBrowseAuthority(DestinationHandler destination,
                                      String destinationName,
                                      SecurityContext secContext,
                                      boolean system)

                    throws SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkBrowseAuthority",
                        new Object[] { destination,
                                      destinationName,
                                      secContext,
                                      Boolean.valueOf(system) });

        // Check authority to browse a destination
        if (!destination.isTemporary() && !system)
        {
            boolean allowed = true;
            boolean failingOpisIdentityAdopter = false;
            // Perform the alternate user check first. If an alternateUser was set then we 
            // need to determine whether the connected subject has the authority to perform
            // alternate user checks.

            if (secContext.isAlternateUserBased())
            {
                if (!destination.checkDestinationAccess(secContext,
                                                        OperationType.IDENTITY_ADOPTER))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "checkBrowseAuthority", "not authorized to perform alternate user checks on this destination");

                    allowed = false;
                    failingOpisIdentityAdopter = true;
                }
            }

            if (allowed) // ok so far
            {
                // Check if its the default exc dest
                if (destinationName.startsWith(SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX))
                {
                    //If its a def exc dest see if we have access to the prefix.
                    if (!_messageProcessor.
                                    getAccessChecker().
                                    checkDestinationAccess(secContext,
                                                           null, // home bus  
                                                           SIMPConstants.SYSTEM_DEFAULT_EXCEPTION_DESTINATION_PREFIX,
                                                           OperationType.RECEIVE))
                    {
                        allowed = false;
                    }
                }
                else
                {
                    if (!destination.checkDestinationAccess(secContext,
                                                            OperationType.BROWSE))
                    {
                        allowed = false;
                    }
                }
            }

            if (!allowed)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkBrowseAuthority", "not authorized to browse this destination");

                // Get the username
                String userName = secContext.getUserName(failingOpisIdentityAdopter);
                OperationType operationType =
                                failingOpisIdentityAdopter ? OperationType.IDENTITY_ADOPTER :
                                                OperationType.BROWSE;

                // Build the message for the Exception and the Notification
                String nlsMessage =
                                nls.getFormattedMessage("USER_NOT_AUTH_BROWSE_ERROR_CWSIP0304",
                                                        new Object[] { destination.getName(),
                                                                      userName },
                                                        null);

                // Fire a Notification if Eventing is enabled
                _accessChecker.
                                fireDestinationAccessNotAuthorizedEvent(destination.getName(),
                                                                        userName,
                                                                        operationType,
                                                                        nlsMessage);

                // Thrown if user denied access to destination
                throw new SINotAuthorizedException(nlsMessage);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkBrowseAuthority");
    }

    /**
     * Checks the authority to inquire on a destination
     * 
     */
    private void checkInquireAuthority(DestinationHandler destination,
                                       String destinationName,
                                       String busName,
                                       SecurityContext secContext,
                                       boolean temporary)
                    throws SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkInquireAuthority",
                        new Object[] { destination,
                                      destinationName,
                                      busName,
                                      secContext,
                                      Boolean.valueOf(temporary) });

        // Check authority to inquire on the destination

        if (!temporary) // Non-temorary destination access check
        {
            if (!destination.checkDestinationAccess(secContext,
                                                    OperationType.INQUIRE))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkInquireAuthority", "not authorized to inquire on this destination");

                // Get the username
                String userName = secContext.getUserName(true);

                //Messaging varies dependent on bus versus destination access check
                String nlsMessage;
                if (destinationName != null)
                {
                    // Build the message for the Exception and the Notification
                    nlsMessage =
                                    nls.getFormattedMessage("USER_NOT_AUTH_INQUIRE_ERROR_CWSIP0314",
                                                            new Object[] { destinationName,
                                                                          userName },
                                                            null);

                    // Fire a Notification if Eventing is enabled
                    _accessChecker.
                                    fireDestinationAccessNotAuthorizedEvent(destinationName,
                                                                            userName,
                                                                            OperationType.INQUIRE,
                                                                            nlsMessage);
                }
                else // foreign bus access
                {
                    // Build the message for the Exception and the Notification
                    nlsMessage =
                                    nls.getFormattedMessage("USER_NOT_AUTH_INQUIRE_FB_ERROR_CWSIP0315",
                                                            new Object[] { busName,
                                                                          userName },
                                                            null);

                    // Fire a Notification if Eventing is enabled
                    _accessChecker.
                                    fireDestinationAccessNotAuthorizedEvent(busName,
                                                                            userName,
                                                                            OperationType.INQUIRE,
                                                                            nlsMessage);
                }

                // Thrown if user denied access to destination
                throw new SINotAuthorizedException(nlsMessage);
            }
        }
        else // Check authority to inquire on temp topic
        {
            // get the temp prefix from the destination name
            String destinationPrefix = SIMPUtils.parseTempPrefix(destinationName);
            if (!_accessChecker.checkTemporaryDestinationAccess(secContext,
                                                                destinationName, // name of destination
                                                                (destinationPrefix == null) ? "" : destinationPrefix, // sib.security wants empty string if prefix is null
                                                                OperationType.INQUIRE))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "checkInquireAuthority", "not authorized to inquire on temporary destination");

                // Get the username
                String userName = secContext.getUserName(true);
                // Build the message for the Exception and the Notification
                String nlsMessage =
                                nls.getFormattedMessage("USER_NOT_AUTH_INQUIRE_ERROR_CWSIP0314",
                                                        new Object[] { destinationPrefix,
                                                                      userName },
                                                        null);

                // Fire a Notification if Eventing is enabled
                _accessChecker.
                                fireDestinationAccessNotAuthorizedEvent(destinationPrefix,
                                                                        userName,
                                                                        OperationType.INQUIRE,
                                                                        nlsMessage);

                // Thrown if user denied access to destination
                throw new SINotAuthorizedException(nlsMessage);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkInquireAuthority");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#addDestinationListener(java.lang.String, com.ibm.wsspi.sib.core.DestinationListener, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.DestinationAvailability)
     */
    @Override
    public SIDestinationAddress[] addDestinationListener(
                                                         String destinationNamePattern,
                                                         DestinationListener destinationListener,
                                                         DestinationType destinationType,
                                                         DestinationAvailability destinationAvailability)
                    throws SIIncorrectCallException,
                    SICommandInvocationFailedException,
                    SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addDestinationListener", new Object[] { destinationNamePattern,
                                                                    destinationListener,
                                                                    destinationType,
                                                                    destinationAvailability });

        // Check that a listener has been specified
        if (destinationListener == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addDestinationListener", "SIIncorrectCallException");
            // Parameter is null, throw an excepiton
            throw new SIIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "ADD_DEST_LISTENER_ERROR_CWSIP0803",
                                                    null,
                                                    null));
        }

        //make sure that the connection is open before trying to use it
        checkNotClosed();

        SIDestinationAddress[] destinationAddresses = null;

        synchronized (this)
        {
            checkNotClosed();
            destinationAddresses =
                            _destinationManager.addDestinationListener(destinationNamePattern,
                                                                       destinationListener,
                                                                       destinationType,
                                                                       destinationAvailability,
                                                                       this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addDestinationListener", new Object[] { destinationAddresses });
        return destinationAddresses;
    }

    /**
     * @return
     */
    @Override
    public boolean isBusSecure()
    {
        return _isBusSecure;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#sendToExceptionDestination(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.SIBusMessage, int,
     * java.lang.Object[], com.ibm.wsspi.sib.core.SITransaction, boolean)
     */
    @Override
    public void sendToExceptionDestination(
                                           SIDestinationAddress address,
                                           SIBusMessage message,
                                           int reason,
                                           String[] inserts,
                                           SITransaction tran,
                                           String alternateUser)
                    throws SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.entry(CoreSPIConnection.tc, "sendToExceptionDestination",
                        new Object[] { this, address, alternateUser, message, Integer.valueOf(reason), inserts, tran });

        if (message == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
                SibTr.exit(CoreSPIConnection.tc, "sendToExceptionDestination", "SIIncorrectCallException - null msg");
            throw new SIIncorrectCallException(
                            nls_cwsir.getFormattedMessage(
                                                          "SEND_EXCEPTION_DEST_CWSIR0121",
                                                          null,
                                                          null));
        }

        // Get an exception destination instance
        ExceptionDestinationHandlerImpl exDestHandler =
                        new ExceptionDestinationHandlerImpl(address, _messageProcessor);

        exDestHandler.sendToExceptionDestination(message,
                                                 alternateUser,
                                                 (TransactionCommon) tran,
                                                 reason,
                                                 this,
                                                 inserts);

        if (TraceComponent.isAnyTracingEnabled() && CoreSPIConnection.tc.isEntryEnabled())
            SibTr.exit(CoreSPIConnection.tc, "sendToExceptionDestination");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#setWaitTimeInMessage(boolean)
     */
    @Override
    public void setSetWaitTimeInMessage(boolean setWaitTime)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setSetWaitTimeInMessage", Boolean.valueOf(setWaitTime));
        _setWaitTimeInMessage = setWaitTime;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setSetWaitTimeInMessage");
    }

    boolean getSetWaitTimeInMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getSetWaitTimeInMessage");
            SibTr.exit(tc, "getSetWaitTimeInMessage", Boolean.valueOf(_setWaitTimeInMessage));
        }
        return _setWaitTimeInMessage;
    }

    /**
     * Returns true if the subject associated with the connection is the
     * privileged SIBServerSubject.
     * 
     * @return true if SIBServerSubject
     */
    private boolean isSIBServerSubject()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isSIBServerSubject");

        boolean ispriv = false;

        if (_subject != null)
            ispriv = _messageProcessor.getAuthorisationUtils().isSIBServerSubject(_subject);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isSIBServerSubject", Boolean.valueOf(ispriv));

        return ispriv;
    }

    /**
     * Returns true if multicast is enabled
     */
    @Override
    public boolean isMulticastEnabled()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isMulticastEnabled");

        boolean enabled = _messageProcessor.isMulticastEnabled();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isMulticastEnabled", Boolean.valueOf(enabled));
        return enabled;
    }

    /**
     * Returns the MulticastProperties for this messaging engine
     * null if multicast is not enabled.
     */
    @Override
    public MulticastProperties getMulticastProperties()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMulticastProperties");

        MulticastProperties props = _messageProcessor.getMulticastProperties();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMulticastProperties", props);

        return props;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#checkMessagingRequired(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType, java.lang.String)
     */
    @Override
    public SIDestinationAddress checkMessagingRequired(SIDestinationAddress requestDestAddr,
                                                       SIDestinationAddress replyDestAddr,
                                                       DestinationType destinationType,
                                                       String alternateUser)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIErrorException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkMessagingRequired",
                        new Object[] { requestDestAddr,
                                      replyDestAddr,
                                      alternateUser,
                                      destinationType });

        //Assume the bus cannot be skipped
        boolean destinationIsFastPath = false;
        boolean requestIsFastPath = false;
        boolean replyIsFastPath = true;
        boolean foreignBusFound = false;
        boolean destinationIsSendAllowed;
        List<JsDestinationAddress> rrp = new LinkedList<JsDestinationAddress>();
        SIDestinationAddress targetPort = null;
        boolean loopFound = false;
        Set<String> duplicateDestinationNames = null;

        // Synchronize on the close object, we don't want the connection closing
        // while we check if the destination can be fast-pathed
        synchronized (this)
        {
            // See if this connection has been closed
            checkNotClosed();

            //Validate the sending destination addresses is not null
            checkProducerSessionNullParameters(requestDestAddr);

            SecurityContext secContext = null;
            // If security is enabled then we need to set up a security context
            if (_isBusSecure)
            {
                // Add the alternate user string to the security context
                secContext = new SecurityContext(_subject,
                                alternateUser,
                                null,
                                _messageProcessor.getAuthorisationUtils());
            }

            String destinationName = requestDestAddr.getDestinationName();
            String busName = requestDestAddr.getBusName();

            if ((busName == null) || (busName.equals(_messageProcessor.getMessagingEngineBus())))
            {
                //Look up the named destination    
                DestinationHandler destination = null;
                destination = _destinationManager.getDestination(destinationName, busName, false);

                // Check the destination type
                checkDestinationType(destinationType, requestDestAddr, destination, false);

                // Ensure that the destination is put enabled
                destinationIsSendAllowed = destination.isSendAllowed();

                if (!destination.isPubSub() && destination.hasLocal() && destinationIsSendAllowed)
                {
                    PtoPLocalMsgsItemStream ptoPLocalMsgsItemStream =
                                    (PtoPLocalMsgsItemStream) destination.getQueuePoint(_messageProcessor.getMessagingEngineUuid());

                    destinationIsSendAllowed = ptoPLocalMsgsItemStream.isSendAllowed();
                }

                // Check authority to produce to destination
                // If security is disabled then we'll bypass the check
                if (_isBusSecure)
                {
                    checkProducerAuthority(requestDestAddr,
                                           destination,
                                           destinationName,
                                           busName,
                                           secContext,
                                           false);

                    // Go on to check authority to consume from the reply destination.
                    // We do this up front to ensure that an SINotAuth exception can
                    // be thrown as early as possible.                       
                    if (replyDestAddr != null)
                    {
                        String replyDestinationName = replyDestAddr.getDestinationName();
                        String replyBusName = replyDestAddr.getBusName();

                        //Look up the reply destination    
                        DestinationHandler replyDestination =
                                        _destinationManager.getDestination(replyDestinationName,
                                                                           replyBusName,
                                                                           false);

                        // Check authority to produce to the reply destination.  The reply will 
                        // be produced with the same userid as the request
                        // If security is disabled then we'll bypass the check
                        checkDestinationAccess(replyDestination,
                                               replyDestinationName,
                                               replyBusName,
                                               secContext);
                    }
                }

                //Set up the reverse routing path as we go along.
                JsDestinationAddress replyDest = destination.getReplyDestination();
                if ((replyDest != null) && (replyDestAddr != null))
                {
                    rrp.add(replyDest);
                }

                //Now validate each entry on the administered forward routing path.
                //If one is found that breaks the fastpath rules, then leave the
                //loop
                JsDestinationAddress entry = null;
                DestinationHandler frpDestination = destination;

                //Get the administered forward routing path from the named destination
                List frp = destination.getForwardRoutingPath();

                if (frp != null)
                {
                    int frpSize = frp.size();
                    int frpCount = 0;

                    //Ensure there is no infinite loop in the forward routing path
                    duplicateDestinationNames = new HashSet<String>();
                    duplicateDestinationNames.add(destination.getName());

                    while ((!frpDestination.isPubSub()) && //not a topicspace
                           (!foreignBusFound) &&
                           (!loopFound) &&
                           (destinationIsSendAllowed) &&
                           (frpCount < frpSize)) //forward routing path
                                                 //contains entries
                    {
                        // read element from FRP
                        entry = (JsDestinationAddress) frp.get(frpCount);
                        frpCount++;

                        String frpName = entry.getDestinationName();
                        String frpBusName = entry.getBusName();

                        if ((frpBusName == null) || (frpBusName.equals(_messageProcessor.getMessagingEngineBus())))
                        {
                            //Get the named destination from the destination manager
                            frpDestination = _destinationManager.getDestination(frpName, frpBusName, false);

                            // If security is enabled, then we need to check authority to access
                            // the next destination
                            // Check authority to produce to destination
                            // If security is disabled then we'll bypass the check
                            if (_isBusSecure)
                            {
                                checkDestinationAccess(frpDestination,
                                                       frpName,
                                                       frpBusName,
                                                       secContext);
                            }

                            //Set up the reverse routing path as we go along.
                            replyDest = frpDestination.getReplyDestination();
                            if ((replyDest != null) && (replyDestAddr != null))
                            {
                                rrp.add(replyDest);
                            }

                            //If this is the last destination in the FRP, then see if this
                            //destination has a default adminstered FRP which we should now
                            //check.
                            if (frpCount == frpSize)
                            {
                                List additionalFRP = frpDestination.getForwardRoutingPath();
                                if (additionalFRP != null)
                                {
                                    //Before adding an additional forward routing path, ensure it wont make us loop forever
                                    if (duplicateDestinationNames.contains(frpDestination.getName()))
                                    {
                                        loopFound = true;
                                    }
                                    else
                                    {
                                        duplicateDestinationNames.add(frpDestination.getName());
                                        frp = additionalFRP;
                                        frpSize = frp.size();
                                        frpCount = 0;
                                    }
                                }
                            }
                        }
                        else
                        {
                            foreignBusFound = true;
                        }
                    }
                }

                //We have either succesfully checked all the destinations on the
                //administered forward routing path, or frpDestination will be
                //referencing the first destination that failed one of the 
                //checks.  Determine here which is the correct response to give
                //the caller.
                if ((!foreignBusFound) &&
                    (!loopFound) &&
                    (destinationIsSendAllowed) &&
                    (frpDestination.hasLocal()) && //local queue-point
                    (frpDestination.getDestinationType() == DestinationType.PORT) &&
                    (!frpDestination.isPubSub())) //not a topicspace 
                {
                    requestIsFastPath = true;
                    targetPort = JsMainAdminComponentImpl.getSIDestinationAddressFactory().createSIDestinationAddress(frpDestination.getName(), frpDestination.getBus());
                }

                //Now check if the reply message can also be fastpathed.
                //Only worth checking if the request can be.
                if (requestIsFastPath)
                {
                    DestinationHandler rrpDestination = null;

                    while (!rrp.isEmpty())
                    {
                        // Pop off first element of RRP
                        entry = rrp.remove(0);

                        String rrpName = entry.getDestinationName();
                        String rrpBusName = entry.getBusName();

                        if ((rrpBusName != null) &&
                            (!(rrpBusName.equals(_messageProcessor.getMessagingEngineBus()))))
                        {
                            replyIsFastPath = false;
                            rrp.clear();
                        }
                        else
                        {
                            //Get the named destination from the destination manager
                            rrpDestination = _destinationManager.getDestination(rrpName, rrpBusName, false);

                            // If security is enabled, then we need to check authority to access
                            // the next destination
                            // Check authority to produce to destination
                            // If security is disabled then we'll bypass the check
                            if (_isBusSecure)
                            {
                                checkDestinationAccess(rrpDestination,
                                                       rrpName,
                                                       rrpBusName,
                                                       secContext);
                            }
                        }
                    }

                    if (replyIsFastPath)
                    {
                        if (replyDestAddr != null)
                        {
                            //The reverse routing path was checked and is ok.  Now check the final
                            //reply destination.

                            destinationName = replyDestAddr.getDestinationName();
                            busName = replyDestAddr.getBusName();

                            if ((busName != null) &&
                                (!(busName.equals(_messageProcessor.getMessagingEngineBus()))))
                            {
                                replyIsFastPath = false;
                            }
                            else
                            {
                                //Look up the named destination    
                                destination = _destinationManager.getDestination(destinationName, busName, false);

                                boolean replyDestinationHasFRP = false;
                                List replyDestinationFRP = destination.getForwardRoutingPath();
                                if ((replyDestinationFRP != null) &&
                                    (!(replyDestinationFRP.isEmpty())))
                                {
                                    replyDestinationHasFRP = true;
                                }

                                //The reply destination must also have a local queue point and no Forward Routing Path
                                if ((!(destination.hasLocal())) ||
                                    (replyDestinationHasFRP == true) ||
                                    (!(destination.isReceiveAllowed())))
                                {
                                    replyIsFastPath = false;
                                }
                            }
                        }
                    }

                    if ((requestIsFastPath) && (replyIsFastPath))
                    {
                        destinationIsFastPath = true;
                    }
                }
            }
        }

        if (!destinationIsFastPath)
            targetPort = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkMessagingRequired", targetPort);

        return targetPort;
    }

    /**
     * Checks the authority of a producer to produce to a destination
     * 
     */
    private void checkDestinationAccess(DestinationHandler destination,
                                        String destinationName,
                                        String busName,
                                        SecurityContext secContext)
                    throws SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDestinationAccess",
                        new Object[] { destination,
                                      destinationName,
                                      busName,
                                      secContext });

        // Check authority to produce to destination
        if (!destination.checkDestinationAccess(secContext,
                                                OperationType.SEND))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkDestinationAccess", "not authorized to produce to this destination");

            // Thrown if user denied access to destination
            SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_18", // USER_NOT_AUTH_SEND_ERROR_CWSIP0306
                                                          new Object[] { destinationName,
                                                                        secContext.getUserName(false) },
                                                          null));

            e.setExceptionReason(SIRCConstants.SIRC0018_USER_NOT_AUTH_SEND_ERROR);
            e.setExceptionInserts(new String[] { destinationName, secContext.getUserName(false) });

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDestinationAccess");
    }

    /**
     * Retrieve the MPSubscription object that represents the named durable subscription
     * 
     * This function is only available on locally homed subscriptions
     **/
    @Override
    public MPSubscription getSubscription(String subscriptionName)
                    throws SIDurableSubscriptionNotFoundException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "getSubscription",
                        subscriptionName);

        HashMap durableSubs = _destinationManager.getDurableSubscriptionsTable();

        ConsumerDispatcher cd = null;
        synchronized (durableSubs)
        {
            //Look up the consumer dispatcher for this subId in the system durable subs list
            cd =
                            (ConsumerDispatcher) durableSubs.get(subscriptionName);

            // Check that the durable subscription exists
            if (cd == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "getSubscription", "Durable sub not found");

                throw new SIDurableSubscriptionNotFoundException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
                                                        new Object[] { subscriptionName,
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }
        }

        MPSubscription mpSubscription = cd.getMPSubscription();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSubscription", mpSubscription);
        return mpSubscription;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.MPCoreConnection#registerCommandHandler(java.lang.String, com.ibm.ws.sib.processor.CommandHandler)
     */
    @Override
    public void registerCommandHandler(String key, CommandHandler handler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerCommandHandler",
                        new Object[] { key, handler });

        // Table of keys and non-tx handlers is managed at ME level
        _messageProcessor.registerCommandHandler(key, handler);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerCommandHandler");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#invokeCommand(java.lang.String, java.lang.String, java.io.Serializable)
     */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData, SITransaction transaction)
                    throws SIIncorrectCallException,
                    SINotAuthorizedException,
                    SICommandInvocationFailedException,
                    SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "invokeCommand",
                        new Object[] { key, commandName, commandData, transaction });

        Serializable returnData = internalInvokeCommand(key,
                                                        commandName,
                                                        commandData,
                                                        true, //always a tx invoke for this flavour of invokeCommand
                                                        transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "invokeCommand", returnData);

        return returnData;
    }

    /**
     * 
     * @param key
     * @param commandName
     * @param commandData
     * @param isTransactionalInvoke tells whether this is the transactional flavour of invoke
     * @param transaction
     * @return
     * @throws SIIncorrectCallException
     * @throws SINotAuthorizedException
     * @throws SICommandInvocationFailedException
     */
    private Serializable internalInvokeCommand(String key, String commandName, Serializable commandData, boolean isTransactionalInvoke, SITransaction transaction)
                    throws SIIncorrectCallException,
                    SINotAuthorizedException,
                    SICommandInvocationFailedException,
                    SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "internalInvokeCommand",
                        new Object[] { key, commandName, commandData, Boolean.valueOf(isTransactionalInvoke), transaction });

        // Check that the parameters are valid
        {
            if (key == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalInvokeCommand", "SIIncorrectCallException");
                // Parameter is null, throw an excepiton
                throw new SIIncorrectCallException(
                                nls.getFormattedMessage(
                                                        "INVOKE_COMMAND_ERROR_CWSIP0802",
                                                        new Object[] { "key" },
                                                        null));
            }
            if (commandName == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalInvokeCommand", "SIIncorrectCallException");
                // Parameter is null, throw an excepiton
                throw new SIIncorrectCallException(
                                nls.getFormattedMessage(
                                                        "INVOKE_COMMAND_ERROR_CWSIP0802",
                                                        new Object[] { "commandName" },
                                                        null));
            }
            if (!isTransactionalInvoke && transaction != null)
            {
                //should never have a transaction associated with the non-tx 
                //flavour of invoke.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalInvokeCommand", "SIIncorrectCallException");
                throw new SIIncorrectCallException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                        new Object[] { "1:7172:1.347.1.25" },
                                                        null));
            }
        }

        //make sure that the connection is open before trying to use it
        checkNotClosed();

        // If the User of the connection is not SIBServerSubject then 
        // reject the attempt to invoke
        if ((_isBusSecure) && (!isSIBServerSubject()))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "internalInvokeCommand", "SINotAuthorizedException");
            throw new SINotAuthorizedException(
                            nls.getFormattedMessage(
                                                    "USER_NOT_AUTH_INVOKE_ERROR_CWSIP0316",
                                                    new Object[] { _subject },
                                                    null));
        }

        Serializable returnValue = null;

        CommandHandler tempCmdHandler = _messageProcessor.getRegisteredCommandHandler(key);

        if (isTransactionalInvoke || (tempCmdHandler instanceof TransactionalCommandHandler))
        {
            //we expect to find a tx command handler for this invoke
            if (tempCmdHandler == null || !(tempCmdHandler instanceof TransactionalCommandHandler))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalInvokeCommand", "SICommandInvocationFailedException");
                throw new SICommandInvocationFailedException(
                                nls.getFormattedMessage(
                                                        "INVOKE_COMMAND_ERROR_CWSIP0801",
                                                        new Object[] { key },
                                                        null));
            }

            TransactionalCommandHandler cHandler = (TransactionalCommandHandler) tempCmdHandler;

            // Now invoke the command against the transactional command handler
            // Note that exceptions will be returned as linked exceptions
            // wrapped in an SICommandInvocationFailedException
            try
            {
                returnValue = cHandler.invoke(commandName, commandData, transaction);
            } catch (Exception e)
            {
                // FFDC
                FFDCFilter.processException(e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalInvokeCommand",
                                            "1:7226:1.347.1.25", this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalInvokeCommand", e);

                throw new SICommandInvocationFailedException(
                                nls.getFormattedMessage(
                                                        "INVOKE_COMMAND_ERROR_CWSIR0150",
                                                        new Object[] { e },
                                                        null),
                                e);
            }
        }
        else
        {
            CommandHandler cHandler = _messageProcessor.getRegisteredCommandHandler(key);
            if (cHandler == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalInvokeCommand", "SICommandInvocationFailedException");
                throw new SICommandInvocationFailedException(
                                nls.getFormattedMessage(
                                                        "INVOKE_COMMAND_ERROR_CWSIR0151",
                                                        new Object[] { key },
                                                        null));
            }

            // Now invoke the command against the non-transactional handler
            // Note that exceptions will be returned as linked exceptions
            // wrapped in an SICommandInvocationFailedException
            try
            {
                returnValue = cHandler.invoke(commandName, commandData);
            } catch (Exception e)
            {
                // FFDC
                FFDCFilter.processException(e,
                                            "com.ibm.ws.sib.processor.impl.ConnectionImpl.internalInvokeCommand",
                                            "1:7267:1.347.1.25", this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalInvokeCommand", e);

                throw new SICommandInvocationFailedException(
                                nls.getFormattedMessage(
                                                        "INVOKE_COMMAND_ERROR_CWSIR0150",
                                                        new Object[] { e },
                                                        null),
                                e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalInvokeCommand", returnValue);

        return returnValue;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#invokeCommand(java.lang.String, java.lang.String, java.io.Serializable)
     */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData)
                    throws SIIncorrectCallException,
                    SINotAuthorizedException,
                    SICommandInvocationFailedException,
                    SIConnectionUnavailableException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "invokeCommand",
                        new Object[] { key, commandName, commandData });

        Serializable returnData = internalInvokeCommand(key,
                                                        commandName,
                                                        commandData,
                                                        false, //always non-tx invoke for this flavour of invokeCommand
                                                        null); //always null for this flavour of invokeCommand

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "invokeCommand", returnData);

        return returnData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#registerConsumerSetMonitor(SIDestinationAddress destinationAddress, String discriminatorExpression, ConsumerSetChangeCallback
     * callback)
     */
    @Override
    public boolean registerConsumerSetMonitor(
                                              SIDestinationAddress destinationAddress,
                                              String discriminatorExpression,
                                              ConsumerSetChangeCallback callback)
                    throws SIResourceException,
                    SINotPossibleInCurrentConfigurationException,
                    SIConnectionUnavailableException,
                    SIConnectionDroppedException,
                    SIIncorrectCallException,
                    SICommandInvocationFailedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "registerConsumerSetMonitor",
                        new Object[] { destinationAddress,
                                      discriminatorExpression,
                                      callback });

        // Check that a listener has been specified
        if (callback == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerConsumerSetMonitor", "SIIncorrectCallException");
            // Parameter is null, throw an excepiton
            throw new SIIncorrectCallException(nls.getFormattedMessage("NULL_CONSUMERSETCHANGECALLBACK_CWSIP0667", null, null));
        } else if (destinationAddress == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerConsumerSetMonitor", "SIIncorrectCallException");
            // Parameter is null, throw an excepiton
            throw new SIIncorrectCallException(nls.getFormattedMessage("NULL_DESTINATIONADDRESS_CWSIP0668", null, null));

        } else if (discriminatorExpression == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerConsumerSetMonitor", "SIIncorrectCallException");
            // Parameter is null, throw an excepiton
            throw new SIIncorrectCallException(nls.getFormattedMessage("NULL_DISCRIMINATOREXPRESSION_CWSIP0669", null, null));

        }
        boolean areConsumers = false;
        try
        {
            DestinationHandler topicSpace =
                            _destinationManager.getDestination((JsDestinationAddress) destinationAddress, false);

            areConsumers =
                            _messageProcessor.
                                            getMessageProcessorMatching().
                                            registerConsumerSetMonitor(topicSpace,
                                                                       discriminatorExpression,
                                                                       this,
                                                                       callback);
        } catch (Exception e)
        {
            // FFDC
            FFDCFilter
                            .processException(
                                              e,
                                              "com.ibm.ws.sib.processor.impl.ConnectionImpl.registerConsumerSetMonitor",
                                              "1:7378:1.347.1.25",
                                              this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerConsumerSetMonitor", "SIErrorException");
            //SIErrorException is RuntimeExcetion hence catching this exception in StaticCATConnection
            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerConsumerSetMonitor", Boolean.valueOf(areConsumers));
        return areConsumers;
    }

    /**
     * Deregisters a previously registered callback.
     */
    @Override
    public void deregisterConsumerSetMonitor(
                                             ConsumerSetChangeCallback callback)
                    throws SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deregisterConsumerSetMonitor",
                        new Object[] { callback });

        _messageProcessor.
                        getMessageProcessorMatching().
                        deregisterConsumerSetMonitor(this, callback);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterConsumerSetMonitor");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsaddressing.HAResource#getAffinityKey()
     */
    /*
     * public synchronized Identity getAffinityKey() {
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     * SibTr.entry(tc, "getAffinityKey");
     * 
     * if (null == _trmMeMain) {
     * _trmMeMain = (TrmMeMain) _messageProcessor.getMessagingEngine()
     * .getEngineComponent(JsConstants.SIB_CLASS_TO_ENGINE);
     * }
     * Identity id = _trmMeMain.getAffinityKey();
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     * SibTr.exit(tc, "getAffinityKey", id);
     * 
     * return id;
     * }
     */

    /**
     * Retrieve the properties associated with this connection.
     */
    public Map getConnectionProperties()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getConnectionProperties");
            SibTr.exit(tc, "getConnectionProperties", _connectionProperties);
        }
        return _connectionProperties;
    }

    /**
     * Set the properties associated with this connection. Supports Unittest environment.
     */
    public void setConnectionProperties(Map connectionProperties)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getConnectionProperties", connectionProperties);
            SibTr.exit(tc, "getConnectionProperties");
        }
        _connectionProperties = connectionProperties;
    }

    /**
     * 
     * @return connectionType i.e Type of connection whether inprocess/comms/commsSSL
     */
    public int getConnectionType() {
        return connectionType;
    }

    /**
     * Sets connection type. </br>
     * Three possible Values: 1 - inproces, 2 - via comms, 3 - via comms ssl.
     */
    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    public boolean isConnectionClosed() {
        return _closed;
    }

}
