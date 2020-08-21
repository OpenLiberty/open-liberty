/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.ws.ssl.config.WSKeyStore;

import test.common.SharedOutputManager;

/**
 *
 */
public class KeyStoreServiceImplTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static final class MockKeyStore extends KeyStore {
        protected MockKeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type) {
            super(keyStoreSpi, provider, type);
        }
    }

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final KeyStoreManager ksMgr = mock.mock(KeyStoreManager.class);
    private final KeyStoreSpi kspi = mock.mock(KeyStoreSpi.class);
    private final KeyStore ks = new MockKeyStore(kspi, null, null);
    private final WSKeyStore wsKS = mock.mock(WSKeyStore.class);
    private final X509Certificate x509Cert = mock.mock(X509Certificate.class);
    private final Certificate cert = mock.mock(Certificate.class);
    @SuppressWarnings("unchecked")
    private final Enumeration<String> trustedCertEnum = mock.mock(Enumeration.class);
    private final Key key = mock.mock(Key.class);
    private final PrivateKey privKey = mock.mock(PrivateKey.class);
    private KeyStoreServiceImpl keyStoreService;

    @Before
    public void setUp() throws Exception {

        mock.checking(new Expectations() {
            {
                one(kspi).engineLoad(null);
            }
        });
        ks.load(null);

        keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.setKeyStore(null);
        keyStoreService.activate();
        keyStoreService.ksMgr = ksMgr;
    }

    @After
    public void tearDown() throws Exception {
        keyStoreService.deactivate();
        keyStoreService.unsetKeyStore(null);
        keyStoreService = null;
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getKeyStoreLocation(java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getKeyStoreLocation_noSuchKeyStore() throws Exception {
        final String keyStoreName = "validKS";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(null));
            }
        });

        keyStoreService.getKeyStoreLocation(keyStoreName);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getKeyStoreLocation(java.lang.String)}.
     */
    @Test
    public void getKeyStoreLocation_validKeyStore() throws Exception {
        final String keyStoreName = "validKS";
        final String location = "";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).getLocation();
                will(returnValue(location));
            }
        });

        assertEquals("FAIL: did not get expected location",
                     location, keyStoreService.getKeyStoreLocation(keyStoreName));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getTrustedCertEntriesInKeyStore(java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getTrustedCertEntriesInKeyStore_noSuchKeyStore() throws Exception {
        final String keyStoreName = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(null));
            }
        });

        keyStoreService.getTrustedCertEntriesInKeyStore(keyStoreName);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getTrustedCertEntriesInKeyStore(java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getTrustedCertEntriesInKeyStore_unexpectedError() throws Exception {
        final String keyStoreName = "validKS";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(throwException(new Exception("Test Exception ")));
            }
        });

        keyStoreService.getTrustedCertEntriesInKeyStore(keyStoreName);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getTrustedCertEntriesInKeyStore(java.lang.String)}.
     */
    @Test
    public void getTrustedCertEntriesInKeyStore_nullCertEntries() throws Exception {
        final String keyStoreName = "validKS";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineAliases();
                will(returnValue(null));
            }
        });

        Collection<String> trustedCerts = keyStoreService.getTrustedCertEntriesInKeyStore(keyStoreName);
        assertTrue("There should be no trusted certificates",
                   trustedCerts.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getTrustedCertEntriesInKeyStore(java.lang.String)}.
     */
    @Test
    public void getTrustedCertEntriesInKeyStore_emptyCertEntries() throws Exception {
        final String keyStoreName = "validKS";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineAliases();
                will(returnValue(trustedCertEnum));

                one(trustedCertEnum).hasMoreElements();
                will(returnValue(false));
            }
        });

        Collection<String> trustedCerts = keyStoreService.getTrustedCertEntriesInKeyStore(keyStoreName);
        assertTrue("There should be no trusted certificates",
                   trustedCerts.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getTrustedCertEntriesInKeyStore(java.lang.String)}.
     */
    @Test
    public void getTrustedCertEntriesInKeyStore_notCertEntry() throws Exception {
        final String keyStoreName = "validKS";
        final String certAlias = "cert1";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineAliases();
                will(returnValue(trustedCertEnum));

                one(trustedCertEnum).hasMoreElements();
                will(returnValue(true));

                one(trustedCertEnum).nextElement();
                will(returnValue(certAlias));

                one(kspi).engineIsCertificateEntry(certAlias);
                will(returnValue(false));

                one(trustedCertEnum).hasMoreElements();
                will(returnValue(false));
            }
        });

        Collection<String> trustedCerts = keyStoreService.getTrustedCertEntriesInKeyStore(keyStoreName);
        assertTrue("There should be no trusted certificates",
                   trustedCerts.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getTrustedCertEntriesInKeyStore(java.lang.String)}.
     */
    @Test
    public void getTrustedCertEntriesInKeyStore_atLeastOneCertEntries() throws Exception {
        final String keyStoreName = "validKS";
        final String certAlias = "cert1";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineAliases();
                will(returnValue(trustedCertEnum));

                one(trustedCertEnum).hasMoreElements();
                will(returnValue(true));

                one(trustedCertEnum).nextElement();
                will(returnValue(certAlias));

                one(kspi).engineIsCertificateEntry(certAlias);
                will(returnValue(true));

                one(kspi).engineGetCertificate(certAlias);
                will(returnValue(cert));

                one(trustedCertEnum).hasMoreElements();
                will(returnValue(false));
            }
        });

        Collection<String> trustedCerts = keyStoreService.getTrustedCertEntriesInKeyStore(keyStoreName);
        assertFalse("There should be one trusted certificate entry",
                    trustedCerts.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getCertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getCertificateFromKeyStore_unexpectedError() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(throwException(new Exception("Test Exception ")));
            }
        });

        keyStoreService.getCertificateFromKeyStore(keyStoreName, alias);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getCertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getCertificateFromKeyStore_noSuchKeyStore() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(null));
            }
        });

        keyStoreService.getCertificateFromKeyStore(keyStoreName, alias);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getCertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = CertificateException.class)
    public void getCertificateFromKeyStore_noSuchAlias() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                allowing(kspi).engineIsCertificateEntry(alias);
                will(returnValue(false));

                allowing(kspi).engineIsKeyEntry(alias);
                will(returnValue(false));
            }
        });

        keyStoreService.getCertificateFromKeyStore(keyStoreName, alias);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getCertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getCertificateFromKeyStore_cert() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineIsCertificateEntry(alias);
                will(returnValue(true));

                one(kspi).engineGetCertificate(alias);
                will(returnValue(x509Cert));
            }
        });

        assertSame("FAIL: did not get back expected mock",
                   x509Cert, keyStoreService.getCertificateFromKeyStore(keyStoreName, alias));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getCertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getCertificateFromKeyStore_x509CertFromKey() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineIsCertificateEntry(alias);
                will(returnValue(false));

                allowing(kspi).engineIsKeyEntry(alias);
                will(returnValue(true));

                one(kspi).engineGetCertificate(alias);
                will(returnValue(x509Cert));
            }
        });

        assertSame("FAIL: did not get back expected mock",
                   x509Cert, keyStoreService.getX509CertificateFromKeyStore(keyStoreName, alias));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getX509CertificateFromKeyStore_unexpectedError() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(throwException(new Exception("Test Exception ")));
            }
        });

        keyStoreService.getX509CertificateFromKeyStore(keyStoreName, alias);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getX509CertificateFromKeyStore_noSuchKeyStore() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(null));
            }
        });

        keyStoreService.getX509CertificateFromKeyStore(keyStoreName, alias);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = CertificateException.class)
    public void getX509CertificateFromKeyStore_noSuchAlias() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                allowing(kspi).engineIsCertificateEntry(alias);
                will(returnValue(false));

                allowing(kspi).engineIsKeyEntry(alias);
                will(returnValue(false));
            }
        });

        keyStoreService.getX509CertificateFromKeyStore(keyStoreName, alias);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = CertificateException.class)
    public void getX509CertificateFromKeyStore_nonX509Cert() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineIsCertificateEntry(alias);
                will(returnValue(true));

                one(kspi).engineGetCertificate(alias);
                will(returnValue(cert));
            }
        });

        keyStoreService.getX509CertificateFromKeyStore(keyStoreName, alias);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getX509CertificateFromKeyStore_x509Cert() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineIsCertificateEntry(alias);
                will(returnValue(true));

                one(kspi).engineGetCertificate(alias);
                will(returnValue(x509Cert));
            }
        });

        assertSame("FAIL: did not get back expected mock",
                   x509Cert, keyStoreService.getX509CertificateFromKeyStore(keyStoreName, alias));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getX509CertificateFromKeyStore_x509CertFromKey() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));

                one(kspi).engineIsCertificateEntry(alias);
                will(returnValue(false));

                allowing(kspi).engineIsKeyEntry(alias);
                will(returnValue(true));

                one(kspi).engineGetCertificate(alias);
                will(returnValue(x509Cert));
            }
        });

        assertSame("FAIL: did not get back expected mock",
                   x509Cert, keyStoreService.getX509CertificateFromKeyStore(keyStoreName, alias));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(String, String, String))}.
     */
    @Test(expected = KeyStoreException.class)
    public void getPrivateKeyFromKeyStore_unexpectedError() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";
        final String keyPassword = "keyPassword";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(throwException(new Exception("Test Exception ")));
            }
        });

        keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, alias, keyPassword);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void getPrivateKeyFromKeyStore_noSuchKeyStore() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";
        final String keyPassword = "keyPassword";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(null));
            }
        });

        keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, alias, keyPassword);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test(expected = CertificateException.class)
    public void getPrivateKeyFromKeyStore_noSuchAlias() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "idontexist";
        final String keyPassword = "keyPassword";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).getKey(alias, keyPassword);
                will(throwException(new CertificateException("Test Exception ")));

            }
        });

        keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, alias, keyPassword);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test(expected = CertificateException.class)
    public void getPrivateKeyFromKeyStore_nonPrivateKey() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";
        final String keyPassword = "keyPassword";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).getKey(alias, keyPassword);
                will(returnValue(key));

            }
        });

        keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, alias, keyPassword);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void getPrivateKeyFromKeyStore_specifiedKeyPassword() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";
        final String keyPassword = "keyPassword";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).getKey(alias, keyPassword);
                will(returnValue(privKey));
            }
        });

        assertSame("FAIL: did not get back expected mock PrivateKey",
                   privKey, keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, alias, keyPassword));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void getPrivateKeyFromKeyStore_nullWSKeyPassword() throws Exception {
        final String keyStoreName = "validAlias";
        final String alias = "validAlias";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).getKey(alias, null);
                will(returnValue(privKey));
            }
        });

        keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, alias, null);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void getPrivateKeyFromKeyStore_nullKeyPassword() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";
        final String wsKeyPassword = "wsKeyPassword";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).getKey(alias, null);
                will(returnValue(privKey));
            }
        });

        keyStoreService.getPrivateKeyFromKeyStore(keyStoreName, alias, null);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#addCertificateToKeyStore(java.lang.String, java.lang.String, java.security.cert.Certificate)}.
     */
    @Test
    public void addCertificateToKeyStore_success() throws Exception {
        final String keyStoreName = "validKS";
        final String alias = "validAlias";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).setCertificateEntry(alias, cert);
                one(wsKS).store();
            }
        });

        keyStoreService.addCertificateToKeyStore(keyStoreName, alias, cert);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getPrivateKeyFromKeyStore(java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void addCertificateToKeyStore_noSuchKeyStore() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(null));
            }
        });

        keyStoreService.addCertificateToKeyStore(keyStoreName, alias, cert);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void addCertificateToKeyStore_unexpectedError() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(throwException(new Exception("Test Exception ")));
            }
        });

        keyStoreService.addCertificateToKeyStore(keyStoreName, alias, cert);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getX509CertificateFromKeyStore(java.lang.String, java.lang.String)}.
     */
    @Test(expected = KeyStoreException.class)
    public void addCertificateToKeyStore_setCertificateError() throws Exception {
        final String keyStoreName = "idontexist";
        final String alias = "idontexist";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getKeyStore(keyStoreName);
                will(returnValue(wsKS));

                one(wsKS).setCertificateEntry(alias, cert);
                will(throwException(new KeyStoreException("Test Exception ")));
            }
        });

        keyStoreService.addCertificateToKeyStore(keyStoreName, alias, cert);
    }

    @Test
    public void testKeyStoreCount() {
        mock.checking(new Expectations() {
            {
                allowing(ksMgr).getKeyStoreCount();
                will(returnValue(45));
            }
        });

        assertTrue("count was:" + keyStoreService.getKeyStoreCount(), keyStoreService.getKeyStoreCount() == 45);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.KeyStoreServiceImpl#getKeyStore(java.lang.String)}.
     */
    @Test
    public void getKeyStore_success() throws Exception {
        final String keyStoreName = "validKS";

        mock.checking(new Expectations() {
            {
                one(ksMgr).getJavaKeyStore(keyStoreName);
                will(returnValue(ks));
            }
        });

        assertEquals("FAIL: did not get expected keyStore",
                     ks, keyStoreService.getKeyStore(keyStoreName));
    }
}
