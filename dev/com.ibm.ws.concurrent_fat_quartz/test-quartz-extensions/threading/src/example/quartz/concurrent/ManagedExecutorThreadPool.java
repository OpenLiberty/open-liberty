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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

/**
 * Thread pool for Quartz that is backed by a ManagedExecutorService and a TransferQueue.
 * In order for this class to make the thread context of the application component that
 * creates the scheduler available to Quartz jobs, you must also configure Quartz to
 * use the ManagedExecutorThreadExecutor. For example,
 *
 * <pre>
 * org.quartz.threadExecutor.class=example.quartz.concurrent.ManagedExecutorThreadExecutor
 * org.quartz.threadExecutor.jndiName=concurrent/quartzExecutor
 * org.quartz.threadPool.class=example.quartz.concurrent.ManagedExecutorThreadPool
 * org.quartz.threadPool.jndiName=concurrent/quartzExecutor
 * org.quartz.threadPool.threadCount=3
 * </pre>
 */
public class ManagedExecutorThreadPool implements Runnable, ThreadPool {
    private ExecutorService executor;
    private final AtomicBoolean isShutDown = new AtomicBoolean();
    private String jndiName = "java:comp/DefaultManagedExecutorService";
    private final TransferQueue<Runnable> jobRunners = new LinkedTransferQueue<Runnable>();
    private int threadCount = 1;

    @Override
    public int blockForAvailableThreads() {
        System.out.println("ManagedExecutorThreadPool > blockForAvailableThreads");

        int available = 0;
        while (available == 0 && !isShutDown.get()) {
            if (jobRunners.hasWaitingConsumer())
                available = jobRunners.getWaitingConsumerCount();
            else
                try {
                    Thread.sleep(200);
                } catch (InterruptedException x) {
                    System.out.println("interrupted");
                    break;
                }
        }

        System.out.println("ManagedExecutorThreadPool < blockForAvailableThreads " + available);
        return available;
    }

    @Override
    public int getPoolSize() {
        return jobRunners.getWaitingConsumerCount();
    }

    public void initialize() throws SchedulerConfigException {
        System.out.println("ManagedExecutorThreadPool initialize " + threadCount + " threads on " + jndiName);
        try {
            executor = InitialContext.doLookup(jndiName);
            for (int i = 0; i < threadCount; i++)
                executor.submit(this);
        } catch (NamingException x) {
            throw (SchedulerConfigException) new SchedulerConfigException(x.getMessage()).initCause(x);
        }
    }

    /**
     * Waits for and executes the next job.
     */
    @Override
    public void run() {
        System.out.println("ManagedExecutorThreadPool > run wait for job...");
        try {
            Runnable jobRunner = jobRunners.poll(20, TimeUnit.SECONDS);

            if (jobRunner != null && !isShutDown.get()) {
                System.out.println("running job " + jobRunner);
                jobRunner.run();
            }
        } catch (InterruptedException x) {
            System.out.println("interrupted");
        } catch (RuntimeException | Error x) {
            x.printStackTrace(System.out);
        }
        if (!isShutDown.get()) {
            System.out.println("rescheduling");
            executor.submit(this);
        }
        System.out.println("ManagedExecutorThreadPool < run");
    }

    @Override
    public boolean runInThread(Runnable jobRunner) {
        System.out.println("ManagedExecutorThreadPool > runInThread " + jobRunner);

        boolean transferred;
        try {
            transferred = !isShutDown.get() && jobRunners.tryTransfer(jobRunner, 10, TimeUnit.SECONDS);
        } catch (InterruptedException x) {
            System.out.println("interrupted");
            transferred = false;
        }

        System.out.println("ManagedExecutorThreadPool < runInThread transferred to thread? " + transferred);
        return transferred;
    }

    @Override
    public void setInstanceId(String name) {
    }

    @Override
    public void setInstanceName(String name) {
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        isShutDown.set(true);
    }
}