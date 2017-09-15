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
package com.ibm.ws.crypto.certificateutil.keytool.bvt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator;

/**
 * These are not true unit tests as they drive a tool and affect the file system.
 * They are in the BVT bucket for that reason.
 */
public class KeytoolSSLCertificateCreatorTest {
    private final KeytoolSSLCertificateCreator creator = new KeytoolSSLCertificateCreator();
    private final String location = System.getProperty("user.dir") + "/build/testKS.jks";

    @Before
    public void setUp() {
        File ks = new File(location);
        if (ks.exists()) {
            if (!ks.delete()) {
                fail("Failed to delete the keystore file, subsequent test will probably fail");
            }
        }
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolCommand#KeytoolCommand(java.lang.String, java.lang.String, int, java.lang.String)}.
     */
    @Test
    public void createKeytoolCommand_minimumValidity() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.MINIMUM_VALIDITY,
                                                      "CN=localhost",
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.SIGALG);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolCommand#KeytoolCommand(java.lang.String, java.lang.String, int, java.lang.String)}.
     */
    @Test
    public void createKeytoolCommand_defaultValidity() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      "CN=localhost",
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.SIGALG);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolCommand#KeytoolCommand(java.lang.String, java.lang.String, int, java.lang.String)}.
     */
    @Test(expected = CertificateException.class)
    public void createKeytoolCommand_doubleCallTriggersException() throws Exception {
        File ks = creator.createDefaultSSLCertificate(location, "Liberty",
                                                      DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                      "CN=localhost",
                                                      DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                      DefaultSSLCertificateCreator.SIGALG);
        assertNotNull("keystore was not created", ks);
        assertTrue("keystore was not created", ks.exists());
        creator.createDefaultSSLCertificate(location, "Liberty",
                                            DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                            "CN=localhost",
                                            DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                            DefaultSSLCertificateCreator.SIGALG);
    }
}
