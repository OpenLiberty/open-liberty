/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

//------------------------------------------------------------------------------
//Class: RecoveryDirectorImpl
//------------------------------------------------------------------------------
/**
 * <p>
 * The RecoveryDirector provides support for the registration of those components
 * that need to use use the Recovery Log Service (RLS) to store persistent
 * information.
 * </p>
 *
 * <p>
 * In order to support interaction with the High Availability (HA) framework in
 * the future, the RecoveryDirector acts as a bridge between the registered
 * components (client services) and the controlling logic that determines when
 * recovery processing is needed.
 * </p>
 *
 * <p>
 * Client services obtain a reference to the RecoveryDirector through its factory
 * class, the RecoveryDirectorFactory, by calling its recoveryDirector method.
 * </p>
 *
 * <p>
 * Client services supply a RecoveryAgent callback object when they register
 * that is driven asynchronously by the RLS when recovery processing is required.
 * Upon registration, they are provided with a RecoveryLogManager object through
 * which they interact with the RLS.
 * </p>
 *
 * <p>
 * This class provides the implementation of the RecoveryDirector interface
 * </p>
 */
public class RecoveryDirectorImpl implements RecoveryDirector {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(RecoveryDirectorImpl.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * CallBack method indicators
     */
    public static final int CALLBACK_RECOVERYSTARTED = 1;
    public static final int CALLBACK_RECOVERYCOMPLETE = 2;
    public static final int CALLBACK_TERMINATIONSTARTED = 3;
    public static final int CALLBACK_TERMINATIONCOMPLETE = 4;
    public static final int CALLBACK_RECOVERYFAILED = 5;

    /**
     * A reference to the singleton instance of the RecoveryDirectorImpl class.
     */
    private static RecoveryDirectorImpl _instance;
    private static RecoveryLogFactory theRecoveryLogFactory;

    /**
     * The registered RecoveryAgent objects, keyed from sequence value. RecoveryAgent
     * objects are stored in an ArrayList to allow multiple registrations at the same
     * sequence value.
     *
     * The TreeMap will return elements in ascending key order.
     *
     * <p>
     * <ul>
     * <li>java.util.TreeMap (Integer -> ArrayList (RecoveryAgent))
     * </ul>
     */
    protected final TreeMap<Integer, ArrayList<RecoveryAgent>> _registeredRecoveryAgents;

    /**
     * A record of the recovery initialization that is "in flight" at any given point.
     * When the RLS requests a RecoveryAgent to perform a unit of a recovery, a
     * record of this request is stored in the map. The client service
     * should perfom sufficient processing to get recovery underway and allow other
     * services to be passed the RecoveryAgent.initiateRecovery call and then invoke
     * the RecoveryDirector.serialRecoveryComplete(RecoveryAgent,FailureScope) method. At this
     * point the record will be removed from this map.
     *
     * Entry is added to the map at the same time as an entry is added to
     * _outstandingRecoveryRecords but its removed at serial completion rather than initial
     * completion (ie earlier).
     *
     * <p>
     * <ul>
     * <li>java.util.HashMap (RecoveryAgent -> HashSet (FailureScope))
     * </ul>
     */
    private final HashMap<RecoveryAgent, HashSet<FailureScope>> _outstandingInitializationRecords;

    /**
     * A record of the recovery work that is "in flight" at any given point. When the
     * RLS requests a RecoveryAgent to perform a unit of a recovery, a record of this request
     * is stored in the map. The client service should complete recovery processing to the
     * extent that is can allow a peer recovery process to take place and then invoke the
     * RecoveryDirector.initialRecoveryComplete(RecoveryAgent,FailureScope) method. At this
     * point the record will be removed from this map.
     *
     * Entry is added to the map at the same time as an entry is added to
     * _outstandingInitializationRecords but its removed at initial completion rather than serial
     * completion (ie later).
     *
     * <p>
     * <ul>
     * <li>java.util.HashMap (FailureScope -> HashSet (RecoveryAgent))
     * </ul>
     */
    private final HashMap<FailureScope, HashSet<RecoveryAgent>> _outstandingRecoveryRecords;

    /**
     *
     * A record of ongoing recovery termination at any given point. When the RLS requests a
     * RecoveryAgent to terminate a unit of recovery, a record of this request is stored in
     * the map. The client service should halt the associated recovery process and then invoke
     * the RecoveryDirector.terminationComplete(RecoveryAgent,FailureScope) method. At this
     * point the record will be removed from this map.
     *
     * Unlike the recovery initilaization logic, there is only a single map to coordinate
     * recovery termination.
     *
     * <p>
     * <ul>
     * <li>java.util.HashMap (RecoveryAgent -> HashSet (FailureScope))
     * </ul>
     */
    private final HashMap<RecoveryAgent, HashSet<FailureScope>> _outstandingTerminationRecords;

    /**
     * A boolean flag indicating if the registration of client services is permitted.
     * Once the first recovery cycle has been initiated, no further client services may
     * be registered with the RLS.
     */
    private boolean _registrationAllowed = true;

    /**
     * A FailureScope that identifies the current point of execution.
     */
    protected FailureScope _currentFailureScope;

    /**
     * Set of registered callback object that get informed when recovery events
     * are occuring.
     */
    private HashSet<RecoveryLogCallBack> _registeredCallbacks = new HashSet<RecoveryLogCallBack>();

    /**
     * Collection of event listeners that need to be called.
     */
    private final RecoveryEventListener _eventListeners; /* @MD19638A */

    /**
     * <p>
     * <ul>
     * <li>java.util.HashSet (FailureScope)
     * </ul>
     */
    private final HashSet<FailureScope> _initFailedFailureScopes;

    /**
     * Set of LogFactories determined at runtime via eclipse plugin mechanism
     */
    protected HashMap<String, RecoveryLogFactory> _customLogFactories = new HashMap<String, RecoveryLogFactory>();

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.RecoveryDirectorImpl
    //------------------------------------------------------------------------------
    /**
     * Constructor for creation of the singleton RecoveryDirectorImpl class.
     * Protected to allow subclassing.
     * Internal code may access this instance via the RecoveryDirectorImpl.instance()
     * method. Client services may access this instance via the RecoveryDirectorFactory.
     * recoveryDirector() method.
     */
    protected RecoveryDirectorImpl() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "RecoveryDirectorImpl");

        // Allocate the map which will contain all registered service managers
        _registeredRecoveryAgents = new TreeMap<Integer, ArrayList<RecoveryAgent>>();

        // Allocate the map which will contain a record of the outstanding serial
        // recovery processing.
        _outstandingInitializationRecords = new HashMap<RecoveryAgent, HashSet<FailureScope>>();

        // Allocate the map which will contain a record of the outstanding initial
        // recovery processing.
        _outstandingRecoveryRecords = new HashMap<FailureScope, HashSet<RecoveryAgent>>();

        // Allocate the map which will contain a record of the oustanding recovery
        // termination requests.
        _outstandingTerminationRecords = new HashMap<RecoveryAgent, HashSet<FailureScope>>();

        // Allocate the set which will contain a list of those failure scopes for
        // which recovery initialization has failed.
        _initFailedFailureScopes = new HashSet<FailureScope>();

        // The collection interface to the recovery event listeners will always be
        // built to simplify the callback logic.
        _eventListeners = RegisteredRecoveryEventListeners.instance(); /* @MD19638A */

        if (theRecoveryLogFactory != null) {
            String className = theRecoveryLogFactory.getClass().getName();
            _customLogFactories.put(className, theRecoveryLogFactory);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RecoveryDirectorImpl: setting RecoveryLogFactory, " + theRecoveryLogFactory + "for classname, " + className);
        } else if (tc.isDebugEnabled())
            Tr.debug(tc, "RecoveryDirectorImpl: the RecoveryLogFactory is null");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "RecoveryDirectorImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.instance
    //------------------------------------------------------------------------------
    /**
     * Create or lookup the singleton instance of the RecoveryDirectorImpl class. This
     * method is intended for internal use only. Client services should access this
     * instance via the RecoveryDirectorFactory.recoveryDirector() method.
     *
     * @return The singleton instance of the RecoveryDirectorImpl class.
     */
    public static synchronized RecoveryDirector instance() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "instance");

        if (_instance == null) {
            _instance = new RecoveryDirectorImpl();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "instance", _instance);
        return _instance;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.registerService
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Invoked by a client service during its initialization to register with the RLS
     * The client service provides RecoveryAgent callback object that will be invoked
     * each time a FailureScope requires recovery. One registration is required per
     * client service. Any re-registration will result in the
     * ConflictingCredentialsException being thrown.
     * </p>
     *
     * <p>
     * The client service provides a 'sequence' value that is used by the RLS to
     * determine the order in which registered RecoveryAgents should be directed
     * to recover. The client service whose RecoveryAgent was registered with the
     * lowest numeric sequence value will be driven first. The remaining
     * RecoveryAgents are driven in ascending sequence value order. RecoveryAgents
     * that are registered with the same sequence value will be driven in
     * an undefined order.
     * </p>
     *
     * <p>
     * The result of registration is a new instance of the RecoveryLogManager
     * class to control recovery logging on behalf of the client service.
     * </p>
     *
     * <p>
     * The RecoveryAgent object is also used to identify the client service
     * and the registration process will fail if it provides a client service
     * identifier or name that has already been used.
     * </p>
     *
     * @param recoveryAgent Client service identification and callback object.
     * @param sequence      Client service sequence value.
     *
     * @return A RecoveryLogManager object that the client service can use to
     *         control recovery logging.
     *
     * @exception ConflictingCredentialsException Thrown if the RecoveryAgent identity or
     *                                                name clashes with a client service that
     *                                                is already registered
     * @exception InvalidStateException           Thrown if the registration occurs after
     *                                                the first recovery process has been
     *                                                started.
     */
    @Override
    public RecoveryLogManager registerService(RecoveryAgent recoveryAgent, int sequence) throws ConflictingCredentialsException, InvalidStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerService", new Object[] { recoveryAgent, sequence, this });

        RecoveryLogManager clientRLM = null;

        // Synchronize on the _registeredRecoveryAgents map since we may have cocurrent registrations
        // taking place.
        synchronized (_registeredRecoveryAgents) {
            // Ensure that this registration event is occuring prior to the first recovery cycle. Throw an exception if its not.
            // This check is required to ensure that recovery agents do not register late and miss recovery processing events.
            if (!_registrationAllowed) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Client service registration attempted after recovery processing has been driven");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "registerService", "InvalidStateException");
                throw new InvalidStateException(null);
            }

            // Ensure that the supplied RecoveryAgent supplies a unique identity and name.
            final int clientIdentifier = recoveryAgent.clientIdentifier();
            final String clientName = recoveryAgent.clientName();

            // Extract the 'values' collection from the _registeredRecoveryAgents map and create an iterator
            // from it. This iterator will return ArrayList objects each containing a set of RecoveryAgent
            // objects. Each ArrayList corrisponds to a different sequence priority value.
            final Collection registeredRecoveryAgentsValues = _registeredRecoveryAgents.values();
            final Iterator registeredRecoveryAgentsValuesIterator = registeredRecoveryAgentsValues.iterator();

            while (registeredRecoveryAgentsValuesIterator.hasNext()) {
                // Extract the next ArrayList and create an iterator from it. This iterator will return RecoveryAgent
                // objects that are registered at the same sequence priority value.
                final ArrayList registeredRecoveryAgentsArray = (java.util.ArrayList) registeredRecoveryAgentsValuesIterator.next();
                final Iterator registeredRecoveryAgentsArrayIterator = registeredRecoveryAgentsArray.iterator();

                while (registeredRecoveryAgentsArrayIterator.hasNext()) {
                    // Extract the next RecoveryAgent object
                    final RecoveryAgent registeredRecoveryAgent = (RecoveryAgent) registeredRecoveryAgentsArrayIterator.next();

                    if ((registeredRecoveryAgent.clientIdentifier() == clientIdentifier) ||
                        (registeredRecoveryAgent.clientName().equals(clientName))) {
                        // The client service has attempted registration with a RecoveryAgent that reports either an
                        // identity or name that conflicts with an existing identity or name. Throw the registration
                        // request out with an exception.
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Client service registration attempted with non-unique identity or name");
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "registerService", "ConflictingCredentialsException");
                        throw new ConflictingCredentialsException(null);
                    }
                }
            }

            // This is a valid registration. Store the RecoveryAgent, keyed from the supplied sequence value.
            final Integer sequenceI = new Integer(sequence);
            ArrayList<RecoveryAgent> sequenceArray = _registeredRecoveryAgents.get(sequenceI);

            if (sequenceArray == null) {
                sequenceArray = new java.util.ArrayList<RecoveryAgent>();
                _registeredRecoveryAgents.put(sequenceI, sequenceArray);
            }

            sequenceArray.add(recoveryAgent);

            // Create a new RecoveryLogManager object that the client service can use to access
            // their recovery logs.
            clientRLM = new RecoveryLogManagerImpl(recoveryAgent, _customLogFactories);
            if (tc.isEventEnabled())
                Tr.event(tc, "New service '" + clientName + "' (" + clientIdentifier + ") registered with RecoveryDirectorImpl");

            // When the transaction service registers, stash this RA away so that we can make a special
            // query of its recovery log directory in support of the exclusive locking model. Once
            // the HA framework provides more support for Network Paritioning, we can remove this logic.
            if (clientIdentifier == ClientId.RLCI_TRANSACTIONSERVICE) {
                Configuration.txRecoveryAgent(recoveryAgent);

                // Also set the isSnapshotSafe flag - we've used the transaction
                // service admin console to configure this custom property,
                // although it's really a RLS specific property.   Here, we extract
                // it from the Tx Recovery Agent and update the RLS configuration
                Configuration.setSnapshotSafe(recoveryAgent.isSnapshotSafe());
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerService", clientRLM);
        return clientRLM;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.serialRecoveryComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Invoked by a client service to indicate that a unit of recovery, identified by
     * FailureScope has been completed. The client service supplies its RecoveryAgent
     * reference to identify itself.
     * </p>
     *
     * <p>
     * When recovery events occur, each client services RecoveryAgent callback object
     * has its initiateRecovery() method invoked. As a result of this call, the client
     * services has an opportunity to perform any SERIAL recovery processing for that
     * failure scope. Once this is complete, the client calls the serialRecoveryComplete
     * method to give the next client service to handle recovery processing. Recovery
     * processing as a whole may or may not be complete before this call is issued -
     * it may continue afterwards on a parrallel thread if required. The latter design
     * is prefereable in an HA-enabled environment as controll must be passed back as
     * quickly as possible to avoid the HA framework shutting down the JVM.
     * </p>
     *
     * <p>
     * Regardless of the style adopted, once the recovery process has performed as much
     * processing as can be conducted without any failed resources becoming available
     * again (eg a failed database), the initialRecoveryComplete call must be issued
     * to indicate this fact. This call is used by the RLS to optomize its interactions
     * with the HA framework.
     * </p>
     *
     * <p>
     * The RecoveryDirector will then pass the recovery request on to other registered
     * client services.
     * </p>
     *
     * @param recoveryAgent The client services RecoveryAgent instance.
     * @param failureScope  The unit of recovery that is completed.
     *
     * @exception InvalidFailureScope The supplied FailureScope was not recognized as
     *                                    outstanding unit of recovery for the client
     *                                    service.
     */
    @Override
    public void serialRecoveryComplete(RecoveryAgent recoveryAgent, FailureScope failureScope) throws InvalidFailureScopeException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serialRecoveryComplete", new Object[] { recoveryAgent, failureScope, this });

        final boolean removed = removeInitializationRecord(recoveryAgent, failureScope);

        if (!removed) {
            if (tc.isEventEnabled())
                Tr.event(tc, "The supplied FailureScope was not recognized as outstaning work for this RecoveryAgent");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "serialRecoveryComplete", "InvalidFailureScopeException");
            throw new InvalidFailureScopeException(null);
        }

        _eventListeners.clientRecoveryComplete(failureScope, recoveryAgent.clientIdentifier());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serialRecoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirector.terminationComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Invoked by a client service to indicate recovery processing for the identified
     * FailureScope ceased. The client service supplies its RecoveryAgent reference to
     * identify itself.
     * </p>
     *
     * <p>
     * The RecoveryDirector will then pass the termination request on to other registered
     * client services.
     * </p>
     *
     * @param recoveryAgent The client services RecoveryAgent instance.
     * @param failureScope  The unit of recovery that is completed.
     *
     * @exception InvalidFailureScopeException The supplied FailureScope was not recognized as
     *                                             outstanding unit of recovery for the client
     *                                             service.
     */
    @Override
    public void terminationComplete(RecoveryAgent recoveryAgent, FailureScope failureScope) throws InvalidFailureScopeException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "terminationComplete", new Object[] { recoveryAgent, failureScope, this });

        final boolean removed = removeTerminationRecord(recoveryAgent, failureScope);

        if (!removed) {
            if (tc.isEventEnabled())
                Tr.event(tc, "The supplied FailureScope was not recognized as an outstaning termination request for this RecoveryAgent");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "terminationComplete", "InvalidFailureScopeException");
            throw new InvalidFailureScopeException(null);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "terminationComplete");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.currentFailureScope
    //------------------------------------------------------------------------------
    /**
     * Invoked by a client service to determine the "current" FailureScope. This is
     * defined as a FailureScope that identifies the current point of execution. In
     * practice this means the current server on distributed or server region on 390.
     *
     * @return FailureScope The current FailureScope.
     */
    @Override
    public synchronized FailureScope currentFailureScope() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "currentFailureScope", this);

        if (_currentFailureScope == null) {
            _currentFailureScope = new FileFailureScope();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "currentFailureScope", _currentFailureScope);
        return _currentFailureScope;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.directInitialization
    //------------------------------------------------------------------------------
    /**
     * Internal method to initiate recovery processing of the given FailureScope. All
     * registered RecoveryAgent objects will be directed to process the FailureScope
     * in sequence.
     *
     * @param FailureScope The FailureScope to process.
     * @return boolean success
     */
    @Override
    @FFDCIgnore({ RecoveryFailedException.class })
    public void directInitialization(FailureScope failureScope) throws RecoveryFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "directInitialization", new Object[] { failureScope, this });

        // Use configuration to determine if recovery is local (for z/OS).
        final FailureScope currentFailureScope = Configuration.localFailureScope(); /* @LI1578-22A */

        // Synchronize to ensure consistency with the registerService
        // method. The remainder of the method is not synchronized on this in order that two independant
        // recovery processes may be driven concurrently on two different threads.
        synchronized (_registeredRecoveryAgents) {
            // Ensure that further RecoveryAgent registrations are prohibited.
            _registrationAllowed = false;
        }

        if (currentFailureScope.equals(failureScope)) /* @LI1578-22C */
        {
            Tr.info(tc, "CWRLS0010_PERFORM_LOCAL_RECOVERY", failureScope.serverName());
        } else {
            Tr.info(tc, "CWRLS0011_PERFORM_PEER_RECOVERY", failureScope.serverName());
        }

        // Extract the 'values' collection from the _registeredRecoveryAgents map and create an iterator
        // from it. This iterator will return ArrayList objects each containing a set of RecoveryAgent
        // objects. Each ArrayList corrisponds to a different sequence priority value.
        final Collection registeredRecoveryAgentsValues = _registeredRecoveryAgents.values();

        Iterator registeredRecoveryAgentsValuesIterator = registeredRecoveryAgentsValues.iterator();
        while (registeredRecoveryAgentsValuesIterator.hasNext()) {
            // Extract the next ArrayList and create an iterator from it. This iterator will return RecoveryAgent
            // objects that are registered at the same sequence priority value.
            final ArrayList registeredRecoveryAgentsArray = (java.util.ArrayList) registeredRecoveryAgentsValuesIterator.next();
            final Iterator registeredRecoveryAgentsArrayIterator = registeredRecoveryAgentsArray.iterator();

            while (registeredRecoveryAgentsArrayIterator.hasNext()) {
                // Extract the next RecoveryAgent object
                final RecoveryAgent recoveryAgent = (RecoveryAgent) registeredRecoveryAgentsArrayIterator.next();
                recoveryAgent.prepareForRecovery(failureScope);

                // Prepare the maps for the recovery event.
                addInitializationRecord(recoveryAgent, failureScope);
                addRecoveryRecord(recoveryAgent, failureScope);
            }
        }

        // This is the opportunity to kick off any Network Parition Detection logic that we deem necessary. Right
        // now we are relying on the Hardware quorum support within the HA framework itself if NP's are tro be
        // handled.

        if (Configuration.HAEnabled()) {
            // Join the "dynamic cluster" in order that IOR references can be associated with the
            // resulting identity. Only do this in an HA-enabled environment.
            Configuration.getRecoveryLogComponent().joinCluster(failureScope);
        }

        // If callbacks are registered, drive then now.
        if (_registeredCallbacks != null) {
            driveCallBacks(CALLBACK_RECOVERYSTARTED, failureScope);
        }

        // Re-set the iterator.
        registeredRecoveryAgentsValuesIterator = registeredRecoveryAgentsValues.iterator();

        while (registeredRecoveryAgentsValuesIterator.hasNext()) {
            // Extract the next ArrayList and create an iterator from it. This iterator will return RecoveryAgent
            // objects that are registered at the same sequence priority value.
            final ArrayList registeredRecoveryAgentsArray = (java.util.ArrayList) registeredRecoveryAgentsValuesIterator.next();
            final Iterator registeredRecoveryAgentsArrayIterator = registeredRecoveryAgentsArray.iterator();

            while (registeredRecoveryAgentsArrayIterator.hasNext()) {
                // Extract the next RecoveryAgent object
                final RecoveryAgent recoveryAgent = (RecoveryAgent) registeredRecoveryAgentsArrayIterator.next();

                // Direct the RecoveryAgent instance to process this failure scope.
                try {
                    // Notify the listeners we're about to make the call
                    _eventListeners.clientRecoveryInitiated(failureScope, recoveryAgent.clientIdentifier()); /* @MD19638A */

                    // HADB Peer Locking function is provided in tWAS to handle the case where a network is partitioned
                    // and transaction recovery logs are stored in an RDBMS.This function, while not strictly required in
                    // Liberty is included in Liberty in order to maintain compatibility and allow testing.
                    //
                    // HADB Peer Locking is enabled through a server.xml enableHADBPeerLocking attribute in the transaction element.
                    boolean shouldBeRecovered = true;
                    boolean enableHADBPeerLocking = recoveryAgent.isDBTXLogPeerLocking();
                    if (enableHADBPeerLocking) {
                        // We need to acquire a Heartbeat Recovery Log reference whether we are recovering a local
                        // or peer server. In each case we get a reference to the appropriate Recovery Log.
                        HeartbeatLog heartbeatLog = recoveryAgent.getHeartbeatLog(failureScope);

                        if (heartbeatLog != null) {
                            if (currentFailureScope.equals(failureScope)) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "LOCAL RECOVERY, claim local logs");
                                shouldBeRecovered = heartbeatLog.claimLocalRecoveryLogs();
                                if (!shouldBeRecovered) {
                                    // Cannot recover the home server, throw exception
                                    RecoveryFailedException rfex = new RecoveryFailedException("HADB Peer locking, local recovery failed");

                                    throw rfex;
                                }

                            } else {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "PEER RECOVERY, take lock, ie check staleness");
                                shouldBeRecovered = heartbeatLog.claimPeerRecoveryLogs();
                                if (!shouldBeRecovered) {
                                    // Cannot recover peer server, throw exception
                                    RecoveryFailedException rfex = new RecoveryFailedException("HADB Peer locking, peer recovery failed");
                                    throw rfex;
                                }
                            }
                        }
                    }

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "now initiateRecovery if shouldBeRecovered - " + shouldBeRecovered);
                    if (shouldBeRecovered)
                        recoveryAgent.initiateRecovery(failureScope);
                } catch (RecoveryFailedException exc) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "directInitialization", exc);
                    throw exc;
                }

                // Wait for 'serialRecoveryComplete' to be called. This callback may be issued from another thread.
                synchronized (_outstandingInitializationRecords) {
                    while (initializationOutstanding(recoveryAgent, failureScope)) {
                        try {
                            _outstandingInitializationRecords.wait();
                        } catch (InterruptedException exc) {
                            // This exception is recieved if another thread interrupts this thread by calling this threads
                            // Thread.interrupt method. The RecoveryDirectorImpl class does not use this mechanism for
                            // breaking out of the wait call - it uses notifyAll to wake up all waiting threads. This
                            // exception should never be generated. If for some reason it is called then ignore it and
                            //start to wait again.
                            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl.directInitialization", "432", this);
                        }
                    }
                }
            }
        }

        if (currentFailureScope.equals(failureScope)) /* @LI1578-22C */
        {
            Tr.info(tc, "CWRLS0012_DIRECT_LOCAL_RECOVERY", failureScope.serverName());
        } else {
            Tr.info(tc, "CWRLS0013_DIRECT_PEER_RECOVERY", failureScope.serverName());
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "directInitialization");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.directTermination
    //------------------------------------------------------------------------------
    /**
     * Internal method to terminate recovery processing of the given FailureScope. All
     * registered RecoveryAgent objects will be directed to terminate processing of the
     * FailureScopein sequence.
     *
     * @param FailureScope The FailureScope to process.
     */
    public void directTermination(FailureScope failureScope) throws TerminationFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "directTermination", new Object[] { failureScope, this });

        Tr.info(tc, "CWRLS0014_HALT_PEER_RECOVERY", failureScope.serverName());

        // If callbacks are registered, drive then now.
        if (_registeredCallbacks != null) {
            driveCallBacks(CALLBACK_TERMINATIONSTARTED, failureScope);
        }

        if (Configuration.HAEnabled()) {
            Configuration.getRecoveryLogComponent().leaveCluster(failureScope);
        }

        // Extract the 'values' collection from the _registeredRecoveryAgents map and create an iterator
        // from it. This iterator will return ArrayList objects each containing a set of RecoveryAgent
        // objects. Each ArrayList corrisponds to a different sequence priority value.
        final Collection registeredRecoveryAgentsValues = _registeredRecoveryAgents.values();
        final Iterator registeredRecoveryAgentsValuesIterator = registeredRecoveryAgentsValues.iterator();

        while (registeredRecoveryAgentsValuesIterator.hasNext()) {
            // Extract the next ArrayList and create an iterator from it. This iterator will return RecoveryAgent
            // objects that are registered at the same sequence priority value.
            final ArrayList registeredRecoveryAgentsArray = (java.util.ArrayList) registeredRecoveryAgentsValuesIterator.next();
            final Iterator registeredRecoveryAgentsArrayIterator = registeredRecoveryAgentsArray.iterator();

            while (registeredRecoveryAgentsArrayIterator.hasNext()) {
                // Extract the next RecoveryAgent object
                final RecoveryAgent recoveryAgent = (RecoveryAgent) registeredRecoveryAgentsArrayIterator.next();

                // Record the fact that we have an outstanding termination request for the RecoveryAgent.
                addTerminationRecord(recoveryAgent, failureScope);

                // Direct the RecoveryAgent instance to terminate processing of this failure scope
                try {
                    recoveryAgent.terminateRecovery(failureScope);
                } catch (TerminationFailedException exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl.directTermination", "540", this);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "directTermination", exc);
                    throw exc;
                } catch (Exception exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl.directTermination", "576", this);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "directTermination", exc);
                    throw new TerminationFailedException(exc);
                }

                // Wait for 'terminationComplete' to be called. This callback may be issued from another thread.
                synchronized (_outstandingTerminationRecords) {
                    while (terminationOutstanding(recoveryAgent, failureScope)) {
                        try {
                            _outstandingTerminationRecords.wait();
                        } catch (InterruptedException exc) {
                            // This exception is recieved if another thread interrupts this thread by calling this threads
                            // Thread.interrupt method. The RecoveryDirectorImpl class does not use this mechanism for
                            // breaking out of the wait call - it uses notifyAll to wake up all waiting threads. This
                            // exception should never be generated. If for some reason it is called then ignore it and
                            //start to wait again.
                            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl.directTermination", "549", this);
                        }
                    }
                }
            }
        }

        // If callbacks are registered, drive then now.
        if (_registeredCallbacks != null) {
            driveCallBacks(CALLBACK_TERMINATIONCOMPLETE, failureScope);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "directTermination");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.addInitializationRecord
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to record an outstanding 'serialRecoveryComplete' call that must
     * be issued by the client service represented by the supplied RecoveryAgent for
     * the given failure scope.
     * </p>
     *
     * <p>
     * Just prior to requesting a RecoveryAgent to "initiateRecovery" of a
     * FailureScope, this method is driven to record the request. When the client
     * service completes the serial portion of the recovery process and invokes
     * RecoveryDirector.serialRecoveryComplete, the removeInitializationRecord method is
     * called to remove this record.
     * </p>
     *
     * <p>
     * This allows the RLS to track the "serial" portion of an ongoing recovery process.
     * </p>
     *
     * <p>
     * [ SERIAL PHASE ] [ INITIAL PHASE ] [ RETRY PHASE ]
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent that is about to be directed to process
     *                          recovery of a FailureScope.
     * @param failureScope  The FailureScope.
     */
    private void addInitializationRecord(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addInitializationRecord", new Object[] { recoveryAgent, failureScope, this });

        synchronized (_outstandingInitializationRecords) {
            // Extract the set of failure scopes that the corrisponding client service is currently
            // processing
            HashSet<FailureScope> failureScopeSet = _outstandingInitializationRecords.get(recoveryAgent);

            // If its not handled yet any then create an empty set to hold both this and future
            // failure scopes.
            if (failureScopeSet == null) {
                failureScopeSet = new HashSet<FailureScope>();
                _outstandingInitializationRecords.put(recoveryAgent, failureScopeSet);
            }

            // Add this new failure scope to the set of those currently being processed by the
            // client service.
            failureScopeSet.add(failureScope);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addInitializationRecord");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.addTerminationRecord
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to record a termination request for the supplied RecoveryAgent
     * and FailureScope combination.
     * </p>
     *
     * <p>
     * Just prior to requesting a RecoveryAgent to "terminateRecovery" of a
     * FailureScope, this method is driven to record the request. When the client
     * service is ready and invokes RecoveryDirector.terminateComplete,
     * the removeTerminationRecord method is called to remove this record.
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent that is about to be directed to terminate
     *                          recovery of a FailureScope.
     * @param failureScope  The FailureScope.
     */
    private void addTerminationRecord(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addTerminationRecord", new Object[] { recoveryAgent, failureScope, this });

        synchronized (_outstandingTerminationRecords) {
            // Extract the set of failure scopes that the corrisponding client service is currently
            // processing
            HashSet<FailureScope> failureScopeSet = _outstandingTerminationRecords.get(recoveryAgent);

            // If its not handled yet any then create an empty set to hold both this and future
            // failure scopes.
            if (failureScopeSet == null) {
                failureScopeSet = new HashSet<FailureScope>();
                _outstandingTerminationRecords.put(recoveryAgent, failureScopeSet);
            }

            // Add this new failure scope to the set of those currently being processed by the
            // client service.
            failureScopeSet.add(failureScope);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addTerminationRecord");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.removeInitializationRecord
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to remove the record of an outstanding 'serialRecoveryComplete'
     * call from the supplied RecoveryAgent for the given failure scope.
     * </p>
     *
     * <p>
     * This call will wake up all threads waiting for serial recovery to be completed.
     * </p>
     *
     * <p>
     * Just prior to requesting a RecoveryAgent to "initiateRecovery" of a
     * FailureScope, the addInitializationRecord method is driven to record the request.
     * When the client service completes the serial portion of the recovery process and
     * invokes RecoveryDirector.serialRecoveryComplete, this method called to remove
     * this record.
     * </p>
     *
     * <p>
     * [ SERIAL PHASE ] [ INITIAL PHASE ] [ RETRY PHASE ]
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent that has completed the serial recovery
     *                          processing phase.
     * @param failureScope  The FailureScope that defined the scope of this recovery
     *                          processing.
     *
     * @return boolean true if there was an oustanding recovery record, otherwise false.
     */
    private boolean removeInitializationRecord(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeInitializationRecord", new Object[] { recoveryAgent, failureScope, this });

        boolean found = false;

        synchronized (_outstandingInitializationRecords) {
            // Extract the set of failure scopes that the corrisponding client service is currently
            // processing.
            final HashSet failureScopeSet = _outstandingInitializationRecords.get(recoveryAgent);

            // Since this method should only be called in response to a request to handle the recovery
            // of a failure scope, then this set should never be null. To avoid a null pointer exception
            // in the event of a client service bug, ensure this set is valid before attempting to
            // remove the given failure scope.
            if (failureScopeSet != null) {
                // Remove the failure scope. As with the set, if this is not found then this due to a failure in
                // the client service. In practice, found should never be false.
                found = failureScopeSet.remove(failureScope);
            }

            // Wake all threads currenly waiting on the completion of units of recovery.
            _outstandingInitializationRecords.notifyAll();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeInitializationRecord", found);
        return found;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.removeTerminationRecord
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to remove the record of an outstanding termination request for
     * the supplied RecoveryAgent and FailureScope combination. See addRecoveryRequest
     * for more information.
     * </p>
     *
     * <p>
     * This call will wake up all threads waiting for units of recovery to be
     * completed.
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent.
     * @param failureScope  The FailureScope
     *
     * @return boolean true if there was an oustanding recovery record, otherwise false.
     */
    private boolean removeTerminationRecord(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeTerminationRecord", new Object[] { recoveryAgent, failureScope, this });

        boolean found = false;

        synchronized (_outstandingTerminationRecords) {
            // Extract the set of failure scopes that the corrisponding client service is currently
            // processing.
            final HashSet failureScopeSet = _outstandingTerminationRecords.get(recoveryAgent);

            // Since this method should only be called in response to a request to handle the recovery
            // of a failure scope, then this set should never be null. To avoid a null pointer exception
            // in the event of a client service bug, ensure this set is valid before attempting to
            // remove the given failure scope.
            if (failureScopeSet != null) {
                // Remove the failure scope. As with the set, if this is not found then this due to a failure in
                // the client service. In practice, found should never be false.
                found = failureScopeSet.remove(failureScope);
            }

            // Wake all threads currenly waiting on the completion of units of recovery.
            _outstandingTerminationRecords.notifyAll();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeTerminationRecord", found);
        return found;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.initializationOutstanding
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to determine if there is an outstanding 'serialRecoveryComplete'
     * call that must be issued by the client service represented by the supplied
     * RecoveryAgent for the given failure scope.
     * </p>
     *
     * <p>
     * Just prior to requesting a RecoveryAgent to "initiateRecovery" of a
     * FailureScope, the addInitializationRecord method is driven to record the
     * request. When the client service completes the serial portion of the recovery
     * process and invokes serialRecoveryComplete, the removeInitializationRecord method
     * is called to remove this record.
     * </p>
     *
     * <p>
     * This allows the RLS to track the "serial" portion of an ongoing recovery process.
     * </p>
     *
     * <p>
     * [ SERIAL PHASE ] [ INITIAL PHASE ] [ RETRY PHASE ]
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent.
     * @param failureScope  The FailureScope.
     *
     * @return boolean true if there is an oustanding recovery request, otherwise false.
     */
    private boolean initializationOutstanding(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initializationOutstanding", new Object[] { recoveryAgent, failureScope, this });

        boolean outstanding = false;

        synchronized (_outstandingInitializationRecords) {
            // Extract the set of failure scopes that the corrisponding client service is currently
            // processing.
            final HashSet failureScopeSet = _outstandingInitializationRecords.get(recoveryAgent);

            // If there are some then determine if the set contains the given FailureScope.
            if (failureScopeSet != null) {
                outstanding = failureScopeSet.contains(failureScope);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initializationOutstanding", outstanding);
        return outstanding;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.terminationOutstanding
    //------------------------------------------------------------------------------
    /**
     * Internal method to determine if there is an outstanding termination request for
     * the supplied RecoveryAgent and FailureScope.
     *
     * @param recoveryAgent The RecoveryAgent.
     * @param failureScope  The FailureScope.
     *
     * @return boolean true if there is an oustanding termination request, otherwise false.
     */
    private boolean terminationOutstanding(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "terminationOutstanding", new Object[] { recoveryAgent, failureScope, this });

        boolean outstanding = false;

        synchronized (_outstandingTerminationRecords) {
            // Extract the set of failure scopes that the corrisponding client service is currently
            // processing.
            final HashSet failureScopeSet = _outstandingTerminationRecords.get(recoveryAgent);

            // If there are some then determine if the set contains the given FailureScope.
            if (failureScopeSet != null) {
                outstanding = failureScopeSet.contains(failureScope);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "terminationOutstanding", outstanding);
        return outstanding;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.driveLocalRecovery
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to drive a recovery process for the current FailureScope. This
     * method is driven at a well defined point during server startup in order to
     * recover the local server node.
     * </p>
     */
    public void driveLocalRecovery() throws RecoveryFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "driveLocalRecovery", this);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "RLSHA: configuring for local only recovery");

        synchronized (this) {
            if (_currentFailureScope == null) {
                // Create a FailureScope that defines the current server.
                _currentFailureScope = new FileFailureScope();
            }
        }

        try {
            directInitialization(_currentFailureScope);
        } catch (RecoveryFailedException exc) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "driveLocalRecovery", exc);
            throw exc;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "driveLocalRecovery");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.addRecoveryRecord
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to record an outstanding 'initialRecoveryComplete' call that must
     * be issued by the client service represented by the supplied RecoveryAgent for
     * the given failure scope.
     * </p>
     *
     * <p>
     * Just prior to requesting a RecoveryAgent to "initiateRecovery" of a
     * FailureScope, this method is driven to record the request. When the client
     * service completes the initial portion of the recovery process and invokes
     * RecoveryDirector.initialRecoveryComplete, the removeRecoveryRecord method is
     * called to remove this record.
     * </p>
     *
     * <p>
     * This allows the RLS to track the "initial" portion of an ongoing recovery process.
     * </p>
     *
     * <p>
     * [ SERIAL PHASE ] [ INITIAL PHASE ] [ RETRY PHASE ]
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent that is about to be directed to process
     *                          recovery of a FailureScope.
     * @param failureScope  The FailureScope.
     */
    private void addRecoveryRecord(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addRecoveryRecord", new Object[] { recoveryAgent, failureScope, this });

        synchronized (_outstandingRecoveryRecords) {
            HashSet<RecoveryAgent> recoveryAgentSet = _outstandingRecoveryRecords.get(failureScope);

            if (recoveryAgentSet == null) {
                recoveryAgentSet = new HashSet<RecoveryAgent>();
                _outstandingRecoveryRecords.put(failureScope, recoveryAgentSet);
            }

            recoveryAgentSet.add(recoveryAgent);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addRecoveryRecord");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.removeRecoveryRecord
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to remove the record of an outstanding 'initialRecoveryComplete'
     * call from the supplied RecoveryAgent for the given failure scope.
     * </p>
     *
     * <p>
     * This call will wake up all threads waiting for initial recovery to be completed.
     * </p>
     *
     * <p>
     * Just prior to requesting a RecoveryAgent to "initiateRecovery" of a
     * FailureScope, the addRecoveryRecord method is driven to record the request.
     * When the client service completes the initial portion of the recovery process and
     * invokes RecoveryDirector.initialRecoveryComplete, this method called to remove
     * this record.
     * </p>
     *
     * <p>
     * [ SERIAL PHASE ] [ INITIAL PHASE ] [ RETRY PHASE ]
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent that has completed the initial recovery
     *                          processing phase.
     * @param failureScope  The FailureScope that defined the scope of this recovery
     *                          processing.
     *
     * @return boolean true if there was an oustanding recovery record, otherwise false.
     */
    private boolean removeRecoveryRecord(RecoveryAgent recoveryAgent, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoveryRecord", new Object[] { recoveryAgent, failureScope, this });

        boolean found = false;

        synchronized (_outstandingRecoveryRecords) {
            final HashSet recoveryAgentSet = _outstandingRecoveryRecords.get(failureScope);

            if (recoveryAgentSet != null) {
                found = recoveryAgentSet.remove(recoveryAgent);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoveryRecord", found);
        return found;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.recoveryOutstading
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to determine if there are any outstanding 'initialRecoveryComplete'
     * calls that must be issued by client services for the given failure scope.
     * </p>
     *
     * <p>
     * Just prior to requesting a RecoveryAgent to "initiateRecovery" of a FailureScope,
     * the addRecoveryRecord method is driven to record the request. When the client
     * service completes the initial portion of the recovery process and invokes
     * initialRecoveryComplete, the removeRecoveryRecord method is called to remove this
     * record.
     * </p>
     *
     * <p>
     * This allows the RLS to track the "initial" portion of an ongoing recovery process
     * accross all client services.
     * </p>
     *
     * <p>
     * [ SERIAL PHASE ] [ INITIAL PHASE ] [ RETRY PHASE ]
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent.
     * @param failureScope  The FailureScope.
     *
     * @return boolean true if there is an oustanding recovery request, otherwise false.
     */
    private boolean recoveryOutstanding(FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryOutstanding", new Object[] { failureScope, this });

        boolean outstanding = false;

        synchronized (_outstandingRecoveryRecords) {
            final HashSet recoveryAgentSet = _outstandingRecoveryRecords.get(failureScope);

            // If there are some then determine if the set contains the given FailureScope.
            if (recoveryAgentSet != null && recoveryAgentSet.size() > 0) {
                outstanding = true;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryOutstanding", outstanding);
        return outstanding;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.initialRecoveryComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Invoked by a client service to indicate that initial recovery processing for the
     * unit of recovery, identified by FailureScope has been completed. The client
     * service supplies its RecoveryAgent reference to identify itself.
     * </p>
     *
     * <p>
     * When recovery events occur, each client services RecoveryAgent callback object
     * has its initiateRecovery() method invoked. As a result of this call, the client
     * services has an opportunity to perform any SERIAL recovery processing for that
     * failure scope. Once this is complete, the client calls the serialRecoveryComplete
     * method to give the next client service to handle recovery processing. Recovery
     * processing as a whole may or may not be complete before this call is issued -
     * it may continue afterwards on a parrallel thread if required. The latter design
     * is prefereable in an HA-enabled environment as controll must be passed back as
     * quickly as possible to avoid the HA framework shutting down the JVM.
     * </p>
     *
     * <p>
     * Regardless of the style adopted, once the recovery process has performed as much
     * processing as can be conducted without any failed resources becoming available
     * again (eg a failed database), the initialRecoveryComplete call must be issued
     * to indicate this fact. This call is used by the RLS to optomize its interactions
     * with the HA framework.
     * </p>
     *
     * <p>
     * The RecoveryDirector will then pass the recovery request on to other registered
     * client services.
     * </p>
     *
     * @param recoveryAgent The client services RecoveryAgent instance.
     * @param failureScope  The unit of recovery that is completed.
     *
     * @exception InvalidFailureScope The supplied FailureScope was not recognized as
     *                                    outstanding unit of recovery for the client
     *                                    service.
     */
    @Override
    public void initialRecoveryComplete(RecoveryAgent recoveryAgent, FailureScope failureScope) throws InvalidFailureScopeException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initialRecoveryComplete", new Object[] { recoveryAgent, failureScope, this });

        final boolean removed = removeRecoveryRecord(recoveryAgent, failureScope);

        if (!removed) {
            if (tc.isEventEnabled())
                Tr.event(tc, "The supplied FailureScope was not recognized as outstaning work for this RecoveryAgent");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "initialRecoveryComplete", "InvalidFailureScopeException");
            throw new InvalidFailureScopeException(null);
        }

        // Once recovery processing has been completed, we need to examine the results and take appropriate
        // action.
        if (!recoveryOutstanding(failureScope)) {
            // Check to see if there is a record of some other recovery agent failing for this
            // recovery work.
            boolean atLeastOneServiceReportedFailure = false;

            synchronized (_initFailedFailureScopes) {
                atLeastOneServiceReportedFailure = _initFailedFailureScopes.remove(failureScope);
            }

            if (!atLeastOneServiceReportedFailure) {
                // There was no record of any failure. Proceed as normal to drive callbacks and enable
                // peer recovery if appropriate
                if (_registeredCallbacks != null) {
                    driveCallBacks(CALLBACK_RECOVERYCOMPLETE, failureScope);
                }

                // Enable peer recovery if this recovery work is for the local server.
                // This method is called by z/OS as well, so use the Confguration to check if the recovery
                // request is 'local' (since z/OS doesn't use FileFailureScope).
                if (failureScope.equals(Configuration.localFailureScope()) && Configuration.HAEnabled()) /* @LI1578-22C */
                {
                    Configuration.getRecoveryLogComponent().enablePeerRecovery();
                }
            } else {
                // The was a record of a failure. Handle things differntly as a result. Drive the failure callbacks.
                if (_registeredCallbacks != null) {
                    driveCallBacks(CALLBACK_RECOVERYFAILED, failureScope);
                }

                if (Configuration.localFailureScope().equals(failureScope)) {
                    // This is the local failure scope. Cause server termination
                    Configuration.getRecoveryLogComponent().localRecoveryFailed();
                } else {
                    // The is a peer failure scope. Terminate and de-activate to try and allow another member
                    // of the cluster to recover.
                    try {
                        directTermination(failureScope);
                    } catch (Exception exc) {
                        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryDirectorImpl.initialRecoveryComplete", "1399", this);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "initialRecoveryComplete", "An unexpected excetion occured whilst terminating recovery processing");
                    }

                    Configuration.getRecoveryLogComponent().deactivateGroup(failureScope, 60);
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initialRecoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirector.initialRecoveryFailed
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Invoked by a client service to indicate that initial recovery processing for the
     * unit of recovery, identified by FailureScope has been attempted but failed. The
     * client service supplies its RecoveryAgent reference to identify itself.
     * </p>
     *
     * <p>
     * Invoking this method on the local failure scope will result in the server being
     * termianted (by the HA framework)
     * </p>
     *
     * @param recoveryAgent The client services RecoveryAgent instance.
     * @param failureScope  The unit of recovery that is failed.
     *
     * @exception InvalidFailureScope The supplied FailureScope was not recognized as
     *                                    outstanding unit of recovery for the client
     *                                    service.
     */
    @Override
    public void initialRecoveryFailed(RecoveryAgent recoveryAgent, FailureScope failureScope) throws InvalidFailureScopeException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initialRecoveryFailed", new Object[] { recoveryAgent, failureScope, this });

        final boolean removed = removeRecoveryRecord(recoveryAgent, failureScope);

        if (!removed) {
            if (tc.isEventEnabled())
                Tr.event(tc, "The supplied FailureScope was not recognized as outstaning work for this RecoveryAgent");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "initialRecoveryFailed", "InvalidFailureScopeException");
            throw new InvalidFailureScopeException(null);
        }

        // Once recovery processing has been completed, we need to examine the results and take appropriate
        // action.
        if (!recoveryOutstanding(failureScope)) {
            // Drive the failure callback.
            if (_registeredCallbacks != null) {
                driveCallBacks(CALLBACK_RECOVERYFAILED, failureScope);
            }

            // Ensure the failure map is clean for this failure scope.
            synchronized (_initFailedFailureScopes) {
                _initFailedFailureScopes.remove(failureScope);
            }

            if (Configuration.localFailureScope().equals(failureScope)) {
                // This is the local failure scope. Cause server termination
                Configuration.getRecoveryLogComponent().localRecoveryFailed();
            } else {
                // The is a peer failure scope. Terminate and de-activate to try and allow another member
                // of the cluster to recover.
                try {
                    directTermination(failureScope);
                } catch (Exception exc) {
                }

                Configuration.getRecoveryLogComponent().deactivateGroup(failureScope, 60);
            }
        } else {
            // Record this failure so as to ensure correct processing later.
            synchronized (_initFailedFailureScopes) {
                _initFailedFailureScopes.add(failureScope);
            }

            // Tell other services about this failure so they can take any required action.
            final int failedClientId = recoveryAgent.clientIdentifier();

            // Extract the 'values' collection from the _registeredRecoveryAgents map and create an iterator
            // from it. This iterator will return ArrayList objects each containing a set of RecoveryAgent
            // objects. Each ArrayList corrisponds to a different sequence priority value.
            final Collection registeredRecoveryAgentsValues = _registeredRecoveryAgents.values();
            final Iterator registeredRecoveryAgentsValuesIterator = registeredRecoveryAgentsValues.iterator();

            while (registeredRecoveryAgentsValuesIterator.hasNext()) {
                // Extract the next ArrayList and create an iterator from it. This iterator will return RecoveryAgent
                // objects that are registered at the same sequence priority value.
                final ArrayList registeredRecoveryAgentsArray = (java.util.ArrayList) registeredRecoveryAgentsValuesIterator.next();
                final Iterator registeredRecoveryAgentsArrayIterator = registeredRecoveryAgentsArray.iterator();

                while (registeredRecoveryAgentsArrayIterator.hasNext()) {
                    // Extract the next RecoveryAgent object
                    final RecoveryAgent informRecoveryAgent = (RecoveryAgent) registeredRecoveryAgentsArrayIterator.next();

                    if (informRecoveryAgent.clientIdentifier() != failedClientId) {
                        informRecoveryAgent.agentReportedFailure(failedClientId, failureScope);
                    }
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initialRecoveryFailed");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.addCallBack
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Adds a callback object to be driven when recovery events take place.
     * </p>
     *
     * @param callback The new callback object.
     */
    @Override
    public synchronized void addCallBack(RecoveryLogCallBack callback) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addCallBack", callback);

        if (_registeredCallbacks == null) {
            _registeredCallbacks = new HashSet<RecoveryLogCallBack>();
        }

        _registeredCallbacks.add(callback);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addCallBack");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.driveCallBack
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Internal method to drive a callback operation onto registered callback objects.
     * Available 'stage' values are defined in this class and consist of the following:
     * </p>
     *
     * <p>
     * <ul>
     * <li>CALLBACK_RECOVERYSTARTED</li>
     * <li>CALLBACK_RECOVERYCOMPLETE</li>
     * <li>CALLBACK_TERMINATIONSTARTED</li>
     * <li>CALLBACK_TERMINATIONCOMPLETE</li>
     * <li>CALLBACK_RECOVERYFAILED</li>
     * </ul>
     * </p>
     *
     *
     * @param stage        The required callback stage.
     * @param failureScope The failure scope for which the event is taking place.
     */
    private void driveCallBacks(int stage, FailureScope failureScope) {
        if (tc.isEntryEnabled()) {
            switch (stage) {
                case CALLBACK_RECOVERYSTARTED:
                    Tr.entry(tc, "driveCallBacks", new Object[] { "CALLBACK_RECOVERYSTARTED", failureScope });
                    break;
                case CALLBACK_RECOVERYCOMPLETE:
                    Tr.entry(tc, "driveCallBacks", new Object[] { "CALLBACK_RECOVERYCOMPLETE", failureScope });
                    break;
                case CALLBACK_TERMINATIONSTARTED:
                    Tr.entry(tc, "driveCallBacks", new Object[] { "CALLBACK_TERMINATIONSTARTED", failureScope });
                    break;
                case CALLBACK_TERMINATIONCOMPLETE:
                    Tr.entry(tc, "driveCallBacks", new Object[] { "CALLBACK_TERMINATIONCOMPLETE", failureScope });
                    break;
                case CALLBACK_RECOVERYFAILED:
                    Tr.entry(tc, "driveCallBacks", new Object[] { "CALLBACK_RECOVERYFAILED", failureScope });
                    break;
                default:
                    Tr.entry(tc, "driveCallBacks", new Object[] { new Integer(stage), failureScope });
                    break;
            }
        }

        if (_registeredCallbacks != null) {
            final Iterator registeredCallbacksIterator = _registeredCallbacks.iterator();

            while (registeredCallbacksIterator.hasNext()) {
                final RecoveryLogCallBack callBack = (RecoveryLogCallBack) registeredCallbacksIterator.next();

                switch (stage) {
                    case CALLBACK_RECOVERYSTARTED:
                        callBack.recoveryStarted(failureScope);
                        break;

                    case CALLBACK_RECOVERYCOMPLETE:
                    case CALLBACK_RECOVERYFAILED:
                        callBack.recoveryCompleted(failureScope);
                        break;

                    case CALLBACK_TERMINATIONSTARTED:
                        callBack.terminateStarted(failureScope);
                        break;

                    case CALLBACK_TERMINATIONCOMPLETE:
                        callBack.terminateCompleted(failureScope);
                        break;
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "driveCallBacks");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirector.getRecoveryLogConfiguration
    //------------------------------------------------------------------------------
    /**
     * <p>
     * This method allows a client service to determine the recovery log configuration
     * for the supplied failure scope.
     * </p>
     *
     * <p>
     * Since the information returned by this method has been cached when the RLS was
     * initialized, this method can any time in the server lifecycle without any need
     * to worry about potential serialization problems with access to the underlying
     * model.
     * </p>
     *
     * @param failureScope The failure scope for which the recovery log configuration is
     *                         required.
     * @return Object The associated configuration
     */
    @Override
    public Object getRecoveryLogConfiguration(FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoveryLogConfiguration", failureScope);

        final Object recoveryLog = Configuration.getRecoveryLogComponent().getRecoveryLogConfig(failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoveryLogConfiguration", recoveryLog);
        return recoveryLog;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.getNonNullCurrentFailureScopeIDString
    //------------------------------------------------------------------------------
    /**
     * Invoked by a client service to determine the Stringified Identity of the current FailureScope.
     * This is different to performing clusterIdentity() on the currentFailureScope in that if HA is not
     * enabled then the identityString is non-null
     *
     * @return String The Identity string of the current FailureScope.
     */
    @Override
    public String getNonNullCurrentFailureScopeIDString() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getNonNullCurrentFailureScopeIDString");

        final String value = Configuration.getRecoveryLogComponent().getNonNullCurrentFailureScopeIDString(currentFailureScope().serverName());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getNonNullCurrentFailureScopeIDString", value);
        return value;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.registerRecoveryEventListener
    //------------------------------------------------------------------------------
    /**
     * Register the recovery event callback listener.
     *
     * @param rel The new recovery event listener
     */
    @Override
    public void registerRecoveryEventListener(RecoveryEventListener rel) /* @MD19638A */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerRecoveryEventListener", rel);

        RegisteredRecoveryEventListeners.instance().add(rel);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerRecoveryEventListener");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorImpl.isHAEnabled()
    //------------------------------------------------------------------------------
    /**
     * This method allows a client service to determine if High Availability support
     * has been enabled for the local cluster.
     *
     * @return boolean true if HA support is enabled, otherwise false.
     */
    @Override
    public boolean isHAEnabled() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isHAEnabled");

        final boolean haEnabled = Configuration.HAEnabled();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isHAEnabled", haEnabled);
        return haEnabled;
    }

    public static void reset() {
        _instance = null;
    }

    public void drivePeerRecovery() throws RecoveryFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "drivePeerRecovery", this);
        RecoveryAgent libertyRecoveryAgent = null;

        // Get peers from the table

        // Use configuration to determine if recovery is local (for z/OS).
        final FailureScope localFailureScope = Configuration.localFailureScope(); /* @LI1578-22A */
        Tr.audit(tc, "WTRN0108I: " +
                     localFailureScope.serverName() + " checking to see if any peers need recovering");
        ArrayList<String> peersToRecover = null;

        // Extract the 'values' collection from the _registeredRecoveryAgents map and create an iterator
        // from it. This iterator will return ArrayList objects each containing a set of RecoveryAgent
        // objects. Each ArrayList corrisponds to a different sequence priority value.
        final Collection registeredRecoveryAgentsValues = _registeredRecoveryAgents.values();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "work with RA values: " + registeredRecoveryAgentsValues + ", collection size: " + registeredRecoveryAgentsValues.size(), this);
        Iterator registeredRecoveryAgentsValuesIterator = registeredRecoveryAgentsValues.iterator();
        while (registeredRecoveryAgentsValuesIterator.hasNext()) {
            // Extract the next ArrayList and create an iterator from it. This iterator will return RecoveryAgent
            // objects that are registered at the same sequence priority value.
            final ArrayList registeredRecoveryAgentsArray = (java.util.ArrayList) registeredRecoveryAgentsValuesIterator.next();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "work with Agents array: " + registeredRecoveryAgentsArray + ", of size: " + registeredRecoveryAgentsArray.size(), this);
            final Iterator registeredRecoveryAgentsArrayIterator = registeredRecoveryAgentsArray.iterator();

            while (registeredRecoveryAgentsArrayIterator.hasNext()) {
                // Extract the next RecoveryAgent object
                final RecoveryAgent recoveryAgent = (RecoveryAgent) registeredRecoveryAgentsArrayIterator.next();

                //TODO: This is a bit hokey. Can we safely assume that there is just the one RecoveryAgent in a Liberty environment?
                libertyRecoveryAgent = recoveryAgent;

                String recoveryGroup = libertyRecoveryAgent.getRecoveryGroup();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "work with Agent: " + recoveryAgent + " and recoveryGroup " + recoveryGroup, this);
                peersToRecover = recoveryAgent.processLeasesForPeers(localFailureScope.serverName(), recoveryGroup);
            }
        }

        if (peersToRecover != null && !peersToRecover.isEmpty() && libertyRecoveryAgent != null)
            peerRecoverServers(libertyRecoveryAgent, localFailureScope.serverName(), peersToRecover);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "drivePeerRecovery");
    }

    @FFDCIgnore({ RecoveryFailedException.class })
    public synchronized void peerRecoverServers(RecoveryAgent recoveryAgent, String myRecoveryIdentity, ArrayList<String> peersToRecover) throws RecoveryFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "peerRecoverServers", new Object[] { recoveryAgent, myRecoveryIdentity, peersToRecover });

        for (String peerRecoveryIdentity : peersToRecover) {

            try {
                //Read lease check if it is still expired. If so, then update lease and proceed to peer recover
                // if not still expired (someone else has grabbed it) then bypass peer recover.
                LeaseInfo leaseInfo = new LeaseInfo();
                if (recoveryAgent.claimPeerLeaseForRecovery(peerRecoveryIdentity, myRecoveryIdentity, leaseInfo)) {

                    // drive directInitialization(**retrieved scope**);
                    Tr.audit(tc, "WTRN0108I: " +
                                 "PEER RECOVER server with recovery identity " + peerRecoveryIdentity);
                    //String peerServerName = "Cell\\Node\\cloud002";
                    FileFailureScope peerFFS = new FileFailureScope(peerRecoveryIdentity, leaseInfo);

                    directInitialization(peerFFS);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to claim lease for peer", this);
                }
            } catch (RecoveryFailedException rfexc) {
                Tr.audit(tc, "WTRN0108I: " +
                             "HADB Peer locking failed for server with recovery identity " + peerRecoveryIdentity);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "peerRecoverServers", rfexc);
                throw rfexc;
            } catch (Exception exc) {
                Tr.audit(tc, "WTRN0108I: " +
                             "HADB Peer locking failed for server with recovery identity " + peerRecoveryIdentity + " with exception " + exc);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "peerRecoverServers", exc);
                throw new RecoveryFailedException(exc);
            }

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "peerRecoverServers");
    }

    @Override
    public void setRecoveryLogFactory(RecoveryLogFactory fac) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setRecoveryLogFactory, factory: " + fac, this);
        theRecoveryLogFactory = fac;

        if (theRecoveryLogFactory != null) {
            String className = theRecoveryLogFactory.getClass().getName();
            _customLogFactories.put(className, theRecoveryLogFactory);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RecoveryDirectorImpl: setting RecoveryLogFactory, " + theRecoveryLogFactory + "for classname, " + className);
        } else if (tc.isDebugEnabled())
            Tr.debug(tc, "RecoveryDirectorImpl: the RecoveryLogFactory is null");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "RecoveryDirectorImpl", this);

    }
}
