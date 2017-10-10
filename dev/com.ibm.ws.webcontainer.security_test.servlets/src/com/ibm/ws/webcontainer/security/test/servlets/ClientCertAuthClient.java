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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import componenttest.topology.impl.LibertyServer;

/**
 * Provide common functionality for accessing the BasicAuth servlet.
 */
public class ClientCertAuthClient extends ServletClientImpl {
    private static final Class<?> c = ClientCertAuthClient.class;
    public static final String DEFAULT_SERVLET_NAME = "Snoop Servlet Client Cert";
    public static final String DEFAULT_CONTEXT_ROOT = "/snoop_cert";
    private final static String CLIENT_CERT_REALM = "Client Cert Authentication";
    private final static String DEFAULT_KS_FILE = "publish" + File.separator + "files" + File.separator + "keystore.jks";
    private final static String DEFAULT_KS_PASSWORD = "password";
    private final static String FORM_LOGIN_PAGE = "Form Login Page";
    private final String servletName;
    protected boolean retryMode = false;

    /**
     * Constructs a client to connect to the default servlet.
     * with the given host and port. The servlet name and context root
     * are defaulted.
     *
     * @param host
     * @param port
     */
    public ClientCertAuthClient(String host, int port, boolean isSSL) {
        this(host, port, isSSL, null, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT, DEFAULT_KS_FILE, DEFAULT_KS_PASSWORD);
    }

    public ClientCertAuthClient(String host, int port, boolean isSSL,
                                LibertyServer server, String servletName, String contextRoot, String ksPath, String ksPassword) {
        super(host, port, isSSL, contextRoot);
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("ClientCertAuthClient: host=" + host +
                    " port=" + port + " isSSL=" + isSSL +
                    " servletName=" + servletName + " contextRoot=" + contextRoot);
        this.servletName = servletName;
        authType = "CLIENT-CERT";
        if (isSSL) {
            SSLHelper.establishSSLContext(client, port, server, ksPath, ksPassword, null, null);
        }
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
    public String access(String urlPattern, int expectedStatusCode) {
        String url = servletURL + urlPattern;
        logger.info("access: url=" + url +
                    " expectedStatusCode=" + expectedStatusCode);

        try {
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            return processGetMethod(response, expectedStatusCode);
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
     * {@inheritDoc}
     */
    public void certAuthFailOverToFormLogin(String url) {
        logger.info("access: url=" + url);
        try {
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity);
            logger.info("Form login page result: " + response.getStatusLine());
            assertEquals("Expected 200 status code for form login page was not returned",
                         200, response.getStatusLine().getStatusCode());
            logger.info("Form login page content: " + content);
            EntityUtils.consume(response.getEntity());
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORM_LOGIN_PAGE, content.contains(FORM_LOGIN_PAGE));
        } catch (Exception e) {
            failWithMessage("Caught unexpected exception: " + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String accessAndAuthenticate(String url, String user,
                                        String password, int expectedStatusCode) {
        logger.info("accessAndAuthenticate: url=" + url +
                    " user=" + user + " password=" + password +
                    " expectedStatusCode=" + expectedStatusCode);
        try {
            if (user != null) {
                client.getCredentialsProvider().setCredentials(new AuthScope(host, AuthScope.ANY_PORT, CLIENT_CERT_REALM),
                                                               new UsernamePasswordCredentials(user, password));
            }
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            String content = processGetMethod(response, expectedStatusCode);
            return content;
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
                client.getCredentialsProvider().setCredentials(new AuthScope(host, AuthScope.ANY_PORT, CLIENT_CERT_REALM),
                                                               new UsernamePasswordCredentials(user, password));
            }
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            String content = processGetMethod(response, 403);
            return content;
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
                client.getCredentialsProvider().setCredentials(new AuthScope(host, AuthScope.ANY_PORT, CLIENT_CERT_REALM),
                                                               new UsernamePasswordCredentials(user, password));
            }
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            String content = processGetMethod(response, 500);
            return content;
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

        try {
            HttpGet getMethod = new HttpGet(url);
            getMethod.setHeader("Cookie", ssoCookieName + "=" + cookie);
            HttpResponse response = client.execute(getMethod);
            return processGetMethod(response, expectedStatusCode);
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
     * @return The response text, or null if not authorized
     * @throws IOException
     */
    private String processGetMethod(HttpResponse response, int expectedStatusCode) throws IOException {
        logger.info("getMethod status: " + response.getStatusLine());
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity);
        logger.info("Servlet response: " + content);
        EntityUtils.consume(entity);

        assertEquals("Expected " + expectedStatusCode + " was not returned",
                     expectedStatusCode, response.getStatusLine().getStatusCode());

        // Paranoia check, make sure we hit the right servlet
        if (response.getStatusLine().getStatusCode() == 200) {
            assertTrue("Response did not contain expected servlet name (" + servletName + ")",
                       content.contains(servletName));
            return content;
        } else {
            return null;
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
