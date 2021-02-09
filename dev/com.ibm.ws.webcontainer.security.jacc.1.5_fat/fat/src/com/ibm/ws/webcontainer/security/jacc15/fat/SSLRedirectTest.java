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

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;
import junit.framework.AssertionFailedError;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SSLRedirectTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.loginmethod");
    private final Class<?> thisClass = SSLRedirectTest.class;

    private final static String employeeUser = "user1";
    private final static String employeePassword = "user1pwd";

    final static String hostName = server.getHostname();
    final static int httpDefault = server.getHttpDefaultPort();
    final static int httpsDefault = server.getHttpDefaultSecurePort();

    final static int httpFAT_1 = Integer.getInteger("httpFAT_1");
    final static int httpFAT_2 = Integer.getInteger("httpFAT_2");
    final static int httpFAT_3 = Integer.getInteger("httpFAT_3");
    final static int httpFAT_4 = Integer.getInteger("httpFAT_4");

    final static String proxyHostName = "my.ibm.com";
    final static int proxyHttpFAT_default = 80;
    final static int proxyHttpsFAT_default = 443;
    // there shouldn't be a controller running in this test, so we'll use these
    // ports since we're relatively confident that they shouldn't be in use.
    final static int proxyHttpFAT_1 = Integer.getInteger("controller_1.http");
    final static int proxyHttpsFAT_1 = Integer.getInteger("controller_1.https");
    final static int proxyHttpFAT_2 = Integer.getInteger("controller_2.http");
    final static int proxyHttpsFAT_2 = Integer.getInteger("controller_2.https");
    final static int proxyHttpFAT_3 = Integer.getInteger("controller_3.http");
    final static int proxyHttpsFAT_3 = Integer.getInteger("controller_3.https");

    // A low connection timeout is necessary, because the proxy tests
    // don't actually connect to real proxies - so if we use default
    // connection timeouts, this test could take a really long time...
    private final static int CONN_TIMEOUT = 2000; //2 seconds
    private final static String LS = System.getProperty("line.separator");

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {

        JACCFatUtils.installJaccUserFeature(server);
        JACCFatUtils.transformApps(server, "loginmethod.ear");

        server.addInstalledAppForValidation("loginmethod");
        server.copyFileToLibertyServerRoot("additionalVHosts.xml");
        server.setServerConfigurationFile("proxyredirect/server.xml");
        server.startServer("SSLRedirectTest.console.log", true);

        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report it was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2200I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2201I")); //Hiroko-Kristen

        assertNotNull("Application should bind to httpOnly virtual host",
                      server.waitForStringInLog("CWWKT0016I.*(httpOnly)"));
        assertNotNull("Application should bind to httpsOnly virtual host",
                      server.waitForStringInLog("CWWKT0016I.*(httpsOnly)"));
        assertNotNull("Application should bind to alternate virtual host",
                      server.waitForStringInLog("CWWKT0016I.*(alternate)"));

        assertNotNull("The default http port should open: " + httpDefault,
                      server.waitForStringInLog("CWWKO0219I.* " + httpDefault));
        assertNotNull("The default https port should open: " + httpsDefault,
                      server.waitForStringInLog("CWWKO0219I.* " + httpsDefault));
        assertNotNull("The httpFAT_1 should open: " + httpFAT_1,
                      server.waitForStringInLog("CWWKO0219I.* " + httpFAT_1));
        assertNotNull("The httpFAT_2 port should open: " + httpFAT_2,
                      server.waitForStringInLog("CWWKO0219I.* " + httpFAT_2));
        assertNotNull("The httpFAT_3 port should open: " + httpFAT_3,
                      server.waitForStringInLog("CWWKO0219I.* " + httpFAT_3));
        assertNotNull("The httpFAT_4 port should open: " + httpFAT_4,
                      server.waitForStringInLog("CWWKO0219I.* " + httpFAT_4));

        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (server.isStarted())
                server.stopServer(true);
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
            server.deleteFileFromLibertyInstallRoot("additionalVHosts.xml");
        }
    }

    /**
     * Test the user constraint CONFIDENTIAL with BasicAuth, it should redirect from http port to the https port.
     * Then test that a second request from http port will also be redirected to https port,
     * and a direct access to https port is successful.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSSLRedirect_BA() throws Exception {
        driveSSLRedirect_BA("testSSLRedirect_BA - default", hostName, httpDefault, httpsDefault);
        driveSSLRedirect_BA("testSSLRedirect_BA - alternate", hostName, httpFAT_3, httpFAT_4);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSLDefaultProxyRedirect_BA() throws Exception {
        driveSSLRedirect_BA("testSSLDefaultProxyRedirect_BA - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_default, proxyHttpsFAT_default, null);
        driveSSLRedirect_BA("testSSLDefaultProxyRedirect_BA - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_default, proxyHttpsFAT_default, null);
    }

    @Test
    public void testSSLSpecifiedProxyRedirect_BA() throws Exception {
        try {
            // tests using a wildcard host name in the config
            server.setServerConfigurationFile("proxyredirect/server1.xml");
            server.waitForStringInLogUsingLastOffset("CWWKG0017I");
            driveSSLRedirect_BA("testSSLSpecifiedProxyRedirect_BA - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_1, proxyHttpsFAT_1, null);
            driveSSLRedirect_BA("testSSLSpecifiedProxyRedirect_BA - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_1, proxyHttpsFAT_1, null);

            // tests that the httpProxyRedirect updates appropriately and
            // that a specific host name in the config works
            server.setServerConfigurationFile("proxyredirect/server2.xml");
            server.waitForStringInLogUsingLastOffset("CWWKG0017I");
            driveSSLRedirect_BA("testSSLSpecifiedProxyRedirect_BA - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_2, proxyHttpsFAT_2, null);
            driveSSLRedirect_BA("testSSLSpecifiedProxyRedirect_BA - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_2, proxyHttpsFAT_2, null);

            // tests that a disabled httpProxyRedirect does not work
            driveSSLRedirect_BA("testSSLSpecifiedProxyRedirect_BA - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_3, proxyHttpsFAT_3, 403);
            driveSSLRedirect_BA("testSSLSpecifiedProxyRedirect_BA - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_3, proxyHttpsFAT_3, 403);
        } finally {
            server.setServerConfigurationFile("proxyredirect/server.xml");
            server.waitForStringInLogUsingLastOffset("CWWKG0017I");
        }
    }

    void driveSSLRedirect_BA(String methodName, String hostName, int httpPort, int httpsPort) throws Exception {
        driveSSLRedirect_BA(methodName, hostName, httpPort, httpsPort, hostName, httpPort, httpsPort, null);
    }

    void driveSSLRedirect_BA(String methodName, String hostName, int httpPort, int httpsPort, String proxyHostName, int proxyHttpPort, int proxyHttpsPort,
                             Integer expectedFailureCode) throws Exception {
        Log.info(thisClass, methodName, "Entering test " + methodName + " with hostName=" + hostName + ", http=" + httpPort + ", https=" + httpsPort +
                                        ", proxyHostName=" + proxyHostName + ", proxyHttp=" + proxyHttpPort + ", proxyHttps=" + proxyHttpsPort);
        String hostHeaderValue = proxyHostName + ":" + proxyHttpPort;

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONN_TIMEOUT);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                                                           new UsernamePasswordCredentials(employeeUser, employeePassword));

        String queryString = "http://" + hostName + ":" + httpPort + BasicAuthClient.DEFAULT_CONTEXT_ROOT + BasicAuthClient.SSL_SECURED_SIMPLE;
        String sslQueryString = "https://" + hostName + ":" + httpsPort + BasicAuthClient.DEFAULT_CONTEXT_ROOT
                                + BasicAuthClient.SSL_SECURED_SIMPLE;

        // Setup for SSL for HTTP port (not HTTPS port)
        SSLHelper.establishSSLContext(httpClient, httpPort, null);

        try {

            Log.info(thisClass, methodName, "queryString = " + queryString);
            sendRequestAndValidate(methodName, httpClient, queryString, hostHeaderValue, expectedFailureCode);
        } catch (Exception e) {
            httpException(methodName, e, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
        }

        try {
            // access http port the second time.  Verify that it is redirected to https port again (defect 55651)
            Log.info(thisClass, methodName, "Access http port again, queryString:" + queryString);
            sendRequestAndValidate(methodName, httpClient, queryString, hostHeaderValue, expectedFailureCode);
        } catch (Exception e) {
            httpException(methodName, e, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
        }

        //no need to access via https directly if expecting an error on the redirect
        if (expectedFailureCode == null) {
            try {
                // access via https directly using sslQueryString
                Log.info(thisClass, methodName, "Access https port directly, queryString:" + sslQueryString);
                sendRequestAndValidate(methodName, httpClient, sslQueryString, hostHeaderValue, expectedFailureCode);
            } catch (Exception e) {
                httpException(methodName, e, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
            }
        }

        //Check for plain text and encoded passwords in trace
        LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);
        passwordChecker.checkForPasswordInAnyFormat(employeePassword);

        Log.info(thisClass, methodName, "Exiting test " + methodName);

    }

    /**
     * Test the user constraint CONFIDENTIAL with Form Login, it should redirect from http port to the https port.
     * Then test that a second request from http port will also be redirected to https port,
     * and a direct access to https port is successful.
     */
    @Test
    public void testSSLRedirect_FL() throws Exception {
        driveRedirect_FL("testSSLRedirect_FL - default", hostName, httpDefault, httpsDefault);
        driveRedirect_FL("testSSLRedirect_FL - alternate", hostName, httpFAT_3, httpFAT_4);
    }

    @Test
    public void testSSLDefaultProxyRedirect_FL() throws Exception {
        driveRedirect_FL("testSSLDefaultProxyRedirect_FL - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_default, proxyHttpsFAT_default, null);
        driveRedirect_FL("testSSLDefaultProxyRedirect_FL - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_default, proxyHttpsFAT_default, null);
    }

    @Test
    public void testSSLSpecifiedProxyRedirect_FL() throws Exception {
        try {
            // tests using a wildcard host name in the config
            server.setServerConfigurationFile("proxyredirect/server1.xml");
            server.waitForStringInLogUsingLastOffset("CWWKG0017I");
            driveRedirect_FL("testSSLSpecifiedProxyRedirect_FL - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_1, proxyHttpsFAT_1, null);
            driveRedirect_FL("testSSLSpecifiedProxyRedirect_FL - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_1, proxyHttpsFAT_1, null);

            // tests that the httpProxyRedirect updates appropriately and
            // that a specific host name in the config works
            server.setServerConfigurationFile("proxyredirect/server2.xml");
            server.waitForStringInLogUsingLastOffset("CWWKG0017I");
            driveRedirect_FL("testSSLSpecifiedProxyRedirect_FL - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_2, proxyHttpsFAT_2, null);
            driveRedirect_FL("testSSLSpecifiedProxyRedirect_FL - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_2, proxyHttpsFAT_2, null);

            // tests that a disabled httpProxyRedirect does not work
            driveRedirect_FL("testSSLSpecifiedProxyRedirect_FL - default", hostName, httpDefault, httpsDefault, proxyHostName, proxyHttpFAT_3, proxyHttpsFAT_3, 403);
            driveRedirect_FL("testSSLSpecifiedProxyRedirect_FL - alternate", hostName, httpFAT_3, httpFAT_4, proxyHostName, proxyHttpFAT_3, proxyHttpsFAT_3, 403);
        } finally {
            server.setServerConfigurationFile("proxyredirect/server.xml");
            server.waitForStringInLogUsingLastOffset("CWWKG0017I");
        }
    }

    private void driveRedirect_FL(String methodName, String hostName, int httpPort, int httpsPort) {
        driveRedirect_FL(methodName, hostName, httpPort, httpsPort, hostName, httpPort, httpsPort, null);
    }

    private void driveRedirect_FL(String methodName, String hostName, int httpPort, int httpsPort, String proxyHostName, int proxyHttpPort, int proxyHttpsPort,
                                  Integer expectedFailureCode) {
        Log.info(thisClass, methodName, "Entering test " + methodName + " with hostName=" + hostName + ", http=" + httpPort + ", https=" + httpsPort +
                                        ", proxyHostName=" + proxyHostName + ", proxyHttp=" + proxyHttpPort + ", and proxyHttps=" + proxyHttpsPort);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONN_TIMEOUT);
        String hostHeaderValue = proxyHostName + ":" + proxyHttpPort;
        String queryString = "http://" + hostName + ":" + httpPort + FormLoginClient.DEFAULT_CONTEXT_ROOT + FormLoginClient.SSL_SECURED_SIMPLE;
        String sslQueryString = "https://" + hostName + ":" + httpsPort + FormLoginClient.DEFAULT_CONTEXT_ROOT
                                + FormLoginClient.SSL_SECURED_SIMPLE;

        Log.info(thisClass, methodName, "queryString = " + queryString);

        // Setup for SSL for HTTP port (not HTTPS port)
        SSLHelper.establishSSLContext(httpClient, httpPort, null);

        // access the form login page
        Map<String, String> addlHeaders = new HashMap<String, String>();
        if (!hostName.equals(proxyHostName)) {
            addlHeaders.put("Host", hostHeaderValue);
        }
        FormLoginClient flClient = new FormLoginClient(server, FormLoginClient.SSL_SECURED_SIMPLE, FormLoginClient.DEFAULT_CONTEXT_ROOT);
        try {
            flClient.accessFormLoginPage(httpClient, queryString, addlHeaders, expectedFailureCode == null ? 200 : expectedFailureCode);
        } catch (Throwable t) {
            if (expectedFailureCode == null) {
                httpException(methodName, t, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
                // if we didn't fail, that means we're simulating a proxy that doesn't exist -
                // but we still need to get the real cookies on our httpclient, so we'll re-submit the request as if there were no proxy
                flClient.accessFormLoginPage(httpClient, queryString, null, 200);
            }
        }
        // if we are testing a failure path and we received the expected failure code
        // in the accessFormLoginPage call above,then we don't need to do any further testing.
        if (expectedFailureCode == null) {
            String location = null;
            try {
                // use Post method to log in to form login JSP page
                HttpPost postMethod = new HttpPost(queryString + "/j_security_check");
                if (hostHeaderValue != null) {
                    Log.info(thisClass, "sendRequestAndValidate", "setting Host header to: " + hostHeaderValue);
                    postMethod.setHeader("Host", hostHeaderValue);
                }

                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("j_username", employeeUser));
                nvps.add(new BasicNameValuePair("j_password", employeePassword));
                postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

                HttpResponse response = httpClient.execute(postMethod);

                assertEquals("Expecting form login getStatusCode 302", 302,
                             response.getStatusLine().getStatusCode());

                Log.info(thisClass, methodName, "response status: " + response.getStatusLine().toString());

                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity);
                Log.info(thisClass, methodName, "response: " + content);
                EntityUtils.consume(entity);

                // redirect location from login JSP
                Header header = response.getFirstHeader("Location");
                location = header.getValue();
            } catch (Exception e) {
                httpException(methodName, e, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
            }
            assertNotNull("Invalid location", location);

            try {
                Log.info(thisClass, methodName, "Redirect location: " + location);
                if (httpsPort != proxyHttpsPort) {
                    // need to swizzle the location so that it points to the real location (host/httpPort)
                    // of the server, and not the spoofed location of the proxy.
                    // Basically, we're acting like a proxy here...
                    int x = location.indexOf("://") + 3;
                    int y = location.indexOf('/', x + 1);
                    location = location.substring(0, x) + hostName + ":" + httpsPort + location.substring(y);
                    Log.info(thisClass, methodName, "Swizzled redirect location: " + location);
                }
                sendRequestAndValidate(methodName, httpClient, location, hostHeaderValue, expectedFailureCode);
            } catch (Exception e) {
                httpException(methodName, e, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
            }
            try {
                // access http port the second time.  Verify that it is redirected to https port again (defect 55651)
                Log.info(thisClass, methodName, "Access http port again, queryString = " + queryString);
                sendRequestAndValidate(methodName, httpClient, queryString, hostHeaderValue, expectedFailureCode);
            } catch (Exception e) {
                httpException(methodName, e, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
            }

            try {
                // access via https directly using sslQueryString
                Log.info(thisClass, methodName, "Access https port directly, queryString = " + sslQueryString);
                sendRequestAndValidate(methodName, httpClient, sslQueryString, hostHeaderValue, expectedFailureCode);

            } catch (Exception e) {
                httpException(methodName, e, httpPort, httpsPort, proxyHostName, proxyHttpPort, proxyHttpsPort);
            }
        }

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * @param httpClient
     * @param queryString
     * @throws IOException
     * @throws ClientProtocolException
     */
    private void sendRequestAndValidate(String methodName, DefaultHttpClient httpClient, String queryString, String hostHeaderValue,
                                        Integer expectedFailureCode) throws IOException, ClientProtocolException {
        HttpResponse response;
        String content;
        HttpGet getMethod;
        getMethod = new HttpGet(queryString);
        if (hostHeaderValue != null) {
            Log.info(thisClass, "sendRequestAndValidate", "setting Host header to: " + hostHeaderValue);
            getMethod.setHeader("Host", hostHeaderValue);
        }

        response = httpClient.execute(getMethod);
        Log.info(thisClass, methodName, "response: " + response.toString());
        Log.info(thisClass, methodName, "response headers: " + toString(response.getAllHeaders()));
        Log.info(thisClass, methodName, "response status: " + response.getStatusLine().toString());

        content = EntityUtils.toString(response.getEntity());
        Log.info(thisClass, methodName, "Servlet content: " + content);
        String[] matchArray = content.split("cookie: LtpaToken2");
        if (matchArray != null) {
            assertTrue("The response should contain only 1 LTPA cookie, but it contains " + (matchArray.length - 2), matchArray.length < 3);
        }
        EntityUtils.consume(response.getEntity());

        // validate the request was redirected to https URL
        if (expectedFailureCode == null) {
            assertTrue("Failed to redirect to https site.\n" + content, content.contains("getRequestURL: https"));
        } else {
            assertEquals("Unexpected response (error) code", (int) expectedFailureCode, response.getStatusLine().getStatusCode());
        }
    }

    private String toString(Header[] headers) {
        StringBuilder sb = new StringBuilder("[");
        for (Header h : headers) {
            sb.append(LS).append(h.getName()).append(": ").append(h.getValue());
        }
        sb.append(LS).append("]");
        return sb.toString();
    }

    private void httpException(String methodName, Throwable t, int httpPort, int httpsPort, String proxyHttpHost, int proxyHttpPort, int proxyHttpsPort) {
        String msg = t.getMessage();
        Log.info(thisClass, methodName, "httpException: " + msg + " httpPort:" + httpPort + " httpsPort:" + httpsPort + " proxyHost:" + proxyHttpHost + " proxyHttp:"
                                        + proxyHttpPort + " proxyHttps:" + proxyHttpsPort,
                 t);
        t.printStackTrace();
        if (proxyHttpPort != httpPort) { // if using proxy, and no proxy exists,then we expect an exception
            assertTrue("Expected exception does not indicate a failure to reach a (missing) proxy server",
                       msg.contains(proxyHttpHost) && msg.contains(":" + proxyHttpsPort));
        } else {
            if (t instanceof AssertionFailedError) {
                throw (AssertionFailedError) t;
            } else {
                fail("Unexpected exception: " + t);
            }
        }
    }
}
