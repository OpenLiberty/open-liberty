/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.utils;

/**
 * This class has methods which return a String representation of the CloneId passed in.
 * The cloneId is so that we can have affinity with regard to sessions
 * 
 * @ibm-private-in-use
 */
public class EncodeCloneID {

    private static final int radix = 32;

    //Tries to get string representation of the number to base 32. 
    //If unsuccessful returns the original as string
    public static String encodeLong(long val) {
        String str = null;
        try {
            str = Long.toString(val, radix);
            return str;
        } catch (Throwable th) {
            com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.utils.EncodeCloneID.encodeLong", "39", "" + val);
        }
        return Long.toString(val);
    }

    //Tries to get string representation of the number to base 32. 
    //If unsuccessful returns the original as string
    public static String encodeString(String str) {
        if (str == null)
            return str;
        if (str.equals("-1"))
            return str;
        try {
            Long lo = new Long(str);
            return encodeLong(lo.longValue());
        } catch (NumberFormatException nfe) {
            // do nothing -- this is expected on zOS, results in string being returned unchanged  242544
        } catch (Throwable th) {
            com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.session.utils.EncodeCloneID.encodeString", "56", str);
        }
        return str;
    }

}
