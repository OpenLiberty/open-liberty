/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.auth.data.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class DefaultPrincipalMappingTest extends FATServletClient {

    private final Class<DefaultPrincipalMappingTest> thisClass = DefaultPrincipalMappingTest.class;
    private static LibertyServer server;
    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.auth.data.fat.dpm");
        AuthDataFatUtils.transformApps(server, "DefaultPrincipalMappingApp.war");
        server.addInstalledAppForValidation("DefaultPrincipalMappingApp");
        server.startServer("DefaultPrincipalMappingTest.log");
        //server.waitForStringInLog("CWWKE0002I");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        //server.updateServerConfiguration(originalServerConfig);
    }

    @Test
    public void programmaticLoginWithDefaultPrincipalMapping() throws Exception {
        String method = "programmaticLoginWithDefaultPrincipalMapping";
        Log.info(thisClass, method, "Executing " + testName.getMethodName());
        runTest(server, "DefaultPrincipalMappingApp/DefaultPrincipalMappingServlet", AuthDataFatUtils.normalizeTestName(testName));
    }

    @Test
    public void getAuthDataUsingAuthDataProvider() throws Exception {
        String method = "getAuthDataUsingAuthDataProvider";
        Log.info(thisClass, method, "Executing " + testName.getMethodName());
        runTest(server, "DefaultPrincipalMappingApp/DefaultPrincipalMappingServlet", AuthDataFatUtils.normalizeTestName(testName));
    }

}
