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
import org.junit.runner.RunWith;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
/**
 * Runs the protected-resource-metadata tests against an RS server started with the
 * {@code -Dcom.ibm.ws.beta.edition=true} JVM flag, which activates the beta-fenced
 * {@code <protectedResourceMetadata>} configuration element.
 *
 * <p>Per RFC 9728, because {@code <protectedResourceMetadata>} is active, every
 * {@code HTTP 401} response returned by the RS (when no Bearer token is presented)
 * MUST include a {@code resource_metadata} parameter in the {@code WWW-Authenticate}
 * header, pointing to the RS's {@code /.well-known/oauth-protected-resource} endpoint.
 *
 * <p>Server used: {@code com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver_beta}
 * (its {@code jvm.options} contains {@code -Dcom.ibm.ws.beta.edition=true}).
 * Config file: {@code server_resourceMetadata_beta_tests.xml}.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class NoOPResourceMetadataBetaRSServerTests extends NoOPResourceMetadataRSServerTests {

    private static final Class<?> thisClass = NoOPResourceMetadataBetaRSServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test - beta RS server");
        // Use the beta RS server whose jvm.options carries -Dcom.ibm.ws.beta.edition=true
        commonSetupBeforeTest(RSServerNameBeta, "server_resourceMetadata_beta_tests.xml");
    }

    /**
     * <b>Beta server – no Bearer token – RS must return 401 with {@code WWW-Authenticate}
     * including the RFC 9728 {@code resource_metadata} parameter.</b>
     *
     * <p><b>Expected results:</b>
     * <ol>
     *   <li>HTTP 401 Unauthorized</li>
     *   <li>{@code WWW-Authenticate} header present, starting with {@code Bearer}</li>
     *   <li>{@code resource_metadata} parameter present and pointing to
     *       {@code .well-known/oauth-protected-resource}</li>
     * </ol>
     */
    @Test
    public void NoOPResourceMetadataBeta_noToken_returns401WithResourceMetadata() throws Exception {
        doNoToken401Test("NoOPResourceMetadataBeta_noToken_returns401WithResourceMetadata");
    }

    /**
     * <b>Beta server – malformed Bearer token – RS must return 401 with {@code WWW-Authenticate}
     * including the RFC 9728 {@code resource_metadata} parameter.</b>
     *
     * <p>RFC 9728 focuses on the missing-token case for the {@code WWW-Authenticate} challenge
     * with {@code resource_metadata}, but the RS also returns a 401 when the token is present
     * but invalid (malformed, expired, wrong signature, etc.).  In that case the {@code WWW-Authenticate}
     * challenge SHOULD still include {@code resource_metadata} so the caller has enough information
     * to obtain a valid token.
     *
     * <p><b>Expected results:</b>
     * <ol>
     *   <li>HTTP 401 Unauthorized</li>
     *   <li>{@code WWW-Authenticate} header present, starting with {@code Bearer}</li>
     *   <li>{@code resource_metadata} parameter present and pointing to
     *       {@code .well-known/oauth-protected-resource}</li>
     * </ol>
     */
    @Test
    @AllowedFFDC("org.apache.http.conn.HttpHostConnectException")
    public void NoOPResourceMetadataBeta_malformedToken_returns401WithResourceMetadata() throws Exception {
        doMalformedToken401Test("NoOPResourceMetadataBeta_malformedToken_returns401WithResourceMetadata");
    }
    
    /**
     * On a beta-fenced server with {@code <protectedResourceMetadata>} configured,
     * the {@code WWW-Authenticate} 401 header MUST contain:
     * <ul>
     *   <li>{@code resource_metadata=} – the RFC 9728 parameter</li>
     *   <li>A URL ending with {@code .well-known/oauth-protected-resource}</li>
     * </ul>
     */
    @Override
    protected void addResourceMetadataExpectations(List<validationData> expectations) throws Exception {
    	// RFC 9728 [3]: WWW-Authenticate must carry Bearer auth-scheme, the resource_metadata
        // parameter, and a value that is the full well-known URL for this RS endpoint — all
        // verified together in a single regex.
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_HEADER, Constants.STRING_MATCHES,
                "WWW-Authenticate header should contain resource_metadata parameter on a beta server.",
                null, Constants.RESPONSE_HEADER_WWWAUTHENTICATE.toUpperCase() + Constants.BEARER + ".*"
                        + Constants.RESOURCE_METADATA + "=\"" + genericTestServer.getHttpsString() + "/"
                        + Constants.PROTECTED_RESOURCE_WELL_KNOWN_URI + "/helloworld/rest/helloworld_resourceMetadata\"");
    }
}
