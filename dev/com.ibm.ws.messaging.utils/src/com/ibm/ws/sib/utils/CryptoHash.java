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
 * The CryptoHash calculator uses SHA-1 to provide a strong cryptographic hash of byte
 * arrays.  It adds syntactic sugar for returning hashcodes as a long and as byte arrays
 * of length less than or equal to 20.  This code is verbatim (after package name change)
 * what is in com.ibm.disthub.impl.util.CryptoHash, which was, in turn, based on
 * CryptoLite code from the IBM Zurich security team.
 **/

public final class CryptoHash {
  /**
   * Returns the first eight bytes of the SHA-1 hash code of the
   * input data as a long.
   *
   * @param data the byte array to be hashed
   *
   * @return the first long bytes of the hash code as a long
   **/
  public static long hash(byte[] data) {
    int[] code = calculate(data);
    return (((long) code[0]) << 32) | (((long) code[1]) & 0xFFFFFFFFL);
  }

  /**
   * Calculates the SHA-1 hash code of the input data and writes up to
   * 20 bytes of it in the output byte array parameter.
   *
   * @param data the byte array to be hashed
   * @param output output parameter for the calculated hash code
   **/
  public static void hash(byte[] data, byte[] output) {
    int len = output.length;
    if (len == 0)
      return;
    int[] code = calculate(data);
    for (int outIndex = 0, codeIndex = 0, shift = 24;
            outIndex < len && codeIndex < 5;
                ++outIndex, shift -= 8) {
      output[outIndex] = (byte) (code[codeIndex] >>> shift);
      if (shift == 0) {
        shift = 32;     // will be decremented by 8 before next iteration
        ++codeIndex;
      }
    }
  }

  /**
   * Calculate a SHA-1 hash code.
   *
   * @param data the byte array to be hashed
   *
   * @return the hash code as an array of 5 int's
   **/
  private static int[] calculate(byte[] data) {
    int A = 0x67452301;
    int B = 0xEFCDAB89;
    int C = 0x98BADCFE;
    int D = 0x10325476;
    int E = 0xC3D2E1F0;

    int[] W = new int[80]; // working buffer
    int len = data.length;
    int off = 0;
    int   X, i, n = len/4;

    boolean done = false;
    boolean padded = false;

    while( !done ) {
      // Fill W array
      for(i=0; (i<16) && (n > 0); n--, off+=4)
        W[i++] = (data[off+3] & 0xFF) | ((data[off+2] & 0xFF) << 8) |
          ((data[off+1] & 0xFF) << 16) | ((data[off] & 0xFF) << 24);

      if (i < 16) {             // pad
        if( !padded ) {         // first time past end of data
          X=len%4;
          int pad1 = 0;
          for (int j = 0; j < X; ++j) {
            pad1 |= ((int) (data[off + j] & 0xFF)) << (24 - 8 * j);
          }
          W[i++] = pad1 | (1 << (31 - X * 8));
//            W[i++] = (X!=0)?(((int)msbf(data,off,X)<<((4-X)*8))|(1<<(31-X*8))):(1<<31);
          if (i==15) W[15]=0;   // 64 bit length of data as bits can't fit
          padded = true;        // don't pad again if there is another iteration
        }
        if (i <= 14) {          // last iteration
          while( i<14 ) W[i++] = 0;
          // bit count = len*8
          W[14] = len >>> 29;
          W[15] = len << 3;
          done = true;
        }
        i = 16;
      }

      do {
        W[i] = ((X = W[i-3] ^ W[i-8] ^ W[i-14] ^ W[i-16]) << 1) | (X >>> 31);
      } while(++i<80);

      // Transform
      int A0 = A;
      int B0 = B;
      int C0 = C;
      int D0 = D;
      int E0 = E;
      i = 0;
      do { X=((A<<5)|(A>>>27))+((B&C)|(~B&D))+E+W[i]+0x5A827999;
      E=D;D=C;C=(B<<30)|(B>>>2);B=A;A=X; } while(++i<20);
      do { X=((A<<5)|(A>>>27))+(B^C^D)+E+W[i]+0x6ED9EBA1;
      E=D;D=C;C=(B<<30)|(B>>>2);B=A;A=X; } while(++i<40);
      do { X=((A<<5)|(A>>>27))+((B&C)|(B&D)|(C&D))+E+W[i]+0x8F1BBCDC;
      E=D;D=C;C=(B<<30)|(B>>>2);B=A;A=X; } while(++i<60);
      do { X=((A<<5)|(A>>>27))+(B^C^D)+E+W[i]+0xCA62C1D6;
      E=D;D=C;C=(B<<30)|(B>>>2);B=A;A=X; } while(++i<80);
      A += A0;
      B += B0;
      C += C0;
      D += D0;
      E += E0;
    }

    int[] result = {A, B, C, D, E};
    return result;
  }

}
