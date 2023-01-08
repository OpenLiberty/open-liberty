/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.xmlws40.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/*
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ProviderLookup {

    private final static Class<?> c = ProviderLookup.class;

    @Rule
    public TestName testName = new TestName();

    @Server("ProviderLookupTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "providerLookup", "com.ibm.jaxws.providerlookup.echo",
                                      "com.ibm.jaxws.providerlookup.echo.client",
                                      "com.ibm.jaxws.providerlookup.servlet");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKZ0013E", "CWWKL0084W", "SRVE9967W", "SRVE0319E", "CWNEN0030E");
        }
    }

    /*
     * This negative test assign a class name to not be found as SPI Provider to fail
     */
    @Test
    @ExpectedFFDC({ "java.security.PrivilegedActionException", "java.lang.reflect.InvocationTargetException", "com.ibm.wsspi.injectionengine.InjectionException",
                    "jakarta.servlet.UnavailableException" })
    public void testClassNotFound() throws Exception {
        String serviceProvider = "com.class.to.not.found";
        Log.info(c, testName.getMethodName(), "SPI Service Provider: " + serviceProvider);

        initServerWithServiceProvider(serviceProvider);
        int respCode = getServletResponse();
        assertTrue("SPI provider :" + serviceProvider + " should throw class not found error. Test should fail.", respCode == HttpURLConnection.HTTP_NOT_FOUND);
    }

    /*
     * This negative test assign CXF to not be found as SPI Provider to fail even though we provide required CXF libraries in the web application
     */
    @Test
    @ExpectedFFDC({ "java.security.PrivilegedActionException", "java.lang.reflect.InvocationTargetException", "com.ibm.wsspi.injectionengine.InjectionException",
                    "jakarta.servlet.UnavailableException" })
    public void testCXFProviderImpl() throws Exception {
        String serviceProvider = "org.apache.cxf.jaxws.spi.ProviderImpl";
        Log.info(c, testName.getMethodName(), "SPI Service Provider: " + serviceProvider);

        initServerWithServiceProvider(serviceProvider);
        int respCode = getServletResponse();
        assertTrue("SPI provider :" + serviceProvider + " is third-party implementation (CXF) Provider of Liberty. Test should fail.",
                   respCode == HttpURLConnection.HTTP_NOT_FOUND);

        assertNotNull("Expected to see class not found exception for class: " + serviceProvider,
                      server.waitForStringInLog("SRVE0319E"));
    }

    /*
     * This positive test assign the default provider for Liberty platform.
     */
    @Test
    @AllowedFFDC({ "java.security.PrivilegedActionException", "java.lang.reflect.InvocationTargetException", "com.ibm.wsspi.injectionengine.InjectionException",
                   "jakarta.servlet.UnavailableException" })
    public void testLibertyProviderImpl() throws Exception {
        String serviceProvider = "com.ibm.ws.jaxws.client.LibertyProviderImpl";
        Log.info(c, testName.getMethodName(), "SPI Service Provider: " + serviceProvider);

        initServerWithServiceProvider(serviceProvider);
        int respCode = getServletResponse();
        assertTrue("SPI provider :" + serviceProvider + " is the default provider of Liberty. Test should pass.", respCode == HttpURLConnection.HTTP_OK);
    }

    /*
     * This positive test assign third-party(sun) provider for Liberty platform.
     */
    @Test
    @AllowedFFDC({ "java.security.PrivilegedActionException", "java.lang.reflect.InvocationTargetException", "com.ibm.wsspi.injectionengine.InjectionException",
                   "jakarta.servlet.UnavailableException" })
    public void testSunProviderImpl() throws Exception {
        String serviceProvider = "com.sun.xml.ws.spi.ProviderImpl";
        Log.info(c, testName.getMethodName(), "SPI Service Provider: " + serviceProvider);

        initServerWithServiceProvider(serviceProvider);
        int respCode = getServletResponse();
        assertTrue("SPI provider :" + serviceProvider + " is third-party implementation (Sun) Provider of Liberty. Test should pass.",
                   respCode == HttpURLConnection.HTTP_OK);
    }

    /*
     * Each test starts server with a different jakarta.xml.ws.spi.Provider trough this ethod
     */
    private void initServerWithServiceProvider(String providerName) throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        jvmOptions.put("-Djakarta.xml.ws.spi.Provider", providerName);
        server.setJvmOptions(jvmOptions);

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*providerLookup");
    }

    /*
     * @returns HTTP connection status as a result
     * When connection successful, log servlet response
     */
    private int getServletResponse() throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, "/providerLookup/ProviderLookupTestServlet");
        if (HttpURLConnection.HTTP_OK == con.getResponseCode()) {
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String result = br.readLine();
            Log.info(ProviderLookup.class, "ServletResponse: ", result);
        }
        return con.getResponseCode();
    }

}
