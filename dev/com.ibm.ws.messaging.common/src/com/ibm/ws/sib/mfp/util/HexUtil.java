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

package com.ibm.ws.sib.mfp.util;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * The HexUtil class contains methods for converting byte sequences to simple Strings or to
 * Strings containing formatted hex "dumps"
 */

public final class HexUtil
{
  private static TraceComponent tc = SibTr.register(HexUtil.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private HexUtil() {} // May not be instantiated

  private static final char[] digits = "0123456789ABCDEF".toCharArray();

  /**
   * Convert an entire int[] frame to a byte[] frame and dump it in hex.  The conversion
   * is done in big-endian.
   *
   * @param frame the int[] frame to dump.
   */
  public static String toString(int[] frame) {
    return toString(frame, 0, frame.length);
  }

  /**
   * Convert an int[] frame to a byte[] frame and dump it in hex.  The conversion is done
   * in big-endian so that the most significant byte appears first in the string dump.
   *
   * @param frame the int[] frame to dump.
   * @param offset The index of the first int in the array to dump.
   * @param length The number of integers to dump.
   */
  public static String toString(int[] frame, int offset, int length) {
    byte[] temp = new byte[length * 4];
    int i, j;
    for(i=j=0; i<length; i++,j+=4) {
      temp[j]   = (byte) (frame[i] >>> 24);
      temp[j+1] = (byte) (frame[i] >>> 16);
      temp[j+2] = (byte) (frame[i] >>> 8);
      temp[j+3] = (byte) (frame[i]);
    }
    return toString(temp, 0, length * 4);
  }

  /**
   * Returns a string of hexadecimal digits from a sequence of bytes
   *
   * @param frame the array in which the bytes are to be found
   * @param offset the offset of the first byte to convert to hex
   * @param length the number of bytes to convert
   */
  public static String toString(byte[] frame, int offset, int length) {
    if (frame == null) return null;
    StringBuffer buf = new StringBuffer();
    int limit = offset + length;
    while (offset < limit) {
      buf.append(digits[(frame[offset] >>> 4) & 0x0f]);
      buf.append(digits[frame[offset++] & 0x0f]);
    }
    return buf.toString();
  }

  /**
   * Returns a string of hexadecimal digits from a byte array
   */
  public static String toString(byte[] array) {
    return toString(array, 0, array.length);
  }

  /**
   * Create a formatted "dump" of a sequence of bytes
   *
   * @param  frame    the byte array containing the bytes to be formatted
   * @param  offset   the offset of the first byte to format
   * @param  length   the number of bytes to format
   * @return a String containing the formatted dump.
   */
  public static String dumpString(byte[] frame, int offset, int length) {
    return dumpString(frame, offset, length, false);
  }

  /**
   * Create a formatted "dump" of a sequence of bytes with ascii translation, if specified.
   * Performs simple ascii translation. No DBCS translations.
   *
   * @param  frame    the byte array containing the bytes to be formatted
   * @param  offset   the offset of the first byte to format
   * @param  length   the number of bytes to format
   * @param  ascii    if true, include ascii translation
   * @return a String containing the formatted dump.
   */
  public static String dumpString(byte[] frame, int offset, int length, boolean ascii) {
    if ((frame == null)|| (length == 0)) return null;
    // Main formatting is performed in buf. asciibuf is used to hold the
    // ascii translation. asciibuf is appended to buf before a new line is started
    StringBuffer buf = new StringBuffer();
    StringBuffer asciibuf = new StringBuffer();
    buf.append("Length=").append(length);
    for (int i = 0; i < length; i++) {
      if (i%32 == 0) {
        if (ascii) {
          buf.append(asciibuf);
          asciibuf.setLength(0);
          asciibuf.append("\n").append(pad(0)).append("   ");
        }
        buf.append("\n").append(pad(offset+i)).append(offset+i).append(": ");
      }
      else if (i%16 == 0) {
        buf.append("  ");
        if (ascii) asciibuf.append("  ");
      }
      else if (i%4 == 0) {
        buf.append(" ");
        if (ascii) asciibuf.append(" ");
      }
      buf.append(digits[(frame[offset+i] >>> 4) & 0x0f]);
      buf.append(digits[frame[offset+i] & 0x0f]);
      if (ascii) {
        if (frame[offset+i]>=0x20 && ((frame[offset+i] & 0x80) == 0)) {
          asciibuf.append(' ').append((char)frame[offset+i]);
        }
        else {
          asciibuf.append(" .");
        }
      }
    }
    if (ascii) buf.append(asciibuf);
    return buf.toString();
  }

  static String pad(int i) {
    if (i > 999)
      return "";
    else if (i > 99)
      return " ";
    else if (i > 9)
      return "  ";
    else
      return "   ";
  }


  // A trivial int-to-hex converter
  public static void main(String[] args) {
    System.err.println(toString(new int[] { Integer.parseInt(args[0]) }));
  }
}
