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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator;

/**
 *
 */
public class DefaultSSLCertificateFactoryTest {

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateFactory#getDefaultSSLCertificateCreator()}.
     */
    @Test
    public void getDefaultSSLCertificateCreator() {
        assertTrue("Was not the expected KeytoolSSLCertificateCreator instance",
                   DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator() instanceof KeytoolSSLCertificateCreator);
    }

    /**
     * Test method for {@link com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateFactory#getDefaultSSLCertificateCreator()}.
     */
    @Test
    public void setDefaultSSLCertificateCreator() {
        KeytoolSSLCertificateCreator creator = new KeytoolSSLCertificateCreator();
        DefaultSSLCertificateFactory.setDefaultSSLCertificateCreator(creator);
        assertSame("Was not the expected KeytoolSSLCertificateCreator instance",
                   creator, DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator());
    }

}
