/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwtFatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt11MPConfigTests;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run tests validate the correct behavior with unique jwk cache conditions.
 * <OL>
 * <LI>Setup 2 servers - one to act as the jwtBuilder and another to consume the tokens using mpJwt.
 * <OL>
 * <LI>
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtJwkTokenCacheTests extends MPJwt11MPConfigTests {

    protected static Class<?> thisClass = MPJwtBasicTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.builder")
    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    /**
     * Startup the builder and resource servers
     * Set flag to tell the code that runs between tests NOT to restore the server config between tests
     * (very few of the tests use the config that the server starts with - this setting will save run time)
     * Build the list of app urls and class names that each test will use
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_jwkCacheTests_1.xml", true, true);

        setUpAndStartRSServerForApiTests(resourceServer, jwtBuilderServer, "rs_server_orig_withAudience.xml", true, "defaultJWT_withAudience");

        skipRestoreServerTracker.addServer(resourceServer);

    }

    /**
     *
     * Test that we can consume a jwt token when we only have one entry in the jwk cache.
     * We ran into a case where we were creating a jwt and when we tried to consume it, we received a signature validation failure.
     * When the token was consumed, the jwk token cache was populated with 1 entry and then further along, we to process a second
     * token. We don't find an entry matching the second kid in the cache and other values to check against are not set, so
     * we ended up falling into getKeyBySetId which will return the 1 and only cache entry - which in this case is not
     * correct.
     * To reproduce the problem:
     * This test will start up a server with a jwtBuilder that has jwkEnabled=false. It will create a jwt token (using the key to generate
     * the kid) and then use that token in another server with mpJwt configured (referencing the jwksUri of the builder).
     * It then reconfigures the builder setting jwkEnabled=true. It will create another token (this time using the jwk to generate the kid).
     * When we try to consume this token, we need to show that we don't end up using the 1 cached value - which would result in a
     * signature validation failure.
     * We should be able to successfully access the protected app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtJwkTokenCacheTests_OnlyOneEntryInCache() throws Exception {

        WebClient webClient = actions.createWebClient();

        MPJwtJwkTokenCacheTests_common(webClient);

        // reconfigure the server to use the "same builder", but this time set jwkEnabled to true to force a new kid value
        jwtBuilderServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_jwkCacheTests_2.xml");

        MPJwtJwkTokenCacheTests_common(webClient);

        actions.destroyWebClient(webClient);

    }

    public void MPJwtJwkTokenCacheTests_common(WebClient webClient) throws Exception {

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer);

        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);

        String url = buildAppUrl(resourceServer, MPJwtFatConstants.MICROPROFILE_SERVLET, MPJwtFatConstants.MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE);

        Expectations expectations = goodTestExpectations(jwtTokenTools, url, MPJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_REQUEST_SCOPE);

        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, url, builtToken);
        validationUtils.validateResult(response, expectations);

    }

}
