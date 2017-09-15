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

import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;

/**
 * Perform conversions between a binary value in an array of bytes and a String
 * or StringBuffer which represents that value in Hex.
 */

public final class HexString {

  private final static TraceComponent tc = SibTr.register(HexString.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);
  private final static TraceNLS       nls = TraceNLS.getTraceNLS(UtConstants.MSG_BUNDLE);


  /**
   * Converts a binary value held in a byte array into a hex string, in the
   * given StringBuffer, using exactly two characters per byte of input.
   *
   * @param bin    The byte array containing the binary value.
   * @param start  The offset into the byte array for conversion to start..
   * @param length The number of bytes to convert.
   * @param hex    The StringBuffer to contain the hex string.
   */
  public static void binToHex(byte[] bin, int start, int length, StringBuffer hex) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "binToHex", new Object[]{bin, Integer.valueOf(start), Integer.valueOf(length), hex});

    /* Constant for binary to Hex conversion                                  */
    final char BIN2HEX[] = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    int binByte;

    for (int i=start; i<start+length; i++)
    {
      binByte = bin[i];

      /* SibTreat the byte as unsigned */
      if (binByte < 0) binByte += 256;

      /* Convert and append each digit */
      hex.append(BIN2HEX[binByte/16]);
      hex.append(BIN2HEX[binByte%16]);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "binToHex", hex);

  }

  /**
   * Converts a binary value held in a byte array into a hex string.
   *
   * @param bin    The byte array containing the binary value.
   * @return String The hex string
   */
  public static String binToHex(byte[] bin)
  {
    StringBuffer hex = new StringBuffer();
    binToHex(bin,0,bin.length,hex);
    return hex.toString();
  }
  

  /**
   * Converts a hex string into a byte array holding the binary value.
   * This code is reused from MA88.
   *
   * @param hex    The String containing the hex data (2 characters per byte)
   * @param start  The int offset into the string (in characters) where the hex data starts
   *
   * @return       The resulting byte array.
   *
   * @throws       IllegalArgumentException if the specified characters don't form a valid hex string.
   */
  public static byte[] hexToBin(String hex, int start) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "hexToBin", new Object[]{hex, Integer.valueOf(start)});
    int digit1, digit2;
    int length = (hex.length() - start); // no of characters to be processed

    // Handle the special case when there is no data
    if (length == 0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "hexToBin");
        return new byte[0];
    }

    // It's an error if we have an odd number of hex characters
    if ((length < 0) || ((length % 2) != 0)) {
      String nlsMsg = nls.getFormattedMessage("BAD_HEX_STRING_CWSIU0200"
                                             ,new Object[]{hex}
                                             ,"The hexadecimal string " + hex + " is incorrectly formatted."
                                             );
      IllegalArgumentException e = new IllegalArgumentException(nlsMsg);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "hexToBin", e);
      throw e;
    }

    // Change length from number of characters to number of bytes to be produced
    length /= 2;

    // Allocate a buffer to hold the result
    byte[] retval = new byte[length];

    // Convert from HexString to a byte array
    for (int i = 0; i < length; i++) {
      digit1 = (Character.digit(hex.charAt(2 * i + start), 16)) << 4;
      digit2 = Character.digit(hex.charAt(2 * i + start + 1), 16);
      // The Character.digit() method signals an error by returning -1, so we have
      // to test for it here
      if ((digit1 < 0) || (digit2 < 0)) {
        String nlsMsg = nls.getFormattedMessage("BAD_HEX_STRING_CWSIF0200"
                                                ,new Object[]{hex}
                                                ,"The hexadecimal string " + hex + " is incorrectly formatted."
                                                );
        IllegalArgumentException e = new IllegalArgumentException(nlsMsg);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "hexToBin", e);
        throw e;
      }

      retval[i] = (byte) (digit1 + digit2);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "hexToBin");

    return retval;
  }

}
