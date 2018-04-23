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
package com.ibm.ws.injection.envxml.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BasicEnvPrimXMLServlet")
public class BasicEnvPrimXMLServlet extends FATServlet {
    private static final String CLASS_NAME = BasicEnvPrimXMLServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants as defined in the XML
    private static final char E_CHAR = 'a';
    private static final byte E_BYTE = 1;
    private static final short E_SHORT = 1;
    private static final int E_INTEGER = 5;
    private static final long E_LONG = 100L;
    private static final boolean E_BOOL = true;
    private static final double E_DOUBLE = 100.0D;
    private static final float E_FLOAT = 100.0F;

    // Resources to be field injected via XML
    private char ifchar;
    private byte ifbyte;
    private short ifshort;
    private int ifint;
    private long iflong;
    private boolean ifboolean;
    private double ifdouble;
    private float iffloat;

    // Resources to be method injected via XML
    private char imchar;
    private byte imbyte;
    private short imshort;
    private int imint;
    private long imlong;
    private boolean imboolean;
    private double imdouble;
    private float imfloat;

    private InitialContext initCtx;

    public BasicEnvPrimXMLServlet() {
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests XML field injection of Primitive Environment Entries
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvPrimFldXMLInjection() {
        assertEquals("The injected character was not the expected value", E_CHAR, ifchar);
        assertNotNull("Failed to lookup the char: \"ifchar\"", lookup("ifchar"));

        assertEquals("The injected byte was not the expected value", E_BYTE, ifbyte);
        assertNotNull("Failed to lookup the byte: \"ifbyte\"", lookup("ifbyte"));

        assertEquals("The injected short was not the expected value", E_SHORT, ifshort);
        assertNotNull("Failed to lookup the short: \"ifshort\"", lookup("ifshort"));

        assertEquals("The injected integer was not the expected value", E_INTEGER, ifint);
        assertNotNull("Failed to lookup the int: \"ifint\"", lookup("ifint"));

        assertEquals("The injected long was not the expected value", E_LONG, iflong);
        assertNotNull("Failed to lookup the long: \"iflong\"", lookup("iflong"));

        assertEquals("The injected boolean was not the expected value", E_BOOL, ifboolean);
        assertNotNull("Failed to lookup the boolean: \"ifboolean\"", lookup("ifboolean"));

        assertEquals("The injected double was not the expected value", E_DOUBLE, ifdouble);
        assertNotNull("Failed to lookup the double: \"ifdouble\"", lookup("ifdouble"));

        assertEquals("The injected float was not the expected value", E_FLOAT, iffloat);
        assertNotNull("Failed to lookup the float: \"iffloat\"", lookup("iffloat"));
    }

    /**
     * Tests XML method injection of Primitive Environment Entries
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvPrimMthdXMLInjection() {
        assertEquals("The injected character was not the expected value", E_CHAR, imchar);
        assertNotNull("Failed to lookup the char: \"imchar\"", lookup("imchar"));

        assertEquals("The injected byte was not the expected value", E_BYTE, imbyte);
        assertNotNull("Failed to lookup the byte: \"imbyte\"", lookup("imbyte"));

        assertEquals("The injected short was not the expected value", E_SHORT, imshort);
        assertNotNull("Failed to lookup the short: \"imshort\"", lookup("imshort"));

        assertEquals("The injected integer was not the expected value", E_INTEGER, imint);
        assertNotNull("Failed to lookup the int: \"imint\"", lookup("imint"));

        assertEquals("The injected long was not the expected value", E_LONG, imlong);
        assertNotNull("Failed to lookup the long: \"imlong\"", lookup("imlong"));

        assertEquals("The injected boolean was not the expected value", E_BOOL, imboolean);
        assertNotNull("Failed to lookup the boolean: \"imboolean\"", lookup("imboolean"));

        assertEquals("The injected double was not the expected value", E_DOUBLE, imdouble);
        assertNotNull("Failed to lookup the double: \"imdouble\"", lookup("imdouble"));

        assertEquals("The injected float was not the expected value", E_FLOAT, imfloat);
        assertNotNull("Failed to lookup the float: \"imfloat\"", lookup("imfloat"));
    }

    /**
     * Performs a global lookup on the given name.
     *
     * @param name Name to lookup
     * @return The Object that was returned from the lookup;
     */
    public Object lookup(String name) {
        try {
            return initCtx.lookup("java:comp/env/" + CLASS_NAME + "/" + name);
        } catch (NamingException e) {
            svLogger.info("There was an exception while performing the lookup");
            e.printStackTrace();
            return null;
        }
    }

    public void setImcharMethod(char imchar) {
        this.imchar = imchar;
    }

    public void setImbyteMethod(byte imbyte) {
        this.imbyte = (byte) (imbyte + E_BYTE);
    }

    public void setImshortMethod(short imshort) {
        this.imshort = (short) (imshort + E_SHORT);
    }

    public void setImintMethod(int imint) {
        this.imint = imint + E_INTEGER;
    }

    public void setImlongMethod(long imlong) {
        this.imlong = imlong + E_LONG;
    }

    public void setImbooleanMethod(boolean imboolean) {
        this.imboolean = imboolean;
    }

    public void setImdoubleMethod(double imdouble) {
        this.imdouble = imdouble + E_DOUBLE;
    }

    public void setImfloatMethod(float imfloat) {
        this.imfloat = imfloat + E_FLOAT;
    }
}