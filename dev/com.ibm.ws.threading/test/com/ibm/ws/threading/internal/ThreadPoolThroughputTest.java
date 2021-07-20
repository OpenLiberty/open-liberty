/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Ignore;

import com.ibm.ws.kernel.service.util.CpuInfo;

@Ignore
public class ThreadPoolThroughputTest {

    private static final int TERMINATION_WAIT_TIME = 30;

    static int poolSize = CpuInfo.getAvailableProcessors().get();

    Runnable doNothingRunnable = new Runnable() {
        @Override
        public void run() {
        }
    };

    static class NotifyOnCompleteRunnable implements Runnable {
        Lock lock = new ReentrantLock();
        Condition complete = lock.newCondition();
        AtomicLong atomicLong;
        Executor executor;

        NotifyOnCompleteRunnable(long iterations, Executor executor) {
            this.atomicLong = new AtomicLong(iterations);
            this.executor = executor;
        }

        @Override
        public void run() {
            if (atomicLong.decrementAndGet() == 0) {
                notifyComplete();
            }
        }

        public void waitForComplete(long millis) throws InterruptedException {
            lock.lockInterruptibly();
            try {
                while (atomicLong.get() > 0) {
                    complete.await(millis, TimeUnit.MILLISECONDS);
                    if (atomicLong.get() > 0) {
                        System.out.println("expired wait: " + executor);
                        dumpThreads();
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        public void notifyComplete() {
            try {
                lock.lock();
                complete.signalAll();
            } finally {
                lock.unlock();
            }
        }
    };

    public static void dumpThreads() {
        Map<Thread, StackTraceElement[]> tracebacks = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> mapEntry : tracebacks.entrySet()) {
            Thread t = mapEntry.getKey();
            System.out.println("---> Thread = " + t);
            for (StackTraceElement ste : mapEntry.getValue()) {
                System.out.println("      " + ste);
            }
        }
    }

    public long jdkExecutorForeignSourceThroughput(final long iterations) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(/* 500 */), new ThreadPoolExecutor.AbortPolicy());

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            executor.execute(doNothingRunnable);
        }
        executor.shutdown();
        executor.awaitTermination(ThreadPoolThroughputTest.TERMINATION_WAIT_TIME, TimeUnit.SECONDS);
        long elapsedTime = System.nanoTime() - startTime;

        assertTrue(iterations == executor.getCompletedTaskCount());
        return elapsedTime;
    }

    public long libertyExecutorForeignSourceThroughput(final long iterations) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            executor.execute(doNothingRunnable);
        }
        executor.shutdown();
        boolean terminationComplete = executor.awaitTermination(ThreadPoolThroughputTest.TERMINATION_WAIT_TIME, TimeUnit.SECONDS);
        long elapsedTime = System.nanoTime() - startTime;

        if (terminationComplete && iterations != executor.getCompletedTaskCount()) {
            System.out.println("executor = " + executor);
            System.out.println("Liberty foreign source execution, completed tasks = " + executor.getCompletedTaskCount());
        } else if (!terminationComplete) {
            System.out.println("executor = " + executor);
            System.out.println("Liberty foreign source execution, timed out without termination, completed tasks = " + executor.getCompletedTaskCount());
        }
        assertTrue(iterations == executor.getCompletedTaskCount());
        return elapsedTime;
    }

    public long jdkExecutorExecuteBatchWait(final long iterations, final long batchSize) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(500), new ThreadPoolExecutor.AbortPolicy());

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            NotifyOnCompleteRunnable work = new NotifyOnCompleteRunnable(batchSize, executor);
            for (int j = 0; j < batchSize; j++) {
                executor.execute(work);
            }
            work.waitForComplete(1000L);
        }
        executor.shutdown();
        executor.awaitTermination(ThreadPoolThroughputTest.TERMINATION_WAIT_TIME, TimeUnit.SECONDS);
        long elapsedTime = System.nanoTime() - startTime;

        assertTrue(iterations * batchSize == executor.getCompletedTaskCount());
        return elapsedTime;
    }

    public long libertyExecutorExecuteBatchWait(final long iterations, final long batchSize) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(500));

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            NotifyOnCompleteRunnable work = new NotifyOnCompleteRunnable(batchSize, executor);
            for (int j = 0; j < batchSize; j++) {
                executor.execute(work);
            }
            work.waitForComplete(1000L);
        }
        executor.shutdown();
        boolean terminationComplete = executor.awaitTermination(ThreadPoolThroughputTest.TERMINATION_WAIT_TIME, TimeUnit.SECONDS);
        long elapsedTime = System.nanoTime() - startTime;

        if (terminationComplete && (iterations * batchSize) != executor.getCompletedTaskCount()) {
            System.out.println("executor = " + executor);
            System.out.println("Liberty batch/wait, completed tasks = " + executor.getCompletedTaskCount());
        } else if (!terminationComplete) {
            System.out.println("executor = " + executor);
            System.out.println("Liberty batch/wait execution, timed out without termination, completed tasks = " + executor.getCompletedTaskCount());
        }
        assertTrue(iterations * batchSize == executor.getCompletedTaskCount());
        return elapsedTime;
    }

    static long average(long[] times) {
        long sum = 0L;
        for (long time : times) {
            sum += time;
        }
        return sum / times.length;
    }

    static void results(String testHeader, long[] times) {
        System.out.println(testHeader + " (" + poolSize + " threads)");
        System.out.println("   Times = " + Arrays.toString(times));
        System.out.println(" average = " + average(times) + " (" + average(times) / 1000000 + " ms)");

        Arrays.sort(times);
        System.out.println("     min = " + times[0]);
        System.out.println("     max = " + times[times.length - 1]);
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        final int availableProcessors = CpuInfo.getAvailableProcessors().get();
        boolean warmupPhase = true;
        for (poolSize = 5 * availableProcessors; poolSize > 0; poolSize--) {

            int warmups = warmupPhase ? 10 : 0;
            int loopCount = 10;
            int initialPoolSize = poolSize;
            if (warmupPhase) {
                poolSize = 4;
                System.out.println("Running warmup phase with " + poolSize + " threads");
            }

            final long iterations = 1000000;
            final long batchSize = 500;

            long[] times = new long[loopCount];
            ThreadPoolThroughputTest test = new ThreadPoolThroughputTest();

            for (int w = 0; w < warmups + 1; w++) {
                for (int i = 0; i < loopCount; i++) {
                    times[i] = test.jdkExecutorForeignSourceThroughput(iterations);
                }
            }
            results("JDK Foreign Source", times);

            for (int w = 0; w < warmups + 1; w++) {
                for (int i = 0; i < loopCount; i++) {
                    times[i] = test.libertyExecutorForeignSourceThroughput(iterations);
                }
            }
            results("Liberty Foreign Source", times);

            for (int w = 0; w < warmups + 1; w++) {
                for (int i = 0; i < loopCount; i++) {
                    times[i] = test.jdkExecutorExecuteBatchWait(iterations / batchSize, batchSize);
                }
            }
            results("JDK Foreign Source with Batch and Wait", times);

            for (int w = 0; w < warmups + 1; w++) {
                for (int i = 0; i < loopCount; i++) {
                    times[i] = test.libertyExecutorExecuteBatchWait(iterations / batchSize, batchSize);
                }
            }
            results("Liberty Foreign Source with Batch and Wait", times);

            if (warmupPhase) {
                poolSize = initialPoolSize + 1;
                warmupPhase = false;
                System.out.println("Warmup phase complete");
            }
        }
    }
}
