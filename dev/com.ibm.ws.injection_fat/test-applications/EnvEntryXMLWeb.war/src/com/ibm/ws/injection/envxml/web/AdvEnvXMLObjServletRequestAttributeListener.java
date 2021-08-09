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

import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;

public class AdvEnvXMLObjServletRequestAttributeListener implements ServletRequestAttributeListener {
    private static final String CLASS_NAME = AdvEnvXMLObjServletRequestAttributeListener.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    HashMap<String, Object> map;

    // Expected Injected Value Constants as defined in the XML
    private static final String E_STRING = "uebrigens";
    private static final Byte E_BYTE = 1;
    private static final Short E_SHORT = 1;
    private static final Integer E_INTEGER = 5;
    private static final Long E_LONG = 100L;
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

    public AdvEnvXMLObjServletRequestAttributeListener() {
        map = new HashMap<String, Object>();
    }

    @Override
    public void attributeAdded(ServletRequestAttributeEvent arg0) {
        svLogger.info("Obj Servlet Request: Attribute added...");
        populateMap();
        EnvXMLObjTestHelper.processRequest(CLASS_NAME, WCEventTracker.KEY_LISTENER_ADD_AdvEnvXMLObjServletRequestAttributeListener, map);
    }

    @Override
    public void attributeRemoved(ServletRequestAttributeEvent arg0) {
        svLogger.info("Obj Servlet Request: Attribute removed...");
        populateMap();
        EnvXMLObjTestHelper.processRequest(CLASS_NAME, WCEventTracker.KEY_LISTENER_DEL_AdvEnvXMLObjServletRequestAttributeListener, map);
    }

    @Override
    public void attributeReplaced(ServletRequestAttributeEvent arg0) {
        svLogger.info("Obj Servlet Request: Attribute replaced...");
        populateMap();
        EnvXMLObjTestHelper.processRequest(CLASS_NAME, WCEventTracker.KEY_LISTENER_REP_AdvEnvXMLObjServletRequestAttributeListener, map);
    }

    public void populateMap() {
        map.clear();
        map.put("ifString", ifString);
        map.put("ifCharacter", ifCharacter);
        map.put("ifByte", ifByte);
        map.put("ifShort", ifShort);
        map.put("ifInteger", ifInteger);
        map.put("ifLong", ifLong);
        map.put("ifBoolean", ifBoolean);
        map.put("ifDouble", ifDouble);
        map.put("ifFloat", ifFloat);

        map.put("imString", imString);
        map.put("imCharacter", imCharacter);
        map.put("imByte", imByte);
        map.put("imShort", imShort);
        map.put("imInteger", imInteger);
        map.put("imLong", imLong);
        map.put("imBoolean", imBoolean);
        map.put("imDouble", imDouble);
        map.put("imFloat", imFloat);
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