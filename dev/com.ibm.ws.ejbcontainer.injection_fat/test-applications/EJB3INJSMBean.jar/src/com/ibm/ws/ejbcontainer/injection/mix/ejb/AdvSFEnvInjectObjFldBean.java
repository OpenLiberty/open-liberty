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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

/**
 * Basic Stateful Bean implementation for testing Environment Injection
 * of primitive object fields.
 **/
@SuppressWarnings("unused")
@Stateful(name = "AdvSFEnvInjectObjFld")
@Local(EnvInjectionLocal.class)
@Remote(EnvInjectionRemote.class)
@Resources({
             @Resource(name = "ClassLevelString", type = String.class),
             @Resource(name = "AnotherClassLevelString")
})
public class AdvSFEnvInjectObjFldBean {
    private static final String CLASS_NAME = AdvSFEnvInjectObjFldBean.class.getName();
    private static final String PASSED = "Passed";

    // Expected Injected Value Constants
    private static final String A_STRING = new String("You have been overridden.");
    private static final String B_STRING = new String("Yo!");
    private static final String C_STRING = null;
    private static final String D_STRING = null;
    private static final String E_STRING = new String("You'll never find me!");
    private static final String F_STRING = new String("They found me!  I don't know how, but they found me!");
    private static final String G_STRING = new String("Look me up");
    private static final String H_STRING = new String("Head of the class");
    private static final String I_STRING = new String("Also in class");
    private static final Integer I_INTEGER = new Integer(42);
    private static final Object I_OBJECT = I_INTEGER;

    private static final String J_STRING = new String("I'm being injected into multiple fields!!");
    private static final Long J_LONG = new Long(72L);
    private static final byte J_BYTE = new Byte("3");
    private static final Integer J_INTEGER = new Integer(88);
    private static final Double J_DOUBLE = new Double(37.44);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    @Resource
    public String overrideString = "Yo!";
    @Resource
    protected String defaultString = "Yo!";
    @Resource
    private String noString;
    @Resource
    public String cannotFindString;
    @Resource(name = "newNameString")
    String renamedString;
    @Resource
    private Object integerObject;

    @Resource
    private SessionContext ivContext;

    @Resource(name = "multipleString")
    public String ivMString1;
    @Resource(name = "multipleString")
    String ivMString2;
    @Resource(name = "multipleLong")
    protected Long ivMLong1;
    @Resource(name = "multipleLong")
    private Long ivMLong2;
    @Resource(name = "multipleLong")
    public Long ivMLong3;
    @Resource(name = "multipleByte")
    public Byte ivMByte1;
    @Resource(name = "multipleByte")
    Byte ivMByte2;
    @Resource(name = "multipleByte")
    protected Byte ivMByte3;
    @Resource(name = "multipleByte")
    private Byte ivMByte4;
    @Resource(name = "multipleInteger")
    public Integer ivMInteger1;
    @Resource(name = "multipleInteger")
    Integer ivMInteger2;
    @Resource(name = "multipleInteger")
    protected Integer ivMInteger3;
    @Resource(name = "multipleInteger")
    private Integer ivMInteger4;
    @Resource(name = "multipleInteger")
    public Integer ivMInteger5;
    @Resource(name = "multipleDouble")
    public Double ivMDouble1;
    @Resource(name = "multipleDouble")
    Double ivMDouble2;
    @Resource(name = "multipleDouble")
    protected Double ivMDouble3;
    @Resource(name = "multipleDouble")
    private Double ivMDouble4;
    @Resource(name = "multipleDouble")
    public Double ivMDouble5;
    @Resource(name = "multipleDouble")
    public Double ivMDouble6;

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that all of the primitive object method types are injected
        // correctly from the Environment Entries.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "overrideString is You have been overridden. : " + overrideString,
                     A_STRING, overrideString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "defaultString is Yo! : " + defaultString,
                     B_STRING, defaultString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "noString is null : " + noString,
                     C_STRING, noString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "cannotFindString is null : " + cannotFindString,
                     D_STRING, cannotFindString);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "renamedString is Doc Brown saying : " + renamedString,
                     F_STRING, renamedString);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "integerObject is 42 : " + integerObject,
                     I_OBJECT, integerObject);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "String field is : " + ivMString1,
                     J_STRING, ivMString1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "String field is : " + ivMString2,
                     J_STRING, ivMString2);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Long field is 72 : " + ivMLong1,
                     J_LONG, ivMLong1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Long field is 72 : " + ivMLong2,
                     J_LONG, ivMLong2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Long field is 72 : " + ivMLong3,
                     J_LONG, ivMLong3);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Byte field is 3 : " + ivMByte1,
                     J_BYTE, ivMByte1.byteValue());
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Byte field is 3 : " + ivMByte2,
                     J_BYTE, ivMByte2.byteValue());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Byte field is 3 : " + ivMByte3,
                     J_BYTE, ivMByte3.byteValue());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Byte field is 3 : " + ivMByte4,
                     J_BYTE, ivMByte4.byteValue());
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Integer field is 88 : " + ivMInteger1,
                     J_INTEGER, ivMInteger1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Integer field is 88 : " + ivMInteger2,
                     J_INTEGER, ivMInteger2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Integer field is 88 : " + ivMInteger3,
                     J_INTEGER, ivMInteger3);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Integer field is 88 : " + ivMInteger4,
                     J_INTEGER, ivMInteger4);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Integer field is 88 : " + ivMInteger5,
                     J_INTEGER, ivMInteger5);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 37.44 : " + ivMDouble1,
                     J_DOUBLE, ivMDouble1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 37.44 : " + ivMDouble2,
                     J_DOUBLE, ivMDouble2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 37.44 : " + ivMDouble3,
                     J_DOUBLE, ivMDouble3);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 37.44 : " + ivMDouble4,
                     J_DOUBLE, ivMDouble4);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 37.44 : " + ivMDouble5,
                     J_DOUBLE, ivMDouble5);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Double field is 37.44 : " + ivMDouble6,
                     J_DOUBLE, ivMDouble6);
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/overrideString";
            Object zString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + zString,
                         A_STRING, zString);
            ++testpoint;

            try {
                envName = CLASS_NAME + "/defaultString";
                Object yString = myEnv.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (NameNotFoundException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/noString";
                Object xString = myEnv.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (NameNotFoundException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/cannotFindString";
                Object wString = myEnv.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (NameNotFoundException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
                ++testpoint;
            }

            envName = CLASS_NAME + "/cantFindString";
            Object vString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + vString,
                         E_STRING, vString);
            ++testpoint;

            envName = "newNameString";
            Object uString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + uString,
                         F_STRING, uString);
            ++testpoint;

            envName = CLASS_NAME + "/renamedString";
            Object tString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + tString,
                         G_STRING, tString);
            ++testpoint;

            envName = "ClassLevelString";
            Object sString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + sString,
                         H_STRING, sString);
            ++testpoint;

            envName = "AnotherClassLevelString";
            Object rString = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + rString,
                         I_STRING, rString);
            ++testpoint;

            envName = CLASS_NAME + "/integerObject";
            Object gInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         I_INTEGER, gInteger);
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
            envName = CLASS_NAME + "/overrideString";
            Object aString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + aString,
                         A_STRING, aString);
            ++testpoint;

            try {
                envName = CLASS_NAME + "/defaultString";
                Object bString = ivContext.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + "should not have been found");
            } catch (IllegalArgumentException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/noString";
                Object cString = ivContext.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + "should not have been found");
            } catch (IllegalArgumentException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/cannotFindString";
                Object dString = ivContext.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + "should not have been found");
            } catch (IllegalArgumentException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
                ++testpoint;
            }

            envName = CLASS_NAME + "/cantFindString";
            Object eString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + eString,
                         E_STRING, eString);
            ++testpoint;

            envName = "newNameString";
            Object fString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + fString,
                         F_STRING, fString);
            ++testpoint;

            envName = CLASS_NAME + "/renamedString";
            Object gString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gString,
                         G_STRING, gString);
            ++testpoint;

            envName = "ClassLevelString";
            Object hString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + hString,
                         H_STRING, hString);
            ++testpoint;

            envName = "AnotherClassLevelString";
            Object iString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + iString,
                         I_STRING, iString);
            ++testpoint;

            envName = CLASS_NAME + "/integerObject";
            Object cInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + cInteger,
                         I_INTEGER, cInteger);
            ++testpoint;
            envName = "multipleString";
            Object jString = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jString,
                         J_STRING, jString);
            ++testpoint;

            envName = "multipleLong";
            Object jLong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jLong,
                         J_LONG, jLong);
            ++testpoint;

            envName = "multipleByte";
            Object jByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jByte,
                         J_BYTE, jByte);

            ++testpoint;

            envName = "multipleInteger";
            Object jInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jInteger,
                         J_INTEGER, jInteger);
            ++testpoint;

            envName = "multipleDouble";
            Object jDouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jDouble,
                         J_DOUBLE, jDouble);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

        return PASSED;
    }

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool.
     **/
    public String verifyNoEnvInjection(int testpoint) {
        return PASSED;
    }

    /* Provided for interface completeness, used by SLSB tests */
    public void discardInstance() {
        return;
    }

    public AdvSFEnvInjectObjFldBean() {
        // Intentionally blank
    }
}
