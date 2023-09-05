/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;

import org.junit.Test;

import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;

import test.UTLocationHelper;

/**
 * Ensure LTPAToken2 remains serializable. This is necessary because
 * SingleSignonTokenImpl must remain serializable and SSOTokenImpl extends
 * AbstractTokenImpl which includes Token which is an LTPAToken2.
 * If LTPAToken2 is changed, create a new LTPAToken2_x.ser file and write a new test.
 * Each new version should be able to deserialze every previous version.
 * This is necessary to maintain version to version compatibility for the
 * distributed authentication cache.
 */
public class LTPAToken2SerializationTest {

    private static final String KEYIMPORTFILE = "${server.config.dir}/resources/security/security.token.ltpa.keys.correct.txt";
    private static final byte[] KEYPASSWORD = "WebAS".getBytes();
    private static final String decodedSharedKey = "Three can keep a secret when two are no longer there";
    private static final String encodedSharedKey = Base64Coder.base64Encode(decodedSharedKey);
    private static LTPAPrivateKey ltpaPrivateKey;
    private static LTPAPublicKey ltpaPublicKey;
    private static byte[] sharedKey;
    private final static byte[] test = new byte[] { -96, 120, 80, -94, 103, 123, -41, -105, -76, -103, 43, 91, -114, 7, 21, 1, 64, -77, -2, -44, 20, 126, -88, 46, 104, -56, -119,
                                                    -80,
                                                    -120, 127, 116, 32, 57, 82, -66, 38, -44, -30, -8, 66, -14, 47, 116, 49, 4, 69, -4, 7, -21, -28, -63, -56, 45, -74, -76, -49,
                                                    91, 93,
                                                    -88, -26, -80, -99, 61, 119, -12, 106, -109, 23, -60, -107, -49, 92, 15, 83, -63, -107, -118, -116, -110, 118, -97, -126, 84,
                                                    -37, -10,
                                                    34, -21, -27, -18, -26, -10, 92, -113, -27, 109, 102, 123, -41, 14, 3, -10, -123, -73, -85, 22, 46, -108, 0, -11, -23, 57, 94,
                                                    35, 107,
                                                    28, 114, 60, 36, -124, -112, -126, -66, -49, 98, 47, -70, 14, -37, -55, -98, -116, -113, 115, -1, -124, 91, 37, 31, -112, 117,
                                                    -87,
                                                    -23, -105, -87, -34, 18, 42, -44, 62, -18, 65, 54, -4, 119, -119, -55, 94, -54, 94, 104, -40, -123, 87, -94, 8, -65, -40, -38,
                                                    -7, -17,
                                                    -48, -61, 91, 26, 59, -111, -70, -66, -79, -82, 20, 27, -82, -50, 20, 118, -38, 27, -55, 94, -3, -29, -18, -1, 21, 86, 81, 67,
                                                    -104,
                                                    -6, 21, 72, 103, -114, -96, 68, 15, -40, 77, -115, 34, -34, -95, 16, -76, 17, -117, -104, -55, 114, 29, -44, 110, -20, -53, -47,
                                                    58,
                                                    85, 80, -39, 123, -80, 2, 117, 66, -60, 51, 88, 103, 87, };

    /**
     * Test to deserialize LTPAToken2_1.ser.
     * Validate token bytes and attributes.
     */
    @Test
    public void deserializeLTPAToken2_1() throws Exception {
        final String filename = "test-resources/test data/ser-files/LTPAToken2_1.ser";

        /*
         * Deserialize the token instance from the file.
         */
        LTPAToken2 object = null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            object = (LTPAToken2) in.readObject();
        }
        assertNotNull("SAML token should not be null.", object);

        /*
         * Check attribute names and values
         */
        assertEquals("The attribute names should be u|expire|", "u|expire|", getEnumerationString(object.getAttributeNames()));
        assertEquals("The u attribute should be user:BasicRealm/user1", "user:BasicRealm/user1", object.getAttributes("u")[0]);
        assertEquals("The expire attribute should be 1658180685586", "1658180685586", object.getAttributes("expire")[0]);

        /*
         * Check expiration
         */
        assertEquals("The expiration should be 4666196170449L", 4666196170449L, object.getExpiration());

        /*
         * Check version
         */
        assertEquals("The version should be 1", 1, object.getVersion());

        /*
         * Check token. The userData, privateKey, sharedKey, and cipher are used to return the byte array.
         */
        assertEquals("The token byte string should be: " + getByteString(test), getByteString(test), getByteString(object.getBytes()));

        /*
         * Validate the token. Validation uses signature and publicKey.
         */
        assertEquals("The token should be valid.", true, object.isValid());

    }

    /**
     * @param arr the byte array to convert
     * @return A string representation of the byte array
     */
    private String getByteString(byte[] arr) {
        String s = "";
        for (byte b : arr) {
            s += b + " ";
        }
        return s;
    }

    /**
     * @param arr the byte array to convert
     * @return A string representation of the byte array
     */
    private String getEnumerationString(Enumeration<String> e) {
        if (e == null) {
            return null;
        }
        String s = "";
        while (e.hasMoreElements()) {
            String param = e.nextElement();
            s += param + "|";
        }
        return s;
    }

    /**
     * Method used to create and serialize the LTPAToken2 for testing.
     *
     * If LTPAToken2 changes, previously serialized versions of
     * LTPAToken2 must remain deserializable.
     * Use this method to create a new LTPAToken2_x.ser file,
     * replacing the x with the current version + 1.
     * Then write a test that deserializes that version and all
     * previous LTPAToken2_x.ser files.
     */
    public static void main(String[] args) throws Exception {
        final String filename = "test-resources/test data/ser-files/LTPAToken2_x.ser";

        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          KEYIMPORTFILE,
                                          KEYPASSWORD, null);
        ltpaPrivateKey = new LTPAPrivateKey(keyInfoManager.getPrivateKey(KEYIMPORTFILE));
        ltpaPublicKey = new LTPAPublicKey(keyInfoManager.getPublicKey(KEYIMPORTFILE));
        sharedKey = encodedSharedKey.getBytes();

        /*
         * Create LTPAToken2
         */
        LTPAToken2 object = null;
        object = new LTPAToken2(test, sharedKey, ltpaPrivateKey, ltpaPublicKey, 1L);

        /*
         * Set expiration via reflection so we don't need to update this test constantly for the token verification to pass. If the test fails validation, there may be something
         * wrong with the expiration
         */
        Field byteField = object.getClass().getDeclaredField("expirationInMilliseconds");
        byteField.setAccessible(true);

        /*
         * Set expiration time to Fri Nov 12 15:36:10 CST 2117
         */
        long time = System.currentTimeMillis() + 3000000000000L;
        byteField.set(object, time);

        /*
         * Serialize the object instance to a file.
         */
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename))) {
            output.writeObject(object);
        }
    }
}
