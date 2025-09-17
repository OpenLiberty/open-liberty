/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package componenttest.topology.utils;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class HttpRequest {

    private static final Class<?> c = HttpRequest.class;

    private final String url;
    private final Set<Integer> expectedResponseCode = new HashSet<Integer>();
    private String reqMethod = "GET";
    private String json = null;
    private String basicAuth = null;
    private final Map<String, String> props = new HashMap<String, String>();
    private Integer timeout;
    private boolean silent = false;
    private int responseCode = -1;

    private static String concat(String... pathParts) {
        String base = "";
        for (String part : pathParts)
            base += part;
        return base;
    }

    public HttpRequest(String url) {
        this.url = url;
    }

    public HttpRequest(LibertyServer server, String... pathParts) {
        this("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + concat(pathParts));
    }

    /**
     * The HTTP request method. Default method is GET.
     */
    public HttpRequest method(String method) {
        this.reqMethod = method;
        return this;
    }

    /**
     * Add a HTTP request property name and value using HttpUrlConnection.setRequestProperty()
     */
    public HttpRequest requestProp(String key, String value) {
        props.put(key, value);
        return this;
    }

    /**
     * Set the expected response code. Default is HTTP_OK
     */
    public HttpRequest expectCode(int expectedResponse) {
        this.expectedResponseCode.add(expectedResponse);

        return this;
    }

    /**
     * Set the json data to send with the request.
     */
    public HttpRequest jsonBody(String json) {
        this.json = json;
        return this;
    }

    public HttpRequest timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public HttpRequest silent() {
        this.silent = true;
        return this;
    }

    public HttpRequest basicAuth(String user, String pass) {
        String userPass = user + ':' + pass;
        String base64Auth = Base64.getEncoder().encodeToString((userPass).getBytes(StandardCharsets.UTF_8));
        this.basicAuth = "Basic " + base64Auth;
        return this;
    }

    /**
     * Make an HTTP request and receive the response as the specified type.
     * The following types are valid parameters:
     * <ul>
     * <li>java.lang.String</li>
     * <li>javax.json.JsonArray</li>
     * <li>javax.json.JsonObject</li>
     * <li>javax.json.JsonStructure</li>
     * </ul>
     */
    public <T> T run(Class<T> type) throws Exception {
        if (!silent) {
            Log.info(c, "run", reqMethod + ' ' + url);
        }

        /*
         * We use Apache HTTP client since java.net URL/URLConnection code is not isolated enough
         * for testing. For example, some code overrides the URLStreamHandlerFactory when used
         * as a client and that makes it so that the URLConnections returned are the HttpsURLConnectionOldImpl
         * which is not the expected HttpsUrlConnection.
         */

        /*
         * Set timeouts for the request and then create the HTTP client.
         */
        HttpConnectionManager cm = new MultiThreadedHttpConnectionManager();
        if (timeout != null) {
            cm.getParams().setConnectionTimeout(timeout);
            cm.getParams().setSoTimeout(timeout);
        }
        HttpClient httpClient = new HttpClient(cm);
        configureClient(httpClient);

        /*
         * Create a request based on the request method specified.
         */
        HttpMethod request = null;
        switch (reqMethod) {
            case "POST":
                request = new PostMethod(url);
                break;
            case "HEAD":
                request = new HeadMethod(url);
                break;
            case "OPTIONS":
                request = new OptionsMethod(url);
                break;
            case "PUT":
                request = new PutMethod(url);
                break;
            case "DELETE":
                request = new DeleteMethod(url);
                break;
            case "TRACE":
                request = new TraceMethod(url);
                break;
            case "GET":
            default:
                if (json != null) {
                    throw new IllegalStateException("Cannot send a payload on a GET request.");
                }
                request = new GetMethod(url);
                break;
        }

        try {
            /*
             * Add the payload.
             */
            if (json != null) {
                request.setRequestHeader("Content-Type", "application/json");
                StringRequestEntity stringEntity = new StringRequestEntity(json, "application/json", StandardCharsets.UTF_8.toString());
                ((EntityEnclosingMethod) request).setRequestEntity(stringEntity);
            } else {
                request.setRequestHeader("Content-Type", "text/html");
            }

            if (type.getPackage().toString().startsWith("javax.json")) {
                request.setRequestHeader("Accept", "application/json");
            }

            /*
             * Add basic auth header.
             */
            if (basicAuth != null) {
                request.setRequestHeader("Authorization", basicAuth);
            }

            /*
             * Add any additional headers.
             */
            if (props != null) {
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    request.addRequestHeader(entry.getKey(), entry.getValue());
                }
            }

            /*
             * Set the expected response code if one is not already set.
             */
            if (expectedResponseCode.isEmpty()) {
                expectCode(HttpURLConnection.HTTP_OK);
            }

            /*
             * Send the request.
             */
            if (!silent) {
                Log.info(c, "run", "Sending HTTP Request: " + request.getName() + " " + url);
            }
            httpClient.executeMethod(request);

            /*
             * Check for the expected response code.
             */
            responseCode = request.getStatusCode();
            if (!silent) {
                Log.info(c, "run", "Received HTTP Response:  " + responseCode + " " + request.getStatusLine());
            }
            if (!expectedResponseCode.contains(responseCode)) {
                Log.info(c, "run", "Got unexpected response code: " + responseCode);
                String responseBody = request.getResponseBodyAsString();
                printResponseContents(responseBody);
                throw new Exception("Unexpected response: " + responseCode + "\nResponse Body: " + responseBody);
            }

            String responseBody = request.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                printResponseContents(responseBody);
                if (JsonArray.class.equals(type)) {
                    return type.cast(Json.createReader(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8))).readArray());
                } else if (JsonObject.class.equals(type)) {
                    return type.cast(Json.createReader(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8))).readObject());
                } else if (JsonStructure.class.equals(type)) {
                    return type.cast(Json.createReader(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8))).read());
                } else if (String.class.equals(type)) {
                    return type.cast(responseBody);
                } else {
                    throw new IllegalArgumentException(type.getName());
                }
            } else {
                return null;
            }
        } finally {
            request.releaseConnection();
        }
    }

    void configureClient(HttpClient httpClient) {}

    public int getResponseCode() {
        return responseCode;
    }

    private void printResponseContents(String contents) {
        final int charsToPrint = 500;
        StringBuffer sb = new StringBuffer();
        if (contents.length() > charsToPrint) {
            sb.append(contents.substring(0, charsToPrint));
            sb.append("<<<" + (contents.length() - charsToPrint) + " ADDITIONAL BYTES REDACTED>>>\n");
        } else {
            sb.append(contents);
        }
        Log.info(c, "run", "Received HTTP Response contents:  \n" + sb.toString());
    }
}
