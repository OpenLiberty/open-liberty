/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.utils;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/*
 * This class provides a package private implementation of a UUID of any length
 * up to the maximum length supported by the CryptoHash algorithm. Concrete
 * classes implementing a fixed UUID length extend this class and provide public
 * constructors.
 */

abstract class SIBUuidLength {

  private static final String className = SIBUuidLength.class.getName();
  private byte[] uuid;
  private final static String ipaddress = getIPAddress();

  protected SIBUuidLength (int length) {
    uuid = new byte[length];

    byte[] seed = UUID.randomUUID().toString().getBytes();

    CryptoHash.hash(seed, uuid);
  }

  // The bytes argument should have previously been obtained using toByteArray

  protected SIBUuidLength (int length, byte[] bytes) {
    if (bytes == null) {
      throw new NullPointerException();
    }

    uuid = new byte[length];

    System.arraycopy(bytes, 0, uuid, 0, Math.min(uuid.length, bytes.length));
  }

  // Only hexadecimal and dash characters are permitted in the string

  protected SIBUuidLength (int length, String string) {
    if (string == null) {
      throw new NullPointerException();
    }

    uuid = new byte[length];

    // Remove all dash characters

    StringBuffer sb = new StringBuffer();
    for (int i=0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (ch != '-') sb.append(ch);
    }

    String string2 = sb.toString();

    // Parse out the hexadecimal byte pairs into bytes

    int j = 0;
    for (int i=0; i < string2.length() && j < uuid.length; i+=2) {
      sb = new StringBuffer();

      sb.append(string2.substring(i, i+1));

      if (i+1 < string2.length()) {
        sb.append(string2.substring(i+1, i+2));
      } else {
        sb.append("0");
      }

      uuid[j++] = (byte)Integer.parseInt(sb.toString(), 16);
    }
  }

  public byte[] toByteArray () {
    return uuid;
  }

  public boolean equals (Object o) {
    boolean rc = false;

    if (this == o) {
      rc = true;
    } else if ((o != null) && (o.getClass() == getClass())) {
      rc = Arrays.equals(uuid, ((SIBUuidLength)o).uuid);
    }

    return rc;
  }

  private int hashcode;

  public int hashCode () {

    if (hashcode == 0) {
      int rc = 0;

      for (int i=0; i < uuid.length; i++) {
        rc += uuid[i];
      }

      hashcode = rc;
    }

    return hashcode;
  }

  private int stringLength = 0;

  public int getStringLength() {

    if (stringLength == 0) {
      stringLength = uuid.length * 2;
    }

    return stringLength;
  }

  private String string;

  public String toString() {

    final char _NIBBLE[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    if (string == null) {

      char[] chars = new char[getStringLength()];

      int j = 0;
      for (int i=0; i < chars.length; i++) {
        chars[i++] = _NIBBLE[(uuid[j]   & 0xF0) >>> 4];
        chars[i]   = _NIBBLE[(uuid[j++] & 0x0F) >>> 0];
      }

      string = new String(chars);
    }

    return string;
  }

  // InetAddress is compatible with both IPv4 & IPv6

  private static String getIPAddress () {
    String rc;

    try {
      rc = InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      // No FFDC code needed
      rc = Long.valueOf(new Random().nextLong()).toString();
    }

    return rc;
  }

}
