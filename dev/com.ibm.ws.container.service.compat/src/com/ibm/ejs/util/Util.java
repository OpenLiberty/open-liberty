/*******************************************************************************
 * Copyright (c) 1999, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.util;

import java.util.Enumeration;
import java.util.Vector;

/**
 * A hodge podge of utilities.
 *
 */

public final class Util {

    public interface Dumpable {
        public String dump();
    }

    /**
     * A generic "toString" operation for arbitrary objects.
     */
    public static String identity(Object x) {
        if (x == null)
            return "" + x;
        return (x.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(x)));
    }

    /**
     * A generic "toString" operation for an array of arbitrary objects.
     */
    public static String identity(Object[] x) {
        if (x == null) {
            return "" + null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < x.length; i++) {
            sb.append(x[i].getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(x[i])));
            sb.append(", ");
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * Returns true if the byte arrays are identical; false
     * otherwise.
     */
    public static boolean sameByteArray(byte[] a, byte[] b) {
        return equal(a, b);
    }

    /**
     * Returns true if the byte arrays are identical; false
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

    public static byte[] byteArray(String s) {
        return byteArray(s, false);
    }

    public static byte[] byteArray(String s, boolean keepBothBytes) {
        byte[] result = new byte[s.length() * (keepBothBytes ? 2 : 1)];
        for (int i = 0; i < result.length; i++)
            result[i] = keepBothBytes ? (byte) (s.charAt(i / 2) >> (i & 1) * 8) : (byte) (s.charAt(i));
        return result;
    }

    /**
     * Converts a byte array to a string.
     */
    public static String toString(byte[] b) {
        StringBuffer result = new StringBuffer(b.length);
        for (int i = 0; i < b.length; i++)
            result.append((char) b[i]);
        return (result.toString());
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
     * Converts a hexadecimal string to a byte array.
     */

    public static byte[] fromHexString(String s) {
        byte[] result = new byte[s.length() / 2];
        for (int i = 0; i < result.length; i++)
            result[i] = (byte) ((digits.indexOf(s.charAt(2 * i)) << 4) + digits.indexOf(s.charAt(2 * i + 1)));
        return result;
    }

    static class Fatal extends Error {
        private static final long serialVersionUID = 5772669795196571203L;

        Fatal(String s) {
            try {
                System.out.println("Fatal error: " + s);
            } catch (Throwable t) {
                // liberty; need to add this              Ffdc.log(t, this, "com.ibm.ejs.util.Util.Fatal", "85", this); // 477704
            }
        }
    }

    public static void Assert(boolean b) {
        if (!b)
            throw new Fatal("assertion failure");
    }

    public static void Exception() {}

    public static void Warning(String s) {
        System.out.println("TRAN WARNING: " + s);
    }

    public class Executor {
        private Object context;

        final void setContext(Object o) {
            context = o;
        }

        final Object getContext() {
            return context;
        }

        void execute(Object o) {}
    }

    public static void apply(Object[] a, Executor e) {
        apply(a, null, e);
    }

    public static void apply(Object[] a, Object context, Executor e) {
        if (a == null)
            return;
        e.setContext(context);
        for (int i = 0; i < a.length; i++)
            e.execute(a[i]);
    }

    public static void apply(Vector v, Executor e) {
        apply(v, null, e);
    }

    public static void apply(Vector v, Object context, Executor e) {
        if (v == null)
            return;
        e.setContext(context);
        for (Enumeration l = v.elements(); l.hasMoreElements();)
            e.execute(l.nextElement());
    }

    public static Object[] extend(Object[] newList, Object[] oldList, Object addition) {
        for (int i = 0; i < oldList.length; i++)
            newList[i] = oldList[i];
        newList[oldList.length] = addition;
        return newList;
    }

    public static Object extend(Object[] oldList, Object addition) {
        if (oldList == null)
            oldList = new Object[0];
        Object result = java.lang.reflect.Array.newInstance(addition.getClass(), oldList.length + 1);
        System.arraycopy(oldList, 0, result, 0, oldList.length);
        ((Object[]) result)[oldList.length] = addition;
        return (result);
    }

    public static Class[] noParameters = new Class[0];

    private static void println(String s) {
        System.out.println(s);
    }

    public static String dump(Object o, boolean inherited, String separator) {
        try {
            Class c = o.getClass();
            StringBuffer result = new StringBuffer();
            boolean first = true;
            do {
                java.lang.reflect.Field[] f = c.getDeclaredFields();
                for (int i = 0; i < f.length; i++) {
                    if (!first)
                        result.append(separator);
                    result.append(f[i].getName());
                    result.append('=');
                    first = false;

                    try {
                        Object d = f[i].get(o);
                        result.append((d == null) ? "null" : d.toString());
                    } catch (IllegalAccessException e) {
                        // liberty; need to add this                    Ffdc.log(e, Util.class, "com.ibm.ejs.util.Util.dump", "150"); // 477704
                        try {
                            java.lang.reflect.Method m = c.getDeclaredMethod(f[i].getName(), noParameters);
                            Object d = m.invoke(o, (Object[]) noParameters); // d366845.3
                            result.append((d == null) ? "null" : d.toString());
                        } catch (Throwable t) {
                            // liberty; need to add this                       Ffdc.log(t, Util.class, "com.ibm.ejs.util.Util.dump", "156"); // 477704
                            result.append("<inaccessible>");
                        }
                    }
                }
            } while (!inherited || ((c = c.getSuperclass()) != null));

            return result.toString();
        } catch (Throwable t) {
            // liberty; need to add this           Ffdc.log(t, Util.class, "com.ibm.ejs.util.Util.dump", "165"); // 477704
            return "<exception " + t.toString() + ">";
        }
    }

}
