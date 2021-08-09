/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

@RunWith(FATRunner.class)
public abstract class ErrorPageBaseTest extends CommonWebServerTests {

    @Test
    public void test404ErrorPage() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/saywhat");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, new int[0], 5, HTTPRequestMethod.GET);
        HttpUtils.findStringInHttpConnection(con, "The requested page does not exist...");
    }

    @Test
    @AllowedFFDC({ "org.springframework.web.util.NestedServletException" })
    public void testExceptioErrorPage() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/exception");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_INTERNAL_ERROR, new int[0], 5, HTTPRequestMethod.GET);
        HttpUtils.findStringInHttpConnection(con, "Exception thrown of type IllegalArgumentException");
    }

    @Test
    @AllowedFFDC({ "org.springframework.web.util.NestedServletException" })
    public void testDefaultErrorPage() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/other-exception");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_INTERNAL_ERROR, new int[0], 5, HTTPRequestMethod.GET);
        HttpUtils.findStringInHttpConnection(con, "This is a default error handling page");
    }

}