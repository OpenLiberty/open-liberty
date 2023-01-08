/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package example.quartz.concurrent;

import jakarta.enterprise.concurrent.ManagedExecutors;
import jakarta.enterprise.concurrent.ManagedTask;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.quartz.spi.ThreadExecutor;

/**
 * <p>Causes the Quartz Scheduler thread to run on a ManagedExecutorService.
 * This is the thread from which Quartz jobs are initialized and submitted to the thread pool.
 * Quartz jobs do not run on this thread. To get Quartz jobs to run on a ManagedExecutorService,
 * you also need the ManagedExecutorThreadPool class. Both ManagedExecutorThreadPool and this
 * class are required to get thread context (such as the application component name space)
 * propagated to Quartz jobs.<p>
 *
 * <p>Example configuration in quartz.properties:</>
 *
 * <pre>
 * org.quartz.threadExecutor.class=example.quartz.concurrent.ManagedExecutorThreadExecutor
 * org.quartz.threadExecutor.jndiName=concurrent/quartzExecutor
 * org.quartz.threadPool.class=example.quartz.concurrent.ManagedExecutorThreadPool
 * org.quartz.threadPool.jndiName=concurrent/quartzExecutor
 * org.quartz.threadPool.threadCount=3
 * </pre>
 */
public class ManagedExecutorThreadExecutor implements ThreadExecutor {
    private ExecutorService executor;
    private String jndiName = "java:comp/DefaultManagedExecutorService";

    public void execute(Thread thread) {
        Map<String, String> props = Collections.singletonMap(ManagedTask.LONGRUNNING_HINT, "true");
        Future<?> f = executor.submit(ManagedExecutors.managedTask(thread, props, null));
        System.out.println("Quartz Scheduler thread will run on ManagedExecutorService as " + f);
    }

    public void initialize() {
        try {
            executor = InitialContext.doLookup(jndiName);
        } catch (NamingException x) {
            throw new RuntimeException(x);
        }
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }
}