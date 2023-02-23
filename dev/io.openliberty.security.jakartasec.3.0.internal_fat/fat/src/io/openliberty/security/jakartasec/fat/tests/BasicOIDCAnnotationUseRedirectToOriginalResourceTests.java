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
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.sharedTests.BasicOIDCAnnotationTests;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class BasicOIDCAnnotationUseRedirectToOriginalResourceTests extends BasicOIDCAnnotationTests {

    protected static Class<?> thisClass = BasicOIDCAnnotationUseRedirectToOriginalResourceTests.class;

    @Server("jakartasec-3.0_fat." + useRedirectToOriginalResource + ".jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat." + useRedirectToOriginalResource + ".opaque.rp")
    public static LibertyServer rpOpaqueServer;

    @ClassRule
    public static RepeatTests r = createTokenTypeRepeats(useRedirectToOriginalResource);

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(thisClass, "setUp", "starting setup");

        baseSetup(rpJwtServer, rpOpaqueServer);

        deployMoreApps();
    }

    public static void deployMoreApps() throws Exception {
        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);

        swh.defaultDropinApp(rpServer, "RedirectToOriginalResourceTruePromptNone.war", "oidc.client.redirectToOriginalResourceTruePromptNone.servlets", "oidc.client.base.*");

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * Test with redirectToOriginalResource=true and prompt=none
     * The prompt=none will cause the callback from the authentication endpoint to contain an error, since there is no previous authentication.
     * The RP should not redirect the browser to the original resource, since the callback contains an error.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20Exception", "io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException" })
    @Test
    public void BasicOIDCAnnotationUseRedirectToOriginalResourceTests_callbackHasError() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "RedirectToOriginalResourceTruePromptNoneServlet";
        String url = rpHttpsBase + "/RedirectToOriginalResourceTruePromptNone/" + app;

        Page response = invokeApp(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_DOES_NOT_CONTAIN, url, "Should not have redirected to the original resource when the callback from the authentication endpoint contains an error."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        validationUtils.validateResult(response, expectations);

    }

}
