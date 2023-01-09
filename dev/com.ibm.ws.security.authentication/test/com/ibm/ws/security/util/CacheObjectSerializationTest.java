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
package com.ibm.ws.security.util;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.junit.Test;

import com.ibm.ws.security.authentication.cache.CacheObject;
import com.ibm.ws.security.authentication.principals.WSPrincipal;

/**
 * Ensure CacheObject and all serialized instances of CacheObject remain serializable.
 * If CacheObject is changed, create a new CacheObject_x.ser file and write a new test.
 * Each new version should be able to deserialze every previous version.
 * This is necessary to maintain version to version compatibility for the distributed authentication cache.
 */
public class CacheObjectSerializationTest {

    /**
     * Test to deserialize CacheObject_1.ser.
     * Validate lookup keys, principals, private creds, and public creds.
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
        Field field = CacheObject.class.getDeclaredField("serializationVersion");
        field.setAccessible(true);
        Object value = field.get(object);
        assertEquals("The serialization version should be: 1.", (short) 1, value);

        //Check lookup keys
        List<Object> keys = object.getLookupKeys();
        assertEquals("There should be two lookup keys.", 2, keys.size());
        assertEquals("There lookup keys should be: [lookupkey1, lookupkey2].", "[lookupkey1, lookupkey2]", keys.toString());

        //Check principals
        Set<Principal> principals = object.getSubject().getPrincipals();
        assertEquals("There should be one principal.", 1, principals.size());
        Principal principal = principals.iterator().next();
        assertEquals("The principal name should be: securityName1.", "securityName1", principal.getName());

        //Check private credentials
        Hashtable<String, Object> privateCreds = (Hashtable<String, Object>) object.getSubject().getPrivateCredentials().iterator().next();
        assertEquals("There should be one private credential.", 1, privateCreds.size());
        assertEquals("The credential should be: {privatekey1=privatevalue1}", "{privatekey1=privatevalue1}", privateCreds.toString());

        //Check public credentials
        Hashtable<String, Object> publicCreds = (Hashtable<String, Object>) object.getSubject().getPublicCredentials().iterator().next();
        assertEquals("There should be one public credential.", 1, publicCreds.size());
        assertEquals("The credential should be: {publickey1=publicvalue1}", "{publickey1=publicvalue1}", publicCreds.toString());

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
    public static void main(String[] args) throws Exception {
        Subject subject = new Subject();

        //Add private creds
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put("privatekey1", "privatevalue1");
        subject.getPrivateCredentials().add(hashtable);

        //Add public creds
        Hashtable<String, Object> hashtable2 = new Hashtable<String, Object>();
        hashtable2.put("publickey1", "publicvalue1");
        subject.getPublicCredentials().add(hashtable2);

        //Add principal
        Principal principal = new WSPrincipal("securityName1", "accessId1", "authMethod1");
        subject.getPrincipals().add(principal);

        //Create CacheObject
        CacheObject object = new CacheObject(subject);

        //Add lookup keys
        object.addLookupKey("lookupkey1");
        object.addLookupKey("lookupkey2");

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
}
