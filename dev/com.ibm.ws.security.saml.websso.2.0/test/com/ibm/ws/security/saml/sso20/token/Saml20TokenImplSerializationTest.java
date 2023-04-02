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
package com.ibm.ws.security.saml.sso20.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

import org.junit.Test;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.Assertion;

import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml.impl.Activator;
import com.ibm.ws.security.saml.sso20.rs.ByteArrayDecoder;

/**
 * Ensure Saml20TokenImpl and all serialized instances of the token remain serializable.
 * If Saml20TokenImpl is changed, create a new Saml20Token_x.ser file and write a new test.
 * Each new version should be able to deserialze every previous version.
 * This is necessary to maintain version to version compatibility for the distributed authentication cache.
 */
public class Saml20TokenImplSerializationTest {
    private final Activator activator = new Activator();

    static String samltokentext = "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"_5a9aa7a5baba3e58916977ebb2345a1f\" IssueInstant=\"2022-08-25T22:33:30.320Z\" Version=\"2.0\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">https://localhost:8960/idp/shibboleth</saml2:Issuer><saml2:Subject xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:uid\" NameQualifier=\"https://localhost:8960/idp/shibboleth\" SPNameQualifier=\"https://localhost:8020/ibm/saml20/sp1\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">testuser</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Address=\"127.0.0.1\" InResponseTo=\"_6KCNA7zJSPbN7RMnkLX231IilkCTCiAT\" NotOnOrAfter=\"2022-08-25T22:38:30.452Z\" Recipient=\"https://localhost:8020/ibm/saml20/sp1/acs\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2022-08-25T22:33:30.320Z\" NotOnOrAfter=\"2022-08-25T22:38:30.320Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:AudienceRestriction><saml2:Audience>https://localhost:8020/ibm/saml20/sp1</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AuthnStatement AuthnInstant=\"2022-08-25T22:33:29.821Z\" SessionIndex=\"_d92695639bbe1513de38822c291b630b\" SessionNotOnOrAfter=\"2022-08-25T23:33:30.350Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:SubjectLocality Address=\"127.0.0.1\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Attribute FriendlyName=\"WindowsDomainNameIdValue\" Name=\"WindowsDomainNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>\"test_WindowsDomainNameIdValue\"</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"uid\" Name=\"urn:oid:0.9.2342.19200300.100.1.1\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\"><saml2:AttributeValue>testuser</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"EntityNameIdValue\" Name=\"EntityNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>\"test_EntityNameIdValue\"</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"persistentNameIdValue\" Name=\"persistentNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>\"test_PersistentNameIdValue\"</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"userUniqueIdentifier\" Name=\"userUniqueIdentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>test_userUniqueIdentifier</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"userIdentifier\" Name=\"userIdentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>test_userIdentifier</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"KerberosNameIdValue\" Name=\"KerberosNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>\"test_KerberosNameIdValue\"</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"group\" Name=\"groupHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\"><saml2:AttributeValue>testuser</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"realmIdentifier\" Name=\"realmIdentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>test_realmIdentifier</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"groupIdentifier\" Name=\"groupIdentifier\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>test_groupIdentifier</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"mail\" Name=\"urn:oid:0.9.2342.19200300.100.1.3\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>testuser</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"CustomizeNameIdValue\" Name=\"CustomizeNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>\"test_CustomizeNameIdValue\"</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"TransientNameIdValue\" Name=\"TransientNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>\"test_TransientNameIdValue\"</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"X509SubjectNameNameIdValue\" Name=\"X509SubjectNameNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue>\"test_X509SubjectNameNameIdValue\"</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"email\" Name=\"urn:oid:0.9.2342.19200300.100.1.3\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\"><saml2:AttributeValue>testuser</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"realmname\" Name=\"hackRealm\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\"><saml2:AttributeValue>CN=testuser,O=IBM,C=US</saml2:AttributeValue></saml2:Attribute><saml2:Attribute FriendlyName=\"EncryptedNameIdValue\" Name=\"EncryptedNameIdHack\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">\"test_EncryptedNameIdValue\"</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

    /**
     * Test to deserialize Saml20Token_1.
     * Validate the samlString. This is used to populate
     * all the fields in Saml20TokenImpl during deserialization.
     *
     * @throws Exception
     */
    @Test
    public void deserializeSaml20Token_1() throws Exception {
        final String filename = "test-resources/ser-files/Saml20TokenImpl_1.ser";

        /*
         * When running in personal build, the tests are run in a single JRE, and the ActivatorTest
         * tests mess with the configuration and this test fails. Clear out the initialization
         * so it can occur cleanly. Then start the activator.
         */
        Field field = Activator.class.getDeclaredField("bInit");
        field.setAccessible(true);
        field.set(null, Boolean.FALSE);
        activator.start(null);

        /*
         * Deserialize the token instance from the file.
         */
        Saml20TokenImpl object = null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            object = (Saml20TokenImpl) in.readObject();
        }
        assertNotNull("SAML token should not be null.", object);

        /*
         * Check serializationVersion via reflection
         */
        field = Saml20TokenImpl.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        Object value = field.get(object);
        assertEquals("The version should be -862850937499495719L", -862850937499495719L, value);

        /*
         * Check samlString. This is used to populate all the other fields of Saml20TokenImpl.
         */
        String samlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + samltokentext;
        assertEquals("The samlString should be: " + samlString, samlString, object.getSAMLAsString());
    }

    /**
     * Method used to create and serialize the Saml20TokenImpl for testing.
     *
     * If Saml20TokenImpl changes, previously serialized versions of
     * Saml20TokenImpl must remain deserializable.
     * Use this method to create a new Saml20TokenImpl_x.ser file,
     * replacing the x with the current version + 1.
     * Then write a test that deserializes that version and all
     * previous Saml20TokenImpl_x.ser files.
     */
    public static void main(String[] args) throws Exception {
        final String filename = "test-resources/ser-files/Saml20TokenImpl_x.ser";

        byte[] bytes = null;
        Saml20Token saml = null;
        String issuer = "https://localhost:8960/idp/shibboleth";
        bytes = samltokentext.getBytes("UTF-8");
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ByteArrayDecoder byteArrayDecoder = new ByteArrayDecoder();
        Object b = byteArrayDecoder.unmarshallMessage(byteArrayInputStream);

        SAMLObject samlxmlobj = (SAMLObject) b;
        if (samlxmlobj instanceof Assertion) {
            saml = new Saml20TokenImpl(((Assertion) samlxmlobj));
        }

        assertNotNull("token is null", saml);
        assertEquals(issuer, saml.getSAMLIssuerName());

        /*
         * Serialize the object instance to a file.
         */
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename))) {
            output.writeObject(saml);
        }

        System.out.println("Object has been serialized");
    }
}
