/*
 * =============================================================================
 * Copyright (c) 2013, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.global.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jndi.global.fat.data.AppName;
import com.ibm.ws.jndi.global.fat.data.ServletName;
import com.ibm.ws.jndi.global.fat.web.JNDITestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import junit.framework.Assert;

/**
 * This test is to ensure that we get the expected behavior when the jndi
 * feature is not enabled.
 */
@RunWith(FATRunner.class)
public class JNDIFeatureTests extends FATServletClient {
    @ClassRule
    public static ServletMethodRunner runner;

    @Server("jndi_fat")
    @TestServlet(servlet = JNDITestServlet.class, contextRoot = "jndi-global")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, FATSuite.JNDI_GLOBAL_WAR);
        runner = new ServletMethodRunner(AppName.JNDI_GLOBAL, ServletName.JNDI_TEST_SERVLET, server);
        server.startServer();

    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }

    /**
     * Verify that behavior is expected with and without the jndi feature
     * enabled dynamically. Start with jndi not enabled and dynamically modify.
     *
     * 1. Run method without jndi enabled - verify correct exception message
     * 2. Run method with jndi enabled - verify it works properly
     * 3. Run method without jndi enabled - verify correct exception message
     */
    @Test
    public void testNoJNDIFeature() throws Exception {
        // Remove jndi feature
//        ServerConfiguration serverConfig = server.getServerConfiguration();
//        serverConfig.getFeatureManager().getFeatures().remove("jndi-1.0");
//        server.updateServerConfiguration(serverConfig);

//        server.startServer();

        modifyFeatureAndWait(false);
        // Run test expecting the error message be returned
        runErrorCase();

        // Enable jndi feature
        modifyFeatureAndWait(true);

        // Run test, expecting clean run now that jndi is enabled
        runner.run("testRebind");

        // Remove jndi feature
        modifyFeatureAndWait(false);

        // Make sure in the same method invocation we get the message again
        runErrorCase();

        //Used modifyFeature instead of server config as we don't start/stop server within the method anymore
        modifyFeatureAndWait(true);

        // Stop the server and reset the jndi feature to be enabled again (just in case tests are not run
        // in order)
//        server.stopServer();

//        serverConfig = server.getServerConfiguration();
//        serverConfig.getFeatureManager().getFeatures().add("jndi-1.0");
//        server.updateServerConfiguration(serverConfig);
    }

    /**
     * Verify that behavior is expected with and without the jndi feature
     * enabled dynamically. Start with jndi enabled and dynamically modify.
     *
     * 1. Run method with jndi enabled - verify it works properly
     * 2. Run method without jndi enabled - verify correct exception message
     * 3. Run method with jndi enabled - verify it works properly
     */
    @Test
    public void testDynamicFeature() throws Exception {
        // Hit servlet and verify that new InitialContext.lookup() works
        runner.run("testRebind");

        // Remove jndi feature
        modifyFeatureAndWait(false);

        // Hit the same servlet method, looking for the expected error message
        runErrorCase();

        // Enable jndi feature
        modifyFeatureAndWait(true);

        // Verify that same servlet request now works again
        runner.run("testRebind");
    }

    /**
     * Wait for the feature and config update to finish. This depends
     * upon a previous call to a setMark* method.
     *
     * @throws Exception
     */
    private void modifyFeatureAndWait(boolean add) throws Exception {
        ServerConfiguration serverConfig = server.getServerConfiguration();
        if (add) {
            serverConfig.getFeatureManager().getFeatures().add("jndi-1.0");
        } else {
            serverConfig.getFeatureManager().getFeatures().remove("jndi-1.0");
        }

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);

        Assert.assertNotNull("Wait for feature update did not succeed.",
                             server.waitForStringInLogUsingMark("CWWKF0008I"));
        Assert.assertNotNull("Wait for config update did not succeed.",
                             server.waitForStringInLogUsingMark("CWWKG0017I"));
    }

    /**
     * Run the test using the method that returns the result, since it won't be
     * "SUCCESS" in the error case. Verify that the message from the fake factory
     * was observed (and not some other exception).
     */
    private void runErrorCase() throws Exception {
        String errorMessage = runner.getResponseFor("testRebind");
        Assert.assertTrue("Did not get the correct error message from servlet invocation: " + errorMessage,
                          errorMessage.matches(".*CWWKE0800W.*JNDITestServlet.*"));
    }

    @Test
    public void sayHello() {
        System.out.println("I am saying hello");
    }
}
