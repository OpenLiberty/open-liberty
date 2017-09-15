package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.DistributedRecoveryLog;
import com.ibm.ws.recoverylog.spi.RecoveryLog;

public class PartnerLogTable {
    private static final TraceComponent tc = Tr.register(PartnerLogTable.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    // A PartnerLogTable is a list which holds PartnerLogData entries.  The PartnerLogData
    // entry consists of a RecoveryWrapper object (_logData) (serialized and
    // unserialized) and an Id.  The _serializedLogData is only present if the _logData has
    // been logged to disk.  The Id is an long identifier which is unique to the logs.
    // This Id is a key (recoverableUnit id) to the Partner (XAresource)s log and is referenced
    // in the transaction logs when a resource (or WSCoordinator) is logged, eg connects the
    // XAResource back to its XAResourceFactory/XAResourceInfo.  The Id in the PartnerLogData
    // may not be the same as the entry position number in the partnerLogTable.  It is the
    // latter (+1 so it's never zero) which is returned on a
    // TranManagerSet.registerResourceInfo call and used in an enlist  call.

    // There are 2 PartnerLogTables created for each FailureScopeController.
    //
    // 1) is created when the FailureScopeController is created.  It is used for all new transaction
    // access under that failure scope, eg registerResourceInfo calls, etc.  This one is used for
    // the current server instance - when we recover other servers, as we never create new transaction
    // it will never get used.  This PLT is created before the partner log is openned - in the HA
    // world, we may get registerResourceInfo calls made before we access the partner log - the
    // data only needs to be pushed to disk at first prepare time.   Thus we need to go back to
    // the FSC to get access to the log when required.
    //
    // 2) is created on recovery of a partner log (together with transactions recovery).  This
    // is created whenever a partner log is openned for recovery (whether for this server or recovering
    // for another server).  The PartnerLogTable is created by RecoveryManager and is given the
    // FailureScopeController.  The FailureScopeController holds references to the RecoveryLog in use.
    // Once recovery has completed for this failure scope, the entries referred by it will be deleted
    // from the partner log.

    // In the split process model, the PLT in this server is only a subset view of entries in the
    // actual partner log which this server has needed access to.  When an entry lookup is performed,
    // if we do not find it in the PLT, we go and re-read the log in case another server has already
    // logged that data.

    protected final ArrayList<PartnerLogData> _partnerLogTable;
    protected final FailureScopeController _failureScopeController;

    private final ReentrantReadWriteLock _pltLock = new ReentrantReadWriteLock();
    private final Lock _pltReadLock = _pltLock.readLock();
    private final Lock _pltWriteLock = _pltLock.writeLock();

    public PartnerLogTable(FailureScopeController failureScopeController) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "PartnerLogTable", failureScopeController);

        _partnerLogTable = new ArrayList<PartnerLogData>();
        _failureScopeController = failureScopeController;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "PartnerLogTable");
    }

    /**
     * Return the entry in the recovery table at the given
     * index, or null if the index is out of the table's bounds
     * 
     * The supplied index is actually one greater than the index into the table.
     * 
     * @param index the index in the table of the PartnerLogData object
     * 
     * @return the entry in the recovery table at the given
     *         index, or null if the index is out of the table's bounds
     */
    public PartnerLogData getEntry(int index) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getEntry", index);

        _pltReadLock.lock();

        try {
            final PartnerLogData entry = _partnerLogTable.get(index - 1);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getEntry", entry);
            return entry;
        } catch (IndexOutOfBoundsException ioobe) {
            // The index was invalid; return null.
            FFDCFilter.processException(ioobe, "com.ibm.ws.Transaction.JTA.PartnerLogTable.getEntry", "122", this);
        } finally {
            _pltReadLock.unlock();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getEntry", null);
        return null;
    }

    /**
     * Add an entry at the end of the recovery table.
     * 
     * @param logData the PartnerLogData object to add to the table
     * 
     * @return result the entry index into the table
     */
    public void addEntry(PartnerLogData logData) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addEntry", logData);

        _pltWriteLock.lock();

        try {
            addPartnerEntry(logData);
        } finally {
            _pltWriteLock.unlock();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addEntry");
    }

    // You'd better be surrounded by write lock/unlock calls when you call this
    protected void addPartnerEntry(PartnerLogData logData) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addPartnerEntry", logData);

        final int result = _partnerLogTable.size();

        _partnerLogTable.add(result, logData);

        // Let the entry know where it's been put
        // Actually, fool it into thinking it's one entry higher to invalidate 0
        logData.setIndex(result + 1);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addPartnerEntry", result + 1);
    }

    //
    // This method searches the recoveryTable for an entry with matching recoveryId.
    //
    // This method may be called from recovery or during runtime (eg reconnect a resource)
    // and on shutdown.
    //
    public PartnerLogData findEntry(long recoveryId) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "findEntry", recoveryId);

        _pltWriteLock.lock();

        try {
            for (PartnerLogData pld : _partnerLogTable) {
                if (recoveryId == pld.getRecoveryId()) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "findEntry", pld);
                    return pld;
                }
            }
        } finally {
            _pltWriteLock.unlock();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "findEntry", null);
        return null;
    }

    /**
     * This method searches the partner log table for an entry with matching wrapper.
     * If an entry does not exist, one is created and added to the table. It should
     * only be accessing the "runtime" table.
     * 
     * Called from: TranManagerSet.registerResourceInfo
     * TranManagerSet.registerJCAProvider
     * TransactionState.setState when logging a superior coord on a subordinate at prepare time
     * WSCoordinatorWrapper.log when logging a subordinate coord on a superior at prepare time
     * RecoveryManager.shutdown when checking for a subordinate or superior coord at shutdown
     * 
     * @param rw
     * @return
     */
    public PartnerLogData findEntry(RecoveryWrapper rw) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "findEntry", rw);

        PartnerLogData entry;

        _pltWriteLock.lock();

        try {
            // Search the partner log table...
            for (PartnerLogData pld : _partnerLogTable) {
                final RecoveryWrapper nextWrapper = pld.getLogData();
                if (rw.isSameAs(nextWrapper)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Found entry in table");
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "findEntry", pld.getIndex());
                    return pld;
                }
            }

            //
            // We have never seen this wrapper before,
            // so we need to add it to the recoveryTable
            //
            entry = rw.container(_failureScopeController);
            addPartnerEntry(entry);
        } finally {
            _pltWriteLock.unlock();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "findEntry", entry);
        return entry;
    }

    //
    // Prior to shutdown, terminate all partner log entries
    // so the partner log is not being updated when we shutdown.
    //
    public void terminate() // 172471
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "terminate");

        for (PartnerLogData pld : _partnerLogTable) {
            pld.terminate();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "terminate");
    }

    /**
     * We are shutting down the server. Cycle through the recovery records and delete those which are
     * marked not recovered. (These are either real not recoverd records or records that belong to
     * active transactions.) If there are no recovery records left, then we zap the files, otherwise
     * we need to rewrite a shutdown record.
     * 
     * @return whether there are entries we need to keep
     */
    public boolean shutdown() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "shutdown");

        boolean result = false;
        final RecoveryLog partnerLog = _failureScopeController.getPartnerLog();

        _pltWriteLock.lock();

        try {
            for (PartnerLogData pld : _partnerLogTable) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "processing: ", pld);
                if (pld.getRecovered()) {
                    if (pld._loggedToDisk && partnerLog != null) {
                        try {
                            // Need to remove this from the log
                            partnerLog.removeRecoverableUnit(pld.getRecoveryId());
                        } catch (Exception e) {
                            FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.PartnerLogTable.shutdown", "364");
                            Tr.error(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "shutdown", "com.ibm.ws.Transaction.JTA.PartnerLog", e });
                            Tr.event(tc, "XAResources log - removeRecoverableUnit failed:", e);
                            result = true; // Couldnt remove entry from log, so it's still there
                        }
                    }
                } else {
                    if (!pld._loggedToDisk && partnerLog != null) {
                        // This is an error. We have a txn which refers to an entry which is not logged
                        Tr.warning(tc, "WTRN0039_SERIALIZE_FAILED");
                    } else {
                        result = true;
                    }
                }
            }
        } finally {
            _pltWriteLock.unlock();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "shutdown", result);
        return result;
    }

    /**
     * Scans through the partners listed in this table and instructs each of them to clear themselves from
     * the recovery log if they are not associated with current transactions. Entries remain in the table
     * an can be re-logged during if they are used again.
     */
    public void clearUnused() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "clearUnused");

        final RecoveryLog partnerLog = _failureScopeController.getPartnerLog();

        _pltWriteLock.lock();

        try {
            boolean cleared = false;

            for (PartnerLogData pld : _partnerLogTable) {
                cleared |= pld.clearIfNotInUse();
            }

            try {
                if (cleared) {
                    ((DistributedRecoveryLog) partnerLog).keypoint();
                }
            } catch (Exception exc) {
                // The keypoint operation has failed. There is very little we can do
                // other than continue with the close logic. This should be the the
                // that access to the log. There is nothing helpfull we can do at
                // this point.
            }
        } finally {
            _pltWriteLock.unlock();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "clearUnused");
    }

    /**
     * Determine XA RMs needing recovery.
     * 
     * <p>
     * For each resource manager known to the transaction service all
     * indoubt transactions are located.
     * 
     * @param cl A class loader for contacting XA RMs
     */
    public boolean recover(RecoveryManager recoveryManager, ClassLoader cl, Xid[] xids) {
        boolean success = true; // flag to indicate that we recovered all RMs

        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover", new Object[] { this, _failureScopeController.serverName() });

        final int restartEpoch = recoveryManager.getCurrentEpoch();
        final byte[] cruuid = recoveryManager.getApplId();

        for (PartnerLogData pld : _partnerLogTable) {
            if (!pld.recover(cl, xids, null, cruuid, restartEpoch)) {
                success = false;
            }

            // Determine if shutdown processing started on another thread. 
            // If it has, no further action can be taken.
            if (recoveryManager.shutdownInProgress()) {
                success = false;
                break;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover", success);
        return success;
    }

    /**
     * @param partnerLogTable
     */
    public void merge(PartnerLogTable incoming) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "merge", incoming);

        if (incoming != null) {
            final PartnerLogData[] incomingItems = incoming.toArray();

            // loop through those partners that are being merged, to check
            // for duplicate entries that have conflicting recovered flags

            // The following is attempting to find identical resources within the array that have conflicting recovered flags.  
            // Each instance of an unrecovered RecoveryWrapper in the incoming array is compared against any following instances 
            // of recovered objects in the array.  If a recovered object is a matching instance of the unrecovered RecoveryWrapper, 
            // that object is changed to be flagged as unrecovered.

            if (incomingItems != null) {
                for (int i = 0; i < (incomingItems.length) - 1; i++) {
                    if (!(incomingItems[i].getRecovered())) {
                        RecoveryWrapper thisWrapper = incomingItems[i].getLogData();
                        if (thisWrapper != null) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "processing thisWrapper: ", incomingItems[i]);
                            for (int k = i + 1; k < incomingItems.length; k++) {
                                if (incomingItems[k].getRecovered()) {
                                    RecoveryWrapper nextWrapper = incomingItems[k].getLogData();
                                    if (nextWrapper != null) {
                                        if (thisWrapper.isSameAs(nextWrapper)) {
                                            incomingItems[k].setRecovered(false);
                                            if (tc.isDebugEnabled())
                                                Tr.debug(tc, "processing isSameAs: ", incomingItems[k]);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (incomingItems != null) {
                for (int i = 0; i < incomingItems.length; i++) {
                    // We're shutting down so we don't care that the index field gets reset
                    addEntry(incomingItems[i]);
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "merge");
    }

    private PartnerLogData[] toArray() {
        _pltWriteLock.lock();

        try {
            final PartnerLogData[] result = new PartnerLogData[_partnerLogTable.size()];

            return _partnerLogTable.toArray(result);
        } finally {
            _pltWriteLock.unlock();
        }
    }
}