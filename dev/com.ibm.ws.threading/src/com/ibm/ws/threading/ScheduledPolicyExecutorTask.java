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
package com.ibm.ws.threading;

/**
 * When tasks implementing this interface are scheduled to the Liberty scheduled executor,
 * it delegates the actual running of the task to the designated policy executor at the
 * appropriate time. This can be useful if you have a PolicyExecutor to which you want
 * scheduled executor tasks to be subject to the constraints of. However, do not use in
 * combination with a policy executor that enables runIfQueueFull, because inline execution
 * of tasks would interfere with the scheduling thread.
 */
public interface ScheduledPolicyExecutorTask {
    /**
     * Returns the policy executor upon which to run this task.
     *
     * @return the policy executor upon which to run this task.
     */
    PolicyExecutor getExecutor();
}
