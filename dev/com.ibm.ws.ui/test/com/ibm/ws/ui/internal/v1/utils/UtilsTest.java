/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.MalformedURLException;

import org.junit.Test;

/**
 * 
 */
public class UtilsTest {

    @Test
    public void urlEncoded() {
        assertEquals("Drives Utils.urlEncoded for happy path coverage... this test is here because we care about coverage and no other real reason",
                     "encodeMe", Utils.urlEncode("encodeMe"));
    }

    @Test
    public void urlEncoded_null() {
        assertNull("Drives Utils.urlEncoded for happy path coverage...",
                   Utils.urlEncode(null));
    }

    @Test
    public void getURL() throws Exception {
        assertNotNull("Drives Utils.getURL for happy path coverage... this test is here because we care about coverage and no other real reason",
                      Utils.getURL("http://www.ibm.com"));
    }

    @Test(expected = MalformedURLException.class)
    public void getURL_badURL() throws Exception {
        Utils.getURL("tp:/.ibm.com");
    }

   /**
    * The algorithm assessment of FIPS 140-3 by updating SHA512 checksum is based on slack discussion with component SMEs. 
    */
    @Test
    public void sha512Test() {
        assertEquals("SHA512 encoding not working", "298bd4e66fec7b0c116bb0ab9858e450b176c07f8b151fdfa083bf775cc258668e93fc8e2ae412939a35062aa712ee484b307690d043ee59d489d27ca0dcddc9".toLowerCase(), Utils.getSHA512String("Test SHA512 String"));
    }
}
