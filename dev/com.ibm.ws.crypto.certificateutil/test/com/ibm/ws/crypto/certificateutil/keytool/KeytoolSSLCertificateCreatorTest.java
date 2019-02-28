/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import org.junit.Test;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.DefaultSubjectDN;

/**
 * This test class does not actually touch the file system. For tests that do,
 * see the BVT test by the same name.
 */
public class KeytoolSSLCertificateCreatorTest {
    private final KeytoolSSLCertificateCreator creator = new KeytoolSSLCertificateCreator();
    private final String location = System.getProperty("user.dir") + "/build/testKS.jks";

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_nullLocation() throws Exception {
        creator.createDefaultSSLCertificate(null, "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_emptyLocation() throws Exception {
        creator.createDefaultSSLCertificate("", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_nullPassword() throws Exception {
        creator.createDefaultSSLCertificate("/", null,
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_emptyPassword() throws Exception {
        creator.createDefaultSSLCertificate("/", "",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_passwordTooShort() throws Exception {
        creator.createDefaultSSLCertificate("/", "WebAS",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_negativeValidity() throws Exception {
        creator.createDefaultSSLCertificate("/", null, -1,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_zeroValidity() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty", 0,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_belowMinimumValidity() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.MINIMUM_VALIDITY - 1,
                                            new DefaultSubjectDN().getSubjectDN(),
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_nullSubjectDN() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      null,
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.KEYALG);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_emptySubjectDN() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      "",
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.KEYALG);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_invalidDN() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            "invalidDN",
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.KEYALG);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_invalidSigAlg() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            "BAD_SIG_ALG");
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator#KeytoolCommand(java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createKeytoolCommand_SigAlgWrongSize() throws Exception {
        creator.createDefaultSSLCertificate("/", "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            null,
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            "SHA256withECDSA");
    }
}
