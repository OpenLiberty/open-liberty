/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ssl.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.ssl.ConsoleWrapper;
import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.ws.ssl.config.WSKeyStore;

/**
 *
 */
public class WSX509TrustManagerTest {
    private static final String AUTH_TYPE = "ignored";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stdout = mock.mock(PrintStream.class, "stdout");
    private final TrustManager mockTM = mock.mock(TrustManager.class);
    private final X509TrustManager mockX509TM = mock.mock(X509TrustManager.class);
    private final TrustManager[] mockTM1 = { mockX509TM };
    private final TrustManager[] emptyTMs = {};
    private final TrustManager[] mockTMs = { mockTM, mockX509TM };
    private final X509Certificate mockX509Root = mock.mock(X509Certificate.class, "mockX509Root");
    private final X509Certificate mockX509Leaf = mock.mock(X509Certificate.class, "mockX509Leaf");
    private final X509Certificate[] mockX509s = { mockX509Leaf, mockX509Root };
    private final WSKeyStore WSKS = mock.mock(WSKeyStore.class, "WSKS");
    KeyStoreManager ksMgr = mock.mock(KeyStoreManager.class, "ksMgr");
    private final SSLConfig sslCfg = mock.mock(SSLConfig.class);
    private WSX509TrustManager tm = new WSX509TrustManager(mockTM1, null, sslCfg, "defaultTrustStore", "\temp\trust.jks");

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.core.WSX509TrustManager#isYes(String)}.
     */
    @Test
    public void isYes_y() {
        assertTrue("'y' should result in true", tm.isYes("y"));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.core.WSX509TrustManager#isYes(String)}.
     */
    @Test
    public void isYes_Y() {
        assertTrue("'Y' should result in true", tm.isYes("y"));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.core.WSX509TrustManager#isYes(String)}.
     */
    @Test
    public void isYes_yes() {
        assertTrue("'yes' should result in true", tm.isYes("yes"));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.core.WSX509TrustManager#isYes(String)}.
     */
    @Test
    public void isYes_Yes() {
        assertTrue("'Yes' should result in true", tm.isYes("Yes"));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.core.WSX509TrustManager#isYes(String)}.
     */
    @Test
    public void isYes_YES() {
        assertTrue("'YES' should result in true", tm.isYes("YES"));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.core.WSX509TrustManager#isYes(String)}.
     */
    @Test
    public void isYes_other() {
        assertFalse("anything else should result in false", tm.isYes("n"));
    }

    @Test
    /**
     * Test checkServerTrust and is not trusted
     */
    public void checkServerTrusted_notTrusted() throws CertificateException {
        tm = new WSX509TrustManager(mockTM1, null, sslCfg, "defaultTrustStore", "\temp\trust.jks");

        mock.checking(new Expectations() {
            {

                one(mockX509TM).checkServerTrusted(mockX509s, AUTH_TYPE);
                will(throwException(new CertificateException("Cert not Trusted")));

                one(mockX509Leaf).getSubjectDN();

                one(mockX509Leaf).getNotBefore();
                will(returnValue(new Date(950308100)));

                one(mockX509Leaf).getNotAfter();
                will(returnValue(new Date(1896215300)));

            }
        });
        try {
            tm.checkServerTrusted(mockX509s, AUTH_TYPE);
        } catch (CertificateException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    /**
     * Test checkServerTrusted, trusted
     */
    public void checkServerTrusted_trusted() throws CertificateException {
        tm = new WSX509TrustManager(mockTM1, null, sslCfg, "defaultTrustStore", "\temp\trust.jks");

        mock.checking(new Expectations() {
            {

                allowing(mockX509TM).checkServerTrusted(mockX509s, AUTH_TYPE);

            }
        });
        tm.checkServerTrusted(mockX509s, AUTH_TYPE);
    }

    /**
     * Test userAcceptedPrompt, answer Yes
     */
    @Test
    public void userAcceptedPrompt_sayY() throws Exception {
        if (!System.getProperty("java.version").startsWith("1."))
            return;

        tm = new WSX509TrustManager(mockTM1, stdin, stdout, false);

        mock.checking(new Expectations() {
            {
                allowing(stdout).println(with(any(String.class)));

                one(mockX509Leaf).getSubjectDN();
                one(mockX509Leaf).getIssuerDN();
                one(mockX509Leaf).getSerialNumber();
                one(mockX509Leaf).getNotAfter();
                one(mockX509Leaf).getEncoded(); // once for full MD5, once for printed sha-1, once for printed md5

                one(mockX509Root).getSubjectDN();
                one(mockX509Root).getIssuerDN();
                one(mockX509Root).getSerialNumber();
                one(mockX509Root).getNotAfter();
                one(mockX509Root).getEncoded(); // once for full MD5, once for printed sha-1, once for printed md5

                one(stdin).readText(with(any(String.class)));
                will(returnValue("y"));
            }
        });

        assertTrue(tm.userAcceptedPrompt(mockX509s));
    }

    /**
     * Test method for userAcceptedPrompt, answers N
     */
    @Test
    public void userAcceptedPrompt_sayN() throws Exception {
        if (!System.getProperty("java.version").startsWith("1."))
            return;

        tm = new WSX509TrustManager(mockTM1, stdin, stdout, false);

        mock.checking(new Expectations() {
            {
                allowing(stdout).println(with(any(String.class)));

                one(mockX509Leaf).getSubjectDN();
                one(mockX509Leaf).getIssuerDN();
                one(mockX509Leaf).getSerialNumber();
                one(mockX509Leaf).getNotAfter();
                one(mockX509Leaf).getEncoded(); // once for full MD5, once for printed sha-1, once for printed md5

                one(mockX509Root).getSubjectDN();
                one(mockX509Root).getIssuerDN();
                one(mockX509Root).getSerialNumber();
                one(mockX509Root).getNotAfter();
                one(mockX509Root).getEncoded(); // once for full MD5, once for printed sha-1, once for printed md5

                one(stdin).readText(with(any(String.class)));
                will(returnValue("n"));
            }
        });

        assertFalse(tm.userAcceptedPrompt(mockX509s));
    }

    /**
     * Test method for userAcceptedPrompt, answers N
     */
    @Test
    public void setCertificateToTruststore_noKeyStore() throws Exception {
        tm = new WSX509TrustManager(mockTM1, null, sslCfg, "defaultTrustStore", "\temp\trust.jks");

        try {
            tm.setCertificateToTruststore(mockX509s);
        } catch (Exception e) {
            assertEquals(e.getMessage(), "Keystore defaultTrustStore does not exist in the configuration.");
        }

    }

}
