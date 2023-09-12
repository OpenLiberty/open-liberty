/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat.crac.app.mxbean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.crac.management.CRaCMXBean;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/crac-test", loadOnStartup = 1)
public class TestCRaCMXBeanServlet extends FATServlet {
    final CRaCMXBean crac = CRaCMXBean.getCRaCMXBean();
    final AtomicLong afterRestoreRestoreTime = new AtomicLong();
    final AtomicLong afterRestoreUptimeSinceRestore = new AtomicLong();

    class TestResource implements Resource {
        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            // before checkpoint both should be -1
            long testRestoreTime = crac.getRestoreTime();
            long testUptimeSinceRestore = crac.getUptimeSinceRestore();
            assertEquals("Wrong restoreTime", -1, testRestoreTime);
            assertEquals("Wrong uptimeSinceRestore", -1, testUptimeSinceRestore);
            System.out.println("TESTING - beforeCheckpoint - testRestoreTime: " + testRestoreTime + " testUptimeSinceRestore: " + testUptimeSinceRestore);
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
            // after Restore both should be > 0 and uptime should be > restore time
            long testRestoreTime = crac.getRestoreTime();
            afterRestoreRestoreTime.set(testRestoreTime);
            long testUptimeSinceRestore = crac.getUptimeSinceRestore();
            afterRestoreUptimeSinceRestore.set(testUptimeSinceRestore);
            Thread.sleep(100);
            assertTrue("Wrong restoreTime: " + testRestoreTime, testRestoreTime > 0 && testRestoreTime < System.currentTimeMillis());
            assertTrue("Wrong uptimeSinceRestore: " + testUptimeSinceRestore, testUptimeSinceRestore <= (System.currentTimeMillis() - testRestoreTime));
            System.out.println("TESTING - afterRestore - testRestoreTime: " + testRestoreTime + " testUptimeSinceRestore: " + testUptimeSinceRestore);
        }
    }

    final TestResource testResource;

    public TestCRaCMXBeanServlet() {
        testResource = new TestResource();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println(getClass().getSimpleName() + ": Registering test resources.");
        Core.getGlobalContext().register(testResource);
    }

    @Test
    public void testGetRestoreTime(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        long testRestoreTime = crac.getRestoreTime();
        assertEquals("Wrong restoreTime", afterRestoreRestoreTime.get(), testRestoreTime);
    }

    @Test
    public void testGetUptimeSinceRestore(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        long testUptimeSinceRestore = crac.getUptimeSinceRestore();
        assertTrue("Wrong uptimeSinceRestore: " + testUptimeSinceRestore, testUptimeSinceRestore <= (System.currentTimeMillis() - crac.getRestoreTime()));
        assertTrue("Wrong uptimeSinceRestore: " + testUptimeSinceRestore, testUptimeSinceRestore > afterRestoreUptimeSinceRestore.get());
    }
}
