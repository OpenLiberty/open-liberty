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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import componenttest.topology.impl.LibertyServer;

/**
 * Provide common functionality for accessing the BasicAuth servlet.
 */
public class FormLoginClient extends ServletClientImpl {
    private static final Class<?> c = FormLoginClient.class;
    public static final String DEFAULT_SERVLET_NAME = "ServletName: FormLoginServlet";
    public static final String DEFAULT_CONTEXT_ROOT = "/formlogin";
    public static final String DEFAULT_JSP_NAME = "JSPName: FormLoginJSP.jsp";
    public static final String DEFAULT_JSP_CONTEXT_ROOT = "/formlogin/JSP";
    public final static String LOGIN_PAGE = "/login.jsp";

    public enum LogoutOption {
        LOGOUT_DEFAULT_PAGE, LOGOUT_TO_LOGIN_PAGE, LOGOUT_OFF_HOST_FAIL, LOGOUT_OFF_HOST_SUCCESS
    };

    private final static String FORM_LOGIN_PAGE = "Form Login Page";
    private final static String FORM_LOGOUT_PAGE = "Form Logout Page";
    private final static String LOGIN_ERROR_PAGE = "Form Login Error Page";
    private final static String SUCCESSFUL_LOGOUT_PAGE = "Successful Logout";
    private final static String OFF_HOST_PAGE_URL = "http://www.w3.org/Protocols/HTTP/AsImplemented.html";

    private final String servletName;

    private final static String SERVLET_SPEC_30 = "30";
    private final static String SERVLET_SPEC_31 = "31";

    private final static String HTTP_PROTOCOL_10 = "10";
    private final static String HTTP_PROTOCOL_11 = "11";
    private final int REDIRECT_STATUS_DEFAULT = 302;
    private final int REDIRECT_STATUS_SERVLET31 = 303;

    protected String servletSpec = SERVLET_SPEC_30;

    protected int redirectStatus = REDIRECT_STATUS_DEFAULT;

    protected boolean setRedirectParam = false;
    protected String httpProtocol = HTTP_PROTOCOL_11;

    protected boolean retryMode = false;

    /**
     * Creates a FormLoginClient with the default values for context
     * root and cookie name.
     *
     * @param host
     * @param port
     */
    public FormLoginClient(String host, int port) {
        this(host, port, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public FormLoginClient(String host, int port, String servletName, String contextRoot) {
        this(host, port, false, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);
    }

    FormLoginClient(String host, int port, boolean isSSL, String servletName, String contextRoot) {
        super(host, port, isSSL, contextRoot);
        this.servletName = servletName;
        authType = "FORM";
    }

    public FormLoginClient(LibertyServer server) {
        this(server, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT);
    }

    public FormLoginClient(LibertyServer server, String servletName, String contextRoot) {
        this(server, false, servletName, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);
    }

    /*
     * Constructor used when testcases need to do specific work for Servlet Spec 3.1
     *
     * Servlet Spec 3.1 has a requirement that the status code on a successful form
     * login be set to 303 when using HTTP 1.1, and 302 when using HTTP 1.0.
     */
    public FormLoginClient(LibertyServer server, boolean isSSL, String servletName, String contextRoot, String servletSpec, String httpProtocol) {
        this(server, isSSL, servletName, contextRoot);
        this.servletSpec = servletSpec;
        this.httpProtocol = httpProtocol;
        setupRedirectValues(servletSpec, httpProtocol);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);
        logger.info("Servlet Spec: " + servletSpec);
        logger.info("HTTP protocol: " + httpProtocol);
    }

    /**
     * For Servlet Spec 3.1 the redirect status should be 303 if the form login
     * is successful and the HTTP protocol version is 1.1. If the HTTP protocol
     * version is 1.0 the default redirect should be the default 302.
     *
     * @param servletSpec - The Servlet Spec level
     * @param httpProtocol - The HTTP protocol version
     */
    private void setupRedirectValues(String spec, String protocol) {
        if (spec.equals(SERVLET_SPEC_31)) {
            setRedirectParam = true;
            if (protocol.equals(HTTP_PROTOCOL_11))
                redirectStatus = REDIRECT_STATUS_SERVLET31;
        }
    }

    /**
     * Root constructor for both FormLogin and SSLFormLoginClient.
     *
     * @param server
     * @param isSSL
     * @param servletName
     * @param contextRoot
     */
    FormLoginClient(LibertyServer server, boolean isSSL, String servletName, String contextRoot) {
        super(server, isSSL, contextRoot);
        this.servletName = servletName;
        authType = "FORM";
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
    public boolean accessPrecludedServlet(String urlPattern) {
        String url = servletURL + urlPattern;
        logger.info("accessPrecludedServlet: " + url);

        return accessPageNoChallenge(client, url,
                                     403) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String access(String url, int expectedStatusCode) {
        logger.info("access: url=" + url +
                    " expectedStatusCode=" + expectedStatusCode);

        return accessPageNoChallenge(client, url, expectedStatusCode);
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
            client.execute(getMethod);
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
     * Access a FormLogin protected URL pattern that is part of the context
     * root. Expected behaviour is dictated by the expected status code.
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
    @Override
    protected String accessAndAuthenticate(String url, String user,
                                           String password, int expectedStatusCode) {

        ssoCookie = null;
        return accessAndAuthenticate(client, url, user, password, expectedStatusCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String accessAndAuthenticateForExpectedInternalError(String url, String user,
                                                                   String password) {
        logger.info("accessAndAuthenticate: url=" + url +
                    " user=" + user + " password=" + password);
        ssoCookie = null;
        accessFormLoginPage(client, url);

        String location = performFormLogin(client, url, user, password, 403);
        return accessPageNoChallenge(client, location, 403);
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

        ssoCookie = null;
        accessFormLoginPage(client, url);

        String location = performFormLogin(client, url, user, password, 403);
        return accessPageNoChallenge(client, location, 500);

    }

    /**
     * @param urlPattern
     * @param user
     * @param password
     * @param expectedStatusCode
     * @param client
     * @return
     */
    public String accessAndAuthenticate(HttpClient client, String url,
                                        String user, String password,
                                        int expectedStatusCode) {
        logger.info("accessAndAuthenticate: url=" + url
                    + " user=" + user + " password=" + password
                    + " expectedStatusCode=" + expectedStatusCode);

        accessFormLoginPage(client, url);

        String location = performFormLogin(client, url, user, password, expectedStatusCode);
        if (expectedStatusCode == 401) {
            return accessLoginErrorPage(client, location);
        } else {
            return accessPageNoChallenge(client, location,
                                         expectedStatusCode);
        }
    }

    /**
     * @param urlPattern
     * @param client
     * @throws IOException
     * @throws HttpException
     */
    public void accessFormLoginPage(HttpClient client, String url) {
        accessFormLoginPage(client, url, null, 200);
    }

    /**
     * @param urlPattern
     * @param client
     * @throws IOException
     * @throws HttpException
     */
    public void accessFormLoginPage(HttpClient client, String url, Map<String, String> addlHeaders, int expectedStatusCode) {
        logger.info("accessFormLoginPage: url=" + url + "  addlHeaders=" + addlHeaders);

        // Get method on form login page
        HttpGet getMethod = new HttpGet(url);
        if (addlHeaders != null) {
            for (Entry<String, String> entry : addlHeaders.entrySet()) {
                getMethod.setHeader(entry.getKey(), entry.getValue());
            }
        }
        accessFormLoginPage(client, getMethod, expectedStatusCode);
    }

    /**
     * @param urlPattern
     * @param client
     * @param reqMethod HttpUriRequest object which also include additional header values.
     * @param expectedStatusCode
     * @throws IOException
     * @throws HttpException
     */
    public HttpResponse accessFormLoginPage(HttpClient client, HttpUriRequest reqMethod, int expectedStatusCode) {
        logger.info("accessFormLoginPage: url=" + reqMethod.getURI().toString() + " request method=" + reqMethod);
        HttpResponse response = null;
        try {
            response = client.execute(reqMethod);

            logger.info("Form login page result: " + response.getStatusLine());
            assertEquals("Expected " + expectedStatusCode + " status code for form login page was not returned",
                         expectedStatusCode, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());
            logger.info("Form login page content: " + content);
            EntityUtils.consume(response.getEntity());

            if (expectedStatusCode == 200) {
                // Verify we get the form login JSP
                assertTrue("Did not find expected form login page: " + FORM_LOGIN_PAGE,
                           content.contains(FORM_LOGIN_PAGE));
            }
        } catch (IOException e) {
            failWithMessage("Caught unexpected exception: " + e);
        }
        return response;
    }

    /**
     *
     * There are difference in redirect status depending on the Servlet Spec level
     * and the HTTP version. This method should handle all the case.
     *
     * Servlet 3.0 302 redirect status for Failed and Successful logins
     * Servlet 3.1 and HTTP 1.0 302 redirect status for Failed and Successful logins
     * Servlet 3.1 and HTTP 1.1 303 redirect status for Successful logins
     * 302 redirect status for Failed logins
     *
     * Note: httpClient behaves different on redirect between Servlet Spec 3.0
     * and 3.1. When using Servlet Spec 3.0 the post method on httpCleint does not
     * follow the redirect by default. When using Servlet Spec 3.1 it does.
     * Because the tests want to check the redirect status when running under
     * Servlet Spec 3.1 handling redirects is disabled so the redirect status can
     * be verified.
     *
     *
     * @param urlPattern
     * @param user
     * @param password
     * @param client
     * @return
     * @throws IOException
     * @throws HttpException
     */
    protected String performFormLogin(HttpClient client, String url, String user,
                                      String password, int expectedStatusCode) {
        logger.info("performFormLogin: url=" + url +
                    " user=" + user + " password=" + password +
                    " expectedStatusCode=" + expectedStatusCode);

        logger.info("Testing with Servlet Spec " + servletSpec + " and HTTP protocol " + httpProtocol);

        int status = redirectStatus;

        // If we are testing with the HTTP 1.0 protocol we need to set the client
        // to the version on the client to HTTP 1.0.
        if (httpProtocol.equals(HTTP_PROTOCOL_10))
            client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_0);

        try {
            // Post method to login
            HttpPost postMethod = new HttpPost(servletURL + "/j_security_check");

            if (setRedirectParam) {
                /*
                 * When running in Servlet Spec 3.1 httpClient does not auto-matically
                 * stop on the redirect after the form login page. This is to set
                 * up httpClient so that it will allow the code to stop on the redirect
                 * so we can check for the proper status.
                 */
                HttpParams params = postMethod.getParams();
                params.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
                postMethod.setParams(params);

                /*
                 * If we expecting a 401, authorization error, then the redirect status
                 * after the form login should be set to the default 302.
                 */
                if (expectedStatusCode == 401)
                    status = REDIRECT_STATUS_DEFAULT;

                logger.info("The expected redirect status is " + status);
            }

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            HttpResponse response = client.execute(postMethod);
            assertEquals("Expecting form login getStatusCode " + status, status,
                         response.getStatusLine().getStatusCode());

            // Verify redirect to servlet
            Header header = response.getFirstHeader("Location");
            String location = header.getValue();
            logger.info("Redirect location: " + location);

            EntityUtils.consume(response.getEntity());

            if (expectedStatusCode != 401) {
                setSSOCookieForLastLogin(response);
            } else {
                validateNoSSOCookie(response);
            }

            if (expectedStatusCode == 200) {
                assertEquals("Redirect location was not the original URL!",
                             url, location);
            }
            return location;
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     *
     * @param client
     * @param location
     * @param expectedStatusCode
     * @return
     */
    protected String accessPageNoChallenge(HttpClient client,
                                           String location, int expectedStatusCode) {
        return accessPageNoChallenge(client, location, expectedStatusCode, null);
    }

    /**
     *
     * @param client
     * @param location
     * @param expectedStatusCode
     * @return
     */
    protected String accessPageNoChallenge(HttpClient client,
                                           String location, int expectedStatusCode, Map<String, String> addlHeaders) {
        logger.info("accessPageNoChallenge: location=" + location +
                    " expectedStatusCode=" + expectedStatusCode + "  addlHeaders=" + addlHeaders);

        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(location);
            if (addlHeaders != null) {
                for (Entry<String, String> entry : addlHeaders.entrySet()) {
                    getMethod.setHeader(entry.getKey(), entry.getValue());
                }
            }
            HttpResponse response = client.execute(getMethod);

            logger.info("getMethod status: " + response.getStatusLine());
            assertEquals("Expected " + expectedStatusCode + " was not returned",
                         expectedStatusCode, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());
            logger.info("Servlet content: " + content);
            EntityUtils.consume(response.getEntity());

            // Paranoia check, make sure we hit the right servlet
            if (response.getStatusLine().getStatusCode() == 200) {
                assertTrue("Response did not contain expected servlet name (" + servletName + ")",
                           content.contains(servletName));
                return content;
            } else if (expectedStatusCode == 401) {
                assertTrue("Response was not the expected error page: "
                           + LOGIN_ERROR_PAGE, content.contains(LOGIN_ERROR_PAGE));
                return null;
            } else {
                return null;
            }
        } catch (IOException e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     *
     * @param client
     * @param location
     * @param expectedStatusCode
     * @return
     */
    protected String accessLoginErrorPage(HttpClient client, String location) {
        logger.info("accessLoginErrorPage: location=" + location);

        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(location);
            HttpResponse response = client.execute(getMethod);

            logger.info("Form login error page result: " + response.getStatusLine());
            assertEquals("Expected 200 status code for form login page error was not returned",
                         200, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());
            logger.info("Form login error page content: " + content);

            // Paranoia check, make sure we hit the right servlet
            assertTrue("Response was not the expected error page: "
                       + LOGIN_ERROR_PAGE, content.contains(LOGIN_ERROR_PAGE));
            return null;
        } catch (IOException e) {
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

        try {
            HttpGet getMethod = new HttpGet(url);
            getMethod.setHeader("Cookie", ssoCookieName + "=" + cookie);
            HttpResponse response = client.execute(getMethod);

            logger.info("getMethod status: " + response.getStatusLine());
            assertEquals("Expected " + expectedStatusCode + " was not returned",
                         expectedStatusCode, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());
            logger.info("Servlet content: " + response);

            // Paranoia check, make sure we hit the right servlet
            if (expectedStatusCode == 200) {
                assertTrue("Response did not contain expected servlet name (" + servletName + ")",
                           content.contains(servletName));
                return content;
            } else {
                return null;
            }
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }
    }

    /**
     * Access a protected URL pattern that is part of the context root with an
     * invalid SSO cookie. Access is not granted and the client is directed to
     * the form login page.
     *
     * @param urlPattern
     *            URL pattern that is under of the context root.
     * @param cookie
     *            SSO cookie to login with
     * @return true if access was redirected to the form login page
     */
    @Override
    public boolean accessProtectedServletWithInvalidCookie(String urlPattern,
                                                           String cookie) {
        String url = servletURL + urlPattern;
        logger.info("accessProtectedServletWithInvalidCookie: " + url +
                    " cookie=" + cookie);

        try {
            HttpGet getMethod = new HttpGet(url);
            getMethod.setHeader("Cookie", ssoCookieName + "=" + cookie);
            HttpResponse response = client.execute(getMethod);

            logger.info("Form login page result: " + response.getStatusLine());
            assertEquals("Expected 200 status code for form login page was not returned",
                         200, response.getStatusLine().getStatusCode());

            String content = EntityUtils.toString(response.getEntity());
            logger.info("Form login page content: " + content);

            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORM_LOGIN_PAGE,
                       content.contains(FORM_LOGIN_PAGE));
            return true;
        } catch (IOException e) {
            failWithMessage("Caught unexpected exception: " + e);
            return false;
        }
    }

    /**
     *
     * Constructs the URL to be used to find the logout page - logout.html.
     *
     */
    protected String servletURLForLogout() {
        return servletURL + "/logout.html";
    }

    /**
     * Performs a form login to PROTECTED_SIMPLE then a form logout,
     * all using the same HttpClient.
     *
     * @param user
     * @param password
     */
    public void formLogout(LogoutOption logoutOption, String user, String password) {
        logger.info("formLogout: logoutOption=" + logoutOption
                    + " user=" + user + " password=" + password);

        try {
            accessAndAuthenticate(client, servletURL + PROTECTED_SIMPLE, user, password, 200);
            // Ensure we have a non-null, populated cookie value
            assertNotNull("The SSO cookie was null", getCookieFromLastLogin());
            assertFalse("The SSO cookie had an empty String value", "".equals(getCookieFromLastLogin()));

            // Validate we have the form login page
            HttpGet getMethod = new HttpGet(servletURLForLogout());
            HttpResponse response = client.execute(getMethod);

            // Verify we got the form logout page
            String content = EntityUtils.toString(response.getEntity());
            logger.info("getMethod.getStatusCode(): " + response.getStatusLine().getStatusCode());
            logger.info("Get response for logout page: " + response);
            assertEquals("The response code was not 200 as expected", 200, response.getStatusLine().getStatusCode());
            assertTrue("Form logout page not found: " + FORM_LOGOUT_PAGE, content.contains(FORM_LOGOUT_PAGE));
            EntityUtils.consume(response.getEntity());

            // Post method to logout
            logger.info("logout URL: " + servletURL + "/ibm_security_logout");
            HttpPost postMethod = new HttpPost(servletURL + "/ibm_security_logout");

            // Set up the postMethod based on the action we wanted
            List<NameValuePair> nvps;
            switch (logoutOption) {
                case LOGOUT_DEFAULT_PAGE:
                    break;
                case LOGOUT_TO_LOGIN_PAGE:
                    nvps = new ArrayList<NameValuePair>();
                    nvps.add(new BasicNameValuePair("logout", "Logout2"));
                    nvps.add(new BasicNameValuePair("logoutExitPage", "/login.jsp"));
                    postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                    break;
                case LOGOUT_OFF_HOST_SUCCESS:
                case LOGOUT_OFF_HOST_FAIL:
                    nvps = new ArrayList<NameValuePair>();
                    nvps.add(new BasicNameValuePair("logout", "Logout3"));
                    nvps.add(new BasicNameValuePair("logoutExitPage", OFF_HOST_PAGE_URL));
                    postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                    break;
            }

            response = client.execute(postMethod);
            logger.info("postMethod.getStatusCode(): " + response.getStatusLine().getStatusCode());
            content = EntityUtils.toString(response.getEntity());
            logger.info("Form logout getResponseBodyAsString: " + content);
            EntityUtils.consume(response.getEntity());

            // Validate that the SSO has been cleared
            setSSOCookieForLastLogin(response);
            assertEquals("", getCookieFromLastLogin());

            HttpGet redirectedGet;
            // Validate the response based on the action we wanted
            switch (logoutOption) {
                case LOGOUT_DEFAULT_PAGE:
                case LOGOUT_OFF_HOST_FAIL:
                    assertEquals("Expected 200 status code in resposne to logout",
                                 200, response.getStatusLine().getStatusCode());
                    assertTrue("Form logout page output not found: " + SUCCESSFUL_LOGOUT_PAGE,
                               content.contains(SUCCESSFUL_LOGOUT_PAGE));
                    break;
                case LOGOUT_TO_LOGIN_PAGE:
                    assertEquals("Expected redirect status code",
                                 302, response.getStatusLine().getStatusCode());
                    redirectedGet = new HttpGet(response.getFirstHeader("Location").getValue());
                    response = client.execute(redirectedGet);
                    content = EntityUtils.toString(response.getEntity());
                    logger.info("Redirected getResponseBodyAsString: " + content);
                    assertTrue("Form login page not found: " + FORM_LOGIN_PAGE, content.contains(FORM_LOGIN_PAGE));
                    EntityUtils.consume(response.getEntity());
                    break;
                case LOGOUT_OFF_HOST_SUCCESS:
                    assertEquals("Expected redirect status code",
                                 302, response.getStatusLine().getStatusCode());
                    String redirectUrlReturned = response.getFirstHeader("Location").getValue();
                    assertEquals("Returned URL to redirect client to should have been " + OFF_HOST_PAGE_URL +
                                 " but was " + redirectUrlReturned,
                                 redirectUrlReturned, OFF_HOST_PAGE_URL);
                    break;
            }
        } catch (IOException e) {
            failWithMessage("Caught unexpected exception: " + e);
        }

    }

    @Override
    protected String accessWithHeaders(String url, int expectedStatusCode, Map<String, String> headers, Boolean ignoreErrorContent, Boolean handleSSOCookie) {
        logger.info("accessWithHeaders: url=" + url + " expectedStatusCode=" + expectedStatusCode);

        try {
            HttpGet getMethod = new HttpGet(url);

            Set<String> headerKeys = headers.keySet();
            StringBuilder headerStringBuilder = new StringBuilder();
            headerStringBuilder.append("[");
            for (String header : headerKeys) {
                getMethod.setHeader(header, headers.get(header));
                headerStringBuilder.append(header + ": " + headers.get(header) + ", ");
            }
            headerStringBuilder = headerStringBuilder.delete(headerStringBuilder.lastIndexOf(","), headerStringBuilder.length()).append("]");
            logger.info("accessWithHeaders: headers=" + headerStringBuilder.toString());

            return executeAndProcessGetMethod(getMethod, expectedStatusCode, handleSSOCookie, ignoreErrorContent);
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
            return null;
        }

    }

    /**
     * Do all of the common steps for handling the getMethod, such as validating
     * the expected status code.
     *
     * @param getMethod
     * @param expectedStatusCode
     * @param handleSSOCookie
     * @param ignoreErrorContent
     * @return The response text, or null if not authorized
     * @throws IOException
     */
    private String executeAndProcessGetMethod(HttpGet getMethod, Integer expectedStatusCode, Boolean handleSSOCookie, Boolean ignoreErrorContent) throws IOException {
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
            assertTrue("Response did not contain expected servlet name (" + servletName + ")",
                       content.contains(servletName));
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
        return content;
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
