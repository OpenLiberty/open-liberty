/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 *
 */
public class NonceTest {
    /**
     * Test method for com.ibm.ws.security.oauth20.util.Nonce.isExpired()
     *
     */
    @Test
    public void isExpired() {
        int lifetimeMillis = 100;
        Nonce testNonce = Nonce.getInstance(lifetimeMillis);
        assertFalse("Nonce expired but should not have.", testNonce.isExpired());
        try {
            Thread.sleep(lifetimeMillis);
            for (int i = 0; i < 60; i++) {
                Thread.sleep(100);
                if (testNonce.isExpired())
                    break;
            }
            assertTrue("Nonce should be expired.", testNonce.isExpired());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail("sleep was interrupted");
        }
    }

    /**
     * Test method for com.ibm.ws.security.oauth20.util.Nonce.isValid()
     *
     */
    @Test
    public void isValid() {
        int lifetimeMillis = 100;
        Nonce testNonce = Nonce.getInstance(lifetimeMillis);
        assertTrue("Nonce is not valid", testNonce.isValid(testNonce.getValue()));
        assertTrue("Nonce should not be valid with bob value", !testNonce.isValid("bob"));
        try {
            Thread.sleep(lifetimeMillis);
            for (int i = 0; i < 60; i++) {
                Thread.sleep(100);
                if (testNonce.isValid(testNonce.getValue()))
                    break;
            }
            assertTrue("Nonce should not be valid", !testNonce.isValid(testNonce.getValue()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail("sleep was interrupted");
        }
    }
}
