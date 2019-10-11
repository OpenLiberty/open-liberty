/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Basic Stateless Bean implementation for testing Environment Injection
 * of primitive object methods.
 **/
@Stateless(name = "BasicSLEnvInjectObjMthd")
@Local(EnvInjectionLocal.class)
@Remote(EnvInjectionRemote.class)
public class BasicSLEnvInjectObjMthdBean {
    private static final String CLASS_NAME = BasicSLEnvInjectObjMthdBean.class.getName();
    private static final String PASSED = "Passed";

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

    private int ivInjectCount = 0;

    public String ivString;
    Character ivCharacter;
    protected Byte ivByte;
    private Short ivShort;
    public Integer ivInteger;
    Long ivLong;
    protected Boolean ivBoolean;
    private Double ivDouble;
    public Float ivFloat;

    private SessionContext ivContext;

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

    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that all of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "All Injection Methods called : 9 : " + ivInjectCount,
                     9, ivInjectCount);
        ++testpoint;

        // Assert that all of the primitive object method types are injected
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
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
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

        // Finally, reset all of the fields to insure injection does not occur
        // when object is re-used from the pool.
        ivString = M_STRING;
        ivCharacter = M_CHAR;
        ivByte = M_BYTE;
        ivShort = M_SHORT;
        ivInteger = M_INTEGER;
        ivLong = M_LONG;
        ivBoolean = M_BOOL;
        ivDouble = M_DOUBLE;
        ivFloat = M_FLOAT;

        ivInjectCount = 0;

        return PASSED;
    }

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool.
     **/
    public String verifyNoEnvInjection(int testpoint) {
        String envName = null;

        // Assert that none of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "No Injection Methods called : 0 : " + ivInjectCount,
                     0, ivInjectCount);
        ++testpoint;

        // Assert that none of the primitive object method types are injected
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
                     "Double field is 2.0  : " + ivDouble,
                     M_DOUBLE, ivDouble);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 3.0  : " + ivFloat,
                     M_FLOAT, ivFloat);
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

        return PASSED;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public BasicSLEnvInjectObjMthdBean() {
        // intentionally blank
    }
}
