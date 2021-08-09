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
package com.ibm.ws.injection.envmix.web;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@Resources(value = {
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_String", type = String.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Character", type = Character.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Byte", type = Byte.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Short", type = Short.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Integer", type = Integer.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Long", type = Long.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Boolean", type = Boolean.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Double", type = Double.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvObjMixServlet/JNDI_Class_Ann_Float", type = Float.class)
})
@WebServlet("/BasicEnvObjMixServlet")
public class BasicEnvObjMixServlet extends FATServlet {
    private static final String CLASS_NAME = BasicEnvObjMixServlet.class.getName();

    // Expected Injected Value Constants as defined in the XML
    private static final String E_STRING = "uebrigens";
    private static final Character E_CHARACTER = 'o';
    private static final Byte E_BYTE = 1;
    private static final Short E_SHORT = 1;
    private static final Integer E_INTEGER = 158;
    private static final Long E_LONG = 254L;
    private static final Boolean E_BOOL = true;
    private static final Double E_DOUBLE = 856.93D;
    private static final Float E_FLOAT = 548.72F;

    // Resources to be field injected via annotation but described by XML
    @Resource
    private String ifString = "This is wrong";
    @Resource
    private Character ifCharacter = 'z';
    @Resource
    private Byte ifByte = 27;
    @Resource
    private Short ifShort = 128;
    @Resource
    private Integer ifInteger = 9875231;
    @Resource
    private Long ifLong = 7823105L;
    @Resource
    private Boolean ifBoolean = false;
    @Resource
    private Double ifDouble = 13.333D;
    @Resource
    private Float ifFloat = 98.333F;

    // Resources to be method injected via annotation but described by XML
    private String imString;
    private Character imCharacter;
    private Byte imByte;
    private Short imShort;
    private Integer imInteger;
    private Long imLong;
    private Boolean imBoolean;
    private Double imDouble;
    private Float imFloat;

    void preventFinal() {
        ifString = "This is wrong";
        ifCharacter = 'z';
        ifByte = 27;
        ifShort = 128;
        ifInteger = 9875231;
        ifLong = 7823105L;
        ifBoolean = false;
        ifDouble = 13.333D;
        ifFloat = 98.333F;
    }

    /**
     * Test Annotated/XML field injection of Object Environment Entries
     * Tests layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace
     */
    @Test
    public void testEnvObjFldMixInjection() {
        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifString", E_STRING, ifString);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifCharacter", E_CHARACTER, ifCharacter);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifByte", E_BYTE, ifByte);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifShort", E_SHORT, ifShort);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifInteger", E_INTEGER, ifInteger);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifLong", E_LONG, ifLong);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifBoolean", E_BOOL, ifBoolean);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifDouble", E_DOUBLE, ifDouble);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "ifFloat", E_FLOAT, ifFloat);
    }

    /**
     * Test Annotated/XML method injection of Object Environment Entries
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvObjMthdMixInjection() {
        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imString", E_STRING, imString);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imCharacter", E_CHARACTER, imCharacter);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imByte", E_BYTE, imByte);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imShort", E_SHORT, imShort);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imInteger", E_INTEGER, imInteger);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imLong", E_LONG, imLong);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imBoolean", E_BOOL, imBoolean);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imDouble", E_DOUBLE, imDouble);

        EnvMixObjTestHelper.testEnvMixObjInjection(CLASS_NAME, "imFloat", E_FLOAT, imFloat);
    }

    /**
     * This test case specifically tests class-level @Resource declaration and
     * lookup on servlets.
     * Tests layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvObjJNDIClassLevelResourceMixLookup() {
        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_String", E_STRING);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Character", E_CHARACTER);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Byte", E_BYTE);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Short", E_SHORT);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Integer", E_INTEGER);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Long", E_LONG);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Boolean", E_BOOL);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Double", E_DOUBLE);

        EnvMixObjTestHelper.testLookup(CLASS_NAME, "JNDI_Class_Ann_Float", E_FLOAT);
    }

    @Resource
    public void setImString(String imString) {
        this.imString = imString;
    }

    @Resource
    public void setImCharacter(Character imCharacter) {
        this.imCharacter = imCharacter;
    }

    @Resource
    public void setImByte(Byte imByte) {
        this.imByte = imByte;
    }

    @Resource
    public void setImShort(Short imShort) {
        this.imShort = imShort;
    }

    @Resource
    public void setImInteger(Integer imInteger) {
        this.imInteger = imInteger;
    }

    @Resource
    public void setImLong(Long imLong) {
        this.imLong = imLong;
    }

    @Resource
    public void setImBoolean(Boolean imBoolean) {
        this.imBoolean = imBoolean;
    }

    @Resource
    public void setImDouble(Double imDouble) {
        this.imDouble = imDouble;
    }

    @Resource
    public void setImFloat(Float imFloat) {
        this.imFloat = imFloat;
    }
}