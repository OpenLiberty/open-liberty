/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.testapp.g3store.utilsConsumer.ConsumerUtils;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * this class is utils class for Store producer and consumer Servlet client tests
 */
public class StoreClientTestsUtils {

    protected static final Class<?> c = StoreClientTestsUtils.class;

    /**
     * @param server
     * @param archive
     * @throws Exception
     */
    public static void addStoreApp(LibertyServer server, boolean archive) throws Exception {

        WebArchive store_war = null;

        try {
            // Use defaultApp the <application> element is used in server.xml for security, cannot use dropin
            store_war = ShrinkHelper.defaultApp(server, "StoreApp.war",
                                                "com.ibm.testapp.g3store.cache",
                                                "com.ibm.testapp.g3store.exception",
                                                "com.ibm.testapp.g3store.interceptor",
                                                "com.ibm.testapp.g3store.grpcservice",
                                                "com.ibm.testapp.g3store.servletStore",
                                                "com.ibm.testapp.g3store.utilsStore",
                                                "com.ibm.test.g3store.grpc", // add generated src
                                                "componenttest.app", // provide componenttest servlet for SVT
                                                "org.junit"); // provide junit for SVT
        } catch (Exception e) {

            Log.info(c, "addStoreApp", "Unable to add the StoreApp.");
            throw e;
        }

        if (archive)
            ShrinkHelper.exportArtifact(store_war, "publish/savedApps/StoreServer/");

    }

    /**
     * @param server
     * @param archive
     * @throws Exception
     */
    public static void addProducerApp(LibertyServer server, boolean archive) throws Exception {

        WebArchive producer_war = null;

        try {
            producer_war = ShrinkHelper.defaultDropinApp(server, "StoreProducerApp.war",
                                                         "com.ibm.testapp.g3store.grpcProducer.api",
                                                         "com.ibm.testapp.g3store.exception",
                                                         "com.ibm.testapp.g3store.restProducer",
                                                         "com.ibm.testapp.g3store.restProducer.api",
                                                         "com.ibm.testapp.g3store.restProducer.model",
                                                         "com.ibm.testapp.g3store.restProducer.client",
                                                         "com.ibm.testapp.g3store.servletProducer",
                                                         "com.ibm.testapp.g3store.utilsProducer",
                                                         "com.ibm.ws.fat.grpc.monitoring",
                                                         "com.ibm.test.g3store.grpc", // add generated src
                                                         "componenttest.app", // provide componenttest servlet for SVT
                                                         "org.junit",
                                                         "org.hamcrest");
        } catch (Exception e) {

            Log.info(c, "addProducerApp", "Unable to add the ProducerApp.");
            throw e;
        }
        if (archive)
            ShrinkHelper.exportArtifact(producer_war, "publish/savedApps/ProducerServer/");

    }

    /**
     * @param server
     * @param archive
     * @throws Exception
     */
    public static void addConsumerApp_RestClient(LibertyServer server, boolean archive) throws Exception {

        WebArchive consumer_restclient_war = null;

        try {
            // Use defaultApp the <application> element is used in server.xml for security, cannot use dropin
            // The consumer tests needs to create data also , we will need to add producer files also

            // the consumer_war in servlet client is different than one created in REST client
            consumer_restclient_war = ShrinkHelper.defaultApp(server, "StoreConsumerApp.war",
                                                              "com.ibm.testapp.g3store.grpcConsumer.api",
                                                              "com.ibm.testapp.g3store.grpcConsumer.security",
                                                              "com.ibm.testapp.g3store.exception",
                                                              "com.ibm.testapp.g3store.restConsumer",
                                                              "com.ibm.testapp.g3store.restConsumer.api",
                                                              "com.ibm.testapp.g3store.restConsumer.model",
                                                              "com.ibm.testapp.g3store.servletConsumer",
                                                              "com.ibm.testapp.g3store.utilsConsumer",
                                                              "com.ibm.testapp.g3store.restConsumer.client",
                                                              "com.ibm.testapp.g3store.grpcProducer.api",
                                                              "com.ibm.testapp.g3store.restProducer",
                                                              "com.ibm.testapp.g3store.restProducer.api",
                                                              "com.ibm.testapp.g3store.restProducer.model",
                                                              "com.ibm.testapp.g3store.servletProducer",
                                                              "com.ibm.test.g3store.grpc", // add generated src
                                                              "com.ibm.testapp.g3store.restProducer.client",
                                                              "componenttest.app", // provide componenttest servlet for SVT
                                                              "org.junit", // provide junit for SVT
                                                              "org.hamcrest"); // provide junit for SVT
        } catch (Exception e) {

            Log.info(c, "addConsumerApp_RestClient", "Unable to add the ConsumerApp.");
            throw e;
        }
        if (archive)
            ShrinkHelper.exportArtifact(consumer_restclient_war, "publish/savedApps/ConsumerServer/");

    }

    /**
     * @param server
     * @param archive
     * @throws Exception
     */
    public static void addConsumerApp(LibertyServer server, boolean archive) throws Exception {

        WebArchive consumer_war = null;

        try {
            // Use defaultApp the <application> element is used in server.xml for security, cannot use dropin
            // The consumer tests needs to create data also , we will need to add producer files also

            // the consumer_war in servlet client is different than one created in REST client
            consumer_war = ShrinkHelper.defaultApp(server, "StoreConsumerApp.war",
                                                   "com.ibm.testapp.g3store.grpcConsumer.api",
                                                   "com.ibm.testapp.g3store.grpcConsumer.security",
                                                   "com.ibm.testapp.g3store.exception",
                                                   "com.ibm.testapp.g3store.restConsumer",
                                                   "com.ibm.testapp.g3store.restConsumer.api",
                                                   "com.ibm.testapp.g3store.restConsumer.model",
                                                   "com.ibm.testapp.g3store.servletConsumer",
                                                   "com.ibm.testapp.g3store.utilsConsumer",
                                                   "com.ibm.testapp.g3store.restConsumer.client",
                                                   "com.ibm.testapp.g3store.restProducer.model",
                                                   "com.ibm.testapp.g3store.restProducer.client",
                                                   "com.ibm.test.g3store.grpc", // add generated src
                                                   "componenttest.app", // provide junit for SVT
                                                   "org.hamcrest"); // provide junit for SVT
        } catch (Exception e) {
            Log.info(c, "addConsumerApp", "Unable to add the ConsumerApp.");
            throw e;
        }
        if (archive)
            ShrinkHelper.exportArtifact(consumer_war, "publish/savedApps/ConsumerServer/");

    }

    /**
     * @param values
     * @return
     */
    public static Object[] readAppNames(Object... values) {
        Object[] arr = new Object[values.length];
        int i = 0;
        for (Object v : values) {
            arr[i++] = v;
        }
        return arr;
    }

    /**
     * This will assert the connection to the producer server.
     * This will submit the test request to the servlet client
     * com.ibm.testapp.g3store.servletProducer.ProducerServlet
     * to run the grpc request
     *
     * @param webClient
     * @param inputTestName
     * @param server
     * @return
     * @throws Exception
     */
    public static HtmlPage getProducerResultPage(WebClient webClient, String inputTestName, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectProducerServer(server, webClient, inputTestName);

        if (form != null) {
            // set the testName
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");
            inputText.setValueAttribute(inputTestName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }

    }

    /**
     * This will assert the connection to the producer server.
     * This will submit the test request to the servlet client
     * com.ibm.testapp.g3store.servletProducer.ProducerServlet
     * to run the grpc request
     *
     * @param webClient
     * @param inputTestName
     * @param inputAppName
     * @param server
     * @return
     * @throws Exception
     */
    public static HtmlPage getProducerResultPage(WebClient webClient, String inputTestName, String inputAppName, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectProducerServer(server, webClient, inputTestName);
        if (form != null) {

            // set the testName
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");
            inputText.setValueAttribute(inputTestName);

            // set the appName
            HtmlTextInput inputApptext = (HtmlTextInput) form.getInputByName("appName");
            inputApptext.setValueAttribute(inputAppName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }
    }

    /**
     * This will assert the connection to the consumer server.
     * This will submit the test request to the servlet client
     * com.ibm.testapp.g3store.servletConsumer.ConsumerServlet
     * to run the grpc request
     *
     * @param webClient
     * @param inputTestName
     * @param server
     * @return
     * @throws Exception
     */
    public static HtmlPage getConsumerResultPage(WebClient webClient, String inputTestName, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectConsumerServer(server, webClient, false, inputTestName);
        if (form != null) {
            // set a name, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");

            inputText.setValueAttribute(inputTestName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }

    }

    /**
     * This will assert the connection to the consumer server.
     * This will submit the test request to the servlet client
     * com.ibm.testapp.g3store.servletConsumer.ConsumerServlet
     * to run the grpc request
     *
     * @param webClient
     * @param inputTestName
     * @param inputAppName
     * @param addAuthHeader
     * @param server
     * @return
     * @throws Exception
     */
    public static HtmlPage getConsumerResultPage(WebClient webClient, String inputTestName, String inputAppName, boolean addAuthHeader, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectConsumerServer(server, webClient, addAuthHeader, inputTestName);
        if (form != null) {
            // set a name, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");
            inputText.setValueAttribute(inputTestName);

            // set the appName
            HtmlTextInput inputApptext = (HtmlTextInput) form.getInputByName("appName");
            inputApptext.setValueAttribute(inputAppName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }
    }

    /**
     * This will assert the connection to the producer server.
     *
     * @param server
     * @param webClient
     * @param inputTestName
     * @return
     * @throws Exception
     */
    public static HtmlForm assertConnectProducerServer(LibertyServer server, WebClient webClient, String inputTestName) throws Exception {

        Log.info(c, inputTestName, inputTestName);
        // Construct the URL for the test
        URL url = GrpcTestUtils.createHttpUrl(server, "StoreProducerApp", "ProducerServlet");
        Log.info(c, inputTestName, " ------URL=[" + url + "]");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Log the page for debugging if necessary in the future.
        Log.info(c, inputTestName, page.asText());
        //Log.info(c, name.getMethodName(), page.asXml());

        assertTrue("the servlet was not loaded correctly",
                   page.asText().contains("gRPC Store Producer client"));

        return page.getFormByName("form1");
    }

    /**
     * This will assert the connection to the consumer server.
     *
     * @param server
     * @param webClient
     * @param addAuthHeader
     * @param inputTestName
     * @return
     * @throws Exception
     */
    public static HtmlForm assertConnectConsumerServer(LibertyServer server, WebClient webClient, boolean addAuthHeader, String inputTestName) throws Exception {

        Log.info(c, inputTestName, inputTestName);
        // Construct the URL for the test
        URL url = GrpcTestUtils.createHttpUrl(server, "StoreConsumerApp", "ConsumerServlet");
        Log.info(c, inputTestName, " ------URL=[" + url + "]");
        if (addAuthHeader) {
            webClient.addRequestHeader("Authorization", getAuthValue(inputTestName));
        }

        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Log the page for debugging if necessary in the future.
        Log.info(c, inputTestName, page.asText());
        //Log.info(c, name.getMethodName(), page.asXml());

        assertTrue("the servlet was not loaded correctly",
                   page.asText().contains("gRPC Store Consumer client"));

        return page.getFormByName("form1");
    }

    /**
     * @param inputTestName
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getAuthValue(String inputTestName) throws UnsupportedEncodingException {
        Log.info(c, inputTestName, ConsumerUtils.createBasicAuthHeaderValue("dev", "hello"));
        return ConsumerUtils.createBasicAuthHeaderValue("dev", "hello");
    }

    /**
     * @param testname
     * @param server
     * @throws Exception
     */
    public static void createAssertMultiApps(String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        String appName1 = "myApp1";
        String appName2 = "myApp2";
        String appName3 = "myApp3";
        String appName4 = "myApp4";
        try {
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- invoking producer servlet client to create apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "createMultiApps", server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                // but there is no order since it is server streaming, assert them individually

                assertTrue(response.contains("Store has successfully added the app [" + appName1 + "]"));
                assertTrue(response.contains("Store has successfully added the app [" + appName2 + "]"));
                assertTrue(response.contains("Store has successfully added the app [" + appName3 + "]"));
                assertTrue(response.contains("Store has successfully added the app [" + appName4 + "]"));

            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- completed producer servlet client to create apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");
            Log.info(c, testname, " ------------------------------------------------------------");

        }
    }

    /**
     * @param testname
     * @param server
     * @throws Exception
     */
    public static void deleteAssertMultiApps(String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        String appName1 = "myApp1";
        String appName2 = "myApp2";
        String appName3 = "myApp3";
        String appName4 = "myApp4";
        try {

            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- invoking producer servlet client to delete apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "deleteMultiApps", server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                assertTrue(response.contains("The app [" + appName1 + "] has been removed from the Store"));
                assertTrue(response.contains("The app [" + appName2 + "] has been removed from the Store"));
                assertTrue(response.contains("The app [" + appName3 + "] has been removed from the Store"));
                assertTrue(response.contains("The app [" + appName4 + "] has been removed from the Store"));

            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- completed producer servlet client to delete apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");
            Log.info(c, testname, " ------------------------------------------------------------");
        }

    }

    /**
     * @param appName
     * @param testname
     * @param server
     * @throws Exception
     */
    public static void createAssertMyApp(String appName, String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- invoking producer servlet client to create app: [" + appName + "]");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "createApp", appName, server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                boolean isValidResponse = response.contains("createResult");

                assertTrue(isValidResponse);
            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- completed producer servlet client to create app: [" + appName + "] ----- ");
            Log.info(c, testname, " ------------------------------------------------------------");

        }
    }

    /**
     * @param appName
     * @param testname
     * @param server
     * @throws Exception
     */
    public static void deleteAssertMyApp(String appName, String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- invoking producer servlet client to delete app: [" + appName + "] ----- ");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "deleteApp", appName, server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                boolean isValidResponse = response.contains("The app [" + appName + "] has been removed from the server");

                assertTrue(isValidResponse);
            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- completed producer servlet client to delete app: [" + appName + "] ----- ");
            Log.info(c, testname, " ------------------------------------------------------------");
        }

    }

}
