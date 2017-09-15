/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

public class RecoveryLogImpl implements DistributedRecoveryLog
{
    private static final TraceComponent tc = Tr.register(RecoveryLogImpl.class, TraceConstants.TRACE_GROUP, null);

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
     * @param recoveryLog MultiScopeLog delegate
     * @param failureScope FailureScope criteria used to filter the delegates
     */
    public RecoveryLogImpl(MultiScopeLog recoveryLog, FailureScope failureScope)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "RecoveryLogImpl", new Object[] { recoveryLog, failureScope });

        _recoveryLog = recoveryLog;
        _failureScope = failureScope;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "RecoveryLogImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.openLog
    //------------------------------------------------------------------------------
    @Override
    public void openLog() throws LogCorruptedException, LogAllocationException, InternalLogException,
                    LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "openLog", this);

        _recoveryLog.openLog();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "openLog");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLog
    //------------------------------------------------------------------------------
    @Override
    public void closeLog() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLog", this);

        _recoveryLog.closeLog();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLog
    //------------------------------------------------------------------------------
    @Override
    public void closeLog(byte[] serviceData) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLog", new Object[] { serviceData, this });

        _recoveryLog.closeLog(serviceData);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLogImmediate
    //------------------------------------------------------------------------------
    @Override
    public void closeLogImmediate() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLogImmediate", this);

        _recoveryLog.closeLogImmediate();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLogImmediate");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.recoveryCompelte
    //------------------------------------------------------------------------------
    @Override
    public void recoveryComplete() throws LogClosedException, InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", this);

        _recoveryLog.recoveryComplete();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.recoveryComplete
    //------------------------------------------------------------------------------
    @Override
    public void recoveryComplete(byte[] serviceData) throws LogClosedException, InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", new Object[] { serviceData, this });

        _recoveryLog.recoveryComplete(serviceData);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryComplete");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.keypoint
    //------------------------------------------------------------------------------
    @Override
    public void keypoint() throws LogClosedException, InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypoint", this);

        _recoveryLog.keypoint();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypoint");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.serviceData
    //------------------------------------------------------------------------------
    @Override
    public byte[] serviceData() throws LogClosedException, InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serviceData", this);

        byte[] serviceData = _recoveryLog.serviceData();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serviceData", RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES));
        return serviceData;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.createRecoverableUnit
    //------------------------------------------------------------------------------
    @Override
    public RecoverableUnit createRecoverableUnit() throws LogClosedException, InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoverableUnit", this);

        final RecoverableUnit runit = _recoveryLog.createRecoverableUnit(_failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoverableUnit", runit);
        return runit;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.removeRecoverableUnit
    //------------------------------------------------------------------------------
    @Override
    public void removeRecoverableUnit(long identity) throws LogClosedException, InvalidRecoverableUnitException, InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeRecoverableUnit", new Object[] { new Long(identity), this });

        _recoveryLog.removeRecoverableUnit(identity);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeRecoverableUnit");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.lookupRecoverableUnit
    //------------------------------------------------------------------------------
    @Override
    public RecoverableUnit lookupRecoverableUnit(long identity) throws LogClosedException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lookupRecoverableUnit", new Object[] { new Long(identity), this });

        RecoverableUnit runit = _recoveryLog.lookupRecoverableUnit(identity);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lookupRecoverableUnit", runit);
        return runit;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.recoverableUnits
    //------------------------------------------------------------------------------
    @Override
    public LogCursor recoverableUnits() throws LogClosedException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoverableUnits", this);

        final LogCursor cursor = _recoveryLog.recoverableUnits(_failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoverableUnits", cursor);
        return cursor;
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.logProperties
    //------------------------------------------------------------------------------
    @Override
    public LogProperties logProperties()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logProperties", this);

        final LogProperties lprops = _recoveryLog.logProperties();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "logProperties", lprops);
        return lprops;
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
    public String toString()
    {
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
    private MultiScopeLog getMultiScopeLog()
    {
        return _recoveryLog;
    }

    //------------------------------------------------------------------------------
    // Method: DistributedRecoveryLog.associateLog
    //------------------------------------------------------------------------------
    /**
     * Associates another log with this one.
     */
    @Override
    public void associateLog(DistributedRecoveryLog otherLog, boolean failAssociatedLog)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "associateLog", new Object[] { otherLog, failAssociatedLog, this });

        if (otherLog instanceof RecoveryLogImpl)
            _recoveryLog.associateLog(((RecoveryLogImpl) otherLog).getMultiScopeLog(), failAssociatedLog);
        else
            _recoveryLog.associateLog(otherLog, failAssociatedLog);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "associateLog");
    }
}
