package com.ibm.tx.util;
/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
public class Utils
{
    /**
     * Converts a byte array to a string.
     */
    public static String toString(byte[] b) {
       StringBuffer result = new StringBuffer(b.length);
       for (int i = 0; i < b.length; i++)
          result.append((char) b[i]);
       return (result.toString());
    }

    public static byte[] byteArray(String s) {
       return byteArray(s, false);
    }
    
    public static byte[] byteArray(String s, boolean keepBothBytes) {
       byte[] result = new byte[s.length() * (keepBothBytes ? 2 : 1)];
       for (int i = 0; i < result.length; i++)
          result[i] = keepBothBytes ? (byte) (s.charAt(i / 2) >> (i & 1) * 8) : (byte) (s.charAt(i));
       return result;
    }
}