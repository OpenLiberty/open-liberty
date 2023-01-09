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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz job that waits for a signal from the test case before completing.
 */
public class LengthyJob implements Job {
    final static Map<String, CountDownLatch> signals = new ConcurrentHashMap<String, CountDownLatch>();

    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Override
    public void execute(JobExecutionContext jobCtx) throws JobExecutionException {
        String jobName = jobCtx.getJobDetail().getKey().getName();
        System.out.println("LengthyJob > execute " + jobName + " on 0x" + Long.toHexString(Thread.currentThread().getId()) + " " + Thread.currentThread().getName());
        try {
            String submitter = jobName.substring(0, jobName.indexOf('-'));
            CountDownLatch signal = signals.get(submitter);
            jobCtx.setResult(signal.await(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            System.out.println("LengthyJob < execute: " + jobCtx.getResult());
        } catch (InterruptedException x) {
            System.out.println("LengthyJob < execute: " + x);
            jobCtx.setResult(x);
            throw new JobExecutionException(x);
        }
    }
}