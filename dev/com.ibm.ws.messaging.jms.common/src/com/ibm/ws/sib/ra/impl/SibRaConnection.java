/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.ra.impl;

import java.io.Serializable;
import java.util.Map;

//lohith liberty change
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.LazyAssociatableConnectionManager;
import javax.resource.spi.ResourceAdapterInternalException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
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
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;
import com.ibm.wsspi.sib.ra.SibRaAutoCommitTransaction;
import com.ibm.wsspi.sib.ra.SibRaNotSupportedException;

/**
 * Implementation of <code>SICoreConnection</code> for core SPI resource
 * adapter. Created by the <code>getConnection</code> method on
 * <code>SibRaManagedConnection</code> this is, in JCA terminology, a
 * connection handle. Initially associated with a managed connection, the fact
 * that the managed connection implements
 * <code>DissociatableManagedConnection</code> means that, at some point in
 * future it may be dissociated from that managed connection. When a managed
 * connection is required again <code>associateConnection</code> must be
 * called on the connection manager. The managed connection is used to determine
 * whether there is currently a container local or global transaction. If there
 * is no container transaction then the caller may create an application local
 * transaction. This transaction object must then be passed as the transaction
 * parameter on method calls until it is completed. The connection handle has
 * its own <code>SICoreConnection</code> to which methods delegate.
 */
final class SibRaConnection implements SICoreConnection {

    /**
     * The managed connection currently associated with this connection. This
     * field should be accessed via <code>getAssociatedManagedConnection</code>
     * if a non-null managed connection is required.
     */
    private SibRaManagedConnection _managedConnection;

    /**
     * The connection request information that led to the creation of this
     * connection. Required in order to re-obtain a managed connection following
     * dissociation.
     */
    private final SibRaConnectionRequestInfo _requestInfo;

    /**
     * The core connection to which method calls delegate.
     */
    private final SICoreConnection _delegateConnection;

    /**
     * Flag indicating whether the connection has been invalidated by
     * <code>cleanup</code> being called on a managed connection while this
     * connection was associated with it.
     */
    private boolean _valid = true;

    /**
     * The current application local transaction created by calling
     * <code>createUncoordinatedTransaction</code> in the absence of a
     * container transaction.
     */
    private SibRaUncoordinatedTransaction _applicationLocalTransaction;

    /**
     * The parent connection factory from which this connection was created.
     * Required in order to obtain access to the managed connection factory and
     * connection manager in order to perform lazy enlistment and
     * re-association.
     */
    private SibRaConnectionFactory _connectionFactory;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
                    .getTraceComponent(SibRaConnection.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();

    /**
     * Constructor. Called by <code>getConnection</code> on
     * <code>SibRaManagedConnection</code>.
     * 
     * @param managedConnection
     *            the managed connection initially associated with this
     *            connection handle
     * @param requestInfo
     *            the request information that led to the creation of this
     *            connection
     * @param coreConnection
     *            the core connection to which methods should delegate
     */
    SibRaConnection(final SibRaManagedConnection managedConnection,
                    final SibRaConnectionRequestInfo requestInfo,
                    final SICoreConnection coreConnection) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaConnection", new Object[] {
                                                                      managedConnection, requestInfo, coreConnection });
        }

        _managedConnection = managedConnection;
        _requestInfo = requestInfo;
        _delegateConnection = coreConnection;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaConnection");
        }
    }

    /**
     * Creates a unique identifier. Checks that the connection is valid and then
     * delegates.
     * 
     * @return a unique identifier
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public byte[] createUniqueId() throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SIResourceException, SIErrorException {

        checkValid();
        return _delegateConnection.createUniqueId();

    }

    /**
     * Creates a temporary destination. Checks that the connection is valid and
     * then delegates.
     * 
     * @return a temporary destination
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIInvalidDestinationPrefixException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public SIDestinationAddress createTemporaryDestination(
                                                           final Distribution distribution, final String destinationPrefix)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIInvalidDestinationPrefixException, SIResourceException,
                    SIErrorException {

        checkValid();
        return _delegateConnection.createTemporaryDestination(distribution,
                                                              destinationPrefix);

    }

    /**
     * Calls invokeCommand on the delegate connection.
     * 
     * @throws SINotAuthorizedException
     * @throws SICommandInvocationFailedException
     * @throws SIIncorrectCallException
     * @throws SIResourceException
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     * @see com.ibm.wsspi.sib.core.SICoreConnection#invokeCommand(java.lang.String, java.lang.String, java.io.Serializable)
     */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData)
                    throws SIConnectionDroppedException, SIConnectionUnavailableException,
                    SINotAuthorizedException, SIResourceException, SIIncorrectCallException,
                    SICommandInvocationFailedException {

        return _delegateConnection.invokeCommand(key, commandName, commandData);

    }

    /**
     * Calls invokeCommand on the delegate connection.
     * 
     * @throws SINotAuthorizedException
     * @throws SICommandInvocationFailedException
     * @throws SIIncorrectCallException
     * @throws SIResourceException
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     * @see com.ibm.wsspi.sib.core.SICoreConnection#invokeCommand(java.lang.String, java.lang.String, java.io.Serializable)
     */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData,
                                      SITransaction tran)
                    throws SIConnectionDroppedException, SIConnectionUnavailableException,
                    SINotAuthorizedException, SIResourceException, SIIncorrectCallException,
                    SICommandInvocationFailedException {

        return _delegateConnection.invokeCommand(key, commandName, commandData, mapTransaction(tran));

    }

    /**
     * Closes this connection. Delegates and then informs the current managed
     * connection.
     * 
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     */
    @Override
    public void close() throws SIConnectionLostException, SIResourceException,
                    SIErrorException, SIConnectionDroppedException,
                    SIConnectionUnavailableException
    {
        try
        {
            _delegateConnection.close();
        } finally
        {
            if (_managedConnection != null)
            {
                _managedConnection.connectionClosed(this);
            }
        }
    }

    /* PM39926-Start */
    /**
     * Closes this connection. Delegates and then informs the current managed
     * connection.
     * 
     * @param bForceFlag - Flag to indicate that connections have to be closed and cannot be reset.
     *            If marked reset the connection would not be released instead will be reused.
     * 
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     */
    @Override
    public void close(boolean bForceFlag) throws SIConnectionLostException, SIResourceException,
                    SIErrorException, SIConnectionDroppedException,
                    SIConnectionUnavailableException
    {
        try
        {
            _delegateConnection.close(bForceFlag);
        } finally
        {
            if (_managedConnection != null)
            {
                _managedConnection.connectionClosed(this);
            }
        }
    }

    /* PM39926-End */

    /**
     * Creates an <code>SIUncoordinatedTransaction</code> to represent an
     * application local transaction. Checks that the connection is valid and
     * there is not currently an application local transaction or container
     * transaction. Delegates and wraps the returned
     * <code>SIUncoordinatedTransaction</code> in order to intercept
     * completion calls.
     * 
     * @return an <code>SIUncoordinatedTransaction</code>
     * @throws SIIncorrectCallException
     *             if there is still an active application local transaction
     *             associated with this connection or if there is an active
     *             container transaction
     * @throws SIResourceException
     */
    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction()
                    throws SIIncorrectCallException, SIConnectionUnavailableException,
                    SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createUncoordinatedTransaction");
        }

        checkValid();

        // Check we don't already have an active application local transaction
        if (_applicationLocalTransaction != null) {

            final SIIncorrectCallException exception = new SIIncorrectCallException(
                            NLS.getString("ACTIVE_LOCAL_TRAN_CWSIV0150"));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        // Check that there isn't an active container transaction
        if (getContainerTransaction() != null) {

            final SIIncorrectCallException exception = new SIIncorrectCallException(
                            NLS.getString("ACTIVE_CONTAINER_TRAN_CWSIV0151"));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        _applicationLocalTransaction = new SibRaUncoordinatedTransaction(
                        _delegateConnection.createUncoordinatedTransaction());

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createUncoordinatedTransaction",
                       _applicationLocalTransaction);
        }
        return _applicationLocalTransaction;
    }

    /**
     * Creates an <code>SIUncoordinatedTransaction</code> to represent an
     * application local transaction. Checks that the connection is valid and
     * there is not currently an application local transaction or container
     * transaction. Delegates and wraps the returned
     * <code>SIUncoordinatedTransaction</code> in order to intercept
     * completion calls.
     * 
     * @param allowSubordinates True if we allow subordinates, false if not
     *            (being set could mean that 1PC optimization is in use).
     * @return an <code>SIUncoordinatedTransaction</code>
     * @throws SIIncorrectCallException
     *             if there is still an active application local transaction
     *             associated with this connection or if there is an active
     *             container transaction
     * @throws SIResourceException
     */
    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction(boolean allowSubordinates)
                    throws SIIncorrectCallException, SIConnectionUnavailableException,
                    SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "createUncoordinatedTransaction", allowSubordinates);
        }

        checkValid();

        // Check we don't already have an active application local transaction
        if (_applicationLocalTransaction != null) {

            final SIIncorrectCallException exception = new SIIncorrectCallException(
                            NLS.getString("ACTIVE_LOCAL_TRAN_CWSIV0150"));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        // Check that there isn't an active container transaction
        if (getContainerTransaction() != null) {

            final SIIncorrectCallException exception = new SIIncorrectCallException(
                            NLS.getString("ACTIVE_CONTAINER_TRAN_CWSIV0151"));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        _applicationLocalTransaction = new SibRaUncoordinatedTransaction(
                        _delegateConnection.createUncoordinatedTransaction(allowSubordinates));

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "createUncoordinatedTransaction",
                       _applicationLocalTransaction);
        }
        return _applicationLocalTransaction;
    }

    /**
     * Creation of an <code>SIXAResource</code> is not supported by the
     * resource adapter.
     * 
     * @return never
     * @throws SibRaNotSupportedException
     *             always
     */
    @Override
    public SIXAResource getSIXAResource() throws SibRaNotSupportedException {

        throw new SibRaNotSupportedException(NLS
                        .getString("XARESOURCE_NOT_SUPPORTED_CWSIV0152"));

    }

    /**
     * Creates a producer session. Checks that the connection is valid and then
     * delegates. Wraps the <code>ProducerSession</code> returned from the
     * delegate in a <code>SibRaProducerSession</code>.
     * 
     * @param destAddr
     *            the address of the destination
     * @param destType
     *            the destination type
     * @param orderingContext
     *            indicates that the order of messages from multiple
     *            ProducerSessions should be preserved
     * @param alternateUser
     *            the name of the user under whose authority operations of the
     *            ProducerSession should be performed (may be null)
     * @return the producer session
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public ProducerSession createProducerSession(
                                                 final SIDestinationAddress destAddr,
                                                 final DestinationType destType,
                                                 final OrderingContext orderingContext, final String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException {

        checkValid();

        final ProducerSession session = _delegateConnection
                        .createProducerSession(destAddr, destType, orderingContext,
                                               alternateUser);

        return new SibRaProducerSession(this, session);

    }

    /**
     * Creates a producer session. Checks that the connection is valid and then
     * delegates. Wraps the <code>ProducerSession</code> returned from the
     * delegate in a <code>SibRaProducerSession</code>.
     * 
     * @param destAddr
     *            the address of the destination
     * @param discriminator
     *            the disriminator
     * @param destType
     *            the destination type
     * @param orderingContext
     *            indicates that the order of messages from multiple
     *            ProducerSessions should be preserved
     * @param alternateUser
     *            the name of the user under whose authority operations of the
     *            ProducerSession should be performed (may be null)
     * @return the producer session
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIDiscriminatorSyntaxException
     *             if the delegation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public ProducerSession createProducerSession(
                                                 final SIDestinationAddress destAddr, final String discriminator,
                                                 final DestinationType destType,
                                                 final OrderingContext orderingContext, final String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SITemporaryDestinationNotFoundException,
                    SIDiscriminatorSyntaxException, SIResourceException,
                    SIErrorException, SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException {

        checkValid();

        final ProducerSession session = _delegateConnection
                        .createProducerSession(destAddr, discriminator, destType,
                                               orderingContext, alternateUser);

        return new SibRaProducerSession(this, session);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(com.ibm.websphere.sib.SIDestinationAddress, java.lang.String, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, java.lang.String, boolean, boolean)
     */
    @Override
    public ProducerSession createProducerSession(
                                                 SIDestinationAddress destAddr,
                                                 String discriminator,
                                                 DestinationType destType,
                                                 OrderingContext extendedMessageOrderingContext,
                                                 String alternateUser,
                                                 boolean fixedMessagePoint,
                                                 boolean preferLocalMessagePoint)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SITemporaryDestinationNotFoundException,
                    SIIncorrectCallException, SIDiscriminatorSyntaxException
    {
        checkValid();

        final ProducerSession session = _delegateConnection
                        .createProducerSession(destAddr, discriminator, destType,
                                               extendedMessageOrderingContext, alternateUser, fixedMessagePoint, preferLocalMessagePoint);

        return new SibRaProducerSession(this, session);
    }

    /**
     * Creates a consumer session. Checks that the connection is valid and then
     * delegates. Wraps the <code>ConsumerSession</code> returned from the
     * delegate in a <code>SibRaConsumerSession</code>.
     * 
     * @param destAddr
     *            the address of the destination
     * @param destType
     *            the destination type
     * @param criteria
     *            the selection criteria
     * @param reliability
     *            the reliability
     * @param enableReadAhead
     *            flag indicating whether read ahead is enabled
     * @param nolocal
     *            flag indicating whether local messages should be received
     * @param unrecoverableReliability
     *            the unrecoverable reliability
     * @param bifurcatable
     *            whether the new ConsumerSession may be bifurcated
     * @param alternateUser
     *            the name of the user under whose authority operations of the
     *            ConsumerSession should be performed (may be null)
     * @return the consumer session
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SIDestinationLockedException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public ConsumerSession createConsumerSession(
                                                 final SIDestinationAddress destAddr,
                                                 final DestinationType destType, final SelectionCriteria criteria,
                                                 final Reliability reliability, final boolean enableReadAhead,
                                                 final boolean nolocal, final Reliability unrecoverableReliability,
                                                 final boolean bifurcatable, final String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        final ConsumerSession session = _delegateConnection
                        .createConsumerSession(destAddr, destType, criteria,
                                               reliability, enableReadAhead, nolocal,
                                               unrecoverableReliability, bifurcatable, alternateUser);

        return new SibRaConsumerSession(this, session);

    }

    /**
     * Creates a consumer session. Checks that the connection is valid and then
     * delegates. Wraps the <code>ConsumerSession</code> returned from the
     * delegate in a <code>SibRaConsumerSession</code>.
     * 
     * @param destAddr
     *            the address of the destination
     * @param destType
     *            the destination type
     * @param criteria
     *            the selection criteria
     * @param reliability
     *            the reliability
     * @param enableReadAhead
     *            flag indicating whether read ahead is enabled
     * @param nolocal
     *            flag indicating whether local messages should be received
     * @param unrecoverableReliability
     *            the unrecoverable reliability
     * @param bifurcatable
     *            whether the new ConsumerSession may be bifurcated
     * @param alternateUser
     *            the name of the user under whose authority operations of the
     *            ConsumerSession should be performed (may be null)
     * @param ignoreInitialIndoubts
     *            whether we the consumer session should wait till all
     *            indoubt transactions have completed
     * @return the consumer session
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SIDestinationLockedException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public ConsumerSession createConsumerSession(
                                                 final SIDestinationAddress destAddr,
                                                 final DestinationType destType, final SelectionCriteria criteria,
                                                 final Reliability reliability, final boolean enableReadAhead,
                                                 final boolean nolocal, final Reliability unrecoverableReliability,
                                                 final boolean bifurcatable, final String alternateUser,
                                                 boolean ignoreInitialIndoubts)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        final ConsumerSession session = _delegateConnection
                        .createConsumerSession(destAddr, destType, criteria,
                                               reliability, enableReadAhead, nolocal,
                                               unrecoverableReliability, bifurcatable, alternateUser,
                                               ignoreInitialIndoubts);

        return new SibRaConsumerSession(this, session);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria, com.ibm.websphere.sib.Reliability, boolean, boolean, com.ibm.websphere.sib.Reliability, boolean, java.lang.String, boolean,
     * boolean)
     */
    @Override
    public ConsumerSession createConsumerSession(
                                                 SIDestinationAddress destAddr,
                                                 DestinationType destType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser,
                                                 boolean ignoreInitialIndoubts,
                                                 boolean allowMessageGathering,
                                                 Map<String, String> messageControlProperties)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        checkValid();

        final ConsumerSession session = _delegateConnection
                        .createConsumerSession(destAddr, destType, criteria,
                                               reliability, enableReadAhead, nolocal,
                                               unrecoverableReliability, bifurcatable, alternateUser,
                                               ignoreInitialIndoubts, allowMessageGathering, messageControlProperties);

        return new SibRaConsumerSession(this, session);
    }

    /**
     * Sends a message. Checks that the connection is valid. Maps the
     * transaction parameter before delegating.
     * 
     * @param msg
     *            the message to send
     * @param tran
     *            the transaction to send the message under
     * @param destAddr
     *            the destination to send the message to
     * @param destType
     *            the type of the destination
     * @param orderingContext
     *            indicates that the order of messages from multiple
     *            ProducerSessions should be preserved
     * @param alternateUser
     *            the name of the user under whose authority the send should be
     *            performed (may be null)
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the transaction parameter is not valid given the current
     *             application and container transactions
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the current container transaction cannot be determined
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public void send(final SIBusMessage msg, final SITransaction tran,
                     final SIDestinationAddress destAddr,
                     final DestinationType destType,
                     final OrderingContext orderingContext, final String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        _delegateConnection.send(msg, mapTransaction(tran), destAddr, destType,
                                 orderingContext, alternateUser);

    }

    /**
     * Cloning of a connection is not supported by the resource apdater.
     * 
     * @return never
     * @throws SibRaNotSupportedException
     *             always
     */
    @Override
    public SICoreConnection cloneConnection() throws SibRaNotSupportedException {

        throw new SibRaNotSupportedException(NLS
                        .getString("CLONE_NOT_SUPPORTED_CWSIV0153"));

    }

    /**
     * Compares the given connection to this one. Delegates. Given that we don't
     * support <code>cloneConnection</code>, will only return
     * <code>true</code> if the given connection is sharing the same managed
     * connection as this one.
     * 
     * @param rhs
     *            the connection to compare
     * @return <code>true</code> if the given connection is equivalent to this
     *         one
     */
    @Override
    public boolean isEquivalentTo(SICoreConnection rhs) {

        final boolean equivalent;

        if (rhs instanceof SibRaConnection) {

            // Unwrap the other connection
            final SICoreConnection rhsDelegate = ((SibRaConnection) rhs)._delegateConnection;
            equivalent = _delegateConnection.isEquivalentTo(rhsDelegate);

        } else {

            equivalent = _delegateConnection.isEquivalentTo(rhs);

        }

        return equivalent;

    }

    /**
     * Returns the name of the messaging engine to which we have connected.
     * Delegates.
     */
    @Override
    public String getMeName() {

        return _delegateConnection.getMeName();

    }

    /**
     * Returns the UUID of the messaging engine to which we have connected.
     * Delegates.
     */
    @Override
    public String getMeUuid() {

        return _delegateConnection.getMeUuid();

    }

    /**
     * Adds a connection listener. Checks that the connection is valid and then
     * delegates.
     * 
     * @param listener
     *            the listener to add
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public void addConnectionListener(SICoreConnectionListener listener)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException {

        checkValid();

        _delegateConnection.addConnectionListener(listener);

    }

    /**
     * Removes a connection listener. Delegates.
     * 
     * @param listener
     *            the listener to remove
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     */
    @Override
    public void removeConnectionListener(SICoreConnectionListener listener)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException {

        _delegateConnection.removeConnectionListener(listener);

    }

    /**
     * Gets the current connection listeners. Delegates.
     * 
     * @return the connection listeners
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     */
    @Override
    public SICoreConnectionListener[] getConnectionListeners()
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException {

        return _delegateConnection.getConnectionListeners();

    }

    /**
     * Returns a description of the API level. Delegates.
     * 
     * @return the API level
     */
    @Override
    public String getApiLevelDescription() {

        return _delegateConnection.getApiLevelDescription();

    }

    /**
     * Returns the major version of the API. Delegates.
     * 
     * @return the major version
     */
    @Override
    public long getApiMajorVersion() {

        return _delegateConnection.getApiMajorVersion();

    }

    /**
     * Returns the minor version of the API. Delegates.
     * 
     * @return the minor version
     */
    @Override
    public long getApiMinorVersion() {

        return _delegateConnection.getApiMinorVersion();

    }

    /**
     * Check the connection is valid then delegates.
     * 
     * @param address
     *            the destination
     * @param message
     *            the message
     * @param reason
     *            the reason
     * @param inserts
     *            the inserts
     * @param tran
     *            the transaction
     * @param alternateUser
     *            the name of the user under whose authority the send should be
     *            performed (may be null)
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public void sendToExceptionDestination(SIDestinationAddress address,
                                           SIBusMessage message, int reason, String[] inserts,
                                           SITransaction tran, final String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIResourceException, SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        _delegateConnection.sendToExceptionDestination(address, message,
                                                       reason, inserts, mapTransaction(tran), alternateUser);
    }

    /**
     * Returns the configuration for the given destination. Checks that the
     * connection is valid and then delegates.
     * 
     * @param destinationAddress
     *            the address of the destination
     * @return the configuration
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public DestinationConfiguration getDestinationConfiguration(
                                                                final SIDestinationAddress destinationAddress)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SINotAuthorizedException, SITemporaryDestinationNotFoundException,
                    SIResourceException, SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        return _delegateConnection
                        .getDestinationConfiguration(destinationAddress);

    }

    /**
     * Deletes a temporary destination. Checks that the connection is valid and
     * then delegates.
     * 
     * @param destinationAddress
     *            the destination to delete
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the delegation fails
     */
    @Override
    public void deleteTemporaryDestination(
                                           SIDestinationAddress destinationAddress)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SINotAuthorizedException, SITemporaryDestinationNotFoundException,
                    SIResourceException, SIErrorException,
                    SIDestinationLockedException, SIIncorrectCallException {

        checkValid();
        _delegateConnection.deleteTemporaryDestination(destinationAddress);

    }

    /**
     * Creates a durable subscription. Checks that the connection is valid and
     * then delegates. Wraps the <code>ConsumerSession</code> returned from
     * the delegate in a <code>SibRaConsumerSession</code>.
     * 
     * @param subscriptionName
     *            the name for the subscription
     * @param durableSubscriptionHome
     *            the home for the durable subscription
     * @param destinationAddress
     *            the address of the destination
     * @param criteria
     *            the selection criteria
     * @param supportsMultipleConsumers
     *            flag indicating whether multiple consumers are supported
     * @param nolocal
     *            flag indicating whether local messages should be received
     * @param alternateUser
     *            the name of the user used to create the durable subscription
     *            (may be null)
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIDurableSubscriptionAlreadyExistsException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public void createDurableSubscription(final String subscriptionName,
                                          final String durableSubscriptionHome,
                                          final SIDestinationAddress destinationAddress,
                                          final SelectionCriteria criteria,
                                          final boolean supportsMultipleConsumers, final boolean nolocal,
                                          final String alternateUser) throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIDurableSubscriptionAlreadyExistsException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        _delegateConnection.createDurableSubscription(subscriptionName,
                                                      durableSubscriptionHome, destinationAddress, criteria,
                                                      supportsMultipleConsumers, nolocal, alternateUser);

    }

    /**
     * Creates a consumer session for a durable subscription. Checks that the
     * connection is valid and then delegates. Wraps the
     * <code>ConsumerSession</code> returned from the delegate in a
     * <code>SibRaConsumerSession</code>.
     * 
     * @param subscriptionName
     *            the name for the subscription
     * @param durableSubscriptionHome
     *            the home for the durable subscription
     * @param destinationAddress
     *            the address of the destination
     * @param criteria
     *            the selection criteria
     * @param reliability
     *            the reliability
     * @param enableReadAhead
     *            flag indicating whether read ahead is enabled
     * @param supportsMultipleConsumers
     *            flag indicating whether multiple consumers are supported
     * @param nolocal
     *            flag indicating whether local messages should be received
     * @param unrecoverableReliability
     *            the unrecoverable reliability
     * @param bifurcatable
     *            whether the new ConsumerSession may be bifurcated
     * @param alternateUser
     *            the name of the user under whose authority operations of the
     *            ConsumerSession should be performed (may be null)
     * @return the consumer session representing the durable subscription
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIDestinationLockedException
     *             if the delegation fails
     * @throws SIDurableSubscriptionMismatchException
     *             if the delegation fails
     * @throws SIDurableSubscriptionNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public ConsumerSession createConsumerSessionForDurableSubscription(
                                                                       final String subscriptionName,
                                                                       final String durableSubscriptionHome,
                                                                       final SIDestinationAddress destinationAddress,
                                                                       final SelectionCriteria criteria,
                                                                       final boolean supportsMultipleConsumers, final boolean nolocal,
                                                                       final Reliability reliability, final boolean enableReadAhead,
                                                                       final Reliability unrecoverableReliability,
                                                                       final boolean bifurcatable, final String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIDurableSubscriptionNotFoundException,
                    SIDurableSubscriptionMismatchException,
                    SIDestinationLockedException, SIResourceException,
                    SIErrorException, SIIncorrectCallException {

        checkValid();

        final ConsumerSession consumerSession = _delegateConnection
                        .createConsumerSessionForDurableSubscription(subscriptionName,
                                                                     durableSubscriptionHome, destinationAddress, criteria,
                                                                     supportsMultipleConsumers, nolocal, reliability,
                                                                     enableReadAhead, unrecoverableReliability,
                                                                     bifurcatable, alternateUser);

        return new SibRaConsumerSession(this, consumerSession);
    }

    /**
     * Deletes a temporary destination. Checks that the connection is valid and
     * then delegates.
     * 
     * @param subscriptionName
     *            the destination to delete
     * @param durableSubscriptionHome
     *            the home for the durable subscription
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIDestinationLockedException
     *             if the delegation fails
     * @throws SIDurableSubscriptionNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public void deleteDurableSubscription(final String subscriptionName,
                                          final String durableSubscriptionHome)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SINotAuthorizedException, SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException, SIResourceException,
                    SIErrorException, SIIncorrectCallException {

        checkValid();
        _delegateConnection.deleteDurableSubscription(subscriptionName,
                                                      durableSubscriptionHome);

    }

    /**
     * Receives a message. Checks that the connection is valid. Maps the
     * transaction parameter before delegating.
     * 
     * @param tran
     *            the transaction to receive the message under
     * @param unrecoverableReliability
     *            the unrecoverable reliability
     * @param destinationAddress
     *            the destination to receive the message from
     * @param destType
     *            the type for the destination
     * @param criteria
     *            the selection criteria
     * @param reliability
     *            the reliability
     * @param alternateUser
     *            the name of the user under whose authority the receive should
     *            be performed (may be null)
     * @return the message or <code>null</code> if none was available
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the transaction parameter is not valid given the current
     *             application and container transactions
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the current container transaction cannot be determined
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SIDestinationLockedException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public SIBusMessage receiveNoWait(final SITransaction tran,
                                      final Reliability unrecoverableReliability,
                                      final SIDestinationAddress destinationAddress,
                                      final DestinationType destType, final SelectionCriteria criteria,
                                      final Reliability reliability, final String alternateUser)
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        return _delegateConnection.receiveNoWait(mapTransaction(tran),
                                                 unrecoverableReliability, destinationAddress, destType,
                                                 criteria, reliability, alternateUser);

    }

    /**
     * Receives a message. Checks that the connection is valid. Maps the
     * transaction parameter before delegating.
     * 
     * @param tran
     *            the transaction to receive the message under
     * @param unrecoverableReliability
     *            the unrecoverable reliability
     * @param destinationAddress
     *            the destination to receive the message from
     * @param destType
     *            the type for the destination
     * @param criteria
     *            the selection criteria
     * @param reliability
     *            the reliability
     * @param timeout
     *            the wait timeout
     * @param alternateUser
     *            the name of the user under whose authority the receive should
     *            be performed (may be null)
     * @return the message or <code>null</code> if none was available
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the transaction parameter is not valid given the current
     *             application and container transactions
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the current container transaction cannot be determined
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SIDestinationLockedException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public SIBusMessage receiveWithWait(final SITransaction tran,
                                        final Reliability unrecoverableReliability,
                                        final SIDestinationAddress destinationAddress,
                                        final DestinationType destType, final SelectionCriteria criteria,
                                        final Reliability reliability, final long timeout,
                                        final String alternateUser) throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        return _delegateConnection.receiveWithWait(mapTransaction(tran),
                                                   unrecoverableReliability, destinationAddress, destType,
                                                   criteria, reliability, timeout, alternateUser);

    }

    /**
     * Creates a browser session. Checks that the connection is valid and then
     * delegates. Wraps the <code>BrowserSession</code> returned from the
     * delegate in a <code>SibRaBrowserSession</code>.
     * 
     * @param destinationAddress
     *            the address of the destination
     * @param destType
     *            the destination type
     * @param criteria
     *            the selection criteria
     * @param alternateUser
     *            the name of the user under whose authority operations of the
     *            BrowserSession should be performed (may be null)
     * @return the browser session
     * @throws SINotPossibleInCurrentConfigurationException
     *             if the delegation fails
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SITemporaryDestinationNotFoundException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public BrowserSession createBrowserSession(
                                               final SIDestinationAddress destinationAddress,
                                               final DestinationType destType, final SelectionCriteria criteria,
                                               final String alternateUser) throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SITemporaryDestinationNotFoundException, SIResourceException,
                    SIErrorException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        checkValid();

        final BrowserSession session = _delegateConnection
                        .createBrowserSession(destinationAddress, destType, criteria,
                                              alternateUser);

        return new SibRaBrowserSession(this, session);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createBrowserSession(com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria, java.lang.String, boolean)
     */
    @Override
    public BrowserSession createBrowserSession(
                                               SIDestinationAddress destinationAddress,
                                               DestinationType destType,
                                               SelectionCriteria criteria,
                                               String alternateUser,
                                               boolean allowMessageGathering)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        checkValid();

        final BrowserSession session = _delegateConnection
                        .createBrowserSession(destinationAddress, destType, criteria,
                                              alternateUser, allowMessageGathering);

        return new SibRaBrowserSession(this, session);
    }

    /**
     * Creates a new OrderingContext, that may be used to ensure messages
     * ordering across multiple ProducerSessions or multiple
     * ConsumerSessions.Checks that the connection is valid and then delegates.
     * 
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public OrderingContext createOrderingContext()
                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIErrorException {

        checkValid();

        return _delegateConnection.createOrderingContext();

    }

    /**
     * Returns the userid associated with this connection. Checks that the
     * connection is valid and then delegates.
     * 
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    @Override
    public String getResolvedUserid() throws SIConnectionDroppedException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SIResourceException, SIErrorException {

        checkValid();

        return _delegateConnection.getResolvedUserid();

    }

    /**
     * Performs the following checks to see if a messaging operation can be
     * avoided by the calling application
     * 
     * Check that the request destination is sendAllowed
     * Check that the reply destination is receiveAllowed
     * Check that neither destination (or any destination on an administered
     * routing path associated with either destination) is mediated
     * Check that the reply destination does not have an associated forward
     * routing path
     * Check that neither destination (or any destination on an administered
     * routing path associated with either destination) is on a different bus
     * to the connected Messaging Engine
     * Check that the resolved destination for the request and the reply
     * destination both have queue points on the same Messaging Engine as the
     * one that the SICoreConnection is connected too
     * Check that the request destination matches the DestinationType
     * supplied
     * Check that the resolved request destination is of type Port
     * Check that no loop exist in any forward routing path associated with
     * the request destination
     * Check that the user has authority to send to each destination (and any
     * destination on an administered routing path associated with either
     * destination)
     * Check that the user has authority to receive from the reply destination
     * 
     * @param requestDestAddr Destination the request message would be sent to
     * @param replyDestAddr Destination the reply message would be sent to
     * @param destinationType Type of destination required for the request
     *            destination
     * @param alternateUser User that access checks are performed against
     *            (if null the user associated with the connection
     *            is used)
     * 
     * @returns null if messaging is required. The SIDestinationAddress of
     *          the resolved request destination if messaging is not required
     * 
     * @throws SIConnectionDroppedException,
     * @throws SIConnectionUnavailableException,
     * @throws SIErrorException,
     * @throws SIIncorrectCallException,
     * @throws SITemporaryDestinationNotFoundException,
     * @throws SIResourceException,
     * @throws SINotAuthorizedException,
     * @throws SINotPossibleInCurrentConfigurationException;
     */
    @Override
    public SIDestinationAddress checkMessagingRequired(SIDestinationAddress requestDestAddr,
                                                       SIDestinationAddress replyDestAddr,
                                                       DestinationType destinationType,
                                                       String alternateUser)

                    throws SIConnectionDroppedException,
                    SIConnectionUnavailableException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SIResourceException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "checkMessagingRequired");
        }

        SIDestinationAddress retVal = _delegateConnection.checkMessagingRequired(requestDestAddr,
                                                                                 replyDestAddr,
                                                                                 destinationType,
                                                                                 alternateUser);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "checkMessagingRequired", retVal);
        }

        return retVal;
    }

    /**
     * This method is used to create a BifurcatedConsumerSession object, which
     * is an additional session representing an existing consumer. Checks that
     * the connection is valid and then delegates.
     * 
     * @param id
     *            the identifier of the consumer
     * @throws SIIncorrectCallException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the delegation fails
     * @throws SISessionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SISessionDroppedException
     *             if the delegation fails
     */
    @Override
    public BifurcatedConsumerSession createBifurcatedConsumerSession(
                                                                     final long id) throws SISessionDroppedException,
                    SIConnectionDroppedException, SISessionUnavailableException,
                    SIConnectionUnavailableException, SIConnectionLostException,
                    SILimitExceededException, SINotAuthorizedException,
                    SIResourceException, SIErrorException, SIIncorrectCallException {

        checkValid();

        final BifurcatedConsumerSession session = _delegateConnection
                        .createBifurcatedConsumerSession(id);

        return new SibRaBifurcatedConsumerSession(this, session);

    }

    /**
     * Returns a string representation of this object.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {

        final StringBuffer buffer = SibRaUtils.startToString(this);
        SibRaUtils.addFieldToString(buffer, "managedConnection",
                                    _managedConnection);
        SibRaUtils.addFieldToString(buffer, "requestInfo", _requestInfo);
        SibRaUtils.addFieldToString(buffer, "delegateConnection",
                                    _delegateConnection);
        SibRaUtils.addFieldToString(buffer, "valid", _valid);
        SibRaUtils.addFieldToString(buffer, "applicationLocalTransaction",
                                    _applicationLocalTransaction);
        SibRaUtils.addFieldToString(buffer, "connectionFactory",
                                    _connectionFactory);
        SibRaUtils.endToString(buffer);

        return buffer.toString();
    }

    /**
     * Returns the managed connection currently associated with this connection.
     * May be <code>null</code> if the connection is not currently associated.
     * 
     * @return the managed connection
     */
    SibRaManagedConnection getManagedConnection() {

        return _managedConnection;

    }

    /**
     * Sets the managed connection currently associated with this connection.
     * May be <code>null</code> to indicate dissociation from the current
     * managed connection.
     * 
     * @param managedConnection
     *            the new managed connection
     */
    void setManagedConnection(final SibRaManagedConnection managedConnection) {

        _managedConnection = managedConnection;

    }

    /**
     * Returns the managed connection associated with this connection. If the
     * connection is not currently associated then calls the connection manager
     * to be re-associated.
     * 
     * @return the managed connection
     * @throws SIResourceException
     *             if the re-association fails
     */
    private SibRaManagedConnection getAssociatedManagedConnection()
                    throws SIResourceException {

        try {

            if (_managedConnection == null) {

                final ConnectionManager connectionManager = _connectionFactory
                                .getConnectionManager();
                if (connectionManager instanceof LazyAssociatableConnectionManager) {

                    ((LazyAssociatableConnectionManager) connectionManager)
                                    .associateConnection(this, _connectionFactory
                                                    .getManagedConnectionFactory(),
                                                         _requestInfo);

                } else {

                    // We shouldn't have been dissociated if the connection
                    // manager does not support lazy enlistment
                    final ResourceException exception = new ResourceAdapterInternalException(
                                    NLS.getFormattedMessage(
                                                            "LAZY_ENLIST_NOT_SUPPORTED_CWSIV0154",
                                                            new Object[] { connectionManager }, null));
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                        SibTr.exception(this, TRACE, exception);
                    }
                    throw exception;

                }

            }

        } catch (ResourceException exception) {

            FFDCFilter
                            .processException(
                                              exception,
                                              "com.ibm.ws.sib.ra.impl.SibRaConnection.getAssociatedManagedConnection",
                                              "1:1843:1.41", this);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new SIResourceException(NLS.getFormattedMessage(
                                                                  "REASSOCIATION_FAILED_CWSIV0155",
                                                                  new Object[] { exception }, null), exception);

        }

        return _managedConnection;

    }

    /**
     * Called by the managed connection during <code>cleanup</code>. In
     * normal processing the connection handle is closed before the managed
     * connection is cleaned up prior to going back in the pool. The calling of
     * this method therefore indicates an error scenario.
     */
    void invalidate() {

        try {

            // Attempt to close delegate connection
            _delegateConnection.close();

        } catch (SIException exception) {

            FFDCFilter.processException(exception,
                                        "com.ibm.ws.sib.ra.impl.SibRaConnection.invalidate",
                                        "1:1875:1.41", this);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
        } catch (SIErrorException exception) {

            FFDCFilter.processException(exception,
                                        "com.ibm.ws.sib.ra.impl.SibRaConnection.invalidate",
                                        "1:1884:1.41", this);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
        }

        _valid = false;

    }

    /**
     * Returns <code>true</code> if this connection has not been invalidated.
     * 
     * @return flag indicating whether this connection is still valid
     */
    boolean isValid() {

        return _valid;

    }

    /**
     * Checks that this connection is still valid.
     * 
     * @throws SIConnectionUnavailableException
     *             if the connection is not valid
     */
    private void checkValid() throws SIConnectionUnavailableException {

        if (!_valid) {

            throw new SIConnectionUnavailableException(NLS
                            .getString("INVALID_CONNECTION_CWSIV0156"));

        }

    }

    /**
     * Returns the current container transaction, if any.
     * 
     * @return the current container transaction
     * @throws SIResourceException
     *             if lazy association or lazy enlistment fails
     */
    private SITransaction getContainerTransaction() throws SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "getContainerTransaction");
        }

        final SITransaction containerTransaction;

        try {

            // Ensure the connection is associate with a managed connection
            final SibRaManagedConnection managedConnection = getAssociatedManagedConnection();

            // Obtain the current container transaction from the managed
            // connection. The connection manager is required to perform lazy
            // enlistment.
            containerTransaction = managedConnection
                            .getContainerTransaction(_connectionFactory
                                            .getConnectionManager());

        } catch (ResourceException exception) {
            // No FFDC code needed

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new SIResourceException(NLS.getFormattedMessage(
                                                                  "CONTAINER_TRAN_CWSIV0157", new Object[] { exception },
                                                                  null), exception);

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "getContainerTransaction",
                       containerTransaction);
        }
        return containerTransaction;

    }

    /**
     * Maps the transaction parameter passed by the caller to the transaction
     * parameter that should be passed on to the delegate based on the current
     * transaction. Implements the following table:
     * 
     * <table>
     * <tr>
     * <th rowspan="2">Transaction parameter</th>
     * <th colspan="4">Current transaction</th>
     * </tr>
     * <tr>
     * <th>None</th>
     * <th>Application local</th>
     * <th>Container local</th>
     * <th>Global</th>
     * </tr>
     * <tr>
     * <th>Null</th>
     * <td>Auto-commit</td>
     * <td>Exception</td>
     * <td>Container local</td>
     * <td>Global</td>
     * </tr>
     * <tr>
     * <th>SIUncoordinatedTransaction</th>
     * <td>Exception</td>
     * <td>Application local</td>
     * <td>Exception</td>
     * <td>Exception</td>
     * </tr>
     * <tr>
     * <th>SIXAResource</th>
     * <td>Exception</td>
     * <td>Exception</td>
     * <td>Exception</td>
     * <td>Exception</td>
     * </tr>
     * <tr>
     * <th>SIAutoCommitTransaction</th>
     * <td>Auto-commit</td>
     * <td>Auto-commit</td>
     * <td>Auto-commit</td>
     * <td>Auto-commit</td>
     * </tr>
     * </table>
     * 
     * @param transactionParameter
     *            the transaction paramter passed by the caller
     * @return the <code>SITransaction</code> to pass to the core SPI
     * @throws SIIncorrectCallException
     *             if the transaction parameter is not valid
     * @throws SIResourceException
     *             if an attempt to obtain the container transaction fails
     */
    SITransaction mapTransaction(final SITransaction transactionParameter)
                    throws SIIncorrectCallException, SIResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "mapTransaction", transactionParameter);
        }

        final SITransaction transaction;

        if (transactionParameter == null) {

            // Not valid if we have an active local transaction
            if (_applicationLocalTransaction != null) {

                final SIIncorrectCallException exception = new SIIncorrectCallException(
                                NLS.getString("ACTIVE_LOCAL_TRAN_CWSIV0158"));
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw exception;

            }

            // Use the current container transaction, if any
            transaction = getContainerTransaction();

        } else if (transactionParameter instanceof SIUncoordinatedTransaction) {

            // Must be the current local transaction
            if (transactionParameter != _applicationLocalTransaction) {

                final SIIncorrectCallException exception = new SIIncorrectCallException(
                                NLS.getString("INCORRECT_LOCAL_TRAN_CWSIV0159"));
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw exception;

            }

            // Get the real core SPI transaction
            transaction = _applicationLocalTransaction
                            .getUncoordinatedTransaction();

        } else if (transactionParameter instanceof SIXAResource) {

            // SIXAResource not permitted (and they didn't get it from here in
            // the first place!)
            final SIIncorrectCallException exception = new SIIncorrectCallException(
                            NLS.getString("INVALID_XARESOURCE_CWSIV0160"));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        } else if (transactionParameter instanceof SibRaAutoCommitTransaction) {

            // Pass null to get auto-commit behaviour
            transaction = null;

        } else {

            // They've found some other sort of SITransaction that we don't
            // know about!
            final SIIncorrectCallException exception = new SIIncorrectCallException(
                            NLS.getFormattedMessage("UNRECOGNISED_TRAN_CWSIV0161",
                                                    new Object[] { transactionParameter }, null));
            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw exception;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "mapTransaction", transaction);
        }
        return transaction;

    }

    /**
     * Sets the parent connection factory. Called by the connection factory
     * prior to returning the connection to the caller. The connection factory
     * is needed in order to obtain the connection manager and managed
     * connection factory in order to perform lazy enlistment and
     * re-association.
     * 
     * @param connectionFactory
     *            the connection factory
     */
    void setConnectionFactory(final SibRaConnectionFactory connectionFactory) {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "setConnectionFactory", connectionFactory);
        }

        _connectionFactory = connectionFactory;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "setConnectionFactory");
        }

    }

    /**
     * Commits any currently active application local transaction. Called by the
     * managed connection's <code>LocalTransaction</code> implementation when
     * a <code>commit</code> occurs without a corresponding <code>begin</code>.
     * This indicates that the container is attempting a cleanup of outstanding
     * application local transaction work. The container shouldn't do this
     * unless it has received a <code>LOCAL_TRANSACTION_STARTED</code> event
     * an no corresponding completion event. If this is the case then we <b>
     * should </b> have an active application local transaction but, on the off
     * chance that we don't, do nothing.
     * 
     * @throws ResourceException
     *             if the commit fails
     */
    void commitApplicationLocalTransaction() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "commitApplicationLocalTransaction");
        }

        if (_applicationLocalTransaction != null) {

            final SIUncoordinatedTransaction uncoordinatedTransaction = _applicationLocalTransaction
                            .getUncoordinatedTransaction();

            try {

                uncoordinatedTransaction.commit();

            } catch (SIException exception) {
                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CONTAINER_COMMIT_FAILED_CWSIV0162",
                                                                    new Object[] { exception }, null), exception);

            } catch (SIErrorException exception) {
                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CONTAINER_COMMIT_FAILED_CWSIV0162",
                                                                    new Object[] { exception }, null), exception);

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "commitApplicationLocalTransaction");
        }

    }

    /**
     * Rolls back any currently active application local transaction. Called by
     * the managed connection's <code>LocalTransaction</code> implementation
     * when a <code>rollback</code> occurs without a corresponding
     * <code>begin</code>. This indicates that the container is attempting a
     * cleanup of outstanding application local transaction work. The container
     * shouldn't do this unless it has received a
     * <code>LOCAL_TRANSACTION_STARTED</code> event an no corresponding
     * completion event. If this is the case then we <b>should </b> have an
     * active application local transaction but, on the off chance that we
     * don't, do nothing.
     * 
     * @throws ResourceException
     *             if the rollback fails
     */
    void rollbackApplicationLocalTransaction() throws ResourceException {

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "rollbackApplicationLocalTransaction");
        }

        if (_applicationLocalTransaction != null) {

            SIUncoordinatedTransaction uncoordinatedTransaction = _applicationLocalTransaction
                            .getUncoordinatedTransaction();

            try {

                uncoordinatedTransaction.rollback();

            } catch (SIException exception) {
                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CONTAINER_ROLLBACK_FAILED_CWSIV0163",
                                                                    new Object[] { exception }, null), exception);

            } catch (SIErrorException exception) {
                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceException(NLS.getFormattedMessage(
                                                                    "CONTAINER_ROLLBACK_FAILED_CWSIV0163",
                                                                    new Object[] { exception }, null), exception);

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "rollbackApplicationLocalTransaction");
        }

    }

    /**
     * is the coreConnection invalid
     * 
     * @return
     */
    public boolean isCoreConnectionInValid()
    {
        return (_delegateConnection == null);
    }

    /**
     * Implementation of <code>SIUncoordinatedTransaction</code> for the core
     * SPI resource adapter. Wraps a delegate
     * <code>SIUncoordinatedTransaction</code> in order to intercept
     * completion calls so that the parent connectino can track the currently
     * active application local transaction.
     */
    private final class SibRaUncoordinatedTransaction implements
                    SIUncoordinatedTransaction {

        /**
         * The real <code>SIUncoordinatedTransaction</code> to which methods
         * delegate.
         */
        private final SIUncoordinatedTransaction _delegateUncoordinatedTransaction;

        /**
         * Constructor.
         * 
         * @param delegate
         *            the real <code>SIUncoordinatedTransaction</code> to
         *            which methods delegate
         * @throws SIResourceException
         *             if re-association is required in order to notify the
         *             managed connection and fails
         */
        private SibRaUncoordinatedTransaction(
                                              SIUncoordinatedTransaction delegate) throws SIResourceException {

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.entry(this, TRACE, "SibRaUncoordinatedTransaction",
                            delegate);
            }

            _delegateUncoordinatedTransaction = delegate;

            // Notify the managed connection that the transaction has started
            getAssociatedManagedConnection().localTransactionStarted(
                                                                     SibRaConnection.this);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, "SibRaUncoordinatedTransaction");
            }

        }

        /**
         * Returns the real <code>SIUncoordinatedTransaction</code>.
         * 
         * @return the real <code>SIUncoordinatedTransaction</code>
         */
        private SIUncoordinatedTransaction getUncoordinatedTransaction() {

            return _delegateUncoordinatedTransaction;

        }

        /**
         * Commits the transaction. Delegates and then notifies the managed
         * connection.
         * 
         * @throws SIErrorException
         *             if the delegation fails
         * @throws SIResourceException
         *             if re-association is required in order to notify the
         *             managed connection and fails
         * @throws SIIncorrectCallException
         *             if the delegation fails
         * @throws SIConnectionLostException
         *             if the delegation fails
         * @throws SIRollbackException
         *             if the delegation fails
         */
        @Override
        public void commit() throws SIRollbackException,
                        SIConnectionLostException, SIIncorrectCallException,
                        SIResourceException, SIErrorException {

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.entry(this, TRACE, "commit");
            }

            _delegateUncoordinatedTransaction.commit();

            // Set the connection's active application local transaction to
            // null
            _applicationLocalTransaction = null;

            // Notify the managed connection that the transaction has been
            // committed
            getAssociatedManagedConnection().localTransactionCommitted(
                                                                       SibRaConnection.this);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, "commit");
            }

        }

        /**
         * Rolls back the transaction. Delegates and then notifies the managed
         * connection.
         * 
         * @throws SIErrorException
         *             if the delegation fails
         * @throws SIResourceException
         *             if re-association is required in order to notify the
         *             managed connection and fails
         * @throws SIIncorrectCallException
         *             if the delegation fails
         * @throws SIConnectionLostException
         *             if the delegation fails
         */
        @Override
        public void rollback() throws SIConnectionLostException,
                        SIIncorrectCallException, SIResourceException, SIErrorException {

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.entry(this, TRACE, "rollback");
            }

            _delegateUncoordinatedTransaction.rollback();

            // Set the connection's active application local transaction to
            // null
            _applicationLocalTransaction = null;

            // Notify the managed connection that the transaction has been
            // committed
            getAssociatedManagedConnection().localTransactionRolledBack(
                                                                        SibRaConnection.this);

            if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                SibTr.exit(this, TRACE, "rollback");
            }

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#addDestinationListener(java.lang.String, com.ibm.wsspi.sib.core.DestinationListener, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.DestinationAvailability)
     */
    @Override
    public SIDestinationAddress[] addDestinationListener(String destinationNamePattern, DestinationListener destinationListener, DestinationType destinationType,
                                                         DestinationAvailability destinationAvailability)
                    throws SIIncorrectCallException, SICommandInvocationFailedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIConnectionLostException
    {
        return _delegateConnection.addDestinationListener(destinationNamePattern, destinationListener, destinationType, destinationAvailability);
    }

    @Override
    public boolean registerConsumerSetMonitor(
                                              SIDestinationAddress destinationAddress,
                                              String discriminatorExpression, ConsumerSetChangeCallback callback)
                    throws SIResourceException,
                    SINotPossibleInCurrentConfigurationException,
                    SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIIncorrectCallException, SICommandInvocationFailedException {
        return _delegateConnection.registerConsumerSetMonitor(destinationAddress, discriminatorExpression, callback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createSharedConsumerSession(java.lang.String, com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType, com.ibm.wsspi.sib.core.SelectionCriteria, com.ibm.websphere.sib.Reliability, boolean, boolean, boolean,
     * com.ibm.websphere.sib.Reliability, boolean, java.lang.String, boolean, boolean, java.util.Map)
     */
    @Override
    public ConsumerSession createSharedConsumerSession(String subscriptionName, SIDestinationAddress destAddr, DestinationType destType, SelectionCriteria criteria,
                                                       Reliability reliability, boolean enableReadAhead, boolean supportsMultipleConsumers, boolean nolocal,
                                                       Reliability unrecoverableReliability, boolean bifurcatable, String alternateUser, boolean ignoreInitialIndoubts,
                                                       boolean allowMessageGathering, Map<String, String> messageControlProperties) throws SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SILimitExceededException, SINotAuthorizedException, SIIncorrectCallException, SIDestinationLockedException, SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException {
        //Venu TODO JMS2.0
        return _delegateConnection.createSharedConsumerSession(subscriptionName, destAddr, destType, criteria, reliability, enableReadAhead, supportsMultipleConsumers, nolocal,
                                                               unrecoverableReliability, bifurcatable, alternateUser, ignoreInitialIndoubts, allowMessageGathering,
                                                               messageControlProperties);
    }

}
