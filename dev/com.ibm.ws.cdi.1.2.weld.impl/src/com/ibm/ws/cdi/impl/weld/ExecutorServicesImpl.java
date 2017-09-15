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
package com.ibm.ws.cdi.impl.weld;

import java.util.concurrent.ExecutorService;

import org.jboss.weld.executor.AbstractExecutorServices;
import org.jboss.weld.manager.api.ExecutorServices;

public class ExecutorServicesImpl extends AbstractExecutorServices implements ExecutorServices
{

    private final ExecutorService executorService;

    public ExecutorServicesImpl(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutorService getTaskExecutor() {
        return executorService;
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
