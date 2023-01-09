/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Basic Stateless Bean implementation for testing Environment Injection
 * of object and primitive fields when set methods are present.
 **/
@Stateless(name = "BasicSLEnvInjectFldWithMthd")
@Local(EnvInjectionLocal.class)
public class BasicSLEnvInjectFldWithMthdBean {
    private static final String CLASS_NAME = BasicSLEnvInjectFldWithMthdBean.class.getName();
    private static final String PASSED = "Passed";
    private static final double DDELTA = 0.0D;
    private static final float FDELTA = 0.0F;

    // Expected Injected Value Constants
    private static final String I_STRING = new String("Hi Bob!");
    private static final Character I_CHAR = new Character('T');
    private static final Byte I_BYTE = new Byte("5");
    private static final Short I_SHORT = new Short("40");
    private static final Integer I_INTEGER = new Integer(91);
    private static final Long I_LONG = new Long(15L);
    private static final Boolean I_BOOL = new Boolean(true);
    private static final Double I_DOUBLE = new Double(12.257D);
    private static final Float I_FLOAT = new Float(16.23F);

    // Modified Value Constants
    private static final String M_STRING = new String("Scooby");
    private static final Character M_CHAR = new Character('A');
    private static final Byte M_BYTE = new Byte("11");
    private static final Short M_SHORT = new Short("58");
    private static final Integer M_INTEGER = new Integer(3);
    private static final Long M_LONG = new Long(8L);
    private static final Boolean M_BOOL = new Boolean(false);
    private static final Double M_DOUBLE = new Double(2D);
    private static final Float M_FLOAT = new Float(3F);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    private int ivInjectCount = 0;

    @Resource
    public char ivchar;
    @Resource
    byte ivbyte;
    byte ivbyte2;
    @Resource
    protected short ivshort;
    @Resource
    private int ivint;
    int ivint2;
    @Resource
    public long ivlong;
    @Resource
    boolean ivboolean;
    boolean ivboolean2;
    @Resource
    protected double ivdouble;
    @Resource
    private float ivfloat;
    public float ivfloat2;

    @Resource
    public String ivString;
    @Resource
    Character ivCharacter;
    @Resource
    protected Byte ivByte;
    @Resource
    private Short ivShort;
    @Resource
    public Integer ivInteger;
    @Resource
    Long ivLong;
    @Resource
    protected Boolean ivBoolean;
    @Resource
    private Double ivDouble;
    @Resource
    public Float ivFloat;

    @EJB(name = "StatefulEJB", beanName = "BasicSFEnvInjectObjFld")
    private EnvInjectionLocal ivEJB;

    @Resource
    private SessionContext ivContext;

    public void setIvchar(char achar) {
        ivchar = achar;
        ++ivInjectCount;
    }

    void setIvbyte(byte abyte) {
        ivbyte = abyte;
        ++ivInjectCount;
    }

    protected void setIvshort(short ashort) {
        ivshort = ashort;
        ++ivInjectCount;
    }

    @SuppressWarnings("unused")
    private void setIvint(int aint) {
        ivint = aint;
        ++ivInjectCount;
    }

    public void setIvlong(long along) {
        ivlong = along;
        ++ivInjectCount;
    }

    void setIvboolean(boolean aboolean) {
        ivboolean = aboolean;
        ++ivInjectCount;
    }

    protected void setIvdouble(double adouble) {
        ivdouble = adouble;
        ++ivInjectCount;
    }

    @SuppressWarnings("unused")
    private void setIvfloat(float afloat) {
        ivfloat = afloat;
        ++ivInjectCount;
    }

    public void setIvString(String aString) {
        ivString = aString;
        ++ivInjectCount;
    }

    void setIvCharacter(Character aCharacter) {
        ivCharacter = aCharacter;
        ++ivInjectCount;
    }

    protected void setIvByte(Byte aByte) {
        ivByte = aByte;
        ++ivInjectCount;
    }

    @SuppressWarnings("unused")
    private void setIvShort(Short aShort) {
        ivShort = aShort;
        ++ivInjectCount;
    }

    public void setIvInteger(Integer aInteger) {
        ivInteger = aInteger;
        ++ivInjectCount;
    }

    void setIvLong(Long aLong) {
        ivLong = aLong;
        ++ivInjectCount;
    }

    protected void setIvBoolean(Boolean aBoolean) {
        ivBoolean = aBoolean;
        ++ivInjectCount;
    }

    @SuppressWarnings("unused")
    private void setIvDouble(Double aDouble) {
        ivDouble = aDouble;
        ++ivInjectCount;
    }

    public void setIvFloat(Float aFloat) {
        ivFloat = aFloat;
        ++ivInjectCount;
    }

    @SuppressWarnings("unused")
    private void setIvContext(SessionContext sc) {
        ivContext = sc;
        ++ivInjectCount;
    }

    public void setIvEJB(EnvInjectionLocal ejb) {
        ivEJB = ejb;
        ++ivInjectCount;
    }

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that no method level injection has occurred.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "char field is T : " + ivchar,
                     0, ivInjectCount);
        ++testpoint;

        // Assert that all of the primitive field types are injected
        // correctly from the Environment Entries.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "char field is T : " + ivchar,
                     I_CHAR.charValue(), ivchar);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "byte field is 5 : " + ivbyte,
                     I_BYTE.byteValue(), ivbyte);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "byte field is 5 : " + ivbyte2,
                     I_BYTE.byteValue(), ivbyte2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "short field is 40 : " + ivshort,
                     I_SHORT.shortValue(), ivshort);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int field is 91 : " + ivint,
                     I_INTEGER.intValue(), ivint);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int2 field is 91 : " + ivint2,
                     I_INTEGER.intValue(), ivint2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "long field is 15 : " + ivlong,
                     I_LONG.longValue(), ivlong);
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "boolean field is true : " + ivboolean,
                   ivboolean);
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "boolean field is true : " + ivboolean2,
                   ivboolean2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 12.257 : " + ivdouble,
                     I_DOUBLE.doubleValue(), ivdouble, DDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 16.23 : " + ivfloat,
                     I_FLOAT.floatValue(), ivfloat, FDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 16.23 : " + ivfloat2,
                     I_FLOAT.floatValue(), ivfloat2, FDELTA);
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/ivchar";
            Object gChar = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gChar,
                         I_CHAR, gChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivbyte";
            Object gByte = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gByte,
                         I_BYTE, gByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivshort";
            Object gShort = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gShort,
                         I_SHORT, gShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivint";
            Object gInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         I_INTEGER, gInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivlong";
            Object gLong = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gLong,
                         I_LONG, gLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivboolean";
            Object gBoolean = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gBoolean,
                         I_BOOL, gBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivdouble";
            Object gDouble = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gDouble,
                         I_DOUBLE, gDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivfloat";
            Object gFloat = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gFloat,
                         I_FLOAT, gFloat);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
        try {
            envName = CLASS_NAME + "/ivchar";
            Object cChar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cChar,
                         I_CHAR, cChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivbyte";
            Object cByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cByte,
                         I_BYTE, cByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivshort";
            Object cShort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cShort,
                         I_SHORT, cShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivint";
            Object cInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cInteger,
                         I_INTEGER, cInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivlong";
            Object cLong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cLong,
                         I_LONG, cLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivboolean";
            Object cBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cBoolean,
                         I_BOOL, cBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivdouble";
            Object cDouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cDouble,
                         I_DOUBLE, cDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivfloat";
            Object cFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cFloat,
                         I_FLOAT, cFloat);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

        // Assert that all of the primitive object field types are injected
        // correctly from the Environment Entries.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "String field is Hi Bob! : " + ivString,
                     I_STRING, ivString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Character field is T : " + ivCharacter,
                     I_CHAR, ivCharacter);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Byte field is 5 : " + ivByte,
                     I_BYTE, ivByte);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Short field is 40 : " + ivShort,
                     I_SHORT, ivShort);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Integer field is 91 : " + ivInteger,
                     I_INTEGER, ivInteger);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Long field is 15 : " + ivLong,
                     I_LONG, ivLong);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Boolean field is true : " + ivBoolean,
                     I_BOOL, ivBoolean);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 12.257 : " + ivDouble,
                     I_DOUBLE, ivDouble);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 16.23 : " + ivFloat,
                     I_FLOAT, ivFloat);
        ++testpoint;

        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "EJB is " + ivEJB, ivEJB);
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/ivString";
            Object gString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gString,
                         I_STRING, gString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object gChar = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gChar,
                         I_CHAR, gChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object gByte = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gByte,
                         I_BYTE, gByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object gShort = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gShort,
                         I_SHORT, gShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object gInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         I_INTEGER, gInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object gLong = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gLong,
                         I_LONG, gLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object gBoolean = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gBoolean,
                         I_BOOL, gBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object gDouble = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gDouble,
                         I_DOUBLE, gDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object gFloat = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gFloat,
                         I_FLOAT, gFloat);
            ++testpoint;

            envName = "StatefulEJB";
            Object gEJB = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName + ":" + gEJB, gEJB);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
        try {
            envName = CLASS_NAME + "/ivString";
            Object cString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cString,
                         I_STRING, cString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object cChar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cChar,
                         I_CHAR, cChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object cByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cByte,
                         I_BYTE, cByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object cShort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cShort,
                         I_SHORT, cShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object cInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cInteger,
                         I_INTEGER, cInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object cLong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cLong,
                         I_LONG, cLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object cBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cBoolean,
                         I_BOOL, cBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object cDouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cDouble,
                         I_DOUBLE, cDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object cFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cFloat,
                         I_FLOAT, cFloat);
            ++testpoint;

            envName = "StatefulEJB";
            Object cEJB = ivContext.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName + ":" + cEJB, cEJB);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

        // Finally, reset all of the fields to insure injection does not occur
        // when object is re-used from the pool.
        ivchar = M_CHAR.charValue();
        ivbyte = M_BYTE.byteValue();
        ivbyte2 = M_BYTE.byteValue();
        ivshort = M_SHORT.shortValue();
        ivint = M_INTEGER.intValue();
        ivint2 = M_INTEGER.intValue();
        ivlong = M_LONG.longValue();
        ivboolean = M_BOOL.booleanValue();
        ivboolean2 = M_BOOL.booleanValue();
        ivdouble = M_DOUBLE.doubleValue();
        ivfloat = M_FLOAT.floatValue();
        ivfloat2 = M_FLOAT.floatValue();

        ivString = M_STRING;
        ivCharacter = M_CHAR;
        ivByte = M_BYTE;
        ivShort = M_SHORT;
        ivInteger = M_INTEGER;
        ivLong = M_LONG;
        ivBoolean = M_BOOL;
        ivDouble = M_DOUBLE;
        ivFloat = M_FLOAT;
        ivEJB = null;

        ivInjectCount = 0;

        return PASSED;
    }

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool.
     **/
    public String verifyNoEnvInjection(int testpoint) {
        String envName = null;

        // Assert that no method level injection has occurred.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "char field is T : " + ivchar,
                     0, ivInjectCount);
        ++testpoint;

        // Assert that none of the primitive field types are injected
        // from the Environment Entries for a pooled instance.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "char field is A : " + ivchar,
                     M_CHAR.charValue(), ivchar);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "byte field is 11 : " + ivbyte,
                     M_BYTE.byteValue(), ivbyte);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "byte2 field is 11 : " + ivbyte2,
                     M_BYTE.byteValue(), ivbyte2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "short field is 58 : " + ivshort,
                     M_SHORT.shortValue(), ivshort);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int field is 3 : " + ivint,
                     M_INTEGER.intValue(), ivint);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int2 field is 3 : " + ivint2,
                     M_INTEGER.intValue(), ivint2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "long field is 8 : " + ivlong,
                     M_LONG.longValue(), ivlong);
        ++testpoint;
        assertFalse(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                    "boolean field is false : " + ivboolean,
                    ivboolean);
        ++testpoint;
        assertFalse(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                    "boolean field is false : " + ivboolean2,
                    ivboolean2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 2.0  : " + ivdouble,
                     M_DOUBLE.doubleValue(), ivdouble, DDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 3.0  : " + ivfloat,
                     M_FLOAT.floatValue(), ivfloat, FDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 3.0  : " + ivfloat2,
                     M_FLOAT.floatValue(), ivfloat2, FDELTA);
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
        // and return the original injected value (not modified value)
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/ivchar";
            Object gChar = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gChar,
                         I_CHAR, gChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivbyte";
            Object gByte = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gByte,
                         I_BYTE, gByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivshort";
            Object gShort = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gShort,
                         I_SHORT, gShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivint";
            Object gInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         I_INTEGER, gInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivlong";
            Object gLong = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gLong,
                         I_LONG, gLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivboolean";
            Object gBoolean = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gBoolean,
                         I_BOOL, gBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivdouble";
            Object gDouble = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gDouble,
                         I_DOUBLE, gDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivfloat";
            Object gFloat = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gFloat,
                         I_FLOAT, gFloat);
            ++testpoint;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
        // and return the original injected value (not modified value)
        try {
            envName = CLASS_NAME + "/ivchar";
            Object cChar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cChar,
                         I_CHAR, cChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivbyte";
            Object cByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cByte,
                         I_BYTE, cByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivshort";
            Object cShort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cShort,
                         I_SHORT, cShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivint";
            Object cInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cInteger,
                         I_INTEGER, cInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivlong";
            Object cLong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cLong,
                         I_LONG, cLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivboolean";
            Object cBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cBoolean,
                         I_BOOL, cBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivdouble";
            Object cDouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cDouble,
                         I_DOUBLE, cDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivfloat";
            Object cFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cFloat,
                         I_FLOAT, cFloat);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

        // Assert that none of the primitive object field types are injected
        // from the Environment Entries for a pooled instance.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "String field is Scooby : " + ivString,
                     M_STRING, ivString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Character field is A : " + ivCharacter,
                     M_CHAR, ivCharacter);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Byte field is 11 : " + ivByte,
                     M_BYTE, ivByte);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Short field is 58 : " + ivShort,
                     M_SHORT, ivShort);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Integer field is 3 : " + ivInteger,
                     M_INTEGER, ivInteger);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Long field is 8 : " + ivLong,
                     M_LONG, ivLong);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Boolean field is false : " + ivBoolean,
                     M_BOOL, ivBoolean);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 2.0 : " + ivDouble,
                     M_DOUBLE, ivDouble);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 3.0  : " + ivFloat,
                     M_FLOAT, ivFloat);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "EJB is " + ivEJB, ivEJB);
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
        // and return the original injected value (not modified value)
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/ivString";
            Object gString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gString,
                         I_STRING, gString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object gChar = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gChar,
                         I_CHAR, gChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object gByte = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gByte,
                         I_BYTE, gByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object gShort = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gShort,
                         I_SHORT, gShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object gInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         I_INTEGER, gInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object gLong = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gLong,
                         I_LONG, gLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object gBoolean = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gBoolean,
                         I_BOOL, gBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object gDouble = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gDouble,
                         I_DOUBLE, gDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object gFloat = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gFloat,
                         I_FLOAT, gFloat);
            ++testpoint;

            envName = "StatefulEJB";
            Object gEJB = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName + ":" + gEJB, gEJB);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
        // and return the original injected value (not modified value)
        try {
            envName = CLASS_NAME + "/ivString";
            Object cString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cString,
                         I_STRING, cString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object cChar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cChar,
                         I_CHAR, cChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object cByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cByte,
                         I_BYTE, cByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object cShort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cShort,
                         I_SHORT, cShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object cInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cInteger,
                         I_INTEGER, cInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object cLong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cLong,
                         I_LONG, cLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object cBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cBoolean,
                         I_BOOL, cBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object cDouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cDouble,
                         I_DOUBLE, cDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object cFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cFloat,
                         I_FLOAT, cFloat);
            ++testpoint;

            envName = "StatefulEJB";
            Object cEJB = ivContext.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName + ":" + cEJB, cEJB);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

        // Finally, reset all of the fields to insure injection does not occur
        // when object is re-used from the pool.
        ivchar = I_CHAR.charValue();
        ivbyte = I_BYTE.byteValue();
        ivbyte2 = I_BYTE.byteValue();
        ivshort = I_SHORT.shortValue();
        ivint = I_INTEGER.intValue();
        ivint2 = I_INTEGER.intValue();
        ivlong = I_LONG.longValue();
        ivboolean = I_BOOL.booleanValue();
        ivboolean2 = I_BOOL.booleanValue();
        ivdouble = I_DOUBLE.doubleValue();
        ivfloat = I_FLOAT.floatValue();
        ivfloat2 = I_FLOAT.floatValue();

        ivString = M_STRING;
        ivCharacter = M_CHAR;
        ivByte = M_BYTE;
        ivShort = M_SHORT;
        ivInteger = M_INTEGER;
        ivLong = M_LONG;
        ivBoolean = M_BOOL;
        ivDouble = M_DOUBLE;
        ivFloat = M_FLOAT;
        ivEJB = null;

        ivInjectCount = 0;

        return PASSED;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public BasicSLEnvInjectFldWithMthdBean() {
        // Intentionally blank
    }
}
