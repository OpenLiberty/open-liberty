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
public class HTTPMetricsNoContextRootTest extends HTTPMetricsAbstractTests {

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_NO_CONTEXTROOT_WAR;
    }

    @Test
    public void testNoContextRootGet() throws Exception {

        String route = "/testController/get";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootPathPost() throws Exception {

        String route = "/testController/post";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.POST.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.POST);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootPathPostPutGetJson() throws Exception {

        String postRoute = "/testController/postJson";
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
        String getRoute = "/testController/getJson/test";
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
        String putRoute = "/testController/putJson/test";
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
    public void testNoContextRootPathPut() throws Exception {

        String route = "/testController/put";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.PUT.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.PUT);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootPathDelete() throws Exception {

        String route = "/testController/delete";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.DELETE.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.DELETE);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootPathOptions() throws Exception {
        /*
         * emits this for some reason: SRVE8094W
         */
        String route = "/testController/options";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.OPTIONS.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.OPTIONS);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootPathHead() throws Exception {

        String route = "/testController/head";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.HEAD.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.HEAD);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootPathParam() throws Exception {

        String route = "/testController/parm/jolly";
        String epectedRoute = "/testController/parm/{pathVar}";
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + epectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootQueryParam() throws Exception {

        String route = "/testController/query?queryVal=123";
        String expectedRoute = "/testController/query";
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootQueryParamWrongQueryParam() throws Exception {

        String route = "/testController/query?querysquall=123";
        String expectedRoute = "/\\*";
        String responseStatus = "400";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        //Need to set Accept header to text/html, otherwise may encounter a 500 instead. Maybe JDK dependant.
        conn.setRequestProperty("Accept", "text/html");
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    //test bad path from root expect /*

    @Test
    public void testNoContextRootBadRouteGet() throws Exception {

        String route = "/testController/nonexistent";
        String expectedRoute = "/\\*";
        String responseStatus = "404";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        //Need to set Accept header to text/html, otherwise may encounter a 500 instead. Maybe JDK dependant.
        conn.setRequestProperty("Accept", "text/html");
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    /**
     * Just a quick test to check that a bad request with another request method
     * would result in a 404 an the appropriate <request_method> (Other than GET which we test above)
     * We're just going to test POST.
     */
    public void testNoContextRootBadRoutePost() throws Exception {

        String route = "/testController/nonexistent";
        String expectedRoute = "/\\*";
        String responseStatus = "404";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.POST.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(HttpUtils.createURL(server, route), HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.POST);
        //Need to set Accept header to text/html, otherwise may encounter a 500 instead. Maybe JDK dependant.
        conn.setRequestProperty("Accept", "text/html");
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    /*
     * quick JSP tests
     */

    @Test
    public void testNoContextRootControllerViewJSP() throws Exception {

        String route = "/testJSPController/testJSPPath";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootWebXMLJSP() throws Exception {

        String route = "/directJSPPath";
        String expectedRoute = route;
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }

    @Test
    public void testNoContextRootDirectHtml() throws Exception {

        String route = "/directHTML.html";
        String expectedRoute = "/\\*";
        String responseStatus = "200";
        int intResponseStatus = Integer.parseInt(responseStatus);
        String requestMethod = HTTPRequestMethod.GET.toString();

        HttpURLConnection conn = HttpUtils.getHttpConnection(server, route);
        Assert.assertTrue(String.format("Expected %d, but got %d", intResponseStatus, conn.getResponseCode()), conn.getResponseCode() == intResponseStatus);
        conn.disconnect();

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        Assert.assertTrue("Failed to find expected Mbean: " + objectName, checkMBeanRegistered(objectName));
    }
}
