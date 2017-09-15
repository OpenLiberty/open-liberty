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

import java.io.NotSerializableException;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;
import com.ibm.ws.recoverylog.spi.RecoveryLog;
import com.ibm.ws.recoverylog.spi.WriteOperationFailedException;
import com.ibm.ws.recoverylog.spi.InternalLogException;                                // PM04949

public abstract class PartnerLogData
{
    private static final TraceComponent tc = Tr.register(PartnerLogData.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected byte[] _serializedLogData;
    protected RecoveryWrapper _logData;
    protected long _recoveryId;
    protected int _index;

    /**
    * Boolean flag to indicate if there the serialized log data in _serlaizedLogData
    * has been deserialized (recovered) into recovery wrapper _logData. For a freshly 
    * enlisted resource this is not applicable so the code just sets this to true 
    * (ie nothing to deserialize) for a recovered resource this flag starts out false 
    * (ie byte data has not yet been processed)
    */
    protected boolean _recovered;
    public boolean _terminating; // 172471 - flag that the entry is being terminated
    private boolean _logEarly;
    protected boolean _loggedToDisk;
    protected FailureScopeController _fsc;
    private RecoveryLog _partnerLog;

    // Set in use count to 1 - it will be decremented if we perform successful recovery
    // It is also incremented for each recovered transaction usage and decremented when
    // the recovered transaction completes.
    protected int _recoveredInUseCount = 1;

    // Section Identifier in log for this partner log data record
    protected int _sectionId;

    //
    // Ctor when called from registration/enlist 
    //
    // Allocate a new object with the firstFreeId value.  Note: firstFreeId needs to be updated
    // and this is done in getIterator() since we always search the table for matches before we
    // ever allocate a new object.  We save the FailureScopeController as the register/enlist
    // could be called before we have a partner log available.
    //
    public PartnerLogData(RecoveryWrapper logData, FailureScopeController fsc)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "PartnerLogData", new Object[]{logData,fsc});
        this._logData           = logData;
        this._recovered         = true;
        this._fsc               = fsc;
        if (tc.isEntryEnabled()) Tr.exit(tc, "PartnerLogData");
    }

    //
    // Ctor when called from recovery from the log
    //
    public PartnerLogData(byte[] serializedLogData, RecoveryWrapper logData, long id, RecoveryLog partnerLog)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "PartnerLogData", new Object[] {id,logData,partnerLog});
        this._serializedLogData = serializedLogData;
        this._logData           = logData;
        this._recoveryId        = id;
        this._loggedToDisk      = true;
        this._partnerLog        = partnerLog;
        if (tc.isEntryEnabled()) Tr.exit(tc, "PartnerLogData");
    }
    

    /*
     * Write the recovery data to to the partner log if it has not already
     * been written unless we are closing down or the RA is being terminated.
     * This will only occur in main-line calls as any recovered data will
     * already be written to disk.  We need to check if the terminate flag
     * is set either because of shutdown or because an RA is stopping, etc.
     */
    public synchronized void logRecoveryEntry() throws Exception
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "logRecoveryEntry", this);

        if (!_loggedToDisk)
        {
            if (_terminating) // d172471
            {
                Tr.warning(tc, "WTRN0084_RESOURCE_ENDING");
                if (tc.isEntryEnabled()) Tr.exit(tc, "logRecoveryEntry", "Terminating");
                throw new IllegalStateException();
            }

            if (_serializedLogData == null)
            {
                Tr.warning(tc, "WTRN0039_SERIALIZE_FAILED");
                if (tc.isEntryEnabled()) Tr.exit(tc, "logRecoveryEntry", "NotSerializable");
                throw new NotSerializableException("XAResource recovery information not serializable");
            }

            try
            {
                // Perform any pre-log data check (only used for XARecoveryData)
                preLogData();

                // Now write the partner log record
                logData(_sectionId);
            }
            catch (Exception e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.XARecoveryData.logRecoveryEntry", "284", this);
                if (tc.isEntryEnabled()) Tr.exit(tc, "logRecoveryEntry", e);
                throw e;
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "logRecoveryEntry");
    }


    /*
     * Default pre-log data check prior to logging the Partner Log Data.
     */
    protected void preLogData() throws Exception
    {
    }


    /*
     * Default post-log data check after logging the Partner Log Data prior to the force.
     */
    protected void postLogData(@SuppressWarnings("unused")RecoverableUnit ru) throws Exception
    {
    }


    //
    // Log the partner log data entry to the partner log.  This normally is invoked at prepare time.
    // We may need to get the recoveryLog from the FailureScopeController.  As we reject any prepares
    // if we do not yet have a recoveryLog available, we can be sure that the FailureScopeController
    // will have a log available once this is called, assuming logging is enabled.
    //
    protected void logData(int sectionId) throws Exception
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "logData", sectionId);

        // If unset, get from the FailureScopeController.  As its our log, we can never lose it.
        if (_partnerLog == null) _partnerLog = _fsc.getPartnerLog();
        
        // If still unset, we are running without logging.
        if (_partnerLog != null)
        {
            RecoverableUnit ru = null;

            try
            {
                if(_recoveryId == 0)
                {
                    ru = _partnerLog.createRecoverableUnit();
                    if (ru == null)                                                // PM04949 
                    {                                                              // PM04949
                        if (tc.isDebugEnabled()) Tr.debug(tc, "logData", "RecoverableUnit returned from createRecoverableUnit was null - throwing InternalLogException"); // PM04949
                        throw new InternalLogException(null);                      // PM04949
                    }                                                              // PM04949
                    // Can set this now we have a log record
                    _recoveryId = ru.identity();
                }
                else
                {
                    ru = _partnerLog.lookupRecoverableUnit(_recoveryId);
                    if (ru == null)                                                // PM04949
                    {                                                              // PM04949
                        if (tc.isDebugEnabled()) Tr.debug(tc, "logData", "RecoverableUnit returned from lookupRecoverableUnit for " + _recoveryId + " was null - throwing InternalLogException"); // PM04949
                        throw new InternalLogException(null);                      // PM04949
                    }                                                              // PM04949
                }
                
                if (tc.isDebugEnabled()) Tr.debug(tc, "logData", "section=" + sectionId + ", recoveryId=" + _recoveryId);
                
                final RecoverableUnitSection section = ru.createSection(sectionId, true);
                section.addData(_serializedLogData);
                postLogData(ru);
                ru.forceSections();
                _loggedToDisk = true;
            }
            catch (Exception e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.PartnerLogData.logData", "285", this);
                try
                {   // Remove the ru from the internal tables
                    if(ru != null)
                    {
                        _partnerLog.removeRecoverableUnit(ru.identity());
                        _recoveryId = 0;
                    } 
                }
                catch (Throwable t)
                {
                    FFDCFilter.processException(t, "com.ibm.ws.Transaction.JTA.PartnerLogData.logData", "156", this);
                }
                if (e instanceof WriteOperationFailedException)
                {
                    Tr.error(tc, "WTRN0066_LOG_WRITE_ERROR", e);
                }
                else
                {
                    Tr.error(tc, "WTRN0000_ERR_INT_ERROR", new Object[]{"logData", "com.ibm.ws.Transaction.JTA.PartnerLogData", e});
                } 
 
                if (tc.isEntryEnabled()) Tr.exit(tc, "logData", e);
                throw e;
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "logData");
    }

    public void setSerializedLogData(byte[] data)
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "setSerializedLogData", Util.toHexString(data));

        _serializedLogData = data;
    }

    public RecoveryWrapper getLogData()
    {
        return _logData;
    }

    public long getRecoveryId()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "getRecoveryId", _recoveryId);
        return _recoveryId;
    }

    public void setRecovered(boolean flag)
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "setRecovered", new Object[]{"From " + _recovered + " to " + flag, this});
        _recovered = flag;
    }

    public boolean getRecovered()
    {
        return _recovered;
    }

    public synchronized void terminate() // d172471
    {
        _terminating = true;
        if (tc.isDebugEnabled()) Tr.debug(tc, "terminate", this);
    }

    public String toString()
    {
        String data = (_logData == null) ? "<null>" : _logData.toString();
        return "index=" + _index + ", recoveryID=" + _recoveryId + ", recovered=" + _recovered + ", terminating=" + _terminating + ", loggedToDisk=" + _loggedToDisk + ", data=" + data + ", class=" + this.getClass().getName();
    }
    
    public void setIndex(int index)
    {
        _index = index;
    }
    
    public int getIndex()
    {
        return _index;
    }

    public void setLogEarly(boolean logEarly)
    {
        _logEarly = logEarly;
    }
    
    public boolean getLogEarly()
    {
        return _logEarly;
    }
    
    public synchronized void incrementCount()
    {
        _recoveredInUseCount++;
        if (tc.isDebugEnabled()) Tr.debug(tc, "incrementCount",
            new Object[] {_recoveryId, _recoveredInUseCount});
    }
    
    public synchronized int decrementCount()
    {
        _recoveredInUseCount--;
        if (tc.isDebugEnabled()) Tr.debug(tc, "decrementCount",
            new Object[] {_recoveryId, _recoveredInUseCount});
        return _recoveredInUseCount;
    }

    /**
    * Clears the recovery log record associated with this partner from the partner log, if this partner is not
    * associated with current transactions. If this partner is re-used, the logData call will allocate a new
    * recoverable unit and re-log the information back to the partner log.
    *
    * @return boolean true if the partner data was cleared from the log, otherwise false. 
    */
    public synchronized boolean clearIfNotInUse()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "clearIfNotInUse",new Object[]{_recoveryId, _recoveredInUseCount});

        boolean cleared = false;

        if (_loggedToDisk && _recoveredInUseCount == 0)
        {
            try
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "removing recoverable unit " + _recoveryId);
                // As it is logged to disk, we must have a _partnerLog available
                _partnerLog.removeRecoverableUnit(_recoveryId);
                _loggedToDisk = false;
                _recoveryId = 0;
                cleared = true;
            }
            catch (Exception e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.PartnerLogData.clearIfNotInUse", "218", this);
                if (e instanceof WriteOperationFailedException)
                {
                    Tr.error(tc, "WTRN0066_LOG_WRITE_ERROR", e);
                }
                else
                {
                    Tr.error(tc, "WTRN0000_ERR_INT_ERROR", new Object[]{"clearIfNotInUse", "com.ibm.ws.Transaction.JTA.PartnerLogData", e});
                } 
                // Just ignore the error and clean it up on the next server run
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "clearIfNotInUse", cleared);
        return cleared;
    }
    
    // Setter used for recovery as not set via constructor
    public void setFailureScopeController(FailureScopeController fsc)
    {
        _fsc = fsc;
    }

    // Default implementation for non-XA recovery data items
    public boolean recover(ClassLoader cl, Xid[] xids, byte[] failedStoken, byte[] cruuid, int restartEpoch)
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "recover", new Object[] {this, cl, xids, failedStoken, cruuid, restartEpoch});

        decrementCount();
        return true;
    }
}
