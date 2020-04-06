/*******************************************************************************
 * Copyright (c) 2014,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.concurrent.persistent;

import com.ibm.ws.concurrent.persistent.internal.InvokerTask;

/**
 * Obtains the task ID of the task that is currently running on the thread, if any.
 */
public class TaskIdAccessor {
    /**
     * Returns the task ID of the task that is currently running on the thread. Otherwise null.
     *
     * @return the task ID of the task that is currently running on the thread. Otherwise null.
     */
    public static final Long get() {
        long[] taskEntry = InvokerTask.runningTaskState.get();
        return taskEntry == null ? null : taskEntry[0];
    }
}
