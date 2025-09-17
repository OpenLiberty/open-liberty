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
import java.util.Arrays;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.fail;

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
private static final byte[] PRIVATE_KEY2 = new byte[] {0, 0, 0, -128, 66, 42, 83, -24, 66, 126, 94, -97, 5, 5, 78, -55, -29, 90, 77, -112, 14, 79, -92, 90, 126, 43, 7, -75, -123, 63, 20, 33, -30, 107, 85, -21, -115, -15, -86, -82, 8, -15, 52, -101, 22, -9, -112, -73, 101, 16, -114, 18, 91, -77, -97, 82, 104, 49, -30, -66, 54, 22, -113, 30, -115, 116, -127, -106, 30, -54, -24, 2, -6, -84, 52, 72, 46, -54, -32, -84, 0, -81, -121, 73, -79, -81, 27, 10, -32, 9, 44, 43, -89, -112, 66, -53, -109, 44, 60, -102, -60, -30, -19, 110, 28, 108, 98, -54, 43, 27, 20, -33, 1, -114, 70, -77, -27, 48, 122, -12, 118, 108, -85, 104, 80, -47, 126, 22, -55, 15, 119, -127, 1, 0, 1, 0, -20, 67, 101, 119, -29, 76, -123, 1, -101, 31, 71, 64, -125, 98, -80, 101, -73, -30, -12, 110, -28, -78, 114, -119, 36, 101, 121, -69, -29, -41, 33, -3, 23, -90, -33, -108, 85, 75, -54, 1, 35, 35, -11, 51, -98, -38, 45, -5, 79, -106, 53, 12, -36, 62, -29, -89, 8, 79, -34, -11, -56, -80, 59, -59, 0, -76, 125, 118, 51, 71, 83, -36, 112, -67, -110, 102, 24, -49, -42, 27, -83, -35, -59, 100, -18, 29, -11, -22, 113, -31, 122, -108, -94, -59, -50, -104, -73, -91, -8, 13, 101, -9, -116, 8, 55, -2, -108, 60, -61, 124, 81, 25, 84, -22, -25, -86, -49, 104, -37, -88, -17, -61, -25, 71, 71, -57, -92, 107, -101};

//Raw key not generated from a valid ltpa private key
private static final byte[][] BAD_RAWKEY = new byte[][] { 
    {(byte) 0x1A, (byte) 0x2B, (byte) 0x3C, (byte) 0x4D},
    {(byte) 0x5E, (byte) 0x6F, (byte) 0x7A, (byte) 0x8B},
    {(byte) 0x9C, (byte) 0xAD, (byte) 0xBE, (byte) 0xCF}
};

private static final byte[] PUBLIC_KEY = new byte[] { 0, -46, -121, -116, 118, 70, 99, 68, 114, -38, 49, -70, -111, 96, 14, 57, -47, -123, -81, -62, 3, -124, -3, -30, -127, 112, 0, -76, -93, 81,
                                     -114, 28, -11, 74, -31, -36, -128, -70, -76, 100, 95, -90, 3, -99, 41, 125, 16, 69, -72, 85, -122, -59, -81, 22, 16, -5, 97, 124, -52, -79, -1,
                                     17, 39, 112, 115, 28, -95, -87, 6, 92, -111, -11, -62, -126, -53, -36, 26, 126, 45, -121, 127, 30, 86, 24, 56, 121, -53, -74, 103, 22, 97, 3,
                                     -113, -79, -22, -89, -21, 127, 39, -111, 21, -107, -91, 91, 26, -56, -123, -79, -10, 58, 39, -116, 82, 113, -6, -10, -20, -117, -106, -95, -34,
                                     -6, 37, -58, -22, -43, 89, -105, -65, 1, 0, 1 };


    private static final byte[] PUBLIC_KEY2 = new byte[] {0, -39, 7, -105, -87, 11, 13, 30, 127, -51, -53, 66, -31, -45, -37, 46, 49, -9, -99, 117, 52, -23, -99, 31, 69, -12, -59, 111, -33, -35, 124, 87, -49, -121, -40, 53, 39, 100, 67, 18, 33, -57, -122, 64, 51, -78, 36, 122, 2, 121, 93, -67, -128, -111, -65, 21, -30, -8, -100, -67, -13, -59, 72, 119, 40, -37, -2, -10, 37, 22, 104, 13, 108, 122, 124, -93, -6, 8, -70, -74, -7, 41, -117, 44, 52, 9, 11, -104, 98, -88, 67, 123, 5, -81, -76, 20, -35, -75, -114, -73, -45, -94, -28, -53, -51, 122, -125, -82, -61, -23, 15, 64, 8, 63, 42, 115, -31, -60, 101, -9, -70, 2, -27, -106, -61, -42, 22, -76, -11, 1, 0, 1};

    private static final byte[] SECRET_KEY = new byte[] { 85, -21, 95, 39, 70, -51, -43, 15, -25, 8, 18, -94, -108, 32, -57, -25, -29, 37, 116, -8, -5, -83, -13, 83 };
    private static final String AES_CBC_CIPHER = "AES/CBC/PKCS5Padding";
    private static final String AES_ECB_CIPHER = "AES/ECB/PKCS5Padding";
    private static final String DES_CBC_CIPHER = "DESede/CBC/PKCS5Padding";
    private static final String DES_ECB_CIPHER = "DESede/ECB/PKCS5Padding";

    private static final byte[] ORIGINAL_DATA;
    private static int MAX_CACHE = 10;
   

    //function to create new data for signing and verifying
    private byte[] getOriginalData(int i) throws Exception {
        return ("It's a original data." + Integer.toString(i)).getBytes("utf-8");
    }

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
        LTPACrypto.setMaxCache(MAX_CACHE);

    }

    @Before
    public void setUpBefore() throws Exception {
        LTPACrypto.emptyVerifyCache();
        LTPACrypto.emptySignCache();

    }
    
    @Test
    public void testSignCorrect() throws Exception {
        // Sign the data.
        LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        byte[][] rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        byte[] signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        //Cache increments by 1 after signing
        Assert.assertEquals(1, LTPACrypto.getSignCacheSize());

        // Verify the signature.
        LTPAPublicKey publicKey = new LTPAPublicKey(PUBLIC_KEY);
        rawKey = publicKey.getRawKey();
        boolean verified = LTPACrypto.verifyISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result.
        Assert.assertTrue(verified);

        //Cache increments by 1 after signing
        Assert.assertEquals(1, LTPACrypto.getVerifyCacheSize());

        // Sign the data again.
        privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        //Cache size stays the same as the key values and data are unchanged
        Assert.assertEquals(1, LTPACrypto.getSignCacheSize());

        // Verify the signature again.
        publicKey = new LTPAPublicKey(PUBLIC_KEY);
        rawKey = publicKey.getRawKey();
        verified = LTPACrypto.verifyISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result again.
        Assert.assertTrue(verified);

        //Cache size stays the same as the key values and data are unchanged
        Assert.assertEquals(1, LTPACrypto.getVerifyCacheSize());
    }

    @Test
    public void testSignCorrectWithDifferentKeys() throws Exception {
        // Sign the data.
        LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        byte[][] rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        byte[] signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        //Cache increments by 1 after signing
        Assert.assertEquals(1, LTPACrypto.getSignCacheSize());

        // Sign the data again with a different key.
        LTPAPrivateKey privateKey2 = new LTPAPrivateKey(PRIVATE_KEY2);
        byte[][] rawKey2 = privateKey2.getRawKey();
        LTPACrypto.setRSAKey(rawKey2);
        byte[] signature2 = LTPACrypto.signISO9796(rawKey2, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        //Cache increments by 1 after signing again
        Assert.assertEquals(2, LTPACrypto.getSignCacheSize());


        // Verify the signature.
        LTPAPublicKey publicKey = new LTPAPublicKey(PUBLIC_KEY);
        rawKey = publicKey.getRawKey();
        boolean verified = LTPACrypto.verifyISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result.
        Assert.assertTrue(verified);

        //Cache increments by 1 after signing
        Assert.assertEquals(1, LTPACrypto.getVerifyCacheSize());

        
        // Verify the signature again with a different key
        LTPAPublicKey publicKey2 = new LTPAPublicKey(PUBLIC_KEY2);
        rawKey2 = publicKey2.getRawKey();
        boolean verified2 = LTPACrypto.verifyISO9796(rawKey2, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature2, 0, signature2.length);

        // Check the result again.
        Assert.assertFalse(verified2);

        //Cache size increments by 1
        Assert.assertEquals(2, LTPACrypto.getVerifyCacheSize());
    }

    @Test
    public void testSignCorrectWithCacheOverFlow() throws Exception {
		for (int i = 1; i <= MAX_CACHE + 1; i++) {
			// Sign the data.
			LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
			byte[][] rawKey = privateKey.getRawKey();
			LTPACrypto.setRSAKey(rawKey);
			byte[] signature = LTPACrypto.signISO9796(rawKey, getOriginalData(i), 0, getOriginalData(i).length);

			if (i == MAX_CACHE + 1) {
				Assert.assertEquals((int) Math.floor(MAX_CACHE * 0.8) + 1, LTPACrypto.getSignCacheSize());
			} else {
				Assert.assertEquals(i, LTPACrypto.getSignCacheSize());
			}

			// Verify the signature.
			LTPAPublicKey publicKey = new LTPAPublicKey(PUBLIC_KEY);
			rawKey = publicKey.getRawKey();
			boolean verified = LTPACrypto.verifyISO9796(rawKey, getOriginalData(i), 0, getOriginalData(i).length,
					signature, 0, signature.length);

			Assert.assertTrue(verified);

			if (i == MAX_CACHE + 1) {
				Assert.assertEquals((int) Math.floor(MAX_CACHE * 0.8) + 1, LTPACrypto.getVerifyCacheSize());
			} else {
				Assert.assertEquals(i, LTPACrypto.getVerifyCacheSize());
			}
		}
	}

    @Test
    public void testBadSignature_IncorrectRawKey() throws Exception {
		        // Sign the data.
                LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
                byte[][] rawKey = privateKey.getRawKey();
                LTPACrypto.setRSAKey(rawKey);
                byte[] signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);
        
                //Cache increments by 1 after signing
                Assert.assertEquals(1, LTPACrypto.getSignCacheSize());
        
                //Sign the data again with a bad raw key.
                try{
                LTPACrypto.setRSAKey(BAD_RAWKEY);
                signature = LTPACrypto.signISO9796(BAD_RAWKEY, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);
                fail("Expected an exception but got [" + Arrays.toString(signature) + "].");

                }catch(ArrayIndexOutOfBoundsException a){
                    //Cache stays the same as we can not use an invalid raw key
                Assert.assertEquals(1, LTPACrypto.getSignCacheSize());
                
                }    
        
	}

    @Test
    public void testBadSignature_NullData() throws Exception {
		        // Sign the data.
                LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
                byte[][] rawKey = privateKey.getRawKey();
                LTPACrypto.setRSAKey(rawKey);
                byte[] signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);
        
                //Cache increments by 1 after signing
                Assert.assertEquals(1, LTPACrypto.getSignCacheSize());
        
                //Sign the data again
                try{
                    LTPACrypto.setRSAKey(rawKey);
                    
                    signature = LTPACrypto.signISO9796(rawKey, null, 0, ORIGINAL_DATA.length);
                    fail("Expected an exception but got [" + Arrays.toString(signature) + "].");
      
                    }catch(IllegalArgumentException i){
                        //Cache stays the same as we can not use null data
                    Assert.assertEquals(1, LTPACrypto.getSignCacheSize());
                    }        
        
	}

    @Test
    public void testBadVerify() throws Exception {

        // Sign the data.
        LTPAPrivateKey privateKey = new LTPAPrivateKey(PRIVATE_KEY);
        byte[][] rawKey = privateKey.getRawKey();
        LTPACrypto.setRSAKey(rawKey);
        byte[] signature = LTPACrypto.signISO9796(rawKey, ORIGINAL_DATA, 0, ORIGINAL_DATA.length);

        //Cache increments by 1 after signing
        Assert.assertEquals(1, LTPACrypto.getSignCacheSize());



        // Verify the signature.
        LTPAPublicKey publicKey = new LTPAPublicKey(PUBLIC_KEY);
        byte[][] rawKey2 = publicKey.getRawKey();
        boolean verified = LTPACrypto.verifyISO9796(rawKey2, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result.
        Assert.assertTrue(verified);

        //Cache increments by 1 after signing
        Assert.assertEquals(1, LTPACrypto.getVerifyCacheSize());

        
        // Verify the signature again with a different key
        LTPAPublicKey publicKey2 = new LTPAPublicKey(PUBLIC_KEY2);
        rawKey2 = publicKey2.getRawKey();
        boolean verified2 = LTPACrypto.verifyISO9796(rawKey2, ORIGINAL_DATA, 0, ORIGINAL_DATA.length, signature, 0, signature.length);

        // Check the result again. Will be false as incorrect public key was used
        Assert.assertFalse(verified2);

        //Cache size increments by 1
        Assert.assertEquals(2, LTPACrypto.getVerifyCacheSize());
        
        
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
