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
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_char", type = Character.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_byte", type = Byte.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_short", type = Short.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_int", type = Integer.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_long", type = Long.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_boolean", type = Boolean.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_double", type = Double.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.BasicEnvPrimMixServlet/JNDI_Ann_float", type = Float.class)
})
@WebServlet("/BasicEnvPrimMixServlet")
public class BasicEnvPrimMixServlet extends FATServlet {
    private static final String CLASS_NAME = BasicEnvPrimMixServlet.class.getName();

    // Expected Injected Value Constants as defined in the XML
    private static final char E_CHAR = 'o';
    private static final byte E_BYTE = 1;
    private static final short E_SHORT = 1;
    private static final int E_INTEGER = 158;
    private static final long E_LONG = 254L;
    private static final boolean E_BOOL = true;
    private static final double E_DOUBLE = 856.93D;
    private static final float E_FLOAT = 548.72F;

    // Resources to be field injected via annotation but described by XML
    // The XML should override the given values
    @Resource
    private char ifchar = 't';
    @Resource
    private byte ifbyte = Byte.MAX_VALUE;
    @Resource
    private short ifshort = Short.MAX_VALUE;
    @Resource
    private int ifint = Integer.MAX_VALUE;
    @Resource
    private long iflong = Long.MAX_VALUE;
    @Resource
    private boolean ifboolean = false;
    @Resource
    private double ifdouble = Double.MAX_VALUE;
    @Resource
    private float iffloat = Float.MAX_VALUE;

    // Resources to be method injected via annotation but described by XML
    private char imchar;
    private byte imbyte;
    private short imshort;
    private int imint;
    private long imlong;
    private boolean imboolean;
    private double imdouble;
    private float imfloat;

    void preventFinal() {
        ifchar = 't';
        ifbyte = Byte.MAX_VALUE;
        ifshort = Short.MAX_VALUE;
        ifint = Integer.MAX_VALUE;
        iflong = Long.MAX_VALUE;
        ifboolean = false;
        ifdouble = Double.MAX_VALUE;
        iffloat = Float.MAX_VALUE;
    }

    /**
     * Tests Annotated/XML field injection of Primitive Environment Entries
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvPrimFldMixInjection() {
        EnvMixPrimTestHelper.testEnvMixPrimChar(CLASS_NAME, "ifchar", ifchar);

        EnvMixPrimTestHelper.testEnvMixPrimByte(CLASS_NAME, "ifbyte", ifbyte);

        EnvMixPrimTestHelper.testEnvMixPrimShort(CLASS_NAME, "ifshort", ifshort);

        EnvMixPrimTestHelper.testEnvMixPrimInt(CLASS_NAME, "ifint", ifint);

        EnvMixPrimTestHelper.testEnvMixPrimLong(CLASS_NAME, "iflong", iflong);

        EnvMixPrimTestHelper.testEnvMixPrimBool(CLASS_NAME, "ifboolean", ifboolean);

        EnvMixPrimTestHelper.testEnvMixPrimDouble(CLASS_NAME, "ifdouble", ifdouble);

        EnvMixPrimTestHelper.testEnvMixPrimFloat(CLASS_NAME, "iffloat", iffloat);
    }

    /**
     * This test case specifically tests class-level @Resource declaration and
     * lookup in servlets
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvPrimJNDIClassLevelResourceMixLookup() {
        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_char", E_CHAR);

        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_byte", E_BYTE);

        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_short", E_SHORT);

        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_int", E_INTEGER);

        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_long", E_LONG);

        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_boolean", E_BOOL);

        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_double", E_DOUBLE);

        EnvMixPrimTestHelper.testLookup(CLASS_NAME, "JNDI_Ann_float", E_FLOAT);
    }

    /**
     * Tests Annotated/XML method injection of Primitive Environment Entries
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvPrimMthdMixInjection() {
        EnvMixPrimTestHelper.testEnvMixPrimChar(CLASS_NAME, "imchar", imchar);

        EnvMixPrimTestHelper.testEnvMixPrimByte(CLASS_NAME, "imbyte", imbyte);

        EnvMixPrimTestHelper.testEnvMixPrimShort(CLASS_NAME, "imshort", imshort);

        EnvMixPrimTestHelper.testEnvMixPrimInt(CLASS_NAME, "imint", imint);

        EnvMixPrimTestHelper.testEnvMixPrimLong(CLASS_NAME, "imlong", imlong);

        EnvMixPrimTestHelper.testEnvMixPrimBool(CLASS_NAME, "imboolean", imboolean);

        EnvMixPrimTestHelper.testEnvMixPrimDouble(CLASS_NAME, "imdouble", imdouble);

        EnvMixPrimTestHelper.testEnvMixPrimFloat(CLASS_NAME, "imfloat", imfloat);
    }

    @Resource
    public void setImchar(char imchar) {
        this.imchar = imchar;
    }

    @Resource
    public void setImbyte(byte imbyte) {
        this.imbyte = imbyte;
    }

    @Resource
    public void setImshort(short imshort) {
        this.imshort = imshort;
    }

    @Resource
    public void setImint(int imint) {
        this.imint = imint;
    }

    @Resource
    public void setImlong(long imlong) {
        this.imlong = imlong;
    }

    @Resource
    public void setImboolean(boolean imboolean) {
        this.imboolean = imboolean;
    }

    @Resource
    public void setImdouble(double imdouble) {
        this.imdouble = imdouble;
    }

    @Resource
    public void setImfloat(float imfloat) {
        this.imfloat = imfloat;
    }
}