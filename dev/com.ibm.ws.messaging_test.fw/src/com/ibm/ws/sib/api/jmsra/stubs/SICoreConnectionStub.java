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

package com.ibm.ws.sib.api.jmsra.stubs;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.transaction.Synchronization;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.wsspi.sib.core.DestinationAvailability;
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
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * Stub class for SICoreConnection.
 */
public class SICoreConnectionStub implements SICoreConnection, Cloneable {

    private Subject _subject = null;

    private String _userName = null;

    private String _password = null;

    private Map _properties = null;

    private final Set _listeners = new HashSet();

    private boolean _remote = false;

    private static Set _connections = new HashSet();

    /**
     * Identifier shared by cloned connections and used to implement
     * equivalence.
     */
    private final int _id;

    /** Identifier used for next connection. */
    private static int _nextId = 0;

    /**
     * Constructor.
     * 
     * @param subject
     *            the subject associated with this connection
     * @param props
     *            the properties passed on creation of this connection
     */
    public SICoreConnectionStub(Subject subject, Map props) {
        this._subject = subject;
        this._properties = props;
        this._id = _nextId++;
        _connections.add(this);
    }

    /**
     * Constructor.
     * 
     * @param user
     *            the user name associated with this connection
     * @param password
     *            the password associated with this connection
     * @param props
     *            the properties passed on creation of this connection
     */
    public SICoreConnectionStub(String user, String password, Map props) {
        this._userName = user;
        this._password = password;
        this._properties = props;
        this._id = _nextId++;
        _connections.add(this);
    }

    /**
     * Constructor.
     * 
     * @param subject
     *            the subject associated with this connection
     * @param props
     *            the properties passed on creation of this connection
     * @param remote
     *            flag indicating whether the connection is remote
     */
    public SICoreConnectionStub(Subject subject, Map props, boolean remote) {
        this._subject = subject;
        this._properties = props;
        this._id = _nextId++;
        this._remote = remote;
        _connections.add(this);
    }

    /**
     * Constructor.
     * 
     * @param user
     *            the user name associated with this connection
     * @param password
     *            the password associated with this connection
     * @param props
     *            the properties passed on creation of this connection
     * @param remote
     *            flag indicating whether the connection is remote
     */
    public SICoreConnectionStub(String user, String password, Map props,
                                boolean remote) {
        this._userName = user;
        this._password = password;
        this._properties = props;
        this._id = _nextId++;
        this._remote = remote;
        _connections.add(this);
    }

    /**
     * Constructor used when cloning a connection.
     * 
     * @param userName
     *            the user name
     * @param password
     *            the password
     * @param subject
     *            the subject
     * @param properties
     *            the properties
     * @param id
     *            the identifier
     */
    private SICoreConnectionStub(String userName, String password,
                                 Subject subject, Map properties, int id) {
        this._userName = userName;
        this._password = password;
        this._subject = subject;
        this._properties = properties;
        this._id = id;
        _connections.add(this);
    }

    @Override
    public void close() {
        _connections.remove(this);
    }

    @Override
    public void close(boolean bForceFlag) {
        close();
    }

    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction() {
        SIUncoordinatedTransaction uncoordinatedTransaction;
        if (_remote) {
            uncoordinatedTransaction = new RemoteSIUncoordinatedTransaction();
        } else {
            uncoordinatedTransaction = new LocalSIUncoordinatedTransaction();
        }
        return uncoordinatedTransaction;
    }

    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction(boolean allowSubs) {
        SIUncoordinatedTransaction uncoordinatedTransaction;
        if (_remote) {
            uncoordinatedTransaction = new RemoteSIUncoordinatedTransaction();
        } else {
            uncoordinatedTransaction = new LocalSIUncoordinatedTransaction();
        }
        return uncoordinatedTransaction;
    }

    @Override
    public SIXAResource getSIXAResource() {
        return new SIXAResourceStub();
    }

    /**
     * Clones this connection to return a connection with the same properties
     * and that will return true when compared with this connection using
     * isEquivalentTo.
     * 
     * @return the clone
     */
    @Override
    public SICoreConnection cloneConnection() {
        return new SICoreConnectionStub(_userName, _password, _subject,
                        _properties, _id);
    }

    /**
     * Compares this connection with the connection passed in.
     * 
     * @param other
     *            the connection to compare with
     * @return <code>true</code> iff the connections are equivalent i.e. one
     *         was created as a clone of the other
     */
    @Override
    public boolean isEquivalentTo(SICoreConnection other) {
        boolean equal = false;

        if (other instanceof SICoreConnectionStub) {
            equal = (getId() == ((SICoreConnectionStub) other).getId());
        }

        return equal;
    }

    @Override
    public String getMeName() {
        return null;
    }

    @Override
    public void addConnectionListener(SICoreConnectionListener listener) {
        _listeners.add(listener);
    }

    @Override
    public void removeConnectionListener(SICoreConnectionListener listener) {
        _listeners.remove(listener);
    }

    @Override
    public SICoreConnectionListener[] getConnectionListeners() {
        return (SICoreConnectionListener[]) _listeners
                        .toArray(new SICoreConnectionListener[] {});
    }

    @Override
    public String getApiLevelDescription() {
        return null;
    }

    /**
     * Returns the password.
     * 
     * @return the password
     */
    public String getPassword() {
        return _password;
    }

    /**
     * Returns the properties.
     * 
     * @return the set of properties
     */
    public Map getProperties() {
        return _properties;
    }

    /**
     * Returns the subject.
     * 
     * @return the subject ubject
     */
    public Subject getSubject() {
        return _subject;
    }

    /**
     * Returns the user name.
     * 
     * @return the user name
     */
    public String getUserName() {
        return _userName;
    }

    /**
     * Returns the id.
     * 
     * @return the id
     */
    public int getId() {
        return _id;
    }

    /**
     * Returns the set of all connection instances.
     * 
     * @return the connections
     */
    public static Set getConnections() {
        return _connections;
    }

    /**
     * Sends a comms failure event to all the connection listeners with the
     * given exception.
     * 
     * @param exc
     */
    public void sendCommsFailure(SIConnectionLostException exc) {
        for (Iterator iterator = _listeners.iterator(); iterator.hasNext();) {
            SICoreConnectionListener listener = (SICoreConnectionListener) iterator
                            .next();
            listener.commsFailure(this, exc);
        }
    }

    /**
     * Sends an ME quiescing event to all the connection listeners.
     */
    public void sendMeQuiescing() {
        for (Iterator iterator = _listeners.iterator(); iterator.hasNext();) {
            SICoreConnectionListener listener = (SICoreConnectionListener) iterator
                            .next();
            listener.meQuiescing(this);
        }
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createUniqueId()
     */
    @Override
    public byte[] createUniqueId() {
        return null;
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getApiMajorVersion()
     */
    @Override
    public long getApiMajorVersion() {
        return 0;
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getApiMinorVersion()
     */
    @Override
    public long getApiMinorVersion() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getMeUuid()
     */
    @Override
    public String getMeUuid() {
        return null;
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createOrderingContext()
     */
    @Override
    public OrderingContext createOrderingContext() {
        return null;
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#deleteDurableSubscription(String, String)
     */
    @Override
    public void deleteDurableSubscription(String arg0, String arg1) {

        // Do nothing

    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#deleteTemporaryDestination(SIDestinationAddress)
     */
    @Override
    public void deleteTemporaryDestination(SIDestinationAddress arg0) {

        // Do nothing

    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getResolvedUserid()
     */
    @Override
    public String getResolvedUserid() {
        return null;
    }

    private class RemoteSIUncoordinatedTransaction implements
                    SIUncoordinatedTransaction {

        @Override
        public void commit() {

            // Do nothing

        }

        @Override
        public void rollback() {

            // Do nothing

        }

    }

    private class LocalSIUncoordinatedTransaction extends
                    RemoteSIUncoordinatedTransaction implements Synchronization {

        @Override
        public void beforeCompletion() {

            // Do nothing

        }

        @Override
        public void afterCompletion(int status) {

            // Do nothing

        }

    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createBifurcatedConsumerSession(long)
     */
    @Override
    public BifurcatedConsumerSession createBifurcatedConsumerSession(long arg0) {
        return null;
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#getDestinationConfiguration(SIDestinationAddress)
     */
    @Override
    public com.ibm.wsspi.sib.core.DestinationConfiguration getDestinationConfiguration(
                                                                                       SIDestinationAddress arg0) {
        return null;
    }

    /**
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createTemporaryDestination(Distribution, String)
     */
    @Override
    public SIDestinationAddress createTemporaryDestination(
                                                           com.ibm.wsspi.sib.core.Distribution arg0, String arg1) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createDurableSubscription(java.lang.String,
     * java.lang.String, com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.SelectionCriteria, boolean, boolean,
     * java.lang.String)
     */
    @Override
    public void createDurableSubscription(String subscriptionName,
                                          String durableSubscriptionHome,
                                          SIDestinationAddress destinationAddress,
                                          SelectionCriteria criteria, boolean supportsMultipleConsumers,
                                          boolean nolocal, String alternateUser)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDurableSubscriptionAlreadyExistsException {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSessionForDurableSubscription(java.lang.String,
     * java.lang.String, com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.SelectionCriteria, boolean, boolean,
     * com.ibm.websphere.sib.Reliability, boolean,
     * com.ibm.websphere.sib.Reliability, boolean, java.lang.String)
     */
    @Override
    public ConsumerSession createConsumerSessionForDurableSubscription(
                                                                       String subscriptionName, String durableSubscriptionHome,
                                                                       SIDestinationAddress destinationAddress,
                                                                       SelectionCriteria criteria, boolean supportsMultipleConsumers,
                                                                       boolean nolocal, Reliability reliability, boolean enableReadAhead,
                                                                       Reliability unrecoverableReliability, boolean bifurcatable,
                                                                       String alternateUser) throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SIDurableSubscriptionNotFoundException,
                    SIDurableSubscriptionMismatchException,
                    SIDestinationLockedException {

        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, java.lang.String)
     */
    @Override
    public ProducerSession createProducerSession(SIDestinationAddress destAddr,
                                                 DestinationType destType,
                                                 OrderingContext extendedMessageOrderingContext, String alternateUser)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SITemporaryDestinationNotFoundException, SIIncorrectCallException {

        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(com.ibm.websphere.sib.SIDestinationAddress,
     * java.lang.String, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, java.lang.String)
     */
    @Override
    public ProducerSession createProducerSession(SIDestinationAddress destAddr,
                                                 String discriminator, DestinationType destType,
                                                 OrderingContext extendedMessageOrderingContext, String alternateUser)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SITemporaryDestinationNotFoundException, SIIncorrectCallException,
                    SIDiscriminatorSyntaxException {

        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createProducerSession(
     * com.ibm.websphere.sib.SIDestinationAddress, java.lang.String,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext,
     * java.lang.String, boolean, boolean)
     */
    @Override
    public ProducerSession createProducerSession(
                                                 SIDestinationAddress destAddr, String discriminator,
                                                 DestinationType destType, OrderingContext extendedMessageOrderingContext,
                                                 String alternateUser, boolean fixedMessagePoint,
                                                 boolean preferLocalMessagePoint)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SINotPossibleInCurrentConfigurationException,
                    SITemporaryDestinationNotFoundException, SIIncorrectCallException,
                    SIDiscriminatorSyntaxException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSession(com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria,
     * com.ibm.websphere.sib.Reliability, boolean, boolean,
     * com.ibm.websphere.sib.Reliability, boolean, java.lang.String)
     */
    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddr,
                                                 DestinationType destType, SelectionCriteria criteria,
                                                 Reliability reliability, boolean enableReadAhead, boolean nolocal,
                                                 Reliability unrecoverableReliability, boolean bifurcatable,
                                                 String alternateUser) throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException {

        return new ConsumerSessionStub();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createConsumerSession(
     * com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria,
     * com.ibm.websphere.sib.Reliability,
     * boolean, boolean, com.ibm.websphere.sib.Reliability,
     * boolean, java.lang.String, boolean, boolean)
     */
    @Override
    public ConsumerSession createConsumerSession(
                                                 SIDestinationAddress destAddr,
                                                 DestinationType destType, SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead, boolean nolocal,
                                                 Reliability unrecoverableReliability, boolean bifurcatable,
                                                 String alternateUser, boolean ignoreInitialIndoubts,
                                                 boolean allowMessageGathering,
                                                 Map<String, String> messageControlProperties)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SIDestinationLockedException, SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#send(com.ibm.wsspi.sib.core.SIBusMessage,
     * com.ibm.wsspi.sib.core.SITransaction,
     * com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.OrderingContext, java.lang.String)
     */
    @Override
    public void send(SIBusMessage msg, SITransaction tran,
                     SIDestinationAddress destAddr, DestinationType destType,
                     OrderingContext extendedMessageOrderingContext, String alternateUser)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#receiveNoWait(com.ibm.wsspi.sib.core.SITransaction,
     * com.ibm.websphere.sib.Reliability,
     * com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria,
     * com.ibm.websphere.sib.Reliability, java.lang.String)
     */
    @Override
    public SIBusMessage receiveNoWait(SITransaction tran,
                                      Reliability unrecoverableReliability,
                                      SIDestinationAddress destAddr, DestinationType destType,
                                      SelectionCriteria criteria, Reliability reliability,
                                      String alternateUser) throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException {

        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#receiveWithWait(com.ibm.wsspi.sib.core.SITransaction,
     * com.ibm.websphere.sib.Reliability,
     * com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria,
     * com.ibm.websphere.sib.Reliability, long, java.lang.String)
     */
    @Override
    public SIBusMessage receiveWithWait(SITransaction tran,
                                        Reliability unrecoverableReliability,
                                        SIDestinationAddress destAddr, DestinationType destType,
                                        SelectionCriteria criteria, Reliability reliability, long timeout,
                                        String alternateUser) throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException {

        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createBrowserSession(com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria, java.lang.String)
     */
    @Override
    public BrowserSession createBrowserSession(
                                               SIDestinationAddress destinationAddress, DestinationType destType,
                                               SelectionCriteria criteria, String alternateUser)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException {

        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#createBrowserSession(
     * com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.SelectionCriteria,
     * java.lang.String, boolean)
     */
    @Override
    public BrowserSession createBrowserSession(
                                               SIDestinationAddress destinationAddress,
                                               DestinationType destType, SelectionCriteria criteria,
                                               String alternateUser, boolean allowMessageGathering)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#sendToExceptionDestination(com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.wsspi.sib.core.SIBusMessage, int, java.lang.String[],
     * com.ibm.wsspi.sib.core.SITransaction, java.lang.String)
     */
    @Override
    public void sendToExceptionDestination(SIDestinationAddress address,
                                           SIBusMessage message, int reason, String[] inserts,
                                           SITransaction tran, String alternateUser)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException, SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#checkMessagingRequired(com.ibm.websphere.sib.SIDestinationAddress,
     * com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
     * java.lang.String)
     */
    @Override
    public SIDestinationAddress checkMessagingRequired(
                                                       SIDestinationAddress requestDestAddr,
                                                       SIDestinationAddress replyDestAddr,
                                                       DestinationType destinationType, String alternateUser)

    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#invokeCommand(java.lang.String, java.lang.String, java.io.Serializable)
     */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData)
                    throws SIConnectionDroppedException, SIConnectionUnavailableException, SIResourceException,
                    SIIncorrectCallException, SINotAuthorizedException, SICommandInvocationFailedException {

        return null;

    }

    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddr,
                                                 DestinationType destType, SelectionCriteria criteria, Reliability reliability,
                                                 boolean enableReadAhead, boolean nolocal, Reliability unrecoverableReliability,
                                                 boolean bifurcatable, String alternateUser, boolean ignoreInitialIndoubts)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException,
                    SIConnectionLostException, SILimitExceededException, SINotAuthorizedException,
                    SIIncorrectCallException, SIDestinationLockedException, SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        return new ConsumerSessionStub();
    }

/*
 * (non-Javadoc)
 * 
 * @see com.ibm.wsspi.sib.core.SICoreConnection#invokeCommand(java.lang.String, java.lang.String, java.io.Serializable, com.ibm.wsspi.sib.core.SITransaction)
 */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData, SITransaction transaction) throws SIConnectionDroppedException, SIConnectionUnavailableException, SIResourceException, SIIncorrectCallException, SINotAuthorizedException, SICommandInvocationFailedException
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.SICoreConnection#addDestinationListener(java.lang.String, com.ibm.wsspi.sib.core.DestinationListener, com.ibm.wsspi.sib.core.DestinationType,
     * com.ibm.wsspi.sib.core.DestinationAvailability)
     */
    @Override
    public SIDestinationAddress[] addDestinationListener(String destinationNamePattern, DestinationListener destinationListener, DestinationType destinationType,
                                                         DestinationAvailability destinationAvailability) throws SIIncorrectCallException, SICommandInvocationFailedException, SIConnectionUnavailableException
    {
        return null;
    }

    @Override
    public boolean registerConsumerSetMonitor(
                                              SIDestinationAddress destinationAddress,
                                              String discriminatorExpression, ConsumerSetChangeCallback callback)
                    throws SIResourceException,
                    SINotPossibleInCurrentConfigurationException,
                    SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIIncorrectCallException, SICommandInvocationFailedException {
        return false;
    }

/*
 * (non-Javadoc)
 * 
 * @see com.ibm.wsspi.sib.core.SICoreConnection#createSharedConsumerSession(java.lang.String, com.ibm.websphere.sib.SIDestinationAddress, com.ibm.wsspi.sib.core.DestinationType,
 * com.ibm.wsspi.sib.core.SelectionCriteria, com.ibm.websphere.sib.Reliability, boolean, boolean, boolean, com.ibm.websphere.sib.Reliability, boolean, java.lang.String, boolean,
 * boolean, java.util.Map)
 */
    @Override
    public ConsumerSession createSharedConsumerSession(String subscriptionName, SIDestinationAddress destAddr, DestinationType destType, SelectionCriteria criteria,
                                                       Reliability reliability, boolean enableReadAhead, boolean supportsMultipleConsumers, boolean nolocal,
                                                       Reliability unrecoverableReliability, boolean bifurcatable, String alternateUser, boolean ignoreInitialIndoubts,
                                                       boolean allowMessageGathering, Map<String, String> messageControlProperties) throws SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SILimitExceededException, SINotAuthorizedException, SIIncorrectCallException, SIDestinationLockedException, SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException {
        // TODO Auto-generated method stub
        return null;
    }

}
