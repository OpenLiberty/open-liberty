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

package com.ibm.ws.sib.jms.util;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Static class which provodes methods which encode Unicode Strings and chars into
 * UTF8 bytes.
 *
 * The UTF8 encoding scheme is defined as follows in the Unicode 4.0 Specification:
 *
 *   00000000 0xxxxxxx  => 0xxxxxxx
 *   00000yyy yyxxxxxx  => 110yyyyy 10xxxxxx
 *   zzzzyyyy yyxxxxxx  => 1110zzzz 10yyyyyy 10xxxxxx
 *   000uuuuu zzzzyyyy yyxxxxxx  => 11110uuu 10uuzzzz 10yyyyyy 10xxxxxx
 *
 *   The last form can be ignored by this encoder as we don't need it for
 *   encoding a a java.lang.String or char.
 *
 *   Note that D800-DFFF are not valid individual characters, but are valid if
 *   grouped appropriately for double-byte UTF16 characters.
 *   We can ignore this constraint and just encode the character individually, as:
 *     a) Such a character shouldn't manage to appear in the wrong place in a java.lang.String
 *     b) Although the Sun encoder encodes them to 0 bytes if found in the wrong place, the
 *        Sun decoder will happily ignore them if it finds them in an incoming byte array.
 *     c) It seems a better idea to encode, and potentially decode, exactly what we are given.
 *
 */
public class UTF8Encoder
{

  private static com.ibm.websphere.ras.TraceComponent tc = SibTr.register(UTF8Encoder.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private final static char ONE_BYTE_ONLY_MASK  = 0xff80;        // 11111111 10000000
  private final static char TWO_BYTES_ONLY_MASK = 0xf800;        // 11111000 00000000

  private final static char FIRST_OF_TWO_MASK   = 0x07c0;        // 00000111 11000000
  private final static int  FIRST_OF_TWO_SHIFT  = 6;             // Need to shift the masked bits 6 to the right, to get them into the bottom byte
  private final static byte FIRST_OF_TWO_TOP    = (byte)0xc0;    // 11000000  (now we've got the individual byte)

  private final static char LAST_BYTE_MASK      = 0x003f;        // 00000000 00111111
  private final static char LOWER_BYTE_TOP      = 0x0080;        // 00000000 10000000

  private final static char FIRST_OF_THREE_MASK   = 0xf000;      // 11110000 00000000
  private final static int  FIRST_OF_THREE_SHIFT  = 12;          // Need to shift the masked bits 12 to the right, to get them in the bottom byte
  private final static byte FIRST_OF_THREE_TOP    = (byte)0xe0;  // 11100000  (now we've got the individual byte)

  private final static char SECOND_OF_THREE_MASK  = 0x0fc0;      // 00001111 11000000
  private final static int  SECOND_OF_THREE_SHIFT  = 6;          // Need to shift the masked bits 6 to the right, to get them into the bottom byte


  /**
   * Calculate the number of bytes needed to UTF8 encode a String
   *
   * @param s    The String whose encoded length is wanted
   *
   * @return int The encoded length of the given String
   */
  public final static int getEncodedLength(String s) {

    int count = 0;
    int strLength = s.length();
    for (int i = 0; i < strLength; i++) {
      count = count + getEncodedLength(s.charAt(i));
    }
    return count;
  }


  /**
   * Encode a String into the given offset into a byte array, & return the new offset
   *
   * Note that:
   *  1. It is the caller's responsibility to provide a byte array long enough to
   *     contain the entire encoded String. There is no checking of the length prior
   *     to encoding, so ArrayIndexOutOfBoundsException will be thrown if the encode
   *     falls off the end of the byte array.
   *  2. Any values already in the relevant portion of the byte array will be lost.
   *
   * @param buff      The byte array into which the char should be encoded
   * @param offset    The offset into buff at which the char should be encoded
   * @param s         The String to be encoded.
   *
   * @return int      The number of bytes written to the byte array.
   */
  public final static int encode(byte[] buff, int offset, String s) {

    int strLength = s.length();
    int start = offset;
    for (int i = 0; i < strLength; i++) {
      offset = encode(buff, offset, s.charAt(i));
    }
    return (offset - start);
  }


  /**
   * Encode a String to UTF8 and return the resulting bytes
   *
   * @param s       The String to be encoded
   *
   * @return byte[] The UTF8 encoding of the given String
   */
  public final static byte[] encode(String s) {

    byte[] bytes = new byte[getEncodedLength(s)];
    int offset = 0;
    int strLength = s.length();

    for (int i = 0; i < strLength; i++) {
      offset = encode(bytes, offset, s.charAt(i));
    }
    return bytes;
  }


  /**
   * Calculate the number of bytes needed to UTF8 encode a single character.
   * This method is only public so it can be Unit Tested specifically.
   *
   * @param c    The char whose encoded length is wanted
   *
   * @return int The encoded length of the given char
   */
  private final static int getEncodedLength(char c) {

    if ((c & ONE_BYTE_ONLY_MASK) == 0) {
      return 1;
    }
    if ((c & TWO_BYTES_ONLY_MASK) == 0) {
      return 2;
    }
    return 3;
  }


  /**
   * Encode a char into the given offset into a byte array, & return the new offset
   *
   * Note that:
   *  1. It is the caller's responsibility to provide a byte array long enough to
   *     contain the encoded char. There is no checking of the length prior
   *     to encoding, so ArrayIndexOutOfBoundsException will be thrown if the encode
   *     falls off the end of the byte array.
   *  2. Any values already in the relevant portion of the byte array will be lost.
   *
   * @param buff      The byte array into which the char should be encoded
   * @param offset    The offset into buff at which the char should be encoded
   * @param c         The char to be encoded.
   *
   * @return int      The position in buff of the first byte after the encoded char.
   */
  private final static int encode(byte[] buff, int offset, char c) {

    char tempC;
    byte tempB;

    // Single byte result
    if ((c & ONE_BYTE_ONLY_MASK) == 0) {
      buff[offset] = (byte)c;
    }

    // Two byte result
    else if ((c & TWO_BYTES_ONLY_MASK) == 0) {

      tempC = (char)((c & FIRST_OF_TWO_MASK) >> FIRST_OF_TWO_SHIFT);
      tempB = (byte)(((byte)tempC) + FIRST_OF_TWO_TOP);
      buff[offset] = tempB;
      offset++;
      buff[offset] = (byte)((c & LAST_BYTE_MASK) + LOWER_BYTE_TOP);
    }

    // Three byte result
    else {
      tempC = (char)((c & FIRST_OF_THREE_MASK) >> FIRST_OF_THREE_SHIFT);
      tempB = (byte)(((byte)tempC) + FIRST_OF_THREE_TOP);
      buff[offset] = tempB;
      offset++;

      tempC = (char)((c & SECOND_OF_THREE_MASK) >> SECOND_OF_THREE_SHIFT);
      tempB = (byte)(((byte)tempC) + LOWER_BYTE_TOP);
      buff[offset] = tempB;
      offset++;

      buff[offset] = (byte)((c & LAST_BYTE_MASK) + LOWER_BYTE_TOP);
    }

    return (offset+1);
  }

}
