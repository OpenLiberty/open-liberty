/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.security.jaspic;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class JASPIBasicAuthenticationTest extends JASPITestBase {

    private static final Set<String> EE8_FEATURES;
    private static final String[] EE8_FEATURES_ARRAY = {
                                                         "usr:jaspicUserTestFeature-1.0"
    };

    private static final Set<String> EE9_FEATURES;
    private static final String[] EE9_FEATURES_ARRAY = {
                                                         "usr:jaspicUserTestFeature-2.0"
    };

    private static final Set<String> EE10_FEATURES;
    private static final String[] EE10_FEATURES_ARRAY = {
                                                          "usr:jaspicUserTestFeature-3.0"
    };

    static {
        EE8_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE8_FEATURES_ARRAY)));
        EE9_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));
        EE10_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE10_FEATURES_ARRAY)));
    }

    private static final String SERVER = "com.ibm.ws.security.jaspic11.fat";

    private static LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER);
    private static Class<?> logClass = JASPIBasicAuthenticationTest.class;
    private String queryString = "/JASPIBasicAuthServlet/JASPIBasic";
    private static String urlBase;

    private DefaultHttpClient httpclient;

    private static final String TCP_CHANNEL_STARTED = "CWWKO0219I:.*defaultHttpEndpoint-ssl";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER).removeFeatures(EE8_FEATURES).addFeatures(EE9_FEATURES).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER).removeFeatures(EE8_FEATURES).removeFeatures(EE9_FEATURES).addFeatures(EE10_FEATURES).fullFATOnly());

    public JASPIBasicAuthenticationTest() {
        super(server, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(server);
        JASPIFatUtils.transformApps(server, "JASPIBasicAuthServlet.war");

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
        assertNotNull("Expected CWWKO0219I message not found", server.waitForStringInLog(TCP_CHANNEL_STARTED));
        server.addInstalledAppForValidation(DEFAULT_APP);
        verifyServerStartedWithJaspiFeature(server);
        urlBase = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            JASPIFatUtils.uninstallJaspiUserFeature(server);
        }
    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and password in the jaspi_basic role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with an invalid userId and invalid password and verify that JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthBadUser_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_invalidUser, jaspi_invalidPwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + queryString);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login without credentials and verify that JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 Challenge response
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthProtectedNoAuthCreds_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestNoAuthCreds(httpclient, urlBase + queryString, HttpServletResponse.SC_UNAUTHORIZED);
        verifyJaspiAuthenticationProcessedInMessageLog();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
