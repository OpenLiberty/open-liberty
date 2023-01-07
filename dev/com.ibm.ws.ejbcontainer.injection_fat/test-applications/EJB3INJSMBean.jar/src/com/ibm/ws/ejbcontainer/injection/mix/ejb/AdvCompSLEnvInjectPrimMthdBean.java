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
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

/**
 * Component/Compatibility Stateless Bean implementation for testing Environment
 * Injection of primitive fields.
 **/
@SuppressWarnings("unused")
@Stateless(name = "AdvCompSLEnvInjectPrimMthd")
@Local(EnvInjectionLocal.class)
@LocalHome(EnvInjectionEJBLocalHome.class)
@Remote(EnvInjectionRemote.class)
@RemoteHome(EnvInjectionEJBRemoteHome.class)
public class AdvCompSLEnvInjectPrimMthdBean implements SessionBean {
    private static final long serialVersionUID = -1919206987912536996L;
    private static final String CLASS_NAME = AdvCompSLEnvInjectPrimMthdBean.class.getName();
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

    private static final Integer J_INTEGER = new Integer(88);
    private static final byte J_BYTE = new Byte("3");
    private static final Boolean J_BOOL = new Boolean(true);
    private static final Float J_FLOAT = new Float(17.45F);

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

    public byte ivMbyte1;
    byte ivMbyte2;
    protected int ivMint1;
    private int ivMint2;
    public int ivMint3;
    public boolean ivMboolean1 = false;
    boolean ivMboolean2 = false;
    protected boolean ivMboolean3 = false;
    private boolean ivMboolean4 = false;
    public float ivMfloat1;
    float ivMfloat2;
    protected float ivMfloat3;
    private float ivMfloat4;
    public float ivMfloat5;

    private SessionContext ivContext;

    @Resource
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

    @Resource(name = "multiplebyte")
    public void setIvMbyte1(byte ivMbyte1) {
        this.ivMbyte1 = ivMbyte1;
        ++ivInjectCount;
    }

    @Resource(name = "multiplebyte")
    public void setIvMbyte2(byte ivMbyte2) {
        this.ivMbyte2 = ivMbyte2;
        ++ivInjectCount;
    }

    @Resource(name = "multipleint")
    public void setIvMint1(int ivMint1) {
        this.ivMint1 = ivMint1;
        ++ivInjectCount;
    }

    @Resource(name = "multipleint")
    public void setIvMint2(int ivMint2) {
        this.ivMint2 = ivMint2;
        ++ivInjectCount;
    }

    @Resource(name = "multipleint")
    public void setIvMint3(int ivMint3) {
        this.ivMint3 = ivMint3;
        ++ivInjectCount;
    }

    @Resource(name = "multipleboolean")
    public void setIvMboolean1(boolean ivMboolean1) {
        this.ivMboolean1 = ivMboolean1;
        ++ivInjectCount;
    }

    @Resource(name = "multipleboolean")
    public void setIvMboolean2(boolean ivMboolean2) {
        this.ivMboolean2 = ivMboolean2;
        ++ivInjectCount;
    }

    @Resource(name = "multipleboolean")
    public void setIvMboolean3(boolean ivMboolean3) {
        this.ivMboolean3 = ivMboolean3;
        ++ivInjectCount;
    }

    @Resource(name = "multipleboolean")
    public void setIvMboolean4(boolean ivMboolean4) {
        this.ivMboolean4 = ivMboolean4;
        ++ivInjectCount;
    }

    @Resource(name = "multiplefloat")
    public void setIvMfloat1(float ivMfloat1) {
        this.ivMfloat1 = ivMfloat1;
        ++ivInjectCount;
    }

    @Resource(name = "multiplefloat")
    public void setIvMfloat2(float ivMfloat2) {
        this.ivMfloat2 = ivMfloat2;
        ++ivInjectCount;
    }

    @Resource(name = "multiplefloat")
    public void setIvMfloat3(float ivMfloat3) {
        this.ivMfloat3 = ivMfloat3;
        ++ivInjectCount;
    }

    @Resource(name = "multiplefloat")
    public void setIvMfloat4(float ivMfloat4) {
        this.ivMfloat4 = ivMfloat4;
        ++ivInjectCount;
    }

    @Resource(name = "multiplefloat")
    public void setIvMfloat5(float ivMfloat5) {
        this.ivMfloat5 = ivMfloat5;
        ++ivInjectCount;
    }

    /**
     * Verify Environment Injection (field or method) occurred properly.
     */
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that three of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Three Injection Methods called : 17 : " + ivInjectCount,
                     17, ivInjectCount);
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
                     "byte field is 3 : " + ivMbyte1,
                     J_BYTE, ivMbyte1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "byte field is 3 : " + ivMbyte2,
                     J_BYTE, ivMbyte2);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int field is 88 : " + ivMint1,
                     J_INTEGER.intValue(), ivMint1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int field is 88 : " + ivMint2,
                     J_INTEGER.intValue(), ivMint2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "int field is 88 : " + ivMint3,
                     J_INTEGER.intValue(), ivMint3);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "boolean field is true : " + ivMboolean1,
                     J_BOOL, ivMboolean1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "boolean field is true : " + ivMboolean2,
                     J_BOOL, ivMboolean2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "boolean field is true : " + ivMboolean3,
                     J_BOOL, ivMboolean3);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "boolean field is true : " + ivMboolean4,
                     J_BOOL, ivMboolean4);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 17.45 : " + ivMfloat1,
                     J_FLOAT.floatValue(), ivMfloat1, FDELTA);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 17.45 : " + ivMfloat2,
                     J_FLOAT.floatValue(), ivMfloat2, FDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 17.45 : " + ivMfloat3,
                     J_FLOAT.floatValue(), ivMfloat3, FDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 17.45 : " + ivMfloat4,
                     J_FLOAT.floatValue(), ivMfloat4, FDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "float field is 17.45 : " + ivMfloat5,
                     J_FLOAT.floatValue(), ivMfloat5, FDELTA);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "integerFloat is 64 : " + integerFloat,
                     I_INTEGER.floatValue(), integerFloat, FDELTA);
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

            envName = "multiplebyte";
            Object jByte = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jByte,
                         J_BYTE, jByte);
            ++testpoint;

            envName = "multipleint";
            Object jInt = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jInt,
                         J_INTEGER, jInt);
            ++testpoint;

            envName = "multipleboolean";
            Object jBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jBoolean,
                         J_BOOL, jBoolean);

            ++testpoint;

            envName = "multiplefloat";
            Object jFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jFloat,
                         J_FLOAT, jFloat);
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

    public AdvCompSLEnvInjectPrimMthdBean() {
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
    @Resource
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }

    public String verifyEnvInjectionCDIEnabled(int testpoint) {
        return PASSED;
    }
}
