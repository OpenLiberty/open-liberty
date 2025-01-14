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

import java.security.PrivateKey;

import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * Represents an LTPA Private Key; Encoding is non-standard. Uses 128 byte RSA.
 * With FIPS enabled, it is based on RSA/SHA-256 (256 byte RSA key).
 */
public final class LTPAPrivateKey implements PrivateKey {

	private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();
	private static final long serialVersionUID = -2566137894245694562L;
	private static final int PRIVATE_EXPONENT = 1;
	private static final int PUBLIC_EXPONENT = 2;
	private static final int PRIME_P = 3;
	private static final int PRIME_Q = 4;
	private static final int PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH = 4;
	private static final int PUBLIC_EXPONENT_LENGTH = 3;
	private static final int PRIME_P_LENGTH = (fipsEnabled ? 129 : 65);
	private static final int PRIME_Q_LENGTH = (fipsEnabled ? 129 : 65);
	private int privateExponentLength;
	private final byte[][] rawKey;
	private final byte[] encodedKey;

	LTPAPrivateKey(byte[][] key) {
		this.rawKey = key;
		LTPACrypto.setRSAKey(key);
		this.privateExponentLength = key[PRIVATE_EXPONENT].length;
		this.encodedKey = encode();
	}

	public LTPAPrivateKey(byte[] encodedKey) {
		this.encodedKey = encodedKey.clone();
		this.rawKey = decode(encodedKey);
	}

	/*
	 * Encoding/decoding are based on non-standard format; basically all the byte
	 * arrays are concatenated. As we know the length of the components, they can be
	 * reconstructed back.
	 *
	 * @param encodedPrivateKey The encoded key
	 */
	private final byte[][] decode(byte[] encodedPrivateKey) {
		byte[][] decodedKey = new byte[8][];
		if (encodedPrivateKey.length > (PUBLIC_EXPONENT_LENGTH + PRIME_P_LENGTH + PRIME_Q_LENGTH)) {
			// it is potentially the new encoding mechanism based on R3.5 [with CRT key
			// information added for Domino]
			// determine the length of the CRT key by looking at the first four bytes
			byte[] lengthBytes = new byte[PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH];
			for (int i = 0; i < PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH; i++) {
				lengthBytes[i] = encodedPrivateKey[i];
			}
			privateExponentLength = toInt(lengthBytes);
			decodedKey[PRIVATE_EXPONENT] = new byte[privateExponentLength];
			decodedKey[PUBLIC_EXPONENT] = new byte[PUBLIC_EXPONENT_LENGTH];
			decodedKey[PRIME_P] = new byte[PRIME_P_LENGTH];
			decodedKey[PRIME_Q] = new byte[PRIME_Q_LENGTH];

			System.arraycopy(encodedPrivateKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH, decodedKey[PRIVATE_EXPONENT], 0,
					privateExponentLength);
			System.arraycopy(encodedPrivateKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength,
					decodedKey[PUBLIC_EXPONENT], 0, PUBLIC_EXPONENT_LENGTH);
			System.arraycopy(encodedPrivateKey,
					PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength + PUBLIC_EXPONENT_LENGTH,
					decodedKey[PRIME_P], 0, PRIME_P_LENGTH);
			System.arraycopy(encodedPrivateKey, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength
					+ PUBLIC_EXPONENT_LENGTH + PRIME_P_LENGTH, decodedKey[PRIME_Q], 0, PRIME_Q_LENGTH);
		} else {
			// it is a R3.02 key [without CRT key information]
			decodedKey[PUBLIC_EXPONENT] = new byte[PUBLIC_EXPONENT_LENGTH];
			decodedKey[PRIME_P] = new byte[PRIME_P_LENGTH];
			decodedKey[PRIME_Q] = new byte[PRIME_Q_LENGTH];

			System.arraycopy(encodedPrivateKey, 0, decodedKey[PUBLIC_EXPONENT], 0, PUBLIC_EXPONENT_LENGTH);
			System.arraycopy(encodedPrivateKey, PUBLIC_EXPONENT_LENGTH, decodedKey[PRIME_P], 0, PRIME_P_LENGTH);
			System.arraycopy(encodedPrivateKey, PUBLIC_EXPONENT_LENGTH + PRIME_P_LENGTH, decodedKey[PRIME_Q], 0,
					PRIME_Q_LENGTH);
		}
		return decodedKey;
	}

	private byte[] encode() {
		int encodedKeyLength = PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength + PUBLIC_EXPONENT_LENGTH
				+ PRIME_P_LENGTH + PRIME_Q_LENGTH;
		byte[] encodedPrivateKey = new byte[encodedKeyLength];
		byte[] lengthBytes = toByteArray(privateExponentLength);
		copy(lengthBytes, 0, PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH, encodedPrivateKey, 0);
		copy(rawKey[PRIVATE_EXPONENT], 0, privateExponentLength, encodedPrivateKey,
				PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH);
		copy(rawKey[PUBLIC_EXPONENT], 0, PUBLIC_EXPONENT_LENGTH, encodedPrivateKey,
				PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength);
		copy(rawKey[PRIME_P], 0, PRIME_P_LENGTH, encodedPrivateKey,
				PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength + PUBLIC_EXPONENT_LENGTH);
		copy(rawKey[PRIME_Q], 0, PRIME_Q_LENGTH, encodedPrivateKey,
				PRIVATE_EXPONENT_LENGTH_FIELD_LENGTH + privateExponentLength + PRIME_P_LENGTH + PUBLIC_EXPONENT_LENGTH);
		return encodedPrivateKey;
	}

	public static byte[] toByteArray(int a) {
		byte[] b = new byte[4];
		b[0] = (byte) ((a >>> 24) & 0xFF);
		b[1] = (byte) ((a >>> 16) & 0xFF);
		b[2] = (byte) ((a >>> 8) & 0xFF);
		b[3] = (byte) ((a >>> 0) & 0xFF);
		return b;
	}

	public static final int toInt(byte[] byteVal) {
		int i = byteVal[3] & 0xFF;
		i |= ((byteVal[2] << 8) & 0xFF00);
		i |= ((byteVal[1] << 16) & 0xFF0000);
		i |= ((byteVal[0] << 24) & 0xFF000000);
		return i;
	}

	private void copy(byte[] from, int offsetFrom, int len, byte[] to, int offsetTo) {
		for (int i = 0; i < len; i++) {
			to[offsetTo + i] = from[offsetFrom + i];
		}
	}

	/**
	 * Return the algorithm used - RSA/SHA-1.
	 *
	 * @return Always RSA/SHA-1
	 */
	@Override
	public final String getAlgorithm() {
		return (fipsEnabled ? "RSA/SHA-256" : "RSA/SHA-1");
	}

	/** {@inheritDoc} */
	@Override
	public final byte[] getEncoded() {
		return encodedKey.clone();
	}

	/**
	 * Get the format of the private key.
	 *
	 * @return Always LTPAFormat
	 */
	@Override
	public final String getFormat() {
		return "LTPAFormat";
	}

	/**
	 * Get the raw data of the private key.
	 *
	 * @return The raw data of the key
	 */
	protected final byte[][] getRawKey() {
		if (rawKey == null) {
			return null;
		} else {
			return rawKey.clone();
		}
	}

}
