/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.fat.quartz.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Date;
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
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;

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
     * on the thread when backed by a ManagedExecutorService.
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
