/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class IdentityStoreTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = IdentityStoreTests.class;

    protected static ShrinkWrapHelpers swh = null;

    @Server("jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.identityStore.jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.identityStore.opaque.rp")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    // create repeats for opaque and jwt tokens - in lite mode, only run with jwt tokens
    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats(TestMode.LITE, Constants.JWT_TOKEN_FORMAT);

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps();

        // Wait to ensure LTPA configuration has completed before starting tests
        opServer.waitForStringInLog("CWWKS4105I");
        rpServer.waitForStringInLog("CWWKS4105I");

        // rspValues used to validate the app output will be initialized before each test - any unique values (other than the
        //  app need to be updated by the test case - the app is updated by the invokeApp* methods)
    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are defined 100% by the source code tree
        swh.defaultDropinApp(rpServer, "IdentityStore.war", "oidc.client.withIdentityStore.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "MultipleIdentityStore.war", "oidc.client.withMultipleIdentityStore.servlets", "oidc.client.base.*");

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/
    /**
     * Test with an app that includes an IdentityStore. The app uses a role/group that is only provided by the IdentityStore.
     * Access being granted shows that the Identity Store was used to provide that group. (we'll also validate that the log
     * contains a trace message from the Identity Store that shows that the getCallerGroups method was called)
     *
     * @throws Exception
     */
    @Test
    public void IdentityStoreTests_use1IdentityStore() throws Exception {

        WebClient webClient1 = getAndSaveWebClient();
        Page response = runGoodEndToEndTest(webClient1, "IdentityStore", "OidcIdentityStore", Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations extraExpectations = new Expectations();
        extraExpectations.addExpectation(new ServerMessageExpectation(rpServer, "GroupOnlyIdentityStore - getCallerGroups", "Runtime did not invoke \"getCallerGroups\" in the Identity Store."));

        validationUtils.validateResult(response, extraExpectations);

    }

    /**
     * Test with an app that includes multiple IdentityStores. The app uses a role/group that is only provided by the IdentityStore with the lowest priority.
     * Access being granted shows that the proper Identity Store was used to provide that group. (we'll also validate that the log
     * contains trace messages from both test Identity Stores. This shows that the getCallerGroups method in both Identity Store were called)
     *
     * @throws Exception
     */
    @Test
    public void IdentityStoreTests_useMultipleIdentityStores() throws Exception {

        WebClient webClient1 = getAndSaveWebClient();
        Page response = runGoodEndToEndTest(webClient1, "MultipleIdentityStore", "OidcIdentityStore", Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations extraExpectations = new Expectations();
        extraExpectations.addExpectation(new ServerMessageExpectation(rpServer, "GroupOnlyIdentityStore1 - getCallerGroups", "Runtime did not invoke \"getCallerGroups\" in the Identity Store 1."));
        extraExpectations.addExpectation(new ServerMessageExpectation(rpServer, "GroupOnlyIdentityStore2 - getCallerGroups", "Runtime did not invoke \"getCallerGroups\" in the Identity Store 2."));

        validationUtils.validateResult(response, extraExpectations);

    }

}
