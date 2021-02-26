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

package com.ibm.adapter.tra;

import javax.resource.spi.work.WorkEvent;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * This work implementation supports concurrently delivery of work instance.
 */
public class FVTConcurrentWorkImpl extends FVTWorkImpl {

    private static int concurrentWorkNumber = 0;

    // 11/20/03: 
    // Need a workEventNumber to keep tracek how many workEvent has received.
    // Notify is called if it is 0.
    private static int workEventNumber = 0;
    private static boolean allWorksHaveReachedState = false;

    // lock object for concurrentWork
    private static Object syncObjConcurr = new Object();

    // lock object for waiting for desire workEvent
    private static Object syncObject = new Object();

    private static final TraceComponent tc = Tr.register(FVTConcurrentWorkImpl.class);

    /**
     * Constructor for FVTConcurrentWorkImpl.
     *
     * @param workName
     */
    public FVTConcurrentWorkImpl(String workName) {
        super(workName);
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "run");

        concurrentWorkNumber--;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "run", "concurrentWorkNumber is " + concurrentWorkNumber);

        synchronized (syncObjConcurr) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "run", "Enter the synchronized block");

            while (concurrentWorkNumber > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "InterruptedException is thrown", ie);
                    throw new RuntimeException(ie);
                }
            }
            syncObjConcurr.notifyAll();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "run", "Exit the synchronized block");

    }

    /**
     * Returns the concurrentWorkNumber.
     *
     * @return int
     */
    public static int getConcurrentWorkNumber() {
        return concurrentWorkNumber;
    }

    /**
     * Sets the concurrentWorkNumber.
     *
     * @param concurrentWorkNumber The concurrentWorkNumber to set
     */
    public static void setConcurrentWorkNumber(int workNumber) {
        concurrentWorkNumber = workNumber;

        // 11/20/03: 
        // need to set workEventNumber = concurrentWorkNumber
        workEventNumber = workNumber;
    }

    // 11/27/03: 
    // This overrides the setSyncObj method in FVTWorkImpl.
    /**
     * Sets the syncObj.
     *
     * @param syncObj The syncObj to set
     */
    @Override
    public void setSyncObj(Object syncObj) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "concurrentWork.setSyncObj", syncObj);
        // Need to reset allWorksHaveReachedState to false as this is a new run of
        // executeConcurrentWork.
        allWorksHaveReachedState = false;
        syncObject = syncObj;
    }

    // 11/20/03: 
    // This overrides the setState method in FVTWorkImpl. Notify is called
    // only if all work received the expected workEvent.
    /**
     * Sets the state.
     *
     * @param state The state to set
     */
    @Override
    public void setState(int state) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "concurrentWork.setState");

        synchronized (this) {
            this.state = state;

            switch (state) {
                case WorkEvent.WORK_ACCEPTED:
                    setWorkAccepted(true);
                    break;
                case WorkEvent.WORK_COMPLETED:
                    setWorkCompleted(true);
                    break;
                case WorkEvent.WORK_REJECTED:
                    setWorkRejected(true);
                    break;
                case WorkEvent.WORK_STARTED:
                    setWorkStarted(true);
                    break;

            }
        }

        if (state == notificationState) {
            workEventNumber--;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setState", "work #" + workEventNumber + " has reached desired state, " + state);

            // 12/01/03: 
            // Need to check if there is exception associated with this work. If so, the other concurrent
            // work can't be executed because this work won't be able to notify the other concurrentWorks
            // which are hold up by the synchronization block (the while loop). So if I got one exception,
            // reduce the concurrentWorkNumber by 1
            if (this.getWorkException() != null)
                concurrentWorkNumber--;

            if (workEventNumber == 0) {
                // 11/20/03: 
                // Now, set the workHasReachedState = true
                allWorksHaveReachedState = true;
                synchronized (syncObject) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "setState", "All work have reached desired state, " + state);
                    syncObject.notifyAll();
                }

            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "concurrentWork.setState");
    }

    /**
     * @return
     */
    @Override
    public boolean isWorkHasReachedState() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "concurrentWork.isWorkHasReachedState");
        return allWorksHaveReachedState;
    }

}
