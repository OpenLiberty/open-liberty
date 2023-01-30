/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class AddNumbersTest {

    @Server("AddNumbersTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("AddNumbersTestServer");

    private static final String SERVLET_PATH = "/addNumbersClient/AddNumbersTestServlet";

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "addNumbersClient", "com.ibm.ws.jaxws.client", "com.ibm.ws.jaxws.client.servlet");
        ShrinkHelper.defaultDropinApp(server, "addNumbersProvider", "com.ibm.ws.jaxws.provider");
        server.copyFileToLibertyServerRoot("", "addNumbersClientTest/AddNumbers.wsdl");
    }

    @Test
    public void testDynamicallyAddRemovalGlobalHandlers() throws Exception {

        //server.installUserBundle("TestHandler1_1.0.0.201311011652");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle2", "com.ibm.ws.userbundle2.myhandler");
        TestUtils.installUserFeature(server, "TestHandler1Feature1");
        //server.installUserBundle("TestHandler2_1.0.0.201311011653");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle3", "com.ibm.ws.userbundle3.myhandler");
        TestUtils.installUserFeature(server, "TestHandler2Feature2");
        server.setServerConfigurationFile("dynamicallyAddRemove/WithNone/server.xml");
        server.startServer("dynamicallyAddingRemovalTest.log");
        String actual = invokeService(1, 2);
        String expected = "Result = 3";
        assertTrue("Expected output to contain \"" + expected
                   + "\", but instead it contained: " + actual + ".",
                   actual.indexOf(expected) != -1);

        //Enable the 1st global handler feature
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("dynamicallyAddRemove/WithFirstOne/server.xml");
        server.waitForStringInLog("CWWKF0012I.*usr:TestHandler1Feature1");
        //server.waitForStringInLog("handler.adding.notification");
        actual = invokeService(1, 2);
        assertTrue("Expected output to contain \"" + expected
                   + "\", but instead it contained: " + actual + ".",
                   actual.indexOf(expected) != -1);
        assertStatesExistedFromMark(true, 5000, new String[] {
                                                               "handle outbound message in TestHandler1InBundle2",
                                                               "handle outbound message in TestHandler2InBundle2",
                                                               "handle inbound message in TestHandler2InBundle2",
                                                               "handle inbound message in TestHandler1InBundle2",
                                                               "handle outbound message in TestHandler1InBundle2",
                                                               "handle outbound message in TestHandler2InBundle2",
                                                               "handle inbound message in TestHandler2InBundle2",
                                                               "handle inbound message in TestHandler1InBundle2" });

        //Enable the 2nd global handler feature
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("dynamicallyAddRemove/WithTwo/server.xml");
        server.waitForStringInLog("CWWKF0012I.*usr:TestHandler2Feature2");
        //server.waitForStringInLog("handler.adding.notification");
        actual = invokeService(1, 2);
        assertTrue("Expected output to contain \"" + expected
                   + "\", but instead it contained: " + actual + ".",
                   actual.indexOf(expected) != -1);

        assertStatesExistedFromMark(true, 5000, new String[] {
                                                               "handle outbound message in TestHandler1InBundle2",
                                                               "handle outbound message in TestHandler1InBundle3",
                                                               "handle outbound message in TestHandler2InBundle2",
                                                               "handle outbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler2InBundle2",
                                                               "handle inbound message in TestHandler1InBundle3",
                                                               "handle inbound message in TestHandler1InBundle2",
                                                               "handle outbound message in TestHandler1InBundle2",
                                                               "handle outbound message in TestHandler1InBundle3",
                                                               "handle outbound message in TestHandler2InBundle2",
                                                               "handle outbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler2InBundle2",
                                                               "handle inbound message in TestHandler1InBundle3",
                                                               "handle inbound message in TestHandler1InBundle2" });

        //Remove the 1st global handler feature
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("dynamicallyAddRemove/WithSecondOne/server.xml");
        server.waitForStringInLog("CWWKF0013I.*usr:TestHandler1Feature1");
        //server.waitForStringInLog("handler.removal.notification");
        actual = invokeService(1, 2);
        assertTrue("Expected output to contain \"" + expected
                   + "\", but instead it contained: " + actual + ".",
                   actual.indexOf(expected) != -1);
        assertStatesExistedFromMark(true, 5000, new String[] {
                                                               "handle outbound message in TestHandler1InBundle3",
                                                               "handle outbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler1InBundle3",
                                                               "handle outbound message in TestHandler1InBundle3",
                                                               "handle outbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler2InBundle3",
                                                               "handle inbound message in TestHandler1InBundle3" });

    }

    @AllowedFFDC({ "java.lang.NullPointerException" })
    @Test
    public void testFaultProcessingInGlobalHandlers() throws Exception {

        //server.installUserBundle("TestHandler3_1.0.0.201311011654");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle4", "com.ibm.ws.userbundle4.myhandler");
        TestUtils.installUserFeature(server, "TestHandler3Feature3");
        server.setServerConfigurationFile("FaultProcessingInGlobalHandlers/WithFirstOne/server.xml");
        server.startServer("FaultProcessingInGlobalHandlersTest.log");
        invokeService(1, 2);

        try {
            server.waitForStringInLog("CWWKF0012I.*usr:TestHandler3Feature3", 5000);
        } catch (Exception e) {
            assertTrue("Should get MyException", e.getMessage().equalsIgnoreCase("Error occurs when handle outbound message in TestHandler2InBundle4"));
        }

        // should call previously called handlers' handle fault and return back

        assertStatesExsited(5000, new String[] {
                                                 "handle outbound message in TestHandler1InBundle4",
                                                 "handle outbound message in TestHandler2InBundle4",
                                                 "handle fault in TestHandler1InBundle4" });

        assertStatesExsited(5000, new String[] {
                                                 "Error occurs when calling handleFault in TestHandler1InBundle4" });
    }

    /*
     * Test if we only define global handler user feature without jaxws-2.2/jaxrs-2.0, the global handlers will not take effect.
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError") // this is an error path test.  On Java 7/8 we get no FFDCs but on Java 9+ we get an FFDC for removed JDK API, so we'll allow it
    public void testGlobalHandlerFeatureWithOutJAXWSRS() throws Exception {

        //server.installUserBundle("TestHandler3_1.0.0.201311011654");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle4", "com.ibm.ws.userbundle4.myhandler");
        TestUtils.installUserFeature(server, "TestHandler3Feature3");
        server.setServerConfigurationFile("GlobalHandlersOnlyWithoutJAXWSRS/WithFirstOne/server.xml");
        server.startServer("GlobalHandlerFeatureWithOutJAXWSRSTest.log");
        assertStatesNotExsited(5000, new String[] {
                                                    "CWWKF0012I.*\\[usr:TestHandler3Feature3\\]" });

    }

    @Test
    public void testGlobalHandlerMessageContextAPI() throws Exception {

        //server.installUserBundle("TestHandler5_1.0.0.201311011655");
        ShrinkHelper.defaultUserFeatureArchive(server, "userBundle5", "com.ibm.ws.userbundle5.myhandler");
        TestUtils.installUserFeature(server, "TestHandler5Feature5");
        server.setServerConfigurationFile("GlobalHandlerMessageContextAPI/server.xml");
        server.startServer("GlobalHandlerMessageContextAPIest.log");
        String actual = invokeService(1, 2);
        //check related API result:
        // API usage call be seen in each handlers in test-applications/userBundle5
        assertStatesExistedFromMark(true, 5000, new String[] {
                                                               "get Engine.Type in OutHandler1:JAX_WS",
                                                               "get Flow.Type in OutHandler1:OUT",
                                                               "get ServiceName in OutHandler1 true",
                                                               "get ServiceName in OutHandler1 true",
                                                               "can see the soap header added by OutHandler1InBuldle5 in OutHandler2InBuldle5",
                                                               "get age in OutHandler3InBuldle5 12",
                                                               "the globalHandlerMessageContext contains age property",
                                                               "get Encoding in InHandler1InBuldle5 UTF-8",
                                                               "get LocalAddr in InHandler1InBuldle5",
                                                               "arg0 has been modified to 5000",
                                                               "arg1 has been modified to 7 in InHandler2InBuldle5",
                                                               "got adding result 5007" });

        String expected = "Result = 3";
        assertTrue("Expected output to contain \"" + expected
                   + "\", but instead it contained: " + actual + ".",
                   actual.indexOf(expected) != -1);

    }

    private String invokeService(int num1, int num2) throws Exception {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?number1=").append(Integer.toString(num1)).append("&number2=").append(Integer.toString(num2));
        String urlStr = sBuilder.toString();

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, 5);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        return line == null ? "null" : line;

    }

    private void assertStatesExistedFromMark(boolean needReset, long timeout, String... states) {
        if (needReset) {
            server.resetLogMarks();
        }

        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLogUsingMark(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }

    private void assertStatesExsited(long timeout, String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }

    private void assertStatesNotExsited(long timeout, String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr == null);
            }
        }
    }
}
