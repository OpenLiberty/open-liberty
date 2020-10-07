/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.security.jwt.KeyException;
import com.ibm.ws.security.common.crypto.KeyAlgorithmChecker;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.common.time.TimeUtils;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.utils.Constants;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class ConsumerUtilTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all");

    private static final String ENTRY1 = "entry1";
    private static final String ENTRY2 = "entry2";
    private static final String ENTRY3 = "entry3";
    private static final String ENTRY4 = "entry4";
    private static final String HS256 = "HS256";
    private static final String RS256 = "RS256";
    private static final String URL = "http://localhost:80/context/sub";
    private static final String trustedAlias = "myAlias";
    private static final String trustStoreRef = "myTrustStore";
    private static final String sharedKey = "mySharedKey";
    private static final String consumerConfigId = "myConsumerConfigId";

    private static final String MSG_JWT_NULL_SIGNING_KEY_WITH_ERROR = "CWWKS6007E";
    private static final String MSG_JWT_ISSUER_NOT_TRUSTED = "CWWKS6022E";
    private static final String MSG_JWT_AUDIENCE_NOT_TRUSTED = "CWWKS6023E";
    private static final String MSG_JWT_IAT_AFTER_EXP = "CWWKS6024E";
    private static final String MSG_JWT_TOKEN_EXPIRED = "CWWKS6025E";
    private static final String MSG_JWT_TOKEN_BEFORE_NBF = "CWWKS6026E";
    private static final String MSG_JWT_MISSING_ALG_HEADER = "CWWKS6027E";
    private static final String MSG_JWT_ALGORITHM_MISMATCH = "CWWKS6028E";
    private static final String MSG_JWT_ERROR_GETTING_SHARED_KEY = "CWWKS6032E";
    private static final String MSG_JWT_ERROR_GETTING_PRIVATE_KEY = "CWWKS6033E";
    private static final String MSG_JWT_MISSING_SHARED_KEY = "CWWKS6034E";
    private static final String MSG_JWT_TRUSTSTORE_SERVICE_NOT_AVAILABLE = "CWWKS6035E";
    private static final String MSG_JWT_CONSUMER_NULL_OR_EMPTY_STRING = "CWWKS6040E";
    private static final String MSG_JWT_CONSUMER_MALFORMED_CLAIM = "CWWKS6043E";
    private static final String MSG_JWT_IAT_AFTER_CURRENT_TIME = "CWWKS6044E";
    private static final String MSG_JWT_TRUSTED_ISSUERS_NULL = "CWWKS6052E";

    private static final String JOSE_EXCEPTION = "org.jose4j.lang.JoseException";
    private static final String PARSE_EXCEPTION = "org.jose4j.json.internal.json_simple.parser.ParseException";
    private static final String INVALID_COMPACT_SERIALIZATION = "Invalid JOSE Compact Serialization";

    private static final long ONE_MINUTE_MS = 1000 * 60;
    private static final long FIVE_MINUTES_MS = 5 * ONE_MINUTE_MS;
    private static final long ONE_HOUR_MS = ONE_MINUTE_MS * 60;
    private static final long STANDARD_CLOCK_SKEW_MS = FIVE_MINUTES_MS;
    private static final long PAST_OUTSIDE_CLOCK_SKEW = -1 * ONE_HOUR_MS;
    private static final long PAST_WITHIN_CLOCK_SKEW = -1 * ONE_MINUTE_MS;
    private static final long FUTURE_WITHIN_CLOCK_SKEW = ONE_MINUTE_MS;
    private static final long FUTURE_OUTSIDE_CLOCK_SKEW = ONE_HOUR_MS;

    private static final String encodedEmptyJsonString = new String(Base64.encodeBase64("{}".getBytes()));

    public final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private ConsumerUtil consumerUtil = null;
    private final TimeUtils timeUtils = new TimeUtils(TimeUtils.YearMonthDateHourMinSecZone);

    private final JwtConsumerConfig jwtConfig = mockery.mock(JwtConsumerConfig.class);
    private final JwtContext jwtContext = mockery.mock(JwtContext.class);
    private final JwtClaims jwtClaims = mockery.mock(JwtClaims.class);
    private final JsonWebStructure jws = mockery.mock(JsonWebStructure.class);
    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<KeyStoreService> kssRef = mockery.mock(AtomicServiceReference.class, "kssRef");
    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<SSLSupport> sslSupportRef = mockery.mock(AtomicServiceReference.class, "sslSupportRef");
    private final KeyStoreService kss = mockery.mock(KeyStoreService.class);
    private final SSLSupport ssl = mockery.mock(SSLSupport.class);
    private final PublicKey publicKey = mockery.mock(PublicKey.class);
    private final RSAPublicKey rsaPublicKey = mockery.mock(RSAPublicKey.class);
    private final X509Certificate cert = mockery.mock(X509Certificate.class);
    private final KeyAlgorithmChecker keyAlgChecker = mockery.mock(KeyAlgorithmChecker.class);

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
        consumerUtil = new ConsumerUtil(kssRef);
        JwtUtils.setSSLSupportService(sslSupportRef);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        System.out.println("Exiting test: " + testName.getMethodName());
    }

    class TestConsumerUtil01 extends ConsumerUtil {
        TestConsumerUtil01() {
            super(null);
        }

        @Override
        boolean validateIssuer(String consumerConfigId, String issuers, String tokenIssuer) throws InvalidClaimException {
            if (issuers.compareTo("issuerFromMap") == 0) {
                throw new RuntimeException("it worked");
            }
            return false;
        }
    }

    /**
     * check that validateClaims reads the issuer from the properties map if supplied
     */
    @Test
    public void testValidateClaimsReadsConfigProperty() {
        boolean valid = false;
        TestConsumerUtil01 cu = new TestConsumerUtil01();
        Map<String, String> propsMap = new HashMap<String, String>();
        propsMap.put(ConsumerUtil.ISSUER, "issuerFromMap");
        cu.setMpConfigProps(propsMap);
        try {
            cu.validateClaims(jwtClaims, jwtContext, null);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                valid = true;
            }
        }
        assertTrue("expected property to be read from map but it was not", valid);
    }

    /********************************************* getSigningKey *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#getSigningKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSigningKey_nullConfig() {
        try {
            Key result = consumerUtil.getSigningKey((JwtConsumerConfig) null, (JwtContext) null);
            assertNull("Result was not null when it should have been. Result: " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getSigningKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSigningKey_unknownAlg() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getSignatureAlgorithm();
                    will(returnValue("SomeUnknownAlg"));
                }
            });
            Key result = consumerUtil.getSigningKey(jwtConfig, jwtContext);
            assertNull("Result was not null when it should have been. Result: " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getSigningKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSigningKey_HS256ThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getSignatureAlgorithm();
                    will(returnValue(HS256));
                    one(jwtConfig).getSharedKey();
                    will(returnValue(null));
                }
            });
            try {
                Key result = consumerUtil.getSigningKey(jwtConfig, jwtContext);
                fail("Should have thrown KeyException but did not. Got key: " + result);
            } catch (KeyException e) {
                validateException(e, MSG_JWT_ERROR_GETTING_SHARED_KEY + ".+" + MSG_JWT_MISSING_SHARED_KEY);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getSigningKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSigningKey_HS256Valid() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getSignatureAlgorithm();
                    will(returnValue(HS256));
                    one(jwtConfig).getSharedKey();
                    will(returnValue(sharedKey));
                }
            });
            Key result = consumerUtil.getSigningKey(jwtConfig, jwtContext);
            assertNotNull("Result was null when it should not have been.", result);
            assertTrue("Result was not an HmacKey. Result was: " + result, result instanceof HmacKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getSigningKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSigningKey_RS256ThrowsException() {
        try {
            ConsumerUtil testConsumerUtil = new ConsumerUtil(null);
            mockery.checking(new Expectations() {
                {
                    allowing(jwtConfig).getSignatureAlgorithm();
                    will(returnValue(RS256));
                    one(jwtConfig).getJwkEnabled(); // for jwksUri(jwkEndpointUrl
                    will(returnValue(false)); //
                    allowing(jwtConfig).getTrustedAlias();
                    will(returnValue(trustedAlias));
                    one(jwtConfig).getTrustStoreRef();
                    will(returnValue(trustStoreRef));
                }
            });
            try {
                Key result = testConsumerUtil.getSigningKey(jwtConfig, jwtContext);
                fail("Should have thrown Exception but did not. Got key: " + result);
            } catch (Exception e) {
                validateException(e, MSG_JWT_ERROR_GETTING_PRIVATE_KEY + ".+\\[" + trustedAlias + "\\].+\\[" + trustStoreRef + "\\].+" + MSG_JWT_TRUSTSTORE_SERVICE_NOT_AVAILABLE);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getSigningKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSigningKey_RS256Valid() {
        consumerUtil.keyAlgChecker = keyAlgChecker;
        try {
            mockery.checking(new Expectations() {
                {

                    allowing(jwtConfig).getSignatureAlgorithm();
                    will(returnValue(RS256));
                    one(keyAlgChecker).isHSAlgorithm(RS256);
                    will(returnValue(false));
                    allowing(keyAlgChecker).isRSAlgorithm(RS256);
                    will(returnValue(true));
                    one(jwtConfig).getJwkEnabled(); // for jwksUri
                    will(returnValue(false)); //
                    allowing(jwtConfig).getTrustedAlias();
                    will(returnValue(trustedAlias));
                    one(jwtConfig).getTrustStoreRef();
                    will(returnValue(trustStoreRef));
                    one(kssRef).getService();
                    will(returnValue(kss));
                    one(kss).getX509CertificateFromKeyStore(trustStoreRef, trustedAlias);
                    will(returnValue(cert));
                    one(cert).getPublicKey();
                    will(returnValue(rsaPublicKey));
                    one(keyAlgChecker).isPublicKeyValidType(rsaPublicKey, RS256);
                    will(returnValue(true));
                }
            });
            Key result = consumerUtil.getSigningKey(jwtConfig, jwtContext);
            assertNotNull("Resulting key was null when it should not have been.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* getConfiguredAudiences *********************************************/

    @Test
    public void test_getConfiguredAudiences_noAudiencesConfigured() {
        mockery.checking(new Expectations() {
            {
                allowing(jwtConfig).getAudiences();
                will(returnValue(null));
            }
        });
        List<String> result = consumerUtil.getConfiguredAudiences(jwtConfig);
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getConfiguredAudiences_audiencesInServerConfig_emptyList() {
        final List<String> serverAudiences = new ArrayList<String>();
        mockery.checking(new Expectations() {
            {
                allowing(jwtConfig).getAudiences();
                will(returnValue(serverAudiences));
            }
        });
        List<String> result = consumerUtil.getConfiguredAudiences(jwtConfig);
        assertEquals("Returned audiences did not match configured value.", serverAudiences, result);
    }

    @Test
    public void test_getConfiguredAudiences_audiencesInServerConfig_nonEmptyList() {
        final List<String> serverAudiences = new ArrayList<String>();
        serverAudiences.add("1");
        serverAudiences.add("2 2");
        serverAudiences.add(" three ");
        mockery.checking(new Expectations() {
            {
                allowing(jwtConfig).getAudiences();
                will(returnValue(serverAudiences));
            }
        });
        List<String> result = consumerUtil.getConfiguredAudiences(jwtConfig);
        assertEquals("Returned audiences did not match configured value.", serverAudiences, result);
    }

    @Test
    public void test_getConfiguredAudiences_audiencesInMpConfig() {
        String audience = "http://www.example.com";
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.VERIFY_AUDIENCES, audience);
        consumerUtil.setMpConfigProps(props);
        mockery.checking(new Expectations() {
            {
                allowing(jwtConfig).getAudiences();
                will(returnValue(null));
            }
        });

        List<String> result = consumerUtil.getConfiguredAudiences(jwtConfig);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 1, result.size());
        assertTrue("List of audiences did not contain [" + audience + "]. Audiences were: " + result, result.contains(audience));
    }

    /********************************************* getAudiencesFromMpConfigProps *********************************************/

    @Test
    public void test_getAudiencesFromMpConfigProps_noMpConfigProps() {
        List<String> result = consumerUtil.getAudiencesFromMpConfigProps();
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_missingAudiences() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.ISSUER, "blah");
        consumerUtil.setMpConfigProps(props);

        List<String> result = consumerUtil.getAudiencesFromMpConfigProps();
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_singleAudience() {
        String audience = "my audience";
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.VERIFY_AUDIENCES, audience);
        consumerUtil.setMpConfigProps(props);

        List<String> result = consumerUtil.getAudiencesFromMpConfigProps();
        assertNotNull("Should have gotten a non-null list of audiences.", result);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 1, result.size());
        assertTrue("List of audiences did not contain [" + audience + "]. Audiences were: " + result, result.contains(audience));
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_multipleAudiences() {
        String audience1 = "aud1";
        String audience2 = " aud 2";
        String audience3 = "aud 3 ";
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.VERIFY_AUDIENCES, audience1 + "," + audience2 + "," + audience3);
        consumerUtil.setMpConfigProps(props);

        List<String> result = consumerUtil.getAudiencesFromMpConfigProps();
        assertNotNull("Should have gotten a non-null list of audiences.", result);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 3, result.size());
        assertTrue("List of audiences did not contain [" + audience1 + "]. Audiences were: " + result, result.contains(audience1));
        assertTrue("List of audiences did not contain [" + audience2 + "]. Audiences were: " + result, result.contains(audience2));
        assertTrue("List of audiences did not contain [" + audience3 + "]. Audiences were: " + result, result.contains(audience3));
    }

    /********************************************* getConfiguredSignatureAlgorithm *********************************************/

    @Test
    public void test_getConfiguredSignatureAlgorithm_sigAlgInServerConfig() {
        final String serverConfigAlg = "RS512";
        mockery.checking(new Expectations() {
            {
                one(jwtConfig).getSignatureAlgorithm();
                will(returnValue(serverConfigAlg));
            }
        });
        String result = consumerUtil.getConfiguredSignatureAlgorithm(jwtConfig);
        assertEquals("Did not get expected signature algorithm value.", serverConfigAlg, result);
    }

    @Test
    public void test_getConfiguredSignatureAlgorithm_sigAlgMissingFromServerConfigAndMpConfig() {
        mockery.checking(new Expectations() {
            {
                one(jwtConfig).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = consumerUtil.getConfiguredSignatureAlgorithm(jwtConfig);
        assertEquals("Did not get expected default signature algorithm value.", RS256, result);
    }

    @Test
    public void test_getConfiguredSignatureAlgorithm_mpConfigPropsUnsupportedAlg() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.PUBLIC_KEY_ALG, "NO256");
        consumerUtil.setMpConfigProps(props);

        mockery.checking(new Expectations() {
            {
                one(jwtConfig).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = consumerUtil.getConfiguredSignatureAlgorithm(jwtConfig);
        assertEquals("Did not get expected default signature algorithm value.", RS256, result);
    }

    @Test
    public void test_getConfiguredSignatureAlgorithm_mpConfigPropsSupportedAlg() {
        String mpConfigSigAlg = "HS512";
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.PUBLIC_KEY_ALG, mpConfigSigAlg);
        consumerUtil.setMpConfigProps(props);

        mockery.checking(new Expectations() {
            {
                one(jwtConfig).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = consumerUtil.getConfiguredSignatureAlgorithm(jwtConfig);
        assertEquals("Did not get expected signature algorithm value.", mpConfigSigAlg, result);
    }

    /****************************** getSignatureAlgorithmFromMpConfigProps ******************************/

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps_noConfigProps() {
        String result = consumerUtil.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected default signature algorithm value.", RS256, result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps_emptyConfigProps() {
        Map<String, String> props = new HashMap<String, String>();
        consumerUtil.setMpConfigProps(props);

        String result = consumerUtil.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected default signature algorithm value.", RS256, result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps_unsupportedAlgorithm() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.PUBLIC_KEY_ALG, "unknown algorithm");
        consumerUtil.setMpConfigProps(props);

        String result = consumerUtil.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected default signature algorithm value.", RS256, result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps() {
        String knownAlgorithm = "ES384";
        Map<String, String> props = new HashMap<String, String>();
        props.put(ConsumerUtil.PUBLIC_KEY_ALG, knownAlgorithm);
        consumerUtil.setMpConfigProps(props);

        String result = consumerUtil.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected signature algorithm value.", knownAlgorithm, result);
    }

    /********************************************* getMpConfigProperty *********************************************/

    @Test
    public void test_getMpConfigProperty_noPropsSet() {
        String propName = ConsumerUtil.PUBLIC_KEY_ALG;
        String result = consumerUtil.getMpConfigProperty(propName);
        assertNull("Returned value should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getMpConfigProperty_nullPropName() {
        String propName = null;
        Map<String, String> props = new HashMap<String, String>();
        consumerUtil.setMpConfigProps(props);

        String result = consumerUtil.getMpConfigProperty(propName);
        assertNull("Returned value should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getMpConfigProperty_missingProperty() {
        String propName = "some prop";
        Map<String, String> props = new HashMap<String, String>();
        consumerUtil.setMpConfigProps(props);

        String result = consumerUtil.getMpConfigProperty(propName);
        assertNull("Returned value should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getMpConfigProperty() {
        String propName = "some prop";
        String propValue = "expected prop value";
        Map<String, String> props = new HashMap<String, String>();
        props.put(propName, propValue);
        consumerUtil.setMpConfigProps(props);

        String result = consumerUtil.getMpConfigProperty(propName);
        assertEquals("Returned value did not match expected value.", propValue, result);
    }

    /********************************************* isSupportedSignatureAlgorithm *********************************************/

    @Test
    public void test_isSupportedSignatureAlgorithm_nullAlg() {
        String sigAlg = null;
        boolean result = consumerUtil.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_emptyAlg() {
        String sigAlg = "";
        boolean result = consumerUtil.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_rs2560() {
        String sigAlg = "RS2560";
        boolean result = consumerUtil.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_rs256() {
        String sigAlg = "RS256";
        boolean result = consumerUtil.isSupportedSignatureAlgorithm(sigAlg);
        assertTrue("Input algorithm [" + sigAlg + "] should have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_rs1024() {
        String sigAlg = "RS1024";
        boolean result = consumerUtil.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_hs384() {
        String sigAlg = "HS384";
        boolean result = consumerUtil.isSupportedSignatureAlgorithm(sigAlg);
        assertTrue("Input algorithm [" + sigAlg + "] should have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_es512() {
        String sigAlg = "ES512";
        boolean result = consumerUtil.isSupportedSignatureAlgorithm(sigAlg);
        assertTrue("Input algorithm [" + sigAlg + "] should have been considered supported.", result);
    }

    /********************************************* getSharedSecretKey *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#getSharedSecretKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSharedSecretKey_nullConfig() {
        try {
            Key result = consumerUtil.getSharedSecretKey(null);
            assertNull("Result was not null when it should have been. Result: " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getSharedSecretKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSharedSecretKey_emptyOrNullKey() {
        try {
            final String missingKey = RandomUtils.getRandomSelection(null, "");
            System.out.println("Chose key: [" + missingKey + "]");
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getSharedKey();
                    will(returnValue(missingKey));
                }
            });
            try {
                Key result = consumerUtil.getSharedSecretKey(jwtConfig);
                fail("Should have thrown KeyException but did not. Got key: " + result);
            } catch (KeyException e) {
                validateException(e, MSG_JWT_MISSING_SHARED_KEY);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getSharedSecretKey(JwtConsumerConfig)}
     */
    @Test
    public void testGetSharedSecretKey_validKey() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getSharedKey();
                    will(returnValue(sharedKey));
                }
            });
            Key result = consumerUtil.getSharedSecretKey(jwtConfig);
            assertNotNull("Resule was null when it should not have been.", result);
            assertTrue("Result was not an HmacKey. Result was: " + result, result instanceof HmacKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* getPublicKey *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#getPublicKey(String, String, String)}
     */
    @Test
    public void testGetPublicKey_missingKeyStoreService() {
        final String randomAlg = RandomUtils.getRandomSelection(null, RS256, HS256);
        try {
            // Keystore service not set
            ConsumerUtil testConsumerUtil = new ConsumerUtil(null);
            try {
                Key result = testConsumerUtil.getPublicKey(trustedAlias, trustStoreRef, randomAlg);
                fail("Should have thrown KeyStoreServiceException but did not. Got key: " + result);
            } catch (KeyException e) {
                validateException(e, MSG_JWT_NULL_SIGNING_KEY_WITH_ERROR + ".+\\[" + randomAlg + "\\].+\\[" + Constants.SIGNING_KEY_X509 + "\\].+" + MSG_JWT_TRUSTSTORE_SERVICE_NOT_AVAILABLE);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getPublicKey(String, String, String)}
     */
    @Test
    public void testGetPublicKey_nullArgs() {
        try {
            // Null alias and trust store ref
            mockery.checking(new Expectations() {
                {
                    one(kssRef).getService();
                    will(returnValue(kss));
                    one(sslSupportRef).getService();
                    will(returnValue(ssl));
                    one(ssl).getJSSEHelper();
                    will(returnValue(null));
                }
            });
            Key result = consumerUtil.getPublicKey(null, null, null);
            assertNull("Resulting key was not null when it should not have been. Result: " + result, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getPublicKey(String, String, String)} - Exception thrown getting key from keystore
     */
    @Test
    public void testGetPublicKey_throwsException() {
        final String randomAlg = RandomUtils.getRandomSelection(RS256, HS256);
        try {
            // Getting private key from keystore throws exception
            final String errorMsg = "Some error finding key";
            mockery.checking(new Expectations() {
                {
                    one(kssRef).getService();
                    will(returnValue(kss));
                    one(kss).getX509CertificateFromKeyStore(trustStoreRef, trustedAlias);
                    will(throwException(new KeyStoreException(errorMsg)));
                }
            });
            try {
                Key result = consumerUtil.getPublicKey(trustedAlias, trustStoreRef, randomAlg);
                fail("Should have thrown KeyException but did not. Got key: " + result);
            } catch (KeyException e) {
                validateException(e, MSG_JWT_NULL_SIGNING_KEY_WITH_ERROR + ".+\\[" + randomAlg + "\\].+\\[" + Constants.SIGNING_KEY_X509 + "\\].+" + errorMsg);
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getPublicKey(String, String, String)}
     */
    @Test
    public void testGetPublicKey_notRSAPublicKey() {
        final String randomAlg = RandomUtils.getRandomSelection(null, RS256, HS256);
        try {
            // Successfully get key from keystore, but it is not an instance of RSAPublicKey
            mockery.checking(new Expectations() {
                {
                    one(kssRef).getService();
                    will(returnValue(kss));
                    one(kss).getX509CertificateFromKeyStore(trustStoreRef, trustedAlias);
                    will(returnValue(cert));
                    one(cert).getPublicKey();
                    will(returnValue(publicKey));
                }
            });
            Key result = consumerUtil.getPublicKey(trustedAlias, trustStoreRef, randomAlg);
            assertEquals("Returned PublicKey did not match the expected object.", publicKey, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getPublicKey(String, String, String)}
     */
    @Test
    public void testGetPublicKey_validKey() {
        final String randomAlg = RandomUtils.getRandomSelection(null, RS256, HS256);
        try {
            // Successfully get RSAPublicKey from keystore
            mockery.checking(new Expectations() {
                {
                    one(kssRef).getService();
                    will(returnValue(kss));
                    one(kss).getX509CertificateFromKeyStore(trustStoreRef, trustedAlias);
                    will(returnValue(cert));
                    one(cert).getPublicKey();
                    will(returnValue(rsaPublicKey));
                }
            });
            Key result = consumerUtil.getPublicKey(trustedAlias, trustStoreRef, randomAlg);
            assertNotNull("Resulting key was null when it should not have been.", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* parseJwtWithValidation *********************************************/

    // TODO - parseJwtWithValidation

    /********************************************* parseJwtWithoutValidation *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#parseJwtWithoutValidation(String, String)}
     */
    @Test
    public void testParseJwtWithoutValidation_nullOrEmptyTokenString() {
        try {
            final String configId = RandomUtils.getRandomSelection(null, "myConfigId");
            final String emptyToken = RandomUtils.getRandomSelection(null, "");
            try {
                mockery.checking(new Expectations() {
                    {
                        one(jwtConfig).getId();
                        will(returnValue(configId));
                    }
                });
                JwtContext context = consumerUtil.parseJwtWithoutValidation(emptyToken, jwtConfig);
                fail("Should have thrown Exception but did not. Got context: " + context);
            } catch (Exception e) {
                validateException(e, MSG_JWT_CONSUMER_NULL_OR_EMPTY_STRING + ".+\\[" + configId + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#parseJwtWithoutValidation(String, String)}
     */
    @Test
    public void testParseJwtWithoutValidation_singlePartTokenString() {
        try {
            final String tokenString = "test";
            try {
                mockery.checking(new Expectations() {
                    {
                        one(jwtConfig).getClockSkew();
                        will(returnValue(0L));
                    }
                });
                JwtContext context = consumerUtil.parseJwtWithoutValidation(tokenString, jwtConfig);
                fail("Should have thrown Exception but did not. Got context: " + context);
            } catch (Exception e) {
                // TODO - anything we can wrap this open source exception with?
                validateException(e, JOSE_EXCEPTION + ".+" + INVALID_COMPACT_SERIALIZATION + ".+" + tokenString);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#parseJwtWithoutValidation(String, String)}
     */
    @Test
    public void testParseJwtWithoutValidation_twoPartTokenString() {
        try {
            final String tokenString = "test.test";
            try {
                mockery.checking(new Expectations() {
                    {
                        one(jwtConfig).getClockSkew();
                        will(returnValue(0L));
                    }
                });
                JwtContext context = consumerUtil.parseJwtWithoutValidation(tokenString, jwtConfig);
                fail("Should have thrown Exception but did not. Got context: " + context);
            } catch (Exception e) {
                // TODO - anything we can wrap this open source exception with?
                validateException(e, JOSE_EXCEPTION + ".+" + INVALID_COMPACT_SERIALIZATION + ".+" + tokenString);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#parseJwtWithoutValidation(String, String)}
     */
    @Test
    public void testParseJwtWithoutValidation_threePartMalformedTokenString() {
        try {
            final String tokenString = "test.test.test";
            try {
                mockery.checking(new Expectations() {
                    {
                        one(jwtConfig).getClockSkew();
                        will(returnValue(0L));
                    }
                });
                JwtContext context = consumerUtil.parseJwtWithoutValidation(tokenString, jwtConfig);
                fail("Should have thrown Exception but did not. Got context: " + context);
            } catch (Exception e) {
                // TODO - anything we can wrap this open source exception with?
                validateException(e, JOSE_EXCEPTION + ".+" + PARSE_EXCEPTION + ".+" + tokenString);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#parseJwtWithoutValidation(String, String)}
     */
    //@Test
    public void testParseJwtWithoutValidation_emptyTokenParts() {
        try {
            final String tokenString = encodedEmptyJsonString + "." + encodedEmptyJsonString + ".test";
            try {
                JwtContext context = consumerUtil.parseJwtWithoutValidation(tokenString, jwtConfig);
                fail("Should have thrown Exception but did not. Got context: " + context);
            } catch (Exception e) {
                // TODO - anything we can wrap this open source exception with?
                validateException(e, "blah.+" + tokenString);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO - parseJwtWithoutValidation

    /********************************************* validateIssuer *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_nullOrEmptyArgs() {
        try {
            String trustedIssuers = null;
            String tokenIssuer = null;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for null trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_TRUSTED_ISSUERS_NULL, tokenIssuer, consumerConfigId);
            }

            try {
                trustedIssuers = null;
                tokenIssuer = "";
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for null trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_TRUSTED_ISSUERS_NULL, tokenIssuer, consumerConfigId);
            }

            try {
                trustedIssuers = "";
                tokenIssuer = null;
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for empty trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_TRUSTED_ISSUERS_NULL, tokenIssuer, consumerConfigId);
            }

            try {
                trustedIssuers = "";
                tokenIssuer = "";
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for empty trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_TRUSTED_ISSUERS_NULL, tokenIssuer, consumerConfigId);
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_whitespaceTrustedIssuers() {
        try {
            String trustedIssuers = " ";
            String tokenIssuer = " ";
            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for whitespace-only token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_commaTrustedIssuers() {
        try {
            String trustedIssuers = ",";
            String tokenIssuer = "";
            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for comma-only trusted issuer and empty token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_nullTokenIssuer() {
        try {
            String trustedIssuers = ENTRY1;
            String tokenIssuer = null;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for null token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_emptyTokenIssuer() {
        try {
            String trustedIssuers = ENTRY1;
            String tokenIssuer = "";

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for empty token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_untrustedTokenIssuer() {
        try {
            String trustedIssuers = ENTRY1;
            String tokenIssuer = ENTRY2;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for untrusted token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_tokenIssuerSubstringOfTrusted() {
        try {
            String trustedIssuers = ENTRY1 + ENTRY2;
            String tokenIssuer = ENTRY2;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for untrusted token issuer that is substring of trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_tokenIssuerSuperStringWithComma() {
        try {
            String trustedIssuers = ENTRY1;
            String tokenIssuer = ENTRY2 + "," + ENTRY1;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for untrusted token issuer using comma-separated value that is superstring of trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_tokenIssuerSuperStringWithSpace() {
        try {
            String trustedIssuers = ENTRY1;
            String tokenIssuer = ENTRY1 + " " + ENTRY2;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for untrusted token issuer using space-separated value that is superstring of trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_emptyTokenIssuer_trailingCommaTrusted() {
        try {
            String trustedIssuers = ENTRY1 + ",";
            String tokenIssuer = "";

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for empty token issuer with trailing comma in trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer_emptyTokenIssuer_leadingAndTrailingCommaTrusted() {
        try {
            String trustedIssuers = "," + ENTRY1 + ",";
            String tokenIssuer = "";

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for empty token issuer with leading and trailing comma in trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_singleTrustedIssuer() {
        try {
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, ENTRY1, ENTRY1));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, " \t" + ENTRY1 + " ", ENTRY1));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, ENTRY1 + ",", ENTRY1));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, " " + ENTRY1 + " ,", ENTRY1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_multipleTrustedIssuers_nullTokenIssuer() {
        try {
            String trustedIssuers = ENTRY1 + "," + ENTRY2;
            String tokenIssuer = null;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for null token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_multipleTrustedIssuers_emptyTokenIssuer() {
        try {
            String trustedIssuers = ENTRY1 + "," + ENTRY2;
            String tokenIssuer = "";

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for empty token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_multipleTrustedIssuers_untrustedTokenIssuer() {
        try {
            String trustedIssuers = ENTRY1 + "," + ENTRY2;
            String tokenIssuer = ENTRY3;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for untrusted token issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_multipleTrustedIssuers_tokenIssuerContainsTrustedIssuer() {
        try {
            String trustedIssuers = ENTRY1 + "," + ENTRY2;
            String tokenIssuer = ENTRY1 + " " + ENTRY3;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for untrusted token issuer that contains substring of a trusted issuer.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_multipleTrustedIssuers_tokenIssuerWithComma() {
        try {
            String trustedIssuers = ENTRY1 + "," + ENTRY2;
            String tokenIssuer = ENTRY1 + "," + ENTRY2;

            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for untrusted token issuer that matches full comma-separated trusted issuers string.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, consumerConfigId, trustedIssuers);
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_multipleTrustedIssuers() {
        try {
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, ENTRY1 + "," + URL, ENTRY1));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, ENTRY1 + "," + URL, URL));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, ENTRY1 + "," + ENTRY1, ENTRY1));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, ENTRY1 + ", " + ENTRY2 + " ", ENTRY2));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIssuer(String, String, String)}
     */
    @Test
    public void testValidateIssuer_trustAllIssuers() {
        try {
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, Constants.ALL_ISSUERS, null));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, Constants.ALL_ISSUERS, ""));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, Constants.ALL_ISSUERS, ENTRY1));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, Constants.ALL_ISSUERS, ENTRY2 + "," + ENTRY2));
            assertTrue("Validation should have succeeded.", consumerUtil.validateIssuer(consumerConfigId, ENTRY1 + ", " + Constants.ALL_ISSUERS, ENTRY1));

            String trustedIssuers = ENTRY1 + " " + Constants.ALL_ISSUERS;
            String tokenIssuer = ENTRY1;
            try {
                consumerUtil.validateIssuer(consumerConfigId, trustedIssuers, tokenIssuer);
                fail("Should have thrown InvalidClaimException for white space separated trusted issuers.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_ISSUER_NOT_TRUSTED, tokenIssuer, trustedIssuers);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* validateAudience *********************************************/

    @Test
    public void testValidateAudience_withConfig_noAudiencesConfigured_nullTokenAudiences_ignoreAudIfNotConfigured() {
        List<String> audiences = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getAudiences();
                    will(returnValue(null));
                    one(jwtConfig).ignoreAudClaimIfNotConfigured();
                    will(returnValue(true));
                }
            });
            consumerUtil.validateAudience(jwtConfig, audiences);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testValidateAudience_withConfig_noAudiencesConfigured_nullTokenAudiences() {
        List<String> audiences = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getAudiences();
                    will(returnValue(null));
                    one(jwtConfig).ignoreAudClaimIfNotConfigured();
                    will(returnValue(false));
                }
            });
            consumerUtil.validateAudience(jwtConfig, audiences);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testValidateAudience_withConfig_noAudiencesConfigured_emptyTokenAudiences() {
        List<String> audiences = new ArrayList<String>();
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getAudiences();
                    will(returnValue(null));
                    one(jwtConfig).ignoreAudClaimIfNotConfigured();
                    will(returnValue(false));
                }
            });
            consumerUtil.validateAudience(jwtConfig, audiences);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testValidateAudience_withConfig_noAudiencesConfigured_nonEmptyTokenAudiences_ignoreAudIfNotConfigured() {
        List<String> audiences = Arrays.asList("aud1", "aud2", "aud3");
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getAudiences();
                    will(returnValue(null));
                    one(jwtConfig).ignoreAudClaimIfNotConfigured();
                    will(returnValue(true));
                }
            });
            consumerUtil.validateAudience(jwtConfig, audiences);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testValidateAudience_withConfig_noAudiencesConfigured_nonEmptyTokenAudiences() {
        List<String> audiences = Arrays.asList("aud1", "aud2", "aud3");
        final String configId = testName.getMethodName();
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getAudiences();
                    will(returnValue(null));
                    one(jwtConfig).ignoreAudClaimIfNotConfigured();
                    will(returnValue(false));
                    one(jwtConfig).getId();
                    will(returnValue(configId));
                }
            });
            try {
                consumerUtil.validateAudience(jwtConfig, audiences);
                fail("Should have thrown an exception but didn't.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_AUDIENCE_NOT_TRUSTED, configId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testValidateAudience_withConfig_audiencesConfigured_nullTokenAudiences() {
        List<String> audiences = null;
        final String configId = testName.getMethodName();
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtConfig).getAudiences();
                    will(returnValue(new ArrayList<String>()));
                    one(jwtConfig).getId();
                    will(returnValue(configId));
                }
            });
            try {
                consumerUtil.validateAudience(jwtConfig, audiences);
                fail("Should have thrown an exception but didn't.");
            } catch (InvalidClaimException e) {
                validateExceptionWithInserts(e, MSG_JWT_AUDIENCE_NOT_TRUSTED, configId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* validateAudience *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#validateAudience(List, List)}
     */
    @Test
    public void testValidateAudience() {
        try {
            List<String> emptyList = new ArrayList<String>();
            List<String> allAudiencesList = new ArrayList<String>();
            allAudiencesList.add(Constants.ALL_AUDIENCES);

            // Null/empty token and allowed audiences
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience((List<String>) null, null));
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience((List<String>) null, emptyList));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(emptyList, null));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(emptyList, emptyList));

            // ALL_AUDIENCES
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience(allAudiencesList, null));
            assertTrue("Validation should NOT have succeeded.", consumerUtil.validateAudience(allAudiencesList, emptyList));
            List<String> tokenAud = new ArrayList<String>();
            tokenAud.add(ENTRY1);
            tokenAud.add(ENTRY2);
            assertTrue("Validation should NOT have succeeded.", consumerUtil.validateAudience(allAudiencesList, tokenAud));

            // ALL_AUDIENCES substring
            List<String> allAudSubList = new ArrayList<String>();
            allAudSubList.add(Constants.ALL_AUDIENCES.substring(0, Constants.ALL_AUDIENCES.length() - 1));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allAudSubList, tokenAud));

            // Null/empty allowed audiences, single aud in the token
            tokenAud = new ArrayList<String>();
            tokenAud.add(ENTRY1);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience((List<String>) null, tokenAud));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(emptyList, tokenAud));

            // Null/empty audiences in token, single aud in allowed audiences
            List<String> allowedAud = new ArrayList<String>();
            allowedAud.add(ENTRY1);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, null));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, emptyList));

            // Single entries in both - match and mismatch
            allowedAud = new ArrayList<String>();
            allowedAud.add(ENTRY1);
            tokenAud = new ArrayList<String>();
            tokenAud.add(ENTRY1);
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));
            tokenAud = new ArrayList<String>();
            tokenAud.add(ENTRY2);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));

            // Multiple entries in allowed audiences
            allowedAud = new ArrayList<String>();
            allowedAud.add(ENTRY1);
            allowedAud.add(ENTRY2);
            allowedAud.add(ENTRY3);
            tokenAud = new ArrayList<String>();
            tokenAud.add(ENTRY4);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));
            // Make sure we're not matching against substrings
            tokenAud.add(ENTRY1.substring(0, ENTRY1.length() - 1));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));
            tokenAud.add(ENTRY2);
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));

            // Multiple entries in allowed audiences with ALL_AUDIENCES
            tokenAud.remove(ENTRY2);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));
            allowedAud.add(Constants.ALL_AUDIENCES);
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));

            // Multiple entries in token
            tokenAud = new ArrayList<String>();
            tokenAud.add(ENTRY1);
            tokenAud.add(ENTRY2);
            tokenAud.add(ENTRY3);
            allowedAud = new ArrayList<String>();
            allowedAud.add(ENTRY4);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));
            // Make sure we're not matching against substrings
            allowedAud.add(ENTRY1.substring(0, ENTRY1.length() - 1));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));
            allowedAud.add(ENTRY2);
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));

            // Multiple entries in token audiences with ALL_AUDIENCES
            allowedAud.remove(ENTRY2);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));
            allowedAud.add(Constants.ALL_AUDIENCES);
            assertTrue("Validation should have succeeded.", consumerUtil.validateAudience(allowedAud, tokenAud));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* validateIatAndExp *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)}
     */
    @Test
    public void testValidateIatAndExp_nullClaims() {
        try {
            // Nothing should happen
            consumerUtil.validateIatAndExp(null, 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)}
     */
    @Test
    public void testValidateIatAndExp_malformedIat() {
        try {
            final String eMsg = "The claim was malformed.";
            mockery.checking(new Expectations() {
                {
                    one(jwtClaims).getIssuedAt();
                    will(throwException(new MalformedClaimException(eMsg)));
                }
            });
            try {
                consumerUtil.validateIatAndExp(jwtClaims, 0);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                validateException(e, MSG_JWT_CONSUMER_MALFORMED_CLAIM + ".+\\[" + Claims.ISSUED_AT + "\\].+" + eMsg);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)}
     */
    @Test
    public void testValidateIatAndExp_malformedExp() {
        try {
            final NumericDate iatDate = createDate(PAST_OUTSIDE_CLOCK_SKEW);
            final String eMsg = "The claim was malformed.";
            mockery.checking(new Expectations() {
                {
                    one(jwtClaims).getIssuedAt();
                    will(returnValue(iatDate));
                    one(jwtClaims).getExpirationTime();
                    will(throwException(new MalformedClaimException(eMsg)));
                }
            });
            try {
                consumerUtil.validateIatAndExp(jwtClaims, 0);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                validateException(e, MSG_JWT_CONSUMER_MALFORMED_CLAIM + ".+\\[" + Claims.EXPIRATION + "\\].+" + eMsg);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)}
     */
    @Test
    public void testValidateIatAndExp_nullIat() {
        try {
            final NumericDate expDate = createDate(FUTURE_OUTSIDE_CLOCK_SKEW);
            setIatAndExpClaimExpectations(null, expDate);
            // Null iat should have no ultimate effect here
            consumerUtil.validateIatAndExp(jwtClaims, 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)}
     */
    @Test
    public void testValidateIatAndExp_nullExp() {
        try {
            final NumericDate iatDate = createDate(PAST_OUTSIDE_CLOCK_SKEW);
            setIatAndExpClaimExpectations(iatDate, null);
            try {
                consumerUtil.validateIatAndExp(jwtClaims, 0);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                validateException(e, MSG_JWT_TOKEN_EXPIRED + ".+\\[" + null + "\\].+\\[" + 0 + "\\] sec");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Timeline:
     * iat -- exp -- [Clock skew start] -- Current time
     */
    @Test
    public void testValidateIatAndExp_iatPast_outsideClockSkew_expPast_outsideClockSkew() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            final NumericDate iatDate = createDate(PAST_OUTSIDE_CLOCK_SKEW);
            final NumericDate expDate = createDate(iatDate, ONE_MINUTE_MS);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String expString = convertDateToLiteralRegexString(expDate);
                validateException(e, MSG_JWT_TOKEN_EXPIRED + ".+\\[" + expString + "\\].+\\[" + (clockSkewMillis / 1000) + "\\] sec");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Timeline:
     * exp -- iat -- [Clock skew start] -- Current time
     */
    @Test
    public void testValidateIatAndExp_expPast_outsideClockSkew_iatPast_outsideClockSkew() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            final NumericDate expDate = createDate(PAST_OUTSIDE_CLOCK_SKEW);
            final NumericDate iatDate = createDate(expDate, ONE_MINUTE_MS);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                String expString = convertDateToLiteralRegexString(expDate);
                validateException(e, MSG_JWT_IAT_AFTER_EXP + ".+\\[" + iatString + "\\].+\\[" + expString + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Timeline:
     * iat -- [Clock skew start] -- exp -- Current time
     */
    @Test
    public void testValidateIatAndExp_iatPast_outsideClockSkew_expPast_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(PAST_OUTSIDE_CLOCK_SKEW);
            final NumericDate expDate = createDate(PAST_WITHIN_CLOCK_SKEW);
            setIatAndExpClaimExpectations(iatDate, expDate);

            consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Timeline:
     * exp -- [Clock skew start] -- iat -- Current time
     */
    @Test
    public void testValidateIatAndExp_expPast_outsideClockSkew_iatPast_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(PAST_WITHIN_CLOCK_SKEW);
            final NumericDate expDate = createDate(PAST_OUTSIDE_CLOCK_SKEW);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                String expString = convertDateToLiteralRegexString(expDate);
                validateException(e, MSG_JWT_IAT_AFTER_EXP + ".+\\[" + iatString + "\\].+\\[" + expString + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Timeline:
     * [Clock skew start] -- iat -- exp -- Current time
     */
    @Test
    public void testValidateIatAndExp_iatPast_withinClockSkew_expPast_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate expDate = createDate(PAST_WITHIN_CLOCK_SKEW);
            final NumericDate iatDate = createDate(expDate, -1 * ONE_MINUTE_MS);
            setIatAndExpClaimExpectations(iatDate, expDate);

            consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Timeline:
     * [Clock skew start] -- exp -- iat -- Current time
     */
    @Test
    public void testValidateIatAndExp_expPast_withinClockSkew_iatPast_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(PAST_WITHIN_CLOCK_SKEW);
            final NumericDate expDate = createDate(iatDate, -1 * ONE_MINUTE_MS);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                String expString = convertDateToLiteralRegexString(expDate);
                validateException(e, MSG_JWT_IAT_AFTER_EXP + ".+\\[" + iatString + "\\].+\\[" + expString + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Timeline:
     * iat -- Current time -- exp
     */
    @Test
    public void testValidateIatAndExp_iatPast_expFuture() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            // Past iat date doesn't matter whether or not it falls within the clock skew
            final NumericDate iatDate = createDate(RandomUtils.getRandomSelection(PAST_OUTSIDE_CLOCK_SKEW, PAST_WITHIN_CLOCK_SKEW));
            // Future exp date doesn't matter whether or not it falls within the clock skew
            final NumericDate expDate = createDate(RandomUtils.getRandomSelection(FUTURE_WITHIN_CLOCK_SKEW, FUTURE_OUTSIDE_CLOCK_SKEW));
            setIatAndExpClaimExpectations(iatDate, expDate);

            consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} exp -- Current time -- [Clock skew end] -- iat
     */
    @Test
    public void testValidateIatAndExp_expPast_iatFuture_outsideClockSkew() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            final NumericDate iatDate = createDate(FUTURE_OUTSIDE_CLOCK_SKEW);
            // Past exp date doesn't matter whether or not it falls within the clock skew
            final NumericDate expDate = createDate(RandomUtils.getRandomSelection(PAST_OUTSIDE_CLOCK_SKEW, PAST_WITHIN_CLOCK_SKEW));
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                validateException(e, MSG_JWT_IAT_AFTER_CURRENT_TIME + ".+" + Claims.ISSUED_AT + ".+\\[" + iatString + "\\].+\\[" + (clockSkewMillis / 1000) + "\\] seconds");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} exp -- Current time -- iat -- [Clock skew end]
     */
    @Test
    public void testValidateIatAndExp_expPast_iatFuture_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(FUTURE_WITHIN_CLOCK_SKEW);
            // Past exp date doesn't matter whether or not it falls within the clock skew
            final NumericDate expDate = createDate(RandomUtils.getRandomSelection(PAST_OUTSIDE_CLOCK_SKEW, PAST_WITHIN_CLOCK_SKEW));
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                String expString = convertDateToLiteralRegexString(expDate);
                validateException(e, MSG_JWT_IAT_AFTER_EXP + ".+\\[" + iatString + "\\].+\\[" + expString + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Current time -- [Clock skew end] -- iat -- exp
     */
    @Test
    public void testValidateIatAndExp_iatFuture_outsideClockSkew_expFuture_outsideClockSkew() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            final NumericDate iatDate = createDate(FUTURE_OUTSIDE_CLOCK_SKEW);
            final NumericDate expDate = createDate(iatDate, ONE_MINUTE_MS);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                validateException(e, MSG_JWT_IAT_AFTER_CURRENT_TIME + ".+" + Claims.ISSUED_AT + ".+\\[" + iatString + "\\].+\\[" + (clockSkewMillis / 1000) + "\\] seconds");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Current time -- [Clock skew end] -- exp -- iat
     */
    @Test
    public void testValidateIatAndExp_expFuture_outsideClockSkew_iatFuture_outsideClockSkew() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            final NumericDate expDate = createDate(FUTURE_OUTSIDE_CLOCK_SKEW);
            final NumericDate iatDate = createDate(expDate, ONE_MINUTE_MS);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                validateException(e, MSG_JWT_IAT_AFTER_CURRENT_TIME + ".+" + Claims.ISSUED_AT + ".+\\[" + iatString + "\\].+\\[" + (clockSkewMillis / 1000) + "\\] seconds");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Current time -- exp -- [Clock skew end] -- iat
     */
    @Test
    public void testValidateIatAndExp_expFuture_withinClockSkew_iatFuture_outsideClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(FUTURE_OUTSIDE_CLOCK_SKEW);
            final NumericDate expDate = createDate(FUTURE_WITHIN_CLOCK_SKEW);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                validateException(e, MSG_JWT_IAT_AFTER_CURRENT_TIME + ".+" + Claims.ISSUED_AT + ".+\\[" + iatString + "\\].+\\[" + (clockSkewMillis / 1000) + "\\] seconds");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Current time -- iat -- [Clock skew end] -- exp
     */
    @Test
    public void testValidateIatAndExp_iatFuture_withinClockSkew_expFuture_outsideClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(FUTURE_WITHIN_CLOCK_SKEW);
            final NumericDate expDate = createDate(FUTURE_OUTSIDE_CLOCK_SKEW);
            setIatAndExpClaimExpectations(iatDate, expDate);

            consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Current time -- iat -- exp -- [Clock skew end]
     */
    @Test
    public void testValidateIatAndExp_iatFuture_withinClockSkew_expFuture_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(FUTURE_WITHIN_CLOCK_SKEW);
            final NumericDate expDate = createDate(FUTURE_WITHIN_CLOCK_SKEW + ONE_MINUTE_MS);
            setIatAndExpClaimExpectations(iatDate, expDate);

            consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateIatAndExp(JwtClaims, long)} Current time -- exp -- iat -- [Clock skew end]
     */
    @Test
    public void testValidateIatAndExp_expFuture_withinClockSkew_iatFuture_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate iatDate = createDate(FUTURE_WITHIN_CLOCK_SKEW + ONE_MINUTE_MS);
            final NumericDate expDate = createDate(FUTURE_WITHIN_CLOCK_SKEW);
            setIatAndExpClaimExpectations(iatDate, expDate);

            try {
                consumerUtil.validateIatAndExp(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String iatString = convertDateToLiteralRegexString(iatDate);
                String expString = convertDateToLiteralRegexString(expDate);
                validateException(e, MSG_JWT_IAT_AFTER_EXP + ".+\\[" + iatString + "\\].+\\[" + expString + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* validateNbf *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#validateNbf(JwtClaims, long)}
     */
    @Test
    public void testValidateNbf_nullClaims() {
        try {
            // Nothing should happen
            consumerUtil.validateNbf(null, 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateNbf(JwtClaims, long)}
     */
    @Test
    public void testValidateNbf_nbfThrowsException() {
        try {
            final String eMsg = "The claim was malformed.";
            mockery.checking(new Expectations() {
                {
                    one(jwtClaims).getNotBefore();
                    will(throwException(new MalformedClaimException(eMsg)));
                }
            });
            try {
                consumerUtil.validateNbf(jwtClaims, 0);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                validateException(e, MSG_JWT_CONSUMER_MALFORMED_CLAIM + ".+\\[" + Claims.NOT_BEFORE + "\\].+" + eMsg);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateNbf(JwtClaims, long)}
     */
    @Test
    public void testValidateNbf_nullNbf() {
        try {
            setNbfClaimExpectations(null);
            consumerUtil.validateNbf(jwtClaims, 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateNbf(JwtClaims, long)}
     */
    @Test
    public void testValidateNbf_pastNbf_outsideClockSkew() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            final NumericDate nbfDate = createDate(PAST_OUTSIDE_CLOCK_SKEW);
            setNbfClaimExpectations(nbfDate);

            consumerUtil.validateNbf(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateNbf(JwtClaims, long)}
     */
    @Test
    public void testValidateNbf_pastNbf_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate nbfDate = createDate(PAST_WITHIN_CLOCK_SKEW);
            setNbfClaimExpectations(nbfDate);

            consumerUtil.validateNbf(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateNbf(JwtClaims, long)}
     */
    @Test
    public void testValidateNbf_futureNbf_withinClockSkew() {
        try {
            long clockSkewMillis = STANDARD_CLOCK_SKEW_MS;

            final NumericDate nbfDate = createDate(FUTURE_WITHIN_CLOCK_SKEW);
            setNbfClaimExpectations(nbfDate);

            consumerUtil.validateNbf(jwtClaims, clockSkewMillis);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateNbf(JwtClaims, long)}
     */
    @Test
    public void testValidateNbf_futureNbf_outsideClockSkew() {
        try {
            // Randomly select clock skew setting to make sure no clock skew and some small clock skew don't affect result
            long clockSkewMillis = getRandomClockSkew();

            final NumericDate nbfDate = createDate(FUTURE_OUTSIDE_CLOCK_SKEW);
            setNbfClaimExpectations(nbfDate);
            try {
                consumerUtil.validateNbf(jwtClaims, clockSkewMillis);
                fail("Should have thrown InvalidClaimException but did not.");
            } catch (InvalidClaimException e) {
                String expString = convertDateToLiteralRegexString(nbfDate);
                validateException(e, MSG_JWT_TOKEN_BEFORE_NBF + ".+\\[" + expString + "\\].+\\[" + (clockSkewMillis / 1000) + "\\] sec");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* getAlgorithmHeader *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#getAlgorithmFromJwtHeader(JwtContext)}
     */
    @Test
    public void testGetAlgorithmHeader_nullContext() {
        try {
            // Null JwtContext object
            String result = consumerUtil.getAlgorithmFromJwtHeader(null);
            assertNull("Result was not null when it should have been. Result was: " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getAlgorithmFromJwtHeader(JwtContext)}
     */
    @Test
    public void testGetAlgorithmHeader_missingJsonWebStructures() {
        try {
            // Null or empty list of JWS within the JwtContext object
            @SuppressWarnings("unchecked")
            final List<JsonWebStructure> emptyJsonStructures = RandomUtils.getRandomSelection(null, new ArrayList<JsonWebStructure>());
            System.out.println("Chose list: " + emptyJsonStructures);

            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJoseObjects();
                    will(returnValue(emptyJsonStructures));
                }
            });
            String result = consumerUtil.getAlgorithmFromJwtHeader(jwtContext);
            assertNull("Result was not null when it should have been. Result was: " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#getAlgorithmFromJwtHeader(JwtContext)}
     */
    @Test
    public void testGetAlgorithmHeader_validAlgorithmHeader() {
        try {
            // Any string (null or non-null) algorithm header value found in JWS
            final String alg = RandomUtils.getRandomSelection(null, RS256, HS256);

            JsonWebStructure myJws = new JsonWebSignature();
            myJws.setAlgorithmHeaderValue(alg);

            final ArrayList<JsonWebStructure> jsonStructures = new ArrayList<JsonWebStructure>();
            jsonStructures.add(myJws);

            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJoseObjects();
                    will(returnValue(jsonStructures));
                }
            });
            assertEquals("Did not find expected algorithm.", alg, consumerUtil.getAlgorithmFromJwtHeader(jwtContext));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* validateAlgorithm *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#validateAlgorithm(JwtContext, String)}
     */
    @Test
    public void testValidateAlgorithm_nullArgs() {
        try {
            // Nothing should happen
            consumerUtil.validateAlgorithm((JwtContext) null, null);
            consumerUtil.validateAlgorithm(jwtContext, null);

            String randomAlg = RandomUtils.getRandomSelection(RS256, HS256);

            // Null JwtContext argument
            try {
                consumerUtil.validateAlgorithm((JwtContext) null, randomAlg);
                fail("Should have thrown InvalidTokenException but did not.");
            } catch (InvalidTokenException e) {
                validateException(e, MSG_JWT_MISSING_ALG_HEADER + ".+\\[" + randomAlg + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateAlgorithm(JwtContext, String)}
     */
    @Test
    public void testValidateAlgorithm_algMismatch() {
        final String randomAlg = RandomUtils.getRandomSelection(RS256, HS256);
        try {
            final List<JsonWebStructure> jwsList = new ArrayList<JsonWebStructure>();
            jwsList.add(jws);

            // Algorithms do not match
            final String otherAlgorithm = "SomeOtherAlgorithm";
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJoseObjects();
                    will(returnValue(jwsList));
                    one(jws).getAlgorithmHeaderValue();
                    will(returnValue(otherAlgorithm));
                }
            });
            try {
                consumerUtil.validateAlgorithm(jwtContext, randomAlg);
                fail("Should have thrown InvalidTokenException but did not.");
            } catch (InvalidTokenException e) {
                validateException(e, MSG_JWT_ALGORITHM_MISMATCH + ".+\\[" + otherAlgorithm + "\\].+\\[" + randomAlg + "\\]");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method under test: {@link ConsumerUtil#validateAlgorithm(JwtContext, String)}
     */
    @Test
    public void testValidateAlgorithm_algMatch() {
        final String randomAlg = RandomUtils.getRandomSelection(RS256, HS256);
        try {
            // Algorithms match
            final List<JsonWebStructure> jwsList = new ArrayList<JsonWebStructure>();
            jwsList.add(jws);

            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJoseObjects();
                    will(returnValue(jwsList));
                    one(jws).getAlgorithmHeaderValue();
                    will(returnValue(randomAlg));
                }
            });
            consumerUtil.validateAlgorithm(jwtContext, randomAlg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************* validateAMRClaim *********************************************/

    /**
     * Method under test: {@link ConsumerUtil#validateAMRClaim(List, List)}
     */
    @Test
    public void testValidateAMRClaim() {
        try {
            List<String> emptyList = new ArrayList<String>();
            List<String> singleList = new ArrayList<String>();
            singleList.add("OTP iris");
            List<String> multipleList = new ArrayList<String>();
            multipleList.add("OTP iris");
            multipleList.add("pwd kba");

            // Null/empty token and allowed amrClaims
            assertTrue("Validation should have succeeded.", consumerUtil.validateAMRClaim(null, null));
            assertTrue("Validation should have succeeded.", consumerUtil.validateAMRClaim(null, emptyList));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAMRClaim(emptyList, null));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAMRClaim(emptyList, emptyList));

            // Null/empty allowed amr, single amr in the token
            assertTrue("Validation should have succeeded.", consumerUtil.validateAMRClaim(null, singleList));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAMRClaim(emptyList, singleList));

            // Null/empty amr in token, single amr in allowed audiences
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAMRClaim(singleList, null));
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAMRClaim(singleList, emptyList));

            // Single entries in both - match and mismatch
            List<String> tokenAMR = new ArrayList<String>();
            tokenAMR.add("OTP");
            tokenAMR.add("iris");
            tokenAMR.add("pwd");
            assertTrue("Validation should have succeeded.", consumerUtil.validateAMRClaim(singleList, tokenAMR));
            tokenAMR = new ArrayList<String>();
            tokenAMR.add(ENTRY2);
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAMRClaim(singleList, tokenAMR));

            // Multiple entries in both
            tokenAMR = new ArrayList<String>();
            tokenAMR.add("pwd");
            tokenAMR.add("kba");
            tokenAMR.add("iris");
            assertFalse("Validation should NOT have succeeded.", consumerUtil.validateAMRClaim(multipleList, tokenAMR));
            tokenAMR = new ArrayList<String>();
            tokenAMR.add("pwd");
            tokenAMR.add("kba");
            assertTrue("Validation should have succeeded.", consumerUtil.validateAMRClaim(multipleList, tokenAMR));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************** Helper methods **********************************************/

    /**
     * Pick a random clock skew setting - either 0 or {@value #STANDARD_CLOCK_SKEW_MS} milliseconds.
     *
     * @return
     */
    private long getRandomClockSkew() {
        return RandomUtils.getRandomSelection(new Long(0), STANDARD_CLOCK_SKEW_MS);
    }

    private long getNow(long timeAdjustMillis) {
        long now = (new Date()).getTime();
        return (now + timeAdjustMillis);
    }

    private NumericDate createDate(long offsetFromCurrentTime) {
        long setTime = getNow(offsetFromCurrentTime);
        final NumericDate date = NumericDate.fromMilliseconds(setTime);
        return date;
    }

    private NumericDate createDate(NumericDate date, long offsetFromGivenDate) {
        long setTime = date.getValueInMillis() + offsetFromGivenDate;
        final NumericDate newDate = NumericDate.fromMilliseconds(setTime);
        return newDate;
    }

    private String convertDateToLiteralRegexString(NumericDate date) {
        String timeString = timeUtils.createDateString(date.getValueInMillis());
        String regexString = Pattern.quote(timeString);
        System.out.println("Original string: [" + timeString + "]");
        System.out.println("Regex string   : [" + regexString + "]");
        return regexString;
    }

    private void validateExceptionWithInserts(Exception e, String msgKey, String... inserts) {
        String errorMsg = e.getLocalizedMessage();
        // Expect exception message to wrap inserts in square brackets '[]'
        StringBuilder regexBuilder = new StringBuilder(msgKey).append(".*");
        for (String insert : inserts) {
            regexBuilder.append("\\[" + insert + "\\]").append(".*");
        }
        String fullPattern = regexBuilder.toString();
        Pattern pattern = Pattern.compile(fullPattern);
        Matcher m = pattern.matcher(errorMsg);
        assertTrue("Exception message did not contain [" + fullPattern + "] as expected. Message was: [" + errorMsg + "]", m.find());
    }

    private void validateException(Exception e, String... errorMsgRegexes) {
        String errorMsg = e.getLocalizedMessage();
        StringBuilder regexBuilder = new StringBuilder();
        for (String regex : errorMsgRegexes) {
            regexBuilder.append(regex).append(".*");
        }
        String fullPattern = regexBuilder.toString();
        Pattern pattern = Pattern.compile(fullPattern);
        Matcher m = pattern.matcher(errorMsg);
        assertTrue("Exception message did not contain [" + fullPattern + "] as expected. Message was: [" + errorMsg + "]", m.find());
    }

    private void setIatAndExpClaimExpectations(final NumericDate iatDate, final NumericDate expDate) throws MalformedClaimException {
        mockery.checking(new Expectations() {
            {
                one(jwtClaims).getIssuedAt();
                will(returnValue(iatDate));
                one(jwtClaims).getExpirationTime();
                will(returnValue(expDate));
            }
        });
    }

    private void setNbfClaimExpectations(final NumericDate nbfDate) throws MalformedClaimException {
        mockery.checking(new Expectations() {
            {
                one(jwtClaims).getNotBefore();
                will(returnValue(nbfDate));
            }
        });
    }

}
