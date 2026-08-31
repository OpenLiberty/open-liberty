/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Runs the protected-resource-metadata tests against a RS server whose
 * {@code openidConnectClient} deliberately omits the
 * {@code <protectedResourceMetadata>} sub-element.
 *
 * <p>This class documents the following behaviour:
 * <ul>
 *   <li>metadata element absent: {@code resource_metadata} NOT in challenge</li>
 * </ul>
 *
 * <p>Server used: {@code com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver}
 * Config file: {@code server_resourceMetadata_no_metadata_tests.xml}, which imports
 * {@code oidcClient_RSResourceMetadata_noConfig.xml} (no {@code <protectedResourceMetadata>} element).
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NoOPNoResourceMetadataRSServerTests extends NoOPResourceMetadataRSServerBaseTests {

    private static final Class<?> thisClass = NoOPNoResourceMetadataRSServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test - RS server with no protectedResourceMetadata config");
        // Use the RS server with the "no metadata" config (element absent)
        commonSetupBeforeTest(RSServerName, "server_resourceMetadata_no_metadata_tests.xml");
    }

    /**
     * <b>No {@code <protectedResourceMetadata>} – no Bearer token – RS must return
     * 401 with {@code WWW-Authenticate} but WITHOUT the RFC 9728 {@code resource_metadata}
     * parameter, because the element is not configured.</b>
     *
     * <p><b>Expected results:</b>
     * <ol>
     *   <li>HTTP 401 Unauthorized</li>
     *   <li>{@code WWW-Authenticate} header present, starting with {@code Bearer}</li>
     *   <li>{@code CWWKS1726E} in RS server log (local rejection, no OP contact)</li>
     *   <li>{@code resource_metadata} parameter absent</li>
     * </ol>
     */
    @Test
    public void NoOPNoResourceMetadata_noToken_returns401WithoutResourceMetadata() throws Exception {
        doNoTokenNoMetadata401Test("NoOPNoResourceMetadata_noToken_returns401WithoutResourceMetadata");
    }

    /**
     * Variant of {@link #doNoToken401Test(String)} that targets the
     * {@code helloworld_noMetadata} URL pattern configured by
     * {@code oidcClient_RSResourceMetadata_noConfig.xml}.
     */
    private void doNoTokenNoMetadata401Test(String testName) throws Exception {
        Log.info(thisClass, testName,
                "Starting test - RS (no metadata config) should return 401 without resource_metadata");

        TestSettings updatedTestSettings =
                rsTools.updateRSProtectedResource(testSettings, "helloworld_noMetadata");

        // RFC 6750 [3.1]: 401 when no token is presented with inboundPropagation=required
        List<validationData> expectations =
                validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);

        // RFC 6750 [3]: WWW-Authenticate header must be present and use the Bearer auth-scheme
        String expectedAuthHeader = WWW_AUTHENTICATE_HEADER.toUpperCase() + ": " + Constants.BEARER;
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                "Response did not include a WWW-Authenticate header with Bearer auth-scheme.",
                null, expectedAuthHeader);

        // resource_metadata must be absent when the element is not configured
        addResourceMetadataExpectations(expectations);

        Log.info(thisClass, testName, "Invoking RS protected resource without any Bearer token");
        helpers.invokeRSProtectedResource(_testName, Constants.POSTMETHOD, null, null,
                updatedTestSettings, expectations);

        Log.info(thisClass, testName,
                "Test completed - verified 401 without resource_metadata (element not configured)");
    }

    /**
     * On this server the {@code <protectedResourceMetadata>} element is absent.
     * The {@code resource_metadata} parameter MUST NOT appear in the
     * {@code WWW-Authenticate} header.
     */
    @Override
    protected void addResourceMetadataExpectations(List<validationData> expectations) throws Exception {
        // The element is not configured.
        // Verify the WWW-Authenticate header does not carry any resource_metadata parameter.
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_HEADER, Constants.STRING_DOES_NOT_MATCH,
                "WWW-Authenticate header must NOT contain resource_metadata when the element is absent.",
                null, Constants.RESPONSE_HEADER_WWWAUTHENTICATE.toUpperCase() + ".*" + Constants.RESOURCE_METADATA);
    }
}
