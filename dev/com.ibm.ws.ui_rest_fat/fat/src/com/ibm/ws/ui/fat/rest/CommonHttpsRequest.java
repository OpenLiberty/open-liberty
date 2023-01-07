/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import javax.ws.rs.core.MediaType;

import componenttest.topology.utils.HttpsRequest;
import componenttest.topology.impl.LibertyServer;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This is a subclass of httpsRequest in simplicity to include support of additional 
 * response body types. It also has additional methods to get the HttpURLConnection
 * request, the response body, the response code, and the response headers.
 */
public class CommonHttpsRequest extends HttpsRequest {
    private final Class<?> c = CommonHttpsRequest.class;

    private final String requestUrl;
    private final Set<Integer> requestExpectedResponseCode = new HashSet<Integer>();
    private String requestReqMethod = "GET";
    private String requestJson = null;
    private String requestPlainText = null;
    private String requestBasicAuth = null;
    private final Map<String, String> requestProps = new HashMap<String, String>();
    private Integer requestTimeout;
    private boolean requestSilent = false;
    private int connectionResponseCode = -1;
    private HttpURLConnection con = null;
    private Object requestResponseBody = null;

    public CommonHttpsRequest(LibertyServer server, String url) {
        super(server, url);
        this.requestUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + url;
    }

    @Override
    public CommonHttpsRequest method(String method) {
        requestReqMethod = method;
        return (CommonHttpsRequest) this;
    }

    @Override
    public CommonHttpsRequest basicAuth(String user, String pass) {
        try {
            String userPass = user + ':' + pass;
            String base64Auth = Base64.getEncoder().encodeToString((userPass).getBytes("UTF-8"));
            this.requestBasicAuth = "Basic " + base64Auth;
        } catch (UnsupportedEncodingException e) {
            // nothing to be done
        }
        return this;
    }

    @Override
    public CommonHttpsRequest expectCode(int expectedResponse) {
        this.requestExpectedResponseCode.add(expectedResponse);
        return this;
    }

    @Override
    public CommonHttpsRequest jsonBody(String json) {
        this.requestJson = json;
        return this;
    }

    @Override
    public CommonHttpsRequest requestProp(String key, String value) {
        this.requestProps.put(key, value);
        return this;
    }

    @Override
    public CommonHttpsRequest silent() {
        this.requestSilent = true;
        return this;
    }

    @Override
    public CommonHttpsRequest timeout(int timeout) {
        this.requestTimeout = timeout;
        return this;
    }

    @Override
    public int getResponseCode() {
        return connectionResponseCode;
    }

    /*
     * Set the payload for POST/PUT request in plain text format.
     */
    public CommonHttpsRequest plainTextBody(String body) {
        this.requestPlainText = body;
        return this;
    }

    /*
     * @return the HttpURLConnection created for the request
     */
    public HttpURLConnection getConnection() {
        return this.con;
    }

    /*
     * @return the response
     */
    public Object getResponseBody() {
        return this.requestResponseBody;
    }

    /*
     * @return the response headers
     */
    public Map<String,List<String>> getResponseHeaders() {
        return this.con.getHeaderFields();
    }

    @Override
    public <T> T run(Class<T> type) throws Exception {
        if (!this.requestSilent) {
            Log.info(c, "run", requestReqMethod + ' ' + this.requestUrl);
        }

        con = (HttpURLConnection) new URL(this.requestUrl).openConnection();

        try {
            // configureConnection(con);

            con.setRequestMethod(this.requestReqMethod);
            if (this.requestReqMethod == "GET") {
                con.setDoInput(true);
            } else {
                con.setDoOutput(true);
            }

            if ("GET".equals(con.getRequestMethod()) && this.requestJson != null) {
                throw new IllegalStateException("Writing a JSON body to a GET request will force the connection to be switched to a POST request at the JDK layer.");
            }


            if ((type.getPackage() != null) && type.getPackage().toString().startsWith("javax.json"))
                con.setRequestProperty("Accept", "application/json");

            if (this.requestBasicAuth != null)
                con.setRequestProperty("Authorization", this.requestBasicAuth);

            if (this.requestProps != null)
                for (Map.Entry<String, String> entry : this.requestProps.entrySet())
                    con.setRequestProperty(entry.getKey(), entry.getValue());

            if (this.requestTimeout != null) {
                con.setConnectTimeout(this.requestTimeout);
                con.setReadTimeout(this.requestTimeout);
            }

            // All changes to the query have to make first before the call con.getOutputStream, otherwise
            // it will throw the following exception:
            //   java.lang.IllegalStateException: Already connected 
            if (this.requestJson != null) {
                con.setRequestProperty("Content-Type", "application/json");
                OutputStream out = con.getOutputStream(); //This line will change a GET request to a POST request
                out.write(this.requestJson.getBytes("UTF-8"));
                out.close();
            } else if (this.requestPlainText != null) {
                con.setRequestProperty("Content-Type", MediaType.TEXT_PLAIN);
                OutputStream out = con.getOutputStream(); //This line will change a GET request to a POST request
                out.write(this.requestPlainText.getBytes("UTF-8"));
                out.close();
            } else {
                con.setRequestProperty("Content-Type", "text/html");
            }

            connectionResponseCode = con.getResponseCode();

            if ((!this.requestExpectedResponseCode.isEmpty()) && (!this.requestExpectedResponseCode.contains(connectionResponseCode))) {
                Log.info(c, "run", "Got unexpected response code: " + connectionResponseCode);
                throw new Exception("Unexpected response (See HTTP_* constant values on HttpURLConnection): " + connectionResponseCode);
            }
            
            Object response = null;
            if (connectionResponseCode / 100 == 2) { // response codes in the 200s mean success
                if (JsonArray.class.equals(type))
                    response = Json.createReader(con.getInputStream()).readArray();
                else if (JsonObject.class.equals(type))
                    response = Json.createReader(con.getInputStream()).readObject();
                else if (JsonStructure.class.equals(type))
                    response = Json.createReader(con.getInputStream()).read();
                else if (String.class.equals(type)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    InputStream in = con.getInputStream();
                    int numBytesRead;
                    for (byte[] b = new byte[8192]; (numBytesRead = in.read(b)) != -1;)
                        out.write(b, 0, numBytesRead);
                    in.close();
                    response = out.toString("UTF-8");
                } else if (byte[].class.equals(type)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    InputStream in = con.getInputStream();
                    int numBytesRead;
                    for (byte[] b = new byte[8192]; (numBytesRead = in.read(b)) != -1;)
                        out.write(b, 0, numBytesRead);
                    in.close();
                    response = out.toByteArray();
                } else
                    throw new IllegalArgumentException(type.getName());
                requestResponseBody = type.cast(response);
                return type.cast(response);
            } else if (con.getErrorStream() != null) {
                if (JsonArray.class.equals(type))
                    response = Json.createReader(con.getErrorStream()).readArray();
                else if (JsonObject.class.equals(type))
                    response = Json.createReader(con.getErrorStream()).readObject();
                else if (JsonStructure.class.equals(type))
                    response = Json.createReader(con.getErrorStream()).read();
                else if (String.class.equals(type)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    InputStream in = con.getErrorStream();
                    int numBytesRead;
                    for (byte[] b = new byte[8192]; (numBytesRead = in.read(b)) != -1;)
                        out.write(b, 0, numBytesRead);
                    in.close();
                    // requestResponseBody = type.cast(out.toString("UTF-8"));
                    response = out.toString("UTF-8");
                } else
                    throw new IllegalArgumentException(type.getName());
                requestResponseBody = type.cast(response);
                return type.cast(response);
            } else {
                return null;
            }
        } finally {
            con.disconnect();
        }
    }
}
