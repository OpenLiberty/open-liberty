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
package com.ibm.websphere.security.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import org.junit.Test;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidBuilderException;
import com.ibm.websphere.security.jwt.JwtException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.websphere.security.jwt.KeyException;
import com.ibm.ws.security.jwt.internal.ClaimsImpl;

/**
 * Ensure UserProfile and all serialized instances of UserProfile remain serializable.
 * If UserProfile is changed, create a new UserProfile_x.ser file and write a new test.
 * Each new version should be able to deserialze every previous version.
 * This is necessary to maintain version to version compatibility for the distributed authentication cache.
 */
public class UserProfileSerializationTest {

    /**
     * Test to deserialize UserProfile_1.ser.
     * Validate JWT, Claims, CustomProps, and userInfo
     */
	//TODO: This test will need to be updated when JwtToken is made serializable.
	// Currently (08/22), TestJwtToken implements Serializable so that the UserProfile
	// object can be serialized. When JwtToken is made serializable, remove
	// Serializable from TestJwtToken and create a new .ser file.
    @Test
    public void deserializeUserProfile_1() throws Exception {
        String filename = "test-resources/ser-files/UserProfile_1.ser";
        FileInputStream file = new FileInputStream(filename);
        ObjectInputStream in = new ObjectInputStream(file);

        UserProfile object = (UserProfile) in.readObject();
        in.close();
    	
        //Check claims
        Claims claims = object.getClaims();
        assertEquals("The claims issuer should be: issuer.", "issuer", claims.getIssuer());
        assertEquals("The claims subject should be: subject.", "subject", claims.getSubject());

        //Check userInfo
        String userInfo = object.getUserInfo();
        assertEquals("The userInfo should be: userInfo.", "userInfo", userInfo);

        
        //Check customProps via accessTokenAlias
        String accessTokenAlias = object.getAccessTokenAlias();
        assertEquals("The accessTokenAlias should be: accessTokenAliasValue.", "accessTokenAliasValue", accessTokenAlias);

        //Check JWT
        JwtToken jwt = object.getIdToken();
        assertEquals("The jtw header should be: header1", "header1", jwt.getHeader(null));

    }

    /**
     * Method used to create and serialize the UserProfile for testing.
     *
     * If UserProfile changes, previously serialized versions of
     * UserProfile must remain deserializable. 
     * Use this method to create a new UserProfile_x.ser file, 
     * replacing the x with the current version + 1.
     * Then write a test that deserializes that version and all
     * previous UserProfile_x.ser files.
     */
    public static void main(String[] args) throws Exception {
        
        //Add private creds
        Hashtable<String, Object> customProps = new Hashtable<String, Object>();
        customProps.put("accessTokenAlias", "accessTokenAliasValue");
        customProps.put("id_token", "jwt");

        //Add public creds
        Claims claims = new ClaimsImpl();
        claims.put("iss", "issuer");
        claims.put("sub", "subject");
        
        //Create the JWT
        JwtToken j = new TestJwtToken();

        //Create UserProfile
        UserProfile object = new UserProfile(j, customProps, claims, "userInfo");

        // Serialization
        String filename = "test-resources/ser-files/UserProfile_x.ser";
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
