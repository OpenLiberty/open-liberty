/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.Headers;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;

public class JwtTokenConsumerImpl implements JwtToken, Serializable {

	/*
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 *
	 *
	 * WARNING!!!!
	 *
	 * Carefully consider changes to this class. Serialization across different
	 * versions must always be supported. Additionally, any referenced classes
	 * must be available to the JCache provider's serialization.
	 *
	 *
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 */

	private static final long serialVersionUID = 1L;

	/**
	 * Non-transient field that can be used in the future to determine the
	 * format of the serialized fields in the
	 * {@link #readObject(ObjectInputStream)} method.
	 */
	@SuppressWarnings("unused")
	private final short serializationVersion = 1;

	private final Claims claims;
	private final String compact;
	private transient Headers jwtHeaders;

	public JwtTokenConsumerImpl(JwtContext jwtContext) {
		claims = new ClaimsImpl();
		claims.putAll(jwtContext.getJwtClaims().getClaimsMap());
		compact = jwtContext.getJwt();
		List<JsonWebStructure> jsonStructures = jwtContext.getJoseObjects();

		JsonWebStructure jsonStruct = jsonStructures.get(0);
		jwtHeaders = jsonStruct.getHeaders();
	}

	@Override
	public Claims getClaims() {
		return claims;
	}

	@Override
	public String getHeader(String name) {
		return jwtHeaders.getStringHeaderValue(name);
	}

	@Override
	public String compact() {
		return compact;
	}

	private void readObject(ObjectInputStream input) throws ClassNotFoundException, IOException {
		/*
		 * Read all non-transient fields.
		 */
		input.defaultReadObject();

		/*
		 * Read the Headers JSON string back and use it to construct a Headers
		 * instance.
		 */
		try {
			jwtHeaders = new Headers();
			jwtHeaders.setFullHeaderAsJsonString((String) input.readObject());
		} catch (JoseException e) {
			throw new IOException("Error deserializing JWT headers.", e);
		}
	}

	private void writeObject(ObjectOutputStream output) throws IOException {
		/*
		 * Write all non-transient fields.
		 */
		output.defaultWriteObject();

		/*
		 * Since the Headers class is not serializable, we are going to copy the
		 * JSON string instead.
		 */
		output.writeObject(jwtHeaders.getFullHeaderAsJsonString());
	}
}
