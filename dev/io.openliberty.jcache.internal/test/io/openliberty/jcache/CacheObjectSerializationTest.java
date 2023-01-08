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
package io.openliberty.jcache;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

import org.junit.Test;

/**
 * Ensure CacheObject and all serialized instances of CacheObject remain serializable.
 * If CacheObject is changed, create a new CacheObject_x.ser file and write a new test.
 * Each new version should be able to deserialze every previous version.
 * This is necessary to maintain version to version compatibility for the distributed authentication cache.
 */
public class CacheObjectSerializationTest {
    private final static byte[] bytes_cacheobject_1 = new byte[] { -96, 120, 80, -94, 103, 123 };

    /**
     * Test to deserialize CacheObject_1.ser.
     * Validate serialVersion and byte string.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void deserializeCacheObject_1() throws Exception {
        String filename = "test-resources/ser-files/CacheObject_1.ser";
        FileInputStream file = new FileInputStream(filename);
        ObjectInputStream in = new ObjectInputStream(file);

        CacheObject object = (CacheObject) in.readObject();
        in.close();

        //Check serializationVersion via reflection
        Field field = CacheObject.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        Object value = field.get(object);
        assertEquals("The version should be 1", (long) 1, value);

        //Check bytes
        assertEquals("The CacheObject ObjectBytes should be: " + getByteString(bytes_cacheobject_1), getByteString(bytes_cacheobject_1), getByteString(object.getObjectBytes()));
    }

    /**
     * Method used to create and serialize the CacheObject for testing.
     *
     * If CacheObject changes, previously serialized versions of
     * CacheObject must remain deserializable.
     * Use this method to create a new CacheObject_x.ser file,
     * replacing the x with the current version + 1.
     * Then write a test that deserializes that version and all
     * previous CacheObject_x.ser files.
     */
    //@Test
    public static void main(String[] args) throws Exception {
        //Create CacheObject
        CacheObject object = new CacheObject(null, bytes_cacheobject_1);

        String filename = "test-resources/ser-files/CacheObject_x.ser";

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

    /**
     * Get the bytes of an array as a string.
     *
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
}