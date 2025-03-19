/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.TestConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * Performs RunAs tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class RunAsTest {

    private static final Class<?> thisClass = RunAsTest.class;
    private static final String USER1 = "user1";
    private static final String USER2 = "user2";
    private static final String USER2PWD = "user2pwd";
    private static final String USER3 = "user3";
    private static final String USER5 = "user5";
    private static final String USER6 = "user6";
    private static final String USER1PWD = "user1pwd";
    private static final String WAR_APP_NAME = "delegation";
    private static final String EAR_APP_NAME = "delegationXML";
    private static final String DEFAULT_CONFIG_FILE = "delegation.server.orig.xml";
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.delegation");
    private static BasicAuthClient basicAuthClient;

    // TestName should be an instance level Rule, not a ClassRule, but static references are made to it,
    // so we will create it as a static field, then keep a reference to it from a non-static field which
    // is annotated with @Rule - junit can make the test method name changes to that field, which should
    // (hopefully) be reflected in the static references as well.
    private static TestName _name = new TestName();

    @Rule
    public TestName name = _name;

    private static final TestConfiguration testConfig = new TestConfiguration(server, thisClass, _name, WAR_APP_NAME);

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
        server.addInstalledAppForValidation(WAR_APP_NAME);
        server.addInstalledAppForValidation(EAR_APP_NAME);

        JACCFatUtils.installJaccUserFeature(server);
        JACCFatUtils.transformApps(server, "delegation.war", "delegationXML.ear");

        testConfig.startServerWithSecurityAndAppStarted(DEFAULT_CONFIG_FILE);

        if (server.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaccFeature(server);
        }

        basicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, "RunAsServlet", "/delegation");
        basicAuthClient.setJaccValidation(true);
    }

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWKS9112W");
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
        }
    }

    @Test
    public void testDelegation() throws Exception {
        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE, WAR_APP_NAME);

        basicAuthClient.resetClientState();
        String response = basicAuthClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServlet", USER1, USER1PWD);

        assertDelegationSubject(response, USER5);
    }

    @Test
    public void testDelegationFallbackToCallerSubject() throws Exception {
        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE, WAR_APP_NAME);

        basicAuthClient.resetClientState();
        String response = basicAuthClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServletUserNotMapped", USER1, USER1PWD);

        assertDelegationSubject(response, USER1);
    }

    @Test
    public void testDelegationEar() throws Exception {
        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE, EAR_APP_NAME);

        BasicAuthClient basicAuthEarClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, "RunAsServlet", "/delegationXML");
        String response = basicAuthEarClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServlet", USER1, USER1PWD);

        assertDelegationSubject(response, USER5);
    }

    @Test
    public void testDelegationUnprotectedFallbackToCallerSubject() throws Exception {
        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE, WAR_APP_NAME);

        basicAuthClient.resetClientState();
        String response = basicAuthClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServlet", USER1, USER1PWD);

        assertDelegationSubject(response, USER5);

        String responseUnprotected = basicAuthClient.accessUnprotectedServlet("/RunAsServletUnprotected");

        assertDelegationSubject(responseUnprotected, USER1);
    }

    /**
     * Server.xml has Manager role with run-as while ibm-application-bnd.xml does not have run-as
     */
    @Test
    public void testDelegationMergeServerXML() throws Exception {
        testConfig.setServerConfiguration("mergeBindingsAndServerXMLRunAs.xml", EAR_APP_NAME);

        BasicAuthClient basicAuthEarClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, "RunAsServlet", "/delegationXML");
        String response = basicAuthEarClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServletUserNotMapped", USER2, USER2PWD);

        assertDelegationSubject(response, USER3);
    }

    /**
     * Employee role in ibm-application-bnd.xm has run-as while server.xml does not have Employee role
     */
    @Test
    public void testDelegationMergeBindings() throws Exception {
        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE, EAR_APP_NAME);

        BasicAuthClient basicAuthEarClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, "RunAsServlet", "/delegationXML");
        String response = basicAuthEarClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServlet", USER1, USER1PWD);

        assertDelegationSubject(response, USER5);
    }

    /**
     * Test a runAs user with no password. Will put a new server.xml file in place and restart the server to be sure the run as
     * caches are cleared.
     */
    @Test
    public void testDelegationNoRunAsPassword() throws Exception {
        testConfig.setServerConfiguration("runAsNoPassword.xml");
        // Because auth cache still has previously logged in user, must restart to clear cache for dynamic update tests
        server.stopServer("CWWKS9112W");
        server.startServer();

        basicAuthClient.resetClientState();
        String response = basicAuthClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServletUserNotMapped", USER2, USER2PWD);

        assertDelegationSubject(response, USER6);
    }

    @Test
    public void testDelegationMergeServerXMLNoRunAsPassword() throws Exception {
        testConfig.setServerConfiguration("mergeBindingsAndServerXMLNoRunAsPassword.xml", EAR_APP_NAME);

        BasicAuthClient basicAuthEarClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, "RunAsServlet", "/delegationXML");
        String response = basicAuthEarClient.accessProtectedServletWithAuthorizedCredentials("/RunAsServletUserNotMapped", USER2, USER2PWD);

        assertDelegationSubject(response, USER3);

        //Check for plain text and encoded passwords in trace
        LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);
        passwordChecker.checkForPasswordInAnyFormat(USER2PWD);
    }

    private void assertDelegationSubject(String response, String user) throws Exception {
        assertTrue("The RunAs subject must be the subject for " + user,
                   response.matches("\\A[\\s\\S]*</br>RunAs subject: Subject:\\s*Principal: WSPrincipal:" + user + "\\s[\\s\\S]*\\z"));
    }

}
