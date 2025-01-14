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

import java.security.MessageDigest;

import com.ibm.ws.common.crypto.CryptoUtils;

final class LTPADigSignature {

	static int keySize = (CryptoUtils.isFips140_3Enabled() ? 256 : 128);

	static byte[][] testRawPubKey = null;
	static byte[][] testRawPrivKey = null;
	static MessageDigest md1 = CryptoUtils.getMessageDigestForLTPA();
	static private Object lockObj1 = new Object();
	static long created = 0;
	static long cacheHits = 0;

	public LTPADigSignature() {
		super();
	}

	static void generateRSAKeys(byte[][] rsaPubKey, byte[][] rsaPrivKey) {
		byte[][] rsaKey = LTPACrypto.rsaKey(keySize, true, true); // 64 is 512, 128
																	// is 1024

		rsaPrivKey[0] = rsaKey[0];
		rsaPrivKey[2] = rsaKey[2];
		rsaPrivKey[4] = rsaKey[3];
		rsaPrivKey[3] = rsaKey[4];
		rsaPrivKey[5] = rsaKey[5];
		rsaPrivKey[6] = rsaKey[6];
		rsaPrivKey[7] = rsaKey[7];

		rsaPubKey[0] = rsaKey[0];
		rsaPubKey[1] = rsaKey[2];
	}

	static boolean verify(byte[] mesg, byte[] signature, LTPAPublicKey pubKey) throws Exception {
		byte[][] rsaPubKey = pubKey.getRawKey();
		byte[] data;
		synchronized (lockObj1) {
			data = md1.digest(mesg);
		}
		return LTPACrypto.verifyISO9796(rsaPubKey, data, 0, data.length, signature, 0, signature.length);
	}

	static LTPAKeyPair generateLTPAKeyPair() {
		byte[][] rsaPubKey = new byte[2][];
		byte[][] rsaPrivKey = new byte[8][];
		generateRSAKeys(rsaPubKey, rsaPrivKey);
		LTPAPublicKey pubKey = new LTPAPublicKey(rsaPubKey);
		LTPAPrivateKey privKey = new LTPAPrivateKey(rsaPrivKey);
		return new LTPAKeyPair(pubKey, privKey);
	}
}
