/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.test.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class PostParamsClient extends FormLoginClient {
    final static public String STORE_COOKIE = "Cookie";
    final static public String STORE_SESSION = "Session";
    final static private String POSTPARAMCOOKIE = "WASPostParam";
    final static private String WASREQURLCOOKIE = "WASReqURL";

    public PostParamsClient(String host, int port) {
        super(host, port);
    }

    public PostParamsClient(String host, int port, String servletName, String contextRoot) {
        super(host, port, servletName, contextRoot);
    }

    PostParamsClient(String host, int port, boolean isSSL, String servletName, String contextRoot) {
        super(host, port, isSSL, servletName, contextRoot);
    }

    public PostParamsClient(LibertyServer server) {
        super(server);
    }

    public PostParamsClient(LibertyServer server, String servletName, String contextRoot) {
        super(server, servletName, contextRoot);
    }

    PostParamsClient(LibertyServer server, boolean isSSL, String servletName, String contextRoot) {
        super(server, isSSL, servletName, contextRoot);
    }

    public PostParamsClient(LibertyServer server, boolean isSSL, String servletName, String contextRoot, String servletSpec, String httpProtocol) {
        super(server, isSSL, servletName, contextRoot, servletSpec, httpProtocol);
    }

    /**
     * @param reqMethod
     * @param user
     * @param password
     * @param storeLocation "Cookie" store the postparameters to a cookie, "Session" store the postparameters to the session.
     * @param expectedStatusCode
     * @param client
     * @return contents
     */
    public String accessAndAuthenticate(HttpPost postMethod, String user, String password, String storeLocation, int expectedStatusCode) {
        return accessAndAuthenticate(client, postMethod, user, password, storeLocation, expectedStatusCode, null);
    }

    /**
     * @param reqMethod
     * @param user
     * @param password
     * @param storeLocation "Cookie" store the postparameters to a cookie, "Session" store the postparameters to the session.
     * @param expectedStatusCode
     * @params addlHeader additional header valudes for the servlet.
     * @param client
     * @return contents
     */
    public String accessAndAuthenticate(HttpPost postMethod, String user, String password, String storeLocation, int expectedStatusCode, Map<String, String> addlHeaders) {
        return accessAndAuthenticate(client, postMethod, user, password, storeLocation, expectedStatusCode, addlHeaders);
    }

    /**
     * @param client
     * @param reqMethod
     * @param user
     * @param password
     * @param storeLocation "Cookie" store the postparameters to a cookie, "Session" store the postparameters to the session.
     * @param expectedStatusCode
     * @param addlHeaders the additional header values for accessing the servlet. this is used for altering the cookie.
     * @param client
     * @return contents
     */
    private String accessAndAuthenticate(HttpClient client, HttpPost postMethod,
                                         String user, String password, String storeLocation,
                                         int expectedStatusCode, Map<String, String> addlHeaders) {
        logger.info("accessAndAuthenticate: request method=" + postMethod
                    + " user=" + user + " password=" + password + " storeLocation=" + storeLocation
                    + " expectedStatusCode=" + expectedStatusCode + " addlHeaders=" + addlHeaders);

        // when POST method, accessFormLoginPage does not follow the redirect response.
        HttpResponse response = accessFormLoginPage(client, postMethod, 302);
        assertTrue("Did not find WASReqURL cookie.", CookieExists(response, WASREQURLCOOKIE));
        if (STORE_COOKIE.equals(storeLocation)) {
            // check postParam Cookie
            assertTrue("Did not find postParam cookie.", CookieExists(response, POSTPARAMCOOKIE));
        }
        // check whether the give url is form login page.
        Header header = response.getFirstHeader("Location");
        String location = header.getValue();
        logger.info("Redirect location: " + location);
        HttpGet getMethod = new HttpGet(location);
        accessFormLoginPage(client, getMethod, 200);

        // do formlogin.
        location = performFormLogin(client, postMethod.getURI().toString(), user, password, expectedStatusCode);
        if (expectedStatusCode == 401) {
            return accessLoginErrorPage(client, location);
        } else {
            return accessPageNoChallenge(client, location, expectedStatusCode, addlHeaders);
        }
    }

    private boolean CookieExists(HttpResponse response, String name) {
        boolean output = false;
        Header[] setCookieHeaders = response.getHeaders("Set-Cookie");
        if (setCookieHeaders != null) {
            for (Header header : setCookieHeaders) {
                logger.info("header: " + header);
                for (HeaderElement e : header.getElements()) {
                    if (e.getName().equals(name)) {
                        output = true;
                        break;
                    }
                }
            }
        }
        logger.info("CookieExists name : " + name + " " + output);
        return output;
    }

    /**
     * Performs a form logout,
     */
    public void formLogout() {
        logger.info("formLogou");

        try {
            // Post method to logout
            logger.info("logout URL: " + servletURL + "/ibm_security_logout");
            HttpPost postMethod = new HttpPost(servletURL + "/ibm_security_logout");
            HttpResponse response = client.execute(postMethod);
            logger.info("postMethod.getStatusCode(): " + response.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(response.getEntity());
            logger.info("Form logout getResponseBodyAsString: " + content);
            EntityUtils.consume(response.getEntity());
            // Validate that the SSO has been cleared
            setSSOCookieForLastLogin(response);
            assertEquals("", getCookieFromLastLogin());
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            failWithMessage("Caught unexpected exception: " + e);
        }

    }

}
