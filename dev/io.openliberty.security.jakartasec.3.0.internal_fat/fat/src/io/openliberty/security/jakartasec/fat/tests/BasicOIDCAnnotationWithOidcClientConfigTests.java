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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.sharedTests.BasicOIDCAnnotationTests;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;
import io.openliberty.security.jakartasec.fat.utils.WsSubjectExpectationHelpers;

/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class BasicOIDCAnnotationWithOidcClientConfigTests extends BasicOIDCAnnotationTests {

    protected static Class<?> thisClass = BasicOIDCAnnotationWithOidcClientConfigTests.class;

    @Server("jakartasec-3.0_fat." + withOidcClientConfig + ".jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat." + withOidcClientConfig + ".opaque.rp")
    public static LibertyServer rpOpaqueServer;

    @ClassRule
    public static RepeatTests r = createTokenTypeRepeats(withOidcClientConfig);

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(thisClass, "setUp", "starting setup");

        baseSetup(rpJwtServer, rpOpaqueServer);

        deployMoreApps();
    }

    /**
     * Add another app that will be protected by OIDC clients.
     *
     * @throws Exception
     */
    public static void deployMoreApps() throws Exception {
        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are defined 100% by the source code tree
        swh.dropinAppWithWebXmlAndBnd(rpServer, "SimpleServlet_rpClientProtected.war", "SimpleServlet.war", "oidc.client.simple.*", "oidc.client.base.utils");

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * The tests that are in this class use oidc clients to protect the app. The RP server config will therefore have oidc client configs.
     * We'll show that we're using the proper config to protect the app - the tests from the BasicOIDCAnnotationTests class that this
     * class extends should be protected with the annotations in the apps and not the oidc clients.
     *
     */
    /**
     * Test that we can access an app protected by an oidc client using a filter
     * OP should be OP2 and client should be client_99
     *
     * @throws Exception
     */
    @Test
    public void BasicOIDCAnnotationWithOidcClientConfigTests_oidcClientProtected() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "rpClientProtected";
        String url = rpHttpsBase + "/SimpleServlet_rpClientProtected/" + app;
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/OP2");
        rspValues.setUsingJakarta(false);

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        // confirm protected resource was accessed
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, url, "Did not land on the RP Protected test application."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "\"client_99\"", "Did not find the proper client which should be client_99."));
        WsSubjectExpectationHelpers.getWsSubjectExpectations(null, expectations, ServletMessageConstants.SERVLET, rspValues);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test that we can access an app protected by an oidc client whose filter does not exclude the app. The config
     * protecting this app would also be approprate for all of the apps that use annotations - those tests will show that
     * the annotated apps are NOT protected by the oidc client config that protects this app.
     *
     * @throws Exception
     */
    @Test
    public void BasicOIDCAnnotationWithOidcClientConfigTests_oidcClientProtected_byWildcard() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "rpClientWildcardProtected";
        String url = rpHttpsBase + "/SimpleServlet_rpClientProtected/" + app;
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/OP2");
        rspValues.setUsingJakarta(false);

        //show that we get to the test app without having to log in

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        // confirm protected resource was accessed
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_CONTAINS, url, "Did not land on the RP Protected test application."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "\"client_98\"", "Did not find the proper client which should be client_98."));
        WsSubjectExpectationHelpers.getWsSubjectExpectations(null, expectations, ServletMessageConstants.SERVLET, rspValues);
        validationUtils.validateResult(response, expectations);

    }

}
