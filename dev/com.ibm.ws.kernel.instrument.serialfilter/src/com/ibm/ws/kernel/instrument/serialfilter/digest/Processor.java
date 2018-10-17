/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.digest;

import com.ibm.ws.kernel.instrument.serialfilter.util.Base64UrlEncoder;
import com.ibm.ws.kernel.instrument.serialfilter.util.MessageUtil;

import org.objectweb.asm.Type;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;



class Processor {
    /** Utilities to update a message digest with arbitrary primitives, strings, and arrays */
    enum Updater {
        NULL(null) {void update(MessageDigest md, Object o) {}},
        BOOLEAN(Boolean.class) {void update(MessageDigest md, Object o) {updateBoolean(md, (Boolean)o);}},
        BYTE(Byte.class) {void update(MessageDigest md, Object o) {updateByte(md, (Byte)o);}},
        CHAR(Character.class) {void update(MessageDigest md, Object o) {updateChar(md, (Character)o);}},
        SHORT(Short.class) {void update(MessageDigest md, Object o) {updateShort(md, (Short)o);}},
        INT(Integer.class) {void update(MessageDigest md, Object o) {updateInt(md, (Integer)o);}},
        LONG(Long.class) {void update(MessageDigest md, Object o) {updateLong(md, (Long)o);}},
        FLOAT(Float.class) {void update(MessageDigest md, Object o) {updateFloat(md, (Float)o);}},
        DOUBLE(Double.class) {void update(MessageDigest md, Object o) {updateDouble(md, (Double)o);}},
        BOOLEAN_ARR(Boolean[].class) {void update(MessageDigest md, Object o) {updateBooleans(md, (boolean[])o);}},
        BYTE_ARR(byte[].class) {void update(MessageDigest md, Object o) {updateBytes(md, (byte[])o);}},
        CHAR_ARR(char[].class) {void update(MessageDigest md, Object o) {updateChars(md, (char[])o);}},
        SHORT_ARR(short[].class) {void update(MessageDigest md, Object o) {updateShorts(md, (short[])o);}},
        INT_ARR(int[].class) {void update(MessageDigest md, Object o) {updateInts(md, (int[])o);}},
        LONG_ARR(long[].class) {void update(MessageDigest md, Object o) {updateLongs(md, (long[])o);}},
        FLOAT_ARR(float[].class) {void update(MessageDigest md, Object o) {updateFloats(md, (float[])o);}},
        DOUBLE_ARR(double[].class) {void update(MessageDigest md, Object o) {updateDoubles(md, (double[])o);}},
        STRING(String.class) {void update(MessageDigest md, Object o) {updateString(md, (String)o);}},
        STRING_ARR(String[].class) {void update(MessageDigest md, Object o) {updateStrings(md, (String[])o);}},
        TYPE(Type.class) {void update(MessageDigest md, Object o) {updateString(md, o.toString());}},
        DEFAULT(null) {
            void update(MessageDigest md, Object o) {
                throw new IllegalArgumentException("Map contains value of unexpected type: " + o.getClass() + " - value = " + o);
            }
        };

        /** A look up table to find the correct digest updater for a given class */
        private static final Map<Class<?>,Updater> LOOKUP_TABLE = new HashMap<Class<?>, Updater>();

        static {
            for (Updater updater : Updater.values())
                if (updater.type != null)
                    LOOKUP_TABLE.put(updater.type, updater);
        }

        static Updater forValue(Object o) {
            if (o == null) return NULL;
            Updater result = LOOKUP_TABLE.get(o.getClass());
            return result == null ? DEFAULT : result;
        }

        final Class<?> type;
        Updater(Class<?> type) {this.type = type;}
        abstract void update(MessageDigest md, Object o);

        private static void updateBoolean(MessageDigest md, boolean val) {md.update(val ? (byte)-1: (byte)0);}

        private static void updateByte(MessageDigest md, byte val) {md.update(val);}

        private static void updateChar(MessageDigest md, char val) {
            md.update((byte) (val));
            md.update((byte) (val >>> 8));
        }

        private static void updateShort(MessageDigest md, short val) {
            md.update((byte) (val));
            md.update((byte) (val >>> 8));
        }

        private static void updateInt(MessageDigest md, int val) {
            md.update((byte) (val));
            md.update((byte) (val >>> 8));
            md.update((byte) (val >>> 16));
            md.update((byte) (val >>> 24));
        }

        private static void updateLong(MessageDigest md, long val) {
            md.update((byte) (val));
            md.update((byte) (val >>> 8));
            md.update((byte) (val >>> 16));
            md.update((byte) (val >>> 24));
            md.update((byte) (val >>> 32));
            md.update((byte) (val >>> 40));
            md.update((byte) (val >>> 48));
            md.update((byte) (val >>> 56));
        }

        private static void updateFloat(MessageDigest md, float val) {updateInt(md, Float.floatToIntBits(val));}

        private static void updateDouble(MessageDigest md, double val) {updateLong(md, Double.doubleToLongBits(val));}

        private static void updateBooleans(MessageDigest md, boolean[] elems) {for (boolean elem : elems) updateBoolean(md, elem);}

        private static void updateBytes(MessageDigest md, byte[] elems) {md.update(elems);}

        private static void updateChars(MessageDigest md, char[] elems) {for (char elem : elems) updateChar(md, elem);}

        private static void updateShorts(MessageDigest md, short[] elems) {for (short elem : elems) updateShort(md, elem);}

        private static void updateInts(MessageDigest md, int[] elems) {for (int elem : elems) updateInt(md, elem);}

        private static void updateLongs(MessageDigest md, long[] elems) {for (long elem : elems) updateLong(md, elem);}

        private static void updateFloats(MessageDigest md, float[] elems) {for (float elem : elems) updateFloat(md, elem);}

        private static void updateDoubles(MessageDigest md, double[] elems) {for (double elem : elems) updateDouble(md, elem);}

        private static void updateString(MessageDigest md, String s) {updateBytes(md, s.getBytes(UTF_8));}

        private static void updateStrings(MessageDigest md, String[] elems) {for (String elem : elems) updateString(md, elem);}
    }

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    static MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(MessageUtil.format("SF_ERROR_NO_SHA_SUPPORT", e));
        }
    }

    private MessageDigest md = newMessageDigest();
    private byte[] digest;

    Processor consider(Map<String, Object> map) {
        // values may be boxed primitives or arrays of primitives
        for (Map.Entry<String, Object> entry: map.entrySet()) {
            md.update(entry.getKey().getBytes(UTF_8));
            Object value = entry.getValue();
            if (value == null)
                continue;
            Updater.forValue(value).update(md, value);
        }
        return this;
    }

    void considerValue(Object value) {
        Updater.forValue(value).update(md, value);
    }

    <D extends Digester> Processor consider(DigesterSortedMap<D> hashers) {
        for (Map.Entry<String, D> entry : hashers)
            consider(entry.getKey()).consider(entry.getValue());
        return this;
    }

    private void consider(Digester digester) {
        Updater.updateBytes(md, digester.getDigest());
    }

    Processor consider(int i) {
        Updater.updateInt(md, i);
        return this;
    }

    Processor consider(String string) {
        Updater.updateString(md, string);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    Processor consider(String...strings) {
        Updater.updateStrings(md, strings);
        return this;
    }

    byte[] getDigest() {
        if (digest == null) {
            digest = md.digest();
            md = null;
        }
        return digest;
    }

    String getDigestAsString() {
        //java.util.Base64UrlEncoder.getEncoder().encodeToString(getDigest());
        return Base64UrlEncoder.URL_AND_FILENAME_SAFE_ENCODING.encode(getDigest());
    }
}
