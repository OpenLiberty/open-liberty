
/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import java.security.Key;

import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

// config holder for JwtData class so it can be called by other projects
public class JwtDataConfig {
	public final String signatureAlgorithm;
	public final JSONWebKey jwk;
	public final String sharedKey;
	public final Key signingKey;
	public final String keyAlias;
	public final String keyStoreRef;
	public final String tokenType;
	public final boolean isJwkEnabled;

	public JwtDataConfig(String signatureAlgorithm, JSONWebKey jwk, String sharedKey, Key signingKey, String keyAlias,
			String keyStoreRef, String tokenType, boolean isJwkEnabled) {
		this.signatureAlgorithm = signatureAlgorithm;
		this.jwk = jwk;
		this.sharedKey = sharedKey;
		this.signingKey = signingKey;
		this.keyAlias = keyAlias;
		this.keyStoreRef = keyStoreRef;
		this.tokenType = tokenType;
		this.isJwkEnabled = isJwkEnabled;
	}

}
