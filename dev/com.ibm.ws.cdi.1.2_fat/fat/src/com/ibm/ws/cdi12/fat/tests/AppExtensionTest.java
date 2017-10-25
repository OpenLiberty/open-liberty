/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.xtq.common.utils.Assert;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class AppExtensionTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("cdi12AppExtensionServer");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application applicationExtension started");

    }

    @Test
    public void testAppServlet() throws Exception {

        HttpUtils.findStringInUrl(server, "/appExtension/TestServlet", "In Same WAR : created in ", "In lib JAR  : created in");
    }

    @Test
    public void testAppExtensionLoaded() throws Exception {
        Assert.assertFalse("Test for before bean discovery event",
                           server.findStringsInLogs("PlainExtension: beginning the scanning process").isEmpty());
        Assert.assertFalse("Test for processing annotation type event",
                           server.findStringsInLogs("PlainExtension: scanning type->").isEmpty());
        Assert.assertFalse("Test for after bean discovery event",
                           server.findStringsInLogs("PlainExtension: finished the scanning process").isEmpty());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
