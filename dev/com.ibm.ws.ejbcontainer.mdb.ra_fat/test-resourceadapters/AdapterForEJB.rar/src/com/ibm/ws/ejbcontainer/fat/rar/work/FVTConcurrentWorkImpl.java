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

package com.ibm.ws.ejbcontainer.fat.rar.work;

import java.util.logging.Logger;

import javax.resource.spi.work.WorkEvent;

/**
 * This work implementation supports concurrently delivery of work instance.
 */
public class FVTConcurrentWorkImpl extends FVTWorkImpl {
    private final static String CLASSNAME = FVTConcurrentWorkImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static int concurrentWorkNumber = 0;

    // Need a workEventNumber to keep tracek how many workEvent has received.
    // Notify is called if it is 0.
    private static int workEventNumber = 0;
    private static boolean allWorksHaveReachedState = false;

    // lock object for concurrentWork
    private static Object syncObjConcurr = new Object();

    // lock object for waiting for desire workEvent
    private static Object syncObject = new Object();

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
        svLogger.entering(CLASSNAME, "run");

        concurrentWorkNumber--;
        svLogger.info("run: concurrentWorkNumber is " + concurrentWorkNumber);

        synchronized (syncObjConcurr) {
            svLogger.info("run: Enter the synchronized block");

            while (concurrentWorkNumber > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    svLogger.info("InterruptedException is thrown: " + ie);
                    throw new RuntimeException(ie);
                }
            }
            syncObjConcurr.notifyAll();
        }

        svLogger.info("run: Exit the synchronized block");
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

        // need to set workEventNumber = concurrentWorkNumber
        workEventNumber = workNumber;
    }

    // This overrides the setSyncObj method in FVTWorkImpl.
    /**
     * Sets the syncObj.
     *
     * @param syncObj The syncObj to set
     */
    @Override
    public void setSyncObj(Object syncObj) {
        svLogger.entering(CLASSNAME, "concurrentWork.setSyncObj", syncObj);
        // Need to reset allWorksHaveReachedState to false as this is a new run of
        // executeConcurrentWork.
        allWorksHaveReachedState = false;
        syncObject = syncObj;
    }

    // This overrides the setState method in FVTWorkImpl. Notify is called
    // only if all work received the expected workEvent.
    /**
     * Sets the state.
     *
     * @param state The state to set
     */
    @Override
    public void setState(int state) {
        svLogger.entering(CLASSNAME, "concurrentWork.setState");

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
            svLogger.info("setState: work #" + workEventNumber + " has reached desired state, " + state);

            // Need to check if there is exception associated with this work. If so, the other concurrent
            // work can't be executed because this work won't be able to notify the other concurrentWorks
            // which are hold up by the synchronization block (the while loop). So if I got one exception,
            // reduce the concurrentWorkNumber by 1
            if (this.getWorkException() != null)
                concurrentWorkNumber--;

            if (workEventNumber == 0) {
                // Now, set the workHasReachedState = true
                allWorksHaveReachedState = true;
                synchronized (syncObject) {
                    svLogger.info("setState: All work have reached desired state, " + state);
                    syncObject.notifyAll();
                }

            }
        }
        svLogger.exiting(CLASSNAME, "concurrentWork.setState");
    }

    /**
     * @return
     */
    @Override
    public boolean isWorkHasReachedState() {
        svLogger.entering(CLASSNAME, "concurrentWork.isWorkHasReachedState");
        return allWorksHaveReachedState;
    }
}