/*******************************************************************************
 * Copyright (c) 2001, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.impl;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.rsadapter.AdapterUtil;
import com.ibm.ws.rsadapter.exceptions.*;
import javax.resource.ResourceException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class implements the Transaction state machine.
 * 
 * <P>
 * Methods are <B>final </B> because I do not expect this class to be sub
 * classed and final methods can be inlined by the JIT compiler for a 3X
 * performance improvement.
 * 
 * <p>
 * This class is here even though only SPI code uses it for 2 reasons.
 * <ol>
 * <li>For packaging convenience as System 390 code will replace the SPI code
 * and keep the CCI code. Then they could continue to use the old TX states in
 * the new SPI code.
 * <li>We want to insulate the Connection Handle against changes for Sys390
 * e.g. the connection handle could call isValid() and if that signature changes
 * for Sys390 then we would have to change CCI code too
 * </ol>
 * 
 * <p>
 * Moved back to SPI since the only user of this code is SPI
 * 
 * <P>
 * <B><U>Transaction States </U> </B>
 * <ul>
 * <li>TRANSACTION_FAIL
 * <li>GLOBAL_TRANSACTION_ACTIVE
 * <li>LOCAL_TRANSACTION_ACTIVE
 * <li>TRANSACTION_ENDING
 * <li>NO_TRANSACTION_ACTIVE
 * <li>TRANSACTION_HEURISTIC_END
 * <li>RRS_GLOBAL_TRANSACTION_ACTIVE
 * </UL>
 * 
 * <p>
 * <B><U>Actions </B> </U>
 * <ol>
 * <li>LT_BEGIN
 * <li>LT_COMMIT
 * <li>LT_ROLLBACK
 * <li>XA_START
 * <li>XA_END
 * <li>XA_END_FAIL
 * <li>XA_COMMIT
 * <li>XA_ROLLBACK
 * <li>XA_FORGET
 * <li>EXECUTE
 * <li>MC_CLEANUP
 * <li>HEURISTIC_END
 * <li>XA_READONLY
 * </ol>
 * 
 * @see javax.resources.cci.Connection
 */
public class WSStateManager {
    // these are transaction states
    // There could be multiple connection handles with their own connection
    // states
    public static final int NO_TRANSACTION_ACTIVE = 0;

    public static final int LOCAL_TRANSACTION_ACTIVE = 1;

    public static final int GLOBAL_TRANSACTION_ACTIVE = 2;

    public static final int TRANSACTION_FAIL = 3;

    public static final int TRANSACTION_ENDING = 4;

    public static final int TRANSACTION_HEURISTIC_END = 5;

    public static final int RECOVERY_IN_PROGRESS = 6; 

    public static final int RRS_GLOBAL_TRANSACTION_ACTIVE = 7; 

    private static final String[] transactions = { "NO_TRANSACTION_ACTIVE",
                                                    "LOCAL_TRANSACTION_ACTIVE", "GLOBAL_TRANSACTION_ACTIVE",
                                                    "TRANSACTION_FAIL", "TRANSACTION_ENDING",
                                                    "TRANSACTION_HEURISTIC_END", "RECOVERY_IN_PROGRESS",
                                                    "RRS_GLOBAL_TRANSACTION_ACTIVE", 
                                                    "INVALID_TX_STATE" };

    // constant for actions
    public static final int LT_BEGIN = 1;

    public static final int LT_COMMIT = 2;

    public static final int LT_ROLLBACK = 3;

    public static final int XA_START = 4;

    public static final int XA_END = 5;

    public static final int XA_END_FAIL = 6;

    public static final int XA_COMMIT = 7;

    public static final int XA_ROLLBACK = 8;

    public static final int XA_RECOVER = 9;

    public static final int XA_FORGET = 10;

    public static final int EXECUTE = 11;

    public static final int MC_CLEANUP = 12;

    public static final int HEURISTIC_END = 13;

    public static final int XA_READONLY = 14;

    private static final String[] actions = { "INVALID_ACTION", "LT_BEGIN",
                                               "LT_COMMIT", "LT_ROLLBACK", "XA_START", "XA_END", "XA_END_FAIL",
                                               "XA_COMMIT", "XA_ROLLBACK", "XA_RECOVER", "XA_FORGET", "EXECUTE",
                                               "MC_CLEANUP", "HEURISTIC_END", "INVALID_ACTION", "XA_READONLY" 
    };

    // The Transaction State
    int transtate = NO_TRANSACTION_ACTIVE;

    //  Trace
    private static final TraceComponent tc = Tr.register(WSStateManager.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE); 

    /**
     * Constructor
     */
    public WSStateManager() {}

    /**
     * Set a new transaction state only if the state transition is valid.
     * 
     * <p>
     * <b>Caller is responsible </b> to synchronize calls to set the state.
     * 
     * @param newAction
     *            incoming action that will set state.
     * @param validateOnly
     *            Need validation only (true) or do you actually want to set the
     *            state (false).
     * @exception TransactionException
     */
    private final void setState(int newAction, boolean validateOnly) throws TransactionException {
        final String methodName = "setState";
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.entry(this, tc, methodName, getStateAsString() + " Action: " + actions[newAction]);

        switch (newAction) {
        // Allow only NO_TRANSACTION_ACTIVE -> GLOBAL_TRANSACTION_ACTIVE
            case XA_START:
                if (transtate == NO_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }
                    transtate = GLOBAL_TRANSACTION_ACTIVE;
                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // Allow only GLOBAL_TRANSACTION_ACTIVE -> TRANSACTION_ENDING
            case XA_END:
                if (transtate == GLOBAL_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }

                    transtate = TRANSACTION_ENDING;
                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // Allow only GLOBAL_TRANSACTION_ACTIVE -> TRANSACTION_ENDING
            case XA_END_FAIL:
                if (transtate == GLOBAL_TRANSACTION_ACTIVE || transtate == TRANSACTION_ENDING) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }

                    transtate = TRANSACTION_FAIL;
                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // Allow only TRANSACTION_ENDING-->NO_TRANSACTION_ACTIVE
            case XA_COMMIT:
            case XA_READONLY: 
                if (transtate == TRANSACTION_ENDING || transtate == RECOVERY_IN_PROGRESS) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }

                    transtate = NO_TRANSACTION_ACTIVE;
                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // Allow only TRANSACTION_ENDING-->NO_TRANSACTION_ACTIVE
            case XA_ROLLBACK:
                if (transtate == TRANSACTION_ENDING || transtate == TRANSACTION_FAIL || transtate == RECOVERY_IN_PROGRESS) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }
                    // In the event of a TRANSACTION_FAIL
                    // we dont want other threads to use this transaction until mc
                    // cleanup occurs
                    // so we keep this TX dirty i.e. in TRANSACTION_FAIL state.
                    if (transtate == TRANSACTION_ENDING || transtate == RECOVERY_IN_PROGRESS) {
                        transtate = NO_TRANSACTION_ACTIVE;
                    }

                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // Set the state to RECOVERY_IN_PROGRESS no matter what the starting
            // state was.
            case XA_RECOVER:
                transtate = RECOVERY_IN_PROGRESS; 
                break;

            // Send to NO_TRANSACTION_ACTIVE no matter what the starting state was.
            // This is the only time we can clean up our state for XA.
            // Even tho the DB would be in a rational state we would be in an in
            // inconsistent state.
            // so let us reset to NO_TRANSACTION_ACTIVE
            case XA_FORGET:
                transtate = NO_TRANSACTION_ACTIVE;
                break;

            // LT.BEGIN: only allow NO_TRANSACTION_ACTIVE ->
            // LOCAL_TRANSACTION_ACTIVE
            case LT_BEGIN:
                if (transtate == NO_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }
                    transtate = LOCAL_TRANSACTION_ACTIVE;
                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // Allow only Local_Transaction_active -> NO_TRANSACTION_Active
            case LT_COMMIT:
            case LT_ROLLBACK:
                if (transtate == LOCAL_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }

                    transtate = NO_TRANSACTION_ACTIVE;
                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], false); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // Allow only in transaction_active
            case EXECUTE:
                if ((transtate == LOCAL_TRANSACTION_ACTIVE) || (transtate == GLOBAL_TRANSACTION_ACTIVE)) {
                    if (validateOnly) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(this, tc, methodName);
                        return;
                    }
                } else {
                    TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(this, tc, methodName, txe);
                    throw txe;
                }
                break;

            // CLEANUP - always set to NO_TRANSACTION_ACTIVE
            case MC_CLEANUP:
                transtate = NO_TRANSACTION_ACTIVE;
                break;

            // Send to TRANSACTION_HEURISTIC_END no matter what the starting state
            // was.
            case HEURISTIC_END:
                transtate = TRANSACTION_HEURISTIC_END;
                break;

            default:
                TransactionException txe = new TransactionException(actions[newAction], transactions[transtate], true); 
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(this, tc, methodName, txe);
                throw txe;
        };

        if (isTraceOn && tc.isEntryEnabled()) 
            Tr.exit(this, tc, methodName, getStateAsString());
    }

    /**
     * Return the current state
     * 
     * @return int Current transaction state
     */
    public final int getState() {
        return transtate;

    }

    public final void setState(int newAction) throws TransactionException {
        setState(newAction, false);
    }

    /**
     * Sets the state of the transaction with no validation on the new state.
     * Only MC.associateConnection should call this method
     * 
     * @param int
     *        state
     */

    public final void setStateNoValidate(int state) {
        transtate = state;
    }

    /**
     * If the action is valid return null. Otherwise return a
     * TransactionException with the cause.
     * 
     * @param newAction
     *            int
     * @return TransactionException
     */
    public final ResourceException isValid(int newAction) {
        try {
            setState(newAction, true);
        } catch (TransactionException exp) {
            FFDCFilter.processException(exp, "com.ibm.ws.rsadapter.spi.WSStateManager.isValid", "385", this);
            return exp;
        }

        return null;
    }

    /**
     * Return a string representation of the current transaction state
     * 
     * <p>
     * <u>Warning: </u> This method is intended for use by tracing only. Please
     * do not use the returned strings in your logic as they may change at any
     * time.
     * 
     * @return String - value of the transaction state
     */
    public final String getStateAsString() {
        return transactions[transtate];
    }

}