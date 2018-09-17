/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2014, 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.anno.util.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.annotation.Trivial;

public class UtilImpl_PoolExecutor extends ThreadPoolExecutor {

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
        return createExecutor( UtilImpl_PoolParameters.createDefaultParameters(), NON_BLOCKING_COUNT );
    }

    @Trivial
    public static UtilImpl_PoolExecutor createNonBlockingExecutor(int coreSize, int maxSize) {
        return createExecutor( UtilImpl_PoolParameters.createDefaultParameters(),
                               coreSize, maxSize, NON_BLOCKING_COUNT );
    }    

    //

    public static final int NON_BLOCKING_COUNT = 0;

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

    public static BlockingQueue<Runnable> createRunnableQueue() {
        return new LinkedBlockingQueue<Runnable>();
    }

    public static RejectedExecutionHandler createRejectionHandler() {
        return new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                // System.out.println("Executor [ " + executor + " ] failed to schedule [ " + r + " ]");
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
            this.completionSemaphore = new Semaphore(-(completionCount - 1));
        } else {
            this.completionSemaphore = null;
        }
    }

    //

    protected final Semaphore completionSemaphore;

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
