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
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Basic Stateless Bean implementation for testing Environment Injection
 * of primitive object fields.
 **/
@Stateless(name = "BasicSLEnvInjectObjFld")
@Local(EnvInjectionLocal.class)
@Remote(EnvInjectionRemote.class)
public class BasicSLEnvInjectObjFldBean {
    private static final String CLASS_NAME = BasicSLEnvInjectObjFldBean.class.getName();
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
    private static final String I_FIRST_INCOMPLETE_FIELD = new String("firstIncompleteEntryValue");
    private static final String I_SECOND_INCOMPLETE_FIELD = new String("secondIncompleteEntryValue");

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

    // This variable should get injected from an env-entry defined in xml.
    //    The point here is to make sure that data from two incomplete
    //    env-entry references in xml is correctly merged together.
    private String ivFirstIncompleteEnvEntry;

    // This reference is also an env-entry defined in xml.
    //    The point here is to make sure that data from an
    //    annotation and data from xml get merged together correctly.
    @Resource(name = "secondIncompleteEnvEntry", type = String.class)
    private String ivSecondIncompleteEnvEntry;

    @Resource
    private SessionContext ivContext;

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that all of the primitive object field types are injected
        // correctly from the Environment Entries.
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "String field is Hi Bob! : " + ivString,
                     I_STRING, ivString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Character field is T : " + ivCharacter,
                     I_CHAR, ivCharacter);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Byte field is 5 : " + ivByte,
                     I_BYTE, ivByte);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Short field is 40 : " + ivShort,
                     I_SHORT, ivShort);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Integer field is 91 : " + ivInteger,
                     I_INTEGER, ivInteger);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Long field is 15 : " + ivLong,
                     I_LONG, ivLong);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Boolean field is true : " + ivBoolean,
                     I_BOOL, ivBoolean);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Double field is 12.257 : " + ivDouble,
                     I_DOUBLE, ivDouble);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Float field is 16.23 : " + ivFloat,
                     I_FLOAT, ivFloat);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "First incomplete field is : " + ivFirstIncompleteEnvEntry,
                     I_FIRST_INCOMPLETE_FIELD, ivFirstIncompleteEnvEntry);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Second incomplete field is : " + ivSecondIncompleteEnvEntry,
                     I_SECOND_INCOMPLETE_FIELD, ivSecondIncompleteEnvEntry);
        ++testpoint;

        // Next, insure the above may be looked up in the component namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/ivString";
            Object gString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gString,
                         I_STRING, gString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object gChar = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gChar,
                         I_CHAR, gChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object gByte = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gByte,
                         I_BYTE, gByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object gShort = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gShort,
                         I_SHORT, gShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object gInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         I_INTEGER, gInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object gLong = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gLong,
                         I_LONG, gLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object gBoolean = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gBoolean,
                         I_BOOL, gBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object gDouble = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gDouble,
                         I_DOUBLE, gDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object gFloat = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gFloat,
                         I_FLOAT, gFloat);
            ++testpoint;

            envName = "firstIncompleteEnvEntry";
            Object firstIncompleteEnvEntry = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + firstIncompleteEnvEntry,
                         I_FIRST_INCOMPLETE_FIELD, firstIncompleteEnvEntry);
            ++testpoint;

            envName = "secondIncompleteEnvEntry";
            Object secondIncompleteEnvEntry = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + secondIncompleteEnvEntry,
                         I_SECOND_INCOMPLETE_FIELD, secondIncompleteEnvEntry);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
        try {
            envName = CLASS_NAME + "/ivString";
            Object cString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cString,
                         I_STRING, cString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object cChar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cChar,
                         I_CHAR, cChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object cByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cByte,
                         I_BYTE, cByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object cShort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cShort,
                         I_SHORT, cShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object cInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cInteger,
                         I_INTEGER, cInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object cLong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cLong,
                         I_LONG, cLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object cBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cBoolean,
                         I_BOOL, cBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object cDouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cDouble,
                         I_DOUBLE, cDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object cFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cFloat,
                         I_FLOAT, cFloat);
            ++testpoint;

            envName = "firstIncompleteEnvEntry";
            Object firstIncompleteEntry = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + firstIncompleteEntry,
                         I_FIRST_INCOMPLETE_FIELD, firstIncompleteEntry);
            ++testpoint;

            envName = "secondIncompleteEnvEntry";
            Object secondIncompleteEntry = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + secondIncompleteEntry,
                         I_SECOND_INCOMPLETE_FIELD, secondIncompleteEntry);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

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
        ivFirstIncompleteEnvEntry = M_STRING;
        ivSecondIncompleteEnvEntry = M_STRING;

        return PASSED;
    }

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool.
     **/
    public String verifyNoEnvInjection(int testpoint) {
        String envName = null;

        // Assert that none of the primitive object field types are injected
        // from the Environment Entries for a pooled instance.
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "String field is Scooby : " + ivString,
                     M_STRING, ivString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Character field is A : " + ivCharacter,
                     M_CHAR, ivCharacter);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Byte field is 11 : " + ivByte,
                     M_BYTE, ivByte);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Short field is 58 : " + ivShort,
                     M_SHORT, ivShort);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Integer field is 3 : " + ivInteger,
                     M_INTEGER, ivInteger);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Long field is 8 : " + ivLong,
                     M_LONG, ivLong);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Boolean field is false : " + ivBoolean,
                     M_BOOL, ivBoolean);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Double field is 2.0 : " + ivDouble,
                     M_DOUBLE, ivDouble);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Float field is 3.0  : " + ivFloat,
                     M_FLOAT, ivFloat);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "First incomplete field is Scooby  : " + ivFirstIncompleteEnvEntry,
                     M_STRING, ivFirstIncompleteEnvEntry);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                     "Second incomplete field is Scooby  : " + ivSecondIncompleteEnvEntry,
                     M_STRING, ivSecondIncompleteEnvEntry);
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
        // and return the original injected value (not modified value)
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/ivString";
            Object gString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gString,
                         I_STRING, gString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object gChar = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gChar,
                         I_CHAR, gChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object gByte = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gByte,
                         I_BYTE, gByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object gShort = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gShort,
                         I_SHORT, gShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object gInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         I_INTEGER, gInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object gLong = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gLong,
                         I_LONG, gLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object gBoolean = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gBoolean,
                         I_BOOL, gBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object gDouble = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gDouble,
                         I_DOUBLE, gDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object gFloat = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gFloat,
                         I_FLOAT, gFloat);
            ++testpoint;

            envName = "firstIncompleteEnvEntry";
            Object firstIncompleteEntry = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + firstIncompleteEntry,
                         I_FIRST_INCOMPLETE_FIELD, firstIncompleteEntry);
            ++testpoint;

            envName = "secondIncompleteEnvEntry";
            Object secondIncompleteEntry = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + secondIncompleteEntry,
                         I_SECOND_INCOMPLETE_FIELD, secondIncompleteEntry);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, insure the above may be looked up from the SessionContext
        // and return the original injected value (not modified value)
        try {
            envName = CLASS_NAME + "/ivString";
            Object cString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cString,
                         I_STRING, cString);
            ++testpoint;

            envName = CLASS_NAME + "/ivCharacter";
            Object cChar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cChar,
                         I_CHAR, cChar);
            ++testpoint;

            envName = CLASS_NAME + "/ivByte";
            Object cByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cByte,
                         I_BYTE, cByte);
            ++testpoint;

            envName = CLASS_NAME + "/ivShort";
            Object cShort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cShort,
                         I_SHORT, cShort);
            ++testpoint;

            envName = CLASS_NAME + "/ivInteger";
            Object cInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cInteger,
                         I_INTEGER, cInteger);
            ++testpoint;

            envName = CLASS_NAME + "/ivLong";
            Object cLong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cLong,
                         I_LONG, cLong);
            ++testpoint;

            envName = CLASS_NAME + "/ivBoolean";
            Object cBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cBoolean,
                         I_BOOL, cBoolean);
            ++testpoint;

            envName = CLASS_NAME + "/ivDouble";
            Object cDouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cDouble,
                         I_DOUBLE, cDouble);
            ++testpoint;

            envName = CLASS_NAME + "/ivFloat";
            Object cFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cFloat,
                         I_FLOAT, cFloat);
            ++testpoint;

            envName = "firstIncompleteEnvEntry";
            Object firstIncompleteEntry = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + firstIncompleteEntry,
                         I_FIRST_INCOMPLETE_FIELD, firstIncompleteEntry);
            ++testpoint;

            envName = "secondIncompleteEnvEntry";
            Object secondIncompleteEntry = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + secondIncompleteEntry,
                         I_SECOND_INCOMPLETE_FIELD, secondIncompleteEntry);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 11 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

        // Finally, reset all of the fields to insure injection does not occur
        // when object is re-used from the cache.
        ivString = I_STRING;
        ivCharacter = I_CHAR;
        ivByte = I_BYTE;
        ivShort = I_SHORT;
        ivInteger = I_INTEGER;
        ivLong = I_LONG;
        ivBoolean = I_BOOL;
        ivDouble = I_DOUBLE;
        ivFloat = I_FLOAT;

        return PASSED;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public BasicSLEnvInjectObjFldBean() {
        // Intentionally blank
    }
}
