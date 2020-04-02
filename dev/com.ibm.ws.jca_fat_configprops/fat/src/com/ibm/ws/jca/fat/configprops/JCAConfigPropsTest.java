/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.configprops;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
@RunWith(FATRunner.class)
public class JCAConfigPropsTest extends FATServletClient {

    private static final String APP_NAME = "fvtweb";
    private static final String RAR_NAME = "MapRA";

    @Server("com.ibm.ws.jca.fat.configprops")
    public static LibertyServer server;

    private void runTest() throws Exception {
        runTest(server, APP_NAME, testName);
    }

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "web");
        ShrinkHelper.defaultRar(server, RAR_NAME, "fat.configpropsra.adapter");

        server.startServer();
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    @Test
    public void testMCFAnnotationOverridesRAWLPExtension() throws Exception {
        runTest();
    }

    @Test
    public void testMCFDDOverridesMCFAnnotation() throws Exception {
        runTest();
    }

    @Test
    public void testMCFJavaBean() throws Exception {
        runTest();
    }

    @Test
    public void testRAAnnotationOverridesRAJavaBean() throws Exception {
        runTest();
    }

    @Test
    public void testRADeploymentDescriptorOverridesRAAnnotation() throws Exception {
        runTest();
    }

    @Test
    public void testRAJavaBeanOverridesMCFJavaBean() throws Exception {
        runTest();
    }

    @Test
    public void testRAWLPExtensionOverridesRADeploymentDescriptor() throws Exception {
        runTest();
    }

    @Test
    public void testServerXMLOverridesWLPExtension() throws Exception {
        runTest();
    }

    @Test
    public void testWLPExtensionOverridesMCFDeploymentDescriptor() throws Exception {
        runTest();
    }
}
