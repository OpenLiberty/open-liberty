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
package com.ibm.ws.jaxrs21.client.fat.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public class JAXRS21AbstractTest {

    private final static int REQUEST_TIMEOUT = 10;

    protected LibertyServer serverRef;

    protected void runTestOnServer(String target, String testMethod, Map<String, String> params, String expectedResponse) throws ProtocolException, MalformedURLException, IOException {

        // build basic URI
        StringBuilder sBuilder = new StringBuilder("http://").append(serverRef.getHostname())
                        .append(":")
                        .append(serverRef.getHttpDefaultPort())
                        .append("/")
                        .append(target)
                        .append("?test=")
                        .append(testMethod);

        // add params to URI
        if (params != null && params.size() > 0) {

            StringBuilder paramStr = new StringBuilder();

            Iterator<String> itr = params.keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                paramStr.append("&@" + key + "=" + params.get(key));
            }

            sBuilder.append(paramStr.toString());
        }

        sBuilder.append("&@secport=" + serverRef.getHttpDefaultSecurePort());
        sBuilder.append("&@hostname=" + serverRef.getHostname());

        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testMethod, "Calling ClientTestApp with URL=" + urlStr);

        HttpURLConnection con;

        con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String firstLine = br.readLine();
        Log.info(this.getClass(), testMethod, "The response: " + firstLine);
        String line;
        while ((line = br.readLine()) != null) {
            Log.info(this.getClass(), testMethod, line);
        }

        assertTrue("Real response is " + firstLine + " and the expected response is " + expectedResponse, firstLine.contains(expectedResponse));

    }
}
