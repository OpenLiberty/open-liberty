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
package concurrent.fat.quartz.web;

import static org.junit.Assert.fail;

import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

/**
 * Quartz job listener with awaitable results.
 */
public class JobTracker implements JobListener {
    final String name;
    private final LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
    private final LinkedBlockingQueue<JobExecutionContext> started = new LinkedBlockingQueue<JobExecutionContext>();

    JobTracker(String name) {
        this.name = name;
    }

    /**
     * Awaits completion of the next job and returns the result if successful.
     *
     * @param timeout maximum amount of time to wait.
     * @param unit
     * @return result if the job completes within the interval. Otherwise null.
     * @throws CompletionException  if the job returns an exceptional result.
     * @throws InterruptedException if interrupted.
     */
    Object awaitResult(long timeout, TimeUnit unit) throws CompletionException, InterruptedException {
        Object r = results.poll(timeout, unit);
        if (r instanceof Throwable)
            throw new CompletionException((Throwable) r);
        else
            return r;
    }

    /**
     * Awaits the start of the next job and returns the snapshot of its JobExecutionContext.
     *
     * @param timeout maximum amount of time to wait.
     * @param unit
     * @return JobExecutionContext of the next job that is about to start. Otherwise null.
     * @throws InterruptedException if interrupted.
     */
    JobExecutionContext awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
        return started.poll(timeout, unit);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext jobCtx) {
        new Exception("Job execution was vetoed. See stack.").printStackTrace(System.out);
        fail("Job execution vetoed");
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext jobCtx) {
        started.add(jobCtx);
    }

    @Override
    public void jobWasExecuted(JobExecutionContext jobCtx, JobExecutionException jobX) {
        if (jobX == null)
            if (jobCtx.getResult() == null)
                results.add(new NullPointerException("Null result"));
            else
                results.add(jobCtx.getResult());
        else
            results.add(jobX);
    }
}