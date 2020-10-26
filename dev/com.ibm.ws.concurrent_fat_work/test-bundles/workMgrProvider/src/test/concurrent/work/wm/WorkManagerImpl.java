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

/**
 * An oversimplified work manager that redirects work to a managed executor.
 */
public class WorkManagerImpl extends ManagedExecutorExtension implements WorkManager {
    private final WSManagedExecutorService executor;

    WorkManagerImpl(WSManagedExecutorService executor, ResourceInfo resourceInfo) {
        super(executor, resourceInfo);
        this.executor = executor;
    }

    public WorkItem schedule(Work work) {
        Future<Work> future = ((ExecutorService) executor).submit(() -> {
            work.run();
            return work;
        });
        return new WorkItem(future);
    }
}
