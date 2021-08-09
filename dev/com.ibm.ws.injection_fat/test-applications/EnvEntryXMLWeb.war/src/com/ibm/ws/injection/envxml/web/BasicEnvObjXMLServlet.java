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
@WebServlet("/BasicEnvObjXMLServlet")
public class BasicEnvObjXMLServlet extends FATServlet {
    private static final String CLASS_NAME = BasicEnvObjXMLServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants as defined in the XML
    private static final String E_STRING = "uebrigens";
    private static final Character E_CHARACTER = 'a';
    private static final Byte E_BYTE = 1;
    private static final Short E_SHORT = 1;
    private static final Integer E_INTEGER = 5;
    private static final Long E_LONG = 100L;
    private static final Boolean E_BOOL = true;
    private static final Double E_DOUBLE = 100.0D;
    private static final Float E_FLOAT = 100.0F;

    // Resources to be field injected via XML
    private String ifString;
    private Character ifCharacter;
    private Byte ifByte;
    private Short ifShort;
    private Integer ifInteger;
    private Long ifLong;
    private Boolean ifBoolean;
    private Double ifDouble;
    private Float ifFloat;

    // Resources to be method injected via XML
    private String imString;
    private Character imCharacter;
    private Byte imByte;
    private Short imShort;
    private Integer imInteger;
    private Long imLong;
    private Boolean imBoolean;
    private Double imDouble;
    private Float imFloat;

    private InitialContext initCtx;

    public BasicEnvObjXMLServlet() {
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests XML field injection of Object Environment Entries
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvObjFldXMLInjection() {
        assertEquals("The injected character was not the expected value", E_STRING, ifString);
        assertNotNull("Failed to lookup the String: \"ifString\"", lookup("ifString"));

        assertEquals("The injected character was not the expected value", E_CHARACTER, ifCharacter);
        assertNotNull("Failed to lookup the Character: \"ifCharacter\"", lookup("ifCharacter"));

        assertEquals("The injected byte was not the expected value", E_BYTE, ifByte);
        assertNotNull("Failed to lookup the Byte: \"ifByte\"", lookup("ifByte"));

        assertEquals("The injected short was not the expected value", E_SHORT, ifShort);
        assertNotNull("Failed to lookup the Short: \"ifShort\"", lookup("ifShort"));

        assertEquals("The injected integer was not the expected value", E_INTEGER, ifInteger);
        assertNotNull("Failed to lookup the Integer: \"ifInteger\"", lookup("ifInteger"));

        assertEquals("The injected long was not the expected value", E_LONG, ifLong);
        assertNotNull("Failed to lookup the Long: \"ifLong\"", lookup("ifLong"));

        assertEquals("The injected boolean was not the expected value", E_BOOL, ifBoolean);
        assertNotNull("Failed to lookup the Boolean: \"ifBoolean\"", lookup("ifBoolean"));

        assertEquals("The injected double was not the expected value", E_DOUBLE, ifDouble);
        assertNotNull("Failed to lookup the Double: \"ifDouble\"", lookup("ifDouble"));

        assertEquals("The injected float was not the expected value", E_FLOAT, ifFloat);
        assertNotNull("Failed to lookup the Float: \"ifFloat\"", lookup("ifFloat"));
    }

    /**
     * Tests XML method injection of Object Environment Entries
     * Test layout:
     * 1. Assert the injected item is of the expected value
     * 2. Assert the injected item CAN be looked up in Global namespace and that
     * that object is not null
     */
    @Test
    public void testEnvObjMthdXMLInjection() {
        assertEquals("The injected string was not the expected value", E_STRING, imString);
        assertNotNull("Failed to lookup the String: \"imString\"", lookup("imString"));

        assertEquals("The injected character was not the expected value", E_CHARACTER, imCharacter);
        assertNotNull("Failed to lookup the Character: \"imCharacter\"", lookup("imCharacter"));

        assertEquals("The injected byte was not the expected value", E_BYTE, imByte);
        assertNotNull("Failed to lookup the Byte: \"imByte\"", lookup("imByte"));

        assertEquals("The injected short was not the expected value", E_SHORT, imShort);
        assertNotNull("Failed to lookup the Short: \"imShort\"", lookup("imShort"));

        assertEquals("The injected integer was not the expected value", E_INTEGER, imInteger);
        assertNotNull("Failed to lookup the Integer: \"imInteger\"", lookup("imInteger"));

        assertEquals("The injected long was not the expected value", E_LONG, imLong);
        assertNotNull("Failed to lookup the Long: \"imLong\"", lookup("imLong"));

        assertEquals("The injected boolean was not the expected value", E_BOOL, imBoolean);
        assertNotNull("Failed to lookup the Boolean: \"imBoolean\"", lookup("imBoolean"));

        assertEquals("The injected double was not the expected value", E_DOUBLE, imDouble);
        assertNotNull("Failed to lookup the Double: \"imDouble\"", lookup("imDouble"));

        assertEquals("The injected float was not the expected value", E_FLOAT, imFloat);
        assertNotNull("Failed to lookup the Float: \"imFloat\"", lookup("imFloat"));
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

    public void setImStringMethod(String imString) {
        this.imString = imString + E_STRING;
    }

    public void setImCharacterMethod(char imCharacter) {
        this.imCharacter = imCharacter;
    }

    public void setImByteMethod(byte imByte) {
        this.imByte = (byte) (imByte + E_BYTE);
    }

    public void setImShortMethod(short imShort) {
        this.imShort = (short) (imShort + E_SHORT);
    }

    public void setImIntegerMethod(int imInteger) {
        this.imInteger = imInteger + E_INTEGER;
    }

    public void setImLongMethod(long imLong) {
        this.imLong = imLong + E_LONG;
    }

    public void setImBooleanMethod(boolean imBoolean) {
        this.imBoolean = imBoolean;
    }

    public void setImDoubleMethod(double imDouble) {
        this.imDouble = imDouble + E_DOUBLE;
    }

    public void setImFloatMethod(float imFloat) {
        this.imFloat = imFloat + E_FLOAT;
    }
}