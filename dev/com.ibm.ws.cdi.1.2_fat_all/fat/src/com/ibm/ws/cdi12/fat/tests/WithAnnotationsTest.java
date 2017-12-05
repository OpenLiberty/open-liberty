/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for <code>@WithAnnotations</code> used in portable extensions for observing type discovery of beans with certain annotations.
 */
@Mode(TestMode.FULL)
public class WithAnnotationsTest extends LoggingTest {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12WithAnnotationsServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return null;
    }

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("withAnnotationsApp").andServlet("testServlet");

    /**
     * Test that receiving a <code>@ProcessAnnotatedType</code> works without <code>@WithAnnotations</code>.
     */
    @Test
    public void testBasicProcessAnnotatedTypeEvent() {}

    /**
     * Test that events aren't fired for classes with no annotations.
     */
    @Test
    public void testNoAnnotations() {}

    /**
     * Test that events aren't fired for classes with annotations which aren't specified.
     */
    @Test
    public void testNonSpecifiedAnnotation() {}

    /**
     * Test that events are fired for classes with the specified annotations.
     */
    @Test
    public void testWithAnnotations() {}

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
