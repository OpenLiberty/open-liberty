/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.config.ssl;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLException;

/**
 *
 */
public class SSLConfigTest {

    private final static String[] CIPHERSUITES = { "SSL_RSA_WITH_AES_128_CBC_SHA", "SSL_DHE_RSA_WITH_AES_128_CBC_SHA", "SSL_DHE_DSS_WITH_AES_128_CBC_SHA" };

    @Test
    public void testSplit() throws Exception {
        test(",");
        test(" ");
        test(", ");
        test(" , ");
        test("\n");
        test("\t,\t");
    }

    private void test(String sep) throws SSLException {
        SSLConfig s = new SSLConfig(null);

        String[] cipherSuites = s.getCipherSuites(null, CIPHERSUITES, props(CIPHERSUITES, sep));
        assertTrue(Arrays.equals(CIPHERSUITES, cipherSuites));

    }

    /**
     * @param string
     * @return
     */
    private Properties props(String[] ciphersuites, String sep) {
        StringBuilder sb = new StringBuilder(ciphersuites[0]);
        for (int i = 1; i < ciphersuites.length; i++) {
            sb.append(sep).append(ciphersuites[i]);
        }
        Properties props = new Properties();
        props.put(Constants.SSLPROP_ENABLED_CIPHERS, sb.toString());
        return props;
    }

}
