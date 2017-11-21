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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;

import componenttest.topology.impl.LibertyServer;

/**
 * Common servlet client functionality
 */
public abstract class ServletClientImpl implements ServletClient {
    private static final String AUTH_TYPE_HEADER = "getAuthType: ";
    private static final String REMOTE_USER_HEADER = "getRemoteUser: ";
    private static final String USER_PRINCIPAL_HEADER = "getUserPrincipal: ";
    private static final String USER_PRINCIPAL_NAME_HEADER = "getUserPrincipal().getName(): ";
    private static final String WSPRINCIPAL = "WSPrincipal:";
    private static final String EMPLOYEE_ROLE_HEADER = "isUserInRole(Employee): ";
    private static final String MANAGER_ROLE_HEADER = "isUserInRole(Manager): ";

    protected final String contextRoot;
    protected final String host;
    protected final int port;
    protected final String servletURL;
    protected String authType;
    protected Logger logger;
    protected LibertyServer server = null;
    protected String ssoCookieName = "LtpaToken2";
    protected String ssoCookie;
    protected DefaultHttpClient client;

    protected ServletClientImpl(String host, int port, boolean isSSL, String contextRoot) {
        if (host == null || port == 0) {
            throw new IllegalArgumentException("Host (" + host
                                               + "is null or port (" + port + ") is zero");
        }
        this.host = host;
        this.port = port;
        this.contextRoot = contextRoot;
        if (isSSL) {
            servletURL = "https://" + host + ":" + port + contextRoot;
        } else {
            servletURL = "http://" + host + ":" + port + contextRoot;
        }
        client = new DefaultHttpClient();
    }

    protected ServletClientImpl(LibertyServer server, boolean isSSL, String contextRoot) {
        this.server = server;
        this.contextRoot = contextRoot;
        host = server.getHostname();
        if (isSSL) {
            int securePort = server.getHttpDefaultSecurePort();
            port = securePort;
            servletURL = "https://" + host + ":" + securePort + contextRoot;
        } else {
            port = server.getHttpDefaultPort();
            servletURL = "http://" + host + ":" + port + contextRoot;
        }
        client = new DefaultHttpClient();
    }

    /**
     * @return
     */
    @Override
    public String getContextRoot() {
        return contextRoot;
    }

    /**
     * Triggers a JUnit failure after printing an error message.
     *
     * @param message
     */
    protected void failWithMessage(String message) {
        logger.severe("FAILURE: " + message);
        fail(message);
    }

    /**
     * Internal hook for various sub-classes to do additional
     * work to reset the client state.
     */
    protected abstract void hookResetClientState();

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetClientState() {
        client.getConnectionManager().shutdown();
        client = new DefaultHttpClient();
        hookResetClientState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseClient() {
        client.getConnectionManager().shutdown();
    }

    /** {@inheritDoc} */
    @Override
    public String accessUnprotectedServlet(String urlPattern) {
        String url = servletURL + urlPattern;
        logger.info("accessUnprotectedServlet: " + url);
        return access(url, 200);
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessDeniedHttpMethodServlet(String urlPattern, String user, String password) {
        String url = servletURL + urlPattern;
        logger.info("accessDeniedHttpMethodServlet: "
                    + url + " user= " + user + " password=" + password);
        return access(url, 403) == null;
    }

    /** {@inheritDoc} */
    @Override
    public String accessUnavailableServlet(String urlPattern) {
        String url = servletURL + urlPattern;
        logger.info("accessUnavailableServlet: " + url);
        return accessWithException(url, HttpHostConnectException.class);
    }

    /** {@inheritDoc} */
    @Override
    public String accessUnavailableServlet(String urlPattern, Class<?> expectedException) {
        String url = servletURL + urlPattern;
        logger.info("accessUnavailableServlet: " + url);
        return accessWithException(url, expectedException);
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessPrecludedServlet(String urlPattern) {
        String url = servletURL + urlPattern;
        logger.info("accessPrecludedServlet: " + url);
        return access(url, 403) == null;
    }

    /** {@inheritDoc} */
    @Override
    public String accessProtectedServletWithAuthorizedCredentials(String urlPattern, String user, String password) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithAuthorizedCredentials: "
                    + url + " user=" + user + " password="
                    + password);

        return accessAndAuthenticate(url, user, password, 200);
    }

    /** {@inheritDoc} */
    // @Override
    public String accessProtectedServletWithAuthorizedCredentialsExpectError500(String urlPattern, String user, String password) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithAuthorizedCredentialsExpectError500: "
                    + url + " user=" + user + " password="
                    + password);

        return accessAndAuthenticateForError500(url, user, password, 500);
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessProtectedServletWithUnauthorizedCredentials(String urlPattern, String user, String password) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithUnauthorizedCredentials: "
                    + url + " user=" + user + " password="
                    + password);

        return accessAndAuthenticate(url, user, password, 403) == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessProtectedServletWithInvalidCredentials(String urlPattern, String user, String password) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithInvalidCredentials: "
                    + url + " user=" + user + " password="
                    + password);

        return accessAndAuthenticate(url, user, password, 401) == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessProtectedServletWithInvalidRegistry(String urlPattern, String user, String password) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithInvalidRegistry: "
                    + url + " user=" + user + " password="
                    + password);

        return accessAndAuthenticate(url, user, password, 401) == null;
    }

    /** {@inheritDoc} */
    @Override
    public String accessProtectedServletWithValidHeaders(String urlPattern, Map<String, String> headers) {
        return accessProtectedServletWithValidHeaders(urlPattern, headers, true);
    }

    /** {@inheritDoc} */
    @Override
    public String accessProtectedServletWithValidHeaders(String urlPattern, Map<String, String> headers, Boolean ignoreErrorContent) {
        return accessProtectedServletWithValidHeaders(urlPattern, headers, ignoreErrorContent, null);
    }

    public String accessProtectedServletWithValidHeaders(String urlPattern, Map<String, String> headers, Boolean ignoreErrorContent, Boolean handleSSOCookie) {
        String url = servletURL + urlPattern;

        Set<String> headerKeys = headers.keySet();
        StringBuilder headerStringBuilder = new StringBuilder();
        headerStringBuilder.append("[");
        for (String header : headerKeys) {
            headerStringBuilder.append(header + ": " + headers.get(header) + ", ");
        }
        headerStringBuilder = headerStringBuilder.delete(headerStringBuilder.lastIndexOf(","), headerStringBuilder.length()).append("]");

        logger.info("accessProtectedServletWithValidHeaders: " + url + ", headers=[" + headerStringBuilder.toString() + "]");

        return accessWithHeaders(url, 200, headers, ignoreErrorContent, handleSSOCookie);
    }

    /** {@inheritDoc} */
    @Override
    public String accessProtectedServletWithInvalidHeaders(String urlPattern, Map<String, String> headers) {
        return accessProtectedServletWithInvalidHeaders(urlPattern, headers, true);
    }

    /** {@inheritDoc} */
    @Override
    public String accessProtectedServletWithInvalidHeaders(String urlPattern, Map<String, String> headers, boolean ignoreErrorContent) {
        return accessProtectedServletWithInvalidHeaders(urlPattern, headers, ignoreErrorContent, 401);
    }

    /** {@inheritDoc} */
    @Override
    public String accessProtectedServletWithInvalidHeaders(String urlPattern, Map<String, String> headers, boolean ignoreErrorContent, int expectedStatusCode) {
        String url = servletURL + urlPattern;

        Set<String> headerKeys = headers.keySet();
        StringBuilder headerStringBuilder = new StringBuilder();
        headerStringBuilder.append("[");
        for (String header : headerKeys) {
            headerStringBuilder.append(header + ": " + headers.get(header) + ", ");
        }
        headerStringBuilder = headerStringBuilder.delete(headerStringBuilder.lastIndexOf(","), headerStringBuilder.length()).append("]");

        logger.info("accessProtectedServletWithInvalidHeaders: " + url + ", headers=[" + headerStringBuilder.toString() + "]");

        return accessWithHeaders(url, expectedStatusCode, headers, ignoreErrorContent);
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessSSLRequiredSevlet(String urlPattern) {
        String url = servletURL + urlPattern;
        logger.info("accessSSLRequiredSevlet: " + url);

        try {
            HttpGet getMethod = new HttpGet(url);
            client.execute(getMethod);
            failWithMessage("Excepted SSL challenge did not occur");
        } catch (SSLPeerUnverifiedException e) {
            return true;
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
        }
        return false;
    }

    /**
     * Access an (un)protected URL pattern that is part of the context root.
     * Expected behavior is dictated by the expected status code.
     *
     * @param url
     *            Full URL to the requested resource
     * @param expectedStatusCode
     *            The expected HTTP status code for the request
     * @return servlet response text, null if access not granted
     */
    protected abstract String access(String url, int expectedStatusCode);

    /**
     * Access a URL pattern that should throw the given exception.
     * Expected behavior is dictated by the expected status code.
     *
     * @param url
     *            Full URL to the requested resource
     * @param expectedException
     *            The expected exception when accessing the request
     * @return exception message, null if exception not thrown
     */
    protected abstract String accessWithException(String url, Class<?> expectedException);

    /**
     * Access a protected URL pattern that is part of the context root. Expected
     * behavior is dictated by the expected status code.
     *
     * @param url
     *            Full URL to the requested resource
     * @param user
     *            user to authenticate as
     * @param password
     *            password to authenticate with
     * @param expectedStatusCode
     *            The expected HTTP status code for the request
     * @return servlet response text, null if access not granted
     */
    protected abstract String accessAndAuthenticateForError500(String url,
                                                               String user, String password, int expectedStatusCode);

    /**
     * Access a protected URL pattern that is part of the context root. Expected
     * behavior is dictated by the expected status code.
     *
     * @param url
     *            Full URL to the requested resource
     * @param user
     *            user to authenticate as
     * @param password
     *            password to authenticate with
     * @param expectedStatusCode
     *            The expected HTTP status code for the request
     * @return servlet response text, null if access not granted
     */
    protected abstract String accessAndAuthenticate(String url,
                                                    String user, String password, int expectedStatusCode);

    /**
     * Access a protected URL pattern that is part of the context root. Expected
     * behaviour is a 403 for an internal error.
     *
     * @param url
     *            Full URL to the requested resource
     * @param user
     *            user to authenticate as
     * @param password
     *            password to authenticate with
     * @return servlet response text, null if access not granted
     */
    protected abstract String accessAndAuthenticateForExpectedInternalError(String url,
                                                                            String user, String password);

    /** {@inheritDoc} */
    @Override
    public void setSSOCookieName(String ssoCookieName) {
        this.ssoCookieName = ssoCookieName;
    }

    /** {@inheritDoc} */
    @Override
    public String getCookieFromLastLogin() {
        return ssoCookie;
    }

    /** {@inheritDoc} */
    @Override
    public String accessProtectedServletWithAuthorizedCookie(String urlPattern,
                                                             String cookie) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithAuthorizedCookie: " + url +
                    " cookie=" + cookie);

        return accessWithCookie(url, cookie, 200);
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessProtectedServletWithUnauthorizedCookie(String urlPattern, String cookie) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithUnauthorizedCookie: " + url +
                    " cookie=" + cookie);

        return accessWithCookie(url, cookie, 403) == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean accessProtectedServletWithInvalidCookie(String urlPattern,
                                                           String cookie) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithInvalidCookie: " + url);

        return accessWithCookie(url, cookie, 401) == null;
    }

    /**
     * Access an (un)protected URL pattern that is part of the context root.
     * Expected behavior is dictated by the expected status code.
     *
     * @param url
     *            Full URL to the requested resource
     * @param expectedStatusCode
     *            The expected HTTP status code for the request
     * @return servlet response text, null if access not granted
     */
    protected abstract String accessWithCookie(String urlPattern, String cookie, int expectedStatusCode);

    /**
     * Access an (un)protected URL pattern that is part of the context root using
     * the passed headers. Expected behavior is dictated by the expected status code.
     *
     * @param url
     *            Full URL to the requested resource
     * @param expectedStatusCode
     *            The expected HTTP status code for the request
     * @param headers
     *            Map of header names and values to be included in the request
     * @param ignoreErrorContent
     *            Boolean specifying whether the HTTP response should be set to null if access was unsuccessful
     * @return servlet response text
     */
    public String accessWithHeaders(String url, int expectedStatusCode, Map<String, String> headers, Boolean ignoreErrorContent) {
        return accessWithHeaders(url, expectedStatusCode, headers, ignoreErrorContent, null);
    }

    /**
     * Access an (un)protected URL pattern that is part of the context root using
     * the passed headers. Expected behavior is dictated by the expected status code.
     *
     * @param url
     *            Full URL to the requested resource
     * @param expectedStatusCode
     *            The expected HTTP status code for the request
     * @param headers
     *            Map of header names and values to be included in the request
     * @param ignoreErrorContent
     *            Boolean specifying whether the HTTP response should be set to null if access was unsuccessful
     * @param handleSSOCookie
     *            Boolean indicating whether an SSO cookie should be handled for requests
     * @return servlet response text
     */
    protected abstract String accessWithHeaders(String url, int expectedStatusCode, Map<String, String> headers, Boolean ignoreErrorContent, Boolean handleSSOCookie);

    /** {@inheritDoc} */
    @Override
    public void checkForPasswordsInLogsAndTrace(String password) throws Exception {
        if (server != null) {
            List<String> passwordsInTrace = server.findStringsInLogsAndTrace(password);
            assertEquals("Should not find password in the log file",
                         Collections.emptyList(), passwordsInTrace);
        }
    }

    /**
     * Validate there are no SSO cookies in the response.
     *
     * @param postMethod
     */
    public void validateNoSSOCookie(HttpMessage httpMessage) {
        logger.info("validateNoSSOCookie");
        Header[] setCookieHeaders = httpMessage.getHeaders("Set-Cookie");
        if (setCookieHeaders != null) {
            for (Header header : setCookieHeaders) {
                logger.info("header: " + header);
                for (HeaderElement e : header.getElements()) {
                    if (e.getName().equals(ssoCookieName)) {
                        failWithMessage("Found unexpected Set-Cookie for SSO cookie. Expecting NO sso cookie to be set");
                    }
                }
            }
        }
    }

    /**
     * Validate there is an SSO cookies in the response, and set it
     * so it can be retrieved.
     *
     * @param postMethod
     */
    protected void setSSOCookieForLastLogin(HttpMessage httpMessage) {
        logger.info("setSSOCookieForLastLogin");
        Header[] setCookieHeaders = httpMessage.getHeaders("Set-Cookie");
        if (setCookieHeaders == null) {
            failWithMessage("setCookieHeaders was null and should not be");
        }
        for (Header header : setCookieHeaders) {
            logger.info("header: " + header);
            for (HeaderElement e : header.getElements()) {
                if (e.getName().equals(ssoCookieName)) {
                    ssoCookie = e.getValue();
                    return;
                }
            }
        }
        fail("Set-Cookie for " + ssoCookieName + " not found in the cookieHeader after login");
    }

    /** {@inheritDoc} */
    @Override
    public boolean verifyUnauthenticatedResponse(String response) {
        return verifyResponse(response, null, null, false, false, null, false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean verifyResponse(String response, String userName,
                                  boolean isUserInEmployeeRole, boolean isUserInManagerRole) {
        return verifyResponse(response, authType, userName, isUserInEmployeeRole,
                              isUserInManagerRole, null, false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean verifyResponse(String response, String userName,
                                  boolean isUserInEmployeeRole, boolean isUserInManagerRole,
                                  String specifiedRole, boolean isUserInSpecifiedRole) {
        return verifyResponse(response, authType, userName, isUserInEmployeeRole,
                              isUserInManagerRole, specifiedRole, isUserInSpecifiedRole);
    }

    /**
     * Perform the checks based in the response String, based on the
     * expected results provided.
     *
     * @param response
     * @param authType
     * @param userName
     * @param isUserInEmployeeRole
     * @param isUserInManagerRole
     * @param specifiedRole
     * @param isUserInSpecifiedRole
     * @return
     */
    private boolean verifyResponse(String response, String authType, String userName,
                                   boolean isUserInEmployeeRole, boolean isUserInManagerRole,
                                   String specifiedRole, boolean isUserInSpecifiedRole) {

        assertNotNull("The response should not be null", response);

        assertTrue("The response did not contain the expected remoteUser",
                   response.contains(REMOTE_USER_HEADER + userName));

        if (authType != null) {
            assertTrue("The response did not contain the expected authType",
                       response.contains(AUTH_TYPE_HEADER + authType));
        } else {
            assertTrue("The response did not contain the expected authType",
                       response.contains(AUTH_TYPE_HEADER + "null"));
        }

        if (userName != null) {
            assertTrue("The response did not contain the expected userPrincipal",
                       response.contains(USER_PRINCIPAL_HEADER + WSPRINCIPAL
                                         + userName));
            assertTrue("The response did not contain the expected Principal name",
                       response.contains(USER_PRINCIPAL_NAME_HEADER + userName));
        } else {
            assertTrue("The response did not contain the expected userPrincipal",
                       response.contains(USER_PRINCIPAL_HEADER + "null"));
        }

        assertTrue("The response did not contain the expected isUserInRole(Employee)",
                   response.contains(EMPLOYEE_ROLE_HEADER + isUserInEmployeeRole));

        assertTrue("The response did not contain the expected isUserInRole(Manager)",
                   response.contains(MANAGER_ROLE_HEADER + isUserInManagerRole));

        assertTrue("The response did not contain the expected isUserInRole(" + specifiedRole + ")",
                   response.contains("isUserInRole(" + specifiedRole + "): " + isUserInSpecifiedRole));

        return true;
    }
}
