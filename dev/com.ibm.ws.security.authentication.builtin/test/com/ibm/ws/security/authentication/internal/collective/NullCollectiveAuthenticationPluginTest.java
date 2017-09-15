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
package com.ibm.ws.security.authentication.internal.collective;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.ibm.ws.security.authentication.AuthenticationException;

/**
 *
 */
public class NullCollectiveAuthenticationPluginTest {
    private final NullCollectiveAuthenticationPlugin nullPlugin = new NullCollectiveAuthenticationPlugin();

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.internal.collective.NullCollectiveAuthenticationPlugin#isCollectiveCertificateChain(java.security.cert.X509Certificate[])}.
     */
    @Test
    public void isCollectiveCertificateChain() {
        assertFalse("Null plugin should always return false",
                    nullPlugin.isCollectiveCertificateChain(null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.internal.collective.NullCollectiveAuthenticationPlugin#authenticateCertificateChain(java.security.cert.X509Certificate[], boolean)}
     * .
     */
    @Test(expected = AuthenticationException.class)
    public void authenticateCertificateChain() throws Exception {
        nullPlugin.authenticateCertificateChain(null, true);
    }

}
