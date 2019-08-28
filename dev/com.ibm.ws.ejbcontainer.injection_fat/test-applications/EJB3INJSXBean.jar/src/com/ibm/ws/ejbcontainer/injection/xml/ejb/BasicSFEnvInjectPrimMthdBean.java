/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Basic Stateful Bean implementation for testing Environment Injection
 * of primitive methods.
 **/
@Stateful(name = "BasicSFEnvInjectPrimMthd")
@Local(EnvInjectionLocal.class)
@Remote(EnvInjectionRemote.class)
public class BasicSFEnvInjectPrimMthdBean {
    private static final String CLASS_NAME = BasicSFEnvInjectPrimMthdBean.class.getName();
    private static final String PASSED = "Passed";
    private static final double DDELTA = 0.0D;
    private static final float FDELTA = 0.0F;

    // Expected Injected Value Constants
    private static final Character I_CHAR = new Character('T');
    private static final Byte I_BYTE = new Byte("5");
    private static final Short I_SHORT = new Short("40");
    private static final Integer I_INTEGER = new Integer(91);
    private static final Long I_LONG = new Long(15L);
    private static final Boolean I_BOOL = new Boolean(true);
    private static final Double I_DOUBLE = new Double(12.257D);
    private static final Float I_FLOAT = new Float(16.23F);

    // Modified Value Constants
    private static final Character M_CHAR = new Character('A');
    private static final Byte M_BYTE = new Byte("11");
    private static final Short M_SHORT = new Short("58");
    private static final Integer M_INTEGER = new Integer(3);
    private static final Long M_LONG = new Long(8L);
    private static final Boolean M_BOOL = new Boolean(false);
    private static final Double M_DOUBLE = new Double(2D);
    private static final Float M_FLOAT = new Float(3F);

    private int ivInjectCount = 0;

    public char ivchar;
    byte ivbyte;
    protected short ivshort;
    private int ivint;
    public long ivlong;
    boolean ivboolean;
    protected double ivdouble;
    private float ivfloat;

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

    @SuppressWarnings("unused")
    private void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that all of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "All Injection Methods called : 8 : " + ivInjectCount,
                     8, ivInjectCount);
        ++testpoint;

        // Assert that all of the primitive method types are injected
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
                     "short field is 40 : " + ivshort,
                     I_SHORT.shortValue(), ivshort);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int field is 91 : " + ivint,
                     I_INTEGER.intValue(), ivint);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "long field is 15 : " + ivlong,
                     I_LONG.longValue(), ivlong);
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "boolean field is true : " + ivboolean,
                   ivboolean);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 12.257 : " + ivdouble,
                     I_DOUBLE.doubleValue(), ivdouble, DDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 16.23 : " + ivfloat,
                     I_FLOAT.floatValue(), ivfloat, FDELTA);
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

        // Finally, reset all of the fields to insure injection does not occur
        // when object is re-used from the cache.
        ivchar = M_CHAR.charValue();
        ivbyte = M_BYTE.byteValue();
        ivshort = M_SHORT.shortValue();
        ivint = M_INTEGER.intValue();
        ivlong = M_LONG.longValue();
        ivboolean = M_BOOL.booleanValue();
        ivdouble = M_DOUBLE.doubleValue();
        ivfloat = M_FLOAT.floatValue();

        ivInjectCount = 0;

        return PASSED;
    }

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the cache.
     **/
    public String verifyNoEnvInjection(int testpoint) {
        String envName = null;

        // Assert that none of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "No Injection Methods called : 0 : " + ivInjectCount,
                     0, ivInjectCount);
        ++testpoint;

        // Assert that none of the primitive method types were injected
        // from the Environment Entries for a cached instance.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "char field is A : " + ivchar,
                     M_CHAR.charValue(), ivchar);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "byte field is 11 : " + ivbyte,
                     M_BYTE.byteValue(), ivbyte);
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
                     "long field is 8 : " + ivlong,
                     M_LONG.longValue(), ivlong);
        ++testpoint;
        assertFalse(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                    "boolean field is false : " + ivboolean,
                    ivboolean);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 2.0  : " + ivdouble,
                     M_DOUBLE.doubleValue(), ivdouble, DDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 3.0  : " + ivfloat,
                     M_FLOAT.floatValue(), ivfloat, FDELTA);
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

        return PASSED;
    }

    /* Provided for interface completeness, used by SLSB tests */
    public void discardInstance() {
        return;
    }

    public BasicSFEnvInjectPrimMthdBean() {
        // intentionally blank
    }
}
