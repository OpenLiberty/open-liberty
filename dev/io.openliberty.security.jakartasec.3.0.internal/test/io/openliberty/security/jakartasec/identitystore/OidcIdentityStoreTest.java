/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.identitystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Set;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import test.common.SharedOutputManager;

public class OidcIdentityStoreTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final ClaimsMappingConfig claimsMappingConfig = mockery.mock(ClaimsMappingConfig.class);
    private final TokenResponse tokenResponse = mockery.mock(TokenResponse.class);
    private final AccessToken accessToken = mockery.mock(AccessToken.class);
    private final JwtClaims idTokenClaims = mockery.mock(JwtClaims.class);

    private OidcIdentityStore identityStore;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        identityStore = new OidcIdentityStore();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    // TODO - createSuccessfulCredentialValidationResult

    @Test
    public void test_getCallerName_noClaimsMapping() throws MalformedClaimException {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerName(config, accessToken, idTokenClaims);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    // TODO - getCallerName

    @Test
    public void test_getCallerGroups_noClaimsMapping() throws MalformedClaimException {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        Set<String> result = identityStore.getCallerGroups(config, accessToken, idTokenClaims);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    // TODO - getCallerGroups

    @Test
    public void test_getCallerNameClaim_noClaimsMapping() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerNameClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerNameClaim_noClaimConfigured() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerNameClaim();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerNameClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerNameClaim() {
        final String claim = "myCallerNameClaim";
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerNameClaim();
                will(returnValue(claim));
            }
        });
        String result = identityStore.getCallerNameClaim(config);
        assertEquals(claim, result);
    }

    @Test
    public void test_getCallerGroupsClaim_noClaimsMapping() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerGroupsClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerGroupsClaim_noClaimConfigured() {
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerGroupsClaim();
                will(returnValue(null));
            }
        });
        String result = identityStore.getCallerGroupsClaim(config);
        assertNull("Result should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getCallerGroupsClaim() {
        final String claim = "myCallerGroupsClaim";
        mockery.checking(new Expectations() {
            {
                one(config).getClaimsMappingConfig();
                will(returnValue(claimsMappingConfig));
                one(claimsMappingConfig).getCallerGroupsClaim();
                will(returnValue(claim));
            }
        });
        String result = identityStore.getCallerGroupsClaim(config);
        assertEquals(claim, result);
    }
    

    // TODO - getClaimValueFromTokens
    // TODO - getClaimFromAccessToken
    // TODO - getClaimFromIdToken
    // TODO - valueExistsAndIsNotEmpty

}
