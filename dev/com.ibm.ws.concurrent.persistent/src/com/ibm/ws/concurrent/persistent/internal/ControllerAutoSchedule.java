/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.controller.Controller;

/**
 * Automatically schedules a task via the controller upon successful transaction commit.
 */
class ControllerAutoSchedule implements Synchronization {
    private final short binaryFlags;
    private final Controller controller;
    private final long expectedExecTime;
    private final long partitionId;
    private final long taskId;
    private final int txTimeout;

    /**
     * Construct a new instance.
     */
    ControllerAutoSchedule(Controller controller, long partitionId, long taskId, long expectedExecTime, short binaryFlags, int txTimeout) {
        this.binaryFlags = binaryFlags;
        this.controller = controller;
        this.expectedExecTime = expectedExecTime;
        this.partitionId = partitionId;
        this.taskId = taskId;
        this.txTimeout = txTimeout;
    }

    /**
     * Upon successful transaction commit, automatically schedules a task via the controller.
     * 
     * @see javax.transaction.Synchronization#afterCompletion(int)
     */
    @Override
    public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED)
            controller.notifyOfTaskAssignment(partitionId, taskId, expectedExecTime, binaryFlags, txTimeout);
    }

    /**
     * @see javax.transaction.Synchronization#beforeCompletion()
     */
    @Override
    @Trivial
    public void beforeCompletion() {}
}
