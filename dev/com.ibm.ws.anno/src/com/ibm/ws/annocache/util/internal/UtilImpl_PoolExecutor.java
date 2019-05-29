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
package com.ibm.ws.annocache.util.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;

public class UtilImpl_PoolExecutor extends ThreadPoolExecutor {
    private static final String CLASS_NAME = UtilImpl_PoolExecutor.class.getSimpleName();
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.util");

    @Trivial
    public static UtilImpl_PoolExecutor createBlockingExecutor(int completionCount) {
        return createExecutor( UtilImpl_PoolParameters.createDefaultParameters(), completionCount );
    }

    @Trivial
    public static UtilImpl_PoolExecutor createBlockingExecutor(
        int coreSize, int maxSize, int completionCount) {

        return createExecutor( UtilImpl_PoolParameters.createDefaultParameters(),
                               coreSize, maxSize,
                               completionCount );
    }    

    @Trivial
    public static UtilImpl_PoolExecutor createNonBlockingExecutor() {
        return createExecutor( UtilImpl_PoolParameters.createDefaultParameters(),
                               NON_BLOCKING_COUNT );
    }

    @Trivial
    public static UtilImpl_PoolExecutor createNonBlockingExecutor(int coreSize, int maxSize) {
        return createExecutor( UtilImpl_PoolParameters.createDefaultParameters(),
                               coreSize, maxSize,
                               NON_BLOCKING_COUNT );
    }    

    //

    public static final int NON_BLOCKING_COUNT = 0;

    @Trivial
    public static UtilImpl_PoolExecutor createExecutor(UtilImpl_PoolParameters parameters,
                                                       int completionCount) {
        return new UtilImpl_PoolExecutor(
            parameters.coreSize,
            parameters.maxSize,
            parameters.keepAliveTime,
            parameters.keepAliveUnit,
            createRunnableQueue(),
            completionCount );
    }
    
    @Trivial
    public static UtilImpl_PoolExecutor createExecutor(UtilImpl_PoolParameters parameters,
                                                       int coreSize, int maxSize,
                                                       int completionCount) {
        return new UtilImpl_PoolExecutor(
            coreSize,
            maxSize,
            parameters.keepAliveTime,
            parameters.keepAliveUnit,
            createRunnableQueue(),
            completionCount );
    }

    @Trivial
    public static BlockingQueue<Runnable> createRunnableQueue() {
        return new LinkedBlockingQueue<Runnable>();
    }

    @Trivial
    public static RejectedExecutionHandler createRejectionHandler() {
        return new RejectedExecutionHandler() {
            private final String INNER_CLASS_NAME = CLASS_NAME + "$" + "RejectedExecutionHandler";
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                String methodName = "rejectedExecution";
                logger.logp(Level.WARNING, INNER_CLASS_NAME, methodName, 
                            "Executor [ {0} ] failed to schedule [ {1} ]",
                            new Object[] { executor, r });
            }
        };
    }

    //

    public UtilImpl_PoolExecutor(int corePoolSize,
                                 int maxPoolSize,
                                 long keepAliveTime,
                                 TimeUnit keepAliveUnit,
                                 BlockingQueue<Runnable> runnableQueue,
                                 int completionCount ) {
        super(corePoolSize, maxPoolSize, keepAliveTime, keepAliveUnit, runnableQueue);

        if ( completionCount > 0 ) {
            this.completionSemaphore = new Semaphore( -(completionCount - 1) );
        } else {
            this.completionSemaphore = null;
        }
    }

    //

    // Added to create a trace point: Trace injection is performed on
    // UtilImpl_PoolExecutor.  Trace injection is *not* performed on
    // the superclass, ThreadPoolExecutor.

    public void execute(Runnable r) {
        super.execute(r);
    }

    //

    protected final Semaphore completionSemaphore;

    @Trivial
    public Semaphore getCompletionSemaphore() {
        return completionSemaphore;
    }

    public void completeExecution() {
        Semaphore useCompletionSemaphore = getCompletionSemaphore();
        if ( useCompletionSemaphore == null ) {
            return;
        }

        useCompletionSemaphore.release();
    }

    public void waitForCompletion() throws InterruptedException {
        Semaphore useCompletionSemaphore = getCompletionSemaphore();
        if ( useCompletionSemaphore == null ) {
            return;
        }

        useCompletionSemaphore.acquire(); // throws InterruptedException
    }
}
