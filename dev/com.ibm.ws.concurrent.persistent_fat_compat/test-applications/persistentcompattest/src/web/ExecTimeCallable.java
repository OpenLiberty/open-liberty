/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.Date;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskIdAccessor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Callable that returns its scheduled execution time when it runs.
 */
public class ExecTimeCallable implements Callable<Date> {
    @Override
    public Date call() throws Exception {
        long taskId = TaskIdAccessor.get();
        System.out.println("OneShotCallable with task id " + taskId + " is running");

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup("java:module/env/myExecutorRef");
        TaskStatus<Date> status = executor.getStatus(taskId);
        return status.getNextExecutionTime();
    }
}
