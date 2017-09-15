package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.SystemException;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.JTS.Configuration;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.LogFullException;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;

// PK84994

/**
 * The TransactionState interface provides operations that maintain the
 * relative commitment state of a TransactionImpl object.
 */
public class TransactionState
{
    /**
     * A state value indicating that the transaction has not yet been started or is finished
     */
    public final static int STATE_NONE = -1;

    /**
     * A state value indicating that the transaction has been started, and not yet
     * completed.
     */
    public final static int STATE_ACTIVE = 0;

    /**
     * A state value indicating that the transaction is in the process of being
     * prepared.
     */
    public final static int STATE_PREPARING = 1;

    /**
     * A state value indicating that the transaction has been successfully prepared,
     * but commit or rollback has not yet started.
     */
    public final static int STATE_PREPARED = 2;

    /**
     * A state value indicating that the transaction is in the process of being
     * committed in one phase. Note: this value should not get logged and so
     * it takes a value outside the range which is currently logged.
     */
    public final static int STATE_COMMITTING_ONE_PHASE = 10;

    /**
     * A state value indicating that the transaction is in the process of being
     * committed.
     */
    public final static int STATE_COMMITTING = 3;

    /**
     * A state value indicating that the transaction has been committed.
     */
    public final static int STATE_COMMITTED = 4;

    /**
     * A state value indicating that the transaction is in the process of being
     * rolled back.
     */
    public final static int STATE_ROLLING_BACK = 5;

    /**
     * A state value indicating that the transaction has been rolled back.
     */
    public final static int STATE_ROLLED_BACK = 6;

    /**
     * A state value indicating that the transaction has commited with
     * heuristics from the Resources.
     */
    public final static int STATE_HEURISTIC_ON_COMMIT = 7;

    /**
     * A state value indicating that the transaction has rolled back with
     * heuristics from the Resources.
     */
    public final static int STATE_HEURISTIC_ON_ROLLBACK = 8;

    /**
     * A state value indicating that the transaction has prepared all
     * enlisted two-phase Resources and is about to attempt last
     * particpant support based completion.
     */
    public final static int STATE_LAST_PARTICIPANT = 9;

    private static final TraceComponent tc = Tr.register(TransactionState.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * A state holder. This takes one of the int values defined above. Note: on distributed platforms this state
     * value is written to the log asis. Thus for release-release compatibility, the logged values must remain the
     * same for at least the states STATE_NONE thru STATE_LAST_PARTICIPANT.
     */
    protected int _state;

    protected TransactionImpl _tran;
    protected RecoverableUnit _tranLog;
    protected RecoverableUnitSection _logSection;
    protected boolean _loggingFailed;

    // We only ever need to access the partnerlogtable for the local server (ie never for peer
    // servers) so cache a direct reference to the PLT for the local server.
    protected static final PartnerLogTable _partnerLogTable;

    static
    {
        final FailureScopeController fsc = Configuration.getFailureScopeController();
        if (fsc != null)
        {
            _partnerLogTable = fsc.getPartnerLogTable();
        }
        else
        {
            _partnerLogTable = null;
        }
    }

    // Byte buffer to hold transaction state for log
    protected byte[] byteData = new byte[1];

    // Defect 1441
    //
    // State table showing valid state transitions for both
    // one-phase and two-phase transactions.
    //
    protected final static boolean[][] validStateChange = {
                                                           /* from to actve ping pd cing cd ring rd h_com h_rb lastp cingop Comments -------------------------- */
/* active */                                               { false, true, false, false, true, true, false, false, false, false, true }, /* In-flight */
                                                           /* preparing */{ false, false, true, true, true, true, true, true, true, true, false }, /* flow xa_prepare */
                                                           /* prepared */{ false, false, false, true, false, true, false, false, false, true, false }, /* subord only; force */
                                                           /* committing */{ false, false, false, false, true, false, false, true, false, false, false }, /*
                                                                                                                                                           * forced in root;
                                                                                                                                                           * non-forced in subord
                                                                                                                                                           */
                                                           /* committed */{ false, false, false, false, false, false, false, false, false, false, false }, /* non-forced write */
                                                           /* rolling_back */{ false, false, false, false, false, false, true, false, true, false, false }, /*
                                                                                                                                                             * forced in root;
                                                                                                                                                             * non-forced in subord
                                                                                                                                                             */
                                                           /* rolled_back */{ false, false, false, false, false, false, false, false, false, false, false }, /*                                      */
                                                           /* heur_on_commit */{ false, false, false, false, true, false, true, false, false, false, false }, /*
                                                                                                                                                               * Subordinate only;
                                                                                                                                                               * forced
                                                                                                                                                               */
                                                           /* heur_on_rollback */{ false, false, false, false, false, false, true, false, false, false, false }, /*
                                                                                                                                                                  * Subordinate
                                                                                                                                                                  * only; forced
                                                                                                                                                                  */
                                                           /* last_participant */{ false, false, false, true, false, true, false, false, false, false, false }, /*
                                                                                                                                                                 * LPS force if
                                                                                                                                                                 * configured
                                                                                                                                                                 */
                                                           /* committing_one_phase */{ false, false, false, false, true, false, true, false, false, false, false } }; /*
                                                                                                                                                                       * root commit
                                                                                                                                                                       * one phase
                                                                                                                                                                       */

    /**
     * Default TransactionState constructor
     * On recovery, the state is overwritten by the state from the log.
     */
    public TransactionState(TransactionImpl tran)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "TransactionState", new java.lang.Object[] { this, tran });

        _state = STATE_ACTIVE;
        _tran = tran;
    }

    /**
     * Directs the TransactionState to recover its state after a failure, based on
     * the given RecoverableUnit object. If the TransactionState has already been
     * defined or recovered, the operation returns the current state of the
     * transaction. If the state cannot be recovered, the operation returns none.
     * If the RecoverableUnit records information prior to a log record being
     * forced, this may result in recovery of an in-flight transaction. The
     * TransactionState returns active in this case.
     * 
     * @param log The RecoverableUnit for the transaction.
     * 
     * @return The current state of the transaction.
     */
    public int reconstruct(RecoverableUnit log) throws SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reconstruct", new Object[] { this, log });

        int result = STATE_NONE;
        int logState = 0;

        // Lookup the TransactionState RecoverableUnitSection
        _logSection = log.lookupSection(TransactionImpl.TRAN_STATE_SECTION);

        if (_logSection != null)
        {
            try
            {
                final byte[] logData = _logSection.lastData();
                if (logData.length == 1)
                {
                    logState = logData[0] & 0xff;
                    // Set the state value to be returned from the reconstruct method
                    switch (logState)
                    {
                        case STATE_PREPARED:
                        case STATE_COMMITTING:
                        case STATE_COMMITTED:
                        case STATE_ROLLING_BACK:
                        case STATE_ROLLED_BACK:
                        case STATE_HEURISTIC_ON_COMMIT:
                        case STATE_HEURISTIC_ON_ROLLBACK:
                        case STATE_LAST_PARTICIPANT:
                            result = logState;
                            break;
                        case STATE_NONE:
                        case STATE_ACTIVE:
                        case STATE_PREPARING:
                        default:
                            throw new SystemException("Transaction recovered in invalid state");
                    }
                }
                else
                {
                    // If the log record data is invalid, then exit immediately.
                    throw new SystemException("Invalid transaction state record data in log");
                }
            } catch (Throwable e)
            {
                FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TransactionState.reconstruct", "274", this);
                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "reconstruct", "com.ibm.tx.jta.impl.TransactionState", e });
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unable to access transaction state log record data");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "reconstruct");
                throw new SystemException(e.toString());
            }
        }
        else
        {
            // PK84994 starts here
            // If the state record is not found, then exit immediately with state set to STATE_NONE.
            // Log an FFDC to show this has happened
            FFDCFilter.processException(new InternalLogException(), "com.ibm.tx.jta.impl.TransactionState.setState", "277", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "No log record data for transaction state - returning NONE");
            _state = STATE_NONE;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "reconstruct", stateToString(_state));
            return _state;
            // PK84994 ends here        
        }

        _state = result;
        _tranLog = log;

        // Create a global identifier.
        final RecoverableUnitSection gtidSection = log.lookupSection(TransactionImpl.GLOBALID_SECTION);
        if (gtidSection != null)
        {
            try
            {
                final byte[] logData = gtidSection.lastData();

                if (logData.length > 12) // We must have formatId and lengths encoded
                {
                    final XidImpl xid = new XidImpl(logData, 0);
                    _tran.setXidImpl(xid);
                }
                else
                {
                    // If the log record data is invalid, then exit immediately.
                    throw new SystemException("Invalid transaction global identifier record data in log");
                }
            } catch (Throwable e)
            {
                FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TransactionState.reconstruct", "334", this);
                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "reconstruct", "com.ibm.tx.jta.impl.TransactionState", e });
                if (tc.isEventEnabled())
                    Tr.event(tc, "Unable to access global transaction id log record data");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "reconstruct");
                throw new SystemException(e.toString());
            }
        }
        else
        {
            // PK84994 starts here
            // If the global transaction id record is not found, then exit immediately with state set to STATE_NONE.
            // Log an FFDC to show this has happened
            FFDCFilter.processException(new InternalLogException(), "com.ibm.tx.jta.impl.TransactionState.setState", "336", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "No log record data for global transaction id - returning NONE");
            _state = STATE_NONE;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "reconstruct", stateToString(_state));
            return _state;
            // PK84994 ends here        
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reconstruct", stateToString(result));
        return result;
    }

    /**
     * Sets the state to the given value and returns true. If the state change is
     * invalid, the state is not changed and the operation returns false. When a
     * top-level transaction has its state changed to prepared, the information
     * is stored in the RecoverableUnit object. When prepared_success, the log
     * record for the transaction is explicitly forced. Otherwise the transaction
     * will be treated as in-flight upon recovery (i.e. will be rolled back). When
     * a subordinate transaction has its state changed to committing or
     * rolling_back, the state is added to the RecoverableUnit object, but is
     * not forced.
     * 
     * @param newState The new state of the transaction.
     * 
     * @return Indicates if the state change is possible.
     */
    public void setState(int newState) throws SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setState", "from " + stateToString(_state) + " to " + stateToString(newState));

        final int oldState = _state;

        // Check that the state change is valid
        if (validStateChange[_state][newState])
        {
            // Change state.  This is the point at which some log records may be written
            _state = newState;

            //
            // Unconditional add of state information to 
            // log for various states.
            //
            if ((newState == STATE_COMMITTING) ||
                (newState == STATE_LAST_PARTICIPANT) ||
                (_tran.isSubordinate() &&
                (newState == STATE_PREPARED ||
                 newState == STATE_HEURISTIC_ON_COMMIT ||
                newState == STATE_HEURISTIC_ON_ROLLBACK
                )
                ))
            {
                byteData[0] = (byte) _state;

                // For a recovered transaction (peer or local), _logSection is always populated by the reconstruct call. As
                // a result we don't need to worry here about which log (ie a peer or a local) to use.
                if (_logSection != null)
                {
                    try
                    {
                        _logSection.addData(byteData);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "State logged ok");
                    } catch (Exception e)
                    {
                        FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TransactionState.setState", "416", this);
                        if (tc.isEventEnabled())
                            Tr.event(tc, "addData failed during setState!");
                    }
                }
                else if (!_loggingFailed) // First time through only - trap any failed attempts
                {
                    try
                    {
                        _tranLog = _tran.getLog();

                        if (_tranLog != null)
                        {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Create sections in the Log for Recovery Coordinator"); // PK84994
                            // Create sections in the Log for Recovery coordinator                                         // PK84994

                            if (_tran.isRAImport())
                            {
                                //  We were imported from an RA
                                //  Write the recoveryId of the partner log entry for this provider to the tran log
                                //  and the imported JCA Xid
                                final JCARecoveryData jcard = _tran.getJCARecoveryData();

                                if (tc.isEventEnabled())
                                    Tr.event(tc, "TX is imported from provider: " + jcard.getWrapper().getProviderId());

                                jcard.logRecoveryEntry();

                                final RecoverableUnitSection raSection = _tranLog.createSection(TransactionImpl.RESOURCE_ADAPTER_SECTION, true);

                                final byte[] longBytes = Util.longToBytes(jcard.getRecoveryId()); // @249308A
                                final byte[] xidBytes = ((XidImpl) _tran.getJCAXid()).toBytes(); // @249308A
                                final byte[] logBytes = new byte[xidBytes.length + longBytes.length]; // @249308A
                                System.arraycopy(longBytes, 0, logBytes, 0, longBytes.length); // @249308A
                                System.arraycopy(xidBytes, 0, logBytes, longBytes.length, xidBytes.length); // @249308A

                                raSection.addData(logBytes); // @249308C
                            }
                            else if (_tran.isSubordinate())
                            {
                                logSupOrRecCoord();
                            }
                            else if (tc.isEventEnabled())
                            {
                                Tr.event(tc, "Superior transaction.");
                            }
                            // PK84994 starts here
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Create sections in the Log for XID and TransactionState");
                            // Create sections in the Log for XID and TransactionState
                            RecoverableUnitSection gtidSection = _tranLog.createSection(TransactionImpl.GLOBALID_SECTION, true);
                            gtidSection.addData(_tran.getXidImpl().toBytes());
                            _logSection = _tranLog.createSection(TransactionImpl.TRAN_STATE_SECTION, true);
                            _logSection.addData(byteData);
                            // PK84994 ends here
                        }
                    } catch (Exception e)
                    {
                        FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TransactionState.setState", "408", this);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "setState : CreateSection failed", e);
                        _logSection = null; // Reset this so we do not write a rollback to the log
                        _loggingFailed = true; // Force no further subordinate logging
                        throw new SystemException(e.toString());
                    }
                }
            }

            // Defect 1441
            //
            // Do we need to write to the log? Have
            // we written any data to our section?
            //
            if (_logSection != null)
            {
                //
                // Conditional add of state data to the log.
                // this only happens if a RecoverableUnitSection
                // has been created previously. This stops 1PC
                // transactions from having any state written to
                // the log.
                //
                if (newState == STATE_COMMITTED ||
                    newState == STATE_ROLLED_BACK ||
                    newState == STATE_ROLLING_BACK ||
                    (_tran.isSubordinate() && newState == STATE_HEURISTIC_ON_ROLLBACK))
                {
                    byteData[0] = (byte) _state;

                    try
                    {
                        _logSection.addData(byteData);
                    } catch (Exception e)
                    {
                        FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TransactionState.setState", "500", this);
                        if (tc.isEventEnabled())
                            Tr.event(tc, "addData failed during setState!");
                    }
                }

                try
                {
                    switch (newState)
                    {
                        case STATE_PREPARED:
                            //
                            // Subordinate transactions need to force a
                            // write before returning vote to root tran.
                            //
                            if (_tran.isSubordinate())
                            {
                                _tranLog.forceSections();
                            }
                            break;

                        case STATE_COMMITTING:
                        case STATE_ROLLING_BACK:
                            //
                            // If we are a root or RA transaction then we need
                            // to force logging once our completion direction
                            // has been chosen. Normally subordinates simply do an
                            // unforced write, but we always force since we reply
                            // early on commit and if a resource has failed and we
                            // crash, the superior may have already forgotten everything.
                            // 
                            _tranLog.forceSections();
                            break;

                        case STATE_HEURISTIC_ON_COMMIT:
                        case STATE_HEURISTIC_ON_ROLLBACK:

                            // Always force heuristic and we only do it on a subordinate...
                            if (_tran.isSubordinate())
                            {
                                // Need to force sections here for the case where
                                // we get a heuristic on prepare
                                _tranLog.forceSections();
                            }
                            break;

                        case STATE_COMMITTED:
                        case STATE_ROLLED_BACK:
                            //
                            // Transaction has finished completion so write
                            // a non-forced log update.
                            //
                            // We only need to update the state to the log
                            // No need to log as we will be logging a removeRecoverableUnit anyway
                            // _logSection.write();
                            break;

                        case STATE_LAST_PARTICIPANT:
                            // All 2PC resources have been prepared and we 
                            // are waiting on the outcome of the 1PC last
                            // participant before confirming our completion
                            // direction.
                            _tranLog.forceSections();
                            break;
                    }
                } catch (Exception e)
                {
                    FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TransactionState.setState", "505", this);
                    if (_partnerLogTable != null)
                        FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.TransactionState.setState", "506", _partnerLogTable);
                    if (e instanceof LogFullException)
                        Tr.error(tc, "WTRN0083_LOG_FULL_ERROR", _tran.getTranName());
                    else
                        Tr.error(tc, "WTRN0066_LOG_WRITE_ERROR", e);
                    // If this is the first log write and it fails, then stop further logging
                    if (oldState == STATE_PREPARING)
                    {
                        _logSection = null; // Reset this so we do not write a rollback to the log
                        _loggingFailed = true; // Force no further subordinate logging
                        _state = oldState; // Reset state so we can rollback
                    }
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "setState: Error writing log record", e);
                    throw new SystemException(e.toString());
                }
            }
        }
        else
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "TransactionState change FAILED from: " + stateToString(_state) + ", to: " + stateToString(newState));
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setState");
            throw new SystemException();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setState");
    }

    protected void logSupOrRecCoord() throws Exception
    {
        // Not used in JTM
    }

    public void setCommittingStateUnlogged()
    {
        if ((_state == STATE_PREPARING) || (_state == STATE_LAST_PARTICIPANT))
        {
            _state = STATE_COMMITTING;
        }
    }

    public void setRollingBackStateUnlogged()
    {
        if ((_state == STATE_PREPARING) || (_state == STATE_LAST_PARTICIPANT))
        {
            _state = STATE_ROLLING_BACK;
        }
    }

    public int getState()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getState", stateToString(_state));
        return _state;
    }

    public void reset()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "reset");
        _state = STATE_NONE;
    }

    public static String stateToString(int state)
    {
        switch (state)
        {
            case STATE_NONE:
                return "STATE_NONE";
            case STATE_ACTIVE:
                return "STATE_ACTIVE";
            case STATE_PREPARING:
                return "STATE_PREPARING";
            case STATE_PREPARED:
                return "STATE_PREPARED";
            case STATE_COMMITTING_ONE_PHASE:
                return "STATE_COMMITTING_ONE_PHASE";
            case STATE_COMMITTING:
                return "STATE_COMMITTING";
            case STATE_COMMITTED:
                return "STATE_COMMITTED";
            case STATE_ROLLING_BACK:
                return "STATE_ROLLING_BACK";
            case STATE_ROLLED_BACK:
                return "STATE_ROLLED_BACK";
            case STATE_HEURISTIC_ON_COMMIT:
                return "STATE_HEURISTIC_ON_COMMIT";
            case STATE_HEURISTIC_ON_ROLLBACK:
                return "STATE_HEURISTIC_ON_ROLLBACK";
            case STATE_LAST_PARTICIPANT:
                return "STATE_LAST_PARTICIPANT";
            default:
                return "STATE_NONE";
        }
    }

    @Override
    public String toString()
    {
        return stateToString(_state);
    }
}
