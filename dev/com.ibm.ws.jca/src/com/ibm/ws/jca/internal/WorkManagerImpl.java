/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import javax.resource.spi.work.WorkRejectedException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.threading.RunnableWithContext;

/**
 * Implementation of J2C WorkManager for WebSphere Application Server
 */
public final class WorkManagerImpl implements WorkManager {
    private static final TraceComponent tc = Tr.register(WorkManagerImpl.class);

    /**
     * Controls how often we purge the list of tracked futures.
     */
    private static final int FUTURE_PURGE_INTERVAL = 20;

    /**
     * The bootstrap context.
     */
    private final BootstrapContextImpl bootstrapContext;

    /**
     * Futures for work that might be scheduled or running. These are tracked so that we can
     * cancel these futures when the resource adapter stops.
     */
    private final ConcurrentLinkedQueue<Future<Void>> futures = new ConcurrentLinkedQueue<Future<Void>>();

    /**
     * Work that is running. This are tracked so that we can release the work when the resource adapter stops.
     */
    private final ConcurrentLinkedQueue<Work> runningWork = new ConcurrentLinkedQueue<Work>();

    /**
     * Indicates if this work manager is stopped.
     */
    private volatile boolean stopped;

    /**
     * Constructs the implementation of WorkManager.
     *
     * @param execSvc          Liberty executor.
     * @param bootstrapContext the bootstrap context.
     */
    public WorkManagerImpl(BootstrapContextImpl bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    /**
     * @param work
     * @throws WorkException
     * @see <a href="http://java.sun.com/j2ee/1.4/docs/api/javax/resource/spi/work/WorkManager.html#doWork(com.ibm.javarx.spi.work.Work)">
     *      com.ibm.javarx.spi.work.WorkManager.doWork(Work)</a>
     */
    @Override
    @Trivial
    public void doWork(Work work) throws WorkException {
        doWork(work, WorkManager.INDEFINITE, null, null);
    }

    /**
     * This method does not return until the work is completed as the caller
     * expects to wait until the work is completed before getting control back.
     * This method accomplishes this by NOT spinning a thread.
     *
     * @pre providerId != null
     *
     * @param work
     * @param startTimeout
     * @param execContext
     * @param workListener
     * @throws WorkException
     * @see <a
     *      href="http://java.sun.com/j2ee/1.4/docs/api/javax/resource/spi/work/WorkManager.html#doWork(com.ibm.javarx.spi.work.Work, long, com.ibm.javarx.spi.work.ExecutionContext, com.ibm.javarx.spi.work.WorkListener)">
     *      com.ibm.javarx.spi.work.WorkManager.doWork(Work, long, ExecutionContext, WorkListener)</a>
     */
    @Override
    public void doWork(
                       Work work,
                       long startTimeout,
                       ExecutionContext execContext,
                       WorkListener workListener) throws WorkException {

        try {
            beforeRunCheck(work, workListener, startTimeout);

            new WorkProxy(work, startTimeout, execContext, workListener, bootstrapContext, runningWork, false).call();
        } catch (WorkException ex) {
            throw ex;
        } catch (Throwable t) {
            WorkRejectedException wrex = new WorkRejectedException(t);
            wrex.setErrorCode(WorkException.INTERNAL);
            if (workListener != null)
                workListener.workRejected(new WorkEvent(work, WorkEvent.WORK_REJECTED, work, wrex));
            throw wrex;
        }
    }

    /**
     * @param work
     * @throws WorkException
     * @return long
     * @see <a href="http://java.sun.com/j2ee/1.4/docs/api/javax/resource/spi/work/WorkManager.html#startWork(com.ibm.javarx.spi.work.Work)">
     *      com.ibm.javarx.spi.work.WorkManager.startWork(Work)</a>
     */
    @Override
    @Trivial
    public long startWork(Work work) throws WorkException {
        return startWork(work, WorkManager.INDEFINITE, null, null);
    }

    /**
     * This method directly starts the work on a thread. The call to
     * ThreadPool.execute() does not return until the work actually
     * starts on the thread. This provides an easy way to determine
     * how long it took for the work to actually start and return
     * this value to the caller.
     *
     * @pre providerId != null
     *
     * @param work
     * @param startTimeout
     * @param execContext
     * @param workListener
     * @throws WorkException
     * @return long
     * @see <a
     *      href="http://java.sun.com/j2ee/1.4/docs/api/javax/resource/spi/work/WorkManager.html#startWork(com.ibm.javarx.spi.work.Work, long, com.ibm.javarx.spi.work.ExecutionContext, com.ibm.javarx.spi.work.WorkListener)">
     *      com.ibm.javarx.spi.work.WorkManager.startWork(Work, long, ExecutionContext, WorkListener)</a>
     */
    @Override
    public long startWork(
                          Work work,
                          long startTimeout,
                          ExecutionContext execContext,
                          WorkListener workListener) throws WorkException {

        try {
            beforeRunCheck(work, workListener, startTimeout);

            WorkProxy workProxy = new WorkProxy(work, startTimeout, execContext, workListener, bootstrapContext, runningWork, true);

            Future f = bootstrapContext.execSvc.submit((RunnableWithContext)workProxy);

            if (futures.add(f) && futures.size() % FUTURE_PURGE_INTERVAL == 0)
                purgeFutures();

            // It is this call that guarantees that startWork will not return until
            // the work is started or times out.
            Long startupDuration = workProxy.waitForStart();
            if (startupDuration == null) { // didn't start in time
                f.cancel(true);
                WorkRejectedException wrex = new WorkRejectedException(Utils.getMessage("J2CA8600.work.start.timeout", work, bootstrapContext.resourceAdapterID,
                                                                                        startTimeout), WorkException.START_TIMED_OUT);
                if (workListener != null)
                    workListener.workRejected(new WorkEvent(work, WorkEvent.WORK_REJECTED, work, wrex));
                throw wrex;
            }

            if (f.isDone())
                try {
                    f.get(); // If the work has already failed, cause the failure to be raised here
                } catch (ExecutionException x) {
                    throw x.getCause();
                }

            return startupDuration;

        } catch (WorkException ex) {
            throw ex;
        } catch (Throwable t) {
            WorkRejectedException wrex = new WorkRejectedException(t);
            wrex.setErrorCode(WorkException.INTERNAL);
            if (workListener != null)
                workListener.workRejected(new WorkEvent(work, WorkEvent.WORK_REJECTED, work, wrex));
            throw wrex;
        }
    }

    /**
     * @param work
     * @throws WorkException
     * @see <a href="http://java.sun.com/j2ee/1.4/docs/api/javax/resource/spi/work/WorkManager.html#scheduleWork(com.ibm.javarx.spi.work.Work)">
     *      com.ibm.javarx.spi.work.WorkManager.scheduleWork(Work)</a>
     */
    @Override
    @Trivial
    public void scheduleWork(Work work) throws WorkException {
        scheduleWork(work, WorkManager.INDEFINITE, null, null);
    }

    /**
     * This method puts the work on a queue that is later processed by the
     * "scheduler" thread. This allows the method to return to the caller
     * without having to wait for the thread to start.
     *
     * @pre providerId != null
     *
     * @param work
     * @param startTimeout
     * @param execContext
     * @param workListener
     * @throws WorkException
     * @exception NullPointerException this method relies on the
     *                                     RALifeCycleManager to call setThreadPoolName() to set
     *                                     theScheduler
     * @see <a
     *      href="http://java.sun.com/j2ee/1.4/docs/api/javax/resource/spi/work/WorkManager.html#scheduleWork(com.ibm.javarx.spi.work.Work, long, com.ibm.javarx.spi.work.ExecutionContext, com.ibm.javarx.spi.work.WorkListener)">
     *      com.ibm.javarx.spi.work.WorkManager.scheduleWork(Work, long, ExecutionContext, WorkListener)</a>
     */
    @Override
    public void scheduleWork(
                             Work work,
                             long startTimeout,
                             ExecutionContext execContext,
                             WorkListener workListener) throws WorkException {

        try {
            beforeRunCheck(work, workListener, startTimeout);

            WorkProxy workProxy = new WorkProxy(work, startTimeout, execContext, workListener, bootstrapContext, runningWork, true);

            FutureTask<Void> futureTask = new FutureTask<Void>(workProxy);
            bootstrapContext.execSvc.executeGlobal(futureTask);

            if (futures.add(futureTask) && futures.size() % FUTURE_PURGE_INTERVAL == 0)
                purgeFutures();
        } catch (WorkException ex) {
            throw ex;
        } catch (Throwable t) {
            WorkRejectedException wrex = new WorkRejectedException(t);
            wrex.setErrorCode(WorkException.INTERNAL);
            if (workListener != null)
                workListener.workRejected(new WorkEvent(work, WorkEvent.WORK_REJECTED, work, wrex));
            throw wrex;
        }
    }

    /**
     * Input parameter checks that can be done before calling run.
     *
     * @param work
     * @param workListener
     * @param startTimeout
     *
     * @throws WorkRejectedException
     */
    @Trivial
    private void beforeRunCheck(
                                Work work,
                                WorkListener workListener,
                                long startTimeout) throws WorkRejectedException {

        WorkRejectedException wrex = null;
        if (work == null) {
            wrex = new WorkRejectedException(new NullPointerException("work"));
            wrex.setErrorCode(WorkException.UNDEFINED);
        } else if (startTimeout < WorkManager.IMMEDIATE)
            wrex = new WorkRejectedException("startTimeout=" + startTimeout, WorkException.START_TIMED_OUT);
        else if (stopped)
            wrex = new WorkRejectedException(new UnavailableException(bootstrapContext.resourceAdapterID));

        if (wrex != null) {
            if (workListener != null) {
                WorkEvent event = new WorkEvent(work == null ? this : work, WorkEvent.WORK_REJECTED, work, wrex);
                workListener.workRejected(event);
            }
            throw wrex;
        }
    }

    /**
     * Purge completed futures from the list we are tracking.
     * This method should be invoked every so often so that we don't leak memory.
     */
    @Trivial
    private final void purgeFutures() {
        for (Iterator<Future<Void>> it = futures.iterator(); it.hasNext();)
            if (it.next().isDone())
                it.remove();
    }

    /**
     * Provides a way to stop the WorkManager.
     */
    public void stop() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Stop accepting work
        stopped = true;

        // Cancel futures for submitted work
        for (Future<Void> future = futures.poll(); future != null; future = futures.poll())
            if (!future.isDone() && future.cancel(true))
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "canceled", future);

        // Release running work
        for (Work work = runningWork.poll(); work != null; work = runningWork.poll()) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "release", work);
            work.release();
        }
    }
}