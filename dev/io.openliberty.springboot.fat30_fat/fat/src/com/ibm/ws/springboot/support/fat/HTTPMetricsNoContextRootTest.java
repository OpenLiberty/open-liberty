/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class HTTPMetricsNoContextRootTest extends HTTPMetricsAbstractTests {

    @Override
    public String getApplication() {
        return NO_CONTEXTROOT_WAR;
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
