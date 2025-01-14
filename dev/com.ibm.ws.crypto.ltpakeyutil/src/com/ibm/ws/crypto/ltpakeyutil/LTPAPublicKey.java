/*******************************************************************************
 * Copyright (c) 1997, 2024 IBM Corporation and others.
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
package com.ibm.ws.crypto.ltpakeyutil;

import java.security.PublicKey;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * Represents an LTPA Public Key based on RSA/SHA-1. Its based on a 128 byte RSA
 * key. With FIPS enabled, it is based on RSA/SHA-256 (256 byte RSA key).
 */
public final class LTPAPublicKey implements PublicKey {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final long serialVersionUID = 6585779055758956436L;
	private static final int MODULUS = 0;
	private static final int EXPONENT = 1;
	private static final int MODULUS_LENGTH = (fipsEnabled ? 257 : 129);
	private static final int EXPONENT_LENGTH = 3;
	private final byte[][] rawKey;
	private final byte[] encodedKey;

	LTPAPublicKey(byte[][] rawKey) {
		this.rawKey = rawKey;
		this.encodedKey = encode();
	}

	public LTPAPublicKey(byte[] encodedKey) {
		this.encodedKey = encodedKey.clone();
		this.rawKey = decode(encodedKey);
	}

	/**
	 * encoding/decoding are based on non-standard LTPA specific algorithm.
	 * concatenates byte arrays of raw key to a format that can be decoded based on
	 * length of each component.
	 *
	 * @param encodedPublicKey The encoded key
	 */
	private byte[][] decode(byte[] encodedPublicKey) {
		byte[][] decodedKey = new byte[2][];
		decodedKey[MODULUS] = new byte[MODULUS_LENGTH];
		decodedKey[EXPONENT] = new byte[EXPONENT_LENGTH];
		System.arraycopy(encodedPublicKey, 0, decodedKey[MODULUS], 0, MODULUS_LENGTH);
		System.arraycopy(encodedPublicKey, MODULUS_LENGTH, decodedKey[EXPONENT], 0, EXPONENT_LENGTH);
		return decodedKey;
	}

	private byte[] encode() {
		int publicKeyLength = MODULUS_LENGTH + EXPONENT_LENGTH;
		byte[] encodedPublicKey = new byte[publicKeyLength];
		System.arraycopy(rawKey[MODULUS], 0, encodedPublicKey, 0, MODULUS_LENGTH);
		System.arraycopy(rawKey[EXPONENT], 0, encodedPublicKey, MODULUS_LENGTH, EXPONENT_LENGTH);
		return encodedPublicKey;
	}

	/** {@inheritDoc} */
	@Override
	public final String getAlgorithm() {
		return (fipsEnabled ? "RSA/SHA-256" : "RSA/SHA-1");
	}

	/** {@inheritDoc} */
	@Override
	public final byte[] getEncoded() {
		return encodedKey.clone();
	}

	/** {@inheritDoc} */
	@Override
	public final String getFormat() {
		return "LTPAFormat";
	}

	protected final byte[][] getRawKey() {
		if (rawKey == null) {
			return null;
		} else {
			return rawKey.clone();
		}
	}
}
