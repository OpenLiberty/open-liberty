/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * This is a helper class to simplify retrieving data from the endpoints on the server
 *
 * Usage examples:
 *
 * 1. new OpenAPIConnection(server, "/air/book").header("key","123").
 * queryParam("name","Bob").download();
 *
 * 2. OpenAPIConnection.openAPIDocsConnection(server, false).queryParam("root","airlines").download()
 *
 */
public class OpenAPIConnection {
    public static final String OPEN_API_DOCS = "/openapi";
    public static final String OPEN_API_UI = "/openapi/ui";
    private int expectedResponseCode = HttpURLConnection.HTTP_OK;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();

    private HTTPRequestMethod method = HTTPRequestMethod.GET;
    private final String path;
    private boolean secure = false;
    private final LibertyServer server;
    private InputStream streamToWrite = null;
    private int port = -1;
    private String hostname = null;

    /**
     * Creates connection with default values
     *
     * @param server - server to use to construct connection
     * @param path - URI to connect to
     */
    public OpenAPIConnection(LibertyServer server, String path) {
        this.server = server;
        if (!path.startsWith("/"))
            path = "/" + path;
        this.path = path;
    }

    private String readConnection(HttpURLConnection connection) throws Exception {
        BufferedReader output = HttpUtils.getResponseBody(connection);
        StringBuilder contents = new StringBuilder();

        for (int i = 0; i != -1; i = output.read()) {
            char c = (char) i;
            if (!Character.isISOControl(c)) {
                contents.append((char) i);
            }
            if (c == '\n') {
                contents.append('\n');
            }
        }

        String urlContent = contents.toString();
        return urlContent;
    }

    /**
     * Downloads contents of URL and converts them to a string
     *
     * @return string containing contents of a url
     */
    public String download() {

        try {
            HttpURLConnection conn = getConnection();
            return readConnection(conn);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    /**
     * @return
     * @throws IOException
     * @throws ProtocolException
     */
    public HttpURLConnection getConnection() throws IOException, ProtocolException {
        HttpURLConnection conn = HttpUtils.getHttpConnection(constructUrl(), expectedResponseCode, null, 30, method, headers, streamToWrite);
        return conn;
    }

    /**
     * Set explicit HTTP response code, default is 200
     *
     * @param expectedResponseCode
     * @return
     */
    public OpenAPIConnection expectedResponseCode(int expectedResponseCode) {
        this.expectedResponseCode = expectedResponseCode;
        return this;
    }

    private URL constructUrl() {
        String protocol = this.secure ? "https" : "http";
        int port;
        if (this.port == -1)
            port = this.secure ? this.server.getHttpDefaultSecurePort() : this.server.getHttpDefaultPort();
        else
            port = this.port;
        try {
            String path = this.path;
            if (queryParams.size() > 0) {
                path += "?";
                for (Entry<String, String> queryParam : queryParams.entrySet()) {
                    path += queryParam.getKey() + "=" + queryParam.getValue() + "&";
                }
                path = path.substring(0, path.length() - 1);
            }
            String hostname = this.hostname != null ? this.hostname : this.server.getHostname();
            return new URL(protocol, hostname, port, path);
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    /**
     * Sets value for specific header
     *
     * @param headerName - header to be set
     * @param headerValue - value of the header
     * @return
     */
    public OpenAPIConnection header(String headerName, String headerValue) {
        this.headers.put(headerName, headerValue);
        return this;
    }

    /**
     *
     * @return map containing headers
     */
    public Map<String, String> headers() {
        return this.headers;
    }

    /**
     * Set explicit host to be used, by default server's host is used
     *
     * @param hostname
     * @return
     */
    public OpenAPIConnection hostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    /**
     * Set HTTP method to be used. Default is GET
     *
     * @param method
     * @return
     */
    public OpenAPIConnection method(HTTPRequestMethod method) {
        this.method = method;
        return this;
    }

    /**
     * Sets explicit port number to be used
     *
     * @param port
     * @return
     */
    public OpenAPIConnection port(int port) {
        this.port = port;
        return this;
    }

    /**
     * Set to true to use HTTPS connection, uses HTTP by default
     *
     * @param secure
     * @return
     */
    public OpenAPIConnection secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * Sets the data to be written for POST/PUT operations
     *
     * @param streamToWrite - stream to be written to the connection
     * @return
     */
    public OpenAPIConnection streamToWrite(InputStream streamToWrite) {
        this.streamToWrite = streamToWrite;
        return this;
    }

    /**
     *
     * @return list of query parameters
     */
    public Map<String, String> queryParams() {
        return this.queryParams;
    }

    public OpenAPIConnection queryParam(String paramName, String paramValue) {
        this.queryParams.put(paramName, paramValue);
        return this;
    }

    /**
     * creates default connection for OpenAPI docs endpoint
     *
     * @param server - server to connect to
     * @param secure - if true connection uses HTTPS
     * @return
     */
    public static OpenAPIConnection openAPIDocsConnection(LibertyServer server, boolean secure) {
        return new OpenAPIConnection(server, OPEN_API_DOCS).secure(secure);
    }
}
