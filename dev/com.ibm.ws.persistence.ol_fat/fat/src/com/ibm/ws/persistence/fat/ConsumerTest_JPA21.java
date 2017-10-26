/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.persistence.fat;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.persistence.consumer.web.ConsumerServlet;

import componenttest.annotation.MinimumJavaLevel;
//import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 1.7)
public class ConsumerTest_JPA21 extends FATServletClient {
    private static final String APP_NAME = "consumer";

    private static final String FEATURE_NAME = "com.ibm.ws.persistence.consumer-1.0";
    private static final String BUNDLE_NAME = "com.ibm.ws.persistence.consumer_1.0.0";

    @Server("com.ibm.ws.persistence.consumer.jpa21")
    @TestServlet(servlet = ConsumerServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @Rule
    public final TestName testName = new TestName();

//     @ClassRule
//     public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.persistence.consumer.ejb", "com.ibm.ws.persistence.consumer.model",
                                      "com.ibm.ws.persistence.consumer.web");

        server.installSystemFeature(FEATURE_NAME);
        server.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.persistence.consumer.jar");
//        server.installSystemBundle(BUNDLE_NAME);
//        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.persistence.consumer.jar");
//        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/com.ibm.ws.persistence.consumer-1.0.mf");
        server.startServer();

//        server.installSystemFeature(FEATURE_NAME);
//        server.installSystemBundle(BUNDLE_NAME);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("WTRN0074E");
//        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/com.ibm.ws.persistence.consumer.jar");
//        server.deleteFileFromLibertyInstallRoot("usr/extension/lib/features/com.ibm.ws.persistence.consumer-1.0.mf");
//        server.uninstallSystemBundle(BUNDLE_NAME);
        server.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.persistence.consumer.jar");
        server.uninstallSystemFeature(FEATURE_NAME);
    }

    @Before
    public void before() throws Exception {
//        server.setServerConfigurationFile("serverWithJPA2.1Feature.xml");
        if (!server.isStarted()) {
            server.startServer();
        }
    }

    private void runTest() throws Exception {
        HttpUtils.findStringInUrl(server, "/consumer?testMethod=" + testName.getMethodName(), "SUCCESS");
    }

    @Test
    public void testQueryInvalidStrig() throws Exception {
        runTest();
    }

    @Test
    public void countCars() throws Exception {
        runTest();
    }

    @Test
    public void testGetPersonName() throws Exception {
        runTest();
    }

    @Test
    // This will fail against databases that don't support unicode
    public void testPersistUnicodeFiltered() throws Exception {
        runTest();
    }

    @Test
    public void testPersistUnicodeNoFilter() throws Exception {
        runTest();
    }

    @Test
    public void testPersistAllValidStrings() throws Exception {
        runTest();
    }

    // @Test -- this recreates defect 159874
    public void testSerializable() throws Exception {
        runTest();
    }

    @Test
    public void testGenerateDDL() throws Exception {
        runTest();
    }

    @Test
    public void testPersistUnicodeFilteredStringBoundaries() throws Exception {
        runTest();
    }
}
