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
 * This class contains static methods for storing and retrieving 2, 4, and 8 byte
 * integers at arbitrary positions in byte arrays.
 */

public final class ArrayUtil {
  private static TraceComponent tc = SibTr.register(ArrayUtil.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  //static methods only
  private ArrayUtil() {
  }

  /**
   * Number of bytes needed to encode the various data types
   */
  public final static int BYTE_SIZE  = 1;
  public final static int SHORT_SIZE = 2;
  public final static int INT_SIZE   = 4;
  public final static int LONG_SIZE  = 8;

  /**
   * Unserializes a long from a byte array at a specific offset in big-endian order
   *
   * @param b byte array from which to read a long value.
   * @param offset offset within byte array to start reading.
   * @return long read from byte array.
   */
  public static long readLong(byte b[], int offset) {
    long retValue;

    retValue = ((long)b[offset++]) << 56;
    retValue |= ((long)b[offset++] & 0xff) << 48;
    retValue |= ((long)b[offset++] & 0xff) << 40;
    retValue |= ((long)b[offset++] & 0xff) << 32;
    retValue |= ((long)b[offset++] & 0xff) << 24;
    retValue |= ((long)b[offset++] & 0xff) << 16;
    retValue |= ((long)b[offset++] & 0xff) << 8;
    retValue |= (long)b[offset] & 0xff;

    return retValue;
  }

  /**
   * Serializes a long into a byte array at a specific offset in big-endian order
   *
   * @param b byte array in which to write a long value.
   * @param offset offset within byte array to start writing.
   * @param value long to write to byte array.
   */
  public static void writeLong(byte b[], int offset, long value) {
    b[offset++] = (byte) (value >>> 56);
    b[offset++] = (byte) (value >>> 48);
    b[offset++] = (byte) (value >>> 40);
    b[offset++] = (byte) (value >>> 32);
    b[offset++] = (byte) (value >>> 24);
    b[offset++] = (byte) (value >>> 16);
    b[offset++] = (byte) (value >>> 8);
    b[offset] = (byte)value;
  }

  /**
   * Unserializes an int from a byte array at a specific offset in big-endian order
   *
   * @param b byte array from which to read an int value.
   * @param offset offset within byte array to start reading.
   * @return int read from byte array.
   */
  public static int readInt(byte b[], int offset) {
    int retValue;

    retValue = ((int)b[offset++]) << 24;
    retValue |= ((int)b[offset++] & 0xff) << 16;
    retValue |= ((int)b[offset++] & 0xff) << 8;
    retValue |= (int)b[offset] & 0xff;

    return retValue;
  }

  /**
   * Serializes an int into a byte array at a specific offset in big-endian order
   *
   * @param b byte array in which to write an int value.
   * @param offset offset within byte array to start writing.
   * @param value int to write to byte array.
   */
  public static void writeInt(byte[] b, int offset, int value) {
    b[offset++] = (byte) (value >>> 24);
    b[offset++] = (byte) (value >>> 16);
    b[offset++] = (byte) (value >>> 8);
    b[offset] = (byte)value;
  }

  /**
   * Unserializes a short from a byte array at a specific offset in big-endian order
   *
   * @param b byte array from which to read a short value.
   * @param offset offset within byte array to start reading.
   * @return short read from byte array.
   */
  public static short readShort(byte b[], int offset) {
    int retValue;

    retValue = b[offset++] << 8;
    retValue |= b[offset] & 0xff;

    return (short)retValue;
  }

  /**
   * Serializes a short into a byte array at a specific offset in big-endian order
   *
   * @param b byte array in which to write a short value.
   * @param offset offset within byte array to start writing.
   * @param value short to write to byte array.
   */
  public static void writeShort(byte b[], int offset, short value) {
    b[offset++] = (byte) (value >>> 8);
    b[offset] = (byte)value;
  }

  /**
   * Unserializes a byte[] from a byte array at a specific offset
   *
   * @param b byte array from which to read a byte[].
   * @param offset offset within byte array to start reading.
   * @param length length of byte[] to read
   * @return byte[] read from byte array.
   */
  public static byte[] readBytes(byte b[], int offset, int length) {
    byte[] retValue = new byte[length];
    System.arraycopy(b, offset, retValue, 0, length);
    return retValue;
  }

  /**
   * Serializes a byte[] into a byte array at a specific offset
   *
   * @param b byte array in which to write a byte[] value.
   * @param offset offset within byte array to start writing.
   * @param value byte[] to write to byte array.
   */
  public static void writeBytes(byte b[], int offset, byte[] value) {
    System.arraycopy(value, 0, b, offset, value.length);
  }
}
