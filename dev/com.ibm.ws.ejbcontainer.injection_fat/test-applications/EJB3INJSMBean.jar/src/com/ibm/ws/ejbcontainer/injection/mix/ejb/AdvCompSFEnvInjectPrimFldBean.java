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
import javax.ejb.CreateException;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

/**
 * Component/Compatibility Stateful Bean implementation for testing Environment
 * Injection of primitive fields.
 **/
@SuppressWarnings("unused")
@Stateful(name = "AdvCompSFEnvInjectPrimFld")
@Local(EnvInjectionLocal.class)
@LocalHome(EnvInjectionEJBLocalHome.class)
@Remote(EnvInjectionRemote.class)
@RemoteHome(EnvInjectionEJBRemoteHome.class)
public class AdvCompSFEnvInjectPrimFldBean implements SessionBean {
    private static final long serialVersionUID = -3380512859253260186L;
    private static final String CLASS_NAME = AdvCompSFEnvInjectPrimFldBean.class.getName();
    private static final String PASSED = "Passed";
    private static final double DDELTA = 0.0D;
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
    private static final Float I_FLOAT = new Float(64F);

    private static final char J_CHAR = new Character('T');
    private static final short J_SHORT = new Short("40");
    private static final long J_LONG = new Long(15L);
    private static final double J_DOUBLE = new Double(37.44D);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    @Resource
    public int overrideInt = 23;
    @Resource
    protected int defaultInt = 23;
    @Resource
    private int noInt;
    @Resource
    public int cannotFindInt;
    @Resource(name = "newNameInt")
    int renamedInt;
    @Resource
    int tooBigInt;
    @Resource
    private float integerFloat;

    @Resource(name = "multiplechar")
    public char ivMchar1;
    @Resource(name = "multiplechar")
    char ivMchar2;
    @Resource(name = "multipleshort")
    protected short ivMshort1;
    @Resource(name = "multipleshort")
    private short ivMshort2;
    @Resource(name = "multipleshort")
    public short ivMshort3;
    @Resource(name = "multiplelong")
    public long ivMlong1;
    @Resource(name = "multiplelong")
    long ivMlong2;
    @Resource(name = "multiplelong")
    protected long ivMlong3;
    @Resource(name = "multiplelong")
    private long ivMlong4;
    @Resource(name = "multipledouble")
    public double ivMdouble1;
    @Resource(name = "multipledouble")
    double ivMdouble2;
    @Resource(name = "multipledouble")
    protected double ivMdouble3;
    @Resource(name = "multipledouble")
    private double ivMdouble4;
    @Resource(name = "multipledouble")
    public double ivMdouble5;

    @Resource
    private SessionContext ivContext;

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that all of the primitive field types are injected
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
                     I_FLOAT.floatValue(), integerFloat, FDELTA);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "char field is T : " + ivMchar1,
                     J_CHAR, ivMchar1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "char field is T : " + ivMchar2,
                     J_CHAR, ivMchar2);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "short field is 40 : " + ivMshort1,
                     J_SHORT, ivMshort1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "short field is 40 : " + ivMshort2,
                     J_SHORT, ivMshort2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "short field is 40 : " + ivMshort3,
                     J_SHORT, ivMshort3);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "long field is 15 : " + ivMlong1,
                     J_LONG, ivMlong1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "long field is 15 : " + ivMlong2,
                     J_LONG, ivMlong2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "long field is 15 : " + ivMlong3,
                     J_LONG, ivMlong3);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "long field is 15 : " + ivMlong4,
                     J_LONG, ivMlong4);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 37.44 : " + ivMdouble1,
                     J_DOUBLE, ivMdouble1, DDELTA);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 37.44 : " + ivMdouble2,
                     J_DOUBLE, ivMdouble2, DDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 37.44 : " + ivMdouble3,
                     J_DOUBLE, ivMdouble3, DDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 37.44 : " + ivMdouble4,
                     J_DOUBLE, ivMdouble4, DDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "double field is 37.44 : " + ivMdouble5,
                     J_DOUBLE, ivMdouble5, DDELTA);
        ++testpoint;

        // Next, insure the above may be looked up in the global namespace
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

        // Next, insure the above may be looked up from the SessionContext
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

            envName = "multiplechar";
            Object jchar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jchar,
                         J_CHAR, jchar);
            ++testpoint;

            envName = "multipleshort";
            Object jshort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jshort,
                         J_SHORT, jshort);
            ++testpoint;

            envName = "multiplelong";
            Object jlong = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jlong,
                         J_LONG, jlong);

            ++testpoint;

            envName = "multipledouble";
            Object jdouble = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jdouble,
                         J_DOUBLE, jdouble);
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

    /* Provided for interface completeness, used by SLSB tests */
    public void discardInstance() {
        return;
    }

    public AdvCompSFEnvInjectPrimFldBean() {
        // Intentionally blank
    }

    public void ejbCreate() throws CreateException {
        // Intentionally blank
    }

    @Override
    public void ejbRemove() {
        // Intentionally blank
    }

    @Override
    public void ejbActivate() {
        // Intentionally blank
    }

    @Override
    public void ejbPassivate() {
        // Intentionally blank
    }

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }

    public String verifyEnvInjectionCDIEnabled(int testpoint) {
        return PASSED;
    }
}
