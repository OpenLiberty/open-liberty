package com.ibm.ws.Transaction.JTA;
/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/** 
 * Util class for use with JTA TM implementation.
 */
public final class Util 
{

    private static TraceComponent tc = Tr.register(com.ibm.ws.Transaction.JTA.Util.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);


    /**
     * Convert JTA transaction status to String representation
     */
    public static String printStatus(int status)
    {
        switch (status)
        {
        case Status.STATUS_ACTIVE:
            return "Status.STATUS_ACTIVE";
        case Status.STATUS_COMMITTED:
            return "Status.STATUS_COMMITTED";
        case Status.STATUS_COMMITTING:
            return "Status.STATUS_COMMITTING";
        case Status.STATUS_MARKED_ROLLBACK:
            return "Status.STATUS_MARKED_ROLLBACK";
        case Status.STATUS_NO_TRANSACTION:
            return "Status.STATUS_NO_TRANSACTION";
        case Status.STATUS_PREPARED:
            return "Status.STATUS_PREPARED";
        case Status.STATUS_PREPARING:
            return "Status.STATUS_PREPARING";
        case Status.STATUS_ROLLEDBACK:
            return "Status.STATUS_ROLLEDBACK";
        case Status.STATUS_ROLLING_BACK:
            return "Status.STATUS_ROLLING_BACK";
        default:
            return "Status.STATUS_UNKNOWN";
        }
    }


    /**
     * Translate flags defined in javax.transaction.xa.XAResource into 
     * string representation
     */
    public static String printFlag(int flags)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(Integer.toHexString(flags));
        sb.append("=");

        if (flags == XAResource.TMNOFLAGS)
        {
            sb.append("TMNOFLAGS");
        }
        else
        {
            if ((flags & XAResource.TMENDRSCAN)   != 0) sb.append("TMENDRSCAN|");
            if ((flags & XAResource.TMFAIL)       != 0) sb.append("TMFAIL|");
            if ((flags & XAResource.TMJOIN)       != 0) sb.append("TMJOIN|");
            if ((flags & XAResource.TMONEPHASE)   != 0) sb.append("TMONEPHASE|");
            if ((flags & XAResource.TMRESUME)     != 0) sb.append("TMRESUME|");
            if ((flags & XAResource.TMSTARTRSCAN) != 0) sb.append("TMSTARTRSCAN|");
            if ((flags & XAResource.TMSUCCESS)    != 0) sb.append("TMSUCCESS|");
            if ((flags & XAResource.TMSUSPEND)    != 0) sb.append("TMSUSPEND|");

            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    /**
     *  toString Helper when object is Corba Ref and we do not want IOR in the trace
     */
    public static String identity(java.lang.Object x)
    {
        if (x == null) return "" + x;
        return(x.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(x)));
    }


    /**
     * Duplicate a byte array.
     */
    public static byte[] duplicateByteArray(byte[] in)
    {
        if (in == null) return null;

        return duplicateByteArray(in, 0, in.length);
    }


    /**
     * Duplicate a piece of a byte array.
     */
    public static byte[] duplicateByteArray(byte[] in, int offset, int length)
    {
        if (in == null) return null;

        byte[] out =  new byte[length];
        System.arraycopy(in, offset, out, 0, length);

        return out;
    }

    /**
     * Read one to four bytes (big-endian) from a byte array and convert
     * to an integer.
     *
     * @param bytes input byte array
     * @param offset offset of integer value
     * @param byteCount number of bytes in value (1 <= byteCount <= 4)
     *
     * @return value of the number
     */
    public static int getIntFromBytes(byte[] bytes,
                                      int    offset,
                                      int    byteCount)
    {
        int value = 0;

        switch (byteCount)
        {
            case 4: value |= ( bytes[offset + 3] & 0xFF);
            case 3: value |= ((bytes[offset + 2] & 0xFF) << 8);
            case 2: value |= ((bytes[offset + 1] & 0xFF) << 16);
            case 1: value |= ((bytes[offset + 0] & 0xFF) << 24);
                    value = value >> ((4 - byteCount) * 8);
                break;
            default:
                final String msg = "byteCount is not between 1 and 4";
                IllegalArgumentException iae = new IllegalArgumentException(msg);
                FFDCFilter.processException(
                    iae,
                    "com.ibm.ws.Transaction.JTA.Util.getIntFromBytes",
                    "553");
                throw iae;
        }

        return value;
    }


    /**
     * Utility function to set sequence numbers in big-endian format 
     * in a byte array.
     */
    public static void setBytesFromInt(byte[] bytes,
                                       int    offset,
                                       int    byteCount,
                                       int    value)
    {
        long maxval = ((1L << (8 * byteCount)) - 1);
        if (value > maxval)
        {
            final String msg = "value too large for byteCount";
            IllegalArgumentException iae = new IllegalArgumentException(msg);
            FFDCFilter.processException(
                iae,
                "com.ibm.ws.Transaction.JTA.Util.setBytesFromInt",
                "579");
            throw iae;
        }

        switch (byteCount)
        {
            case 4: bytes[offset++] = (byte) ((value >> 24) & 0xFF);
            case 3: bytes[offset++] = (byte) ((value >> 16) & 0xFF);
            case 2: bytes[offset++] = (byte) ((value >> 8 ) & 0xFF);
            case 1: bytes[offset++] = (byte) ((value      ) & 0xFF);
                break;
            default:
                final String msg = "byteCount is not between 1 and 4";
                IllegalArgumentException iae =
                    new IllegalArgumentException(msg);
                FFDCFilter.processException(
                    iae,
                    "com.ibm.ws.Transaction.JTA.Util.setBytesFromInt",
                    "598");
                throw iae;
        }
    }

    /**
     * Utility function which extracts a long from a byte array representation in big endian format
     */
    public static long getLongFromBytes(byte[] bytes,
                                        int    offset)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getLongFromBytes: length = " + bytes.length + ", data = " + toHexString(bytes));
        long result = -1;
        if (bytes.length >= offset + 8)
        {
            result = ((bytes[0 + offset]&0xff) << 56) +
                     ((bytes[1 + offset]&0xff) << 48) +
                     ((bytes[2 + offset]&0xff) << 40) +
                     ((bytes[3 + offset]&0xff) << 32) +
                     ((bytes[4 + offset]&0xff) << 24) +
                     ((bytes[5 + offset]&0xff) << 16) +
                     ((bytes[6 + offset]&0xff) << 8) +
                      (bytes[7 + offset]&0xff);
        }
        if (tc.isEntryEnabled()) Tr.exit(tc, "getLongFromBytes " + result);

        return result;
    }


    /**
     * Utility function which transfers a long to a byte array in big endian format.
     */
    public static byte[] longToBytes(long rmid)
    {
        return new byte[] { (byte)(rmid>>56), (byte)(rmid>>48), (byte)(rmid>>40), (byte)(rmid>>32),
                            (byte)(rmid>>24), (byte)(rmid>>16), (byte)(rmid>>8), (byte)(rmid)};
    }


    /**
     * Utility function which transfers an int to a byte array in big endian format.
     */
    public static byte[] intToBytes(int rmid)
    {
        return new byte[] { (byte)(rmid>>24), (byte)(rmid>>16), (byte)(rmid>>8), (byte)(rmid)};
    }

    final static String digits = "0123456789abcdef";
    
    /**
     * Converts a byte array to a hexadecimal string.
     */
    public static String toHexString(byte[] b) {
       StringBuffer result = new StringBuffer(b.length * 2);
       for (int i = 0; i < b.length; i++) {
          result.append(digits.charAt((b[i] >> 4) & 0xf));
          result.append(digits.charAt(b[i] & 0xf));
       }
       return (result.toString());
    }
    
    /**
     *  Returns true if the byte arrays are identical; false
     * otherwise.
     */
    public static boolean equal(byte[] a, byte[] b) {
       if (a == b)
          return (true);
       if ((a == null) || (b == null))
          return (false);
       if (a.length != b.length)
          return (false);
       for (int i = 0; i < a.length; i++)
          if (a[i] != b[i])
             return (false);
       return (true);
    }
    
    /**
     * Converts a double byte array to a string.
     */
    public static String byteArrayToString(byte[] b) {
       final int l = b.length/2;
       if (l*2 != b.length) throw new IllegalArgumentException();
       StringBuffer result = new StringBuffer(l);
       int o = 0;
       for (int i = 0; i < l; i++)
       {
           int i1 = b[o++] & 0xff;
           int i2 = b[o++] & 0xff;
           i2 = i2 << 8;
           i1 = i1 | i2;
           result.append((char)i1);
       }
       return (result.toString());
    }
    
    /**
     * Get a string containing the stack of the specified exception
     *
     * @param e
     * @return
     */
    public static String stackToDebugString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        String text = sw.toString();
        
        // Jump past the throwable
        text = text.substring(text.indexOf("at"));
        return text;
    }
}

