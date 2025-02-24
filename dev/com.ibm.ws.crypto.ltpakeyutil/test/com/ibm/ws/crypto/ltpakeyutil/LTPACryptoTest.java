/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
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
package com.ibm.ws.crypto.ltpakeyutil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LTPACryptoTest {

	// These 'magic numbers' come from real LTPA key bytes that we use for the purpose of the unit test
	private static final byte[] PRIVATE_KEY = new byte[] { 0, 0, 0, -128, 121, -119, 123, 19, 68, -85, 69, -10, -81, -12, 9, 106, -89, 115, 78, -7, -96, 29, -56, -7, 83, 43, 102, 57, -89, 57, 33, 15,
                                      47, -93, -59, 31, -18, 108, -39, 105, 15, 95, -40, -102, 71, 50, -54, -26, 10, -90, -4, 56, -32, 86, -1, -124, -122, 78, -93, 112, -78, 37,
                                      81,
                                      -108, -104, -120, -106, 39, 49, -84, -114, -43, 1, 112, -53, -61, -41, 55, 65, 120, 13, 33, -118, -32, -99, 46, -17, 24, -13, -57, -77, -62,
                                      89, 42, -38, -98, -64, -10, -90, -3, 64, 115, -72, -47, -117, 102, -57, -60, -34, 118, -46, -101, 101, 41, -55, 83, 98, 2, 79, 88, 6, 49, -58,
                                      -108, -109, 110, 44, -31, 82, 24, 73, 49, 1, 0, 1, 0, -11, 41, 36, -30, 21, 79, -10, -128, -107, 5, -127, 23, -2, -122, 14, 38, -39, -40, -94,
                                      30, 65, -112, 48, 29, -71, 95, 0, 2, 32, 39, -126, -73, 121, 67, -18, 69, -82, 121, 126, 83, -50, -109, -73, 86, 4, 60, -122, -15, 26, 10,
                                      119,
                                      -53, -106, -68, 115, 21, -100, -78, -37, -82, -73, -39, -7, -115, 0, -37, -42, 110, 46, 81, 94, 62, -44, -112, 72, 102, -82, 2, 98, 64, -16,
                                      99, -84, 55, 27, -91, -16, 66, 27, -92, -85, 96, -39, -51, 110, -77, 114, 119, -107, -48, 83, -16, -23, -86, 98, 43, 63, 79, -52, -65, -91,
                                      -101, -93, -5, -56, 124, 7, 7, -34, 1, -34, 108, 8, 60, -38, -60, 102, -75, 123 };

	private static final byte[] PUBLIC_KEY = new byte[] { 0, -46, -121, -116, 118, 70, 99, 68, 114, -38, 49, -70, -111, 96, 14, 57, -47, -123, -81, -62, 3, -124, -3, -30, -127, 112, 0, -76, -93, 81,
                                     -114, 28, -11, 74, -31, -36, -128, -70, -76, 100, 95, -90, 3, -99, 41, 125, 16, 69, -72, 85, -122, -59, -81, 22, 16, -5, 97, 124, -52, -79, -1,
                                     17, 39, 112, 115, 28, -95, -87, 6, 92, -111, -11, -62, -126, -53, -36, 26, 126, 45, -121, 127, 30, 86, 24, 56, 121, -53, -74, 103, 22, 97, 3,
                                     -113, -79, -22, -89, -21, 127, 39, -111, 21, -107, -91, 91, 26, -56, -123, -79, -10, 58, 39, -116, 82, 113, -6, -10, -20, -117, -106, -95, -34,
                                     -6, 37, -58, -22, -43, 89, -105, -65, 1, 0, 1 };

	private static final byte[] SECRET_KEY = new byte[] { 85, -21, 95, 39, 70, -51, -43, 15, -25, 8, 18, -94, -108, 32, -57, -25, -29, 37, 116, -8, -5, -83, -13, 83 };

    private static final String AES_CBC_CIPHER = "AES/CBC/PKCS5Padding";
    private static final String AES_ECB_CIPHER = "AES/ECB/PKCS5Padding";
    private static final String DES_CBC_CIPHER = "DESede/CBC/PKCS5Padding";
    private static final String DES_ECB_CIPHER = "DESede/ECB/PKCS5Padding";

    private static final byte[] ORIGINAL_DATA;

    static {
        byte[] b;
        try {
            b = "It's a original data.".getBytes("utf-8");
        } catch (Exception e) {
            b = null;
        }
        ORIGINAL_DATA = b;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LTPACrypto.setMaxCache(5);
    }

    @Test
    public void testSignCorrect() throws Exception {
        // Sign the data.
        LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        byte[][] rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        byte[] signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        // Verify the signature.
        LTPAPublicKey publicKey = new LTPAPublicKey(PUBLIC_KEY);
        rawKey = publicKey.getRawKey();
        boolean verified = LTPACrypto.verifyISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result.
        Assert.assertTrue(verified);

        // Sign the data again.
        privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        // Verify the signature again.
        publicKey = new LTPAPublicKey(PUBLIC_KEY);
        rawKey = publicKey.getRawKey();
        verified = LTPACrypto.verifyISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result again.
        Assert.assertTrue(verified);
    }

    @Test
    public void testSignCorrectWithCacheOverFlow() throws Exception {
        // Sign the data.
        LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        byte[][] rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        byte[] signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        // Verify the signature.
        LTPAPublicKey publicKey = new LTPAPublicKey(PUBLIC_KEY);
        rawKey = publicKey.getRawKey();
        boolean verified = LTPACrypto.verifyISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result.
        Assert.assertTrue(verified);

        // Sign the data again.
        privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        // Verify the signature again.
        publicKey = new LTPAPublicKey(PUBLIC_KEY);
        rawKey = publicKey.getRawKey();
        verified = LTPACrypto.verifyISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result again.
        Assert.assertTrue(verified);
    }

    @Test
    public void testEncryptCorrectWithAESCBC() throws Exception {
        // Encrypt the data.
        byte[] secretKey = SECRET_KEY;
        byte[] encrypted = LTPACrypto.encrypt(ORIGINAL_DATA, secretKey, AES_CBC_CIPHER);

        // Decrypt the data.
        byte[] decrypted = LTPACrypto.decrypt(encrypted, secretKey, AES_CBC_CIPHER);

        // Check the length of the bytes.
        Assert.assertTrue(ORIGINAL_DATA.length == decrypted.length);

        // Check each byte.
        boolean same = true;
        for (int i = 0, il = ORIGINAL_DATA.length; i < il; i++) {
            if (ORIGINAL_DATA[i] != decrypted[i]) {
                same = false;
                break;
            }
        }
        Assert.assertTrue(same);
    }

    @Test
    public void testEncryptCorrectWithAESECB() throws Exception {
        // Encrypt the data.
        byte[] secretKey = SECRET_KEY;
        byte[] encrypted = LTPACrypto.encrypt(ORIGINAL_DATA, secretKey, AES_ECB_CIPHER);

        // Decrypt the data.
        byte[] decrypted = LTPACrypto.decrypt(encrypted, secretKey, AES_ECB_CIPHER);

        // Check the length of the bytes.
        Assert.assertTrue(ORIGINAL_DATA.length == decrypted.length);

        // Check each byte.
        boolean same = true;
        for (int i = 0, il = ORIGINAL_DATA.length; i < il; i++) {
            if (ORIGINAL_DATA[i] != decrypted[i]) {
                same = false;
                break;
            }
        }
        Assert.assertTrue(same);
    }

    @Test
    public void testEncryptCorrectWithDESECB() throws Exception {
        // Encrypt the data.
        byte[] secretKey = SECRET_KEY;
        byte[] encrypted = LTPACrypto.encrypt(ORIGINAL_DATA, secretKey, DES_ECB_CIPHER);

        // Decrypt the data.
        byte[] decrypted = LTPACrypto.decrypt(encrypted, secretKey, DES_ECB_CIPHER);

        // Check the length of the bytes.
        Assert.assertTrue(ORIGINAL_DATA.length == decrypted.length);

        // Check each byte.
        boolean same = true;
        for (int i = 0, il = ORIGINAL_DATA.length; i < il; i++) {
            if (ORIGINAL_DATA[i] != decrypted[i]) {
                same = false;
                break;
            }
        }
        Assert.assertTrue(same);
    }

    @Test
    public void testEncryptCorrectWithDESCBC() throws Exception {
        // Encrypt the data.
        byte[] encrypted = LTPACrypto.encrypt(ORIGINAL_DATA, SECRET_KEY, DES_CBC_CIPHER);

        // Decrypt the data.
        byte[] decrypted = LTPACrypto.decrypt(encrypted, SECRET_KEY, DES_CBC_CIPHER);

        // Check the length of the bytes.
        Assert.assertTrue(ORIGINAL_DATA.length == decrypted.length);

        // Check each byte.
        boolean same = true;
        for (int i = 0, il = ORIGINAL_DATA.length; i < il; i++) {
            if (ORIGINAL_DATA[i] != decrypted[i]) {
                same = false;
                break;
            }
        }
        Assert.assertTrue(same);
    }

}
