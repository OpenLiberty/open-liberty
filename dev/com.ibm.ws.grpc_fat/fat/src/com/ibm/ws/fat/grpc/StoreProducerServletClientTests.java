/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * @author anupag
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class StoreProducerServletClientTests extends FATServletClient {

    protected static final Class<?> c = StoreProducerServletClientTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("StoreServer")
    public static LibertyServer storeServer;

    @Server("ProducerServer")
    public static LibertyServer producerServer;

    @BeforeClass
    public static void setUp() throws Exception {

        storeServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        producerServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        boolean isArchive = false;
        // To export the assembled services application archive files, set isArchive to true
        // run it locally , keep this false when merging
        StoreClientTestsUtils.addStoreApp(storeServer, isArchive);

        StoreClientTestsUtils.addProducerApp(producerServer, isArchive);

        storeServer.startServer(c.getSimpleName() + ".log");
        Log.info(c, "setUp", "Check if in store server ssl started");
        assertNotNull("CWWKO0219I.*ssl not received", storeServer.waitForStringInLog("CWWKO0219I.*ssl"));

        producerServer.useSecondaryHTTPPort(); // sets httpSecondaryPort and httpSecondarySecurePort
        producerServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", producerServer.waitForStringInLog("CWWKO0219I.*ssl"));

        Log.info(c, "setUp", "Check if Store.war started");
        assertNotNull(storeServer.waitForStringInLog("CWWKZ0001I: Application StoreApp started"));

        Log.info(c, "setUp", "Check if Prodcuer.war started");
        assertNotNull(producerServer.waitForStringInLog("CWWKZ0001I: Application StoreProducerApp started"));

        //once this war file is installed on external Server
        // send the request e.g.
        // URL=http://localhost:8030/StoreProducerApp/ProducerServlet?testName=testClientStreaming

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception excep = null;

        try {
            //This can be ignored
            //SRVE9967W: The manifest class path xml-apis.jar can not be found in jar file
            //wsjar:file:/.../open-liberty/dev/build.image/wlp/usr/servers/StoreServer/
            //apps/StoreApp.war!/WEB-INF/lib/serializer-2.7.2.jar or its parent.
            //SRVE8055E and SRVE8056E: Error when a reset stream comes in from the client to
            //cancel a stream while data is being written from the HTTP2 connection on that stream
            if (storeServer != null)
                storeServer.stopServer("SRVE9967W", "SRVE8055E", "SRVE8056E");
        } catch (Exception e) {
            excep = e;
            Log.error(c, "store tearDown", e);
        }

        try {
            if (producerServer != null)
                producerServer.stopServer();
        } catch (Exception e) {
            if (excep == null)
                excep = e;
            Log.error(c, "producer tearDown", e);
        }

        if (excep != null)
            throw excep;
    }

    //____________________________________________________
    // Test definition starts
    //____________________________________________________

    /**
     * This test will sent grpc requests to create data, delete data.
     * The test passes when correct string is asserted in response.
     */
    @Test
    public void testCreateDeleteMyApp_SC() throws Exception {
        this.createDeleteMyApp();
    }

    /**
     * This test will sent grpc requests to create data, create same data, delete data.
     * The test passes when correct expected failure string is asserted in response.
     */
    @Test
    public void testDuplicate_CreateDeleteMyApp_SC() throws Exception {
        this.duplicate_createDeleteMyApp();
    }

    /**
     * This test will sent grpc requests to create data using streaming, delete data.
     * The test passes when correct string is asserted in response.
     */
    @Test
    public void testCreateDeleteMultiApp_SC() throws Exception {
        this.createDeleteMultiApp();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testClientStreaming_SC() throws Exception {
        this.clientStreaming();
    }

    //@Mode(TestMode.FULL)
    //@Test
    public void testClientStreamingMetrics_SC() throws Exception {
        this.clientStreamingMetrics();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testServerStreaming_SC() throws Exception {
        this.serverStreaming();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testServerStreamingMetrics_SC() throws Exception {
        this.serverStreamingMetrics();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTwoWayStreaming_SC() throws Exception {
        this.twoWayStreaming();

    }

    @Mode(TestMode.FULL)
    @Test
    public void testTwoWayStreamingAsyncThread_SC() throws Exception {
        this.twoWayStreamingAsyncThread();
    }

    //____________________________________________________
    // Test definition ends
    //____________________________________________________

    // Test implementation starts
    //____________________________________________________

    private void createDeleteMyApp() throws Exception {

        Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--START-----------------------");

        String appName = "myApp";
        try {
            // create appNames
            StoreClientTestsUtils.createAssertMyApp(appName, name.getMethodName(), producerServer);
        } finally {
            // even if the above fails try to delete the appName
            // delete appNames
            StoreClientTestsUtils.deleteAssertMyApp(appName, name.getMethodName(), producerServer);
            Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--FINISH-----------------------");
            Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        }
    }

    private void duplicate_createDeleteMyApp() throws Exception {

        Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--START-----------------------");

        String appName = "myApp";
        try {
            // create appNames
            StoreClientTestsUtils.createAssertMyApp(appName, name.getMethodName(), producerServer);

            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- invoking Producer servlet client to create duplicate app to test ALREADY EXIST exception.");

            // call again to create same entry
            // create appNames
            StoreClientTestsUtils.createAssertMyApp(appName, name.getMethodName(), producerServer);

        } catch (Exception e) {
            Log.info(c, name.getMethodName(), ": exception message: " + e.getMessage());
            e.printStackTrace();

        } finally {
            // even if the above fails try to delete the appName
            // delete appNames
            StoreClientTestsUtils.deleteAssertMyApp(appName, name.getMethodName(), producerServer);
            Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--FINISH-----------------------");
            Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        }
    }

    private void createDeleteMultiApp() throws Exception {

        // first create the data
        Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--START-----------------------");

        try {
            // create appNames
            StoreClientTestsUtils.createAssertMultiApps(name.getMethodName(), producerServer);
        } finally {
            // even if the above fails try to delete the appName
            // delete appNames
            StoreClientTestsUtils.deleteAssertMultiApps(name.getMethodName(), producerServer);
            Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--FINISH-----------------------");
            Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        }

    }

    private void clientStreaming() throws Exception {

        String m = name.getMethodName();

        Log.info(c, m, " ----------------------------------------------------------------");
        Log.info(c, m, " ------------" + m + "--START-----------------------");

        startclientStreaming();

        Log.info(c, m, " ------------" + m + "--FINISH-----------------------");
        Log.info(c, m, " ----------------------------------------------------------------");
    }

    private void clientStreamingMetrics() throws Exception {

        String m = name.getMethodName();

        Log.info(c, m, " ----------------------------------------------------------------");
        Log.info(c, m, " ------------" + m + "--START-----------------------");

        try {
            startclientStreaming();
            Log.info(c, m, " ---startclientStreaming--in Metrics---FINISH--------------");
        } finally {
            getClientStreamingMetricValue();
        }
        Log.info(c, m, " ------------" + m + "--FINISH-----------------------");
        Log.info(c, m, " ----------------------------------------------------------------");
    }

    /**
     * @throws Exception
     *
     */
    private void getClientStreamingMetricValue() throws Exception {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- invoking producer servlet client for clientStreamingMetrics");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "clientStreamingMetrics", producerServer);

            if (page != null) {
                String metricValue = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, name.getMethodName(), ": client stream entity/result: " + metricValue);

                if (metricValue == null || Integer.parseInt(metricValue) < 200) {
                    fail(String.format("Incorrect metric value [%s]. Expected [%s], got [%s]", "grpc.server.receivedMessages.total", ">=200", metricValue));
                }
            } else {
                fail("getClientStreamingMetricValue: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
        }

    }

    private void startclientStreaming() throws Exception {
        String m = name.getMethodName();

        WebClient webClient = new WebClient();
        try {

            Log.info(c, m, " ------------------------------------------------------------");
            Log.info(c, m, " ----- invoking producer servlet client for clientStreaming");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "clientStreaming", producerServer);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, m, ": client stream entity/result: " + response);

                boolean isValidResponse = response.contains("success");

                assertTrue(isValidResponse);
            } else {
                fail("startclientStreaming: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
        }

    }

    private void serverStreaming() throws Exception {

        String m = name.getMethodName();
        Log.info(c, m, " ----------------------------------------------------------------");
        Log.info(c, m, " ------------" + m + "--START-----------------------");

        startServerStreaming();

        Log.info(c, m, " ------------" + m + "--FINISH-----------------------");
        Log.info(c, m, " ----------------------------------------------------------------");
    }

    private void serverStreamingMetrics() throws Exception {

        String m = name.getMethodName();
        Log.info(c, m, " ----------------------------------------------------------------");
        Log.info(c, m, " ------------" + m + "--START-----------------------");

        try {
            startServerStreaming();
            Log.info(c, m, " -startServerStreaming--in Metrics---FINISH--------------");
        } finally {
            getServerStreamingMetricValue();
        }

        Log.info(c, m, " ------------" + m + "--FINISH-----------------------");
        Log.info(c, m, " ----------------------------------------------------------------");
    }

    private void startServerStreaming() throws Exception {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- invoking producer servlet client for serverStreaming");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "serverStreaming", producerServer);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, name.getMethodName(), ": client stream entity/result: " + response);

                boolean isValidResponse = response.contains("success");

                assertTrue(isValidResponse);
            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
        }
    }

    /**
     * @throws Exception
     *
     */
    private void getServerStreamingMetricValue() throws Exception {
        WebClient webClient = new WebClient();
        try {

            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- invoking producer servlet client for serverStreamingMetrics");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "serverStreamingMetrics", producerServer);

            if (page != null) {
                String metricValue = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, name.getMethodName(), ": client stream entity/result: " + metricValue);

                if (metricValue == null || new Float(metricValue).intValue() < 200) {
                    fail(String.format("Incorrect metric value [%s]. Expected [%s], got [%s]", "grpc.client.receivedMessages.total", ">=200", metricValue));
                }
            } else {
                fail("getServerStreamingMetricValue: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
        }

    }

    private void twoWayStreaming() throws Exception {

        String m = name.getMethodName();
        Log.info(c, m, " ----------------------------------------------------------------");
        Log.info(c, m, " ------------" + m + "--START-----------------------");

        WebClient webClient = new WebClient();
        try {

            Log.info(c, m, " ------------------------------------------------------------");
            Log.info(c, m, " ----- invoking producer servlet client for twoWayStreamAppAsyncFlagFalse");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "twoWayStreamAppAsyncFlagFalse", producerServer);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, m, ": client stream entity/result: " + response);

                boolean isValidResponse = response.contains("success");

                assertTrue(isValidResponse);
            } else {
                fail(name.getMethodName() + " no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
            Log.info(c, m, " ------------" + m + "--FINISH-----------------------");
            Log.info(c, m, " ----------------------------------------------------------------");
        }
    }

    private void twoWayStreamingAsyncThread() throws Exception {

        String m = name.getMethodName();
        Log.info(c, m, " ----------------------------------------------------------------");
        Log.info(c, m, " ------------" + m + "--START-----------------------");

        WebClient webClient = new WebClient();
        try {

            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- invoking producer servlet client for twoWayStreamAppAsyncFlagTrue");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "twoWayStreamAppAsyncFlagTrue", producerServer);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, name.getMethodName(), ": client stream entity/result: " + response);

                boolean isValidResponse = response.contains("success");

                assertTrue(isValidResponse);
            } else {
                fail(name.getMethodName() + " no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
            Log.info(c, m, " ------------" + m + "--FINISH-----------------------");
            Log.info(c, m, " ----------------------------------------------------------------");
        }
    }

}
