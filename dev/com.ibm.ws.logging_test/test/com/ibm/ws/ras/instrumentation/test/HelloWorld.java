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
package com.ibm.ws.ras.instrumentation.test;

public class HelloWorld extends SuperImpl implements java.io.Serializable {

    private static final long serialVersionUID = -8283309080922484935L;

    static Object o = new Object();

    static {
        System.out.println("Loaded class");
    }

    public static void main(String[] args) {
        System.out.println("Hello, World!");

        new HelloWorld(1, 555.666777888, "My String", new Object());
    }

    public static String getString() {
        return "String";
    }

    public HelloWorld(int i, double d, String str, Object o) {
        super(getString());

        simpleIntMethod(i);
        simpleStringMethod(str);
        simpleObjectMethod(o);
        simpleVoidMethod();

        simpleBooleanMethod(true);
        simpleByteMethod((byte) 1);
        simpleCharMethod('c');
        simpleDoubleMethod(1.234);
        simpleDoubleMethod(d);
        simpleFloatMethod(5.678f);
        simpleIntMethod(999999999);
        simpleLongMethod(System.currentTimeMillis());
        simpleObjectMethod("String as Object");
        simpleShortMethod((short) 5677);

        simpleVoidDouble(1.234);
        simpleDoubleDouble(1.234, 5.678);
        simpleLongLong(System.currentTimeMillis(), 0L);
        simpleDoubleDoubleDouble(1.234, 5.678, 9.012);

        complexMethod(true, (byte) 1, 'c', 1.234, (float) 5.678, 9999999, System.currentTimeMillis(), "String as Object", (short) 5677);
        HelloWorld.simpleStaticObject("simpleStaticObject");

        try {
            simpleThrowExceptionMethod();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    boolean simpleBooleanMethod(boolean b) {
        return b;
    }

    byte simpleByteMethod(byte b) {
        return b;
    }

    char simpleCharMethod(char c) {
        return c;
    }

    double simpleDoubleMethod(double d) {
        return d;
    }

    float simpleFloatMethod(float f) {
        return f;
    }

    int simpleIntMethod(int i) {
        return i;
    }

    long simpleLongMethod(long l) {
        return l;
    }

    Object simpleObjectMethod(Object o) {
        return o;
    }

    short simpleShortMethod(short s) {
        return s;
    }

    String simpleStringMethod(String str) {
        return str;
    }

    void simpleVoidMethod() {
        return;
    }

    void simpleVoidDouble(double a) {
        return;
    }

    void simpleDoubleDouble(double a, double b) {
        return;
    }

    long simpleLongLong(long a, long b) {
        return a + b;
    }

    double simpleDoubleDoubleDouble(double a, double b, double c) {
        return a + b + c;
    }

    void complexMethod(boolean bool, byte b, char c, double d, float f, int i, long l, Object o, short s) {
        return;
    }

    void simpleThrowExceptionMethod() throws Exception {
        throw new Exception("Exception test");
    }

    static Object simpleStaticObject(Object o) {
        return o;
    }
}

class SuperImpl {
    String s;
    static java.util.logging.Logger logger = null;

    SuperImpl(String str) {
        if (logger != null && logger.isLoggable(java.util.logging.Level.FINER)) {
            logger.entering(SuperImpl.class.getName(), "<init>", new Object[] { str });
        }
        if (s == null) {
            s = str;
        }
        if (logger != null && logger.isLoggable(java.util.logging.Level.FINER)) {
            logger.exiting(SuperImpl.class.getName(), "<init>", this);
        }
    }
}
