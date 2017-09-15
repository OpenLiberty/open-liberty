package com.ibm.ws.sib.utils;
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

public class Base64Utils
{
    public static final char HEADER = '!';

    private static final String digits  = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789*-";
    private static final String padding = "=";

    public static String encodeBase64(byte[] data)
    {   
        byte oneA, oneB, twoA, twoB, threeA = 0;

        StringBuilder retval = new StringBuilder(data.length*2);

        // Add the header to the front of the encoded string so that we
        // can tell this is Base64 encoded data.
        retval.append(HEADER);

        for (int i=0; i<data.length; i=i+3)
        {
            // Encode data
            // 
            // !WARNING! - The byte manipulation operators in Java return at 
            //             the least an int so even if you are working on two 
            //             bytes you will need to take this into account. How
            //             I have got around this is by masking the result so
            //             that I only get the 6 bits that I care about.
            // 
            // 
            // 1st digit = 1st 6 bits of 1st byte
            //             10101011
            //             ^----^
            // 
            //             byte 1 >>> 2 = 00101010
            retval.append(digits.charAt((byte)((data[i] >>> 2) & 0x3f)));
    

            if (i+1 < data.length)
            {
                // 2nd digit = last 2 bits of 1st byte and
                //             first 4 bits of 2nd byte
                //             10101011  01101010
                //                   ^------^
                // 
                //             byte 1 & 00000011 = 00000011 = byte 1'
                // 
                //             byte 1' << 4      = 00110000 = byte 1''
                // 
                //             byte 2  >>> 4     = 00000110 = byte 2'
                // 
                //             byte 1'' | byte 2' = 00110110
                oneA = (byte)(data[i] & 0x03);
                oneB = (byte)(oneA << 4);
                twoA = (byte)((data[i+1] >>> 4) & 0x0f);
                retval.append(digits.charAt((byte)((oneB | twoA) & 0x3f)));

                if (i+2 < data.length)
                {
                    // 3rd digit = last 4 bits of 2nd byte and
                    //             first 2 bits of 3rd byte
                    //             01101010  11000101
                    //                 ^------^
                    // 
                    //             byte 2 & 00001111 = 00001010 = byte 2'
                    // 
                    //             byte 2' << 2      = 00101000 = byte 2''
                    //                             
                    //             byte 3 >>> 6      = 00000011 = byte 3'
                    // 
                    //             byte 2'' | byte 3' = 00101011
                    twoA   = (byte)(data[i+1] & 0x0f);              // Mask off first four bits from byte 2
                    twoB   = (byte)(twoA << 2);                     // Shift left 2 to make space for byte 3 bits
                    threeA = (byte)((data[i+2] >>> 6) & 0x03);      // Shift firt 2 bits of byte 3 right and mask
                    retval.append(digits.charAt((byte)((twoB | threeA) & 0x3f)));

            
                    // 4th digit = last 6 bits of 3rd byte
                    //             11000101
                    //               ^----^
                    // 
                    //             byte 3 & 00111111 = 00000101
                    retval.append(digits.charAt((byte)(data[i+2] & 0x3f)));
                }
                else
                {
                    // last 4 bits of 2nd byte still to encode
                    // then pad to the end.
                    twoA = (byte)(data[i+1] & 0x0f);
                    twoB = (byte)(twoA << 2);
                    retval.append(digits.charAt((byte)(twoB & 0x3f)));
                    retval.append(padding);
                }
            }
            else
            {
                // last 2 bits of the 1st byte still to encode
                // then pad to the end.
                oneA = (byte)(data[i] & 0x03);
                retval.append(digits.charAt((byte)((oneA << 4) & 0x3f)));
                retval.append(padding);
                retval.append(padding);
            }
        }

        return retval.toString();
    }

    public static byte[] decodeBase64(String data)
    {
        byte oneA, twoA, threeA, fourA = 0;

        // Work out the number of padding characters 
        // we have on the end of the data.
        int paddingCount = data.length() - data.indexOf("=");
        int length;

        // NOTE: When calculating the length we need to remember
        //       to remove the header character from the length 
        //       of the data.
        if (paddingCount < 3)
        {
            length = (((data.length() - 1) / 4) * 3) - paddingCount;
        }
        else
        {
            length = ((data.length() - 1) / 4) * 3;
        }

        byte[] retval = new byte[length];
        int  position = 0;

        // Start at 1 here so that we skip the header character.
        for (int i=1; i<data.length(); i=i+4)
        {
            // NOTE - When converting from digit positions to
            //        bytes we are only manipulating the least
            //        significant 6 bits in the int as this is
            //        the range of values we are using.
            //
            // 1st byte = all 6 bits of 1st digit followed by
            //            the first 2 bits of the 2nd digit
            //            00011010 00101010
            //              ^---------^
            // 
            //            digit 1 << 2        = 01101000 = digit 1'
            // 
            //            digit 2 >>> 4       = 00000010 = digit 2'
            // 
            //            digit 1' | digit 2' = 01101010
            oneA = (byte)((digits.indexOf(data.charAt(i)) << 2) & 0xff);
            twoA = (byte)((digits.indexOf(data.charAt(i+1)) >>> 4) & 0x03);
            retval[position] = (byte)(oneA | twoA);
            position++;

            // If the last two characters are padding then 
            // there was only 1 byte originally so we are done.
            if (data.charAt(i+2) != '=')
            {
                // 2nd byte = last 4 bits of the 2nd digit
                //            followed by the first 4 bits
                //            of the 3rd digit.
                //            00101010 00111101
                //                ^---------^ 
                // 
                //            digit 2 << 4        = 10100000 = digit 2'
                // 
                //            digit 3 >>> 2       = 00001111 = digit 3'
                // 
                //            digit 2' | digit 3' = 10101111
                twoA   = (byte)((digits.indexOf(data.charAt(i+1)) << 4) & 0xff);
                threeA = (byte)((digits.indexOf(data.charAt(i+2)) >>> 2) & 0x0f);
                retval[position] = (byte)(twoA | threeA);
                position++;

                if (data.charAt(i+3) != '=')
                {
                    // 3rd byte = last 2 bits of the 3rd digit
                    //            followed by all 6 bits of the
                    //            4th digit.
                    //            00111101 00001010
                    //                  ^---------^
                    //  
                    //            digit 3 << 6       = 01000000 = digit 3'
                    // 
                    //            digit 3' | digit 4 = 01001010
                    threeA = (byte)((digits.indexOf(data.charAt(i+2)) << 6) & 0xff);
                    fourA  = (byte)(digits.indexOf(data.charAt(i+3)) & 0x3f);
                    retval[position] = (byte)(threeA | fourA);
                    position++;
                }
            }
        }

        return retval;
    }
}
