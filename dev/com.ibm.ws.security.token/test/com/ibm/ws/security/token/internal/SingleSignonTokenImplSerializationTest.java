/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        String filename = "test-resources/ser-files/SingleSignonTokenImpl_1.ser";
        FileInputStream file = new FileInputStream(filename);
        ObjectInputStream in = new ObjectInputStream(file);

        SingleSignonTokenImpl object = (SingleSignonTokenImpl) in.readObject();
        in.close();

        //Check version and name
        assertEquals("The version number should be: 2.", 2, object.getVersion());
        assertEquals("The name should be: LtpaToken", "LtpaToken", object.getName());

        //Check token and token expiration
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
        //Create TokenService
        TokenService ts = new TestTokenServiceImpl();

        //Create SingleSignonTokenImpl
        SingleSignonTokenImpl object = new SingleSignonTokenImpl(ts, "tokenType_1");
        object.initializeToken(new byte[] {});

        String filename = "test-resources/ser-files/SingleSignonTokenImpl_x.ser";

        // Serialization
        //Saving of object in a file
        FileOutputStream file = new FileOutputStream(filename);
        ObjectOutputStream output = new ObjectOutputStream(file);

        // Method for serialization of object
        output.writeObject(object);

        output.close();
        file.close();

        System.out.println("Object has been serialized");

    }
}
