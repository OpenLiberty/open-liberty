/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter;

import com.ibm.adapter.message.FVTMessageProviderImpl;
import com.ibm.adapter.work.FVTWorkDispatcher;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 *         <p>This class is used check if the underlying EIS (messageProvider)
 *         fails or not. When the timer expires, check if EISFail is true.
 *         If so, need to do sth with the WorkDispatcher.
 *         The constructor should take the object of FVTMessageProviderImpl.</p>
 */
public class EISTimer implements Runnable {

    private static final TraceComponent tc = Tr.register(EISTimer.class);

    /** Default max expiry time for EIS Timer */
    private static final int defaultMaxTimeLeft = 5;

    /** State of the EIS timer */
    public static final int TIMER_STATE_NORMAL = 0;
    public static final int TIMER_STATE_ROLLBACK_COMPLETE = 1;
    public static final int TIMER_STATE_ROLLBACK_FAILURE = 2;

    /** Max expiry time */
    private int maxTimeLeft;

    /** Time counter */
    private int timeLeft;

    /** Message provider instance */
    private FVTMessageProviderImpl provider;

    /** WorkDispatcher instance */
    private FVTWorkDispatcher dispatcher;

    /*** current EIS status */
    private int currentEISStatus;

    /** last EIS status */
    private int previousEISStatus = FVTMessageProviderImpl.STATUS_OK;

    /** Whether RA is shutting down */
    private boolean raShutdown = false;

    /** Whether there is problem when rollback active transaction */
    private int EISTimerState = TIMER_STATE_NORMAL;

    /** Sync Object */
    private Object syncObj;

    /**
     * <p>Constructor.</p>
     * <p>This constructor uses the user defined maxTimeLeft value.</p>
     *
     * @param wd the FVTWorkDispatcher instance.
     * @param mp the FVTMessageProviderImpl instance.
     * @param maxTimer the user defined maxTimeLeft of the timer.
     *
     */
    // 10/22/03: 
    public EISTimer(FVTWorkDispatcher wd, FVTMessageProviderImpl mp, int maxTimer) {
        // 10/22/03: 
        // Modified the constructor of the EISTimer, add WorkDispatcher
        // this avoid provider need to know the WorkDispatcher instance.

        if (tc.isEntryEnabled())
            Tr.entry(tc, "EISTimer: <init>", new Object[] { mp, new Integer(maxTimer) });

        maxTimeLeft = maxTimer;
        timeLeft = maxTimeLeft;
        provider = mp;
        dispatcher = wd;

        if (tc.isEntryEnabled()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "WorkDispatcher on EISTimer init", dispatcher);
            Tr.exit(tc, "<init>", this);
        }

    }

    /**
     * <p>Constructor.</p>
     * <p>This constructor uses the default maxTimeLeft value.</p>
     *
     * @param wd the FVTWorkDispatcher instance.
     * @param mp the FVTMessageProviderImpl instance.
     *
     */
    // 10/22/03: 
    public EISTimer(FVTWorkDispatcher wd, FVTMessageProviderImpl mp) {
        // 10/22/03: 
        // Modified the constructor of the EISTimer, add WorkDispatcher
        // this avoid provider need to know the WorkDispatcher instance.
        this(wd, mp, defaultMaxTimeLeft);
    }

    @Override
    public void run() {
        try {
            while (!raShutdown) {
                if (tc.isEntryEnabled())
                    Tr.entry(tc, "EISTimer: run() - while loop", new Object[] { new Integer(timeLeft) });
                while ((timeLeft > 0) && !raShutdown) {
                    // Sleep for 1000 millisecond (1 sec)
                    Thread.sleep(1000);
                    timeLeft--;
                }

                // Check if RA is shutting down. If so, exit the loop without checking the EIS status.
                if (raShutdown) {
                    break;
                }

                currentEISStatus = provider.getEISStatus();

                // Check if the EIS fails after timer expires.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "run", "Check EIS status.");
                    Tr.debug(tc, "run", "Current EIS status: " + new String(currentEISStatus == 0 ? "Fail" : "OK"));
                    Tr.debug(tc, "run", "Previous EIS status: " + new String(previousEISStatus == 0 ? "Fail" : "OK"));
                }

                if (currentEISStatus == FVTMessageProviderImpl.STATUS_FAIL) {
                    if (previousEISStatus == FVTMessageProviderImpl.STATUS_OK) {
                        // Since EIS fails now, rollback all active transactions.
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", "EIS fails now. Rollback all active transactions.");

                        previousEISStatus = currentEISStatus;

                        // Here, rollback all active trans. Call a function in WorkDispatcher
                        // to do this.
                        if (dispatcher.rollbackAllActiveTrans()) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "run", "Rollback all active transactions completed.");
                            EISTimerState = TIMER_STATE_ROLLBACK_COMPLETE;
                        } else {
                            EISTimerState = TIMER_STATE_ROLLBACK_FAILURE;

                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "run", "Problems in rollback active transactions. ERROR!");
                        }

                        // Since the rollback is completed or failed, need to notify the EIS
                        // that the EISTimer/RA has finished the rollback.
                        synchronized (syncObj) {
                            syncObj.notifyAll();
                        }

                        // Since EIS is failed and rollback active transactions, reset the timer counter
                        // and check the status again in another maxTimeLeft seconds.
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", "EIS just failed and rollback active transactions. Check again in " + maxTimeLeft + " seconds.");
                    } else {
                        // Since EIS still fails, reset the timer counter and check the status again
                        // in another maxTimeLeft seconds.
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", "EIS still fails. Check again in " + maxTimeLeft + " seconds.");

                    }

                } else if (previousEISStatus == FVTMessageProviderImpl.STATUS_FAIL) {
                    // EIS is recovered now.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "run", "EIS recovers now. Set TimerState to NORMAL");

                    // Here, set the status back to STATUS_OK
                    previousEISStatus = currentEISStatus;

                    // 12/01/03: 
                    // Reset Timer state back to Normal
                    resetEISTimerState();

                    // Since the EIS/messageProvider is recovered, notify
                    // the EISTimer/RA that the EISTimer is back to normal state.
                    synchronized (syncObj) {
                        syncObj.notifyAll();
                    }
                } else {
                    // Since EIS is still OK, reset the timer counter and check the status again
                    // in another maxTimeLeft seconds.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "run", "EIS is still running OK. Check again in " + maxTimeLeft + " seconds.");
                }
                // Finished checking the EISStatus, reset the time left.
                resetTimeLeft();

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "EISTimer: run() - while loop");
            }
        } catch (InterruptedException e) {
            Tr.debug(tc, "InterruptedException is caught in EISTimer", e);
            throw new RuntimeException("InterruptedException is caught in EISTimer", e);
        }
    }

    /**
     * <p>Reset the timer counter to maxTimeLeft</p>
     *
     * <p>This method will be called by FVTWorkDispatcher.deliverComplexMessage and
     * deliverySimpleMessage since this means the MessageProvider is still sending
     * messages to the TRA.
     */

    public void resetTimeLeft() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "resetTimeLeft");
        timeLeft = maxTimeLeft;
    }

    // 09/26/03
    // Since there are messages being sent, that means a new test case is
    // being executed. So reset the rollback status.
    /**
     * Reset rollback status to true.
     */
    public void resetEISTimerState() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "resetEISTimerState", "Set EIS timer state to NORMAL.");
        EISTimerState = TIMER_STATE_NORMAL;
    }

    // Return rollback status to signalEISFailure()
    /**
     * Return rollback status
     *
     * @return boolean isRollbackFailed
     */
    //public int getEISTimerState()
    public boolean isRollbackFailed() {
        // 12/01/03: 
        // There is no need to expose the state of the EIS Timer to EIS.
        // EIS/messageProvider just need to know if the rollback success or not.
        //return EISTimerState;
        if (EISTimerState == TIMER_STATE_ROLLBACK_FAILURE) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "isRollbackFailed", "Rollback active transactions failed.");
            return true;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isRollbackFailed", "Rollback active transactions successful.");
        return false;
    }

    /**
     * This method allow EIS/messageProvider to provide the syncObj for notification
     *
     * @param syncObj
     */
    public void setSyncObject(Object syncObj) {
        this.syncObj = syncObj;
    }

    /**
     * <p>Set the raShutDown to true.</p>
     *
     * <p>This method will be called by FVTWorkDispatcher.raShutDown. The variable
     * raShutDown will be set to true and the while loop will be exited.
     *
     */
    public void RAShutdown() {
        raShutdown = true;
    }

}
