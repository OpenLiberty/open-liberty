/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.token;

import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import test.common.SharedOutputManager;

/**
 *
 */
public class TokenResponseValidatorTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected final OidcClientConfig oidcClientConfig = mock.mock(OidcClientConfig.class, "oidcClientConfig");
    protected final JwtContext jwtContext = mock.mock(JwtContext.class, "jwtContext");
    private static final String idToken = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk";

    static JwtClaims jwtClaims = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        jwtClaims = JwtParsingUtils.parseJwtWithoutValidation(idToken).getJwtClaims();

    }

    /**
     * Make sure we do the correct error message for a null jwtContext
     */
    @Test
    public void test_getJwtClaimsFromIdTokenContext_NullContext() {
        final String methodName = "test_getJwtClaimsFromIdTokenContext_NullContext";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });

            TokenResponseValidator validator = getTokenResponseValidotor();

            validator.getJwtClaimsFromIdTokenContext(null);

        } catch (TokenValidationException e) {
            String error = e.getMessage();
            assertTrue("message", error.contains("CWWKS2425E"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Make sure we do the correct error message for a valid jwtContext with a null jwtClaims
     */
    @Test
    public void test_getJwtClaimsFromIdTokenContext_NullClaims() {
        final String methodName = "test_getJwtClaimsFromIdTokenContext_NullClaims";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                    allowing(jwtContext).getJwtClaims();
                    will(returnValue(null));
                }
            });

            TokenResponseValidator validator = getTokenResponseValidotor();

            validator.getJwtClaimsFromIdTokenContext(jwtContext);

        } catch (TokenValidationException e) {
            String error = e.getMessage();
            assertTrue("message", error.contains("CWWKS2425E"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * No error message for an existing jwtContext with jwtClaims
     */
    @Test
    public void test_getJwtClaimsFromIdTokenContext() {
        final String methodName = "test_getJwtClaimsFromIdTokenContext";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                    allowing(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                }
            });

            TokenResponseValidator validator = getTokenResponseValidotor();

            validator.getJwtClaimsFromIdTokenContext(jwtContext);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private TokenResponseValidator getTokenResponseValidotor() {
        return new TokenResponseValidator(oidcClientConfig, null, null);
    }
}
