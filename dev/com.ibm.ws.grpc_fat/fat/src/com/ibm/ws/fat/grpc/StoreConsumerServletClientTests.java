/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessController;
import java.security.PrivilegedAction;
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
import componenttest.annotation.SkipForRepeat;
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
// Skip for EE11: OpenAPI NPE due to @OAuthScope having a name but no description in ConsumerRestAppWrapper.
@SkipForRepeat(SkipForRepeat.EE11_FEATURES)
public class StoreConsumerServletClientTests extends FATServletClient {

    protected static final Class<?> c = StoreConsumerServletClientTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("StoreServer")
    public static LibertyServer storeServer;

    @Server("ProducerServer")
    public static LibertyServer producerServer;

    @Server("ConsumerServer")
    public static LibertyServer consumerServer;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @BeforeClass
    public static void setUp() throws Exception {

        storeServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        producerServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        consumerServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        boolean isArchive = false;
        // To export the assembled services application archive files, set isArchive to true
        // run it locally , keep this false when merging

        StoreClientTestsUtils.addStoreApp(storeServer, isArchive);

        StoreClientTestsUtils.addProducerApp(producerServer, isArchive);

        // the consumer_war is different here than one created in REST client
        StoreClientTestsUtils.addConsumerApp(consumerServer, isArchive);

        storeServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", storeServer.waitForStringInLog("CWWKO0219I.*ssl"));

        producerServer.useSecondaryHTTPPort(); // sets httpSecondaryPort and httpSecondarySecurePort
        producerServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", producerServer.waitForStringInLog("CWWKO0219I.*ssl"));

        // set bvt.prop.member_1.http=8080 and bvt.prop.member_1.https=8081
        consumerServer.setHttpDefaultPort(Integer.parseInt(getSysProp("member_1.http")));
        int securePort = Integer.parseInt(getSysProp("member_1.https"));

        Log.info(c, "setUp", "here is the secure port " + securePort);

        consumerServer.setHttpDefaultSecurePort(securePort);
        consumerServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", consumerServer.waitForStringInLog("CWWKO0219I.*ssl"));

        Log.info(c, "setUp", "Check if Store.war started");
        assertNotNull(storeServer.waitForStringInLog("CWWKZ0001I: Application StoreApp started"));

        Log.info(c, "setUp", "Check if Prodcuer.war started");
        assertNotNull(producerServer.waitForStringInLog("CWWKZ0001I: Application StoreProducerApp started"));

        //once this war file is installed on external Server
        // send the request e.g.
        // URL=http://localhost:8030/StoreProducerApp/ProducerEndpointFATServlet?testMethod=testClientStreaming

    }

    //Similar to these are added in logs and we can ignore
    //SRVE9967W: The manifest class path xml-apis.jar can not be found in jar file wsjar:file:/.../open-liberty/dev/build.image/wlp/usr/servers/StoreServer/apps/StoreApp.war!/WEB-INF/lib/serializer-2.7.2.jar or its parent.
    //SRVE9967W: The manifest class path xercesImpl.jar can not be found in jar file wsjar:file:/.../open-liberty/dev/build.image/wlp/usr/servers/StoreServer/apps/StoreApp.war!/WEB-INF/lib/xalan-2.7.2.jar or its parent.
    @AfterClass
    public static void tearDown() throws Exception {
        Exception excep = null;

        try {
            //Expected failures

            //CWIML4537E: The login operation could not be completed.
            //The specified principal name dev2 is not found in the back-end repository.

            //CWWKS1725E: The resource server failed to validate the access token
            //because the validationEndpointUrl [null] was either not a valid URL
            // or could not perform the validation.

            //CWWKS1737E: The OpenID Connect client [null] failed to validate the JSON Web Token.
            //The cause of the error was: [CWWKS1781E: Validation failed for the token requested
            //by the client [null] because the (iss) issuer [testIssuer] that is specified in
            //the token does not match any of the trusted issuers [testIssuerBad]
            //that are specified by the [issuerIdentifier] attribute of the OpenID
            //Connect client configuration.]

            if (storeServer != null)
                storeServer.stopServer("SRVE9967W", "CWIML4537E", "CWWKS1725E", "CWWKS1737E", "CWWKT0204E", "CWWKT0205E");
        } catch (Exception e) {
            excep = e;
            Log.error(c, "store tearDown", e);
        }

        try {
            if (consumerServer != null)
                consumerServer.stopServer("SRVE9967W");
        } catch (Exception e) {
            if (excep == null)
                excep = e;
            Log.error(c, "consumer tearDown", e);
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

    @Test
    public void testConsumerWarStartWithGrpcService() throws Exception {
        Log.info(c, name.getMethodName(), "Check if Consumer.war started");
        assertNotNull(consumerServer.waitForStringInLog("CWWKZ0001I: Application StoreConsumerApp started"));

    }

    /**
     * This test will send a valid Basic Authorization header created in ConsumerServiceRestClient
     * The Authorization header will be propagated using grpcClient.
     *
     * This test will sent grpc requests to create data, getAppInfo with Auth header , delete data.
     * The test passes when correct "appName" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppInfo_Valid_BasicAuth_SC() throws Exception {
        // first create the data
        Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--START-----------------------");

        String appName = "myAppConsumerSC1";
        try {
            // create appName
            StoreClientTestsUtils.createAssertMyApp(appName, name.getMethodName(), producerServer);

            // getApp Info appName
            this.getAppInfo(appName, "getAppInfo", true);
        } finally {
            // even if the above fails try to delete the appName
            // delete appName
            StoreClientTestsUtils.deleteAssertMyApp(appName, name.getMethodName(), producerServer);

            Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--FINISH-----------------------");
            Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        }
    }

    /**
     * This test will send a Bad Basic Authorization header created in ConsumerServiceRestClient
     * The Authorization header will be propagated using grpcClient.
     *
     * This test will sent grpc requests to create data, getAppInfo with Bad Auth header , delete data.
     * The test passes when "Expected auth failure" is asserted in response.
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    @Test
    public void testGetAppInfo_Bad_BasicAuth_SC() throws Exception {
        // first create the data
        Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--START-----------------------");

        String appName = "myAppConsumerSC1";
        try {
            // create appName
            StoreClientTestsUtils.createAssertMyApp(appName, name.getMethodName(), producerServer);

            // getApp Info appName
            this.getAppInfo(appName, "getAppInfo_Bad_BasicAuth_SC", false);
        } finally {
            // even if the above fails try to delete the appName
            // delete appName
            StoreClientTestsUtils.deleteAssertMyApp(appName, name.getMethodName(), producerServer);

            Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--FINISH-----------------------");
            Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        }
    }

    @Test
    public void testGetAppPrice_SC() throws Exception {

        // first create the data
        Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--START-----------------------");

        String appName = "myAppPriceConsumerSC1";
        String testName = "getAppPrice";
        try {
            // create appName
            StoreClientTestsUtils.createAssertMyApp(appName, name.getMethodName(), producerServer);

            // getApp price given appName
            this.getAppPrice(appName, testName, true);
        } finally {
            // even if the above fails try to delete the appName
            // delete appName
            StoreClientTestsUtils.deleteAssertMyApp(appName, name.getMethodName(), producerServer);
            Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--FINISH-----------------------");
            Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        }
    }

    //TODO
    @Test
    public void testGetMultiAppNames_SC() throws Exception {
        // first create the data
        Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--START-----------------------");

        try {
            // create appNames
            StoreClientTestsUtils.createAssertMultiApps(name.getMethodName(), producerServer);

            // TODO
            // get the multi app names
        } finally {
            // even if the above fails try to delete the appName
            // delete appNames
            StoreClientTestsUtils.deleteAssertMultiApps(name.getMethodName(), producerServer);
            Log.info(c, name.getMethodName(), " ------------" + name.getMethodName() + "--FINISH-----------------------");
            Log.info(c, name.getMethodName(), " ----------------------------------------------------------------");
        }

    }

    private void getAppPrice(String appName, String inputTestName, boolean b) {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- invoking consumer servlet client to get app info");

            HtmlPage page = StoreClientTestsUtils.getConsumerResultPage(webClient, inputTestName, appName, false, consumerServer);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, name.getMethodName(), response);
                boolean isValidResponse = false;
                isValidResponse = response.contains("BLUEPOINTS");
                assertTrue(isValidResponse);

            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            webClient.close();
            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- completed consumer servlet client to get app info");
            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");

        }
    }

    /**
     * @throws Exception
     *
     */
    private void getAppInfo(String appName, String inputTestName, boolean addAuthHeader) throws Exception {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- invoking consumer servlet client to get app info");

            HtmlPage page = StoreClientTestsUtils.getConsumerResultPage(webClient, inputTestName, appName, addAuthHeader, consumerServer);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, name.getMethodName(), response);
                boolean isValidResponse = false;
                if (addAuthHeader) {
                    isValidResponse = response.contains(appName);
                    assertTrue(isValidResponse);
                } else {
                    isValidResponse = response.contains("Expected auth failure");
                    assertTrue(isValidResponse);
                }
            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            webClient.close();
            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");
            Log.info(c, name.getMethodName(), " ----- completed consumer servlet client to get app info");
            Log.info(c, name.getMethodName(), " ------------------------------------------------------------");

        }
    }

}
