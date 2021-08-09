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
package test.concurrent.work.wm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.concurrent.ext.ManagedExecutorExtension;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

import test.concurrent.work.Work;
import test.concurrent.work.WorkCompletedException;
import test.concurrent.work.WorkItem;
import test.concurrent.work.WorkManager;
import test.concurrent.work.WorkRejectedException;

/**
 * An oversimplified work manager that redirects work to a managed executor.
 */
public class WorkManagerImpl extends ManagedExecutorExtension implements WorkManager {
    private final WSManagedExecutorService executor;
    private final String toString;

    WorkManagerImpl(WSManagedExecutorService executor, ResourceInfo resourceInfo) {
        super(executor, resourceInfo);
        this.executor = executor;

        // Generate toString output from which tests can check that the proper managed executor was used:
        // WorkManager@12345678 java:module/env/wm/executorRef of ManagedExecutor@9abcdef0 wm/executor
        // or
        // direct lookup @87654321 of ManagedExecutor@9abcdef0 wm/executor
        String type = resourceInfo == null ? "direct lookup " : resourceInfo.getType().substring(resourceInfo.getType().lastIndexOf('.'));
        StringBuilder s = new StringBuilder(type).append('@').append(Integer.toHexString(System.identityHashCode(this)));
        if (resourceInfo != null)
            s.append(' ').append(resourceInfo.getName());
        s.append(" of ").append(executor.toString());
        toString = s.toString();
    }

    @Override
    public WorkItem schedule(Work work) throws WorkRejectedException {
        try {
            Future<Work> future = ((ExecutorService) executor).submit(() -> {
                work.run();
                return work;
            });
            return new WorkItem(future);
        } catch (RejectedExecutionException x) {
            throw new WorkRejectedException(x);
        }
    }

    @Override
    public final String toString() {
        return toString;
    }
}
