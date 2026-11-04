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

//------------------------------------------------------------------------------
/** Various utilities and constants. */
public final class SmfUtil {

    /** Managed Object Framework server type enumeration value. */
    public final static int MofwServerType = 0;

    /** J2ee server type enumeration value. */
    public final static int J2eeServerType = 1;

    /** EBCDIC encoding id. */
    public final static String EBCDIC = new String("Cp1047");

    /** ASCII encoding id. */
    public final static String ASCII = new String("Cp850");

    /** UNICODE encoding id. */
    public final static String UNICODE = new String("UTF-16BE");

    /** Auxiliary array with character nulls. */
    private final static char[] s_nulls = { '0', '0', '0', '0', '0', '0', '0', '0' };

    //----------------------------------------------------------------------------
    /**
     * Checks a bit as spcified.
     * 
     * @param aWord Word wherein to check for aBit
     * @param aBit  Bit to check for.
     * @return Value of the requested bit.
     */
    public static boolean checkBit(int aWord, int aBit) {

        return (aWord & aBit) == aBit;

    } // SmfUtil.checkBit()

    //----------------------------------------------------------------------------
    /**
     * Returns a String representation of the given byte array.
     * 
     * @param aByteArray Byte array to convert.
     * @param aStartX    Start index.
     * @param aLength    Number of bytes to show.
     * @return String representation of the given byte array.
     */
    public static String hexString(byte[] aByteArray, int aStartX, int aLength) {
        int len = aByteArray.length;
        if (len == 0)
            return "";
        if (aStartX >= len)
            return "";
        if (aStartX < 0)
            aStartX = 0;
        if (aLength <= 0)
            aLength = len;
        if ((aStartX + aLength) > len)
            aLength = len - aStartX;
        StringBuffer str = new StringBuffer(aLength * 2);
        int xEnd = aStartX + aLength;
        for (int x = aStartX; x < xEnd; ++x) {
            int i = byteToUnsignedInt(aByteArray[x]);
            if (i < 16) {
                str.append("0").append(Integer.toHexString(i));
            } else {
                str.append(Integer.toHexString(i));
            }
        } // for
        return str.toString();
    } // SmfUtil.hexString(...)

    //----------------------------------------------------------------------------
    /**
     * Returns a String representation of the given int array.
     * 
     * @param anIntArray Array of int to convert.
     * @return String representation of the given byte array.
     */
    public static String hexString(int[] anIntArray) {
        StringBuffer str = new StringBuffer(anIntArray.length * 8);
        for (int x = 0; x < anIntArray.length; ++x) {
            StringBuffer part = new StringBuffer(Integer.toHexString(anIntArray[x]));
            int len = part.length();
            if (len < 8) {
                part.insert(0, s_nulls, 0, 8 - len);
            }
            str.append(part);
        } // for
        return str.toString();
    } // SmfUtil.hexString(...)

    //----------------------------------------------------------------------------
    /**
     * Translates a byte to an unsigned int.
     * 
     * @param byteVal Byte to translate.
     * @return Int representation of byteVal.
     */
    public static int byteToUnsignedInt(byte byteVal) {

        int intVal = new Byte(byteVal).intValue();

        if (intVal > 0) {
            return intVal;
        } else {
            return ((intVal ^ -128) + 128);
        }

    } // SmfUtil.byteToUnsignedInt(...)

} // SmfUtil