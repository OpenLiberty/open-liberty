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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.BasicRegistry;
import com.ibm.websphere.simplicity.config.BasicRegistry.Group.Member;
import com.ibm.websphere.simplicity.config.BasicRegistry.User;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import appsecurity.AppsecurityBean;
import appsecurity.AppsecurityServlet;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class AppsecurityTest extends FATServletClient {

    private static final String SERVER_NAME = "checkpointAppSecurity";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String APP_NAME = "appsecurity";

    private static final String TCP_CHANNEL_STARTED = "CWWKO0219I:.*defaultHttpEndpoint-ssl";

    private TestMethod testMethod;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() //
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly()) //
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    @BeforeClass
    public static void createAppAndExportToServer() throws Exception {
        Package pkg = AppsecurityServlet.class.getPackage();
        WebArchive appsecurityApp = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClass(AppsecurityServlet.class)
                        .addClass(AppsecurityBean.class)
                        .addPackages(true, pkg)
                        .addAsWebResource(new File("test-applications/appsecurity/resources/index.html"))
                        .addAsWebResource(new File("test-applications/appsecurity/resources/welcome.html"))
                        .addAsWebResource(new File("test-applications/appsecurity/resources/error.html"))
                        .addAsWebResource(new File("test-applications/appsecurity/resources/error403.html"))
                        .addAsWebInfResource(new File("test-applications/appsecurity/resources/WEB-INF/web.xml"));
        ShrinkHelper.exportAppToServer(server, appsecurityApp);
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: Loading Web Module: " + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application appsecurity started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + APP_NAME + " started", 0));
                                 configureBeforeRestore();
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        assertNotNull("Expected CWWKO0219I message not found", server.waitForStringInLog(TCP_CHANNEL_STARTED));
    }

    private void configureBeforeRestore() {
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testUpdateInAuthentication:
                    updateBasicRegistryUserPassword("bob", "newbobpwd");
                    break;
                case testUpdateInAuthorization:
                    updateBasicRegistryUserGroup("Employee", "dave");
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }

    private void updateBasicRegistryUserGroup(String group, String memberName) throws Exception {
        // update group by adding new member before restore
        ServerConfiguration config = server.getServerConfiguration();
        BasicRegistry registry = config.getBasicRegistries().getById("basic");
        Member member = new Member();
        member.setName(memberName);
        registry.getGroups().getById(group).getMembers().add(member);
        server.updateServerConfiguration(config);
    }

    private void updateBasicRegistryUserPassword(String name, String newPassword) throws Exception {
        // change password of a user before restore
        ServerConfiguration config = server.getServerConfiguration();
        BasicRegistry registry = config.getBasicRegistries().getById("basic");
        User user = registry.getUsers().getById(name);
        user.setPassword(newPassword);
        server.updateServerConfiguration(config);
    }

    @Test
    public void testAuthorizationForAdmin() throws Exception {
        executeURL("/", "admin", "bob", "bobpwd", false,
                   HttpServletResponse.SC_OK, "admin, user");
    }

    @Test
    public void testAuthorizationForUser() throws Exception {
        executeURL("/", "user", "alice", "alicepwd", false,
                   HttpServletResponse.SC_OK, "user");
    }

    @Test
    public void testAuthorizationFail() throws Exception {
        executeURL("/", "user", "dave", "davepwd", false,
                   HttpServletResponse.SC_FORBIDDEN, "Error 403: Authorization failed");
    }

    @Test
    public void testAuthenticationFail() throws Exception {
        executeURL("/", "admin", "bob", "wrongpassword", true, -1, "Don't care");
    }

    @Test
    public void testUpdateInAuthentication() throws Exception {
        executeURL("/", "admin", "bob", "newbobpwd", false,
                   HttpServletResponse.SC_OK, "admin, user");
    }

    @Test
    public void testUpdateInAuthorization() throws Exception {
        executeURL("/", "user", "dave", "davepwd", false,
                   HttpServletResponse.SC_OK, "user");
    }

    private void executeURL(
                            String testUrl, String group, String userid, String password,
                            boolean expectLoginFail, int expectedCode, String expectedContent) throws Exception {

        // Use HttpClient to execute the testUrl by HTTP
        URI url = new URI("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + testUrl);
        HttpGet getMethod = new HttpGet(url);
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();

        initializeSSLContext();
        SSLContext sslContext = SSLContext.getDefault();
        clientBuilder.setSSLContext(sslContext);
        clientBuilder.setDefaultRequestConfig(
                                              RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build());
        HttpClient client = clientBuilder.build();
        HttpResponse response = client.execute(getMethod);

        // Response should be index.html
        String loginBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        assertTrue("Not redirected to index.html", loginBody.contains("window.location.assign"));
        String[] redirect = loginBody.split("'");

        // Use j_security_check to login
        HttpPost postMethod = new HttpPost("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/j_security_check");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("j_username", userid));
        nvps.add(new BasicNameValuePair("j_password", password));
        postMethod.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        response = client.execute(postMethod);
        assertEquals("Expected " + expectedCode + " status code for login", HttpServletResponse.SC_FOUND,
                     response.getStatusLine().getStatusCode());

        if (expectLoginFail) {
            String location = response.getFirstHeader("Location").getValue();
            assertTrue("Error.html was not returned", location.contains("error.html"));
            return;
        }

        // Use HttpClient to execute the redirected url
        url = new URI("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + redirect[1]);
        getMethod = new HttpGet(url);
        response = client.execute(getMethod);
        assertEquals("Expected " + expectedCode + " status code for login", expectedCode, response.getStatusLine().getStatusCode());

        String actual = "";
        switch (expectedCode) {
            case HttpServletResponse.SC_OK:
                // Check the content of the response returned
                actual = EntityUtils.toString(response.getEntity(), "UTF-8");
                assertTrue("The url " + testUrl + " did not return the expected content",
                           actual.contains("GROUP: " + group + ", NAME: " + userid + ", ROLES: " + expectedContent));
                break;

            case HttpServletResponse.SC_FORBIDDEN:
                actual = EntityUtils.toString(response.getEntity(), "UTF-8");
                assertTrue("The url " + testUrl + " did not return the expected content \""
                           + expectedContent + "\"" + "The actual content was:\n" + actual, actual.contains(expectedContent));
                break;

            default:
                break;

        }
    }

    private static void initializeSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(
                        null,
                        new TrustManager[] {
                                             new X509TrustManager() {
                                                 @Override
                                                 public void checkClientTrusted(X509Certificate[] arg0,
                                                                                String arg1) {
                                                 }

                                                 @Override
                                                 public void checkServerTrusted(X509Certificate[] arg0,
                                                                                String arg1) {
                                                 }

                                                 @Override
                                                 public X509Certificate[] getAcceptedIssuers() {
                                                     return null;
                                                 }
                                             }
                        },
                        new SecureRandom());
        SSLContext.setDefault(sslContext);
        HttpsURLConnection.setDefaultSSLSocketFactory(
                                                      sslContext.getSocketFactory());
    }

    @After
    public void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.restoreServerConfiguration();
        }
    }

    static enum TestMethod {
        testAuthorizationForAdmin,
        testAuthorizationForUser,
        testAuthorizationFail,
        testAuthenticationFail,
        testUpdateInAuthentication,
        testUpdateInAuthorization,
        unknown
    }

}
