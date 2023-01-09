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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.quartz.impl.matchers.NameMatcher;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class QuartzTestServlet extends FATServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    /**
     * Defines a resource reference that Quartz jobs can try to look up to verify that
     * application component context is available.
     */
    @Resource(name = "java:module/env/concurrent/quartzExecutorRef", lookup = "concurrent/quartzExecutor")
    ExecutorService executorRef;

    /**
     * Quartz scheduler
     */
    private Scheduler scheduler;

    @Override
    public void destroy() {
        if (scheduler != null)
            try {
                scheduler.shutdown();
                scheduler = null;
            } catch (SchedulerException x) {
                x.printStackTrace(System.out);
                fail(x.getMessage());
            }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Verify that Quartz jobs initialize with application component context
     * on the thread when the ThreadExecutor is backed by a ManagedExecutorService.
     */
    @Test
    public void testQuartzJobInitializesWithAppComponentContext() throws Exception {
        JobDetail job = JobBuilder.newJob(LookupOnInitJob.class) //
                        .withIdentity("testQuartzJobInitializesWithAppComponentContext-job") //
                        .build();

        Trigger onceAfter300ms = TriggerBuilder.newTrigger() //
                        .startAt(new Date(System.currentTimeMillis() + 300)) //
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0)) //
                        .withIdentity("testQuartzJobInitializesWithAppComponentContext-300ms-trigger") //
                        .build();

        JobTracker listener = new JobTracker("testQuartzJobInitializesWithAppComponentContext-listener");
        scheduler.getListenerManager().addJobListener(listener, KeyMatcher.keyEquals(job.getKey()));
        try {
            scheduler.scheduleJob(job, onceAfter300ms);

            Object[] results = (Object[]) listener.awaitResult(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertNotNull(results);
            assertNotNull(results[0]);
            assertNotNull(results[1]);
        } finally {
            scheduler.getListenerManager().removeJobListener(listener.name);
        }
    }

    /**
     * Verify that Quartz jobs runs with application component context
     * on the thread when the ThreadPool is backed by a ManagedExecutorService.
     */
    @Test
    public void testQuartzJobRunsWithAppComponentContext() throws Exception {
        JobDetail job = JobBuilder.newJob(LookupOnRunJob.class) //
                        .withIdentity("testQuartzJobRunsWithAppComponentContext-job") //
                        .build();

        Trigger onceAfter400ms = TriggerBuilder.newTrigger() //
                        .startAt(new Date(System.currentTimeMillis() + 400)) //
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0)) //
                        .withIdentity("testQuartzJobRunsWithAppComponentContext-400ms-trigger") //
                        .build();

        JobTracker listener = new JobTracker("testQuartzJobRunsWithAppComponentContext-listener");
        scheduler.getListenerManager().addJobListener(listener, KeyMatcher.keyEquals(job.getKey()));
        try {
            scheduler.scheduleJob(job, onceAfter400ms);

            Object[] results = (Object[]) listener.awaitResult(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertNotNull(results);
            assertNotNull(results[0]);
            assertNotNull(results[1]);
        } finally {
            scheduler.getListenerManager().removeJobListener(listener.name);
        }
    }

    /**
     * Verify that the managed executor's concurrencyPolicy max constrains the number of Quartz jobs
     * that can run at the same time.
     */
    @Test
    public void testQuartzJobsConstrainedByConcurrencyPolicy() throws Exception {
        CountDownLatch allowJobsToComplete = new CountDownLatch(1);
        LengthyJob.signals.put("testQuartzJobsConstrainedByConcurrencyPolicy", allowJobsToComplete);

        JobDetail job1 = JobBuilder.newJob(LengthyJob.class) //
                        .withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-job-1") //
                        .build();

        JobDetail job2 = JobBuilder.newJob(LengthyJob.class) //
                        .withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-job-2") //
                        .build();

        JobDetail job3 = JobBuilder.newJob(LengthyJob.class) //
                        .withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-job-3") //
                        .build();

        JobDetail job4 = JobBuilder.newJob(LengthyJob.class) //
                        .withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-job-4") //
                        .build();

        JobDetail job5 = JobBuilder.newJob(LengthyJob.class) //
                        .withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-job-5") //
                        .build();

        TriggerBuilder<SimpleTrigger> onceAfter500ms = TriggerBuilder.newTrigger() //
                        .startAt(new Date(System.currentTimeMillis() + 500)) //
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0));

        JobTracker listener = new JobTracker("testQuartzJobsConstrainedByConcurrencyPolicy-listener");
        scheduler.getListenerManager().addJobListener(listener, NameMatcher.jobNameStartsWith("testQuartzJobsConstrainedByConcurrencyPolicy-job-"));
        try {
            scheduler.scheduleJob(job1, onceAfter500ms.withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-500ms-trigger-1").build());
            scheduler.scheduleJob(job2, onceAfter500ms.withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-500ms-trigger-2").build());
            scheduler.scheduleJob(job3, onceAfter500ms.withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-500ms-trigger-3").build());
            scheduler.scheduleJob(job4, onceAfter500ms.withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-500ms-trigger-4").build());
            scheduler.scheduleJob(job5, onceAfter500ms.withIdentity("testQuartzJobsConstrainedByConcurrencyPolicy-500ms-trigger-5").build());

            assertNotNull(listener.awaitStart(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertNotNull(listener.awaitStart(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertNotNull(listener.awaitStart(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // per concurrencyPolicy max, only 3 can run at once
            assertNull(listener.awaitStart(1, TimeUnit.SECONDS));

            // unblock jobs
            allowJobsToComplete.countDown();

            // fourth and fifth jobs start now
            assertNotNull(listener.awaitStart(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertNotNull(listener.awaitStart(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            // all jobs must complete successfully
            assertNotNull(listener.awaitResult(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertNotNull(listener.awaitResult(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertNotNull(listener.awaitResult(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertNotNull(listener.awaitResult(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertNotNull(listener.awaitResult(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        } finally {
            scheduler.getListenerManager().removeJobListener(listener.name);
        }
    }

    /**
     * Verify that quartz.properties is being used by checking the Quartz scheduler name.
     */
    @Test
    public void testQuartzPropertiesUsed() throws Exception {
        assertEquals("QuartzInOpenLiberty", scheduler.getSchedulerName());
    }
}
