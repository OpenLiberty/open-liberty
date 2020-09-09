/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
public class JCA17Test extends FATServletClient {
    @Server("com.ibm.ws.jca.fat")
    public static LibertyServer server;

    private static final String SERVLET_AOD = "aod";
    private static final String SERVLET_AODS = "aods";
    private static final String SERVLET_DD = "JCADDServlet";
    private static final String SERVLET_DFDS = "dfds";
    private static final String SERVLET_TRAN = "transupport";

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation("fvtapp");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("J2CA8030E",
                          "J2CA0241W",
                          "J2CA0045E.*connfactory(1|2)",
                          "SRVE0319E.*JCADDServlet",
                          "SRVE0276E.*JCADDServlet",
                          "CWNEN1006E");
    }

    private void runTest(String servlet) throws Exception {
        FATServletClient.runTest(server, "fvtweb/" + servlet, testName);
    }

    @Test
    @AllowedFFDC("java.lang.Exception")
    public void testBeanArchiveResourceAdapter() throws Exception {
        assertNotNull("Server should report : Resource Adapter Bean Archives are not supported",
                      server.waitForStringInLog("J2CA0241W"));
    }

    @Test
    public void testLookupAdministeredObjectDefinition() throws Exception {
        runTest(SERVLET_AOD);
    }

    @Test
    public void testLookupAdministeredObjectDefinitions() throws Exception {
        runTest(SERVLET_AODS);
    }

    @Test
    public void testLookupAdministeredObjectNoInterfaceName() throws Exception {
        runTest(SERVLET_AODS);
    }

    @Test
    public void testLookupAdministeredObjectWithProperties() throws Exception {
        runTest(SERVLET_AODS);
    }

    @Test
    public void testLookupConnectionFactoryWebDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupConnectionFactoryAppDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupConnectionFactoryEJBDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupAdministeredObjectWebDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupAdministeredObjectAppDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupAdministeredObjectEJBDD() throws Exception {
        runTest(SERVLET_DD);
    }

    @Test
    public void testLookupConnectionFactoryAnnotation() throws Exception {
        runTest(""); // servlet context root is '/'
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException")
    public void testConnectionFactoryAnnotationMaxPoolSize() throws Exception {
        runTest(""); // servlet context root is '/'
    }

    @Test
    public void testLookupConnectionFactoryDefinitionsAnnotation() throws Exception {
        runTest(SERVLET_DFDS);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException")
    public void testConnectionFactoryDefinitionsAnnotationMaxPoolSize() throws Exception {
        runTest(SERVLET_DFDS);
    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException",
                    "javax.servlet.UnavailableException",
                    "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testConnectionFactoryAnnotationTransactionSupport() throws Throwable {
        try {
            runTest(SERVLET_TRAN);
        } catch (Throwable e) {
            if (!e.getMessage().contains("CWNEN1006E"))
                throw e;
        }
    }
}
