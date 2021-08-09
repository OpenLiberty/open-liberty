/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.concurrent.CountDownLatch;

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.ws.threading.ScheduledPolicyExecutorTask;

/**
 * A subclass of CountDownTask that directs its execution to the specified PolicyExecutor
 * when submitted to the Liberty scheduled executor.
 */
public class ScheduledCallableTask extends CountDownTask implements ScheduledPolicyExecutorTask {
    private final PolicyExecutor executor;

    public ScheduledCallableTask(PolicyExecutor executor, CountDownLatch beginLatch, CountDownLatch continueLatch) {
        super(beginLatch, continueLatch, PolicyExecutorServlet.TIMEOUT_NS * 2);
        this.executor = executor;
    }

    @Override
    public PolicyExecutor getExecutor() {
        return executor;
    }

    @Override
    public Exception resubmitFailed(Exception failure) {
        return failure;
    }
}
