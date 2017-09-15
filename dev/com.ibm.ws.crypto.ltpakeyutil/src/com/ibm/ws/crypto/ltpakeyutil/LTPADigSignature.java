/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.ltpakeyutil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class LTPADigSignature {

    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA";

    static byte[][] testRawPubKey = null;
    static byte[][] testRawPrivKey = null;
    static MessageDigest md1 = null;
    static MessageDigest md2 = null;
    static MessageDigest md1JCE = null;
    static MessageDigest md2JCE = null;
    static private Object lockObj1 = new Object();
    static private Object lockObj2 = new Object();
    static long created = 0;
    static long cacheHits = 0;

    static {
        try {
            md1JCE = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
            md2JCE = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
            md1 = MessageDigest.getInstance("SHA");
            md2 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            // instrumented ffdc
        }
    }

    public LTPADigSignature() {
        super();
    }

    static void generateRSAKeys(byte[][] rsaPubKey, byte[][] rsaPrivKey) {
        byte[][] rsaKey = LTPACrypto.rsaKey(128, true, true); //64 is 512, 128 is 1024

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

    static byte[] sign(byte[] mesg, LTPAPrivateKey privKey) throws Exception {
        return sign(mesg, privKey, false);
    }

    static byte[] sign(byte[] mesg, LTPAPrivateKey privKey, boolean useJCE) throws Exception {
        byte[][] rsaPrivKey = privKey.getRawKey();
        byte[] data;
        synchronized (lockObj1) {
            if (useJCE)
                data = md1JCE.digest(mesg);
            else
                data = md1.digest(mesg);
        }

        LTPACrypto.setRSAKey(rsaPrivKey);
        byte[] signature = LTPACrypto.signISO9796(rsaPrivKey, data, 0, data.length);
        return signature;
    }

    static boolean verify(byte[] mesg, byte[] signature, LTPAPublicKey pubKey) throws Exception {
        return verify(mesg, signature, pubKey, false);
    }

    static boolean verify(byte[] mesg, byte[] signature, LTPAPublicKey pubKey, boolean useJCE) throws Exception {
        byte[][] rsaPubKey = pubKey.getRawKey();
        byte[] data;
        synchronized (lockObj2) {
            if (useJCE)
                data = md2JCE.digest(mesg);
            else
                data = md2.digest(mesg);
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
