/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package com.ibm.ws.gvt.rest.fat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * Some HTTP convenience methods.
 */
public class GvtUtils {

    public static String performPostGvt(LibertyServer server, String endpoint,
                                        int expectedResponseStatus, String expectedResponseContentType, String user,
                                        String password, String contentType, String content) throws Exception {

        return HttpUtils.postRequest(server, endpoint, expectedResponseStatus, expectedResponseContentType, user, password, contentType, content);

    }

    public static HttpURLConnection getHttpConnectionForUTF(LibertyServer server) throws IOException {
        int timeout = 5000;
        URL url = createURL(server);
        HttpURLConnection connection = getHttpConnection(url, timeout, HTTPRequestMethod.GET);
        Log.info(HttpUtils.class, "getHttpConnection", "Connecting to " + url.toExternalForm() + " expecting http response in " + timeout + " seconds.");
        connection.connect();
        return connection;
    }

    public static HttpURLConnection getHttpConnection(URL url, int timeout, HTTPRequestMethod requestMethod) throws IOException, ProtocolException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod(requestMethod.toString());
        connection.setConnectTimeout(timeout);

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        return connection;
    }

    public static URL createURL(LibertyServer server) throws MalformedURLException {

        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort());
    }

}
