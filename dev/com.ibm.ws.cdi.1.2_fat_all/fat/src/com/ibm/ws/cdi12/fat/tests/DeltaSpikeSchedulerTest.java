/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class DeltaSpikeSchedulerTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("cdi12DeltaSpikeServer");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application deltaspikeTest started");
    }

    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testSchedulingeJob() throws Exception {
        int count = 0;
        boolean found = false;
        while ((count < 6) && (!found)) {
            Thread.sleep(1000); //sleep for 1s
            found = !server.findStringsInLogs("#increase called by com.ibm.ws.cdi.deltaspike.scheduler.MyScheduler").isEmpty();
            count++;
        }
        Assert.assertTrue("Test for deltaspike scheduler ", found);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W");
    }
}
