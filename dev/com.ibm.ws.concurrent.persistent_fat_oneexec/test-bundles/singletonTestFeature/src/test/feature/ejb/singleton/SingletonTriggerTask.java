/*******************************************************************************
 * Copyright (c) 2014, 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.feature.ejb.singleton;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedTaskListener;

import com.ibm.ws.concurrent.persistent.ejb.TaskLocker;

/**
 * This task helps simulate a deadlock scenario with EJB singletons, which is
 * solved by introducing a pre/postInvoke to ensure the EJB singleton lock is always obtained
 * before the persistent executor locks the task entry in the database.
 */
public class SingletonTriggerTask implements Callable<Void>, TaskLocker {
    private static final long serialVersionUID = -2416685776008060348L;

    // captures results regardless of whether persistent executor eventually rolls back the transaction to retry
    public static final LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();

    // simulates a lock for a singleton (which would really be under a transaction, not a Java lock like this)
    public static final Lock singletonLock = new ReentrantReadWriteLock().writeLock();

    private transient ClassLoader classloader;

    public SingletonTriggerTask() {
        classloader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Void call() throws Exception {
        if (singletonLock.tryLock(30, TimeUnit.SECONDS)) {
            singletonLock.unlock();
            results.add("SUCCESS");
        } else {
            Exception failure = new Exception("Task was deadlocked for 30 seconds.");
            results.add(failure);
            throw failure;
        }

        return null;
    }

    @Override
    public String getAppName() {
        return "persistoneexectest";
    }

    @Override
    public ClassLoader getClassLoader() {
        return classloader;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return null;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        // The task runs immediately, and only once.
        return lastExecution == null ? taskScheduledTime : null;
    }

    @Override
    public void lock() {
        singletonLock.lock();
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }

    @Override
    public void unlock() {
        singletonLock.unlock();
    }
}
