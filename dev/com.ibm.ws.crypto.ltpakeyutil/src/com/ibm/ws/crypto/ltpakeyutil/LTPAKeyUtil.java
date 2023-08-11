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
import com.ibm.ws.kernel.service.util.JavaInfo;

public final class LTPAKeyUtil {

  public static boolean ibmJCEAvailable = false;
  public static boolean openJCEPlusAvailable = false;
  public static boolean providerChecked = false;
  public static boolean zosProviderChecked = false;

  public static boolean javaVersionChecked = false;
  public static boolean isJava11orHigher = false;

  public static String osName = System.getProperty("os.name");
  public static boolean isZOS = false;
  public static boolean osVersionChecked = false;

  public static String IBM_JCE_PROVIDER = "com.ibm.crypto.provider.IBMJCE";
  public static String OPENJCEPLUS_PROVIDER = "com.ibm.crypto.plus.provider.OpenJCEPlus";

  public static byte[] encrypt(byte[] data, byte[] key, String cipher) throws Exception {
    return LTPACrypto.encrypt(data, key, cipher);
  }

  public static byte[] decrypt(byte[] msg, byte[] key, String cipher) throws Exception {
    return LTPACrypto.decrypt(msg, key, cipher);
  }

  public static boolean verifyISO9796(byte[][] key, byte[] data, int off, int len, byte[] sig, int sigOff, int sigLen)
      throws Exception {
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
    } else {
      ibmJCEAvailable = JavaInfo.isSystemClassAvailable(IBM_JCE_PROVIDER);
      providerChecked = true;
      return ibmJCEAvailable;
    }

  }

  public static boolean isOpenJCEPlusAvailable() {
    // change to z
    if (zosProviderChecked) {
      return openJCEPlusAvailable;
    } else {
      openJCEPlusAvailable = JavaInfo.isSystemClassAvailable(OPENJCEPLUS_PROVIDER);
      zosProviderChecked = true;
      return openJCEPlusAvailable;
    }

  }

  private static boolean isJava11orHigher() {
    if (javaVersionChecked) {
      return isJava11orHigher;
    } else {
      isJava11orHigher = JavaInfo.majorVersion() >= 11;
      javaVersionChecked = true;
      return isJava11orHigher;
    }
  }

  private static boolean isZOS() {
    if (osVersionChecked) {
      return isZOS;
    } else {
      isZOS = (osName.equalsIgnoreCase("z/OS") || osName.equalsIgnoreCase("OS/390"));
      osVersionChecked = true;
      return isZOS;
    }
  }

  public static boolean isZOSandRunningJava11orHigher() {
    return isJava11orHigher() && isZOS();
  }

}