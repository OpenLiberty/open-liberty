/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.ltpakeyutil;


import java.security.Provider;
import java.security.Security;

public final class LTPAKeyUtil {

  public static boolean ibmJCEAvailable = false;
  public static boolean providerChecked = false;

  public static byte[] encrypt(byte[] data, byte[] key, String cipher) throws Exception {
    return LTPACrypto.encrypt(data, key, cipher);
  }

  public static byte[] decrypt(byte[] msg, byte[] key, String cipher) throws Exception {
    return LTPACrypto.decrypt(msg, key, cipher);
  }

  public static boolean verifyISO9796(byte[][] key, byte[] data, int off, int len, byte[] sig, int sigOff, int sigLen) throws Exception {
    return LTPACrypto.verifyISO9796(key, data, off, len, sig, sigOff, sigLen);
  }

  public static byte[] signISO9796(byte[][] key, byte[] data, int off, int len) throws Exception {
    return LTPACrypto.signISO9796(key, data, off, len);
  }

  public static void setRSAKey(byte[][] key) {
    LTPACrypto.setRSAKey(key);
  }

  public static byte[][] getRawKey(LTPAPrivateKey privKey) {
    return privKey.getRawKey();
  }

  public static byte[][] getRawKey(LTPAPublicKey pubKey) {
    return pubKey.getRawKey();
  }

  public static LTPAKeyPair generateLTPAKeyPair() {
    return LTPADigSignature.generateLTPAKeyPair();
  }

  public static byte[] generate3DESKey() {
    return LTPACrypto.generate3DESKey();
  }

  public static boolean isIBMJCEAvailable() {
    if (providerChecked) {
      return ibmJCEAvailable;
    }
    else {
      Provider[] providers = Security.getProviders();
      for (int i = 0; i < providers.length; i++) {
        if (providers[i].toString().contains("IBMJCE")) {
          ibmJCEAvailable = true;
        }
      }
      providerChecked = true;
      return ibmJCEAvailable;
    }

  }

}