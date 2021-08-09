/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.WsocBufferException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

// Utils is excluded from auto instrumented tracing via the build.xml

public class Utils {

    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    private static final TraceComponent tc = Tr.register(Utils.class);

    // convert unsigned long to signed int.
    public static int longToInt(long inLong) {

        if (inLong < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        if (inLong > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) inLong;
    }

//    public static byte byteArrayToByte(byte[] ba) {
//        if ((ba == null) || (ba.length == 0)) {
//            // Question: add translation for this exception?
//            IllegalArgumentException x = new IllegalArgumentException("Array of no size passed in");
//            throw x;
//        }
//
//        return ba[0];
//    }

    public static short byteArrayToShort(byte[] ba) throws WsocBufferException {
        short num = 0;
        if ((ba == null) || (ba.length == 0)) {
            IllegalArgumentException x = new IllegalArgumentException("Array of no size passed in");
            throw new WsocBufferException(x);
        }

        if (ba.length >= 2) {
            num = (short) (ba[0] << 8 + ba[1]);
        } else {
            num = ba[0];
        }
        return num;
    }

    public static char byteArrayToChar(byte[] ba) throws WsocBufferException {
        short num = 0;
        num = byteArrayToShort(ba);
        char c = (char) num;
        return c;
    }

//    public static int byteArrayToInt(byte[] ba) {
//        // convert up to the first 4 bytes in the array to an int
//        int num = 0;
//
//        if ((ba == null) || (ba.length == 0)) {
//            // Question: add translation for this exception?
//            IllegalArgumentException x = new IllegalArgumentException("Array of no size passed in");
//            throw x;
//        }
//
//        int loopMax = ba.length;
//        if (loopMax > 4) {
//            loopMax = 4;
//        }
//
//        for (int i = 0; i < loopMax; i++) {
//            if (i > 0) {
//                num = num << 8;
//            }
//            num = num + ba[i];
//        }
//        return num;
//    }

//    public static long byteArrayToLong(byte[] ba) {
//        // convert up to the first 8 bytes in the array to a long
//        long num = 0;
//
//        if ((ba == null) || (ba.length == 0)) {
//            // Question: add translation for this exception?
//            IllegalArgumentException x = new IllegalArgumentException("Array of no size passed in");
//            throw x;
//        }
//
//        int loopMax = ba.length;
//        if (loopMax > 8) {
//            loopMax = 8;
//        }
//
//        for (int i = 0; i < loopMax; i++) {
//            if (i > 0) {
//                num = num << 8;
//            }
//            num = num + ba[i];
//        }
//        return num;
//    }

//    public static Float byteArrayToFloat(byte[] ba) {
//        float num = 0;
//
//        if ((ba == null) || (ba.length == 0)) {
//            // Question: add translation for this exception?
//            IllegalArgumentException x = new IllegalArgumentException("Array of no size passed in");
//            throw x;
//        }
//
//        // Question: what to do if the array is less than 4 bytes? pad or return an error? - but no callers of this method now.
//
//        ByteBuffer buffer = ByteBuffer.wrap(ba);
//        // network order is big endian
//        buffer.order(ByteOrder.BIG_ENDIAN);
//
//        num = ByteBuffer.wrap(ba).getFloat();
//
//        return num;
//    }

//    public static double byteArrayToDouble(byte[] ba) {
//        double num = 0;
//
//        if ((ba == null) || (ba.length == 0)) {
//            // Question: add translation for this exception?
//            IllegalArgumentException x = new IllegalArgumentException("Array of no size passed in");
//            throw x;
//        }
//
//        // Question: what to do if the array is less than 8 bytes? pad or return an error?  - but no callers of this method now.
//
//        ByteBuffer buffer = ByteBuffer.wrap(ba);
//        // network order is big endian
//        buffer.order(ByteOrder.BIG_ENDIAN);
//
//        num = ByteBuffer.wrap(ba).getDouble();
//
//        return num;
//    }

//    public static boolean byteArrayToBoolean(byte[] ba) {
//        if ((ba == null) || (ba.length == 0)) {
//            // Question: add translation for this exception?
//            IllegalArgumentException x = new IllegalArgumentException("Array of no size passed in");
//            throw x;
//        }
//
//        // Question:  do we look for 0 and 1 here, or a text string of "true"/"false" ?   - but no callers of this method now.
//        if (ba[0] == 0) {
//            return false;
//        } else {
//            return true;
//        }
//    }
//
//    public static byte[] booleanToByteArray(boolean x) {
//        if (x) {
//            byte[] ba = new byte[1];
//            ba[0] = 1;
//            return ba;
//        } else {
//            byte[] ba = new byte[1];
//            ba[0] = 0;
//            return ba;
//        }
//    }
//
//    public static byte[] characterToByteArray(Character x) {
//        short s = (short) x.charValue(); // both shorts are chars are 16 bit primitives.
//        byte[] ba = new byte[2];
//
//        ba = shortToByteArray(s);
//        return ba;
//    }
//
//    public static byte[] shortToByteArray(Short x) {
//        byte[] ba = new byte[2];
//
//        ba[0] = (byte) (x >>> 8); // most signifcant byte first in array
//        ba[1] = (byte) (x >>> 0); // can assign byte to short outright
//
//        return ba;
//    }
//
//    public static byte[] integerToByteArray(Integer x) {
//        int numBytes = 4;
//        byte[] ba = new byte[numBytes];
//
//        // most significant byte first in array
//        for (int i = 0; i < numBytes; i++) {
//            ba[i] = (byte) (x >>> (8 * (numBytes - 1 - i)));
//        }
//        return ba;
//    }
//
//    public static byte[] longToByteArray(Long x) {
//        int numBytes = 8;
//        byte[] ba = new byte[numBytes];
//
//        // most significant byte first in array
//        for (int i = 0; i < numBytes; i++) {
//            ba[i] = (byte) (x >>> (8 * (numBytes - 1 - i)));
//        }
//        return ba;
//    }
//
//    public static byte[] floatToByteArray(Float x) {
//        byte[] ba = new byte[4];
//
//        ByteBuffer buffer = ByteBuffer.allocate(4);
//        buffer.order(ByteOrder.BIG_ENDIAN);
//        buffer.putFloat(x);
//
//        // don't use buffer.array, since that assumes a backing array
//        buffer.position(0);
//        buffer.get(ba);
//
//        return ba;
//    }
//
//    public static byte[] doubleToByteArray(Double x) {
//        byte[] ba = new byte[8];
//
//        ByteBuffer buffer = ByteBuffer.allocate(8);
//        buffer.order(ByteOrder.BIG_ENDIAN);
//        buffer.putDouble(x);
//
//        // don't use buffer.array, since that assumes a backing array
//        buffer.position(0);
//        buffer.get(ba);
//
//        return ba;
//    }

    public static void printOutBuffers(WsByteBuffer[] bufs) {
        int j = 0;
        int maxToPrint = 20;
        for (int i = 0; i < bufs.length; i++) {
            if (j >= maxToPrint)
                break;
            WsByteBuffer buf = bufs[i];
            if (buf != null) {
                int oldPos = buf.position();
                while ((buf.position() < buf.limit()) && (j < maxToPrint)) {
                    byte b = buf.get();
                    // Special Debug and comment for findbug avoidance
                    // System.out.println("buf:" + i + " pos:" + (buf.position() - 1) + "-->0x" + Integer.toHexString(b & 0xff));
                    j++;
                }
                buf.position(oldPos);
            }
        }
    }

    public static void printOutBuffer(WsByteBuffer buf) {
        int j = 0;
        int maxToPrint = 20;
        if (buf != null) {
            int oldPos = buf.position();
            while ((buf.position() < buf.limit()) && (j < maxToPrint)) {
                byte b = buf.get();
                // Special Debug and comment for findbug avoidance
                // System.out.println("buf pos:" + (buf.position() - 1) + "-->0x" + Integer.toHexString(b & 0xff));
                j++;
            }
            buf.position(oldPos);
        }
    }

    //recursively gets all interfaces
    public static void getAllInterfaces(Class<?> classObject, ArrayList<Type> interfaces) {
        Type[] superInterfaces = classObject.getGenericInterfaces();
        interfaces.addAll((Arrays.asList(superInterfaces)));

        // for cases where the extended class is defined with a Generic.
        Type tgs = classObject.getGenericSuperclass();
        if (tgs != null) {
            if ((tgs) instanceof ParameterizedType) {
                interfaces.add(tgs);
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "updated interfaces to: " + interfaces);
        }

        Class<?> superClass = classObject.getSuperclass();
        if (superClass != null) {
            getAllInterfaces(superClass, interfaces);
        }
    }

    public static Class<?> getCodingClass(Type t) {
        Class<?> clazz = null;

        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Type[] types = pt.getActualTypeArguments();
            if (types.length > 0) {
                Type t2 = pt.getActualTypeArguments()[0];
                clazz = getClassByType(t2);
            }
        }
        return clazz;
    }

    public static Class<?> getClassByType(Type t) {
        Class<?> clazz = null;

        if (t instanceof Class) {
            clazz = (Class<?>) t;
        } else if (t instanceof ParameterizedType) {
            ParameterizedType pp = (ParameterizedType) t;
            Type tp = pp.getRawType();
            if (tp instanceof Class) {
                clazz = (Class<?>) tp;
            }
        }
        return clazz;
    }

    public static void maskPayload(@Sensitive int maskNumber, WsByteBuffer[] frameBuffers, int count) {

        byte[] maskArray = new byte[4];
        maskArray[0] = (byte) ((maskNumber >> 24) & 0x000000ff);
        maskArray[1] = (byte) ((maskNumber >> 16) & 0x000000ff);
        maskArray[2] = (byte) ((maskNumber >> 8) & 0x000000ff);
        maskArray[3] = (byte) ((maskNumber) & 0x000000ff);
        maskPayload(maskArray, frameBuffers, count);

    }

    public static void maskPayload(@Sensitive byte[] maskArray, WsByteBuffer[] frameBuffers, int count) {
        // Assume that format data has been processed, therefore all WsByteBuffer Position value point to that start of the payload data in a given buffer.
        // Limit then points to the byte after the end of the data.  if Position equals Limit, then the buffer has not payload data.
        // Go through each byte buffer and XOR the mask, byte by byte

        // assume the mask is setup as follows:
        // most significant byte of mask is mask[0], least is mask[3];

        // match mask offset across multiple buffers
        int resume = 0;

        // XOR mask into the bytes in the buffer
        for (int i = 0; i < count; i++) {

            WsByteBuffer buf = frameBuffers[i];
            int position = buf.position();
            int limit = buf.limit();
            // make sure buffer has at least one byte of data in it.
            if ((limit - position) > 0) {
                if (buf.hasArray()) {
                    byte[] ba = buf.array();
                    int arrayPosition = position + buf.arrayOffset();
                    int arrayLimit = limit + buf.arrayOffset();

                    resume = maskArray(ba, arrayPosition, arrayLimit - 1, resume, maskArray);

                } else {
                    // no backing array, so read buffer into an array, mask, then put it back
                    int size = buf.remaining();
                    byte[] ba = new byte[size];

                    // read buffer into an array, reset position when done
                    int oldPosition = buf.position();
                    buf.get(ba, 0, size);
                    buf.position(oldPosition);

                    // mask the bytes in the array
                    resume = maskArray(ba, 0, size - 1, resume, maskArray);

                    // but the array back in the buffer, reset  position when done
                    buf.put(ba, 0, size);
                    buf.position(oldPosition);
                }
            }
        }
    }

    @Sensitive
    public static int maskArray(@Sensitive byte[] ba, int start, int end, int resume, @Sensitive byte[] masks) {
        // start is the first valid byte in the array to mask.
        // end is that last valid byte in the array to mask.
        // resume holds where we left off masking

        int i = start;
        int index0 = (0 + resume) % 4;
        int index1 = (1 + resume) % 4;
        int index2 = (2 + resume) % 4;
        int index3 = (3 + resume) % 4;

        while (end - i >= 3) {
            ba[i] = (byte) (ba[i] ^ masks[index0]);
            i++;
            ba[i] = (byte) (ba[i] ^ masks[index1]);
            i++;
            ba[i] = (byte) (ba[i] ^ masks[index2]);
            i++;
            ba[i] = (byte) (ba[i] ^ masks[index3]);
            i++;
        }

        // 0 -3 bytes left in the array to mask
        if (i <= end) {
            ba[i] = (byte) (ba[i] ^ masks[index0]);
            resume++;
            i++;

            if (i <= end) {
                ba[i] = (byte) (ba[i] ^ masks[index1]);
                resume++;
                i++;

                if (i <= end) {
                    ba[i] = (byte) (ba[i] ^ masks[index2]);
                    resume++;
                }
            }
        }

        return resume % 4;
    }

    @Sensitive
    public static String makeAcceptResponseHeaderValue(String key) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        String inputKey = key + Constants.GUID;

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] arrayKey = inputKey.getBytes(StandardCharsets.ISO_8859_1);
        // Question: should it be:  "utf-8" above?
        md.update(arrayKey, 0, arrayKey.length);
        byte[] sha1hash = md.digest();

        return Base64Coder.encode(sha1hash);

    }

    @Sensitive
    public static String uTF8byteArrayToString(byte[] data) throws CharacterCodingException {

        CharsetDecoder d = Utils.UTF8_CHARSET.newDecoder();
        d.onMalformedInput(CodingErrorAction.REPORT);
        d.onUnmappableCharacter(CodingErrorAction.REPORT);
        return d.decode(ByteBuffer.wrap(data)).toString();

    }

    //close reason needs to be truncated to 123 UTF-8 encoded bytes
    public static String truncateCloseReason(String reasonPhrase) {
        if (reasonPhrase != null) {
            byte[] reasonBytes = reasonPhrase.getBytes(Utils.UTF8_CHARSET);
            int len = reasonBytes.length;

            // Why 120?   UTF-8 can take 4 bytes per character, so we either hit the boundary, or are off by 1-3 bytes.
            //  Subtract 3 from 123 and we'll try to cut only if it is greater than that.
            if (len > 120) {
                String updatedPhrase = cutStringByByteSize(reasonPhrase, 120);
                reasonPhrase = updatedPhrase;
            }

        }
        return reasonPhrase;
    }

    public static String cutStringByByteSize(String s, int len) {

        CharsetDecoder cd = UTF8_CHARSET.newDecoder();
        cd.onMalformedInput(CodingErrorAction.IGNORE);

        byte[] bytez = s.getBytes(UTF8_CHARSET);

        // safeguard the wrap
        if (len > bytez.length) {
            len = bytez.length;
        }

        ByteBuffer bb = ByteBuffer.wrap(bytez, 0, len);
        CharBuffer cb = CharBuffer.allocate(len);
        cd.decode(bb, cb, true);
        cd.flush(cb);

        return new String(cb.array(), 0, cb.position());
    }

    public static ClassLoader getContextClassloaderPrivileged() {
        final Thread tr = Thread.currentThread();
        return AccessController.doPrivileged(
                                             new PrivilegedAction<ClassLoader>() {
                                                 @Override
                                                 public ClassLoader run() {

                                                     return tr.getContextClassLoader();
                                                 }
                                             });
    }
}
