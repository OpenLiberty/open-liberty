/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.application.Application;
import com.ibm.wsspi.application.ApplicationState;

/**
 * Keeps track of which applications are started, so that we know when it is okay to run tasks,
 * and when task execution ought to be deferred.
 */
@Component(service = ApplicationTracker.class,
           configurationPolicy = ConfigurationPolicy.IGNORE)
public class ApplicationTracker {
    private static final TraceComponent tc = Tr.register(ApplicationTracker.class);

    /**
     * Service property indicating the application name.
     */
    private static final String NAME = "name";

    /**
     * Tracks the state of starting/started applications.
     */
    private final Map<String, ApplicationState> appStates = new HashMap<String, ApplicationState>();

    /**
     * Tasks that are deferred because the application needed to run them isn't available.
     * This field must only be accessed when holding the lock.
     */
    private final Map<String, Set<Runnable>> deferredTasks = new HashMap<String, Set<Runnable>>();

    /**
     * Common Liberty thread pool.
     */
    private ExecutorService executor;

    /**
     * Lock for accessing application/deferred task information.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Declarative Services method for setting a started Application instance
     *
     * @param ref reference to the service
     */
    @Reference(service = Application.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               target = "(application.state=STARTED)")
    protected void addStartedApplication(ServiceReference<Application> ref) {
        ExecutorService executor;
        String appName = (String) ref.getProperty(NAME);
        Set<Runnable> tasks;
        lock.writeLock().lock();
        try {
            executor = this.executor;
            appStates.put(appName, ApplicationState.STARTED);
            tasks = deferredTasks.remove(appName);
        } finally {
            lock.writeLock().unlock();
        }

        if (tasks != null)
            for (Runnable task : tasks)
                executor.submit(task);
    }

    /**
     * Declarative Services method for setting a starting Application instance
     *
     * @param ref reference to the service
     */
    @Reference(service = Application.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               target = "(application.state=STARTING)")
    protected void addStartingApplication(ServiceReference<Application> ref) {
        String appName = (String) ref.getProperty(NAME);
        lock.writeLock().lock();
        try {
            if (!appStates.containsKey(appName))
                appStates.put(appName, ApplicationState.STARTING);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Defer task execution until the application/module becomes available.
     * If, upon attempting to defer the task, we discover that the app has started,
     * we submit the task for execution instead of deferring.
     */
    void deferTask(Runnable task, String appName, PersistentExecutorImpl persistentExecutor) {
        ApplicationState state;
        ExecutorService executor;
        lock.writeLock().lock();
        try {
            executor = this.executor;
            state = appStates.get(appName);
            if (state != ApplicationState.STARTED) {
                Set<Runnable> taskSet = deferredTasks.get(appName);
                if (taskSet == null) {
                    deferredTasks.put(appName, taskSet = new HashSet<Runnable>());
                    // Avoid the warning if the application is in the process of starting
                    if (state != ApplicationState.STARTING && !persistentExecutor.deactivated)
                        Tr.warning(tc, "CWWKC1556.task.exec.deferred", appName);
                }
                taskSet.add(task);
            }
        } finally {
            lock.writeLock().unlock();
        }

        // No need to defer, the app has started
        if (state == ApplicationState.STARTED)
            executor.submit(task);
    }

    /**
     * Dump internal state to the introspector.
     *
     * @param out writer for the introspector.
     */
    void introspect(PrintWriter out) {
        if (lock.readLock().tryLock())
            try {
                for (Map.Entry<String, Set<Runnable>> entry : deferredTasks.entrySet()) {
                    out.print("Deferred tasks for ");
                    out.print(entry.getKey());
                    out.print(": ");
                    out.println(entry.getValue());
                }
                for (Map.Entry<String, ApplicationState> entry : appStates.entrySet()) {
                    out.print(entry.getKey());
                    out.print(" is ");
                    out.println(entry.getValue());
                }
            } finally {
                lock.readLock().unlock();
            }
    }

    /**
     * Returns true if the application with the specified name is started, otherwise false.
     *
     * @return true if the application with the specified name is started, otherwise false.
     */
    boolean isStarted(String appName) {
        lock.readLock().lock();
        try {
            return appStates.get(appName) == ApplicationState.STARTED;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Declarative Services method for unsetting a started Application instance
     *
     * @param ref reference to the service
     */
    protected void removeStartedApplication(ServiceReference<Application> ref) {
        String appName = (String) ref.getProperty(NAME);
        lock.writeLock().lock();
        try {
            appStates.remove(appName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for unsetting a starting Application instance
     *
     * @param ref reference to the service
     */
    protected void removeStartingApplication(ServiceReference<Application> ref) {
    }

    /**
     * Declarative Services method for setting the Liberty executor.
     *
     * @param svc the service
     */
    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected void setExecutor(ExecutorService svc) {
        executor = svc;
    }

    /**
     * Declarative Services method for unsetting the Liberty executor.
     *
     * @param svc the service
     */
    protected void unsetExecutor(ExecutorService svc) {
        executor = null;
    }
}
