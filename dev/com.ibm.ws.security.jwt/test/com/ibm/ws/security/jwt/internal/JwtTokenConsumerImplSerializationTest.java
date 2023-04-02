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

package com.ibm.ws.security.jwt.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.Test;

/**
 * Test serialization of the JwtTokenConsumerImpl class. This class needs to
 * maintain deserialization across versions.
 */
public class JwtTokenConsumerImplSerializationTest {

	private static final String JWT_STRING = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5Q2FqVE9EZmRCNzZHQTdXWGNsSWlxdjQ4NnhCSENLMkpfYjBoMllGcGtVIn0.eyJleHAiOjE2NjUwMjE1NTQsImlhdCI6MTY2NTAyMTI1NCwiYXV0aF90aW1lIjoxNjY1MDIxMjUzLCJqdGkiOiJkNTk3YTNjMC0zYjUzLTQ1YzctOWEyZi05YTkxMjM5OWRkNGQiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo0OTE2My9hdXRoL3JlYWxtcy9UZXN0UmVhbG0iLCJhdWQiOiJvaWRjX2NsaWVudCIsInN1YiI6ImVkNDM4MzY0LWQyMDgtNGI1Ny05YmZhLTI3ODg0ZmQzZWU4YiIsInR5cCI6IklEIiwiYXpwIjoib2lkY19jbGllbnQiLCJub25jZSI6Inh1OTJocWxNZlFuMTZuTDJwN3hQIiwic2Vzc2lvbl9zdGF0ZSI6ImNmN2E1ZWQ2LTE4NjgtNGJiYi05Y2Q4LTY2OGE3ZTVjZTczZiIsImF0X2hhc2giOiI3LTBVODhVeW5JZllBX1hXRndSVkR3IiwiYWNyIjoiMSIsInNpZCI6ImNmN2E1ZWQ2LTE4NjgtNGJiYi05Y2Q4LTY2OGE3ZTVjZTczZiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoidGVzdHVzZXIiLCJlbWFpbCI6InRlc3R1c2VyQGxpYmVydHkub3JnIn0.FmPWclimtj_FOeeaQTSYtekZ775crwJdVEjjaCWwD5tmBWCjCAyEZvbeAXJcLIBDlwznhYIJ4E4dKdjiyVBUzjtc39DpTWbilu0KGDvallvOCj8bxBeGW3CHYQKCwOgWhxQ7mQV4pJ7lW5VQWHxNw4jxHbQ-HEWqhaf04vtKMXqFaEQpwXqeU4jYSnq6-1lKsFmaUpJU5to9YpfZV_bibMLfnN8vCS84CrMhG_mTfEsvvb-R2OwTTxC2q1sCnStX0CgfViKoiAo1wqNRRZU7boqnt_R50nL1hCz2quIHGnVHqLUdEjAeUt0fHLTVfK-edM773IjwKhe5ytg81rVT_A";
	private static final String JSON_CLAIMS = "{\"at_hash\":\"7-0U88UynIfYA_XWFwRVDw\",\"sub\":\"ed438364-d208-4b57-9bfa-27884fd3ee8b\",\"email_verified\":false,\"iss\":\"https://localhost:49163/auth/realms/TestRealm\",\"typ\":\"ID\",\"preferred_username\":\"testuser\",\"nonce\":\"xu92hqlMfQn16nL2p7xP\",\"sid\":\"cf7a5ed6-1868-4bbb-9cd8-668a7e5ce73f\",\"aud\":\"oidc_client\",\"acr\":\"1\",\"azp\":\"oidc_client\",\"auth_time\":1665021253,\"exp\":1665021554,\"session_state\":\"cf7a5ed6-1868-4bbb-9cd8-668a7e5ce73f\",\"iat\":1665021254,\"jti\":\"d597a3c0-3b53-45c7-9a2f-9a912399dd4d\",\"email\":\"testuser@liberty.org\"}";

	private static final String SERIALIZED_FILE_VERS_1 = "test-resources/testdata/ser-files/JwtTokenConsumerImpl_1.ser";

	@Test
	public void testSerialization_Ver1() throws Exception {
		/*
		 * Deserialize the object from the serialized file.
		 */
		JwtTokenConsumerImpl object = null;
		try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(SERIALIZED_FILE_VERS_1))) {
			object = (JwtTokenConsumerImpl) input.readObject();
		}
		assertNotNull("JwtTokenConsumerImpl instance could not be read from the serialized file.", object);

		/*
		 * Verify the data from the instance matches the expected.
		 */
		assertEquals("The compact() method returned an unexpected value.", JWT_STRING, object.compact());
		assertEquals("The getClaims() method returned an unexpected value.", JSON_CLAIMS,
				object.getClaims().toJsonString());
		assertEquals("The getHeader() method returned an unexpected value.", "RS256", object.getHeader("alg"));
		assertEquals("The getHeader() method returned an unexpected value.", "JWT", object.getHeader("typ"));
		assertEquals("The getHeader() method returned an unexpected value.",
				"9CajTODfdB76GA7WXclIiqv486xBHCK2J_b0h2YFpkU", object.getHeader("kid"));
	}

	/**
	 * Method used to create and serialize the JwtTokenConsumerImpl for testing.
	 *
	 * If JwtTokenConsumerImpl changes, previously serialized versions of
	 * JwtTokenConsumerImpl must remain deserializable. Use this method to
	 * create a new JwtTokenConsumerImpl_x.ser file, replacing the x with the
	 * current version + 1. Then write a test that deserializes that version and
	 * all previous JwtTokenConsumerImpl_x.ser files.
	 */
	public static void main(String[] args) throws Exception {
		final String filename = "test-resources/testdata/ser-files/JwtTokenConsumerImpl_x.ser";

		/*
		 * Create JwtTokenConsumerImpl
		 */
		JwtConsumerBuilder builder = new JwtConsumerBuilder().setSkipAllValidators().setDisableRequireSignature()
				.setSkipSignatureVerification();
		JwtTokenConsumerImpl object = new JwtTokenConsumerImpl(builder.build().process(JWT_STRING));

		/*
		 * Serialize the object to a file.
		 */
		try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename))) {
			output.writeObject(object);
		}
	}
}
