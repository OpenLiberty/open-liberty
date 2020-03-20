package com.ibm.tx.jta.embeddable.impl;

/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.impl.TimeoutManager;
import com.ibm.tx.jta.impl.TransactionImpl;

/**
 * This class records state for timing out transactions, and runs a thread
 * which performs occasional checks to time out transactions.
 */
public class EmbeddableTimeoutManager extends TimeoutManager {
    private static final TraceComponent tc = Tr.register(
                                                         EmbeddableTimeoutManager.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * table of transactions and associated timeoutinfo structures
     */
    private static final Hashtable<EmbeddableTransactionImpl, TimeoutInfo> inactivityTimeouts = new Hashtable<EmbeddableTransactionImpl, TimeoutInfo>();

    /**
     * Sets the timeout for the transaction to the specified type and time in
     * seconds.
     * <p>
     * If the type is none, the timeout for the transaction is
     * cancelled, otherwise the current timeout for the transaction is modified
     * to be of the new type and duration.
     *
     * @param localTID    The local identifier for the transaction.
     * @param timeoutType The type of timeout to establish.
     * @param seconds     The length of the timeout.
     *
     * @return Indicates success of the operation.
     */
    public static void setTimeout(EmbeddableTransactionImpl tran, int timeoutType, int seconds) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "setTimeout", new Object[] { tran, timeoutType, seconds });

        // assert tran != null ... throw IllegalArgumentException ???
        if (tran == null)
            throw new IllegalArgumentException("setTimeout called with null tran");

        TimeoutInfo info = null;

        switch (timeoutType) {
            // If the new type is active or in_doubt, then create a new TimeoutInfo
            // if
            // necessary, and set up the type and interval.
            case TimeoutManager.ACTIVE_TIMEOUT:
            case TimeoutManager.IN_DOUBT_TIMEOUT:
            case TimeoutManager.REPEAT_TIMEOUT:
                info = (TimeoutInfo) tran.setTimeoutInfo(new TimeoutInfo(tran, seconds, timeoutType));

                if (traceOn && tc.isDebugEnabled() && info != null
                    && timeoutType != TimeoutManager.REPEAT_TIMEOUT)
                    Tr.debug(tc, "Found existing timeout for transaction: " + info);
                // not expecting this, should we cancel it?

                break;

            case TimeoutManager.INACTIVITY_TIMEOUT:
                if (seconds == 0) // cancel
                {
                    info = inactivityTimeouts.remove(tran);
                    if (null != info) {
                        info.cancelAlarm();
                    } else {
                        if (traceOn && tc.isDebugEnabled())
                            Tr.debug(tc,
                                     "Failed to find existing timeout for transaction: "
                                         + tran);
                    }
                } else {
                    info = new TimeoutInfo(tran, seconds, timeoutType);
                    info = inactivityTimeouts.put(tran, info);

                    if (traceOn && tc.isDebugEnabled() && info != null)
                        Tr.debug(tc,
                                 "Found existing inactivity timeout for transaction: "
                                     + info);
                    // not expecting this, should we cancel it?
                }

                break;

            // For any other type, remove the timeout if there is one.
            default:
                info = (TimeoutInfo) tran.setTimeoutInfo(null);
                if (null != info) {
                    info.cancelAlarm();
                } else {
                    if (traceOn && tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "Failed to find existing timeout for transaction: "
                                     + tran);
                }

                break;
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "setTimeout");
    }

    /**
     * This class records information for a timeout for a transaction.
     */
    static class TimeoutInfo extends TimeoutManager.TimeoutInfo {
        TimeoutInfo(TransactionImpl tran, int duration, int type) {
            super(tran, duration, type);
        }

        /**
         * Takes appropriate action for a timeout.
         * The entry in the pendingTimeouts hashtable will be removed by
         * the transaction completion code.
         */
        @Override
        public void alarm(Object alarmContext) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(tc, "alarm", _tran);

            switch (_timeoutType) {
                // If active, then attempt to roll the transaction back.
                case TimeoutManager.ACTIVE_TIMEOUT:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Transaction timeout", _tran);

                    _tran.timeoutTransaction(true);

                    Tr.info(tc, "WTRN0006_TRANSACTION_HAS_TIMED_OUT", new Object[] { _tran.getTranName(), _duration });

                    final Thread thread = _tran.getMostRecentThread();
                    if (thread != null) {
                        final StackTraceElement[] stack = thread.getStackTrace();

                        final StringWriter writer = new StringWriter();
                        final PrintWriter printWriter = new PrintWriter(writer);

                        printWriter.println();

                        for (StackTraceElement element : stack) {
                            printWriter.println("\t" + element);
                        }

                        Tr.info(tc, "WTRN0124_TIMED_OUT_TRANSACTION_STACK", new Object[] { thread, writer.getBuffer() });
                    }
                    break;

                case TimeoutManager.REPEAT_TIMEOUT:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Transaction repeat timeout", _tran);
                    _tran.timeoutTransaction(false);
                    break;

                case TimeoutManager.INACTIVITY_TIMEOUT:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Transaction inactivity timeout", _tran);
                    Tr.info(tc, "WTRN0080_CLIENT_INACTIVITY_TIMEOUT", new Object[] { _tran.getTranName(), new Integer(_duration) });

                    inactivityTimeouts.remove(_tran);
                    ((EmbeddableTransactionImpl) _tran).inactivityTimeout();
                    break;

                // If in doubt, then replay_completion needs to be driven.
                // This is done by telling the TransactionImpl to act as
                // if in recovery.
                case TimeoutManager.IN_DOUBT_TIMEOUT:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "In doubt timeout", _tran);
                    // Remove the pending timer entry.  Do this here as the recover code
                    // may be called without using the timer.  We need to remove it first
                    // as the recover code could restart another timer.   Active timeout does
                    // not need to remove the timer as it will eventually rollback which
                    // will remove any timer entries.
                    _tran.setTimeoutInfo(null);
                    _tran.recover();
                    break;

                default: // Otherwise do nothing.
                    break;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "alarm");
        }
    }
}