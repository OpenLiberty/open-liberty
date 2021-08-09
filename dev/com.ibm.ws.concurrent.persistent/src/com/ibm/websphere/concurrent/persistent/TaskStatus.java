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

import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

/**
 * Snapshot of status for a persistent task.
 * 
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
// TODO switch to proposed spec class
public interface TaskStatus<T> extends ScheduledFuture<T> {
    /**
     * Returns the expected next execution time, as of the point in time when
     * the snapshot of task status was captured. A value of <code>null</code> is returned
     * if no further executions are expected.
     * 
     * @return the expected next execution time.
     */
    Date getNextExecutionTime();

    /**
     * Returns the most recent task result as of the point in time when the snapshot of task status
     * was captured. Each invocation of this method causes a new copy of the result to be deserialized.
     * A value of <code>null</code> is returned in the following situations
     * <li>if the most recent execution of the task returned a <code>null</code> result
     * <li>if the task is a <code>Runnable</code> and does not specify a result
     * <li>if the task has not completed any executions and has not ended, for example, due to cancellation, abort, or failure.
     * 
     * @return the result.
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     */
    T getResult() throws ExecutionException;

    /**
     * Returns the unique identifier for the task.
     * 
     * @return the unique identifier for the task.
     */
    long getTaskId();

    /**
     * Returns the name assigned to the task, if any. If unnamed, then a String consisting of a single space character is returned.
     * 
     * @return the name assigned to the task.
     */
    String getTaskName();

    /**
     * Returns true if, as of the point in time when the snapshot of task status was captured,
     * the task has completed at least one execution or has ended, for example, due to cancellation, abort, or failure.
     * 
     * @return true if the task has a result. False if the task has yet to execute and has not ended.
     */
    boolean hasResult();
}
