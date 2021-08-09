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
 * This class represents a 12 byte long UUID
 */

public final class SIBUuid12 extends SIBUuidLength {

  private static final int LENGTH = 12;
  private static final String zeroString = new SIBUuid12(new byte[LENGTH]).toString();

  /**
   * Construct a SIBUuid12 object.
   */

  public SIBUuid12 () {
    super(LENGTH);
  }

  /**
   * Construct a SIBUuid12 object from a byte array. The byte array length is
   * not significant, if less than 12 bytes long then 0 padding will be applied,
   * if longer than 12 bytes then the first 12 bytes of the byte array will be
   * used.
   *
   * @param bytes The byte array representing the UUID
   */

  public SIBUuid12 (byte[] bytes) {
    super(LENGTH, bytes);
  }

  /**
   * Construct a SIBUuid12 object from a String. The string length is not
   * significant, if less than 12 characters then 0 padding will be applied,
   * if longer than 12 characters then the first 12 characters of the string will
   * be used. Only hexadecimal and dash characters are permitted in the string.
   *
   * @param string The string representing the UUID
   */

  public SIBUuid12 (String string) {
    super(LENGTH, string);
  }

  /**
   * Return the zero SIBUuid12 representation
   *
   * @return A string representing the zero SIBUuid12
   */

  public static String toZeroString () {
    return zeroString;
  }

}
