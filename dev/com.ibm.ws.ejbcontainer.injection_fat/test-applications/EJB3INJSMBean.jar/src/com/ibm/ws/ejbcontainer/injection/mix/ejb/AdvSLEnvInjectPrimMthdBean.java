/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

/**
 * Basic Stateless Bean implementation for testing Environment Injection
 * of primitive fields.
 **/
@SuppressWarnings("unused")
@Stateless(name = "AdvSLEnvInjectPrimMthd", description = "This is another random description")
@Local(EnvInjectionLocal.class)
@Remote(EnvInjectionRemote.class)
public class AdvSLEnvInjectPrimMthdBean {
    private static final String CLASS_NAME = AdvSLEnvInjectPrimMthdBean.class.getName();
    private static final String PASSED = "Passed";
    private static final float FDELTA = 0.0F;

    // Expected Injected Value Constants
    private static final Integer A_INTEGER = new Integer(42);
    private static final Integer B_INTEGER = new Integer(23);
    private static final Integer C_INTEGER = new Integer(0);
    private static final Integer D_INTEGER = new Integer(0);
    private static final Integer E_INTEGER = new Integer(9999);
    private static final Integer F_INTEGER = new Integer(1234);
    private static final Integer G_INTEGER = new Integer(4321);
    private static final Integer H_INTEGER = new Integer(0);
    private static final Integer I_INTEGER = new Integer(64);
    private static final Character J_CHAR = 'B';

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    private int ivInjectCount = 0;

    public int overrideInt = 23;
    protected int defaultInt = 23;
    private int noInt;
    public int cannotFindInt;
    int renamedInt;
    int tooBigInt;
    private float integerFloat;

    char renamedInjectionCharacter = 'A';
    char differentField = 'D';

    private SessionContext ivContext;

    @Resource(description = "A random description")
    public void setOverrideInt(int aint) {
        overrideInt = aint;
        ++ivInjectCount;
    }

    @Resource
    protected void setDefaultInt(int bint) {
        defaultInt = bint;
        ++ivInjectCount;
    }

    @Resource
    private void setNoInt(int cint) {
        noInt = cint;
        ++ivInjectCount;
    }

    @Resource
    public void setCannotFindInt(int dint) {
        cannotFindInt = dint;
        ++ivInjectCount;
    }

    @Resource(name = "newNameInt")
    void setRenamedInt(int eint) {
        renamedInt = eint;
        ++ivInjectCount;
    }

    @Resource
    private void setIntegerFloat(float afloat) {
        integerFloat = afloat;
        ++ivInjectCount;
    }

    @Resource
    public void setDifferentField(char differentField) {
        this.differentField = differentField;
        ++ivInjectCount;
    }

    @Resource(name = "newInjectionCharacter")
    public void setRenamedInjectionCharacter(char renamedInjectionCharacter) {
        this.renamedInjectionCharacter = renamedInjectionCharacter;
        ++ivInjectCount;
    }

    @Resource
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that three of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Three Injection Methods called : 5 : " + ivInjectCount,
                     5, ivInjectCount);
        ++testpoint;

        // Assert that all of the primitive method types are injected
        // correctly from the Environment Entries.
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "overrideInt is 42. : " + overrideInt,
                     A_INTEGER.intValue(), overrideInt);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "defaultInt is 23 : " + defaultInt,
                     B_INTEGER.intValue(), defaultInt);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "noInt is 0 : " + noInt,
                     C_INTEGER.intValue(), noInt);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "cannotFindInt is 0 : " + cannotFindInt,
                     D_INTEGER.intValue(), cannotFindInt);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "renamedInt is 1234 : " + renamedInt,
                     F_INTEGER.intValue(), renamedInt);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "tooBigInt is 0 : " + tooBigInt,
                     H_INTEGER.intValue(), tooBigInt);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "integerFloat is 64 : " + integerFloat,
                     I_INTEGER.floatValue(), integerFloat, FDELTA);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "renamedInjectionCharacter is B : " + renamedInjectionCharacter,
                     J_CHAR.charValue(), renamedInjectionCharacter);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "differentField is B : " + differentField,
                     J_CHAR.charValue(), differentField);
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = CLASS_NAME + "/overrideInt";
            Object zInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + zInteger,
                         A_INTEGER, zInteger);
            ++testpoint;

            try {
                envName = CLASS_NAME + "/defaultInt";
                Object yInteger = myEnv.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (NameNotFoundException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/noInt";
                Object xInteger = myEnv.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (NameNotFoundException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/cannotFindInt";
                Object wInteger = myEnv.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (NameNotFoundException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
                ++testpoint;
            }

            envName = CLASS_NAME + "/cantFindInt";
            Object vInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + vInteger,
                         E_INTEGER, vInteger);
            ++testpoint;

            envName = "newNameInt";
            Object uInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + uInteger,
                         F_INTEGER, uInteger);
            ++testpoint;

            envName = CLASS_NAME + "/renamedInt";
            Object tInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + tInteger,
                         G_INTEGER, tInteger);
            ++testpoint;

            try {
                envName = CLASS_NAME + "/tooBigInt";
                Object sInteger = myEnv.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (NameNotFoundException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by JNDI lookup", e);
                ++testpoint;
            }

            envName = CLASS_NAME + "/integerFloat";
            Object rInteger = myEnv.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + rInteger,
                         I_INTEGER, rInteger);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Global Environment Property lookup failed : " +
                 envName + " : " + ex);
        }

        // Next, ensure the above may be looked up from the SessionContext
        try {
            envName = CLASS_NAME + "/overrideInt";
            Object aInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + aInteger,
                         A_INTEGER, aInteger);
            ++testpoint;

            try {
                envName = CLASS_NAME + "/defaultInt";
                Object bInteger = ivContext.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + "should not have been found");
            } catch (IllegalArgumentException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/noInt";
                Object cInteger = ivContext.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + "should not have been found");
            } catch (IllegalArgumentException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
                ++testpoint;
            }

            try {
                envName = CLASS_NAME + "/cannotFindInt";
                Object dInteger = ivContext.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + "should not have been found");
            } catch (IllegalArgumentException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
                ++testpoint;
            }

            envName = CLASS_NAME + "/cantFindInt";
            Object eInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + eInteger,
                         E_INTEGER, eInteger);
            ++testpoint;

            envName = "newNameInt";
            Object fInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + fInteger,
                         F_INTEGER, fInteger);
            ++testpoint;

            envName = CLASS_NAME + "/renamedInt";
            Object gInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + gInteger,
                         G_INTEGER, gInteger);
            ++testpoint;

            try {
                envName = CLASS_NAME + "/tooBigInt";
                Object hInteger = ivContext.lookup(envName);
                fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     envName + " should not have been found");
            } catch (IllegalArgumentException e) {
                assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                              "16.4.1.3 : " + envName + " correctly not found by EJBContext.lookup", e);
                ++testpoint;
            }

            envName = CLASS_NAME + "/integerFloat";
            Object iInteger = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + iInteger,
                         I_INTEGER, iInteger);
            ++testpoint;

            envName = "newInjectionCharacter";
            Object character = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + character,
                         J_CHAR, character);
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
     * a method is called using an instance from the pool.
     **/
    public String verifyNoEnvInjection(int testpoint) {
        return PASSED;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public AdvSLEnvInjectPrimMthdBean() {
        // Intentionally blank
    }
}
