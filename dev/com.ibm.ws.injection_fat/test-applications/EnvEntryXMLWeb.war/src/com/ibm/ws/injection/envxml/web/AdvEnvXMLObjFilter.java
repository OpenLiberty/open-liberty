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

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class AdvEnvXMLObjFilter implements Filter {
    private static final String CLASS_NAME = AdvEnvXMLObjFilter.class.getName();
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

    public AdvEnvXMLObjFilter() {
        map = new HashMap<String, Object>();
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse sp, FilterChain fc) throws IOException, ServletException {
        if (sr.getParameter("testMethod").equals("testEnvXMLObjServletFilter")) {
            svLogger.info("Testing in doFilter...");
            populateMap();
            EnvXMLObjTestHelper.processRequest(CLASS_NAME, WCEventTracker.KEY_FILTER_DOFILTER_AdvEnvXMLObjFilter, map);
        }
        fc.doFilter(sr, sp);
    }

    @Override
    public void destroy() {
        // Do Nothing
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // Do Nothing
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