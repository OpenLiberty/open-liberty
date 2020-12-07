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

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;

/**
 * <p>This class implements the Work interface, which could be executed by the WorkManager upon
 * submission. This is a generic class which users can extend to customize their work instances
 * when writing test cases.</p>
 *
 * <p>Users are not encouraged to spawn a thread in the subclass implementation. However,
 * having a work implementation to spawn a thread is an interesting test case. </p>
 */
public abstract class FVTWorkImpl implements Work {
    private final static String CLASSNAME = FVTWorkImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** work name. Work name is used to identify the work instance. */
    protected String name;

    /** work state */
    protected int state;

    /** workHasReachedState */
    protected boolean workHasReachedState = false;

    protected boolean workAccepted = false;
    protected boolean workRejected = false;
    protected boolean workCompleted = false;
    protected boolean workStarted = false;

    /**
     * notification status. When the work reaches the notification status, a notification
     * should be sent via syncObj.notifyAll() method.
     */
    protected int notificationState;

    /** lock object */
    protected Object syncObj;

    /** constants for work state */
    public static final int INITIAL = 0;

    /** Work Exception object */
    protected WorkException workException = null;

    /**
     * Constructor
     *
     * @param workName the name of the work
     */
    public FVTWorkImpl(String workName) {
        svLogger.entering(CLASSNAME, "<init>", workName);

        // If passed-in workName is null, use the hash code of the work as the workName.
        if (workName == null || workName.equals("") || workName.trim().equals("")) {
            name = "" + this.hashCode();
        } else {
            name = workName;
        }

        // Set the state to INITIAL state.
        state = INITIAL;

        svLogger.exiting(CLASSNAME, "<init>", this);
    }

    /**
     * The WorkManager might call this method to hint the active Work instance to complete
     * execution as soon as possible.
     */
    @Override
    public void release() {
        // setState(WorkEvent.WORK_COMPLETED);

        // We can add some other things here later.
    }

    /**
     * <p>This method delivers message(s) to endpoint application. This is an abstract method.</p>
     *
     */
    @Override
    public abstract void run();

    /**
     * Returns the name.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the state.
     *
     * @return int
     */
    public int getState() {
        return state;
    }

    /**
     * Sets the state.
     *
     * @param state The state to set
     */
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
            // Now, set the workHasReachedState = true
            workHasReachedState = true;
            synchronized (syncObj) {
                syncObj.notifyAll();
            }
            svLogger.info("setState - work: " + this + " has reached desired state, " + state);
        }
    }

    /**
     * Sets the notificationState.
     *
     * @param notificationState The notificationState to set
     */
    public void setNotificationState(int notificationState) {
        this.notificationState = notificationState;
    }

    /**
     * Sets the syncObj.
     *
     * @param syncObj The syncObj to set
     */
    public void setSyncObj(Object syncObj) {
        this.syncObj = syncObj;
    }

    /**
     * Returns the workCompleted.
     *
     * @return boolean
     */
    public boolean isWorkCompleted() {
        return workCompleted;
    }

    /**
     * Returns the workRejected.
     *
     * @return boolean
     */
    public boolean isWorkRejected() {
        return workRejected;
    }

    /**
     * Sets the workAccepted.
     *
     * @param workAccepted The workAccepted to set
     */
    public void setWorkAccepted(boolean workAccepted) {
        this.workAccepted = workAccepted;
    }

    /**
     * Sets the workCompleted.
     *
     * @param workCompleted The workCompleted to set
     */
    public void setWorkCompleted(boolean workCompleted) {
        this.workCompleted = workCompleted;
    }

    /**
     * Sets the workRejected.
     *
     * @param workRejected The workRejected to set
     */
    public void setWorkRejected(boolean workRejected) {
        this.workRejected = workRejected;
    }

    /**
     * Sets the workStarted.
     *
     * @param workStarted The workStarted to set
     */
    public void setWorkStarted(boolean workStarted) {
        this.workStarted = workStarted;
    }

    /*
     * Get the exception object for WorkDispatcher so that sendMessagexxx can
     * throw WorkException.
     */
    /**
     * Sets the WorkException.
     *
     * @param workStarted The workStarted to set
     */
    public WorkException getWorkException() {
        return this.workException;
    }

    /*
     * Set the exception object to the Work so that sendMessagexxx can
     * throw WorkException.
     */
    /**
     * Sets the WorkException.
     *
     * @param workStarted The workStarted to set
     */
    public void setWorkException(WorkException we) {
        this.workException = we;
    }

    /**
     * Returns the workAccepted.
     *
     * @return boolean
     */
    public boolean isWorkAccepted() {
        return workAccepted;
    }

    /**
     * Returns the workStarted.
     *
     * @return boolean
     */
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
    public boolean hasWorkBeenInState(int state) {
        svLogger.entering(CLASSNAME, "hasWorkBeenInState", new Object[] { this, AdapterUtil.getWorkStatusString(state) });
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

        svLogger.exiting(CLASSNAME, "hasWorkBeenInState", new Boolean(ret));

        return ret;
    }

    /**
     * @return
     */
    public boolean isWorkHasReachedState() {
        return workHasReachedState;
    }
}