/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.io.BufferedReader;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(FULL)
public class HTTPMetricsWithContextRootTest extends HTTPMetricsAbstractTests {

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_WITH_CONTEXTROOT_WAR;
    }

    @Test
    public void testWithContextRootGet() throws Exception {

        String route = CONTEXT_ROOT + "/testController/get";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextPathPost() throws Exception {

        String route = CONTEXT_ROOT + "/testController/post";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.POST.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.POST);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextPathPostPutGetJson() throws Exception {

        String postRoute = CONTEXT_ROOT + "/testController/postJson";
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);

        // Prepare JSON data
        String jsonData = "{\"message\":\"test\",\"value\":123}";

        // Step 1: POST data
        HttpURLConnection postConn = HttpUtils.getHttpConnection(
                                                                 HttpUtils.createURL(server, postRoute),
                                                                 HttpUtils.DEFAULT_TIMEOUT,
                                                                 HTTPRequestMethod.POST);

        // Set Content-Type header for JSON
        postConn.setRequestProperty("Content-Type", "application/json");

        // Write JSON data to request body
        postConn.getOutputStream().write(jsonData.getBytes("UTF-8"));
        postConn.getOutputStream().flush();

        // Verify POST response code
        Assert.assertTrue(
                          String.format("Expected %d, but got %d", intResponseStatus, postConn.getResponseCode()),
                          postConn.getResponseCode() == intResponseStatus);

        postConn.disconnect();

        // Step 2: GET data to verify it was stored
        String getRoute = CONTEXT_ROOT + "/testController/getJson/test";
        HttpURLConnection getConn = HttpUtils.getHttpConnection(server, getRoute);

        // Verify GET response code
        Assert.assertTrue(
                          String.format("Expected %d for GET, but got %d", intResponseStatus, getConn.getResponseCode()),
                          getConn.getResponseCode() == intResponseStatus);

        // Verify response contains expected data
        BufferedReader br = HttpUtils.getConnectionStream(getConn);
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        Assert.assertTrue(
                          "Response should contain 'test'",
                          response.toString().contains("test"));
        Assert.assertTrue(
                          "Response should contain value 123",
                          response.toString().contains("123"));

        getConn.disconnect();

        // Step 2: PUT to update the data to value=456
        String putRoute = CONTEXT_ROOT + "/testController/putJson/test";
        String updatedJsonData = "{\"message\":\"test\",\"value\":456}";

        HttpURLConnection putConn = HttpUtils.getHttpConnection(
                                                                HttpUtils.createURL(server, putRoute),
                                                                HttpUtils.DEFAULT_TIMEOUT,
                                                                HTTPRequestMethod.PUT);
        putConn.setRequestProperty("Content-Type", "application/json");
        putConn.getOutputStream().write(updatedJsonData.getBytes("UTF-8"));
        putConn.getOutputStream().flush();

        Assert.assertTrue(
                          String.format("Expected %d for PUT, but got %d", intResponseStatus, putConn.getResponseCode()),
                          putConn.getResponseCode() == intResponseStatus);

        // Verify PUT response contains updated data
        BufferedReader putBr = HttpUtils.getConnectionStream(putConn);
        StringBuilder putResponse = new StringBuilder();
        String putLine;
        while ((putLine = putBr.readLine()) != null) {
            putResponse.append(putLine);
        }
        Assert.assertTrue(
                          "PUT response should contain 'test'",
                          putResponse.toString().contains("test"));
        Assert.assertTrue(
                          "PUT response should contain value 456",
                          putResponse.toString().contains("456"));
        putConn.disconnect();

        // Step 3: GET again to verify the update to value=456
        HttpURLConnection getConn2 = HttpUtils.getHttpConnection(server, getRoute);

        Assert.assertTrue(
                          String.format("Expected %d for final GET, but got %d", intResponseStatus, getConn2.getResponseCode()),
                          getConn2.getResponseCode() == intResponseStatus);

        // Verify final GET response contains updated value 456
        BufferedReader getBr2 = HttpUtils.getConnectionStream(getConn2);
        StringBuilder getResponse2 = new StringBuilder();
        String getLine2;
        while ((getLine2 = getBr2.readLine()) != null) {
            getResponse2.append(getLine2);
        }
        Assert.assertTrue(
                          "Final GET response should contain 'test'",
                          getResponse2.toString().contains("test"));
        Assert.assertTrue(
                          "Final GET response should contain updated value 456",
                          getResponse2.toString().contains("456"));
        getConn2.disconnect();
    }

    @Test
    public void testWithContextPathPut() throws Exception {

        String route = CONTEXT_ROOT + "/testController/put";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.PUT.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.PUT);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextPathDelete() throws Exception {

        String route = CONTEXT_ROOT + "/testController/delete";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.DELETE.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.DELETE);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextPathOptions() throws Exception {
        /*
         * emits this for some reason: SRVE8094W
         */
        String route = CONTEXT_ROOT + "/testController/options";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.OPTIONS.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.OPTIONS);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextPathHead() throws Exception {

        String route = CONTEXT_ROOT + "/testController/head";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.HEAD.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.HEAD);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextPathParam() throws Exception {

        String route = CONTEXT_ROOT + "/testController/parm/jolly";
        String epectedRoute = CONTEXT_ROOT + "/testController/parm/{pathVar}";
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + epectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextQueryParam() throws Exception {

        String route = CONTEXT_ROOT + "/testController/query?queryVal=123";
        String expectedRoute = CONTEXT_ROOT + "/testController/query";
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextQueryParamWrongQueryParam() throws Exception {

        String route = CONTEXT_ROOT + "/testController/query?querysquall=123";
        String expectedRoute = CONTEXT_ROOT + "/\\*";
        String responseStatus = "400";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        //Need to set Accept header to text/html, otherwise may encounter a 500 instead. Maybe JDK dependant.
        conn.setRequestProperty("Accept", "text/html");
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    //test bad path from root expect /*

    @Test
    public void testWithContextBadRouteGet() throws Exception {

        String route = CONTEXT_ROOT + "/testController/nonexistent";
        String expectedRoute = CONTEXT_ROOT + "/\\*";
        String responseStatus = "404";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        //Need to set Accept header to text/html, otherwise may encounter a 500 instead. Maybe JDK dependant.
        conn.setRequestProperty("Accept", "text/html");
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    /**
     * Just a quick test to check that a bad request with another request method
     * would result in a 404 an the appropriate <request_method> (Other than GET which we test above)
     * We're just going to test POST.
     */
    public void testWithContextBadRoutePost() throws Exception {

        String route = CONTEXT_ROOT + "/testController/nonexistent";
        String expectedRoute = CONTEXT_ROOT + "/\\*";
        String responseStatus = "404";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.POST.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.POST);
        //Need to set Accept header to text/html, otherwise may encounter a 500 instead. Maybe JDK dependant.
        conn.setRequestProperty("Accept", "text/html");
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextRootBadContextRoot() throws Exception {

        /*
         * Since Spring has established that it needs to serve a context root
         * a "bad" path from the server would result in a 404.
         * The request is sent to the OpenLiberty server over springboot and this 404 is handled as `/`
         */

        String route = "/beepBoop";
        String expectedRoute = "/";
        String responseStatus = "404";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    /*
     * quick JSP tests
     */

    @Test
    public void testWithContextContextRootControllerViewJSP() throws Exception {

        String route = CONTEXT_ROOT + "/testJSPController/testJSPPath";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextContextRootWebXMLJSP() throws Exception {

        String route = CONTEXT_ROOT + "/directJSPPath";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }

    @Test
    public void testWithContextContextRootDirectHtml() throws Exception {

        String route = CONTEXT_ROOT + "/directHTML.html";
        String expectedRoute = CONTEXT_ROOT + "/\\*";
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName, true));
    }
}
