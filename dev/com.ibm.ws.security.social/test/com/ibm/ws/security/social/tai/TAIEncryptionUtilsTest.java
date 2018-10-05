/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;
import test.common.junit.rules.MaximumJavaLevelRule;

public class TAIEncryptionUtilsTest extends CommonTestClass {
	
	// Cap this unit test to Java 8 because it relies on legacy cglib which is not supported post JDK 8
	@ClassRule
	public static TestRule maxJavaLevel = new MaximumJavaLevelRule(8);

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static String ACCESS_TOKEN = "EAANQIE2J5nMBAErWBIFfkmu9r6yQeGoIMg39mHRJrZA7L0jbiD7GEpLSZBm96tgqvvlbQI3UIgQXSJaO6sRJGaFEZCwn5kolWgSjs5q71rrNg0GdbHk5yxrtsZAWsZBv3XV1xFmJ4reZBKA6sx5PqQJejg5RtTWKPg4jJoP0zk1AZDZD";
    private SecretKey secretKey;
    private final String configId = "myConfigId";
    private final String RSA = "RSA";
    private final String AES = "AES";
    private final String UNKNOWN_ALG = "Unknown Algorithm";
    private final String CIPHER_RSA = "RSA/ECB/PKCS1Padding";
    private final String RSA_X509_FORMAT = "X.509";
    private final String CLIENT_SECRET = "myClientSecret";

    final String LOWER_ALPHANUM_REGEX = "[a-z0-9]";
    final int RSA_ENC_TOKEN_LENGTH = 512;
    /**
     * Encrypted token lengths vary for AES, however the ACCESS_TOKEN variable used here should produce the same encrypted token
     * length each run.
     */
    final int AES_ENC_TOKEN_LENGTH = 384;
    final BigInteger keyModulus = BigInteger.probablePrime(512, new Random());
    final BigInteger keyExponent = BigInteger.valueOf(3);

    final static String MSG_ACCESS_TOKEN_TO_ENCRYPT_IS_NULL = "CWWKS5439E";
    final static String MSG_ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_RSA = "CWWKS5440E";
    final static String MSG_ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_AES = "CWWKS5441E";
    final static String MSG_ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_RSA = "CWWKS5444E";
    final static String MSG_ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_AES = "CWWKS5445E";
    final static String MSG_VALUE_NOT_HEXADECIMAL = "CWWKS5446E";

    final SocialLoginConfig clientConfig = mockery.mock(SocialLoginConfig.class);
    final RSAPublicKey rsaPublicKey = mockery.mock(RSAPublicKey.class);
    final RSAPrivateKey rsaPrivateKey = mockery.mock(RSAPrivateKey.class);
    final Cipher cipher = mockery.mock(Cipher.class);

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        String getEncryptedAccessTokenUsingAlgorithm() throws SocialLoginException;

        String encryptAccessTokenUsingRsa() throws SocialLoginException;

        String encryptAccessTokenUsingAes() throws SocialLoginException;

        String rsaEncrypt() throws Exception;

        String aesEncrypt() throws Exception;

        String getDecryptedAccessTokenUsingAlgorithm() throws SocialLoginException;

        String decryptAccessTokenUsingRsa() throws SocialLoginException;

        String decryptAccessTokenUsingAes() throws SocialLoginException;

        String rsaDecrypt() throws Exception;

        String aesDecrypt() throws Exception;
    }

    TAIEncryptionUtils utils = new TAIEncryptionUtils();

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
        secretKey = keyGenerator.generateKey();
        utils = new TAIEncryptionUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();

        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /******************************************* getEncryptedAccessToken *******************************************/

    @Test
    public void getEncryptedAccessToken_nullToken() throws Exception {
        try {
            final String logAndExceptionMsg = MSG_ACCESS_TOKEN_TO_ENCRYPT_IS_NULL;
            try {
                String result = utils.getEncryptedAccessToken(clientConfig, null);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, logAndExceptionMsg);
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEncryptedAccessToken_exceptionThrown() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String getEncryptedAccessTokenUsingAlgorithm(SocialLoginConfig clientConfig, @Sensitive String accessToken, String algorithm) throws SocialLoginException {
                    return mockInterface.getEncryptedAccessTokenUsingAlgorithm();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getAlgorithm();
                    will(returnValue(RSA));
                    one(mockInterface).getEncryptedAccessTokenUsingAlgorithm();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });
            try {
                String result = utils.getEncryptedAccessToken(clientConfig, ACCESS_TOKEN);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEncryptedAccessToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String getEncryptedAccessTokenUsingAlgorithm(SocialLoginConfig clientConfig, @Sensitive String accessToken, String algorithm) throws SocialLoginException {
                    return mockInterface.getEncryptedAccessTokenUsingAlgorithm();
                }
            };
            final String encryptedToken = "myEncryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getAlgorithm();
                    will(returnValue(RSA));
                    one(mockInterface).getEncryptedAccessTokenUsingAlgorithm();
                    will(returnValue(encryptedToken));
                }
            });
            String result = utils.getEncryptedAccessToken(clientConfig, ACCESS_TOKEN);
            assertEquals("Resutld did not match expected value.", encryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getEncryptedAccessTokenUsingAlgorithm *******************************************/

    @Test
    public void getEncryptedAccessTokenUsingAlgorithm_unknownEncryptionAlg() throws Exception {
        try {
            String result = utils.getEncryptedAccessTokenUsingAlgorithm(clientConfig, ACCESS_TOKEN, UNKNOWN_ALG);
            assertNull("Result should have been null but was not. Got result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEncryptedAccessTokenUsingAlgorithm_RSA_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String encryptAccessTokenUsingRsa(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.encryptAccessTokenUsingRsa();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).encryptAccessTokenUsingRsa();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                String result = utils.getEncryptedAccessTokenUsingAlgorithm(clientConfig, ACCESS_TOKEN, RSA);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEncryptedAccessTokenUsingAlgorithm_RSA_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String encryptAccessTokenUsingRsa(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.encryptAccessTokenUsingRsa();
                }
            };
            final String encryptedToken = "myEncryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).encryptAccessTokenUsingRsa();
                    will(returnValue(encryptedToken));
                }
            });
            String result = utils.getEncryptedAccessTokenUsingAlgorithm(clientConfig, null, RSA);
            assertEquals("Resutld did not match expected value.", encryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEncryptedAccessTokenUsingAlgorithm_AES_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String encryptAccessTokenUsingAes(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.encryptAccessTokenUsingAes();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).encryptAccessTokenUsingAes();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                String result = utils.getEncryptedAccessTokenUsingAlgorithm(clientConfig, ACCESS_TOKEN, AES);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getEncryptedAccessTokenUsingAlgorithm_AES_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String encryptAccessTokenUsingAes(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.encryptAccessTokenUsingAes();
                }
            };
            final String encryptedToken = "myEncryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).encryptAccessTokenUsingAes();
                    will(returnValue(encryptedToken));
                }
            });
            String result = utils.getEncryptedAccessTokenUsingAlgorithm(clientConfig, null, AES);
            assertEquals("Resutld did not match expected value.", encryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* encryptAccessTokenUsingRsa *******************************************/

    @Test
    public void encryptAccessTokenUsingRsa_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String rsaEncrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.rsaEncrypt();
                }
            };
            final String eMsg = "This is an exception message.";
            final String logAndExceptionMsg = MSG_ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_RSA + ".+\\[" + configId + "\\].+" + Pattern.quote(eMsg);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).rsaEncrypt();
                    will(throwException(new SocialLoginException(eMsg, null, null)));
                    allowing(clientConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });

            try {
                String result = utils.encryptAccessTokenUsingRsa(clientConfig, ACCESS_TOKEN);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, logAndExceptionMsg);
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encryptAccessTokenUsingRsa_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String rsaEncrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.rsaEncrypt();
                }
            };
            final String encryptedToken = "myEncryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).rsaEncrypt();
                    will(returnValue(encryptedToken));
                }
            });

            String result = utils.encryptAccessTokenUsingRsa(clientConfig, null);
            assertEquals("Resutld did not match expected value.", encryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* encryptAccessTokenUsingAes *******************************************/

    @Test
    public void encryptAccessTokenUsingAes_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String aesEncrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.aesEncrypt();
                }
            };
            final String eMsg = "This is an exception message.";
            final String logAndExceptionMsg = MSG_ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_AES + ".+\\[" + configId + "\\].+" + Pattern.quote(eMsg);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).aesEncrypt();
                    will(throwException(new SocialLoginException(eMsg, null, null)));
                    allowing(clientConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });

            try {
                String result = utils.encryptAccessTokenUsingAes(clientConfig, ACCESS_TOKEN);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, logAndExceptionMsg);
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encryptAccessTokenUsingAes_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String aesEncrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.aesEncrypt();
                }
            };
            final String encryptedToken = "myEncryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).aesEncrypt();
                    will(returnValue(encryptedToken));
                }
            });

            String result = utils.encryptAccessTokenUsingAes(clientConfig, null);
            assertEquals("Resutld did not match expected value.", encryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getDecryptedAccessToken *******************************************/

    @Test
    public void getDecryptedAccessToken_nullToken() throws Exception {
        try {
            String result = utils.getDecryptedAccessToken(clientConfig, null);
            assertNull("Result should have been null but was not. Got result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDecryptedAccessToken_exceptionThrown() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String getDecryptedAccessTokenUsingAlgorithm(SocialLoginConfig clientConfig, @Sensitive String accessToken, String algorithm) throws SocialLoginException {
                    return mockInterface.getDecryptedAccessTokenUsingAlgorithm();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getAlgorithm();
                    will(returnValue(RSA));
                    one(mockInterface).getDecryptedAccessTokenUsingAlgorithm();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });
            try {
                String result = utils.getDecryptedAccessToken(clientConfig, ACCESS_TOKEN);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDecryptedAccessToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String getDecryptedAccessTokenUsingAlgorithm(SocialLoginConfig clientConfig, @Sensitive String accessToken, String algorithm) throws SocialLoginException {
                    return mockInterface.getDecryptedAccessTokenUsingAlgorithm();
                }
            };
            final String decryptedToken = "myDecryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getAlgorithm();
                    will(returnValue(RSA));
                    one(mockInterface).getDecryptedAccessTokenUsingAlgorithm();
                    will(returnValue(decryptedToken));
                }
            });
            String result = utils.getDecryptedAccessToken(clientConfig, ACCESS_TOKEN);
            assertEquals("Resutld did not match expected value.", decryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getDecryptedAccessTokenUsingAlgorithm *******************************************/

    @Test
    public void getDecryptedAccessTokenUsingAlgorithm_unknownEncryptionAlg() throws Exception {
        try {
            String result = utils.getDecryptedAccessTokenUsingAlgorithm(clientConfig, ACCESS_TOKEN, UNKNOWN_ALG);
            assertNull("Result should have been null but was not. Got result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDecryptedAccessTokenUsingAlgorithm_RSA_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String decryptAccessTokenUsingRsa(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.decryptAccessTokenUsingRsa();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).decryptAccessTokenUsingRsa();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                String result = utils.getDecryptedAccessTokenUsingAlgorithm(clientConfig, ACCESS_TOKEN, RSA);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDecryptedAccessTokenUsingAlgorithm_RSA_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String decryptAccessTokenUsingRsa(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.decryptAccessTokenUsingRsa();
                }
            };
            final String decryptedToken = "myDecryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).decryptAccessTokenUsingRsa();
                    will(returnValue(decryptedToken));
                }
            });
            String result = utils.getDecryptedAccessTokenUsingAlgorithm(clientConfig, null, RSA);
            assertEquals("Resutld did not match expected value.", decryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDecryptedAccessTokenUsingAlgorithm_AES_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String decryptAccessTokenUsingAes(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.decryptAccessTokenUsingAes();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).decryptAccessTokenUsingAes();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                String result = utils.getDecryptedAccessTokenUsingAlgorithm(clientConfig, ACCESS_TOKEN, AES);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getDecryptedAccessTokenUsingAlgorithm_AES_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                String decryptAccessTokenUsingAes(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
                    return mockInterface.decryptAccessTokenUsingAes();
                }
            };
            final String decryptedToken = "myDecryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).decryptAccessTokenUsingAes();
                    will(returnValue(decryptedToken));
                }
            });
            String result = utils.getDecryptedAccessTokenUsingAlgorithm(clientConfig, null, AES);
            assertEquals("Resutld did not match expected value.", decryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* decryptAccessTokenUsingRsa *******************************************/

    @Test
    public void decryptAccessTokenUsingRsa_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String rsaDecrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.rsaDecrypt();
                }
            };
            final String eMsg = "This is an exception message.";
            final String logAndExceptionMsg = MSG_ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_RSA + ".+\\[" + configId + "\\].+" + Pattern.quote(eMsg);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).rsaDecrypt();
                    will(throwException(new SocialLoginException(eMsg, null, null)));
                    allowing(clientConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });

            try {
                String result = utils.decryptAccessTokenUsingRsa(clientConfig, ACCESS_TOKEN);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, logAndExceptionMsg);
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void decryptAccessTokenUsingRsa_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String rsaDecrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.rsaDecrypt();
                }
            };
            final String decryptedToken = "myDecryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).rsaDecrypt();
                    will(returnValue(decryptedToken));
                }
            });

            String result = utils.decryptAccessTokenUsingRsa(clientConfig, null);
            assertEquals("Resutld did not match expected value.", decryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* decryptAccessTokenUsingAes *******************************************/

    @Test
    public void decryptAccessTokenUsingAes_throwsException() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String aesDecrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.aesDecrypt();
                }
            };
            final String eMsg = "This is an exception message.";
            final String logAndExceptionMsg = MSG_ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_AES + ".+\\[" + configId + "\\].+" + Pattern.quote(eMsg);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).aesDecrypt();
                    will(throwException(new SocialLoginException(eMsg, null, null)));
                    allowing(clientConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });

            try {
                String result = utils.decryptAccessTokenUsingAes(clientConfig, ACCESS_TOKEN);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, logAndExceptionMsg);
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void decryptAccessTokenUsingAes_nullToken() throws Exception {
        try {
            utils = new TAIEncryptionUtils() {
                @Override
                protected String aesDecrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
                    return mockInterface.aesDecrypt();
                }
            };
            final String decryptedToken = "myDecryptedToken";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).aesDecrypt();
                    will(returnValue(decryptedToken));
                }
            });

            String result = utils.decryptAccessTokenUsingAes(clientConfig, null);
            assertEquals("Resutld did not match expected value.", decryptedToken, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* rsaEncrypt *******************************************/

    @Test
    public void rsaEncrypt_nullToken() throws Exception {
        try {
            String result = utils.rsaEncrypt(clientConfig, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void rsaEncrypt_emptyToken() throws Exception {
        try {
            publicKeyExpectations();

            String result = utils.rsaEncrypt(clientConfig, "");
            assertTrue("Encrypted empty token is expected to also be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void rsaEncrypt() throws Exception {
        try {
            publicKeyExpectations();

            String result = utils.rsaEncrypt(clientConfig, ACCESS_TOKEN);
            assertEncryptedToken(RSA_ENC_TOKEN_LENGTH, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* rsaDecrypt *******************************************/

    @Test
    public void rsaDecrypt_nullToken() throws Exception {
        try {
            String result = utils.rsaDecrypt(clientConfig, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    //@Test
    public void rsaDecrypt_emptyToken() throws Exception {
        try {
            // TODO - test runs successfully locally, but fails in person builds
            privateKeyRSAExpectations();

            String result = utils.rsaDecrypt(clientConfig, "");
            assertTrue("Decrypted empty token is expected to also be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO - rsaDecrypt - valid token

    /******************************************* aesEncrypt *******************************************/

    @Test
    public void aesEncrypt_nullToken() throws Exception {
        try {
            String result = utils.aesEncrypt(clientConfig, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void aesEncrypt_emptyToken() throws Exception {
        try {
            privateKeyAESExpectations();

            String result = utils.aesEncrypt(clientConfig, "");
            assertEncryptedToken(32, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void aesEncrypt() throws Exception {
        try {
            privateKeyAESExpectations();

            String result = utils.aesEncrypt(clientConfig, ACCESS_TOKEN);
            assertEncryptedToken(AES_ENC_TOKEN_LENGTH, result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* aesDecrypt *******************************************/

    @Test
    public void aesDecrypt_nullToken() throws Exception {
        try {
            String result = utils.aesDecrypt(clientConfig, null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void aesDecrypt_emptyToken() throws Exception {
        try {
            privateKeyAESExpectations();

            String result = utils.aesDecrypt(clientConfig, "");
            assertTrue("Decrypted empty token is expected to also be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // TODO - aesDecrypt - valid token

    /******************************************* getBytes *******************************************/

    @Test
    public void getBytes_nullInput() throws Exception {
        try {
            byte[] result = utils.getBytes(cipher, null, 10);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_emptyInput() throws Exception {
        try {
            final String input = "";

            byte[] result = utils.getBytes(cipher, input.getBytes(), 10);
            assertEquals("Result was not empty when it should have been. Result: [" + result + "]", 0, result.length);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_negativeLength() throws Exception {
        try {
            final String input = "input";

            byte[] result = utils.getBytes(cipher, input.getBytes(), -10);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_zeroLength() throws Exception {
        try {
            final String input = "input";

            byte[] result = utils.getBytes(cipher, input.getBytes(), 0);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_smallLength() throws Exception {
        try {
            final String input = "input";

            publicKeyExpectations();

            Cipher cipher = Cipher.getInstance(CIPHER_RSA);
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);

            // The value will change with every run, but in any case the result should not be null
            byte[] result = utils.getBytes(cipher, input.getBytes(), 1);
            assertNotNull("Result was null when it should have been.", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBytes_largeLength() throws Exception {
        try {
            final String input = "input";

            publicKeyExpectations();

            Cipher cipher = Cipher.getInstance(CIPHER_RSA);
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);

            // The value will change with every run, but in any case the result should not be null
            byte[] result = utils.getBytes(cipher, input.getBytes(), 1000000);
            assertNotNull("Result was null when it should have been.", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getSecretKey *******************************************/

    @Test
    public void getSecretKey_nullClientSecret() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getClientSecret();
                    will(returnValue(null));
                }
            });
            Key result = utils.getSecretKey(clientConfig);
            verifyNonNullAesKey(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_emptyClientSecret() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getClientSecret();
                    will(returnValue(""));
                }
            });
            Key result = utils.getSecretKey(clientConfig);
            verifyNonNullAesKey(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_nonEmptyClientSecret() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getClientSecret();
                    will(returnValue(CLIENT_SECRET));
                }
            });
            Key result = utils.getSecretKey(clientConfig);
            verifyNonNullAesKey(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getIvSpec *******************************************/

    @Test
    public void getIvSpec_nullClientSecret() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getClientSecret();
                    will(returnValue(null));
                }
            });
            IvParameterSpec result = utils.getIvSpec(clientConfig);
            verifyNonNullIvSpec(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIvSpec_emptyClientSecret() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getClientSecret();
                    will(returnValue(""));
                }
            });
            IvParameterSpec result = utils.getIvSpec(clientConfig);
            verifyNonNullIvSpec(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIvSpec_nonEmptyClientSecret() {
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getClientSecret();
                    will(returnValue(CLIENT_SECRET));
                }
            });
            IvParameterSpec result = utils.getIvSpec(clientConfig);
            verifyNonNullIvSpec(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getClientSecretHash *******************************************/

    @Test
    public void getClientSecretHash_nullClientSecret() {
        try {
            final String clientSecret = null;
            byte[] result = utils.getClientSecretHash(clientSecret);
            assertNull("Result should have been null but was " + Arrays.toString(result), result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getClientSecretHash_emptyClientSecret() {
        try {
            final String clientSecret = "";
            byte[] result = utils.getClientSecretHash(clientSecret);
            verifyNonNullClientSecretHash(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getClientSecretHash_nonEmptyClientSecret() {
        try {
            final String clientSecret = CLIENT_SECRET;
            byte[] result = utils.getClientSecretHash(clientSecret);
            verifyNonNullClientSecretHash(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getMessageDigest *******************************************/

    @Test
    public void getMessageDigest_nullAlg() {
        try {
            String algorithm = null;
            MessageDigest result = utils.getMessageDigest(algorithm);
            assertNull("Result should have been null for a null algorithm string, but got MessageDigest with algorithm: " + (result != null ? result.getAlgorithm() : ""),
                    result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getMessageDigest_unknownAlg() {
        try {
            String algorithm = "Some unknown algorithm";
            MessageDigest result = utils.getMessageDigest(algorithm);
            assertNull("Result should have been null for an unknown algorithm string, but got MessageDigest with algorithm: " + (result != null ? result.getAlgorithm() : ""),
                    result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getMessageDigest_sha256() {
        try {
            String algorithm = "SHA-256";
            MessageDigest result = utils.getMessageDigest(algorithm);
            assertNotNull("Result should not have been null for the provided algorithm string [" + algorithm + "].", result);
            assertEquals("Actual algorithm did not match the input algorithm.", algorithm, result.getAlgorithm());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* bytesToHexString *******************************************/

    @Test
    public void bytesToHexString_null() throws Exception {
        try {
            String result = utils.bytesToHexString(null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void bytesToHexString_empty() throws Exception {
        try {
            String result = utils.bytesToHexString(new byte[0]);
            assertTrue("Result is expected to be an empty string. Result was: [" + result + "].", result.isEmpty());

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void bytesToHexString_singleByteChars() throws Exception {
        try {
            // Any unicode value that takes more than 127 bits to represent must be split into two separate bytes,
            // hence why '\u007f' (127) is still represented as "7f" while \u0080 (128) requires two bytes: "c2" + "80".
            String singleByteChars = "\u0000\u007f";
            byte[] input = singleByteChars.getBytes();

            String result = utils.bytesToHexString(input);
            assertEquals("Result did not match expected value.", "007f", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    // 20170420 bt/av: for unknown reasons, probably some codepage thing, this fails on windows.
    // todo: debug this
    @Test
    public void bytesToHexString_multiByteChars() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("window")) {
            try {
                // Any unicode value that takes more than 127 bits to represent must be split into two separate bytes,
                // hence why '\u007f' (127) is still represented as "7f" while \u0080 (128) requires two bytes: "c2" + "80".
                String charsStart128 = "\u0080\u00bf";
                String charsStart192 = "\u00c0\u00ff";
                String charsStart256 = "\u0100\u013f";
                //            String charsStart2048 = "\u0800\u083f";
                //            String charsStart65472 = "\uffc0\uffff";
                byte[] input = (charsStart128 + charsStart192 + charsStart256).getBytes();

                String result = utils.bytesToHexString(input);
                assertEquals("Result did not match expected value.", "c280c2bf" + "c380c3bf" + "c480c4bf", result);

                verifyNoLogMessage(MSG_BASE);

            } catch (Throwable t) {
                outputMgr.failWithThrowable(testName.getMethodName(), t);
            }
        }
    }

    /******************************************* hexStringToBytes *******************************************/

    @Test
    public void hexStringToBytes_null() throws Exception {
        try {
            byte[] result = utils.hexStringToBytes(null);
            assertNull("Result was not null when it should have been. Result: [" + result + "]", result);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hexStringToBytes_empty() throws Exception {
        try {
            byte[] result = utils.hexStringToBytes("");
            assertEquals("Result is expected to be an empty array. Result was: [" + result + "].", 0, result.length);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hexStringToBytes_singleChar() throws Exception {
        try {
            // The code currently expects the input string to be an EVEN number of characters. Any characters beyond the last character in an
            // even index will simply be ignored. Thus, an input with a single char will have its only character ignored.
            byte[] result = utils.hexStringToBytes("a");
            assertEquals("Result is expected to be an empty array. Result was: [" + result + "].", 0, result.length);

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void hexStringToBytes_notHexadecimal() throws Exception {
        try {
            final String logAndExceptionMsg = MSG_VALUE_NOT_HEXADECIMAL;

            try {
                byte[] result = utils.hexStringToBytes("Some non-hex value.");
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, logAndExceptionMsg);
            }

            verifyNoLogMessage(MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* End-to-end *******************************************/

    @Test
    public void endToEndEncryptionAndDecryption_AES() throws Exception {
        try {
            privateKeyAESExpectations();

            assertEquals("The encrypted access token must be able to be decrypted.", ACCESS_TOKEN,
                    utils.getDecryptedAccessToken(clientConfig, utils.getEncryptedAccessToken(clientConfig, ACCESS_TOKEN)));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void endToEndHexToBytes() throws Exception {
        try {
            String input = "0123456789abcdef";
            String result = utils.bytesToHexString(utils.hexStringToBytes(input));

            assertEquals("Byte to hex conversion result did not match input.", input, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* Helper methods *******************************************/

    private void publicKeyExpectations() throws SocialLoginException {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getAlgorithm();
                will(returnValue(RSA));
                allowing(clientConfig).getPublicKey();
                will(returnValue(rsaPublicKey));
                allowing(rsaPublicKey).getAlgorithm();
                will(returnValue(RSA));
                allowing(rsaPublicKey).getModulus();
                will(returnValue(keyModulus));
                allowing(rsaPublicKey).getPublicExponent();
                will(returnValue(keyExponent));
                allowing(rsaPublicKey).getFormat();
                will(returnValue(RSA_X509_FORMAT));
            }
        });
    }

    private void privateKeyRSAExpectations() throws SocialLoginException {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getAlgorithm();
                will(returnValue(RSA));
                one(clientConfig).getPrivateKey();
                will(returnValue(rsaPrivateKey));
                allowing(rsaPrivateKey).getAlgorithm();
                will(returnValue(RSA));
                allowing(rsaPrivateKey).getModulus();
                will(returnValue(keyModulus));
                allowing(rsaPrivateKey).getPrivateExponent();
                will(returnValue(keyExponent));
                allowing(rsaPrivateKey).getFormat();
                will(returnValue(RSA_X509_FORMAT));
            }
        });
    }

    private void privateKeyAESExpectations() throws SocialLoginException {
        mockery.checking(new Expectations() {
            {
                allowing(clientConfig).getAlgorithm();
                will(returnValue(AES));
                allowing(clientConfig).getClientSecret();
                will(returnValue(CLIENT_SECRET));
            }
        });
    }

    private void assertEncryptedToken(int expectedLength, String encryptedToken) {
        final String encTokenPatternString = LOWER_ALPHANUM_REGEX + "{" + expectedLength + "}";
        final Pattern encTokenPattern = Pattern.compile(encTokenPatternString);
        Matcher m = encTokenPattern.matcher(encryptedToken);
        assertTrue("Encrypted token value did not match expected pattern '" + encTokenPatternString + "'. Result was: [" + encryptedToken + "].", m.matches());
    }

    private void verifyNoLogMessage(String messageRegex) {
        verifyNoLogMessage(outputMgr, messageRegex);
    }

    private void verifyNonNullAesKey(Key key) {
        assertNotNull("Key should not have been null but was.", key);
        assertEquals("Algorithm did not match expected value.", AES, key.getAlgorithm());
        assertEquals("Byte length of encoded secret key for new password string did not match expected value.", 16, key.getEncoded().length);
        assertEquals("Format of the secret key did not match expected value.", "RAW", key.getFormat());
    }

    private void verifyNonNullIvSpec(IvParameterSpec iv) {
        assertNotNull("IvParameterSpec should not have been null but was.", iv);
        assertEquals("Byte length of initialization vector did not match expected value.", 16, iv.getIV().length);
    }

    private void verifyNonNullClientSecretHash(byte[] hash) {
        assertNotNull("Result should not have been null but was.", hash);
        assertEquals("Hash length did not match expected value.", 32, hash.length);
    }

}