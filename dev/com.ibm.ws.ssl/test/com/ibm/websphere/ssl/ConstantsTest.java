/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.ssl;

import static org.junit.Assert.assertEquals;
import org.junit.Assume;
import org.junit.Before;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import org.junit.Test;

public class ConstantsTest {

    @Test
    public void testRemoveModifier() {
        String[] ciphers = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384" };
        String modifier = "-TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384";
    assertEquals("Returned list should be empty", 0, Constants.adjustSupportedCiphers(ciphers, modifier).length);
    }

    @Test
    public void testAddModifier() {
        String[] ciphers = new String[0];
        String modifier = "+TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384";
    String[] adjustedCiphers = Constants.adjustSupportedCiphers(ciphers, modifier);
        assertEquals("Returned list should be length 1", 1, adjustedCiphers.length);
        assertEquals("Only cipher suite should be TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", adjustedCiphers[0]);
    }

    @Test
    public void testAddAndRemoveModifiers() {
        String[] ciphers = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384" };
        String modifier = "-TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 +TLS_AES_256_GCM_SHA384";
    String[] adjustedCiphers = Constants.adjustSupportedCiphers(ciphers, modifier);
        assertEquals("Returned list should be length 1", 1, adjustedCiphers.length);
        assertEquals("Only cipher suite should be TLS_AES_256_GCM_SHA384", "TLS_AES_256_GCM_SHA384", adjustedCiphers[0]);
    }

    @Test
    public void testRemoveCipherSuiteWithWildcard() {
        String[] ciphers = new String[] {
            "SSL_RSA_WITH_AES_128_CBC_SHA",
            "SSL_RSA_WITH_AES_256_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA"
        };
        String modifier = "-SSL_RSA*";
    String[] adjustedCiphers = Constants.adjustSupportedCiphers(ciphers, modifier);
        assertEquals("Should have 2 ciphers remaining", 2, adjustedCiphers.length);
        assertEquals("First cipher should be TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", adjustedCiphers[0]);
        assertEquals("Second cipher should be TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", adjustedCiphers[1]);
    }

    @Test
    public void testAddAndRemoveWithWildcard() {
        String[] ciphers = new String[] {
            "SSL_RSA_WITH_AES_128_CBC_SHA",
            "SSL_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        };
        String modifier = "-SSL_RSA* +TLS_AES_256_GCM_SHA384";
    String[] adjustedCiphers = Constants.adjustSupportedCiphers(ciphers, modifier);
        assertEquals("Should have 2 ciphers", 2, adjustedCiphers.length);
        }

    @Test
    public void testMultipleWildcardPatterns() {
        String[] ciphers = new String[] {
            "SSL_RSA_WITH_AES_128_CBC_SHA",
            "SSL_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        };
        // Pattern -*_DSS_* has wildcard not at end (first * at position 0), so it's invalid and ignored
        // Only -SSL_RSA* is applied
        String modifier = "-SSL_RSA* -*_DSS_*";
    String[] adjustedCiphers = Constants.adjustSupportedCiphers(ciphers, modifier);
        assertEquals("Should have 3 ciphers remaining (invalid pattern -*_DSS_* ignored)", 3, adjustedCiphers.length);
    }

    @Test
    public void testWildcardMatchesNothing() {
        String[] ciphers = new String[] {
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        };
        String modifier = "-SSL_RSA*";
    String[] adjustedCiphers = Constants.adjustSupportedCiphers(ciphers, modifier);
        assertEquals("Should have all 2 ciphers remaining", 2, adjustedCiphers.length);
        }


}
