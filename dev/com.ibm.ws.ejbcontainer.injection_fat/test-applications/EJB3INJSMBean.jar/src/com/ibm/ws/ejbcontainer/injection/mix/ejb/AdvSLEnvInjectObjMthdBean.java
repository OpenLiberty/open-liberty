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
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

/**
 * Basic Stateless Bean implementation for testing Environment Injection
 * of primitive object methods.
 **/
@SuppressWarnings("unused")
@Stateless(name = "AdvSLEnvInjectObjMthd")
@Local(EnvInjectionLocal.class)
@Remote(EnvInjectionRemote.class)
@Resources({
             @Resource(name = "ClassLevelString", type = String.class),
             @Resource(name = "AnotherClassLevelString")
})
public class AdvSLEnvInjectObjMthdBean {
    private static final String CLASS_NAME = AdvSLEnvInjectObjMthdBean.class.getName();
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

    private static final Character J_CHARACTER = new Character('C');
    private static final Short J_SHORT = new Short("40");
    private static final Boolean J_BOOLEAN = new Boolean(true);
    private static final Float J_FLOAT = new Float(17.45F);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    private int ivInjectCount = 0;

    public String overrideString = "Yo!";
    protected String defaultString = "Yo!";
    private String noString;
    public String cannotFindString;
    String renamedString;
    private Object integerObject;
    String ctxMethod = "SessionContext not injected before methods.";
    String fieldMethod = "Fields are not injected before methods.";

    public Character ivMCharacter1;
    Character ivMCharacter2;
    protected Short ivMShort1;
    private Short ivMShort2;
    public Short ivMShort3;
    public Boolean ivMBoolean1 = false;
    Boolean ivMBoolean2 = false;
    protected Boolean ivMBoolean3 = false;
    private Boolean ivMBoolean4 = false;
    public Float ivMFloat1;
    Float ivMFloat2;
    protected Float ivMFloat3;
    private Float ivMFloat4;
    public Float ivMFloat5;

    private SessionContext ivContext;

    @Resource
    public void setOverrideString(String lString) {
        overrideString = lString;
        ++ivInjectCount;

        try {
            String eName = CLASS_NAME + "/overrideString";
            Object orString = ivContext.lookup(eName);
            if (orString.equals("You have been overridden.")) {
                ctxMethod = "SessionContext injected before methods.";
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Resource
    protected void setDefaultString(String mString) {
        defaultString = mString;
        ++ivInjectCount;
    }

    @Resource
    private void setNoString(String nString) {
        noString = nString;
        ++ivInjectCount;
    }

    @Resource
    public void setCannotFindString(String oString) {
        cannotFindString = oString;
        ++ivInjectCount;
    }

    @Resource(name = "newNameString")
    void setRenamedString(String pString) {
        renamedString = pString;
        ++ivInjectCount;

        if (fieldInjection.equals("Injected")) {
            fieldMethod = "Fields injected before methods.";
        }
    }

    @Resource
    private void setIntegerObject(Object aObject) {
        integerObject = aObject;
        ++ivInjectCount;
    }

    @Resource(name = "multipleCharacter")
    public void setIvMCharacter1(Character ivMCharacter1) {
        this.ivMCharacter1 = ivMCharacter1;
        ++ivInjectCount;
    }

    @Resource(name = "multipleCharacter")
    public void setIvMCharacter2(Character ivMCharacter2) {
        this.ivMCharacter2 = ivMCharacter2;
        ++ivInjectCount;
    }

    @Resource(name = "multipleShort")
    public void setIvMShort1(Short ivMShort1) {
        this.ivMShort1 = ivMShort1;
        ++ivInjectCount;
    }

    @Resource(name = "multipleShort")
    public void setIvMShort2(Short ivMShort2) {
        this.ivMShort2 = ivMShort2;
        ++ivInjectCount;
    }

    @Resource(name = "multipleShort")
    public void setIvMShort3(Short ivMShort3) {
        this.ivMShort3 = ivMShort3;
        ++ivInjectCount;
    }

    @Resource(name = "multipleBoolean")
    public void setIvMBoolean1(Boolean ivMBoolean1) {
        this.ivMBoolean1 = ivMBoolean1;
        ++ivInjectCount;
    }

    @Resource(name = "multipleBoolean")
    public void setIvMBoolean2(Boolean ivMBoolean2) {
        this.ivMBoolean2 = ivMBoolean2;
        ++ivInjectCount;
    }

    @Resource(name = "multipleBoolean")
    public void setIvMBoolean3(Boolean ivMBoolean3) {
        this.ivMBoolean3 = ivMBoolean3;
        ++ivInjectCount;
    }

    @Resource(name = "multipleBoolean")
    public void setIvMBoolean4(Boolean ivMBoolean4) {
        this.ivMBoolean4 = ivMBoolean4;
        ++ivInjectCount;
    }

    @Resource(name = "multipleFloat")
    public void setIvMFloat1(Float ivMFloat1) {
        this.ivMFloat1 = ivMFloat1;
        ++ivInjectCount;
    }

    @Resource(name = "multipleFloat")
    public void setIvMFloat2(Float ivMFloat2) {
        this.ivMFloat2 = ivMFloat2;
        ++ivInjectCount;
    }

    @Resource(name = "multipleFloat")
    public void setIvMFloat3(Float ivMFloat3) {
        this.ivMFloat3 = ivMFloat3;
        ++ivInjectCount;
    }

    @Resource(name = "multipleFloat")
    public void setIvMFloat4(Float ivMFloat4) {
        this.ivMFloat4 = ivMFloat4;
        ++ivInjectCount;
    }

    @Resource(name = "multipleFloat")
    public void setIvMFloat5(Float ivMFloat5) {
        this.ivMFloat5 = ivMFloat5;
        ++ivInjectCount;
    }

    @Resource
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
        ++ivInjectCount;
    }

    @Resource
    String fieldInjection = "Not injected";

    /**
     * Verify Environment Injection (field or method) occurred properly.
     **/
    public String verifyEnvInjection(int testpoint) {
        String envName = null;

        // Assert that eight of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Eighteen Injection Methods called : 18 : " + ivInjectCount,
                     18, ivInjectCount);
        ++testpoint;

        // Assert that the injections occurred in the correct order: SessionContext, fields, methods
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SessionContext injected before methods",
                     "SessionContext injected before methods.", ctxMethod);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Fields injected before methods",
                     "Fields injected before methods.", fieldMethod);
        ++testpoint;

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
                     "Character field is C : " + ivMCharacter1,
                     J_CHARACTER, ivMCharacter1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Character field is C : " + ivMCharacter2,
                     J_CHARACTER, ivMCharacter1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Short field is 40 : " + ivMShort1,
                     J_SHORT, ivMShort1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Short field is 40 : " + ivMShort2,
                     J_SHORT, ivMShort2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Short field is 40 : " + ivMShort3,
                     J_SHORT, ivMShort3);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Boolean field is true : " + ivMBoolean1,
                     J_BOOLEAN, ivMBoolean1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Boolean field is true : " + ivMBoolean2,
                     J_BOOLEAN, ivMBoolean2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Boolean field is true : " + ivMBoolean3,
                     J_BOOLEAN, ivMBoolean3);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Boolean field is true : " + ivMBoolean4,
                     J_BOOLEAN, ivMBoolean4);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 17.45 : " + ivMFloat1,
                     J_FLOAT, ivMFloat1);
        ++testpoint;

        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 17.45 : " + ivMFloat2,
                     J_FLOAT, ivMFloat2);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 17.45 : " + ivMFloat3,
                     J_FLOAT, ivMFloat3);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 17.45 : " + ivMFloat4,
                     J_FLOAT, ivMFloat4);
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "Float field is 17.45 : " + ivMFloat5,
                     J_FLOAT, ivMFloat5);
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
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

        // Next, ensure the above may be looked up from the SessionContext
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
            envName = "multipleCharacter";
            Object jchar = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jchar,
                         J_CHARACTER, jchar);
            ++testpoint;

            envName = "multipleShort";
            Object jshort = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jshort,
                         J_SHORT, jshort);
            ++testpoint;

            envName = "multipleBoolean";
            Object jBoolean = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jBoolean,
                         J_BOOLEAN, jBoolean);

            ++testpoint;

            envName = "multipleFloat";
            Object jFloat = ivContext.lookup(envName);
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "lookup:" + envName + ":" + jFloat,
                         J_FLOAT, jFloat);
            ++testpoint;
        } catch (AssertionError error) {
            throw error;
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "SessionContext Environment lookup failed : " +
                 envName + " : " + ex);
        }

        ivInjectCount = 0;

        return PASSED;
    }

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool.
     **/
    public String verifyNoEnvInjection(int testpoint) {
        return PASSED;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public AdvSLEnvInjectObjMthdBean() {
        // Intentionally blank
    }
}
