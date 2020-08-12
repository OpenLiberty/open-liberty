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
package com.ibm.testapp.g3store.utilsConsumer;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

/**
 * @author anupag
 *
 */
public class ConsumerUtils {

    static Logger _log = Logger.getLogger(ConsumerUtils.class.getName());

    public static boolean isBlank(String str) {
        boolean isBlank = false;

        if (str == null || str.trim().length() == 0) {
            isBlank = true;
        }
        return isBlank;
    }

    /**
     * @param builderId
     * @param user
     * @param pw
     * @param baseUrlStr
     * @return
     * @throws MalformedURLException
     * @throws UnsupportedEncodingException
     */
    public static WebRequest buildJwtTokenEndpointRequest(String builderId, String user, String pw, String baseUrlStr) throws MalformedURLException, UnsupportedEncodingException {

        String jwtTokenEndpoint = "jwt/ibm/api/" + "%s" + "/token";
        String jwtBuilderUrl = baseUrlStr + String.format(jwtTokenEndpoint, builderId);

        _log.info(" ----------------------------jwtBuilderUrl= " + jwtBuilderUrl);

        WebRequest request = new WebRequest(new URL(jwtBuilderUrl));
        // Token endpoint requires authentication, so provide credentials
        request.setAdditionalHeader("Authorization", createBasicAuthHeaderValue(user, pw));

        return request;
    }

    /**
     * JWT /token endpoint should return a JSON object whose only key, "token", stores the JWT built by the builder.
     */
    /**
     * @param response
     * @return
     * @throws Exception
     */
    public static String extractJwtFromTokenEndpointResponse(WebResponse response) throws Exception {
        JsonReader reader = Json.createReader(new StringReader(response.getContentAsString()));
        JsonObject jsonResponse = reader.readObject();
        return jsonResponse.getString("token");
    }

    public static String createBasicAuthHeaderValue(String username, String password) throws UnsupportedEncodingException {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    // Helper methods
    /**
     * @param webClient
     * @param request
     * @param testName
     */
    private void printRequestParts(WebClient webClient, WebRequest request, String testName) {

        if (request == null) {
            _log.info("The request is null - nothing to print");
            return;
        }
        if (webClient != null) {
            printAllCookies(webClient);
        }
        printRequestInfo(request, testName);

    }

    /**
     * @param webClient
     */
    private void printAllCookies(WebClient webClient) {
        if (webClient == null) {
            return;
        }
        CookieManager cookieManager = webClient.getCookieManager();
        Set<Cookie> cookies = cookieManager.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                _log.info("Cookie: " + cookie.getName() + " Value: " + cookie.getValue());
            }
        }
    }

    /**
     * @param request
     * @param testName
     */
    private void printRequestInfo(WebRequest request, String testName) {
        _log.info("Request URL: " + request.getUrl());
        printRequestHeaders(request, testName);
        printRequestParameters(request, testName);
        printRequestBody(request, testName);
    }

    /**
     * @param request
     * @param testName
     */
    public static void printRequestHeaders(WebRequest request, String testName) {
        Map<String, String> requestHeaders = request.getAdditionalHeaders();
        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                _log.info("Request header: " + entry.getKey() + ", set to: " + entry.getValue());
            }
        }
    }

    /**
     * @param request
     * @param testName
     */
    private void printRequestParameters(WebRequest request, String testName) {
        List<NameValuePair> requestParms = request.getRequestParameters();
        if (requestParms != null) {
            for (NameValuePair req : requestParms) {
                _log.info("Request parameter: " + req.getName() + ", set to: " + req.getValue());
            }
        }
    }

    /**
     * @param request
     * @param testName
     */
    private void printRequestBody(WebRequest request, String testName) {
        _log.info("Request body: " + request.getRequestBody());
    }

    /**
     * @param key
     * @return
     */
    public static String getSysProp(String key) {
        return AccessController.doPrivileged(
                                             (PrivilegedAction<String>) () -> System.getProperty(key));
    }

    /**
     * @return
     */
    public static String getServerConfigFolder() {

        String serverDirKey = "server.config.dir";

        String base = getSysProp(serverDirKey);

        if (base == null) {
            _log.severe("no property SERVER_CONFIG_DIR defined.");
        }

        return base;
    }

}
