/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests.implicit;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.cdi12.suite.ShrinkWrapServer;

import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test that wars can be implicit bean archives.
 */
public class ImplicitEJBTest {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12ImplicitServer");

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @BeforeClass
    public static void setUp() throws Exception {
        for (Archive archive : ShrinkWrapServer.getAppsForServer("cdi12ImplicitServer")) {
            ShrinkHelper.exportDropinAppToServer(server, archive);
        }

    }

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("implicitEJBInWar").andServlet("");

    @Test
    public void testImplicitEJB() {}

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
