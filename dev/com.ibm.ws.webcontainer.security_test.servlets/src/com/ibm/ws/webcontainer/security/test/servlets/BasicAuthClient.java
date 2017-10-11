/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import componenttest.topology.impl.LibertyServer;

/**
 * Provide common functionality for accessing the BasicAuth servlet.
 */
public class BasicAuthClient extends ServletClientImpl {
    private static final Class<?> c = BasicAuthClient.class;
    public static final String DEFAULT_REALM = "Basic Authentication";
    public static final String DEFAULT_SERVLET_NAME = "ServletName: BasicAuthServlet";
    public static final String DEFAULT_JSP_NAME = "JSPName: BasicAuthJSP.jsp";
    public static final String DEFAULT_CONTEXT_ROOT = "/basicauth";
    public static final String SPNEGO_DEFAULT_CONTEXT_ROOT = "/spnegoAuth";
    public static final String DEFAULT_JSP_CONTEXT_ROOT = "/basicauth/JSP";
    private final String realm;
    private final String servletName;
    protected boolean retryMode = false;

    /**
     * Constructs a client to connect to the default servlet (BasicAuthServlet)
     * with the given host and port. The realm, servlet name and context root
     * are defaulted.
     *
     * @param host
     * @param port
     */
    public BasicAuthClient(String host, int port) {
        this(host, port, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    /**
     * Constructs a client to connect to the default servlet (BasicAuthServlet)
     * with the given host, port and realm configuration.
     *
     * @param host
     * @param port
     * @param realm
     */
    public BasicAuthClient(String host, int port, String realm) {
        this(host, port, realm, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public BasicAuthClient(String host, int port, String realm,
                           String servletName, String contextRoot) {
        this(host, port, false, realm, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);
    }

    BasicAuthClient(String host, int port, boolean isSecure, String realm,
                    String servletName, String contextRoot) {
        super(host, port, isSecure, contextRoot);
        this.realm = realm;
        this.servletName = servletName;
        authType = "BASIC";
    }

    public BasicAuthClient(LibertyServer server) {
        this(server, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public BasicAuthClient(LibertyServer server, String realm) {
        this(server, realm, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public BasicAuthClient(LibertyServer server, String realm,
                           String servletName, String contextRoot) {
        this(server, false, realm, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);
    }

    BasicAuthClient(LibertyServer server, boolean isSSL, String realm,
                    String servletName, String contextRoot) {
        super(server, isSSL, contextRoot);
        this.realm = realm;
        this.servletName = servletName;
        authType = "BASIC";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookResetClientState() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public String access(String url, int expectedStatusCode) {
        logger.info("access: url=" + url +
                    " expectedStatusCode=" + expectedStatusCode);

        try {
            HttpGet getMethod = new HttpGet(url);
            return executeAndProcessGetMethod(getMethod, expectedStatusCode, null);
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String accessWithException(String url, Class<?> expectedException) {
        logger.info("access: url=" + url +
                    " expectedException=" + expectedException);

        try {
            HttpGet getMethod = new HttpGet(url);
            executeAndProcessGetMethod(getMethod, null, null);
            failWithMessage("Didn't catch expected exception: " + expectedException);
            return null;
        } catch (Exception e) {
            if (e.getClass().equals(expectedException)) {
                return e.getMessage();
            } else {
                failWithMessage("Caught unexpected exception: " + e);
                return null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String accessAndAuthenticate(String url, String user,
                                           String password, int expectedStatusCode) {
        logger.info("accessAndAuthenticate: url=" + url +
                    " user=" + user + " password=" + password +
                    " expectedStatusCode=" + expectedStatusCode);

        try {
            if (user != null) {
                client.getCredentialsProvider().setCredentials(new AuthScope(host, AuthScope.ANY_PORT, realm),
                                                               new UsernamePasswordCredentials(user, password));
            }
            HttpGet getMethod = new HttpGet(url);
            return executeAndProcessGetMethod(getMethod, expectedStatusCode, true);
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String accessAndAuthenticateForError500(String url, String user,
                                                      String password, int expectedStatusCode) {
        logger.info("accessAndAuthenticateForError500: url=" + url +
                    " user=" + user + " password=" + password +
                    " expectedStatusCode=" + expectedStatusCode);

        try {
            if (user != null) {
                client.getCredentialsProvider().setCredentials(new AuthScope(host, AuthScope.ANY_PORT, realm),
                                                               new UsernamePasswordCredentials(user, password));
            }
            HttpGet getMethod = new HttpGet(url);
            return executeAndProcessGetMethod(getMethod, expectedStatusCode, false);
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String accessAndAuthenticateForExpectedInternalError(String url, String user,
                                                                   String password) {
        logger.info("accessAndAuthenticate: url=" + url +
                    " user=" + user + " password=" + password);

        try {
            if (user != null) {
                client.getCredentialsProvider().setCredentials(new AuthScope(host, AuthScope.ANY_PORT, realm),
                                                               new UsernamePasswordCredentials(user, password));
            }
            HttpGet getMethod = new HttpGet(url);
            return executeAndProcessGetMethod(getMethod, 403, false);
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String accessWithCookie(String url, String cookie,
                                      int expectedStatusCode) {
        logger.info("accessWithCookie: url=" + url +
                    " cookie=" + cookie +
                    " expectedStatusCode=" + expectedStatusCode);

        Map<String, String> cookieHeader = new HashMap<String, String>();
        cookieHeader.put("Cookie", ssoCookieName + "=" + cookie);
        return accessWithHeaders(url, expectedStatusCode, cookieHeader, true);
    }

    @Override
    public String accessWithHeaders(String url, int expectedStatusCode, Map<String, String> headers, Boolean ignoreErrorContent, Boolean handleSSOCookie) {
        return accessWithHeaders(url, expectedStatusCode, headers, ignoreErrorContent, false, handleSSOCookie);
    }

    public String accessWithHeaders(String url, int expectedStatusCode, Map<String, String> headers, Boolean ignoreErrorContent, Boolean dumpAuthHeader, Boolean handleSSOCookie) {
        logger.info("accessWithHeaders: url=" + url + " expectedStatusCode=" + expectedStatusCode);

        try {
            HttpGet getMethod = new HttpGet(url);
            if (headers != null) {
                Set<String> headerKeys = headers.keySet();
                StringBuilder headerStringBuilder = new StringBuilder();
                headerStringBuilder.append("[");
                for (String header : headerKeys) {
                    getMethod.setHeader(header, headers.get(header));
                    headerStringBuilder.append(header + ": " + headers.get(header) + ", ");
                }
                headerStringBuilder = headerStringBuilder.delete(headerStringBuilder.lastIndexOf(","), headerStringBuilder.length()).append("]");
                logger.info("accessWithHeaders: headers=" + headerStringBuilder.toString());
            }
            return executeAndProcessGetMethod(getMethod, expectedStatusCode, handleSSOCookie, ignoreErrorContent, dumpAuthHeader);
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }

    }

    /**
     * Do all of the common steps for handling the getMethod, such as validating
     * the expected status code. This method will return null if a non-200 status
     * code is found.
     *
     * @param getMethod
     * @param expectedStatusCode
     * @return The response text, or null if not authorized
     * @throws IOException
     */
    private String executeAndProcessGetMethod(HttpGet getMethod, Integer expectedStatusCode, Boolean handleSSOCookie) throws IOException {
        return executeAndProcessGetMethod(getMethod, expectedStatusCode, handleSSOCookie, true);
    }

    /**
     * Do all of the common steps for handling the getMethod, such as validating
     * the expected status code.
     *
     * @param getMethod
     * @param expectedStatusCode
     * @param handleSSOCookie
     * @param ignoreErrorContent
     * @return The response text. If {@code ignoreErrorContent} is true, null is returned if not authorized
     * @throws IOException
     */
    private String executeAndProcessGetMethod(HttpGet getMethod, Integer expectedStatusCode, Boolean handleSSOCookie, Boolean ignoreErrorContent) throws IOException {
        return executeAndProcessGetMethod(getMethod, expectedStatusCode, handleSSOCookie, ignoreErrorContent, false);
    }

    /**
     * Do all of the common steps for handling the getMethod, such as validating
     * the expected status code.
     *
     * @param getMethod
     * @param expectedStatusCode
     * @param handleSSOCookie
     * @param ignoreErrorContent
     * @return The response text. If {@code ignoreErrorContent} is true, null is returned if not authorized
     * @throws IOException
     */
    private String executeAndProcessGetMethod(HttpGet getMethod, Integer expectedStatusCode, Boolean handleSSOCookie, Boolean ignoreErrorContent,
                                              Boolean dumpAuthHeader) throws IOException {
        HttpResponse response = client.execute(getMethod);
        String content = getEntityContent(response);
        int statusCode = response.getStatusLine().getStatusCode();
        if (retryMode) {
            if (statusCode == 404 && (expectedStatusCode == null || expectedStatusCode != 404)) {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                }
                logger.info("Retry servlet access ");
                response = client.execute(getMethod);
                content = getEntityContent(response);
                statusCode = response.getStatusLine().getStatusCode();
            }
        }
        if (expectedStatusCode == null) {
            return null;
        }

        assertEquals("Expected " + expectedStatusCode + " was not returned",
                     expectedStatusCode.intValue(), statusCode);

        // Paranoia check, make sure we hit the right servlet
        if (statusCode == 200) {
            assertTrue("Response did not contain expected servlet name (" + servletName + ")", content.contains(servletName));
        } else if (ignoreErrorContent) {
            content = null;
        }
        if (handleSSOCookie != null) {
            if (handleSSOCookie && expectedStatusCode != 401) {
                setSSOCookieForLastLogin(response);
            } else {
                validateNoSSOCookie(response);
            }
        }
        if (dumpAuthHeader) {
            getAuthHeader(response);
        }
        return content;
    }

    /**
     * Validate there is an Authenticate Header in the response, and check if
     * set to Negotiate.
     *
     * @param postMethod
     */
    protected void getAuthHeader(HttpMessage httpMessage) {
        logger.info("getAuthHeader");
        Header[] setHeaders = httpMessage.getHeaders("WWW-Authenticate");
        if (setHeaders == null) {
            failWithMessage("setHeaders was null and should not be");
        }
        for (Header header : setHeaders) {
            logger.info("header: " + header);
            for (HeaderElement e : header.getElements()) {
                if (e.getName().contains("Negotiate")) {
                    return;
                }
            }
        }
        fail("Get-Header for WWW-Authenticate was not found in the Header");
    }

    private String getEntityContent(HttpResponse response) throws IOException {
        logger.info("getMethod status: " + response.getStatusLine());
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity);
        logger.info("Servlet response: " + content);
        EntityUtils.consume(entity);
        return content;
    }
}
