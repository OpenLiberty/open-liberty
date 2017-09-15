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

import java.io.PrintWriter;
import java.io.StringWriter;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.alarm.Alarm;
import com.ibm.tx.util.alarm.AlarmListener;
import com.ibm.tx.util.alarm.AlarmManager;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/**
 * This class records state for timing out transactions, and runs a thread
 * which performs occasional checks to time out transactions.
 */
public class TimeoutManager
{
    private static final TraceComponent tc = Tr.register(
                                                         TimeoutManager.class
                                                         , TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * Constants which define the types of timeout possible.
     */
    public static final int CANCEL_TIMEOUT = 0;
    public static final int NO_TIMEOUT = 0;
    public static final int ACTIVE_TIMEOUT = 1;
    public static final int IN_DOUBT_TIMEOUT = 2;
    public static final int REPEAT_TIMEOUT = 3;
    public static final int INACTIVITY_TIMEOUT = 4;
    public static final int SR_TERMINATION_TIMEOUT = 5;

    /**
     * Sets the timeout for the transaction to the specified type and time in
     * seconds.
     * <p>
     * If the type is none, the timeout for the transaction is
     * cancelled, otherwise the current timeout for the transaction is modified
     * to be of the new type and duration.
     * 
     * @param localTID The local identifier for the transaction.
     * @param timeoutType The type of timeout to establish.
     * @param seconds The length of the timeout.
     * 
     * @return Indicates success of the operation.
     */
    public static void setTimeout(TransactionImpl tran, int timeoutType, int seconds)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setTimeout",
                     new Object[] { tran, timeoutType, seconds });

        switch (timeoutType)
        {
        // If the new type is active or in_doubt, then create a new TimeoutInfo
        // if
        // necessary, and set up the type and interval.
            case TimeoutManager.ACTIVE_TIMEOUT:
            case TimeoutManager.IN_DOUBT_TIMEOUT:
            case TimeoutManager.REPEAT_TIMEOUT:
                TimeoutInfo info = tran.setTimeoutInfo(new TimeoutInfo(tran, seconds, timeoutType));

                if (tc.isDebugEnabled() && info != null
                    && timeoutType != TimeoutManager.REPEAT_TIMEOUT)
                    Tr.debug(tc, "Found existing timeout for transaction: " + info);
                // not expecting this, should we cancel it?

                break;

            // For any other type, remove the timeout if there is one.
            default:
                info = tran.getTimeoutInfo();
                if (null != info)
                {
                    tran.setTimeoutInfo(null);
                    info.cancelAlarm();
                }
                else
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "Failed to find existing timeout for transaction: "
                                                 + tran);
                }

                break;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setTimeout");
    }

    /**
     * This class records information for a timeout for a transaction.
     */
    public static class TimeoutInfo implements AlarmListener
    {
        protected final TransactionImpl _tran;
        protected final int _duration;
        protected final int _timeoutType; // = TimeoutManager.NO_TIMEOUT;
        private Alarm _alarm;

        private final AlarmManager _alarmManager = ConfigurationProviderManager.getConfigurationProvider().getAlarmManager();

        protected TimeoutInfo(TransactionImpl tran, int duration, int type)
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "TimeoutInfo", tran);

            _tran = tran;
            _duration = duration;
            _timeoutType = type;

            _alarm = _alarmManager.scheduleAlarm(_duration * 1000l, this, null);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "TimeoutInfo");
        }

        /**
         * Takes appropriate action for a timeout.
         * The entry in the pendingTimeouts hashtable will be removed by
         * the transaction completion code.
         */
        @Override
        public void alarm(Object alarmContext)
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "alarm", _tran);

            switch (_timeoutType)
            {
            // If active, then attempt to roll the transaction back.
                case TimeoutManager.ACTIVE_TIMEOUT:
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Transaction timeout", _tran);
                    Tr.info(tc, "WTRN0006_TRANSACTION_HAS_TIMED_OUT", new Object[]
                    { _tran.getTranName(), new Integer(_duration) });

                    final Thread thread = _tran.getMostRecentThread();

                    if (thread != null)
                    {
                        final StackTraceElement[] stack = thread.getStackTrace();

                        final StringWriter writer = new StringWriter();
                        final PrintWriter printWriter = new PrintWriter(writer);

                        printWriter.println();

                        for (StackTraceElement element : stack)
                        {
                            printWriter.println("\t" + element);
                        }

                        Tr.info(tc, "WTRN0124_TIMED_OUT_TRANSACTION_STACK", new Object[] { thread, writer.getBuffer() });
                    }

                    _tran.timeoutTransaction(true);
                    break;

                case TimeoutManager.REPEAT_TIMEOUT:
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Transaction repeat timeout", _tran);
                    _tran.timeoutTransaction(false);
                    break;

                // If in doubt, then replay_completion needs to be driven.
                // This is done by telling the TransactionImpl to act as
                // if in recovery.  
                case TimeoutManager.IN_DOUBT_TIMEOUT:
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

            if (tc.isEntryEnabled())
                Tr.exit(tc, "alarm");
        }

        public void cancelAlarm()
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "cancelAlarm", _alarm);

            if (_alarm != null)
            {
                _alarm.cancel();
                _alarm = null;
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "cancelAlarm");
        }
    }

    protected static String getThreadId(Thread thread)
    {
        final StringBuffer buffer = new StringBuffer();

        // pad the HexString ThreadId so that it is always 8 characters long
        String tid = Long.toHexString(thread.getId());

        int length = tid.length();

        for (int i = length; i < 8; ++i)
        {
            buffer.append('0');
        }

        buffer.append(tid);

        return buffer.toString();
    }
}