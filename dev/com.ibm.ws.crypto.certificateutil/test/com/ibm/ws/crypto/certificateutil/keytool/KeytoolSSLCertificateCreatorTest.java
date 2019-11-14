/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.DefaultSubjectDN;

/**
 * This test class does not actually touch the file system. For tests that do,
 * see the BVT test by the same name.
 */
public class KeytoolSSLCertificateCreatorTest {
    private final KeytoolSSLCertificateCreator creator = new KeytoolSSLCertificateCreator();
    private final String location = System.getProperty("user.dir") + "/build/testKS.p12";
    List<String> san = new ArrayList<String>();

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_nullLocation() throws Exception {
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate(null, "Liberty",
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("", "Liberty",
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", null,
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", "",
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", "WebAS",
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", null, -1,
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", "Liberty", 0,
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", "Liberty",
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
        san.add("SAN=DNS:localhost");
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
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
        san.add("SAN=DNS:localhost");
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", "Liberty",
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", "Liberty",
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
        san.add("SAN=DNS:localhost");
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            "SHA256withECDSA",
                                            san);
    }
}
