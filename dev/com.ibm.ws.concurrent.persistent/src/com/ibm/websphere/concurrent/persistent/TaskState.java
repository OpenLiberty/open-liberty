/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.concurrent.persistent;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Enumeration of task states. A task can be in a combination of states.
 * For example, both SCHEDULED and UNATTEMPTED. Or both CANCELED and ENDED.
 */
@Trivial
public enum TaskState {
    // Bit values are specifically choose to allowing efficient less-than/greater-than comparisons in the database.
    // For example,
    // state < TaskState.ENDED indicates the task has not yet ended. It is either SCHEDULED or SUSPENDED.
    // state < TaskState.SUSPENDED indicates the task is SCHEDULED
    // state >= TaskState.ENDED indicates a task is ENDED
    // state >= TaskState.SUCCESSFUL AND state < TaskState.FAILURE_LIMIT_REACHED indicates a task has successfully completed (or skipped) all executions.
    // state >= TaskState.FAILURE_LIMIT_REACHED AND state < TaskState.CANCELED indicates a task has failed
    // state >= TaskState.CANCELED indicates a task is CANCELED

    /**
     * Task is scheduled to execute. SCHEDULED, ENDED, and SUSPENDED states are all mutually exclusive.
     */
    SCHEDULED((short) 0x1),

    /**
     * No executions have completed. This means TaskStatus.get cannot return a result.
     */
    UNATTEMPTED((short) 0x2),

    // 0x4 reserved for additional scheduled state

    // 0x8 reserved for additional scheduled state

    /**
     * Task execution was skipped.
     */
    SKIPPED((short) 0x10),

    /**
     * Trigger.skipRun raised an error. The SKIPPED state also applies when in this state.
     */
    SKIPRUN_FAILED((short) 0x20),

    // 0x40 reserved for additional scheduled+skipped state

    /**
     * Task is suspended.
     */
    SUSPENDED((short) 0x80),

    // 0x100 reserved for additional suspended state

    /**
     * No further task executions will occur.
     */
    ENDED((short) 0x200),

    /**
     * The task has successfully completed (or skipped) all executions.
     */
    SUCCESSFUL((short) 0x400),

    /**
     * Task exceeded the consecutive failure limit. The ENDED state also applies when in this state.
     */
    FAILURE_LIMIT_REACHED((short) 0x800),

    // 0x1000 reserved for additional ended+failed state

    /**
     * Task is canceled. The ENDED state also applies when in this state.
     */
    CANCELED((short) 0x2000),

    // 0x4000 reserved for additional ended+canceled state

    // 0x8000 do not use

    /**
     * Match any task state.
     */
    ANY((short) -1);

    /**
     * The bit value for the state.
     */
    public final short bit;

    private TaskState(short bit) {
        this.bit = bit;
    }
}
