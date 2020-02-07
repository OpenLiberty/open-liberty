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
package com.ibm.ws.crypto.certificateutil;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.List;

import org.junit.Test;

/**
 *
 */
public class DefaultSSLCertificateFactoryTest {

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateFactory#getDefaultSSLCertificateCreator()}.
     */
    @Test
    public void getDefaultSSLCertificateCreator() {
        assertNull("Was not the expected KeytoolSSLCertificateCreator instance",
                   DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator());
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateFactory#getDefaultSSLCertificateCreator()}.
     */
    @Test
    public void setDefaultSSLCertificateCreator() {
        /*
         * Create new implementation.
         */
        DefaultSSLCertificateCreator creator = new DefaultSSLCertificateCreator() {

            @Override
            public void updateDefaultSSLCertificate(KeyStore keyStore, File keyStoreFile, String password) throws CertificateException {
                // TODO Auto-generated method stub

            }

            @Override
            public File createDefaultSSLCertificate(String filePath, String password, int validity, String subjectDN, int keySize, String sigAlg,
                                                    List<String> extInfo) throws CertificateException {
                // TODO Auto-generated method stub
                return null;
            }
        };

        /*
         * Set it as the creator on the factory and retrieve it.
         */
        DefaultSSLCertificateFactory.setDefaultSSLCertificateCreator(creator);
        assertSame("Was not the expected KeytoolSSLCertificateCreator instance",
                   creator, DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator());
    }

}
