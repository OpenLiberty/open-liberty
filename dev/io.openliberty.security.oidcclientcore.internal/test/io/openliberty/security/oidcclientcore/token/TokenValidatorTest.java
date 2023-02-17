/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import test.common.SharedOutputManager;

/**
 *
 */
public class TokenValidatorTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected final OidcClientConfig oidcClientConfig = mock.mock(OidcClientConfig.class, "oidcClientConfig");
    protected final OidcProviderMetadata oidcmd = mock.mock(OidcProviderMetadata.class, "oidcmd");
    private static final String idToken = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOi8vaGFybW9uaWM6ODAxMS9vYXV0aDIvZW5kcG9pbnQvT0F1dGhDb25maWdTYW1wbGUvdG9rZW4iLCJpYXQiOjEzODczODM5NTMsInN1YiI6InRlc3R1c2VyIiwiZXhwIjoxMzg3Mzg3NTUzLCJhdWQiOiJjbGllbnQwMSJ9.ottD3eYa6qrnItRpL_Q9UaKumAyo14LnlvwnyF3Kojk";

    JwtContext jwtcontext = null;
    static JwtClaims jwtClaims = null;
    TokenValidator tokenValidator = null;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
        jwtClaims = JwtParsingUtils.parseJwtWithoutValidation(idToken).getJwtClaims();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        tokenValidator = new IdTokenValidator(oidcClientConfig);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#validateIssuer()}.
     */
    @Test
    public void testValidateIssuer() {
        final String methodName = "testValidateIssuer";
        String iss_claim_from_token = null;
        String iss_from_config = "http://harmonic:8011/oauth2/endpoint/OAuthConfigSample/token";
        try {
            iss_claim_from_token = jwtClaims.getIssuer();

            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcmd).getIssuer();
                    will(returnValue(iss_from_config));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.issuer(iss_claim_from_token).issuerconfigured(iss_from_config);          
            tokenValidator.validateIssuer();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testValidateIssuerNull() {
        final String methodName = "testValidateIssuerNull";
        String iss_claim_from_token = null;
        String iss_from_config = "someotherissuer";
        try {
            
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();//.getIssuer();
                    will(returnValue(oidcmd));
                    allowing(oidcmd).getIssuer();
                    will(returnValue(iss_from_config));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.issuer(iss_claim_from_token).issuerconfigured(iss_from_config);
            tokenValidator.validateIssuer();
            fail("Should have thrown an exception but didn't.");
        } catch (TokenValidationException e) {
            String error = e.getMessage();
            assertTrue("message", error.contains("issuer"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidateIssuerMismatch() {
        final String methodName = "testValidateIssuerMismatch";
        String iss_claim_from_token = null;
        String iss_from_config = "someotherissuer";
        try {
            iss_claim_from_token = jwtClaims.getIssuer();

            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();//.getIssuer();
                    will(returnValue(oidcmd));
                    allowing(oidcmd).getIssuer();
                    will(returnValue(iss_from_config));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.issuer(iss_claim_from_token).issuerconfigured(iss_from_config);
            tokenValidator.validateIssuer();
            fail("Should have thrown an exception but didn't.");

        } catch (TokenValidationException e) {
            String error = e.getMessage();
            assertTrue("message", error.contains("issuer"));
        } catch (Throwable t) {
            //assertEquals("expecting issuer to be [ " + iss_from_config + " ]", iss_claim_from_token, iss_from_config);
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#validateSubject()}.
     */
    @Test
    public void testValidateSubject() {
        final String methodName = "testValidateSubject";
        String subject_claim_from_token = null;
        try {
            subject_claim_from_token = jwtClaims.getSubject();
            tokenValidator = tokenValidator.subject(subject_claim_from_token);
            tokenValidator.validateSubject();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testValidateSubject_emptyClaim() {
        final String methodName = "testValidateSubject_emptyClaim";
        String subject_claim_from_token = "";
        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                }
            });
            tokenValidator = tokenValidator.subject(subject_claim_from_token);
            tokenValidator.validateSubject();
            fail("Should have thrown an exception but didn't.");
        } catch(TokenValidationException e) {
            String received = e.getMessage();
            String expected = "CWWKS2426E: The token has an empty [sub] claim.";
            assertTrue("error message should have empty [sub] claim", received.contains(expected));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testValidateSubject_nullSubject_claim() {
        final String methodName = "testValidateSubject_nullSubject_claim";
        String subject_claim_from_token = null;
        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                }
            });
            tokenValidator = tokenValidator.subject(subject_claim_from_token);
            tokenValidator.validateSubject();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testValidateSubject_missingSubject_claim() {
        final String methodName = "testValidateSubject_missingSubject_claim";
        tokenValidator = new IdTokenValidator(oidcClientConfig);
        try {
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                }
            });          
            tokenValidator.validateSubject();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#validateAudiences()}.
     */
    @Test
    public void testValidateAudiences() {
        final String methodName = "testValidateAudiences";
        List<String> aud_claim_from_token = null;

        try {
            aud_claim_from_token = jwtClaims.getAudience();
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();//.getIssuer();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                }
            });
            tokenValidator = tokenValidator.audiences(aud_claim_from_token);
            tokenValidator.validateAudiences();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testValidateAudiences_multipleAudiences_matchingClientId_missingAzp() {
        final String methodName = "testValidateAudiences_multipleAudiences_matchingClientId_missingAzp";
        List<String> aud_claim_from_token = new ArrayList<String>();

        try {
            aud_claim_from_token.add("client01");
            aud_claim_from_token.add("client02");
            aud_claim_from_token.add("client03");
            
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();//.getIssuer();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                }
            });
            tokenValidator = tokenValidator.audiences(aud_claim_from_token);
            tokenValidator.validateAudiences();
            fail("Should have thrown an exception but didn't.");
        } catch (TokenValidationException e) {
            String error = e.getMessage();
            assertTrue("error message should include - The token is missing the required [azp] claim", error.contains("CWWKS2417E: The token is missing the required [azp] claim."));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testValidateAudiences_multipleAudiences_matchingClientId() {
        final String methodName = "testValidateAudiences_multipleAudiences_matchingClientId";
        List<String> aud_claim_from_token = new ArrayList<String>();

        try {
            aud_claim_from_token.add("client01");
            aud_claim_from_token.add("client02");
            aud_claim_from_token.add("client03");
            
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();//.getIssuer();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                }
            });
            tokenValidator = tokenValidator.audiences(aud_claim_from_token);
            tokenValidator = tokenValidator.azp("some azp");
            tokenValidator.validateAudiences();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testValidateAudiences_multipleAudiences_clientIdNotMatching() {
        final String methodName = "testValidateAudiences_multipleAudiences_clientIdNotMatching";
        List<String> aud_claim_from_token = new ArrayList<String>();

        try {
            aud_claim_from_token.add("client04");
            aud_claim_from_token.add("client02");
            aud_claim_from_token.add("client03");
  
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();//.getIssuer();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("client01"));
                }
            });
            tokenValidator = tokenValidator.audiences(aud_claim_from_token);
            tokenValidator.validateAudiences();
            fail("Should have thrown an exception but didn't.");
        } catch (TokenClaimMismatchException e) {
            //error message = "CWWKS2424E: The [\"client04\" \"client02\" \"client03\" ] value for the [aud] claim in the token does not match the [client01] expected value.]";
            String expected = "value for the [aud] claim in the token does not match the [client01]";
            String received = e.getMessage();
            assertTrue("error should include - value for the [aud] claim in the token does not match the [client01]", received.contains("CWWKS2424E:")&&received.contains(expected));
            
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#validateAZP()}.
     */
    @Test
    public void testAzp() {
        final String methodName = "testValidateAZP";
        String azp_claim_from_token = null;
        String azp_from_config = "clientid";

        try {
            azp_claim_from_token = (String) jwtClaims.getClaimValue("azp");
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();//.getIssuer();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.azp(azp_claim_from_token);
            tokenValidator.validateAZP();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#iat(org.jose4j.jwt.NumericDate)}.
     */
    @Test
    public void testIat() {
        final String methodName = "testIat";
        NumericDate iat;

        try {
            iat = NumericDate.now();
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.iat(iat);
            tokenValidator.validateIssuedAt();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testIatInFuture() {
        final String methodName = "testIatInFuture";
        NumericDate iat;

        try {
            iat = NumericDate.now();
            iat.addSeconds(240);
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.iat(iat);
            tokenValidator.validateIssuedAt();
            fail("Should have thrown an exception but didn't.");

        } catch(TokenValidationException e) {
            String received = e.getMessage();
            String expected = "CWWKS2428E: The token is deemed invalid";
            assertTrue("iat is in future and should have received an error", received.contains(expected) && received.contains("[iat] claim"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testIatInPast() {
        final String methodName = "testIatInPast";
        NumericDate iat;

        try {
            iat = NumericDate.now();
            long now = iat.getValue();
            iat.setValue(now-240);
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.iat(iat);
            tokenValidator.validateIssuedAt();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#exp(org.jose4j.jwt.NumericDate)}.
     */
    @Test
    public void testExp() {
        final String methodName = "testExp";
        NumericDate exp;

        try {
            exp = NumericDate.now();
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.exp(exp);
            tokenValidator.validateExpiration();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testExpInPast() {
        final String methodName = "testExpInPast";
        NumericDate exp;

        try {
            exp = NumericDate.now();
            long now = exp.getValue();
            exp.setValue(now-240);
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.exp(exp);
            tokenValidator.validateExpiration();
            fail("Should have thrown an exception but didn't.");

        } catch(TokenValidationException e) {
            String received = e.getMessage();
            String expected = "CWWKS2427E: The token is not valid because the token expired.";
            assertTrue("exp is in past and should have received an error", received.contains(expected));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testExpInFuture() {
        final String methodName = "testExpInFuture";
        NumericDate exp;

        try {
            exp = NumericDate.now();
            exp.addSeconds(240);
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.exp(exp);
            tokenValidator.validateExpiration();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#nbf(org.jose4j.jwt.NumericDate)}.
     */
    @Test
    public void testNbf() {
        final String methodName = "testNbf";
        NumericDate nbf;

        try {
            nbf = NumericDate.now();
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.nbf(nbf);
            tokenValidator.validateNotBefore();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testNbfInFuture() {
        final String methodName = "testNbfInFuture";
        NumericDate nbf;

        try {
            nbf = NumericDate.now();
            nbf.addSeconds(240);
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.nbf(nbf);
            tokenValidator.validateNotBefore();
            fail("Should have thrown an exception but didn't.");

        } catch(TokenValidationException e) {
            String received = e.getMessage();
            String expected = "CWWKS2428E: The token is deemed invalid";
            assertTrue("nbf is in future and should have received an error", received.contains(expected)&&received.contains("[nbf] claim"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
    
    @Test
    public void testNbfInPast() {
        final String methodName = "testNbfInPast";
        NumericDate nbf;

        try {
            nbf = NumericDate.now();
            long now = nbf.getValue();
            nbf.setValue(now-240);
            mock.checking(new Expectations() {
                {
                    allowing(oidcClientConfig).getProviderMetadata();
                    will(returnValue(oidcmd));
                    allowing(oidcClientConfig).getClientId();
                    will(returnValue("oidcclientid"));
                }
            });
            tokenValidator = tokenValidator.nbf(nbf);
            tokenValidator.validateNotBefore();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test method for {@link io.openliberty.security.oidcclientcore.token.TokenValidator#validate()}.
     */
    @Test
    public void testValidate() {
        //fail("Not yet implemented");
    }

}
