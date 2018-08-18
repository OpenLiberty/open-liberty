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
package com.ibm.ws.security.social.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class SslRefInfoImplTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    SslRefInfoImpl refInfo = null;

    final SSLSupport sslSupport = mockery.mock(SSLSupport.class);
    final JSSEHelper jsseHelper = mockery.mock(JSSEHelper.class);
    final Properties properties = mockery.mock(Properties.class);
    final KeyStoreService keyStoreService = mockery.mock(KeyStoreService.class);
    final Certificate cert = mockery.mock(Certificate.class);
    final PublicKey publicKey = mockery.mock(PublicKey.class);
    final PrivateKey privateKey = mockery.mock(PrivateKey.class);

    final String sslRef = "mySslRef";
    final String keyAliasName = "myKeyAliasName";
    final String keyStoreName = "myKeyStoreName";
    final String trustStoreName = "myTrustStoreName";

    @SuppressWarnings("unchecked")
    final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = mockery.mock(AtomicServiceReference.class, "keyStoreServiceRef");

    public interface MockInterface {
        public void init() throws SocialLoginException;

        public HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException;
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);
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

    /************************************** init **************************************/

    @Test
    public void init_nullSslSupport() {
        try {
            refInfo = new SslRefInfoImpl(null, keyStoreServiceRef, sslRef, keyAliasName);

            refInfo.init();

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(null, keyStoreServiceRef, sslRef, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void init_noJsseHelper() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, null, null);

            mockery.checking(new Expectations() {
                {
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(null));
                }
            });

            refInfo.init();

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, null, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void init_gettingPropertiesThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, sslRef, null);

            mockery.checking(new Expectations() {
                {
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(jsseHelper));
                    one(jsseHelper).getProperties(sslRef);
                    will(throwException(new SSLException(defaultExceptionMsg)));
                }
            });

            try {
                refInfo.init();
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5466E_ERROR_LOADING_SSL_PROPS + ".*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, sslRef, jsseHelper, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void init_nullSslRef_nullProps() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, null, null);
            final Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(com.ibm.websphere.ssl.Constants.CONNECTION_INFO_DIRECTION, com.ibm.websphere.ssl.Constants.DIRECTION_INBOUND);

            mockery.checking(new Expectations() {
                {
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(jsseHelper));
                    one(jsseHelper).getProperties(null, connectionInfo, null, true);
                    will(returnValue(null));
                }
            });

            refInfo.init();

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, null, jsseHelper, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void init_validSslRef() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, sslRef, null);

            mockery.checking(new Expectations() {
                {
                    one(sslSupport).getJSSEHelper();
                    will(returnValue(jsseHelper));
                    one(jsseHelper).getProperties(sslRef);
                    will(returnValue(properties));
                    one(properties).getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_KEY_STORE_NAME);
                    will(returnValue(keyStoreName));
                    one(properties).getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_TRUST_STORE_NAME);
                    will(returnValue(trustStoreName));
                }
            });

            refInfo.init();

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getPublicKeys **************************************/

    @Test
    public void getPublicKeys_initThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, sslRef, null) {
                @Override
                void init() throws SocialLoginException {
                    mockInterface.init();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).init();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                HashMap<String, PublicKey> result = refInfo.getPublicKeys();
                fail("Should have thrown SocialLoginException but did not. Result was " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, sslRef, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_instantiateWithNullArgs() {
        try {
            refInfo = new SslRefInfoImpl(null, null, null, null);

            HashMap<String, PublicKey> result = refInfo.getPublicKeys();

            assertTrue("Map of keys returned should be empty but was not. Result was " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(null, null, null, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_missingKeyStoreService() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(null));
                }
            });

            try {
                HashMap<String, PublicKey> result = refInfo.getPublicKeys();
                fail("Should have thrown SocialLoginException but did not. Result was " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5467E_KEYSTORE_SERVICE_NOT_FOUND);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_gettingCertThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getTrustedCertEntriesInKeyStore(trustStoreName);
                    will(throwException(new KeyStoreException(defaultExceptionMsg)));
                }
            });

            try {
                HashMap<String, PublicKey> result = refInfo.getPublicKeys();
                fail("Should have thrown SocialLoginException but did not. Result was " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5468E_ERROR_LOADING_KEYSTORE_CERTIFICATES + ".*\\[" + trustStoreName + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_emptyCertEntries() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null);
            final Set<String> certEntries = new HashSet<String>();

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getTrustedCertEntriesInKeyStore(trustStoreName);
                    will(returnValue(certEntries));
                }
            });

            HashMap<String, PublicKey> result = refInfo.getPublicKeys();

            assertTrue("Map of keys returned should be empty but was not. Result was " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_certEntryDoesNotExist() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null);
            final String badEntry = "some entry";
            final Set<String> certEntries = new HashSet<String>();
            certEntries.add(badEntry);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getTrustedCertEntriesInKeyStore(trustStoreName);
                    will(returnValue(certEntries));
                    one(keyStoreService).getCertificateFromKeyStore(trustStoreName, badEntry);
                    will(throwException(new CertificateException(defaultExceptionMsg)));
                }
            });

            try {
                HashMap<String, PublicKey> result = refInfo.getPublicKeys();
                fail("Should have thrown SocialLoginException but did not. Result was " + result);
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5469E_ERROR_LOADING_CERTIFICATE + ".*\\[" + badEntry + "\\].*\\[" + trustStoreName + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKeys_validKey() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null);
            final String certAlias = "some entry";
            final Set<String> certEntries = new HashSet<String>();
            certEntries.add(certAlias);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getTrustedCertEntriesInKeyStore(trustStoreName);
                    will(returnValue(certEntries));
                    one(keyStoreService).getCertificateFromKeyStore(trustStoreName, certAlias);
                    will(returnValue(cert));
                    one(cert).getPublicKey();
                }
            });

            HashMap<String, PublicKey> result = refInfo.getPublicKeys();

            assertNotNull("Map of keys returned should not be null.", result);
            assertEquals("Size of map of keys returned did not match expected value. Result was " + result, 1, result.size());
            assertTrue("Map of keys returned did not contain expected key. Result was " + result, result.containsKey(certAlias));

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getPublicKey **************************************/

    @Test
    public void getPublicKey_initThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, sslRef, null) {
                @Override
                void init() throws SocialLoginException {
                    mockInterface.init();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).init();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                PublicKey result = refInfo.getPublicKey();
                fail("Should have thrown SocialLoginException but did not. Result was " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, sslRef, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_instantiateWithNullArgs() {
        try {
            refInfo = new SslRefInfoImpl(null, null, null, null);

            PublicKey result = refInfo.getPublicKey();

            assertNull("Result should have been null but was not. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(null, null, null, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_nullOrEmptyKeyAlias_getPublicKeysThrowsException() {
        try {
            final String keyAliasName = RandomUtils.getRandomSelection(null, "");
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName) {
                @Override
                public HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException {
                    return mockInterface.getPublicKeys();
                }
            };

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getPublicKeys();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                PublicKey result = refInfo.getPublicKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5470E_ERROR_LOADING_GETTING_PUBLIC_KEYS + ".*\\[" + keyAliasName + "\\].*\\[" + trustStoreName + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_nullKeyAlias_noPublicKeys() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null) {
                @Override
                public HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException {
                    return mockInterface.getPublicKeys();
                }
            };

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getPublicKeys();
                    will(returnValue(new HashMap<String, PublicKey>()));
                }
            });

            PublicKey result = refInfo.getPublicKey();
            assertNull("Public key result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_nullKeyAlias_withPublicKeys() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null) {
                @Override
                public HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException {
                    return mockInterface.getPublicKeys();
                }
            };
            final Map<String, PublicKey> publicKeys = new HashMap<String, PublicKey>();
            publicKeys.put("some key", publicKey);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getPublicKeys();
                    will(returnValue(publicKeys));
                }
            });

            PublicKey result = refInfo.getPublicKey();
            assertNotNull("Public key result should not have been null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_validKeyAlias_missingKeyStoreService() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(null));
                }
            });

            try {
                PublicKey result = refInfo.getPublicKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5467E_KEYSTORE_SERVICE_NOT_FOUND);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_certEntryDoesNotExist() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getCertificateFromKeyStore(keyStoreName, keyAliasName);
                    will(throwException(new CertificateException(defaultExceptionMsg)));
                }
            });

            try {
                PublicKey result = refInfo.getPublicKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5469E_ERROR_LOADING_CERTIFICATE + ".*\\[" + keyAliasName + "\\].*\\[" + trustStoreName + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPublicKey_validKey() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getCertificateFromKeyStore(keyStoreName, keyAliasName);
                    will(returnValue(cert));
                    one(cert).getPublicKey();
                }
            });

            PublicKey result = refInfo.getPublicKey();

            assertNotNull("Key returned should not be null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getPrivateKey **************************************/

    @Test
    public void getPrivateKey_initThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, sslRef, null) {
                @Override
                void init() throws SocialLoginException {
                    mockInterface.init();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).init();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                PrivateKey result = refInfo.getPrivateKey();
                fail("Should have thrown SocialLoginException but did not. Result was " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, sslRef, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_instantiateWithNullArgs() {
        try {
            refInfo = new SslRefInfoImpl(null, null, null, null);

            PrivateKey result = refInfo.getPrivateKey();

            assertNull("Result should have been null but was not. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(null, null, null, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_missingKeyStoreService() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(null));
                }
            });

            try {
                PrivateKey result = refInfo.getPrivateKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5467E_KEYSTORE_SERVICE_NOT_FOUND);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_nullOrEmptyKeyAlias_gettingPrivateKeyThrowsException() {
        try {
            String keyAlias = RandomUtils.getRandomSelection(null, "");
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAlias);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getPrivateKeyFromKeyStore(keyStoreName);
                    will(throwException(new KeyStoreException(defaultExceptionMsg)));
                }
            });

            try {
                PrivateKey result = refInfo.getPrivateKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5472E_ERROR_LOADING_PRIVATE_KEY + ".*\\[" + keyStoreName + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_nullKeyAlias_validKey() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, null);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getPrivateKeyFromKeyStore(keyStoreName);
                    will(returnValue(privateKey));
                }
            });

            PrivateKey result = refInfo.getPrivateKey();
            assertNotNull("Private key returned should not have been null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_gettingPrivateKeyThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getPrivateKeyFromKeyStore(keyStoreName, keyAliasName, null);
                    will(throwException(new CertificateException(defaultExceptionMsg)));
                }
            });

            try {
                PrivateKey result = refInfo.getPrivateKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5471E_ERROR_LOADING_SPECIFIC_PRIVATE_KEY + ".*\\[" + keyAliasName + "\\].*\\[" + keyStoreName + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getPrivateKey_validKey() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getPrivateKeyFromKeyStore(keyStoreName, keyAliasName, null);
                }
            });

            PrivateKey result = refInfo.getPrivateKey();

            assertNotNull("Key returned should not be null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getSecretKey **************************************/

    @Test
    public void getSecretKey_initThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, null, sslRef, null) {
                @Override
                void init() throws SocialLoginException {
                    mockInterface.init();
                }
            };

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).init();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                SecretKey result = refInfo.getSecretKey();
                fail("Should have thrown SocialLoginException but did not. Result was " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, null, sslRef, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_instantiateWithNullArgs() {
        try {
            refInfo = new SslRefInfoImpl(null, null, null, null);

            SecretKey result = refInfo.getSecretKey();

            assertNull("Result should have been null but was not. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(null, null, null, null, null, null);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_nullOrEmptyKeyAlias() {
        try {
            String keyAlias = RandomUtils.getRandomSelection(null, "");
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAlias);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);

            SecretKey result = refInfo.getSecretKey();

            assertNull("Result should have been null but was not. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_missingKeyStoreService() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(null));
                }
            });

            try {
                SecretKey result = refInfo.getSecretKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5467E_KEYSTORE_SERVICE_NOT_FOUND);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_gettingPrivateKeyThrowsException() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getSecretKeyFromKeyStore(keyStoreName, keyAliasName, null);
                    will(throwException(new KeyStoreException(defaultExceptionMsg)));
                }
            });

            try {
                SecretKey result = refInfo.getSecretKey();
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5473E_ERROR_LOADING_SECRET_KEY + ".*\\[" + keyAliasName + "\\].*\\[" + keyStoreName + "\\].*" + Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSecretKey_validKey() {
        try {
            refInfo = new SslRefInfoImpl(sslSupport, keyStoreServiceRef, sslRef, keyAliasName);

            getGoodPropertyExpectations(keyStoreName, trustStoreName);
            mockery.checking(new Expectations() {
                {
                    one(keyStoreServiceRef).getService();
                    will(returnValue(keyStoreService));
                    one(keyStoreService).getSecretKeyFromKeyStore(keyStoreName, keyAliasName, null);
                }
            });

            SecretKey result = refInfo.getSecretKey();

            assertNotNull("Key returned should not be null.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

            assertMemberValues(sslSupport, keyStoreServiceRef, sslRef, jsseHelper, keyStoreName, trustStoreName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    void getGoodPropertyExpectations(final String keyStoreName, final String trustStoreName) throws SSLException {
        mockery.checking(new Expectations() {
            {
                one(sslSupport).getJSSEHelper();
                will(returnValue(jsseHelper));
                one(jsseHelper).getProperties(sslRef);
                will(returnValue(properties));
                one(properties).getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_KEY_STORE_NAME);
                will(returnValue(keyStoreName));
                one(properties).getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_TRUST_STORE_NAME);
                will(returnValue(trustStoreName));
            }
        });

    }

    void assertMemberValues(SSLSupport sslSupport, AtomicServiceReference<KeyStoreService> keyStoreServiceRef, String sslRef, JSSEHelper jsseHelper, String keystoreName, String truststoreName) {
        if (sslSupport == null) {
            assertNull("SSLSupport object should have been null but was not.", refInfo.sslSupport);
        } else {
            assertNotNull("SSLSupport object should not have been null.", refInfo.sslSupport);
        }
        if (keyStoreServiceRef == null) {
            assertNull("Keystore service ref should have been null but was not.", refInfo.keyStoreServiceRef);
        } else {
            assertNotNull("Keystore service ref should not have been null.", refInfo.keyStoreServiceRef);
        }
        if (sslRef == null) {
            assertNull("sslRef should have been null but was not.", refInfo.sslRef);
        } else {
            assertEquals("sslRef value did not match expected value.", sslRef, refInfo.sslRef);
        }
        if (jsseHelper == null) {
            assertNull("JSSEHelper object should have been null but was not.", refInfo.jsseHelper);
        } else {
            assertNotNull("JSSEHelper object should not have been null.", refInfo.jsseHelper);
        }
        if (keystoreName == null) {
            assertNull("SSL keystore name should have been null but was not.", refInfo.sslKeyStoreName);
        } else {
            assertEquals("SSL keystore name did not match expected value.", keystoreName, refInfo.sslKeyStoreName);
        }
        if (truststoreName == null) {
            assertNull("SSL truststore name should have been null but was not.", refInfo.sslTrustStoreName);
        } else {
            assertEquals("SSL truststore name did not match expected value.", truststoreName, refInfo.sslTrustStoreName);
        }
    }

}
