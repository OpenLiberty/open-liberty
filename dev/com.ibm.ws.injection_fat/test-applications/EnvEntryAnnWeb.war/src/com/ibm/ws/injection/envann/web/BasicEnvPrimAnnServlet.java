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
@WebServlet("/BasicEnvPrimAnnServlet")
public class BasicEnvPrimAnnServlet extends FATServlet {
    private static final String CLASS_NAME = BasicEnvPrimAnnServlet.class.getName();

    // Resources to be injected
    @Resource
    private char ifchar;
    @Resource
    private byte ifbyte;
    @Resource
    private short ifshort;
    @Resource
    private int ifint;
    @Resource
    private long iflong;
    @Resource
    private boolean ifboolean;
    @Resource
    private double ifdouble;
    @Resource
    private float iffloat;

    // Resources to be injected
    private char imchar;
    private byte imbyte;
    private short imshort;
    private int imint;
    private long imlong;
    private boolean imboolean;
    private double imdouble;
    private float imfloat;

    /**
     * This test case specifically tests @Resource field injection of
     * env-entries into a Servlet.
     *
     * Tests annotated field injection of Primitive Environment Entries
     * Tests layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CANNOT be looked up in Global namespace
     */
    public void testEnvPrimFldAnnInjection() throws Exception {
        EnvAnnPrimTestHelper.testEnvAnnPrimChar(CLASS_NAME, "ifchar", ifchar);

        EnvAnnPrimTestHelper.testEnvAnnPrimByte(CLASS_NAME, "ifbyte", ifbyte);

        EnvAnnPrimTestHelper.testEnvAnnPrimShort(CLASS_NAME, "ifshort", ifshort);

        EnvAnnPrimTestHelper.testEnvAnnPrimInt(CLASS_NAME, "ifint", ifint);

        EnvAnnPrimTestHelper.testEnvAnnPrimLong(CLASS_NAME, "iflong", iflong);

        EnvAnnPrimTestHelper.testEnvAnnPrimBool(CLASS_NAME, "ifboolean", ifboolean);

        EnvAnnPrimTestHelper.testEnvAnnPrimDouble(CLASS_NAME, "ifdouble", ifdouble);

        EnvAnnPrimTestHelper.testEnvAnnPrimFloat(CLASS_NAME, "iffloat", iffloat);
    }

    /**
     * This test case specifically tests @Resource method injection of
     * env-entries into a Servlet.
     *
     * Tests annotated method injection of Primitive Environment Entries
     * Tests layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CANNOT be looked up in Global namespace
     */
    @Test
    public void testEnvPrimMthdAnnInjection() throws Exception {
        EnvAnnPrimTestHelper.testEnvAnnPrimChar(CLASS_NAME, "imchar", imchar);

        EnvAnnPrimTestHelper.testEnvAnnPrimByte(CLASS_NAME, "imbyte", imbyte);

        EnvAnnPrimTestHelper.testEnvAnnPrimShort(CLASS_NAME, "imshort", imshort);

        EnvAnnPrimTestHelper.testEnvAnnPrimInt(CLASS_NAME, "imint", imint);

        EnvAnnPrimTestHelper.testEnvAnnPrimLong(CLASS_NAME, "imlong", imlong);

        EnvAnnPrimTestHelper.testEnvAnnPrimBool(CLASS_NAME, "imboolean", imboolean);

        EnvAnnPrimTestHelper.testEnvAnnPrimDouble(CLASS_NAME, "imdouble", imdouble);

        EnvAnnPrimTestHelper.testEnvAnnPrimFloat(CLASS_NAME, "imfloat", imfloat);
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