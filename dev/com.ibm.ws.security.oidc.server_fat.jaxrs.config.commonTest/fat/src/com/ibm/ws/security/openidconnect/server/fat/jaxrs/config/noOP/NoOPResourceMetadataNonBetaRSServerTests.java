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
import org.junit.Test;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
/**
 * Runs the protected-resource-metadata tests against a standard (non-beta) RS server —
 * one whose {@code jvm.options} does <em>not</em> contain
 * {@code -Dcom.ibm.ws.beta.edition=true}.
 *
 * <p>Because the {@code protectedResourceMetadata} feature is beta-fenced, the
 * {@code <protectedResourceMetadata>} element in the server XML is silently ignored
 * at runtime.  Consequently the {@code resource_metadata} parameter MUST NOT appear
 * in the {@code WWW-Authenticate} header of any {@code HTTP 401} response, even though
 * the element is present in the configuration.
 *
 * <p>Server used: {@code com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver}
 * (standard, no beta flag).
 * Config file: {@code server_resourceMetadata_tests.xml}.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NoOPResourceMetadataNonBetaRSServerTests extends NoOPResourceMetadataRSServerTests {

    private static final Class<?> thisClass = NoOPResourceMetadataNonBetaRSServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test - non-beta RS server");
        // Use the standard (non-beta) RS server
        commonSetupBeforeTest(RSServerName, "server_resourceMetadata_tests.xml");
    }

    /**
     * <b>Non-beta server – no Bearer token – RS must return 401 with {@code WWW-Authenticate}
     * but WITHOUT the RFC 9728 {@code resource_metadata} parameter (feature is beta-fenced).</b>
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
    public void NoOPResourceMetadataNonBeta_noToken_returns401WithoutResourceMetadata() throws Exception {
        doNoToken401Test("NoOPResourceMetadataNonBeta_noToken_returns401WithoutResourceMetadata");
    }
    
    /**
     * On a non-beta server the {@code protectedResourceMetadata} element is ignored.
     * The {@code resource_metadata} parameter MUST NOT appear in the
     * {@code WWW-Authenticate} header.
     */
    @Override
    protected void addResourceMetadataExpectations(List<validationData> expectations) throws Exception {
        // Verify that the WWW-Authenticate header (identified by its scheme prefix) does not
        // carry any resource_metadata parameter — a single regex covers both facts together.
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_HEADER, Constants.STRING_DOES_NOT_MATCH,
                "WWW-Authenticate header must NOT contain resource_metadata on a non-beta server.",
                null, Constants.RESPONSE_HEADER_WWWAUTHENTICATE.toUpperCase() + ".*" + Constants.RESOURCE_METADATA);
    }
}
