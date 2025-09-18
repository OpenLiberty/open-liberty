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
        return WITH_CONTEXTROOT_WAR;
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
