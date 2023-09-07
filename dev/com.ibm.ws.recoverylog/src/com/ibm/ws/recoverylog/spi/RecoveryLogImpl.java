/*******************************************************************************
 * Copyright (c) 2003, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

public class RecoveryLogImpl implements DistributedRecoveryLog {
    private static final TraceComponent tc = Tr.register(RecoveryLogImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    private final MultiScopeLog _recoveryLog;
    private final FailureScope _failureScope;

    /**
     * <p>
     * Each recovery log is contained within a FailureScope. For example, the
     * transaction service on a distributed system has a transaction log in each
     * server node (ie in each FailureScope). Because of this, the caller must
     * specify the FailureScope for recovery log being created.
     * </p>
     *
     * @param recoveryLog  MultiScopeLog delegate
     * @param failureScope FailureScope criteria used to filter the delegates
     */
    public RecoveryLogImpl(MultiScopeLog recoveryLog, FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "RecoveryLogImpl", recoveryLog, failureScope);

        _recoveryLog = recoveryLog;
        _failureScope = failureScope;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "RecoveryLogImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.openLog
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void openLog() throws LogCorruptedException, LogAllocationException, InternalLogException, LogIncompatibleException {
        _recoveryLog.openLog();
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLog
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void closeLog() throws InternalLogException {
        _recoveryLog.closeLog();
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLog
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void closeLog(byte[] serviceData) throws InternalLogException {
        _recoveryLog.closeLog(serviceData);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLogImmediate
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void closeLogImmediate() throws InternalLogException {
        _recoveryLog.closeLogImmediate();
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.recoveryCompelte
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void recoveryComplete() throws LogClosedException, InternalLogException, LogIncompatibleException {
        _recoveryLog.recoveryComplete();
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.recoveryComplete
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void recoveryComplete(byte[] serviceData) throws LogClosedException, InternalLogException, LogIncompatibleException {
        _recoveryLog.recoveryComplete(serviceData);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.keypoint
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void keypoint() throws LogClosedException, InternalLogException, LogIncompatibleException {
        _recoveryLog.keypoint();
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.serviceData
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public byte[] serviceData() throws LogClosedException, InternalLogException {
        return _recoveryLog.serviceData();
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.createRecoverableUnit
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public RecoverableUnit createRecoverableUnit() throws LogClosedException, InternalLogException, LogIncompatibleException {
        return _recoveryLog.createRecoverableUnit(_failureScope);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.removeRecoverableUnit
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public void removeRecoverableUnit(long identity) throws LogClosedException, InvalidRecoverableUnitException, InternalLogException, LogIncompatibleException {
        _recoveryLog.removeRecoverableUnit(identity);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.lookupRecoverableUnit
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public RecoverableUnit lookupRecoverableUnit(long identity) throws LogClosedException {
        return _recoveryLog.lookupRecoverableUnit(identity);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.recoverableUnits
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public LogCursor recoverableUnits() throws LogClosedException {
        return _recoveryLog.recoverableUnits(_failureScope);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.logProperties
    //------------------------------------------------------------------------------
    @Override
    @Trivial
    public LogProperties logProperties() {
        return _recoveryLog.logProperties();
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.toString
    //------------------------------------------------------------------------------
    /**
     * Returns the string representation of this object instance.
     *
     * @return String The string representation of this object instance.
     */
    @Override
    public String toString() {
        return "" + _recoveryLog + " [" + _failureScope + "]";
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.getMultiScopeLog
    //------------------------------------------------------------------------------
    /**
     * Returns the MultiScopeLog wrappered by this object.
     *
     * @return MultiScopeLog MultiScopeLog wrappered by this object instance.
     */
    @Trivial
    private MultiScopeLog getMultiScopeLog() {
        return _recoveryLog;
    }

    //------------------------------------------------------------------------------
    // Method: DistributedRecoveryLog.associateLog
    //------------------------------------------------------------------------------
    /**
     * Associates another log with this one.
     */
    @Override
    @Trivial
    public void associateLog(DistributedRecoveryLog otherLog, boolean failAssociatedLog) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "associateLog {0} {1} {2}", otherLog, failAssociatedLog, this);

        if (otherLog instanceof RecoveryLogImpl)
            _recoveryLog.associateLog(((RecoveryLogImpl) otherLog).getMultiScopeLog(), failAssociatedLog);
        else
            _recoveryLog.associateLog(otherLog, failAssociatedLog);
    }

    @Override
    @Trivial
    public boolean delete() {
        return _recoveryLog.delete();
    }

    @Override
    @Trivial
    public void retainLogsInPeerRecoveryEnv(boolean retainLogs) {
        _recoveryLog.retainLogsInPeerRecoveryEnv(retainLogs);
    }

    @Override
    @Trivial
    public boolean failed() {
        return _recoveryLog.failed();
    }
}