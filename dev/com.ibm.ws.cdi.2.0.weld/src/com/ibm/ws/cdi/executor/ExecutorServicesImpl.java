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
package com.ibm.ws.cdi.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.weld.executor.AbstractExecutorServices;
import org.jboss.weld.manager.api.ExecutorServices;

/**
 * The original version of the is was in what is now the com.ibm.ws.cdi.weld bundle.
 * It has now been split out into this 2.0 version and a corresponding 1.2 version
 */
public class ExecutorServicesImpl extends AbstractExecutorServices implements ExecutorServices {

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    public ExecutorServicesImpl(ExecutorService executorService, ScheduledExecutorService scheduledExecutorService) {
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorService getTaskExecutor() {
        return executorService;
    }

    /** {@inheritDoc} */
    @Override
    public ScheduledExecutorService getTimerExecutor() {
        return scheduledExecutorService;
    }

    /** {@inheritDoc} */
    @Override
    protected int getThreadPoolSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void cleanup() {
        //no-op
    }
}
