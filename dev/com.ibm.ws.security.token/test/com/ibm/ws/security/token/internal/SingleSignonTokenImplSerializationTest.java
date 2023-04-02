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
package com.ibm.ws.security.token.internal;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import com.ibm.ws.security.token.TokenService;

/**
 * Ensure SingleSignonTokenImpl/AbstractTokenImpl and all serialized instances remain serializable.
 * If either TokenImpl is changed, create a new SingleSignonTokenImpl_x.ser file and write a new test.
 * SingleSignonTokenImpl extends AbstractTokenImpl so we are testing both with SSOTI. AbstractTokenImpl
 * includes a Token which is only implemented by LTPAToken2. The serialization test for LTPAToken2 is
 * separate from this one (in com.ibm.ws.security.token.ltpa).
 * Each new version should be able to deserialze every previous version.
 * This is necessary to maintain version to version compatibility for the distributed authentication cache.
 */
public class SingleSignonTokenImplSerializationTest {
    /**
     * Test to deserialize SingleSignonTokenImpl_1.ser.
     * Validate token, version, and name.
     */
    @Test
    public void deserializeSingleSignonTokenImpl_1() throws Exception {
        final String filename = "test-resources/ser-files/SingleSignonTokenImpl_1.ser";

        /*
         * Deserialize the token from the file.
         */
        SingleSignonTokenImpl object = null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            object = (SingleSignonTokenImpl) in.readObject();
        }

        /*
         * Check version and name
         */
        assertEquals("The version number should be: 2.", 2, object.getVersion());
        assertEquals("The name should be: LtpaToken", "LtpaToken", object.getName());

        /*
         * Check token and token expiration
         */
        assertEquals("The token string should be: tokenstring: 0.5967827720935945.", "tokenstring: 0.5967827720935945", object.getToken().toString());
        assertEquals("The token expiration should be: 123456789L.", 123456789L, object.getToken().getExpiration());

    }

    /**
     * Method used to create and serialize the SingleSignonTokenImpl for testing.
     *
     * If SingleSignonTokenImpl changes, previously serialized versions of
     * SingleSignonTokenImpl must remain deserializable.
     * Use this method to create a new SingleSignonTokenImpl_x.ser file,
     * replacing the x with the current version + 1.
     * Then write a test that deserializes that version and all
     * previous SingleSignonTokenImpl_x.ser files.
     */
    public static void main() throws Exception {
        final String filename = "test-resources/ser-files/SingleSignonTokenImpl_x.ser";

        /*
         * Create TokenService
         */
        TokenService ts = new TestTokenServiceImpl();

        /*
         * Create SingleSignonTokenImpl
         */
        SingleSignonTokenImpl object = new SingleSignonTokenImpl(ts, "tokenType_1");
        object.initializeToken(new byte[] {});

        /*
         * Serialize the object instance to a file.
         */
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename))) {
            output.writeObject(object);
        }
        System.out.println("Object has been serialized");
    }
}
