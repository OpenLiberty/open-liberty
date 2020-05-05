/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.certificateutil.keytool;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.DefaultSubjectDN;

/**
 * Unit tests for the {@link KeytoolSSLCertificateCreator} class.
 */
public class KeytoolSSLCertificateCreatorTest {
    private final KeytoolSSLCertificateCreator creator = new KeytoolSSLCertificateCreator();
    private final String location = System.getProperty("user.dir") + "/build/testKS.p12";
    List<String> san = new ArrayList<String>();

    @Before
    public void setUp() {
        File ks = new File(location);
        if (ks.exists()) {
            if (!ks.delete()) {
                fail("Failed to delete the keystore file, subsequent test will probably fail");
            }
        }

        san.add("SAN=dns:localhost");
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_nullLocation() throws Exception {
        creator.createDefaultSSLCertificate(null, "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_emptyLocation() throws Exception {
        creator.createDefaultSSLCertificate("", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_nullPassword() throws Exception {
        creator.createDefaultSSLCertificate("/", null,
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_emptyPassword() throws Exception {
        creator.createDefaultSSLCertificate("/", "",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_passwordTooShort() throws Exception {
        creator.createDefaultSSLCertificate("/", "WebAS",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_negativeValidity() throws Exception {
        creator.createDefaultSSLCertificate("/", null,
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            -1,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_zeroValidity() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            0,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_belowMinimumValidity() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.MINIMUM_VALIDITY - 1,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_nullSubjectDN() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                                      null,
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      null,
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.KEYALG,
                                                      san);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_emptySubjectDN() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                                      null,
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      "",
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.KEYALG,
                                                      san);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_invalidDN() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            "invalidDN",
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG,
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_invalidSigAlg() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            "BAD_SIG_ALG",
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_SigAlgWrongSize() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            "SHA256withECDSA",
                                            san);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolCommand#KeytoolCommand(java.lang.String, java.lang.String, int, java.lang.String)}.
     */
    @Test
    public void createKeytoolCommand_defaultValidity() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                                      null,
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      "CN=localhost",
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.SIGALG,
                                                      san);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolCommand#KeytoolCommand(java.lang.String, java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = CertificateException.class)
    public void createKeytoolCommand_doubleCallTriggersException() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                                      null,
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      "CN=localhost",
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.SIGALG,
                                                      san);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
        creator.createDefaultSSLCertificate(location, "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            "CN=localhost",
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.SIGALG,
                                            san);
    }
}
