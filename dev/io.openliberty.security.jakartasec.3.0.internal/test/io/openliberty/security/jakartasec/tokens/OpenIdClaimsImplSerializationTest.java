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
package io.openliberty.security.jakartasec.tokens;

import static jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant.SUBJECT_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;

/**
 * Serialization test for the OpenIdClaims.
 */
public class OpenIdClaimsImplSerializationTest {

    private static final String SERIALIZED_FILE_VERS_1 = "test-resources/testdata/ser-files/OpenIdClaimsImpl_1.ser";

    private static final String SUBJECT = "thisIsTheSubjectName";

    @Test
    public void testOpenIdClaimsSerialization_Ver1() throws Exception {
        /*
         * Deserialize the object from the serialized file.
         */
        OpenIdClaimsImpl object = null;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(SERIALIZED_FILE_VERS_1))) {
            object = (OpenIdClaimsImpl) input.readObject();
        }
        assertNotNull("OpenIdClaimsImpl instance could not be read from the serialized file.", object);

        assertEquals("Incorrect subject", SUBJECT, object.getSubject());
    }

    /**
     * Method used to create and serialize the OpenIdClaimsImpl for testing.
     *
     * If OpenIdClaimsImpl changes, previously serialized versions of
     * OpenIdClaimsImpl must remain deserializable. Use this method to
     * create a new OpenIdClaimsImpl_x.ser file, replacing the x with the
     * current version + 1. Then write a test that deserializes that version and
     * all previous OpenIdClaimsImpl_x.ser files.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Create a serialized OpenIdClaims file");
        final String filename = "test-resources/testdata/ser-files/OpenIdClaimsImpl_x.ser"; // change x to a version number now, or after creation

        OpenIdClaims openIdClaimsToWrite = OpenIdClaimsImplTest.createOpenIdClaimsWithStringClaim(SUBJECT_IDENTIFIER, SUBJECT);
        /*
         * Serialize the object to a file.
         */
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename))) {
            output.writeObject(openIdClaimsToWrite);
        }
    }

}
