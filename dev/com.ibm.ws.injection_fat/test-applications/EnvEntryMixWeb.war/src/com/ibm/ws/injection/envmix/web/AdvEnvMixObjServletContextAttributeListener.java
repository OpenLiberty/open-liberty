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

import java.util.HashMap;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;

@Resources(value = {
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_String", type = String.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Character", type = Character.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Byte", type = Byte.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Short", type = Short.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Integer", type = Integer.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Long", type = Long.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Boolean", type = Boolean.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Double", type = Double.class),
                     @Resource(name = "com.ibm.ws.injection.envmix.web.AdvEnvMixObjServletContextAttributeListener/JNDI_Class_Ann_Float", type = Float.class)
})
public class AdvEnvMixObjServletContextAttributeListener implements ServletContextAttributeListener {
    private static final String CLASS_NAME = AdvEnvMixObjServletContextAttributeListener.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    HashMap<String, Object> map;

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

    public AdvEnvMixObjServletContextAttributeListener() {
        map = new HashMap<String, Object>();
    }

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

    @Override
    public void attributeAdded(ServletContextAttributeEvent arg0) {
        svLogger.info("Obj Servlet Context: Attribute added...");
        doWork(WCEventTracker.KEY_LISTENER_ADD_AdvEnvMixObjContextAttributeListener);
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent arg0) {
        svLogger.info("Obj Servlet Context: Attribute removed...");
        doWork(WCEventTracker.KEY_LISTENER_DEL_AdvEnvMixObjContextAttributeListener);
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent arg0) {
        svLogger.info("Obj Servlet Context: Attribute replaced...");
        doWork(WCEventTracker.KEY_LISTENER_REP_AdvEnvMixObjContextAttributeListener);
    }

    public void doWork(String key) {
        populateMap();
        EnvMixObjTestHelper.processRequest(CLASS_NAME, key, map);
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

        // Despite the fact this looks like it is testing the expected values,
        // this is really testing JNDI lookup of class-level resources. The
        // expected values are there as place holders.
        map.put("JNDI_Class_Ann_String", E_STRING);
        map.put("JNDI_Class_Ann_Character", E_CHARACTER);
        map.put("JNDI_Class_Ann_Byte", E_BYTE);
        map.put("JNDI_Class_Ann_Short", E_SHORT);
        map.put("JNDI_Class_Ann_Integer", E_INTEGER);
        map.put("JNDI_Class_Ann_Long", E_LONG);
        map.put("JNDI_Class_Ann_Boolean", E_BOOL);
        map.put("JNDI_Class_Ann_Double", E_DOUBLE);
        map.put("JNDI_Class_Ann_Float", E_FLOAT);
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