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

package com.ibm.adapter.work;

import javax.resource.ResourceException;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.Xid;

import com.ibm.adapter.AdapterUtil;
import com.ibm.adapter.message.FVTMessage;
import com.ibm.adapter.message.FVTMessageProvider;
import com.ibm.adapter.message.MessageWaitTimeoutException;
import com.ibm.adapter.tra.FVTWorkImpl;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FVTNestedWorkImpl extends FVTComplexWorkImpl {

    /** The FVTMessage object */
    // FVTMessage fvtMessage;

    /** the FVT work dispatcher */
    // FVTWorkDispatcher workDispatcher;

    // 11/26/03: 
    /** the work name of the child nested work */
    String workNameChild;

    /** the work name of the parent work */
    String workName;

    /** work type of child nested work */
    int doWorkType;
    int waitTime;
    int state;
    Xid xid = null;

    /** Object used for synchronization */
    // protected Object syncObj = new Object();

    /** indicate whether the message delivery method is timed out or not */
    protected boolean timedout = false;

    private static final TraceComponent tc = Tr
                    .register(FVTNestedWorkImpl.class);

    /**
     * Constructor for FVTNestedWorkImpl.
     *
     * @param workName
     *            the name of the work
     * @param message
     *            the FVTMessage object
     * @param workDispatcher
     *            the work dispatcher
     * @param nestedDoWorkType
     *            the work type of child nested work
     * @param waitTime
     *            This parameter specifies the maximum time for waiting the work
     *            to reach the specified state. Once the waitTime is reached,
     *            the method will return even if the work instance hasn't reach
     *            the specified state yet.
     * @param state
     *            the state which the work instance has reached when the method
     *            call returns.
     */
    public FVTNestedWorkImpl(String workName, FVTMessage message,
                             FVTWorkDispatcher workDispatcher, int nestedDoWorkType,
                             int waitTime, int state, Xid xid) {
        super(workName, message, workDispatcher);
        if (tc.isDebugEnabled())
            Tr.entry(tc, "<init>", new Object[] { workName, message,
                                                  workDispatcher, new Integer(nestedDoWorkType),
                                                  new Integer(waitTime), new Integer(state), xid });
        this.fvtMessage = message;
        this.workDispatcher = workDispatcher;
        this.workName = workName;
        // 11/26/03: 
        this.workNameChild = workName + "_child";
        this.doWorkType = nestedDoWorkType;
        this.waitTime = waitTime;
        this.state = state;
        this.xid = xid;
    }

    @Override
    public void run() {
        if (tc.isDebugEnabled())
            Tr.entry(tc, "run", workName);

        // 11/26/03: 
        FVTComplexWorkImpl newWork = new FVTComplexWorkImpl(workNameChild, fvtMessage, workDispatcher);

        // need to add nested work to the hash map
        // 11/26/03: 
        workDispatcher.addWork(workNameChild, newWork);

        ExecutionContext ec = new ExecutionContext();
        ec.setXid(xid);

        if (doWorkType == FVTMessageProvider.NO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "run", "no work");
            }
        } else if (doWorkType == FVTMessageProvider.DO_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "run", "do work");
            }

            try {
                workDispatcher.workManager.doWork(newWork,
                                                  WorkManager.INDEFINITE, ec, workDispatcher);
                // 11/26/03: 
                FVTWorkImpl w = workDispatcher.getWork(workNameChild);

                if (w.isWorkCompleted()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "run", workNameChild
                                            + " : isWorkCompleted -> true");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "run", workNameChild
                                            + " : isWorkCompleted -> false");
                    throw new WorkException("Nested Work Child - "
                                            + workNameChild
                                            + " : is not in completed state after doWork.");
                }

            } catch (WorkException we) {
                throw new WorkRuntimeException(" *WorkException from Nested Work Child - "
                                               + workNameChild + " : " + we.getMessage());
            }
        }

        else if (doWorkType == FVTMessageProvider.START_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "run", "start work");
            }

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            newWork.setNotificationState(state);
            newWork.setSyncObj(syncObj);

            try {
                workDispatcher.workManager.startWork(newWork,
                                                     WorkManager.INDEFINITE, ec, workDispatcher);
            } catch (WorkException we) {
                throw new WorkRuntimeException(" *WorkException from Nested Work Child - "
                                               + workNameChild + " : " + we.getMessage());
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (newWork.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", "work: " + workNameChild
                                                + " has reached "
                                                + AdapterUtil.getWorkStatusString(state)
                                                + " state");
                    } else {
                        // start the timer

                        // 11/20/03: 
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, newWork,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = newWork;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr
                                                                    .debug(tc, "run",
                                                                           "StartWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", workNameChild
                                                + " wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr
                                    .debug(
                                           tc,
                                           "InterruptedException is caught in FVTNestedWorkImpl.run",
                                           ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "FVTNestedWorkImpl.run", "Exception");

                throw new WorkRuntimeException("InterupttedException is caught in delivering the child work.", ie);

            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "run", workNameChild
                                        + " has not reached the desire state: " + state);
                throw new MessageWaitTimeoutException("Child work submission has been timed out. Desire state: "
                                                      + state + " has not reached.");
            } else {
                // d174256: 
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (newWork.getWorkException() != null) {
                    throw new WorkRuntimeException(newWork.getWorkException());
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "run", workNameChild
                                            + " has reached the desire state: " + state
                                            + ".");
                }
            }

            /*
             * try { FVTWorkImpl w = workDispatcher.getWork(workName);
             *
             * if (w.isWorkCompleted()) { if (tc.isDebugEnabled()) Tr.debug(tc,
             * "run", workName + " : isWorkCompleted -> true"); } else { if
             * (tc.isDebugEnabled()) Tr.debug(tc, "run", workName +
             * " : isWorkCompleted -> false"); throw new
             * WorkException("Nested Work Child - " + workName +
             * " : is not in WorkCompleted state after doWork."); } } catch
             * (WorkException we) { throw new
             * WorkRuntimeException(" *WorkException from Nested Work Child - "
             * + workName + " : " + we.getMessage()); }
             */

        }

        else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "run", "schedule work");
            }

            // 11/20/03: 
            // Define the local variable for syncObject
            Object syncObj = new Object();

            // 02/17/04: 
            // Need a final object for doPrivileged Call
            final Object pSyncObj = syncObj;

            newWork.setNotificationState(state);
            newWork.setSyncObj(syncObj);

            try {
                workDispatcher.workManager.scheduleWork(newWork,
                                                        WorkManager.INDEFINITE, ec, workDispatcher);
            } catch (WorkException we) {
                throw new WorkRuntimeException(" *WorkException from Nested Work Child - "
                                               + workNameChild + " : " + we.getMessage());
            }

            try {
                synchronized (syncObj) {
                    // 174742 - coming here, the work might have already been in
                    // the state.
                    // check the state before start the timer
                    if (newWork.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", "work: " + workNameChild
                                                + " has reached "
                                                + AdapterUtil.getWorkStatusString(state)
                                                + " state");
                    } else {
                        // start the timer

                        // 11/20/03: 
                        // 02/17/04: begin
                        // Need to use doPrivleged call to create a thread
                        // Thread t = new Thread(new Timer(waitTime, newWork,
                        // syncObj));
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = newWork;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr
                                                                    .debug(tc, "run",
                                                                           "ScheduleWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        }); // 02/17/04: ended
                        t.start();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", workNameChild
                                                + " wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr
                                    .debug(
                                           tc,
                                           "InterruptedException is caught in FVTNestedWorkImpl.run",
                                           ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "FVTNestedWorkImpl.run", "Exception");

                throw new WorkRuntimeException("InterupttedException is caught in delivering the child work.", ie);
            }

            if (timedout) { // d173989
                timedout = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "run", workNameChild
                                        + " has not reached the desire state: " + state);
                throw new MessageWaitTimeoutException("Child work submission has been timed out. Desire state: "
                                                      + state + " has not reached.");
            } else {
                // d174256: 
                // This else statemetns get the exception object from the Work
                // so that
                // sendMessagexxx can throw WorkCompletedException.

                if (newWork.getWorkException() != null) {
                    throw new WorkRuntimeException(newWork.getWorkException());
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "run", workNameChild
                                            + " has reached the desire state: " + state
                                            + ".");
                }
            }
            /*
             * try { FVTWorkImpl w = workDispatcher.getWork(workName);
             *
             * if (w.isWorkCompleted()) { if (tc.isDebugEnabled()) Tr.debug(tc,
             * "run", workName + " : isWorkCompleted -> true"); } else { if
             * (tc.isDebugEnabled()) Tr.debug(tc, "run", workName +
             * " : isWorkCompleted -> false"); throw new
             * WorkException("Nested Work Child - " + workName +
             * " : is not in WorkCompleted state after doWork."); } } catch
             * (WorkException we) { throw new
             * WorkRuntimeException(" *WorkException from Nested Work Child - "
             * + workName + " : " + we.getMessage()); }
             */

        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "run", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :"
                                           + doWorkType);
        }

        // 12/03/03: 
        // Add the xid to the ActiveTransSet only if the xid is valid (format id
        // != -1)
        if ((xid != null) && (xid.getFormatId() >= 0)) {
            // 12/03/03: 
            // Add the childXid to the SetActiveTrans only if the work is
            // executed
            // successfully. (according to Cathy).
            // Do not rethrow ResourceException since the test case may supply
            // the same Xid for parent and child work (exception flow)
            // Indicate in trace is enough. The reason is if
            // deliverNestedWorkMessage
            // may send message with same Xid for parent and child for
            // exceptional flow scenarios.
            // Then it's not appropriate to throw ResourceException
            try {
                if (!workDispatcher.addActiveTransToSet(xid)) {
                    if (tc.isEntryEnabled())
                        Tr.debug(tc, "deliverNestedWorkMessage",
                                 "Duplicate childXid with global trans id: "
                                                                 + xid.getGlobalTransactionId()[0]);
                }
            } catch (ResourceException re) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "deliverNestedWorkMessage",
                            "Unexpected ResourceException. Rethrow it.");
                throw new WorkRuntimeException(re);
            }
        }

        if (tc.isDebugEnabled())
            Tr.exit(tc, "run");
    }

    class Timer implements Runnable {
        private final Object syncObj;
        private final int waitTimeout;

        // 11/20/03: 
        // Let timer to find out if work has reached the desired state.
        private final FVTWorkImpl newWork;

        // 11/20/03: 
        public Timer(int waitTimeout, FVTWorkImpl newWork, Object obj) {
            syncObj = obj;
            this.waitTimeout = waitTimeout;
            this.newWork = newWork;
        }

        @Override
        public void run() {
            try {
                // Sleep for waitTimeout millisecond.
                Thread.sleep(waitTimeout);

                // 11/20/03: 
                // Has to set timedout to true if thread only if work hasn't
                // reached
                // the desire state
                if (newWork.isWorkHasReachedState()) {
                    timedout = true;

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "timer", "timeout");

                    // Notify the waiters
                    synchronized (syncObj) {
                        syncObj.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "InterruptedException is caught in Timer", e);
                throw new RuntimeException("InterruptedException is caught in Timer", e);
            }
        }
    }

}
