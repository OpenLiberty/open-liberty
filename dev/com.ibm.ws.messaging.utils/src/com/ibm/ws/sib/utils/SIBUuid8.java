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

/**
 * This class represents a 8 byte long UUID
 */

public final class SIBUuid8 extends SIBUuidLength {

  private static final int LENGTH = 8;
  private static final String zeroString = new SIBUuid8(new byte[LENGTH]).toString();

  /**
   * Construct a SIBUuid8 object.
   */

  public SIBUuid8 () {
    super(LENGTH);
  }

  /**
   * Construct a SIBUuid8 object from a byte array. The byte array length is
   * not significant, if less than 8 bytes long then 0 padding will be applied,
   * if longer than 8 bytes then the first 8 bytes of the byte array will be
   * used.
   *
   * @param bytes The byte array representing the UUID
   */

  public SIBUuid8 (byte[] bytes) {
    super(LENGTH, bytes);
  }

  /**
   * Construct a SIBUuid8 object from a String. The string length is not
   * significant, if less than 8 characters then 0 padding will be applied,
   * if longer than 8 characters then the first 8 characters of the string will
   * be used. Only hexadecimal and dash characters are permitted in the string.
   *
   * @param string The string representing the UUID
   */

  public SIBUuid8 (String string) {
    super(LENGTH, string);
  }

  /**
   * Return the zero SIBUuid8 representation
   *
   * @return A string representing the zero SIBUuid8
   */

  public static String toZeroString () {
    return zeroString;
  }
  
  /**
   * Return a long representing the SIBUuid8.
   *
   * @return A long representing the SIBUuid8
   */
  public long toLong () {
    long result = 0;
    
    byte[] uuidBytes = super.toByteArray();
    
    for (int i = 0; i < 8; i++)
    {
      result = result << 8;
      result += (int) uuidBytes[i];
    }

    return result;
  }
}
