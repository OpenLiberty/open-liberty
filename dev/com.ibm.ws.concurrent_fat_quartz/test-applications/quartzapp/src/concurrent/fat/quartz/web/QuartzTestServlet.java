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
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class QuartzTestServlet extends FATServlet {

    // Maximum number of nanoseconds to wait for a task to finish.
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

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
     * Verify that quartz.properties is being used by checking the Quartz scheduler name.
     */
    @Test
    public void testQuartzPropertiesUsed() throws Exception {
        assertEquals("QuartzInOpenLiberty", scheduler.getSchedulerName());
    }
}
