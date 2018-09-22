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
package com.ibm.ws.microprofile.metrics.fat.utils;

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

import com.ibm.ws.common.internal.encoder.Base64Coder;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 *
 */
public class MetricsConnection {

    public static final String METRICS_ENDPOINT = "/metrics/";
    public static final String ADMINISTRATOR_USERNAME = "admin";
    public static final String ADMINISTRATOR_PASSWORD = "adminpwd";
    public static final String VIEWER_USERNAME = "viewer";
    public static final String VIEWER_PASSWORD = "viewerpwd";
    public static final String UNAUTHORIZED_USER_USERNAME = "user";
    public static final String UNAUTHORIZED_USER_PASSWORD = "userpwd";
    public static final String INVALID_USER_USERNAME = "idontexist";
    public static final String INVALID_USER_PASSWORD = "idontexistpwd";

    private int expectedResponseCode = HttpURLConnection.HTTP_OK;
    private final int[] allowedUnexpectedResponseCodes = new int[] { HttpURLConnection.HTTP_MOVED_TEMP };
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private InputStream streamToWrite = null;

    private HTTPRequestMethod method = HTTPRequestMethod.GET;
    private final String path;
    private boolean secure = false;
    private final LibertyServer server;
    private int port = -1;
    private String hostname = null;

    /**
     * Creates connection with default values
     *
     * @param server - server to use to construct connection
     * @param path - URI to connect to
     */
    public MetricsConnection(LibertyServer server, String path) {
        this.server = server;
        if (!path.startsWith("/"))
            path = "/" + path;
        this.path = path;
    }

    /**
     * Set explicit HTTP response code, default is 200
     *
     * @param expectedResponseCode
     * @return
     */
    public MetricsConnection expectedResponseCode(int expectedResponseCode) {
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

    /*
     * @return
     *
     * @throws IOException
     *
     * @throws ProtocolException
     */
    public HttpURLConnection getConnection() throws IOException, ProtocolException {
        HttpURLConnection conn = HttpUtils.getHttpConnection(constructUrl(), expectedResponseCode, allowedUnexpectedResponseCodes, 30, method, headers, streamToWrite);
        return conn;
    }

    /**
     * Sets the data to be written for POST/PUT operations
     *
     * @param streamToWrite - stream to be written to the connection
     * @return
     */
    public MetricsConnection streamToWrite(InputStream streamToWrite) {
        this.streamToWrite = streamToWrite;
        return this;
    }

    /**
     * Sets value for specific header
     *
     * @param headerName - header to be set
     * @param headerValue - value of the header
     * @return
     */
    public MetricsConnection header(String headerName, String headerValue) {
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
    public MetricsConnection hostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    /**
     * Set HTTP method to be used. Default is GET
     *
     * @param method
     * @return
     */
    public MetricsConnection method(HTTPRequestMethod method) {
        this.method = method;
        return this;
    }

    /**
     * Sets explicit port number to be used
     *
     * @param port
     * @return
     */
    public MetricsConnection port(int port) {
        this.port = port;
        return this;
    }

    /**
     * Set to true to use HTTPS connection, uses HTTP by default
     *
     * @param secure
     * @return
     */
    public MetricsConnection secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    /**
     * Creates a connection for private (authorized) docs endpoint using HTTPS
     * and the Administrator role
     *
     * @param server - server to connect to
     * @return
     */
    public static MetricsConnection connection_administratorRole(LibertyServer server) {
        return new MetricsConnection(server, METRICS_ENDPOINT).secure(true)
                        .header("Authorization",
                                "Basic " + Base64Coder.base64Encode(ADMINISTRATOR_USERNAME + ":" + ADMINISTRATOR_PASSWORD));
    }

    /**
     * Creates a connection for private (authorized) docs endpoint using HTTPS
     * and the Viewer role
     *
     * @param server - server to connect to
     * @return
     */
    public static MetricsConnection connection_viewerRole(LibertyServer server) {
        return new MetricsConnection(server, METRICS_ENDPOINT).secure(true)
                        .header("Authorization",
                                "Basic " + Base64Coder.base64Encode(VIEWER_USERNAME + ":" + VIEWER_PASSWORD));
    }

    /**
     * Creates a connection for private (authorized) docs endpoint using HTTPS
     * and an unauthorized user ID
     *
     * @param server - server to connect to
     * @return
     */
    public static MetricsConnection connection_unauthorized(LibertyServer server) {
        return new MetricsConnection(server, METRICS_ENDPOINT).secure(true)
                        .header("Authorization",
                                "Basic " + Base64Coder
                                                .base64Encode(UNAUTHORIZED_USER_USERNAME + ":" + UNAUTHORIZED_USER_PASSWORD));
    }

    /**
     * Creates a connection for private (authorized) docs endpoint using HTTPS
     * and an invalid user ID
     *
     * @param server - server to connect to
     * @return
     */
    public static MetricsConnection connection_invalidUser(LibertyServer server) {
        return new MetricsConnection(server, METRICS_ENDPOINT).secure(true)
                        .header("Authorization",
                                "Basic " + Base64Coder.base64Encode(INVALID_USER_USERNAME + ":" + INVALID_USER_PASSWORD));
    }

    /**
     * Creates a connection for public (unauthenticated) docs endpoint using HTTP
     *
     * @param server - server to connect to
     * @return
     */
    public static MetricsConnection connection_unauthenticated(LibertyServer server) {
        return new MetricsConnection(server, METRICS_ENDPOINT);
    }

}
