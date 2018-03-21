/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.envann.web;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BasicEnvObjAnnServlet")
public class BasicEnvObjAnnServlet extends FATServlet {
    private static final String CLASS_NAME = BasicEnvObjAnnServlet.class.getName();

    // Expected Injected Value Constants
    private static final String E_STRING = null;
    private static final Character E_CHAR = null;
    private static final Byte E_BYTE = null;
    private static final Short E_SHORT = null;
    private static final Integer E_INTEGER = null;
    private static final Long E_LONG = null;
    private static final Boolean E_BOOL = null;
    private static final Double E_DOUBLE = null;
    private static final Float E_FLOAT = null;

    // Resources to be injected via field injection
    @Resource
    private String ifString;
    @Resource
    private Character ifCharacter;
    @Resource
    private Byte ifByte;
    @Resource
    private Short ifShort;
    @Resource
    private Integer ifInteger;
    @Resource
    private Long ifLong;
    @Resource
    private Boolean ifBoolean;
    @Resource
    private Double ifDouble;
    @Resource
    private Float ifFloat;

    // Resources to be injected via setter method injection
    private String imString;
    private Character imCharacter;
    private Byte imByte;
    private Short imShort;
    private Integer imInteger;
    private Long imLong;
    private Boolean imBoolean;
    private Double imDouble;
    private Float imFloat;

    /**
     * This test case specifically tests @Resource field injection of
     * env-entries into a Servlet.
     *
     * Tests annotated field injection of Object Environment Entries
     * Tests layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CANNOT be looked up in Global namespace
     */
    @Test
    public void testEnvObjFldAnnInjection() throws Exception {
        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifString", E_STRING, ifString);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifCharacter", E_CHAR, ifCharacter);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifByte", E_BYTE, ifByte);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifShort", E_SHORT, ifShort);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifInteger", E_INTEGER, ifInteger);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifLong", E_LONG, ifLong);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifBoolean", E_BOOL, ifBoolean);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifDouble", E_DOUBLE, ifDouble);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "ifFloat", E_FLOAT, ifFloat);
    }

    /**
     * This test case specifically tests @Resource method injection of
     * env-entries into a Servlet.
     *
     * Tests annotated method injection of Object Environment Entries
     * Tests layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CANNOT be looked up in Global namespace
     */
    @Test
    public void testEnvObjMthdAnnInjection() throws Exception {
        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imString", E_STRING, imString);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imCharacter", E_CHAR, imCharacter);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imByte", E_BYTE, imByte);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imShort", E_SHORT, imShort);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imInteger", E_INTEGER, imInteger);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imLong", E_LONG, imLong);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imBoolean", E_BOOL, imBoolean);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imDouble", E_DOUBLE, imDouble);

        EnvAnnObjTestHelper.testEnvAnnObjInjection(CLASS_NAME, "imFloat", E_FLOAT, imFloat);
    }

    // Basic Resource injection
    @Resource
    public void setImstring(String imstring) {
        this.imString = imstring;
    }

    @Resource
    public void setImchar(char imchar) {
        this.imCharacter = imchar;
    }

    @Resource
    public void setImbyte(byte imbyte) {
        this.imByte = imbyte;
    }

    @Resource
    public void setImshort(short imshort) {
        this.imShort = imshort;
    }

    @Resource
    public void setImint(int imint) {
        this.imInteger = imint;
    }

    @Resource
    public void setImlong(long imlong) {
        this.imLong = imlong;
    }

    @Resource
    public void setImboolean(boolean imboolean) {
        this.imBoolean = imboolean;
    }

    @Resource
    public void setImdouble(double imdouble) {
        this.imDouble = imdouble;
    }

    @Resource
    public void setImfloat(float imfloat) {
        this.imFloat = imfloat;
    }
}