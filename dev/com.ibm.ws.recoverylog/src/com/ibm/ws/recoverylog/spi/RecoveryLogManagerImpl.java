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

import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: RecoveryLogManagerImpl
//------------------------------------------------------------------------------
/**
 * <p>
 * The RecoveryLogManagerImpl class implements the RecoveryLogManager interface
 * and provides support for access to the recovery logs associated with a client
 * service.
 * </p>
 *
 * <p>
 *
 * An instance of the RecoveryLogManagerImpl class is provided to each client
 * service when it registers with the RecoveryDirector.
 * </p>
 */
public class RecoveryLogManagerImpl implements RecoveryLogManager {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(RecoveryLogManagerImpl.class,
                                                         TraceConstants.TRACE_GROUP, null);

    /**
     * The name of the client service that owns the RecoveryLogManager instance.
     */
    private final String _clientName;

    /**
     * The identity of the client service that owns the RecoveryLogManager instance.
     */
    private final int _clientIdentity;

    /**
     * The version of the client service that owns the RecoveryLogManager instance.
     */
    private final int _clientVersion;

    /**
     * The RecoveryAgent object supplied by the client service that owns the
     * RecoveryLogManager instance
     */
    private final RecoveryAgent _recoveryAgent;
    /* @PK01151D */
    /**
     * A record of the RecoveryLogs accessed by the client service for each FailureScope. The
     * initial map is keyed from FailureScope to a secondary map which is keyed from log
     * identifier to RecoveryLog. The log identifier is obtained from the LogProperties object
     * when a recovery log is accessed.
     *
     * This map contains only those RecoveryLog objects that have been accessed during the
     * current server run. It does not reflect all those recovery logs that have been accessed
     * during the current server deployment.
     *
     * <p>
     * <ul>
     * <li>java.util.HashMap (FailureScope -> java.util.HashMap (Recovery Log Identifier -> RecoveryLog))
     * </ul>
     */
    private final java.util.HashMap<Integer, HashMap<FailureScope, RecoveryLog>> _recoveryLogs;

    /**
     * This map is used in conjuction with _recoveryLogs to support more than one failure scope
     * in a single file based recovery log. This map stores MultiScopeRecoveryLog entiries
     * keyed from the log identifier. If, when we access _recoveryLogs, we cant find an existing
     * recovery log for the FailureScope and Recovery Log Identifier combination, we can check
     * to see if there is a MultiScopeRecoveryLog for the RLI. If there is then this can be
     * re-used for the new failure scope as the log type supports this. In this case, we don't
     * create a new RecoveryLog to put into _recoveryLogs, we use the one we found in
     * _multiScopeRecoveryLogs.
     *
     * <p>
     * <ul>
     * <li>java.util.HashMap (Recovery Log Identifier -> MultiScopeRecoveryLog)
     * </ul>
     */
    private final HashMap<Integer, HashMap<String, MultiScopeLog>> _multiScopeRecoveryLogs;

    /**
     * Set of LogFactories determined at runtime via eclipse plugin mechanism
     * passed via c'tor from RecoveryDirector and common to all RecoveryLogManagerImpl's
     */
    private final HashMap<String, RecoveryLogFactory> _customLogFactories;

    /**
     * The 'traceId' string is output at key trace points to allow easy mapping
     * of recovery log operations to clients logs.
     */
    private String _traceId;

    //------------------------------------------------------------------------------
    // Method: RecoveryLogManagerImpl.RecoveryLogManagerImpl
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Package access constructor for the creation of RecoveryLogManagerImpl objects.
     * </p>
     *
     * <p>
     * This method should only be called by the RecoveryDirector class in response to a
     * RecoveryDirector.registerService() call.
     * </p>
     *
     * <p>
     * Creates a new RecoveryLogManagerImpl object to handle access to the recovery
     * logs associated with the client service that owns the supplied RecoveryAgent.
     * </p>
     *
     * @param recoveryAgent The RecoveryAgent instance.
     */
    RecoveryLogManagerImpl(RecoveryAgent recoveryAgent, HashMap<String, RecoveryLogFactory> customLogFactories) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "RecoveryLogManagerImpl", new Object[] { recoveryAgent, customLogFactories });

        // Cache the supplied information
        _recoveryAgent = recoveryAgent;
        _customLogFactories = customLogFactories;

        // Determine the client identity
        _clientName = _recoveryAgent.clientName();
        _clientVersion = _recoveryAgent.clientVersion();
        _clientIdentity = _recoveryAgent.clientIdentifier();

        // Allocate the map which will contain all registered recovery logs
        _recoveryLogs = new HashMap<Integer, HashMap<FailureScope, RecoveryLog>>();
        _multiScopeRecoveryLogs = new HashMap<Integer, HashMap<String, MultiScopeLog>>();
        /* @PK01151D */

        if (tc.isEntryEnabled())
            Tr.exit(tc, "RecoveryLogManagerImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLogManagerImpl.getRecoveryLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Returns a RecoveryLog that can be used to access a specific recovery log.
     * </p>
     *
     * <p>
     * Each recovery log is contained within a FailureScope. For example, the
     * transaction service on a distributed system has a transaction log in each
     * server node (ie in each FailureScope). Because of this, the caller must
     * specify the FailureScope of the required recovery log.
     * </p>
     *
     * <p>
     * Additionally, the caller must specify information regarding the identity and
     * physical properties of the recovery log. This is done through the LogProperties
     * object provided by the client service.
     * </p>
     *
     * @param failureScope  The required FailureScope
     * @param logProperties Contains the identity and physical properties of the
     *                          recovery log.
     *
     * @return The RecoveryLog instance.
     *
     * @exception InvalidLogPropertiesException The RLS does not recognize or cannot
     *                                              support the supplied LogProperties
     */
    @Override
    public synchronized RecoveryLog getRecoveryLog(FailureScope failureScope, LogProperties logProperties) throws InvalidLogPropertiesException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoveryLog", new java.lang.Object[] { failureScope, logProperties, this });
        /* 5@PK01151D */
        // If we're on Z, we can have a ZLogProperties (System Logger) based
        // recovery log.  Otherwise, FileLogProperties and CustomLogProperties are the only supported types.
        if (logProperties instanceof StreamLogProperties) {
            // final PlatformHelper ph = PlatformHelperFactory.getPlatformHelper();
            // if (ph.isZOS() == false)
            if (Configuration.isZOS() == false) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unable to create stream based recovery log on non-ZOS platform"); /* @LIDB2561.1A */
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getRecoveryLog");
                throw new InvalidLogPropertiesException();
            }
        } else if (!(logProperties instanceof FileLogProperties || logProperties instanceof CustomLogProperties)) {
            if (tc.isEventEnabled())
                Tr.event(tc, "Unable to create non-file based or non-Custom recovery log");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getRecoveryLog");
            throw new InvalidLogPropertiesException();
        }

        final int logIdentifier = logProperties.logIdentifier();

        RecoveryLog recoveryLog = null;

        // Extract all of the logs that are currently available for the given log
        // identifier.
        HashMap<FailureScope, RecoveryLog> logsByFailureScope = _recoveryLogs.get(logIdentifier);

        if (logsByFailureScope != null) {
            // One or more logs for the given identifier is already available.
            // See if any of these logs are for the given failure scope.
            recoveryLog = logsByFailureScope.get(failureScope);
        } else {
            // There were no logs for the given identifier. Initialize the
            // hashmap for this identifier and add it to the outer map so
            // that it can be used to store the log that will be created
            // below.
            logsByFailureScope = new HashMap<FailureScope, RecoveryLog>();
            _recoveryLogs.put(logIdentifier, logsByFailureScope);
        }

        if (recoveryLog == null) {
            if (logProperties instanceof FileLogProperties) {
                // The requested log has not been accessed already so we must create it.
                MultiScopeLog multiScopeRecoveryLog = null;
                HashMap<String, MultiScopeLog> multiScopeLogsByServerName = null; /* @253893A */
                String serverName = failureScope.serverName(); /* @253893A */

                final FileLogProperties fileLogProperties = (FileLogProperties) logProperties;

                // File-based recovery logs have two different types. A multiple
                // scope log type indicates that records for more than one failure
                // scope may be stored in the same log. A single scope log type
                // indicates that records for different failure scopes must be
                // stored in different logs.
                final int logType = fileLogProperties.logType();

                if (logType == FileLogProperties.LOG_TYPE_MULTIPLE_SCOPE) {
                    // If this is a multiple scope log then we can check to see if
                    // we have an existing log for the given identifier. If we do
                    // we can re-use it.  A multi-scope log will only contain failure
                    // scopes that share a server name.  In an HA-enabled environment
                    // it's possible for a log identifier to have more than one
                    // log.
                    multiScopeLogsByServerName = _multiScopeRecoveryLogs.get(logIdentifier); /* @253893C */

                    if (multiScopeLogsByServerName != null) /* @253893A */
                    { /* @253893A */
                        multiScopeRecoveryLog = multiScopeLogsByServerName.get(serverName); /* @253893A */
                    } /* @253893A */
                    else /* @253893A */
                    { /* @253893A */
                        multiScopeLogsByServerName = new HashMap<String, MultiScopeLog>(); /* @253893A */
                        _multiScopeRecoveryLogs.put(logIdentifier, multiScopeLogsByServerName); /* @253893A */
                    } /* @253893A */
                }

                if (multiScopeRecoveryLog == null) {
                    // Either a single scope log is required or there was no
                    // existing log for the given identifier. Create a new log.
                    multiScopeRecoveryLog = new MultiScopeRecoveryLog(fileLogProperties, _recoveryAgent, failureScope);

                    if (logType == FileLogProperties.LOG_TYPE_MULTIPLE_SCOPE) {
                        // If this is a multiple scope log then we store it in the map so
                        // that it can be re-used by subsequent requests for a log with the
                        // same log identifier.
                        //
                        // It is worth noting that in an environment where the only logs
                        // created are single scope logs this map will
                        // remain empty.
                        multiScopeLogsByServerName.put(serverName, multiScopeRecoveryLog); /* @253893A */
                    }
                }

                // Create a new RecoveryLog object to be returned to the caller and
                // store it in the map so that it can be re-used if necessary.
                recoveryLog = new RecoveryLogImpl(multiScopeRecoveryLog, failureScope);
            } else if (logProperties instanceof CustomLogProperties) {
                // The requested log has not been accessed already so we must create it.
                MultiScopeLog multiScopeRecoveryLog = null;
                HashMap<String, MultiScopeLog> multiScopeLogsByServerName = null; /* @253893A */
                String serverName = failureScope.serverName(); /* @253893A */

                final CustomLogProperties customLogProperties = (CustomLogProperties) logProperties;

                // Custom recovery logs have two different types. A multiple
                // scope log type indicates that records for more than one failure
                // scope may be stored in the same log. A single scope log type
                // indicates that records for different failure scopes must be
                // stored in different logs.
                final int logType = customLogProperties.logType();

                if (logType == CustomLogProperties.LOG_TYPE_MULTIPLE_SCOPE) {
                    // If this is a multiple scope log then we can check to see if
                    // we have an existing log for the given identifier. If we do
                    // we can re-use it.  A multi-scope log will only contain failure
                    // scopes that share a server name.  In an HA-enabled environment
                    // it's possible for a log identifier to have more than one
                    // log.
                    multiScopeLogsByServerName = _multiScopeRecoveryLogs.get(logIdentifier); /* @253893C */

                    if (multiScopeLogsByServerName != null) /* @253893A */
                    { /* @253893A */
                        multiScopeRecoveryLog = multiScopeLogsByServerName.get(serverName); /* @253893A */
                    } /* @253893A */
                    else /* @253893A */
                    { /* @253893A */
                        multiScopeLogsByServerName = new HashMap<String, MultiScopeLog>(); /* @253893A */
                        _multiScopeRecoveryLogs.put(logIdentifier, multiScopeLogsByServerName); /* @253893A */
                    } /* @253893A */
                }

                if (multiScopeRecoveryLog == null) {
                    // Either a single scope log is required or there was no
                    // existing log for the given identifier. Create a new log.

                    // Need to locate the factory for this Custom log via RecoveryDirector
                    // then create a log ... this could be a single or multiple scope log at this point
                    // check the type of returned log and if necessary wrap in a RecoveryLogImpl
                    final String customLogId = customLogProperties.pluginId();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Look in properties with customLogId, " + customLogId);
                    RecoveryLogFactory factory = _customLogFactories.get(customLogId);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Retrieved factory, " + factory);

                    if (factory == null) {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Custom recovery log factory NOT FOUND for ", customLogId);

                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "getRecoveryLog");
                        throw new InvalidLogPropertiesException();
                    }

                    recoveryLog = factory.createRecoveryLog(customLogProperties, _recoveryAgent, Configuration.getRecoveryLogComponent(), failureScope);
                    if (recoveryLog == null) {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Custom recovery log factory returned NULL recovery log", customLogId);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "getRecoveryLog");
                        throw new InvalidLogPropertiesException();
                    }

                    if (logType == CustomLogProperties.LOG_TYPE_MULTIPLE_SCOPE && recoveryLog instanceof MultiScopeLog) {
                        // If this is a multiple scope log then we store it in the map so
                        // that it can be re-used by subsequent requests for a log with the
                        // same log identifier.
                        //
                        // It is worth noting that in an environment where the only logs
                        // created are single scope logs this map will
                        // remain empty.
                        multiScopeRecoveryLog = (MultiScopeLog) recoveryLog;
                        multiScopeLogsByServerName.put(serverName, multiScopeRecoveryLog); /* @253893A */

                        recoveryLog = new RecoveryLogImpl(multiScopeRecoveryLog, failureScope);
                    }
                } else {
                    // Create a new RecoveryLog object to be returned to the caller and
                    // store it in the map so that it can be re-used if necessary.
                    recoveryLog = new RecoveryLogImpl(multiScopeRecoveryLog, failureScope);
                }
            } else {
                // This is a stream log properties object so create
                // the z-specific log - use reflection to do this
                // to avoid a compile and runtime dependency on
                // the z-specific code.
                // TDK - IXGRecoveryLogImpl is in the same component....
                try {
                    final Constructor<?> ixgLogConstructor = Class.forName("com.ibm.ws390.recoverylog.spi.IXGRecoveryLogImpl").getConstructor(new Class[] {
                                                                                                                                                            com.ibm.ws.recoverylog.spi.FailureScope.class,
                                                                                                                                                            com.ibm.ws.recoverylog.spi.StreamLogProperties.class,
                                                                                                                                                            com.ibm.ws.recoverylog.spi.RecoveryAgent.class });
                    recoveryLog = (RecoveryLog) ixgLogConstructor.newInstance(new Object[] { failureScope, (StreamLogProperties) logProperties, _recoveryAgent });
                } catch (Exception e) {
                    FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.RecoveryLogManagerImpl.getRecoveryLog", "278", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Exception caught initializing stream-based log", e);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getRecoveryLog", "InvalidLogPropertiesException");
                    throw new InvalidLogPropertiesException(e);
                }
            }

            logsByFailureScope.put(failureScope, recoveryLog);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoveryLog", recoveryLog);
        return recoveryLog;
    }

    @Override
    public SharedServerLeaseLog getLeaseLog(String localRecoveryIdentity, String recoveryGroup, int leaseCheckInterval, String leaseCheckStrategy,
                                            int leaseLength, LogProperties logProperties) throws InvalidLogPropertiesException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getLeaseLog",
                     new java.lang.Object[] { localRecoveryIdentity, recoveryGroup, leaseCheckInterval, leaseCheckStrategy, leaseLength, logProperties, this });

        SharedServerLeaseLog leaseLog = null;
        CustomLogProperties customLogProperties = null;

        if (logProperties instanceof CustomLogProperties) {
            //TODO: Use recoveryGroup in RDBMS implementation
            customLogProperties = (CustomLogProperties) logProperties;
            // Need to locate the factory for this Custom log. We use the same factory as for the SQLMultiScopeRecoveryLog
            final String customLogId = customLogProperties.pluginId();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Look in properties with customLogId, " + customLogId);
            RecoveryLogFactory factory = _customLogFactories.get(customLogId);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Retrieved factory, " + factory);
            if (factory == null) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Custom recovery log factory NOT FOUND for ", customLogId);

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getLeaseLog");
                throw new InvalidLogPropertiesException();
            }

            leaseLog = factory.createLeaseLog(customLogProperties);
            if (leaseLog == null) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Custom recovery log factory returned NULL lease log", customLogId);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getLeaseLog");
                throw new InvalidLogPropertiesException();
            }
        } else if (logProperties instanceof FileLogProperties) {
            // Set up FileLogProperites
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Found FileLogProperties");

            FileLogProperties fileLogProperties = (FileLogProperties) logProperties;

            String logDirStem = fileLogProperties.logDirectoryStem();

            // If necessary, create a new RecoveryLog object to be returned to the caller.
            leaseLog = FileSharedServerLeaseLog.getFileSharedServerLeaseLog(logDirStem, localRecoveryIdentity, recoveryGroup);
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getLeaseLog");
            throw new InvalidLogPropertiesException();
        }

        leaseLog.setPeerRecoveryLeaseTimeout(leaseLength);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getLeaseLog", leaseLog);
        return leaseLog;
    }

    /* 15@PK01151D */

    //------------------------------------------------------------------------------
    // Method: RecoveryLogManagerImpl.toString
    //------------------------------------------------------------------------------
    /**
     * Returns the string representation of this object instance.
     *
     * @return String The string representation of this object instance.
     */
    @Override
    public String toString() {
        if (_traceId == null)
            // Now establish a 'traceId' string. This is output at key trace points to allow
            // easy mapping of recovery log operations to clients logs.
            _traceId = "RecoveryLogManagerImpl:" + "clientName=" + _clientName + ":"
                       + "clientVersion=" + _clientVersion + ":"
                       + "clientIdentity=" + _clientIdentity + " @"
                       + System.identityHashCode(this);

        return _traceId;
    }
}
