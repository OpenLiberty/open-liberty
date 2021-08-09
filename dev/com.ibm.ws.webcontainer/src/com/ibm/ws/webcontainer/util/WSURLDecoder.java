/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.io.UnsupportedEncodingException;


/*
 * HISTORY
 * CMVC Ref        Date          Who           Description
 * -------------- --------     --------    -------------------------------------------
 *  PK23481       04/24/06      todkap         DECODED URI PATH ELEMENTS SHOULD NOT CONSIDER "+" TO BE A       SPECIAL CHARACTER. (ONLY IN QUERY STRINGS)
 *
 *
 *
*/

public class WSURLDecoder {
	
    private static final int STATE_NORMAL_CHAR = 0;
    private static final int STATE_START_DECODE = 1;
    private static final int STATE_DECODE_FIRST_DIGIT = 2;
    private static final int STATE_DECODE_SECOND_DIGIT = 3;
    private static final int STATE_FINISH_DECODE = 4;
    
    // We can NLS these later.  These are copies of the JDK URLDecoder error
    // messages.
    
    private static final String ERROR_INCOMPLETE =
         "URLDecoder: Incomplete trailing escape (%) pattern";
    private static final String ERROR_ILLEGAL =
         "URLDecoder: Illegal hex characters in escape (%) pattern";
         
    public static String decode (String url, String encoding)
         throws UnsupportedEncodingException {
         byte buffer[];
         char srcChars[];
         int srcCharBegin = 0;
         int urlLength;
         int byteValue = 0;
         int bufferOffset = 0;
         StringBuffer result;
         int state = WSURLDecoder.STATE_NORMAL_CHAR;

         if (url.length() == 0) {
              throw new UnsupportedEncodingException();
         }
         
         srcChars = url.toCharArray();
         urlLength = srcChars.length;
         buffer = new byte[urlLength];
         result = new StringBuffer (urlLength);

         for (int i = 0; i < urlLength; ++i) {
              char ch = srcChars[i];
              
              switch (state) {
                   case WSURLDecoder.STATE_NORMAL_CHAR: {
                        if (ch == '%') {
                             state = WSURLDecoder.STATE_DECODE_FIRST_DIGIT;
                             
                             // The beginning of a byte that must be
                             // decoded has been encountered.  All
                             // characters before this character are
                             // "normal" and should be directly appended
                             // to the result now.
                   
                             result.append (srcChars, srcCharBegin, i -
                                  srcCharBegin);
                        }

                        break;
                   }

                   case WSURLDecoder.STATE_DECODE_FIRST_DIGIT: {
                        switch (ch) {
                             case '0': case '1': case '2': case '3': case '4':
                             case '5': case '6': case '7': case '8':
                             case '9': {
                                  byteValue = 16 * (ch - 48);

                                  break;
                             }

                             case 'A': case 'B': case 'C': case 'D': case 'E':
                             case 'F': {
                                  byteValue = 16 * (ch - 55);

                                  break;
                             }
                             
                             case 'a': case 'b': case 'c': case 'd': case 'e':
                             case 'f': {
                                  byteValue = 16 * (ch - 87);

                                  break;
                             }

                             default: {
                                  // Must have been an illegal character.

                                  throw new IllegalArgumentException
                                       (WSURLDecoder.ERROR_ILLEGAL);
                             }
                        }

                        // Advance to the next state.

                        state = WSURLDecoder.STATE_DECODE_SECOND_DIGIT;
                        
                        break;
                   }

                   case WSURLDecoder.STATE_DECODE_SECOND_DIGIT: {
                        // Grab the value of the second hex digit by
                        // manipulating the character value.
                        
                        switch (ch) {
                             case '0': case '1': case '2': case '3': case '4':
                             case '5': case '6': case '7': case '8':
                             case '9': {
                                  byteValue += ch - 48;

                                  break;
                             }

                             case 'A': case 'B': case 'C': case 'D': case 'E':
                             case 'F': {
                                  byteValue += ch - 55;

                                  break;
                             }
                             
                             case 'a': case 'b': case 'c': case 'd': case 'e':
                             case 'f': {
                                  byteValue += ch - 87;

                                  break;
                             }

                             default: {
                                  // Must have been an illegal character.
                                  
                                  throw new IllegalArgumentException
                                       (WSURLDecoder.ERROR_ILLEGAL);
                             }
                        }

                        // Append the decoded byte to the byte buffer.
                        
                        buffer[bufferOffset++] = (byte) byteValue;
                        
                        // Advance to the next state.

                        state = WSURLDecoder.STATE_FINISH_DECODE;
                        
                        break;
                   }

                   case WSURLDecoder.STATE_FINISH_DECODE: {
                        switch (ch) {
                             case '%': {
                                  // Another encoded character found, so
                                  // change the state.

                                  state = WSURLDecoder.STATE_DECODE_FIRST_DIGIT;

                                  break;
                             }

                             default: {
                                  // A "normal" character was found, so the
                                  // contents of the decoded byte buffer need
                                  // to be encoded into the specified encoding
                                  // and copied directly into the result
                                  // string.

                                  result.append (new String (buffer, 0,
                                       bufferOffset, encoding));
                                  
                                  bufferOffset = 0;

                                  // Update the beginning of the source array.

                                  srcCharBegin = i;
                                  
                                  // Advance the state.

                                  state = WSURLDecoder.STATE_NORMAL_CHAR;
                             }
                        }
                   }
              }
         }
         
         // See what state we ended up in, as we may be unfinished.

         switch (state) {
              case WSURLDecoder.STATE_NORMAL_CHAR: {
                   // We need to make a copy to the destination array in order
                   // to finish up.

                   result.append (srcChars, srcCharBegin, urlLength -
                        srcCharBegin);
                   
                   break;
              }

              case WSURLDecoder.STATE_START_DECODE:
              case WSURLDecoder.STATE_DECODE_FIRST_DIGIT:
              case WSURLDecoder.STATE_DECODE_SECOND_DIGIT: {
                   // This means that there is an unterminated hex string.

                   throw new IllegalArgumentException
                        (WSURLDecoder.ERROR_INCOMPLETE);
              }

              case WSURLDecoder.STATE_FINISH_DECODE: {
                   // We need to append the byte buffer to the result string.

                   result.append (new String (buffer, 0, bufferOffset,
                        encoding));
              }
         }

         // Finished, return the decoded string.
         
         return result.toString();
    }

    private static void testDecode (String url, String encoding, int reps,
         int runs) throws Exception {
         long begin = 0;
         long end = 0;
         double jdkElapsed = 0;
         double webElapsed = 0;
         java.text.DecimalFormat format = new java.text.DecimalFormat
              ("0.00");
         
         System.out.println ("Encoding url: '" + url + "', encoding: " +
              encoding);

         for (int i = 0; i < runs; ++i) {
              if (i == (runs - 1)) {
                   begin = System.currentTimeMillis();
              }
              
              for (int j = 0; j < reps; ++j) {
                   java.net.URLDecoder.decode (url, encoding);
              }

              if (i == (runs - 1)) {
                   end = System.currentTimeMillis();
              }
         }

         jdkElapsed = end - begin;
         
         System.out.println ("java.net.WSURLDecoder result: '" +
              java.net.URLDecoder.decode (url, encoding) + "'");
         System.out.println ("java.net.WSURLDecoder time, " + reps +
              " repetitions after " + runs + " runs: " + jdkElapsed + "ms\n");

         for (int i = 0; i < runs; ++i) {
              if (i == (runs - 1)) {
                   begin = System.currentTimeMillis();
              }
              
              for (int j = 0; j < reps; ++j) {
                   WSURLDecoder.decode (url, encoding);
              }

              if (i == (runs - 1)) {
                   end = System.currentTimeMillis();
              }
         }

         webElapsed = end - begin;
         
         System.out.println ("webcontainer WSURLDecoder result: '" +
              WSURLDecoder.decode (url, encoding) + "'");
         System.out.println ("webcontainer WSURLDecoder time, " + reps +
              " repetitions after " + runs + " runs: " + webElapsed + "ms");
         System.out.println ("webcontainer WSURLDecoder performance " +
              "difference: " + format.format (((jdkElapsed - webElapsed) /
              jdkElapsed) * 100) + "%\n");
    }
    
    public static void main (String args[]) throws Exception {
         WSURLDecoder.testDecode ("a", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("ab", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("abc", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("abcd", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("abcde", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("abcdef", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("this/is/a/short/url/with/no/encoded/chars", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("this/is/a/looooooooooooooooonger/url/with/no/encoded/chars", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("this/is/a/muuuuuuuuuuuuuuuuuuuuuuuuuuuuch/looooooooooooooooooooooooooooooonger/url/with/no/encoded/chars/and/some+plus+characters", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("this/is/a/short/url/with/some/%65%66%67%68%69/encoded/chars", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("this/%65%66%67%68%69/is/%65%66%67%68%69/a/%65%66%67%68%69/longer/%65%66%67%68%69/url/with/scattered/%65%66%67%68%69/encoded/chars", "UTF-8", 100000, 5);
         WSURLDecoder.testDecode ("this/%65%66%67%68%69/is/%65%66%67%68%69/a/%65%66%67%68%69/longer/%65%66%67%68%69/url/with/scattered/%65%66%67%68%69/encoded/chars/and/ends/with/encoded/chars/%65%66%67%68%69", "UTF-8", 100000, 5);
    }


}
