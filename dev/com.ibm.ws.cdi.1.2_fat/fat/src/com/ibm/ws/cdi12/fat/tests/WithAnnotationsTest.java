/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
