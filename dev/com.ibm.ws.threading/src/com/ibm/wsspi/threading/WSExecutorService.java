/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * An extension of <tt>ExecutorService</tt> with additional methods that provide
 * more control over how submitted and executed work is handled.
 */
public interface WSExecutorService extends ExecutorService {

    /**
     * Executes the given command at some time in the future. Although the
     * command may execute in a new thread, in a pooled thread, or in the
     * calling thread, there is no bias towards executing the command in
     * the calling thread.
     * 
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     *             accepted for execution.
     * @throws NullPointerException if command is null
     */
    public void executeGlobal(Runnable command) throws RejectedExecutionException;
}
