/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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

package com.ibm.ws.security.context_fat;
import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 *
 */
public class HttpHelper {

    /**
     * @throws IOException
     * @throws ClientProtocolException
     * 
     */
    public String get(String url) throws ClientProtocolException, IOException {
        return execute(new HttpGet(url));
    }

    /**
     * Execute the httpRequest and return the response content.
     * 
     * @return the response content
     */
    protected String execute(HttpUriRequest httpRequest) throws IOException {

        CloseableHttpClient httpclient = HttpClients.custom().build();
        try {
            log("execute: Executing request " + httpRequest.getRequestLine());
            return getResponseContent(httpclient.execute(httpRequest));
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            httpclient.close();
        }
    }

    /**
     * Note: The response object is closed by this method.
     * 
     * @return the response content as a string.
     * @throws IOException
     * @throws ParseException
     */
    public String getResponseContent(CloseableHttpResponse response) throws ParseException, IOException {

        try {
            return EntityUtils.toString(response.getEntity());
        } finally {
            response.close();
        }
    }

    /**
    *
    */
    private static void log(Object obj) {
        System.out.println("HttpHelper: " + String.valueOf(obj));
    }

}

