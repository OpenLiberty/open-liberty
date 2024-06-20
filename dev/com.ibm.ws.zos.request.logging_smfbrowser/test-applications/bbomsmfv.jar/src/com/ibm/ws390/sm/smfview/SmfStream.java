/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.sm.smfview;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

/**
 * The stream from which SMF records are read. Data from the file is placed into
 * a byte array and given to the constructor for this class.  Parsers then 
 * read data from the stream. 
 * 
 */
public class SmfStream extends ByteArrayInputStream {

  // 002@L2A, @P2A, @P3A
  private final static String s_validSpecialChars = new String("-+=:;,.()^!'$%&/?`{[]}*~#'_\\<>"); //@P4C
  
  /**
   * Constructor.  Calls ByteArrayInputStream constructor
   * @param aBuffer An SMF record in a byte array
   */
  public SmfStream(byte[] aBuffer) {
    
    super(aBuffer);
    
  } // SmfStream.SmfStream(...
  
  //--------------------------------------------------------------------------------
  /** Returns a Java int from aByteN(parameter) bytes from the SmfStream. 		//@P4C
  * @param aByteN The number of bytes from the SmfStream to be converted to an int.	//@P4A
  * @return an int decoded from aByteN(parameter) bytes from the SmfStream. 		//@P4C
  */
  public int getInteger(int aByteN) {
        
    // return value
    int r = 0;
    
    // remember if the binary representation of the integer in "buf[]" is
    // in twos complement (representation of negative values)
    //System.out.println(">>> pos = " + pos);
    //System.out.println(">>> buf.length = " + buf.length);
    int sign = (buf[pos] < 0) ? -1 : 1;
    
    // compute the integer value by adding (two complemented, if "negative" is
    // true) bytes, that are raised to the appropriate power, to the integer
    // value
    int posN = pos + aByteN;
    for (int x = pos; x < posN; ++x) {
      int b = buf[x];                   // get byte
      if (sign < 0) b = ~b;             // reverse bits
      if (b < 0) b = b + 256;           // correct sign
      r = (r << 8) + b;                 // shift old value and add byte
    }
    
    if (sign < 0) ++r;                  // add one for two's complement

    skip(aByteN);                       // bump buf
    
    return r * sign;                    // apply sign

  } // SmfStream.getInteger(int aByteN)	  						//@P4C
  
  //----------------------------------------------------------------------------
  /** Returns a copy string, decoded as specified, of specified size.
   * @param aStringS Requested string size.
   * @param anEncoding Requested encoding.
   * @return copy of string, decoded as specified, of specified size.
   * @throws UnsupportedEncodingException when anEncoding is unknown.
   */
  public String getString(int aStringS,String anEncoding)
  throws UnsupportedEncodingException {
    
    if (anEncoding == null) anEncoding = SmfUtil.EBCDIC;
    
    String s = null;
    
    s = new String(buf,pos,aStringS,anEncoding);
    
    skip(aStringS);
    
    int stringL = s.length();                                                  //@P0C
    for (int cX=0; cX < stringL; ++cX) {                                       //@P0C
      char c = s.charAt(cX);
      if (Character.isDefined(c)) {
        if (   Character.isJavaIdentifierPart(c)
        || Character.isWhitespace(c) // accept whitespace whithin the string
        || (s_validSpecialChars.indexOf(c) >= 0)) {                            //    @L2C
          continue;
        }
      }
      stringL = cX;
      break;
    }
    
    if (stringL < 1) return new String();
    
    // remove leading/trailing whitespace
    return s.substring(0,stringL).trim();
  } // SmfStream.getString(...)
  
  //----------------------------------------------------------------------------
  /** Returns a copied byte buffer of the specified size.
   * @param aNumBytes Number of bytes requested.
   * @return copied byte buffer of the specified size.
   */
  public byte[] getByteBuffer(int aNumBytes) {
    
    byte buffer[] = new byte[aNumBytes];
    
    read(buffer, 0, aNumBytes);
    
    return buffer;
    
  } // SmfStream.getByteBuffer(...)
  
  //----------------------------------------------------------------------------
  /** Retzrns the size of the SmfStream.
   * @return size of SmfStream
   */
  public int size() {
    return count;
  } // SmfStream.size()

  //----------------------------------------------------------------------------
  /** Returns a long from 8 bytes of the buffer.
   * @return long.
   */
  public long getLong(){ // @MD17014 3 A
      return (((getInteger(4) & 0xFFFFFFFFL)) << 32) + (getInteger(4) & 0xFFFFFFFFL);
  } //SmfStream.getLong()
  
} // SmfStream