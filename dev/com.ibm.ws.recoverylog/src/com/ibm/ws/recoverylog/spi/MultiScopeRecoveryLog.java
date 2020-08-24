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

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.utils.DirUtils;
import com.ibm.ws.recoverylog.utils.RecoverableUnitIdTable;

//------------------------------------------------------------------------------
// Class: MultiScopeRecoveryLog
//------------------------------------------------------------------------------
/**
 * <p>
 * The MultiScopeRecoveryLog class implements the DistributedRecoveryLog interface and
 * provides support for controling a specific recovery log on behalf of a client
 * service.
 * </p>
 *
 * <p>
 * This class provides facilities for opening and closing the recovery log, as
 * well as access to the underlying RecoverableUnits from which it
 * is comprised.
 * </p>
 *
 * <p>
 * This class implements DistributedRecoveryLog rather than directly implementing
 * RecoveryLog. This intermediate interface provides additional support for
 * "service data". Service data is a byte array whose contents and size is not
 * defined by the RLS. It is supplied by the client serivce and associated with
 * the recovery log.
 * </p>
 *
 * <p>
 * This class also implements the LogCursorCallback interface. This interface
 * allows an instance of MultiScopeRecoveryLog to be notified when the client service
 * invokes remove on a LogCursor created by this class. This is required in order
 * to allow this class to write corrisponding deletion records to the recovery
 * log.
 * </p>
 */
public class MultiScopeRecoveryLog implements LogCursorCallback, MultiScopeLog {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(MultiScopeRecoveryLog.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * Constant to define the default recovery log size (specified in kilobytes)
     */
    private static int DEFAULT_LOGFILE_SIZE = 1024;

    /**
     * Constant to define the minimum recovery log size (specified in kilobytes)
     */
    private static final int MIN_LOGFILE_SIZE = 8;

    /**
     * Constants to identify specific lock get/set pairs. These values must be
     * unique within the RLS code. Rather than use any form of allocation method
     * for these values components use statically defined constants to save
     * CPU cycles. All instances throughout the RLS source should be prefixed
     * with LOCK_REQUEST_ID to assist location and help ensure that no repeats
     * are introduced by mistake. This comment should also be included whereever
     * these values are defined.
     */
    private static final int LOCK_REQUEST_ID_MSRL_CREATE = 1;
    private static final int LOCK_REQUEST_ID_MSRL_REMOVE = 2;

    /**
     * Constant indicating that the recovery log is "memory backed" rather than "file
     * backed". When using memory backed storage, all active information written to
     * the recovery log is retained in memory in addition to being written to disk during force
     * operations. When using file backed storage the in memory copy is discarded once the
     * data has been written to persistant storage. Subsequent retrieval of the information
     * (eg during keypoint) will require a disk read operation.
     */
    protected static final int MEMORY_BACKED = 1;

    /**
     * Constant indicating that the recovery log is "file backed" rather than "memory
     * backed" (see above)
     */
    protected static final int FILE_BACKED = 2;

    /**
     * Constant indicating the maximum size recovery log that will be allowed to use
     * memory backed storage (specified in kilobytes). Recovery logs with maximum sizes
     * smaller than this value will use memory backed storage. Recovery logs with miximum
     * sizes greater than or equal to this value will use file backed storage.
     */
    protected static final int IN_MEMORY_MAXIMUM = 5120;

    /**
     * The next two parameters are used to control the resize behaviour of a recovery
     * log. The information retained in a recovery log has a well defined lifecycle -
     * it is added, written to disk and eventually deleted. The total amount of
     * information that must be retained at any one time is referered to as the "active"
     * data. At the same time as new data is being added, older data is being deleted
     * so two key properties of a recovery log are its data throughput and its active
     * data size. The recovery log consists of two physical disk files of which one
     * at a time is used to hold the data written to disk. As information is deleted
     * from the in memory model of the recovery log, a record of these deletions is
     * also written to disk. For example, if we write records R1, R2 and R3 we have
     * a log file contents as follows:-
     *
     * [ R1 R2 R3 ... ]
     *
     * Now if we delete R2, add R4, delete R1 and finally add R5 and R6 we have
     * a log file contents as follows:-
     *
     * [ R1 R2 R3 D2 R4 D1 R5 R6 ... ]
     *
     * Since the file holding this information is a set size, eventually there will
     * be insufficient space to write additional records. When this occurs a "keypoint"
     * operation is driven to re-write all active data into the unused file. This then
     * becomes the target file into which new records are written. After a keypoint,
     * the new file will contain the following content :-
     *
     * [ R3 R4 R5 R6 ... ]
     *
     * This process operates fine until the active data (eg R3+R4+R5+R6) in the above
     * example is too large to fit within the log file. When this occurs, the file size
     * must be increased and these two parameters define when and how this resize
     * occurs.
     *
     * Once the amount of active data being held exceeds the TOTAL_DATA_RESIZE_TRIGGER
     * it will be resized to (active data size) * TOTAL_DATA_RESIZE_MULTIPLIER.
     */
    private static float TOTAL_DATA_RESIZE_TRIGGER = 0.95f;
    private static float TOTAL_DATA_RESIZE_MULTIPLIER = 1.25f;

    /**
     * The RecoveryAgent object supplied by the client service that owns
     * the recovery log.
     */
    private final RecoveryAgent _recoveryAgent;

    /**
     * The name of the client service that owns the recovery log.
     */
    private final String _clientName;

    /**
     * The version number of the client service that owns the recovery log.
     */
    private final int _clientVersion;

    /**
     * The name under which the MultiScopeRecoveryLog was registered.
     */
    private final String _logName;

    /**
     * The identity under which the MultiScopeRecoveryLog was registered.
     */
    private final int _logIdentifier;

    /**
     * The name of the application server for which the recovery log has
     * been created.
     */
    private final String _serverName;

    /**
     * The directory path under which the files that make up this recovery log will
     * be stored.
     */
    private String _logDirectory;

    /**
     * The size of this recovery log in kilobytes.
     */
    private int _logFileSize;

    /**
     * The maximum log file size in kilobytes
     */
    private int _maxLogFileSize;

    /**
     * A map of recoverable units. Each recoverable unit is keyed by its identity.
     */
    private HashMap<Long, RecoverableUnit> _recoverableUnits;

    /**
     * The LogHandle class provides facilities for buffering data and then writing
     * that data to disk as required. The LogHandle class does this in a generic
     * fashion without exposing the specifics of the underlying buffering / file
     * access mechanism through its interface.
     */
    private LogHandle _logHandle;

    /**
     * Lock to control access to the data structures that make up the in memory log.
     * Operations that wish to modify the internal data structures need to obtain a
     * 'shared' lock on _controlLock. The keypoint operation must first obtain a
     * 'shared' lock and then an 'exclusive' lock.
     */
    private final Lock _controlLock;

    /**
     * Counter to track the number of times the recovery log must be closed by the
     * client service before the underlying log will actually be keypointed and
     * closed. This value is incremented by one each time the log is opened and
     * decremented by one each time the log is closed. When the counter reaches
     * zero during a close call, the underlying log is keypointed and closed. This
     * provides support for multiple open and closed calls to the same recovery
     * log by the client service.
     */
    private int _closesRequired;

    /**
     * Cached copy of the system file separator character
     */
    private static String _fileSeparator;

    /**
     * A reference to the FileLogProperties object that defines the identity and physical
     * location of the recovery log.
     */
    private final FileLogProperties _fileLogProperties;

    /**
     * The size, in bytes, required to write all of this RecoveryLog's unwritten
     * RecoverableUnits to persistent storage.
     */
    private final AtomicInteger _unwrittenDataSize = new AtomicInteger();

    /**
     * The size, in bytes, required to write, or rewrite, all of this RecoveryLog's
     * RecoverableUnits to persistent storage. This includes both the unwritten
     * RecoverableUnits and those that have already been output to persistent storage.
     */
    private int _totalDataSize;

    /**
     * The storage mode in use by this recovery log. Default is memory backed.
     */
    private int _storageMode = MEMORY_BACKED;

    /**
     * Flag indicating that the recovery log has suffered an internal error
     * that leaves the recovey log in an undefined or unmanageable state.
     * Once this flag is set, any interface method that could modify the disk
     * state is stopped with an InternalLogException. This protects the on-disk
     * information from damage and allows a subseqent server bounce to recover
     * correctly.
     */
    private boolean _failed;

    /**
     * Flag indicating that the recovery log represented by this instance is at
     * an incompatible level to this service. Once this flag is set, any interface
     * method that accesses the recovery log will be stopped with a
     * LogIncompatibleException.
     */
    private boolean _incompatible;

    /**
     * Flag to indicate that a warning message has been issued to the associated
     * RecoveryAgent indicating that the log is filling up. The notification
     * is issued once when the threshold defined by
     * _totalDataSize > LOG_WARNING_FACTOR * (_maxLogFileSize - _totalDataSize)
     * is exceeded.
     */
    private boolean _logWarningIssued;

    private static final int LOG_WARNING_FACTOR = 3; // warning issued at 75% full

    /**
     * A RecoverableUnitId table used to assign the ID of each newly created
     * recoverable unit when no identity is specified on createRecoverableUnit
     */
    private final RecoverableUnitIdTable _recUnitIdTable = new RecoverableUnitIdTable();

    /**
     * The 'traceId' string is output at key trace points to allow easy mapping
     * of recovery log operations to clients logs.
     */
    private String _traceId;

    /**
     * Flag to indicate if the containment check should be bypassed inside the
     * recoverableUnits() method.
     */
    private final boolean _bypassContainmentCheck;

    /**
    */
    FailureScope _failureScope;
    volatile MultiScopeLog _associatedLog = null;
    volatile boolean _failAssociatedLog = false;

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.MultiScopeRecoveryLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Package access constructor for the creation of MultiScopeRecoveryLog objects.
     * </p>
     *
     * <p>
     * This method should only be called by the RecoveryLogManager class in response
     * to a <code>RecoveryLogManager.getRecoveryLog</code> call.
     * </p>
     *
     * <p>
     * Additionally, the caller must provide a LogProperties object that defines
     * the identity and physical properties of the recovery log. Since this implementation
     * of the RecoveryLog interface currently only supports "file" based recovery
     * logs, the supplied object must be of type FileLogProperties.
     * </p>
     *
     * @param fileLogProperties The identity and physical properties of the recovery log.
     * @param recoveryAgent The RecoveryAgent of the associated client service.
     * @param fs The FailureScope of the associated client service.
     */
    MultiScopeRecoveryLog(FileLogProperties fileLogProperties, RecoveryAgent recoveryAgent, FailureScope fs) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "MultiScopeRecoveryLog", new Object[] { fileLogProperties, recoveryAgent, fs });

        // Cache the supplied information
        _fileLogProperties = fileLogProperties;
        _recoveryAgent = recoveryAgent;

        // Extract further information about the identity and physical location of the required
        // recovery log.
        _logName = _fileLogProperties.logName();
        _logIdentifier = _fileLogProperties.logIdentifier();
        _logDirectory = _fileLogProperties.logDirectory();
        _logFileSize = _fileLogProperties.logFileSize();
        _maxLogFileSize = _fileLogProperties.maxLogFileSize();
        _clientName = recoveryAgent.clientName();
        _clientVersion = recoveryAgent.clientVersion();
        _serverName = fs.serverName();
        _failureScope = fs;

        // Lookup the system file separator character needed to build path names
        if (_fileSeparator == null) {
            _fileSeparator = java.security.AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty("file.separator");
                }
            });
        }

        // Ensure that if the physical location has been left unspecified (field is null) it is updated
        // with the correct default.
        if (_logDirectory == null) {
            _logDirectory = Configuration.WASInstallDirectory() + _fileSeparator + "recoveryLogs" + _fileSeparator +
                            DirUtils.createDirectoryPath(_serverName) + _fileSeparator + _clientName + _fileSeparator +
                            _logName;
        }

        // Ensure that if the physical size has been left unspecified or is invalid it is updated with
        // the correct default.
        if (_logFileSize == 0) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Using default values for log file size and maximum size");
            _logFileSize = DEFAULT_LOGFILE_SIZE;
            _maxLogFileSize = DEFAULT_LOGFILE_SIZE;
        }

        // Ensure that the log file size is at least as big as the minimum allowable size.
        if (_logFileSize < MIN_LOGFILE_SIZE) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Log file size is too small. Enforcing minimum size");
            _logFileSize = MIN_LOGFILE_SIZE;
        }

        // Ensure that the log file maximum size is at least as big as the minimum allowable
        // size.
        if (_maxLogFileSize < MIN_LOGFILE_SIZE) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Log file maximum size is too small. Enforcing minimum size");
            _maxLogFileSize = MIN_LOGFILE_SIZE;
        }

        // Ensure that the log file size does not exceed the maximum.
        if (_logFileSize > _maxLogFileSize) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Log file size is greater than maximum. Constraining log file size");
            _logFileSize = _maxLogFileSize;
        }

        // Define a free form identity for this lock object. The lock object will output this string within its
        // trace.
        String lockId = "serverName=" + _serverName + ":"
                        + "clientName=" + _clientName + ":"
                        + "clientVersion=" + _clientVersion + ":"
                        + "logName=" + _logName + ":"
                        + "logIdentifier=" + _logIdentifier;

        // Create a control lock for this recovery log instance. The control lock will be
        // used to coordinate general access to and keypoint of the recovery log.
        _controlLock = new Lock(lockId);

        // Choose the storage mode for the recovey log. This can be either "memory backed" or
        // "file backed". When using memory backed storage, all active information written to
        // the recovery log is retained in memory in addition to being written to disk during force
        // operations. When using file backed storage the in memory copy is discarded once the
        // data has been written to persistant storage. Subsequent retrieval of the information
        // (eg during keypoint) will require a disk read operation. We select file backed storage
        // mode if the maximum log size is greater than or equal to our limit value.
        if (_maxLogFileSize >= IN_MEMORY_MAXIMUM) {
            _storageMode = FILE_BACKED;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Using a 'retain only on disk' model for forced log data");
        } else {
            _storageMode = MEMORY_BACKED;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Using a 'retain in memory' model for forced log data");
        }

        // Now output consolidated trace information regarding the configuration of this object.
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Recovery log belongs to server " + _serverName);
            Tr.debug(tc, "Recovery log created by client service " + _clientName + " at version " + _clientVersion);
            Tr.debug(tc, "Recovery log name is " + _logName);
            Tr.debug(tc, "Recovery log identifier is " + _logIdentifier);
            Tr.debug(tc, "Recovery log directory is " + _logDirectory);
            Tr.debug(tc, "Recovery log file size is " + _logFileSize);
            Tr.debug(tc, "Recovery log file size is " + _maxLogFileSize);
        }

        // Now we require a temporary solution to support the manual movement of a
        // recovery log file set from server a to server b on distributed. Since
        // the multi-scope recovery log can actually support the storage of records
        // from any number of servers there would be no logical owner of a given log
        // if we made use of this facility on distributed (currently only
        // do on z/OS). Since we make no use of this facility, we can assume that
        // the 'creator server name' in the header of the log is the log owner
        // and that there is no real need to enforce the failure scope check
        // in the recoverableUnits method. We must disable these checks in this
        // environment to ensure that this form of peer recovery is can operate.
        _bypassContainmentCheck = (!Configuration.HAEnabled() && (!Configuration.isZOS()));

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_bypassContainmentCheck = " + _bypassContainmentCheck);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "MultiScopeRecoveryLog", this);
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.openLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Open the recovery log. Before a recovery log may be used, it must be opened by
     * the client service. The first time a recovery log is opened, any data stored
     * on disk will be used to reconstruct the RecoverableUnits and
     * RecoverableUnitSections that were active when the log was closed or the server
     * failed. The recovery log service guarentees to recover any information that was
     * forced to disk (RecoverableUnit.forceSections or RecoverableUnitSection.force).
     * Information that was written to disk (RecoverableUnit.writeSections or
     * RecoverableUnitSection.write) may be recovererd. Finally, information that was
     * not written or forced will not be recovered.
     * </p>
     *
     * <p>
     * The client service may issue any number of openLog calls, but each must be
     * paired with a corrisponding closeLog call. This allows common logic to be
     * executed on independent threads without any need to determine if a
     * recovery log is already open. For example, a recovery process may be in
     * progress on one thread whilst forward processing is being performed on
     * another. Both pieces of logic may issue their own openLog and closeLog
     * calls independently.
     * </p>
     *
     * @exception LogCorruptedException The recovery log has become corrupted and
     *                cannot be opened.
     * @exception LogAllocationException The recovery log could not be created.
     * @exception InternalLogException An unexpected failure has occured.
     */
    @Override
    public synchronized void openLog() throws LogCorruptedException, LogAllocationException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "openLog", this);

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "InternalLogException");
            throw new InternalLogException(null);
        }

        // If this is the first time the recovery log has been opened during the current server run, then
        // allocate a new LogHandle to manage the underlying recovery log and retrieve the information stored
        // on disk, otherwise just return directly to the caller (after incrementing the closes required counter)
        if (_logHandle == null) {
            // Create the LogHandle which will manage the physical log files.
            _logHandle = new LogHandle(this, _clientName, _clientVersion, _serverName, _logName, _logDirectory, _logFileSize, _maxLogFileSize, _failureScope);

            // Allocate the map which holds the RecoverableUnit objects
            _recoverableUnits = new HashMap<Long, RecoverableUnit>();

            try {
                // Open the log and restore the logged objects. As long as the basic header information at the front of the
                // target log file is formatted correctly, the code behind openLog will tollerate a single corruption at any
                // point through the reload process. If a corruption is detected then the read will stop and recovery will
                // take place with the information read upto that point. The rational behind this is that the recovery log
                // is designed to cope with corruption that occurs due to a system failure (eg power failure) where the corrption
                // will actually occur at the end of the file. In this case the code which attempted to force the data to
                // disk will not have regained control, so it is safe to ignore the corrupted data. The recovery log is
                // NOT designed to be able to recovery if arbitary corrption (eg user has damaged the file manually) has
                // occured. In such cases the recovery log will not open and this method will generate a LogCorruptedException
                // exception.
                _logHandle.openLog();
            } catch (LogOpenException exc) {
                // The code above should have checked if the log is open or not fron the point of view of this class.
                // If the LogHandle thinks the log is open but this class does not then they have become out of step
                // and this represents an internal recovery log failure.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "464", this);
                markFailed(exc); /* @MD19484C */
                _logHandle = null;
                _recoverableUnits = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "InternalLogException");
                throw new InternalLogException(exc);
            } catch (LogCorruptedException exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "473", this);
                markFailed(exc); /* @MD19484C */
                _logHandle = null;
                _recoverableUnits = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", exc);
                throw exc;
            } catch (LogIncompatibleException exc) {
                // No FFDC code needed
                // This specific failure type where a recovery log has been found to be incompatible
                // with this version of the RLS is most likely to occur in an HA enabled environment
                // where there are other servers in the cluster that are from a down-level websphere
                // product. For example 5.1 and 6.0 servers in the same cluster. When a 6.0 server
                // attempts to peer recover the 5.1 log (as will be the case since the 5.1 server
                // will not join an HA group get ownership of its recovery logs) a
                // LogIncompatibleException will be generated. To try and avoid confusion, this is
                // logged in a single place only (in the trace - to be replaced with messages asap)
                // Additionally, no FFDC is generated.
                //
                // Clearly, this is somewhat in conflict with the reporting of errors when a recovery
                // log from an older WS install is manually recovered on newer WS install (not supported
                // anyway). The HA issues above are deemed to be more important.
                markIncompatible();
                _logHandle = null;
                _recoverableUnits = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", exc);
                throw exc;
            } catch (LogAllocationException exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "482", this);
                markFailed(exc); /* @MD19484C */
                _logHandle = null;
                _recoverableUnits = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", exc);
                throw exc;
            } catch (InternalLogException exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "491", this);
                markFailed(exc); /* @MD19484C */
                _logHandle = null;
                _recoverableUnits = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", exc);
                throw exc;
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "500", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unexpected exception caught in openLog", exc);
                markFailed(exc); /* @MD19484C */
                _logHandle = null;
                _recoverableUnits = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "InternalLogException");
                throw new InternalLogException(exc);
            }

            // Obtain a list of recovered RecoverableUnits
            final ArrayList<ReadableLogRecord> records = _logHandle.recoveredRecords();

            if ((records != null) && (records.size() > 0)) {
                try {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "MultiScopeRecoveryLog " + _logName + " contains " + records.size() + " records to recover");

                    // Process each recovered record and rebuild the objects it represents
                    for (int i = 0; i < records.size(); i++) {
                        final ReadableLogRecord record = records.get(i);

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovering record " + i);

                        RecoverableUnitImpl.recover(this, record, _logHandle, _storageMode, _controlLock);

                    }
                } catch (LogCorruptedException exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "531", this);
                    markFailed(exc); /* @MD19484C */
                    _logHandle = null;
                    _recoverableUnits = null;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "A LogCorruptedException exception occured when reconstructng a RecoverableUnit");
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "openLog", exc);
                    throw exc;
                } catch (InternalLogException exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "541", this);
                    markFailed(exc); /* @MD19484C */
                    _logHandle = null;
                    _recoverableUnits = null;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "An InternalLogException exception occured when reconstructng a RecoverableUnit");
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "openLog", exc);
                    throw exc;
                } catch (Throwable exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.openLog", "551", this);
                    markFailed(exc); /* @MD19484C */
                    _logHandle = null;
                    _recoverableUnits = null;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "An exception occured reconstructng a RecoverableUnit", exc);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "openLog", "InternalLogException");
                    throw new InternalLogException(exc);
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "MultiScopeRecoveryLog " + _logName + " is empty");
            }
        }

        // In light of this open operation, the caller will need to issue an additional close operation to fully close
        // the recovey log.
        _closesRequired++;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Closes required: " + _closesRequired);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "openLog");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.serviceData
    //------------------------------------------------------------------------------
    /**
     * Returns a copy of the service data or null if there is none defined. Changes to
     * the copy will have no affect on the service data stored by the RLS.
     *
     * @return The service data.
     *
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public byte[] serviceData() throws LogClosedException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serviceData", this);

        // Check that the log is open.
        if (_logHandle == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "serviceData", "LogClosedException");
            throw new LogClosedException(null);
        }

        // Lookup and copy the service data block. Return the copy to the caller in order
        // to prevent them from chaging the information we are actually using.
        byte[] serviceData = null;

        try {
            serviceData = _logHandle.getServiceData();
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.serviceData", "609", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "serviceData", exc);
            throw exc;
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.serviceData", "615", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "serviceData", "InternalLogException");
            throw new InternalLogException(exc);
        }

        byte[] serviceDataCopy = null;

        if (serviceData != null) {
            serviceDataCopy = new byte[serviceData.length];
            System.arraycopy(serviceData, 0, serviceDataCopy, 0, serviceData.length);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serviceData", RLSUtils.toHexString(serviceDataCopy, RLSUtils.MAX_DISPLAY_BYTES));
        return serviceDataCopy;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.recoveryComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the RLS that any outstanding recovery process for the recovery log is
     * complete. Client services may issue this call to give the RLS an opportunity
     * to optomize log access by performing a keypoint operation. Client services do
     * not have to issue this call.
     * </p>
     *
     * <p>
     * This call is separate from the <code>RecoveryDirector.recoveryComplete</code>
     * method which must be invoked by a client service in response to a recovery
     * request. The RecoveryDirector callback indicates that sufficient recovery
     * processing has been performed to allow the request to be passed to the next
     * client service. The recovery process may however still execute on a separate
     * thread and call <code>RecoveryLog.recoveryComplete</code> when it has
     * finished.
     * </p>
     *
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     *
     */
    @Override
    public void recoveryComplete() throws LogClosedException, InternalLogException, LogIncompatibleException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", this);

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", this);
            throw new InternalLogException(null);
        }

        // Check that the log is open.
        if (_logHandle == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogClosedException");
            throw new LogClosedException(null);
        }

        try {
            keypoint();
        } catch (LogClosedException exc) {
            // In practice this exception is unlikely to occur. We have already checked above that the log is
            // open. It is possible that in a multi-threaded environment this could occur, but its the
            // responsibility of the user of the recovery log service to ensure they do not try and close
            // the log at the same time as calling recoveryComplete! All we can do is re-throw the exception.
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.recoveryComplete", "686", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", exc);
            throw exc;
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.recoveryComplete", "692", this);
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", exc);
            throw exc;
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.recoveryComplete", "699", this);
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.recoveryComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the RLS that any outstanding recovery process for the recovery log is
     * complete. Client services may issue this call to give the RLS an opportunity
     * to optomize log access by performing a keypoint operation. Client services do
     * not have to issue this call.
     * </p>
     *
     * <p>
     * This call is separate from the <code>RecoveryDirector.recoveryComplete</code>
     * method which must be invoked by a client service in response to a recovery
     * request. The RecoveryDirector callback indicates that sufficient recovery
     * processing has been performed to allow the request to be passed to the next
     * client service. The recovery process may however still execute on a separate
     * thread and call <code>RecoveryLog.recoveryComplete</code> when it has
     * finished.
     * </p>
     *
     * <p>
     * This extended version of the <code>RecoveryLog.recoveryCompelte()</code> method
     * allows the service data to be updated.
     * </p>
     *
     * @param serviceData The updated service data.
     *
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public void recoveryComplete(byte[] serviceData) throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", this);
            throw new InternalLogException(null);
        }

        // Check that the log is open.
        if (_logHandle == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "LogClosedException");
            throw new LogClosedException(null);
        }

        // Take a copy of the supplied service data to ensure that future changes
        // to it do not effect the recovery log. This is the only point in the
        // code where the service data may be updated. When the log keypointed,
        // the new service data will be written to disk.
        byte[] serviceDataCopy = null;

        if (serviceData != null) {
            serviceDataCopy = new byte[serviceData.length];
            System.arraycopy(serviceData, 0, serviceDataCopy, 0, serviceData.length);
        }

        try {
            // Update the service data held by the logHandle
            _logHandle.setServiceData(serviceDataCopy);

            // Now that the service data has been updated, keypoint the log.
            keypoint();
        } catch (LogClosedException exc) {
            // In practice this exception is unlikely to occur. We have already checked above that the log is
            // open. It is possible that in a multi-threaded environment this could occur, but its the
            // responsibility of the user of the recovery log service to ensure they do not try and close
            // the log at the same time as calling recoveryComplete. All we can do is re-throw the exception.
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.recoveryComplete", "785", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", exc);
            throw exc;
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.recoveryComplete", "791", this);
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", exc);
            throw exc;
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.recoveryComplete", "798", this);
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryComplete", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.closeLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Close the recovery log. The RLS will ensure that all active RecoverableUnits
     * and RecoverableUnitSections are stored persistently and, if possible that out
     * of date information is purged from the recovery log. The recovery log should
     * be opened again before further access.
     * </p>
     *
     * <p>
     * Since the client service may issue any number of openLog calls, each must be
     * paired with a corrisponding closeLog call. This allows common logic to be
     * executed on independent threads without any need to determine if a
     * recovery log is already open. This model would typically be used when
     * different threads obtain the same RecoveryLog object through independant
     * calls to <p>RecoveryLogDirector.getRecoveryLog</p>. For example, a recovery
     * process may be in progress on one thread whilst forward processing is being
     * performed on another. Both pieces of logic may issue their own openLog and
     * closeLog calls independently.
     * </p>
     *
     * <p>
     * Alternativly, the reference to a RecoveryLog may be shared directly around
     * the program logic or between threads. Using this model, a single openLog and
     * closeLog pair are required at well defined initialziation and shutdown points
     * in the client service.
     * </p>
     *
     * <p>
     * This implementation of the RecoveryLog interface uses a simple counter
     * '_closesRequired' to track the number of times openLog has been called
     * and consiquentially the number of times closeLog must be invoked to
     * 'fully' close the recovery log.
     * <p>
     *
     * <p>
     * This extended version of the <code>RecoveryLog.closeLog()</code> method
     * allows the service data to be updated prior to the close operation being
     * performed.
     * </p>
     *
     * @param serviceData The updated service data.
     *
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public void closeLog(byte[] serviceData) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLog", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        if (_logHandle != null) {
            try {
                // Update the service data prior to closing the log.
                _logHandle.setServiceData(serviceData);

                // Close the log.
                closeLog();
            } catch (InternalLogException exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLog", "870", this);
                markFailed(exc); /* @MD19484C */
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "closeLog", exc);
                throw exc;
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLog", "877", this);
                markFailed(exc); /* @MD19484C */
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "closeLog", "InternalLogException");
                throw new InternalLogException(exc);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.closeLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Close the recovery log. The RLS will ensure that all active RecoverableUnits
     * and RecoverableUnitSections are stored persistently and, if possible that out
     * of date information is purged from the recovery log. The recovery log should
     * be opened again before further access.
     * </p>
     *
     * <p>
     * Since the client service may issue any number of openLog calls, each must be
     * paired with a corrisponding closeLog call. This allows common logic to be
     * executed on independent threads without any need to determine if a
     * recovery log is already open. This model would typically be used when
     * different threads obtain the same RecoveryLog object through independant
     * calls to <p>RecoveryLogDirector.getRecoveryLog</p>. For example, a recovery
     * process may be in progress on one thread whilst forward processing is being
     * performed on another. Both pieces of logic may issue their own openLog and
     * closeLog calls independently.
     * </p>
     *
     * <p>
     * Alternativly, the reference to a RecoveryLog may be shared directly around
     * the program logic or between threads. Using this model, a single openLog and
     * closeLog pair are required at well defined initialziation and shutdown points
     * in the client service.
     * </p>
     *
     * <p>
     * This implementation of the RecoveryLog interface uses a simple counter
     * '_closesRequired' to track the number of times openLog has been called
     * and consiquentially the number of times closeLog must be invoked to
     * 'fully' close the recovery log.
     * </p>
     *
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public void closeLog() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLog", this);

        if (_logHandle != null) {
            // Only try to keypoint the recovery log if its in a valid state. If the service
            // is closing the log in response to a failure, we do not want to make things worse
            // by performing a keypoint operation and corrupting the log. Same applies to
            // the compatibility test.
            if (!failed() && !incompatible()) {
                try {
                    keypoint();
                } catch (LogClosedException exc) {
                    // The log is already closed so absorb the exception.
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLog", "944", this);
                } catch (InternalLogException exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLog", "948", this);
                    markFailed(exc); /* @MD19484C */
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "closeLog", exc);
                    throw exc;
                } catch (Throwable exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLog", "955", this);
                    markFailed(exc); /* @MD19484C */
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "closeLog", "InternalLogException");
                    throw new InternalLogException(exc);
                }
            }

            synchronized (this) {
                _closesRequired--;

                if (_closesRequired <= 0) {
                    try {
                        _logHandle.closeLog();
                    } catch (InternalLogException exc) {
                        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLog", "974", this);
                        markFailed(exc); /* @MD19484C */
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "closeLog", exc);
                        throw exc;
                    } catch (Throwable exc) {
                        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLog", "981", this);
                        markFailed(exc); /* @MD19484C */
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "closeLog", "InternalLogException");
                        throw new InternalLogException(exc);
                    }

                    // Reset the internal state so that a subsequent open operation does not
                    // occurs with a "clean" environment.
                    _logHandle = null;
                    _recoverableUnits = null;
                    _closesRequired = 0;
                    _unwrittenDataSize.set(0);
                    _totalDataSize = 0;
                    _failed = false;
                }
            }
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Closes required: " + _closesRequired);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.closeLogImmediate
    //------------------------------------------------------------------------------
    /**
    */
    @Override
    public void closeLogImmediate() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLogImmediate", this);

        if (_logHandle != null) {
            try {
                _logHandle.closeLog();
            } catch (InternalLogException exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLogImmediate", "1173", this);
                markFailed(exc); /* @MD19484C */
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "closeLogImmediate", exc);
                throw exc;
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.closeLogImmediate", "1180", this);
                markFailed(exc); /* @MD19484C */
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "closeLogImmediate", "InternalLogException");
                throw new InternalLogException(exc);
            }

            // Reset the internal state so that a subsequent open operation does not
            // occurs with a "clean" environment.
            _logHandle = null;
            _recoverableUnits = null;
            _closesRequired = 0;
            _unwrittenDataSize.set(0);
            _totalDataSize = 0;
            _failed = false;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLogImmediate");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.createRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Create a new RecoverableUnit under which to write information to the recovery
     * log.
     * </p>
     *
     * <p>
     * Information written to the recovery log is grouped by the service into a number
     * of RecoverableUnit objects, each of which is then subdivided into a number of
     * RecoverableUnitSection objects. Information to be logged is passed to a
     * RecoverableUnitSection in the form of a byte array.
     * </p>
     *
     * <p>The identity of the recoverable unit will be allocated by the recovery log.
     * Use of this method <b>must not</b> be mixed with createRecoverableUnit(long)</p>
     *
     * @return The new RecoverableUnit.
     *
     * @exception LogClosedException Thrown if the recovery log is closed and must be
     *                opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public RecoverableUnit createRecoverableUnit(FailureScope failureScope) throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoverableUnit", new Object[] { failureScope, this });

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "InternalLogException");
            throw new InternalLogException(null);
        }

        // Check that the log is actually open.
        if (_logHandle == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "LogClosedException");
            throw new LogClosedException(null);
        }

        RecoverableUnitImpl recoverableUnit = null;

        // We are going to change the internal data structures by adding a new recoverable unit. Take a shared lock
        // to avoid a clash with the keypoint logic.
        _controlLock.getSharedLock(LOCK_REQUEST_ID_MSRL_CREATE);

        synchronized (this) {
            long identity = _recUnitIdTable.nextId(this);
            recoverableUnit = new RecoverableUnitImpl(this, identity, failureScope, _logHandle, _storageMode, _controlLock);
            if (tc.isEventEnabled())
                Tr.event(tc, "MultiScopeRecoveryLog '" + _logName + "' created a new RecoverableUnit with id '" + identity + "'");
        }

        try {
            _controlLock.releaseSharedLock(LOCK_REQUEST_ID_MSRL_CREATE);
        } catch (NoSharedLockException exc) {
            // In practice this exception will not be generated as we have obtained the shared lock at the top of this method
            // so we should be able to release it here. If this exception is generated then all we can do is throw the
            // InternalLogException exception to indicate a serious problem with the recovery log service.
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.createRecoverableUnit", "1070", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "InternalLogException");
            throw new InternalLogException(exc);
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.createRecoverableUnit", "1076", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createRecoverableUnit", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoverableUnit", recoverableUnit);
        return recoverableUnit;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.removeRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Remove a RecoverableUnit from the recovery logs set of active RecoverableUnits.
     * </p>
     *
     * <p>
     * The RecoverableUnit and its associated RecoverableUnitSections are no longer
     * considered valid after this call. The client service must not invoke any further
     * methods on them.
     * </p>
     *
     * <p>
     * The RLS will remove these objects from its "in memory" copy of the recovery
     * log and write (but not force) a record of this deletion to persistent storage.
     * This means that in the event of a server failure, removed objects may be still
     * be reconstructed during recovery processing and client services must be able
     * to cope with this. Any subsequent force operation will ensure that this
     * deletion record is persisted to disk and any subsequent keypoint operation
     * will remove all reference to the recoverable unit from the recovery log.
     * </p>
     *
     * <p>
     * This method must not be invoked whilst an unclosed LogCursor is held (for either
     * all RecoverableUnits or this RecoverableUnits RecoverableUnitSections.) The
     * <code>LogCursor.remove</code> method should be used instead.
     * </p>
     *
     * @param identity Identity of the RecoverableUnit to be removed.
     *
     * @exception LogClosedException Thrown if the recovery log is closed and must be
     *                opened before this call can be issued.
     * @exception InvalidRecoverableUnitException Thrown if the RecoverableUnit does not exist.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public void removeRecoverableUnit(long identity) throws LogClosedException, InvalidRecoverableUnitException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoverableUnit", new Object[] { identity, this });

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", this);
            throw new InternalLogException(null);
        }

        // Ensure the log is actually open.
        if (_logHandle == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "LogClosedException");
            throw new LogClosedException(null);
        }

        // We are going to change the internal data structures by removing a new recoverable unit. Take a shared lock
        // to avoid a clash with the keypoint logic.
        _controlLock.getSharedLock(LOCK_REQUEST_ID_MSRL_REMOVE);

        RecoverableUnitImpl recoverableUnit = null;

        synchronized (this) {
            // REQD Thread safety
            recoverableUnit = removeRecoverableUnitMapEntries(identity);
        }

        // If the RecoverableUnit corresponding to 'identity' was not found in the map then throw an exception
        if (recoverableUnit == null) {
            try {
                _controlLock.releaseSharedLock(LOCK_REQUEST_ID_MSRL_REMOVE);
            } catch (Exception exc) {
                // We are processing an error condition anyway. Ignore this problem as there is nothing
                // that can be done about it. The InternalLogException exception indicates to the
                // original caller that the recovery log service has failed.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.removeRecoverableUnit", "1165", this);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "InvalidRecoverableUnitException");
            throw new InvalidRecoverableUnitException(null);
        }

        // Inform the recoverable unit that it is being deleted. This enables it to write a "DELETED" entry in the
        // recovery log (unforced) to ensure that a previous instance of this recoverable unit does not get confused with
        // a newer instance (otherwise the recovey logic would have no way of distinguising between older, deleted information
        // and newer recoverable information)
        try {
            recoverableUnit.remove();
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.removeRecoverableUnit", "1182", this);

            markFailed(exc); /* @MD19484C */

            try {
                _controlLock.releaseSharedLock(LOCK_REQUEST_ID_MSRL_REMOVE);
            } catch (Exception exc2) {
                // We are processing an internal failure anyway. Ignore this problem as there is nothing
                // that can be done about it. The InternalLogException exception indicates to the
                // original caller that the recovery log service has failed.
                FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.removeRecoverableUnit", "1195", this);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", exc);
            throw exc;
        }

        // We have finished updating the internal data structure so release the shared lock.
        try {
            _controlLock.releaseSharedLock(LOCK_REQUEST_ID_MSRL_REMOVE);
        } catch (NoSharedLockException exc) {
            // In practice this exception will not be generated as we have obtained the shared lock at the top of this method
            // so we should be able to release it here. If this exception is generated then all we can do is throw the
            // InternalLogException exception to indicate a serious problem with the recovery log service.
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.removeRecoverableUnit", "1212", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "InternalLogException");
            throw new InternalLogException(exc);
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.removeRecoverableUnit", "1218", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removeRecoverableUnit", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoverableUnit");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.recoverableUnits
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Returns a LogCursor that can be used to itterate through all active
     * RecoverableUnits. The order in which they are returned is not defined.
     *
     * z/OS requires that this method be synchronized because, in this environment,
     * there could be multiple servants accessing the log at the same time. @MD19706.
     * </p>
     *
     * <p>
     * The LogCursor must be closed when it is no longer needed or its itteration
     * is complete. (See the LogCursor class for more information)
     * </p>
     *
     * <p>
     * Objects returned by <code>LogCursor.next</code> or <code>LogCursor.last</code>
     * must be cast to type RecoverableUnit.
     * </p>
     *
     * <p>
     * Care must be taken not remove or add recoverable units whilst the resulting
     * LogCursor is open. Doing so will result in a ConcurrentModificationException
     * being thrown.
     * </p>
     *
     * @return A LogCursor that can be used to itterate through all active
     *         RecoverableUnits.
     *
     * @exception LogClosedException Thrown if the recovery log is closed and must be
     *                opened before this call can be issued.
     */
    @Override
    public synchronized LogCursor recoverableUnits(FailureScope failureScope) throws LogClosedException /* @MD19706C */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoverableUnits", new Object[] { failureScope, this });

        // Check that the log is actually open
        if (_logHandle == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoverableUnits", "LogClosedException");
            throw new LogClosedException(null);
        }

        final List<RecoverableUnitImpl> recoverableUnits = new ArrayList<RecoverableUnitImpl>();

        // No need to access this inside a sync block as the caller is required to
        // hold off from changing the underlying structures whilst the cursor is open.
        final Iterator iterator = _recoverableUnits.values().iterator();

        while (iterator.hasNext()) {
            final RecoverableUnitImpl recoverableUnit = (RecoverableUnitImpl) iterator.next();

            if (_bypassContainmentCheck || (recoverableUnit.failureScope().isContainedBy(failureScope))) {
                recoverableUnits.add(recoverableUnit);
            }
        }

        final LogCursor cursor = new LogCursorImpl(_controlLock, recoverableUnits, true, this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoverableUnits", cursor);

        return cursor;
    }

    // Added to support RecoveryLog interface so that RecoveryLogImpl can store all types of RecoveryLog (eg new SQL log)
    @Override
    public LogCursor recoverableUnits() throws LogClosedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoverableUnits", this);

        final LogCursor cursor = recoverableUnits(_failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoverableUnits", cursor);

        return cursor;
    }

/* TODO remove the getRecoverableUnit method ... this is the 'interface' method */
    @Override
    public RecoverableUnit lookupRecoverableUnit(long identity) throws LogClosedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lookupRecoverableUnit", new Object[] { new Long(identity), this });

        RecoverableUnit runit = getRecoverableUnit(identity);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lookupRecoverableUnit", runit);
        return runit;
    }

    @Override
    public RecoverableUnit createRecoverableUnit() throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoverableUnit", this);

        final RecoverableUnit runit = createRecoverableUnit(_failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoverableUnit", runit);
        return runit;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.logProperties
    //------------------------------------------------------------------------------
    /**
     * Returns the LogProperties object that defines the physical nature and identity
     * of the associated recovery log.
     *
     * @return The LogProperties object.
     */
    @Override
    public LogProperties logProperties() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "logProperties", _fileLogProperties);
        return _fileLogProperties;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.keypoint
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Instructs the recovery log to perfom a keypoint operation. Any redundant
     * information will be removed and all cached information will be forced to disk.
     * </p>
     *
     * @exception LogClosedException Thrown if the log is closed.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    @Override
    public void keypoint() throws LogClosedException, InternalLogException, LogIncompatibleException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypoint", this);

        // If this recovery log instance has been marked as incompatible then throw an exception
        // accordingly.
        if (incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypoint", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing.
        if (failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypoint", this);
            throw new InternalLogException(null);
        }

        // Check that the log is open.
        if (_logHandle == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypoint", "LogClosedException");
            throw new LogClosedException(null);
        }

        // Try and obtain the exclusive lock that must be held to perform keypoint processing. If this
        // lock is denied then another thread is performing the required keypoint operation and so
        // this method can return after checking that the other thread sucessfully keypointed.
        boolean obtainedLock = false;
        try {
            obtainedLock = _controlLock.attemptExclusiveLock();
        } catch (HoldingExclusiveLockException exc) {
            // This thread already holds the exclusive lock. That means that the keypoint operation is already
            // in progress on this thread. This would occur if the keypoint operation actually triggered a
            // keypoint operation. This should never occur in paractice and would represent a serious internal
            // error.
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1353", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The keypoint operation has triggered a keypoint operation.");
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypoint", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (obtainedLock) {
            // This thread has been allocated the exclusive lock. That means that we will perform a 'real' keypoint
            // operation on this thread.

            // Inform the underlying log handle that we are about to process a keypoint operation. It can
            // use this opportunity to switch files and re-calculate the amount of avialble free space.
            try {
                _logHandle.keypointStarting();
            } catch (InternalLogException exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1373", this);

                markFailed(exc); /* @MD19484C */

                try {
                    _controlLock.releaseExclusiveLock();
                } catch (Throwable exc2) {
                    // IGNORE - The recovery log service is failing anyway.
                    FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1384", this);
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "keypoint", exc);
                throw exc;
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1392", this);

                markFailed(exc); /* @MD19484C */

                try {
                    _controlLock.releaseExclusiveLock();
                } catch (Throwable exc2) {
                    // IGNORE - The recovery log service is failing anyway.
                    FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1403", this);
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "keypoint", "InternalLogException");
                throw new InternalLogException(exc);
            }

            // Check that there is sufficient space available in the log
            // to perform this keypoint operation.

            if (tc.isDebugEnabled()) {
                int targetFreeSpace = _logHandle.getFreeSpace();
                Tr.debug(tc, "Recovery log contains " + _totalDataSize + " payload bytes");
                Tr.debug(tc, "Target keypoint file has " + targetFreeSpace + " available free bytes");
                Tr.debug(tc, "Resize trigger constant is " + TOTAL_DATA_RESIZE_TRIGGER);
                Tr.debug(tc, "Resize trigger value is " + targetFreeSpace * TOTAL_DATA_RESIZE_TRIGGER + " bytes");
            }

            if (_totalDataSize > (_logHandle.getFreeSpace() * TOTAL_DATA_RESIZE_TRIGGER)) {
                // There is insufficient space in the underlying file to write all of the log's data
                // while maintaining the required amount of free space in the log. We must, if possible,
                // resize it.

                // Determine the target for the log's size, capping it to the maximum size specified by the user.
                final int logFileHeaderSize; /* @MD19753A */
                try /* @MD19753A */
                { /* @MD19753A */
                    logFileHeaderSize = _logHandle.logFileHeader().length();/* @MD19753A */
                } /* @MD19753A */
                catch (InternalLogException ile) /* @MD19753A */
                { /* 3@MD19753A */
                    if (tc.isEventEnabled())
                        Tr.debug(tc, "Could not get log file header length", ile);

                    FFDCFilter.processException(ile, "com.ibm.ws.recoverylog.spi.RecoveryLogImpl.keypoint", "1780", this);

                    markFailed(ile); /* @MD19753A */

                    try /* @MD19753A */
                    { /* @MD19753A */
                        _controlLock.releaseExclusiveLock(); /* @MD19753A */
                    } /* @MD19753A */
                    catch (Throwable exc) /* @MD19753A */
                    { /* @MD19753A */
                        // IGNORE - The recovery log service is failing anyway. 2@MD19753A
                        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoveryLogImpl.keypoint", "1791", this);
                    } /* @MD19753A */

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "keypoint"); /* @MD19753A */
                    throw ile; /* @MD19753A */
                } /* @MD19753A */
                catch (LogIncompatibleException lie) {
                    FFDCFilter.processException(lie, "com.ibm.ws.recoverylog.spi.RecoveryLogImpl.keypoint", "1575", this);

                    // Unlike some instances of LogIncompatibleException that occur when initially opening a recovery
                    // log, this instance is unlikely to occur unless there is a bug in the code. We check the
                    // version code when we initially open the log and if its not compatible we clear the state and
                    // stop responding. To get as far as a keypoint before this is detected should not occur. Thats
                    // why we convert this exception into an InternalLogException.
                    markFailed(lie);

                    try {
                        _controlLock.releaseExclusiveLock();
                    } catch (Throwable exc) {
                        // No FFDC code needed
                        // IGNORE - The recovery log service is failing anyway.
                    }

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "keypoint");
                    throw new InternalLogException(lie);
                }

                final int targetSize = Math.min((int) (_totalDataSize * TOTAL_DATA_RESIZE_MULTIPLIER), ((_maxLogFileSize * 1024) - logFileHeaderSize)); /* @MD19753C */

                if (targetSize < _totalDataSize) {
                    // The log cannot be resized to accommodate all of its data. Mark it as failed to prevent
                    // further I/O occuring and throw the LogFullException back to the caller. Note that we must
                    // mark it as failed BEFORE releasing the exclsuive lock to ensure that any other threads
                    // that are waiting on the lock will be able to detect the failure when they wake up.
                    LogFullException lfe = new LogFullException(null); /* @MD19484M */
                    markFailed(lfe); /* @MD19484C */

                    try {
                        _controlLock.releaseExclusiveLock();
                    } catch (Throwable exc2) {
                        // IGNORE - The recovery log service is failing anyway.
                        FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1446", this);
                    }

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "keypoint", "LogFullException");
                    throw lfe; /* @MD19484C */
                }

                // Resize the log to the target size
                _logHandle.resizeLog(targetSize);
            }

            try {
                final Iterator recoverableUnits = _recoverableUnits.values().iterator();

                while (recoverableUnits.hasNext()) {
                    final RecoverableUnitImpl recoverableUnit = (RecoverableUnitImpl) recoverableUnits.next();
                    recoverableUnit.writeSections(true);
                }

                _logHandle.keypoint();
            } catch (Throwable exc) {
                // First try and release the locks. Since the recovery log has suffered a fatal error condition
                // we ignore any failures that occur here.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1478", this);

                if (tc.isEventEnabled())
                    Tr.event(tc, "Exception caught performing keypoint", exc);

                markFailed(exc);

                try {
                    _controlLock.releaseExclusiveLock();
                } catch (Throwable exc2) {
                    // IGNORE - The recovery log service is failing anyway.
                    FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1491", this);
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "keypoint", "InternalLogException");
                throw new InternalLogException(exc);
            }

            // issue warning message if log is filling up
            if (!_logWarningIssued && _totalDataSize > ((_maxLogFileSize * 1024 - _totalDataSize) * LOG_WARNING_FACTOR)) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Logfile is filling up, issuing warning.", _logName);

                _logWarningIssued = true;
                try {
                    _recoveryAgent.logFileWarning(_logName, _totalDataSize, _maxLogFileSize * 1024);
                } catch (Throwable t) {
                    // shouldn't happen, swallow to ensure lock released
                    FFDCFilter.processException(t, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1511", this);
                }
            }

            try {
                _controlLock.releaseExclusiveLock();
            } catch (NoExclusiveLockException exc2) {
                // This should not occur as we did get the exclusive lock at the top of the method. If this
                // does occur all we can do is throw an InternalLogException exception.
                FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1506", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "keypoint", "InternalLogException");
                throw new InternalLogException(exc2);
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.keypoint", "1512", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "keypoint", "InternalLogException");
                throw new InternalLogException(exc);
            }
        } else {
            // This thread has not been allocated the exclusive lock. This has occured because some other thread
            // was allocated the exclusive lock, performed a 'real' keypoint and then released the exclusive lock
            // again. This thread has been blocked inside the 'attemptExclusiveLock' call whilst this has
            // been going on and has performed a 'piggyback' keypoint - ie all the real work has already
            // been done for this thread by the 'real' keypoint, so we just exit the method as if we had done
            // the keypoint directly.

            // Check that no serious internal error occured during the real keypoint operation. If it did then
            // this "piggybacked" keypoint has logically also failed. This must be reported to
            // the caller just as if it was this thread that encountered the problem.
            if (failed()) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "keypoint", this);
                throw new InternalLogException(null);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypoint");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.removing
    //------------------------------------------------------------------------------
    /**
     * This method is defined by the LogCursorCallback interface. When the client
     * service calls <code>MultiScopeRecoveryLog.recoverableUnits</code>, a LogCursor is
     * created passing the current instance of this class as the LogCursorCallback
     * argument. Whenever the <code>LogCursor.remove</code> method is invoked, the
     * <code>MultiScopeRecoveryLog.removing</code> method is driven automatically. This
     * gives the MultiScopeRecoveryLog class an opportunity to write a corrisponding
     * deletion record to the recovery log.
     *
     * @param target The RecoverableUnit that is being removed. Typed as an Object.
     *
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    @Override
    public void removing(Object target) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removing", new Object[] { target, this });

        // If this recovery log instance has experienced a serious internal error then prevent this operation from
        // executing. The log should not be able to get to this point if its not compatible (as this method is only
        // called as a result of a remove on the LogCursor provided by the recoverableUnits method and this method
        // will generate a LogIncompatibleException in that event to prevent the cursor from being provided to the
        // caller. As a result, an incompatible log at this point is actually a code error of some form so
        // treat it in the same way as failure.
        if (failed() || incompatible()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", this);
            throw new InternalLogException(null);
        }

        try {
            ((RecoverableUnitImpl) target).remove();
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.removing", "1573", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error occured whilst removing a RecoverableUnit");
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", exc);
            throw exc;
        } catch (Exception exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.removing", "1581", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error occured whilst removing a RecoverableUnit");
            markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "removing", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removing");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.payloadAdded
    //------------------------------------------------------------------------------
    /**
     * Informs the recovery log that data has been added to one of its recoverable
     * units. The recovery log must use the supplied information to track the amount
     * of active data it holds. This information will be required each time a keypoint
     * occurs, in order to determine if there is sufficient space in the current file.
     * to perform the keypoint sucessfully.
     *
     * This call is driven from the recoverable unit to which data has been added and
     * accounts for both the data and header fields necessary to write the data
     * completly.
     *
     * This data has not yet been written to persistent storage and must therefour be
     * tracked in both total and unwritten data size fields. It is important to
     * understand why two parameters are required on this call rather than a single
     * value that could be reflected in both fields. The following example can be used
     * to illustrate the reason for this. Consider that data items of sizes D1, D2
     * and D3 have been added to an initially empty recovery log. If H represents the
     * size of all the header information that the underlying recoverable units, sections
     * and data items will need to form a persistent record of the data, then the
     * unwritten and total data size fields will be made up as follows:
     *
     * unwritten total
     * D3 D3
     * D2 D2
     * D1 D1
     * H H
     *
     * Once this information has been written to disk, D1,D2 and D3 will be deducted
     * from the unwritten total (see payloadWritten) whilst the total data size remains
     * unchanged. Because there is then no further unwritten information there is no
     * requirement for header H, so it also is removed as follows:-
     *
     * unwritten total
     * D3
     * D2
     * D1
     * - H
     *
     * Consider that a new data item of size D4 is added. We need to add D4 + H to the
     * unwritten field and D4 alone to the total field. Since the caller takes care of
     * the size of H anyway, we need two parameters to contain this level of detail.
     * At this point the makeup of these two fields is as follows:-
     *
     * unwritten total
     * D4
     * D3
     * D2
     * D4 D1
     * H H
     *
     * @param unwrittenPayloadSize The additional number of bytes that would be needed
     *            to form a persistent record of the new data item
     *            that has been added within this recovery log when a
     *            writeSections or forceSections operation is driven
     *            by the client service.
     * @param totalPayloadSize The additional number of bytes that would be needed
     *            to form a persistent record of the new data item
     *            that has been added within this recovery log when a
     *            keypoint operation occurs.
     */
    protected void payloadAdded(int unwrittenPayloadSize, int totalPayloadSize) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "payloadAdded", new Object[] { this, unwrittenPayloadSize, totalPayloadSize });

        _unwrittenDataSize.addAndGet(unwrittenPayloadSize);
        synchronized (this) {
            _totalDataSize += totalPayloadSize;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize.get() + " totalDataSize = " + _totalDataSize);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "payloadAdded");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.payloadWritten
    //------------------------------------------------------------------------------
    /**
     * Informs the recovery log that previously unwritten data has been written to disk
     * by one of its recoverable units and no longer needs to be tracked in the unwritten
     * data field. The recovery log must use the supplied information to track the amount
     * of unwritten active data it holds.
     *
     * This call is driven from the recoverable unit from which data has been written
     * and accounts for both the data and header fields necessary to write the data
     * completly.
     *
     * Writing data in this manner will not change the total amount of active data contained
     * by the recovery log so only the unwritten data size will be effected.
     * The following example can be used to illustrate this. Consider that data items of sizes
     * D1, D2 and D3 have been added to an initially empty recovery log (see payloadAdded).
     * If H represents the size of all the header information that the underlying recoverable
     * units, sections and data items will need to form a persistent record of the data, then
     * the unwritten and total data size fields will be made up as follows:
     *
     * unwritten total
     * D3 D3
     * D2 D2
     * D1 D1
     * H H
     *
     * Suppose that the data item corrisponding to D2 has been written to disk. D2 + h2
     * (where h2 is any component of H that will no longer be required to form a
     * persistent record of the unwritten data) will be removed from the unwritten total so we
     * have:-
     *
     * unwritten total
     * D3
     * D3 D2
     * D1 D1
     * H-h2 H
     *
     * If the remaining data items are also written it should be clear that D3+h3 + D1+h1
     * bytes will also be removed from the unwritten total leaving it at zero. Also that
     * h1 + h2 + h3 = H.
     *
     * @param payloadSize The number of bytes that no longer need to be written in order
     *            to form a persistent record of the remaining unwritten data items
     *            when a writeSections or forceSections operation is driven by the
     *            client service.
     */
    protected void payloadWritten(int payloadSize) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "payloadWritten", new Object[] { this, payloadSize });

        _unwrittenDataSize.addAndGet(-payloadSize);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize.get() + " totalDataSize = " + _totalDataSize);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "payloadWritten");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.payloadDeleted
    //------------------------------------------------------------------------------
    /**
     * Informs the recovery log that data has been removed from one of its recoverable
     * units. Right now, this means that a recoverable unit and all its content has
     * been removed, but in the future this will also be driven when a single recoverable
     * unit section within a recoverable unit has been removed. The recovery log must use
     * the supplied information to track the amount of active data it holds. This information
     * will be required each time a keypoint occurs, in order to determine if there is
     * sufficient space in the current file to perform the keypoint sucessfully.
     *
     * This call is driven by the recoverable unit that has been removed and accounts for
     * both the data and header fields that would have been necessary to form a persistnet
     * record of the the recoverable unit and its content.
     *
     * This data may or may not have been written to persistent storage and must therefour be
     * tracked in both total and unwritten data size fields. It is important to
     * understand why two parameters are required on this call rather than a single
     * value that could be reflected in both fields. The following example can be used
     * to illustrate the reason for this. Consider that data items of sizes D1, D2
     * and D3 have been added to an initially empty recovery log. If H represents the
     * size of all the header information that the underlying recoverable units, sections
     * and data items will need to form a persistent record of the data, then the
     * unwritten and total data size fields will be made up as follows:
     *
     * unwritten total
     * D3 D3
     * D2 D2
     * D1 D1
     * H H
     *
     * If this information has been written to disk, D1+D2+d3+H will be deducted
     * from the unwritten total (see payloadWritten) whilst the total data size remains
     * unchanged.
     *
     * unwritten total
     * D3
     * D2
     * D1
     * - H
     *
     * If D1,D2 and D3 are subsequently deleted, the total will need to be reduced but
     * the unwritten field will remian unchanged. Since it is the callers responsibility
     * to determine the amount that needs to be removed from each, two arguments are
     * required.
     *
     * @param unwrittenPayloadSize The number of bytes that will no longer be required
     *            to form a persistent record of the recovery log when a
     *            writeSections or forceSections operation is driven
     *            by the client service.
     * @param totalPayloadSize The number of bytes that will no longer be required
     *            to form a persistent record of the recovery log the
     *            next time a keypoint operation occurs.
     */
    protected void payloadDeleted(int totalPayloadSize, int unwrittenPayloadSize) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "payloadDeleted", new Object[] { this, totalPayloadSize, unwrittenPayloadSize });

        _unwrittenDataSize.addAndGet(-unwrittenPayloadSize);
        synchronized (this) {
            _totalDataSize -= totalPayloadSize;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize.get() + " totalDataSize = " + _totalDataSize);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "payloadDeleted");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.failed
    //------------------------------------------------------------------------------
    /**
     * Accessor method to read the recovery log failure state.
     *
     * The _failed flag is set to true if the recovery log has suffered an
     * internal error that leaves it in an undefined or unmanageable state.
     * Once this flag is set, any interface method that could modify the disk
     * state is stopped with an InternalLogException. This protects the on-disk
     * information from damage and allows a subseqent server bounce to recover
     * correctly.
     *
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if the it has been marked as failed.
     *
     * @return true if a serious internal error has occured, otherwise false.
     */
    protected boolean failed() {
        if (tc.isDebugEnabled() && _failed)
            Tr.debug(tc, "failed: RecoveryLog has been marked as failed. [" + this + "]");
        return _failed;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.incompatible
    //------------------------------------------------------------------------------
    /**
     * Accessor method to read the recovery log compatibility state.
     *
     * The _incompatible flag is set to true if the recovery log has previously been
     * opened and found to be at a level not compatible with this version of the RLS.
     *
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if the it has been marked as incompatible.
     *
     * @return true if the recovery log has been marked as incompatible otherwise false.
     */
    protected boolean incompatible() {
        if (tc.isDebugEnabled() && _incompatible)
            Tr.debug(tc, "incompatible: RecoveryLog has been marked as incompatible. [" + this + "]");
        return _incompatible;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.markFailed
    //------------------------------------------------------------------------------
    /**
     * Marks the recovery log as failed.
     *
     * Set the flag indicating that the recovery log has suffered an internal error
     * that leaves the recovey log in an undefined or unmanageable state.
     * Once this flag is set, any interface method that could modify the disk
     * state is stopped with an InternalLogException. This protects the on-disk
     * information from damage and allows a subseqent server bounce to recover
     * correctly.
     *
     * d453958: For HA configuration, this condition should force failover as the
     * server is now unusable.
     *
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if this is the first time the call has been
     * made.
     */
    protected void markFailed(Throwable t) /* @MD19484C */
    {
        markFailed(t, true);
    }

    protected void markFailed(Throwable t, boolean report) {
        boolean newFailure = false;
        synchronized (this) {
            if (tc.isDebugEnabled() && _failed)
                Tr.debug(tc, "markFailed: RecoveryLog has been marked as failed. [" + this + "]");

            if (!_failed) {
                newFailure = true;
                _failed = true;

                if (report) {
                    // On z/OS, the Tr.audit will go to hardcopy and the Tr.info
                    // will go to sysout and sysprint.  We really want the audit
                    // to go to the glass (wto) but can't because there is no Tr.wto
                    // in the 'common' Tr class.  This is the next best thing.
                    Object[] errorObject = new Object[] {
                                                          _logIdentifier, _clientName };
                    Tr.audit(tc, "CWRLS0008_RECOVERY_LOG_FAILED",
                             errorObject);
                    Tr.info(tc, "CWRLS0009_RECOVERY_LOG_FAILED_DETAIL", t);
                }

                // If this is a local recovery process then direct the server to terminate
                if (Configuration.HAEnabled()) {
                    if (Configuration.localFailureScope().equals(_failureScope)) {
                        // d254326 - output a message as to why we are terminating the server as in
                        // this case we never drop back to log any messages as for peer recovery.
                        Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", t);
                        Configuration.getRecoveryLogComponent().terminateServer();
                    } else {
                        Configuration.getRecoveryLogComponent().leaveGroup(_failureScope);
                    }
                }

            }
        }
        if (newFailure && _associatedLog != null) {
            if (_failAssociatedLog) {
                if (tc.isDebugEnabled() && _failed)
                    Tr.debug(tc, "associated log will be marked as failed", _associatedLog);
                _associatedLog.markFailedByAssociation();
            } else {
                _associatedLog.provideServiceability();
            }
        }
    }

    @Override
    public synchronized void markFailedByAssociation() {
        if (!_failed) {
            _failed = true;
            if (tc.isDebugEnabled() && _failed)
                Tr.debug(tc, "markFailedByAssociation: RecoveryLog has been marked as failed by association. [" + this + "]");
            provideServiceability();
        } else if (tc.isDebugEnabled() && _failed)
            Tr.debug(tc, "markFailedByAssociation: RecoveryLog was already failed when marked as failed by association. [" + this + "]");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.markIncompatible
    //------------------------------------------------------------------------------
    /**
     * Marks the recovery log as incompatible.
     *
     * Set the flag indicating that the recovery log represented by this object is
     * at an incompatible level.
     *
     * To avoid confusion in the trace, we have no entry/exit trace in this
     * method, but do put out a debug trace if this is the first time the call has been
     * made.
     */
    protected synchronized void markIncompatible() {
        if (tc.isDebugEnabled() && _incompatible)
            Tr.debug(tc, "markIncompatible: RecoveryLog has been marked as incompatible. [" + this + "]");
        _incompatible = true;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.addRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * Adds a new RecoverableUnitImpl object, keyed from its identity to this
     * classes collection of such objects.
     *
     * @param recoverableUnit The RecoverableUnit to be added
     * @param recovered Flag to indicate if this instances have been created during
     *            recovery (true) or normal running (false). If its been created
     *            during recovery we need to reserve the associated id so that
     *            it can't be allocated to an independent RecoverableUnit.
     */
    protected void addRecoverableUnit(RecoverableUnit recoverableUnit, boolean recovered) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addRecoverableUnit", new Object[] { recoverableUnit, recovered, this });

        final long identity = recoverableUnit.identity();

        _recoverableUnits.put(identity, recoverableUnit);

        if (recovered) {
            _recUnitIdTable.reserveId(identity, recoverableUnit);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addRecoverableUnit");
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.removeRecoverableUnitMapEntries
    //------------------------------------------------------------------------------
    /**
     * Removes a RecoverableUnitImpl object, keyed from its identity from this
     * classes collection of such objects.
     *
     * @param identity The identity of the RecoverableUnitImpl to be removed
     *
     * @return RecoverableUnitImpl The RecoverableUnitImpl thats no longer associated
     *         with the MultiScopeRecoveryLog.
     */
    protected RecoverableUnitImpl removeRecoverableUnitMapEntries(long identity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoverableUnitMapEntries", new Object[] { identity, this });

        final RecoverableUnitImpl recoverableUnit = (RecoverableUnitImpl) _recoverableUnits.remove(identity);

        if (recoverableUnit != null) {
            _recUnitIdTable.removeId(identity);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoverableUnitMapEntries", recoverableUnit);
        return recoverableUnit;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.getRecoverableUnit
    //------------------------------------------------------------------------------
    /**
     * Retrieves a RecoverableUnitImpl object, keyed from its identity from this
     * classes collection of such objects.
     *
     * @param identity The identity of the RecoverableUnitImpl to be retrieved
     *
     * @return RecoverableUnitImpl The required RecoverableUnitImpl
     */
    protected RecoverableUnitImpl getRecoverableUnit(long identity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoverableUnit", new Object[] { identity, this });

        RecoverableUnitImpl recoverableUnit = null;

        // Only attempt to resolve the recoverable unit if the log is compatible and valid.
        if (!incompatible() && !failed()) {
            recoverableUnit = (RecoverableUnitImpl) _recoverableUnits.get(identity);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoverableUnit", recoverableUnit);
        return recoverableUnit;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.serverName
    //------------------------------------------------------------------------------
    /**
     * Returns the name of the server that owns this object instance. This may not
     * necessarly be the local server as this object may represent a recovery log
     * owned by a peer server.
     *
     * @return String The server name
     */
    String serverName() {
        return _serverName;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.clientName
    //------------------------------------------------------------------------------
    /**
     * Returns the name of the client service that owns this object instance.
     *
     * @return String The client name.
     */
    String clientName() {
        return _clientName;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.clientVersion
    //------------------------------------------------------------------------------
    /**
     * Returns the version number of the client service that owns this object instance.
     *
     * @return int The client version number
     */
    public int clientVersion() {
        return _clientVersion;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.logName
    //------------------------------------------------------------------------------
    /**
     * Returns the log name.
     *
     * @return String The log name
     */
    public String logName() {
        return _logName;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.logIdentifier
    //------------------------------------------------------------------------------
    /**
     * Returns the log identifier.
     *
     * @return int The log identifier
     */
    public int logIdentifier() {
        return _logIdentifier;
    }

    /**
     * @return the _logDirectory
     */
    public String getLogDirectory() {
        return _logDirectory;
    }

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.toString
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
            _traceId = "MultiScopeRecoveryLog:" + "serverName=" + _serverName + ":"
                       + "clientName=" + _clientName + ":"
                       + "clientVersion=" + _clientVersion + ":"
                       + "logName=" + _logName + ":"
                       + "logIdentifier=" + _logIdentifier + " @"
                       + System.identityHashCode(this);

        return _traceId;
    }

    //------------------------------------------------------------------------------
    // Method: DistributedRecoveryLog.associateLog
    //------------------------------------------------------------------------------
    /**
     * Associates another log with this one. PI45254.
     * The code is protects against infinite recursion since associated logs are only marked as failed if
     * the log isn't already mark as failed.
     * The code does NOT protect against deadlock due to synchronization for logA->logB and logB->logA
     * - this is not an issue since failAssociated is only set to true for tranLog and not partnerLog
     * - this could be fixed for general use by delegating to an 'AssociatedLogGroup' object shared between associated logs.
     */
    @Override
    public void associateLog(DistributedRecoveryLog otherLog, boolean failAssociatedLog) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "associateLog", new Object[] { otherLog, failAssociatedLog, this });

        if (otherLog instanceof MultiScopeLog) {
            _associatedLog = (MultiScopeLog) otherLog;
            _failAssociatedLog = failAssociatedLog;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "associateLog");
    }

    /**
     * Called when logs fail. Provides more comprehensive FFDC - PI45254.
     * this is NOT synchronized to avoid deadlocks.
     */
    @Override
    public void provideServiceability() {
        Exception e = new Exception();
        try {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.provideServiceability", "2469", this);
            LogHandle lh = _logHandle;
            if (lh != null)
                FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.provideServiceability", "2471", lh);
            HashMap<Long, RecoverableUnit> rus = _recoverableUnits;
            if (rus != null)
                FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.MultiScopeRecoveryLog.provideServiceability", "2473", rus);
        } catch (Exception ex) {
            // Do nothing
        }
    }
}
