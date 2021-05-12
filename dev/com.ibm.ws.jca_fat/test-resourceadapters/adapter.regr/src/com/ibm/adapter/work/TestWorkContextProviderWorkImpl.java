/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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

import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkContextProvider;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

import com.ibm.adapter.AdapterUtil;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>
 * This class implements the Work interface, which could be executed by the
 * WorkManager upon submission. This is a generic class which users can extend
 * to customize their work instances when writing test cases.
 * </p>
 *
 * <p>
 * Users are not encouraged to spawn a thread in the subclass implementation.
 * However, having a work implementation to spawn a thread is an interesting
 * test case.
 * </p>
 */
public class TestWorkContextProviderWorkImpl extends FVTGeneralWorkImpl implements WorkContextProvider {

    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr
                    .register(TestWorkContextProviderWorkImpl.class);

    /** work name. Work name is used to identiy the work instance. */
    protected String name;

    /** work state */
    protected int state;

    // 11/20/03: 
    /** workHasReachedState */
    protected boolean workHasReachedState = false;

    protected boolean workAccepted = false;
    protected boolean workRejected = false;
    protected boolean workCompleted = false;
    protected boolean workStarted = false;

    /**
     * notfication status. When the work reaches the notification status, a
     * notification should be sent via syncObj.notifyAll() method.
     */
    protected int notificationState;

    /** lock object */
    protected Object syncObj;

    /** constants for work state */
    public static final int INITIAL = 0;

    private final List<WorkContext> wcList;

    /** Work Exception object */
    protected WorkException workException = null;

    /**
     * Constructor
     *
     * @param workName
     *            the name of the work
     */
    public TestWorkContextProviderWorkImpl(String workName,
                                           List<WorkContext> workContexts) {
        super(workName);
        if (tc.isEntryEnabled())
            Tr.entry(tc, "<init>", workName);

        // If passed-in workName is null, use the hash code of the work as the
        // workName.
        if (workName == null || workName.equals("")
            || workName.trim().equals("")) {
            name = "" + this.hashCode();
        } else {
            name = workName;
        }

        wcList = workContexts;

        // Set the state to INITIAL state.
        state = INITIAL;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "<init>", this);
    }

    /**
     * The WorkManager might call this method to hint the active Work instance
     * to complete execution as soon as possible.
     */
    @Override
    public void release() {
        // setState(WorkEvent.WORK_COMPLETED);

        // We can add some other things here later.
    }

    @Override
    public void run() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "run", this);
    }

    @Override
    public List<WorkContext> getWorkContexts() {
        return wcList;
    }

    /**
     * Returns the name.
     *
     * @return String
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name
     *            The name to set
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the state.
     *
     * @return int
     */
    @Override
    public int getState() {
        return state;
    }

    /**
     * Sets the state.
     *
     * @param state
     *            The state to set
     */
    @Override
    public void setState(int state) {
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
            // 11/20/03: 
            // Now, set the workHasReachedState = true
            workHasReachedState = true;
            synchronized (syncObj) {
                syncObj.notifyAll();
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setState", "work: " + this
                                         + " has reached desired state, " + state);
        }
    }

    /**
     * Sets the notificationState.
     *
     * @param notificationState
     *            The notificationState to set
     */
    @Override
    public void setNotificationState(int notificationState) {
        this.notificationState = notificationState;
    }

    /**
     * Sets the syncObj.
     *
     * @param syncObj
     *            The syncObj to set
     */
    @Override
    public void setSyncObj(Object syncObj) {
        this.syncObj = syncObj;
    }

    /**
     * Returns the workCompleted.
     *
     * @return boolean
     */
    @Override
    public boolean isWorkCompleted() {
        return workCompleted;
    }

    /**
     * Returns the workRejected.
     *
     * @return boolean
     */
    @Override
    public boolean isWorkRejected() {
        return workRejected;
    }

    /**
     * Sets the workAccepted.
     *
     * @param workAccepted
     *            The workAccepted to set
     */
    @Override
    public void setWorkAccepted(boolean workAccepted) {
        this.workAccepted = workAccepted;
    }

    /**
     * Sets the workCompleted.
     *
     * @param workCompleted
     *            The workCompleted to set
     */
    @Override
    public void setWorkCompleted(boolean workCompleted) {
        this.workCompleted = workCompleted;
    }

    /**
     * Sets the workRejected.
     *
     * @param workRejected
     *            The workRejected to set
     */
    @Override
    public void setWorkRejected(boolean workRejected) {
        this.workRejected = workRejected;
    }

    /**
     * Sets the workStarted.
     *
     * @param workStarted
     *            The workStarted to set
     */
    @Override
    public void setWorkStarted(boolean workStarted) {
        this.workStarted = workStarted;
    }

    /*
     * d174256: Get the exception object for WorkDispatcher so
     * that sendMessagexxx can throw WorkException.
     */
    /**
     * Sets the WorkException.
     *
     * @param workStarted
     *            The workStarted to set
     */
    @Override
    public WorkException getWorkException() {
        return this.workException;
    }

    /*
     * d174256: Set the exception object to the Work so that
     * sendMessagexxx can throw WorkException.
     */
    /**
     * Sets the WorkException.
     *
     * @param workStarted
     *            The workStarted to set
     */
    @Override
    public void setWorkException(WorkException we) {
        this.workException = we;
    }

    /**
     * Returns the workAccepted.
     *
     * @return boolean
     */
    @Override
    public boolean isWorkAccepted() {
        return workAccepted;
    }

    /**
     * Returns the workStarted.
     *
     * @return boolean
     */
    @Override
    public boolean isWorkStarted() {
        return workStarted;
    }

    /**
     * Check whether the work has been in certain state or not.
     *
     * @param state
     *
     * @return true if the work has been in certain state; otherwise false
     */
    @Override
    public boolean hasWorkBeenInState(int state) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "hasWorkBeenInState", new Object[] { this,
                                                              AdapterUtil.getWorkStatusString(state) });

        boolean ret = false;

        switch (state) {
            case WorkEvent.WORK_ACCEPTED:
                ret = isWorkAccepted();
                break;

            case WorkEvent.WORK_COMPLETED:
                ret = isWorkCompleted();
                break;

            case WorkEvent.WORK_REJECTED:
                ret = isWorkRejected();
                break;

            case WorkEvent.WORK_STARTED:
                ret = isWorkStarted();
                break;

            default:
                ret = false;
                break;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "hasWorkBeenInState", new Boolean(ret));

        return ret;
    }

    /**
     * @return
     */
    @Override
    public boolean isWorkHasReachedState() {
        return workHasReachedState;
    }

}
