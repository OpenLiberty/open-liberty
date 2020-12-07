// IBM Confidential
//
// OCO Source Materials
//
// Copyright IBM Corp. 2013
//
// The source code for this program is not published or otherwise divested 
// of its trade secrets, irrespective of what has been deposited with the 
// U.S. Copyright Office.
//
// Change Log:
//  Date        pgmr       reason       Description
//  --------    -------    ------       ---------------------------------
//  07/10/03    jitang     LIDB2110.31  create - Provide J2C 1.5 resource adapter
//  11/20/03    swai                    Modified executeConcurrentWork such that it will return only all the 
//                                      concurrentWorks have reached the desired state.
//                                      The design is all concurrentWorks are submitted in a for loop. After all
//                                      works are submitted to the WM, then the syn block is notified only all
//                                      works reached the desired state (refer below), or if 
//                                      the timer expires.  
//  11/20/03    swai       d178406      Need to override setState method as 
//                                      notify should be called only if all concurrent
//                                      work has received the desire workEvent.  
//                                      Need to override isWorkHasReachedState such that
//                                      it returns the value of allWorksHaveReachedState.
//                                      This static variable is set to true only all works reached the desire
//                                      state.  
//  11/27/03    swai                    Override setSyncObj method because the syncObj is static in
//                                      FVTConcurrentWorkImpl. It has to be static as we notify the
//                                      sync block only when all works are submitted successfully.
//  12/18/03    swai                    Change Tr.error, Tr.info, Tr.warning call to Tr.debug
//  02/17/04    swai                    Due to Java 2 Security, creating threads is a privilieged action. Therefore
//                                      it is necessary to wrap the thread creation with doPrivileged call.
//  --------------------------------------------------------------------

package com.ibm.websphere.ejbcontainer.test.rar.work;

import java.util.logging.Logger;

import javax.resource.spi.work.WorkEvent;

/**
 * This work implementation supports concurrently delivery of work instance.
 */
public class FVTConcurrentWorkImpl extends FVTWorkImpl {
    private final static String CLASSNAME = FVTConcurrentWorkImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static int concurrentWorkNumber = 0;

    // 11/20/03: swai
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

        // 11/20/03: swai
        // need to set workEventNumber = concurrentWorkNumber
        workEventNumber = workNumber;
    }

    // 11/27/03: swai
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

    // 11/20/03: swai
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

            // 12/01/03: swai????
            // Need to check if there is exception associated with this work. If so, the other concurrent
            // work can't be executed because this work won't be able to notify the other concurrentWorks
            // which are hold up by the synchronization block (the while loop). So if I got one exception,
            // reduce the concurrentWorkNumber by 1
            if (this.getWorkException() != null)
                concurrentWorkNumber--;

            if (workEventNumber == 0)
            {
                // 11/20/03: swai
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