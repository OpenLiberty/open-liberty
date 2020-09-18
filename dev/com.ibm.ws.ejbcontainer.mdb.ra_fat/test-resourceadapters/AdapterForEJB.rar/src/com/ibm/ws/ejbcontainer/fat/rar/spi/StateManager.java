/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.spi;

import java.util.logging.Logger;

import javax.resource.ResourceException;

/**
 * This class implements the Transaction state machine.
 *
 * <P>Methods are <B>final</B> because I do not expect this class to be
 * sub classed and final methods can be inlined by the JIT compiler
 * for a 3X performance improvement.
 *
 * <p>This class is here even though only SPI code uses it for 2 reasons.
 * <ol>
 * <li>For packaging convenience as System 390 code will replace the SPI code and keep
 * the CCI code. Then they could continue to use the old TX states in the new SPI code.
 * <li>We want to insulate the Connection Handle against changes for Sys390 e.g. the
 * connection handle could call isValid() and if that signature changes for Sys390 then
 * we would have to change CCI code too
 * </ol>
 *
 * <p>Moved back to SPI since the only user of this code is SPI
 *
 * <P><B><U>Transaction States </U></B>
 * <ul>
 * <li>TRANSACTION_FAIL
 * <li>GLOBAL_TRANSACTION_ACTIVE
 * <li>LOCAL_TRANSACTION_ACTIVE
 * <li>TRANSACTION_ENDING
 * <li>NO_TRANSACTION_ACTIVE
 * <li>TRANSACTION_HEURISTIC_END
 * </UL>
 *
 * <p><B><U>Actions</B></U>
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
public class StateManager {
    private final static String CLASSNAME = StateManager.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // these are transaction states
    // There could be multiple connection handles with their own connection states
    public static final int NO_TRANSACTION_ACTIVE = 0;
    public static final int LOCAL_TRANSACTION_ACTIVE = 1;
    public static final int GLOBAL_TRANSACTION_ACTIVE = 2;
    public static final int TRANSACTION_FAIL = 3;
    public static final int TRANSACTION_ENDING = 4;
    public static final int TRANSACTION_HEURISTIC_END = 5;
    public static final int RECOVERY_IN_PROGRESS = 6; // d131094

    public static final String[] transactions = {
                                                  "NO_TRANSACTION_ACTIVE",
                                                  "LOCAL_TRANSACTION_ACTIVE",
                                                  "GLOBAL_TRANSACTION_ACTIVE",
                                                  "TRANSACTION_FAIL",
                                                  "TRANSACTION_ENDING",
                                                  "TRANSACTION_HEURISTIC_END",
                                                  "RECOVERY_IN_PROGRESS",
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
    //d117074
    public static final int XA_READONLY = 14;

    public static final String[] actions = {
                                             "INVALID_ACTION",
                                             "LT_BEGIN",
                                             "LT_COMMIT",
                                             "LT_ROLLBACK",
                                             "XA_START",
                                             "XA_END",
                                             "XA_END_FAIL",
                                             "XA_COMMIT",
                                             "XA_ROLLBACK",
                                             "XA_RECOVER",
                                             "XA_FORGET",
                                             "EXECUTE",
                                             "MC_CLEANUP",
                                             "HEURISTIC_END",
                                             "INVALID_ACTION",
                                             "XA_READONLY" //d117074
    };

    // The Transaction State
    int transtate = NO_TRANSACTION_ACTIVE;

    /**
     * Constructor
     */
    public StateManager() {
    }

    /**
     * Set a new transaction state only if the state transition is valid.
     *
     * <p><b>Caller is responsible</b> to synchronize calls to set the state.
     *
     * @param newAction incoming action that will set state.
     * @param validateOnly
     *            Need validation only (true) or do you actually want to set the state (false).
     * @exception ResourceException
     */
    private final void setState(int newAction, boolean validateOnly) throws ResourceException {
        svLogger.entering(CLASSNAME, "setState", getStateAsString());

        switch (newAction) {
            // Allow only NO_TRANSACTION_ACTIVE -> GLOBAL_TRANSACTION_ACTIVE
            case XA_START:
                if (transtate == NO_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        return;
                    }
                    transtate = GLOBAL_TRANSACTION_ACTIVE;
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // Allow only GLOBAL_TRANSACTION_ACTIVE -> TRANSACTION_ENDING
            case XA_END:
                if (transtate == GLOBAL_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        return;
                    }

                    transtate = TRANSACTION_ENDING;
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // Allow only GLOBAL_TRANSACTION_ACTIVE -> TRANSACTION_ENDING
            case XA_END_FAIL:
                if (transtate == GLOBAL_TRANSACTION_ACTIVE || transtate == TRANSACTION_ENDING) //133434
                {
                    if (validateOnly) {
                        return;
                    }

                    transtate = TRANSACTION_FAIL;
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // Allow only  TRANSACTION_ENDING-->NO_TRANSACTION_ACTIVE
            case XA_COMMIT:
            case XA_READONLY: //d117074
                if (transtate == TRANSACTION_ENDING || transtate == RECOVERY_IN_PROGRESS) // d131094
                {
                    if (validateOnly) {
                        return;
                    }

                    transtate = NO_TRANSACTION_ACTIVE;
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // Allow only  TRANSACTION_ENDING-->NO_TRANSACTION_ACTIVE
            case XA_ROLLBACK:
                if (transtate == TRANSACTION_ENDING || transtate == TRANSACTION_FAIL || transtate == RECOVERY_IN_PROGRESS) // d131094
                {
                    if (validateOnly) {
                        return;
                    }
                    // In the event of a TRANSACTION_FAIL
                    // we dont want other threads to use this transaction until mc cleanup occurs
                    // so we keep this TX dirty i.e. in TRANSACTION_FAIL state.
                    if (transtate == TRANSACTION_ENDING || transtate == RECOVERY_IN_PROGRESS) {
                        transtate = NO_TRANSACTION_ACTIVE;
                    }
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // Set the state to RECOVERY_IN_PROGRESS no matter what the starting state was.
            case XA_RECOVER:
                transtate = RECOVERY_IN_PROGRESS; // d131094
                break;
            // Send to NO_TRANSACTION_ACTIVE no matter what the starting state was.
            // This is the only time we can clean up our state for XA.
            // Even tho the DB would be in a rational state we would be in an in inconsistent state.
            // so let us reset to NO_TRANSACTION_ACTIVE
            case XA_FORGET:
                transtate = NO_TRANSACTION_ACTIVE;
                break;
            // LT.BEGIN: only allow NO_TRANSACTION_ACTIVE -> LOCAL_TRANSACTION_ACTIVE
            case LT_BEGIN:
                if (transtate == NO_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        return;
                    }
                    transtate = LOCAL_TRANSACTION_ACTIVE;
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // Allow only Local_Transaction_active -> NO_TRANSACTION_Active
            case LT_COMMIT:
            case LT_ROLLBACK:
                if (transtate == LOCAL_TRANSACTION_ACTIVE) {
                    if (validateOnly) {
                        return;
                    }

                    transtate = NO_TRANSACTION_ACTIVE;
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // Allow only in transaction_active
            case EXECUTE:
                if ((transtate == LOCAL_TRANSACTION_ACTIVE) || (transtate == GLOBAL_TRANSACTION_ACTIVE)) {
                    if (validateOnly)
                        return;
                } else {
                    throw new ResourceException(actions[newAction], transactions[transtate]);
                }
                break;
            // CLEANUP - always set to NO_TRANSACTION_ACTIVE
            case MC_CLEANUP:
                transtate = NO_TRANSACTION_ACTIVE;
                break;
            // Send to TRANSACTION_HEURISTIC_END no matter what the starting state was.
            case HEURISTIC_END:
                transtate = TRANSACTION_HEURISTIC_END;
                break;
            default:
                throw new ResourceException(actions[newAction], transactions[transtate]);
        };

        svLogger.exiting(CLASSNAME, "setState", getStateAsString());
    }

    /**
     * Return the current state
     *
     * @return int Current transaction state
     */
    public final int getState() {
        return transtate;
    }

    public final void setState(int newAction) throws ResourceException {
        setState(newAction, false);
    }

    //@debrae
    /**
     * Sets the state of the transaction with no validation on the new state.
     * Only MC.associateConnection should call this method
     *
     * @param int state
     */
    public final void setStateNoValidate(int state) {
        transtate = state;
    }

    /**
     * If the action is valid return null. Otherwise return a ResourceException
     * with the cause.
     *
     * @param newAction int
     * @return ResourceException
     */
    public final ResourceException isValid(int newAction) {
        try {
            setState(newAction, true);
        } catch (ResourceException exp) {
            return exp;
        }

        return null;
    }

    /**
     * Return a string representation of the current transaction state
     *
     * <p><u>Warning:</u> This method is intended for use by tracing only. Please do not
     * use the returned strings in your logic as they may change at any time.
     *
     * @return String - value of the transaction state
     */
    public final String getStateAsString() {
        return transactions[transtate];
    }
}