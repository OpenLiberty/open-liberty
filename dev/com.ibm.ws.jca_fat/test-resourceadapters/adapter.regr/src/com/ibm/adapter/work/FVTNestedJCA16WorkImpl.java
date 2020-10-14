/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.work;

import java.util.List;

import javax.resource.ResourceException;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkContext;
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
public class FVTNestedJCA16WorkImpl extends FVTComplexJCA16WorkImpl {

    private static final long serialVersionUID = 3139971535244596783L;

    /** the work name of the child nested work */
    String workNameChild;

    /** the work name of the parent work */
    String workName;

    /** work type of child nested work */
    int doWorkType;
    int waitTime;
    int state;
    Xid xid = null;
    /** the spec level of the nested work **/
    int workSpecLevel;
    /** The work contexts of the child **/
    List<WorkContext> workCtxs;
    /** indicate whether the message delivery method is timed out or not */
    protected boolean timedout = false;

    private static final TraceComponent tc = Tr.register(FVTNestedJCA16WorkImpl.class);

    /**
     * Constructor for FVTNestedJCA16WorkImpl.
     *
     * @param workName
     *            the name of the work
     * @param message
     *            the FVTMessage object
     * @param workDispatcher
     *            the work dispatcher
     * @param workCtxs
     *            the list of work contexts supported by this resource adapter.
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
     * @param nestedWorkSpecLevel
     *            the spec level of the nested work instance. This can be 1.5 (0)or 1.6 (1)and
     *            is used to determine whether the nested work supports Generic Work Contexts
     *
     * @param xid The xid of the transaction branch.
     */
    public FVTNestedJCA16WorkImpl(String workName, FVTMessage message,
                                  FVTWorkDispatcher workDispatcher, List<WorkContext> workCtxs, List<WorkContext> childWorkCtxs,
                                  int nestedDoWorkType, int waitTime, int state,
                                  int nestedWorkSpecLevel, Xid xid) {
        super(workName, message, workDispatcher, workCtxs);
        if (tc.isDebugEnabled())
            Tr.entry(tc, "<init>", new Object[] { workName, message,
                                                  workDispatcher, new Integer(nestedDoWorkType),
                                                  new Integer(waitTime), new Integer(state),
                                                  new Integer(nestedWorkSpecLevel), xid });
        this.fvtMessage = message;
        this.workDispatcher = workDispatcher;
        this.workName = workName;
        this.workSpecLevel = nestedWorkSpecLevel;
        this.workNameChild = workName + "_child";
        this.doWorkType = nestedDoWorkType;
        this.waitTime = waitTime;
        this.state = state;
        this.xid = xid;
        this.workCtxs = childWorkCtxs;
    }

    @Override
    public void run() {
        if (tc.isDebugEnabled())
            Tr.entry(tc, "run", workName);
        super.run();

        FVTComplexWorkImpl newWork = null;
        ExecutionContext ec = null;
        if (workSpecLevel == 0) {
            newWork = new FVTComplexWorkImpl(workNameChild, fvtMessage, workDispatcher);
            ec = new ExecutionContext();
            ec.setXid(xid);
        } else if (workSpecLevel == 1) {
            newWork = new FVTComplexJCA16WorkImpl(workNameChild, fvtMessage, workDispatcher, workCtxs);
        }
        workDispatcher.addWork(workNameChild, newWork);

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
            Object syncObj = new Object();
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
                    if (newWork.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", "work: " + workNameChild
                                                + " has reached "
                                                + AdapterUtil.getWorkStatusString(state)
                                                + " state");
                    } else {
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
                                        });
                        t.start();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", workNameChild
                                                + " wait for notifications");
                        syncObj.wait();
                    }
                }
            } catch (InterruptedException ie) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc,
                             "InterruptedException is caught in FVTNestedJCA16WorkImpl.run",
                             ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "FVTNestedJCA16WorkImpl.run", "Exception");

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
        }

        else if (doWorkType == FVTMessageProvider.SCHEDULE_WORK) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "run", "schedule work");
            }

            Object syncObj = new Object();
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
                    if (newWork.hasWorkBeenInState(state)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "run", "work: " + workNameChild
                                                + " has reached "
                                                + AdapterUtil.getWorkStatusString(state)
                                                + " state");
                    } else {
                        final int pWaitTime = waitTime;
                        final FVTComplexWorkImpl pWork = newWork;
                        Thread t = (Thread) java.security.AccessController
                                        .doPrivileged(new java.security.PrivilegedAction() {
                                            @Override
                                            public Object run() {
                                                if (tc.isEntryEnabled())
                                                    Tr.debug(tc, "run", "ScheduleWork: doPrivileged on create Thread t");
                                                return new Thread(new Timer(pWaitTime, pWork, pSyncObj));
                                            }
                                        });
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
                                           "InterruptedException is caught in FVTNestedJCA16WorkImpl.run",
                                           ie);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "FVTNestedJCA16WorkImpl.run", "Exception");

                throw new WorkRuntimeException("InterupttedException is caught in delivering the child work.", ie);
            }

            if (timedout) {
                timedout = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "run", workNameChild
                                        + " has not reached the desire state: " + state);
                throw new MessageWaitTimeoutException("Child work submission has been timed out. Desire state: "
                                                      + state + " has not reached.");
            } else {
                if (newWork.getWorkException() != null) {
                    throw new WorkRuntimeException(newWork.getWorkException());
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "run", workNameChild
                                            + " has reached the desire state: " + state
                                            + ".");
                }
            }

        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "run", "Exception");
            throw new WorkRuntimeException("Unsupported do_work type :"
                                           + doWorkType);
        }

        // Add the xid to the ActiveTransSet only if the xid is valid (format id
        // != -1)
        if ((xid != null) && (xid.getFormatId() >= 0)) {
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

    public List<WorkContext> getChildWorkContexts() {
        return workCtxs;
    }

    class Timer implements Runnable {
        private Object syncObj;
        private int waitTimeout;

        // Let timer to find out if work has reached the desired state.
        private FVTWorkImpl newWork;

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
