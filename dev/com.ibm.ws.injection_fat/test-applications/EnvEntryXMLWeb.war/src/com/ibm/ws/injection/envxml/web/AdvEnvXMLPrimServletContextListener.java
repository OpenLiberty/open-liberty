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

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AdvEnvXMLPrimServletContextListener implements ServletContextListener {
    private static final String CLASS_NAME = AdvEnvXMLPrimServletContextListener.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Expected Injected Value Constants as defined in the XML
    private static final byte E_BYTE = 1;
    private static final short E_SHORT = 1;
    private static final int E_INTEGER = 5;
    private static final long E_LONG = 100L;
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

    public AdvEnvXMLPrimServletContextListener() {
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            svLogger.info("Error setting up the context");
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // Do Nothing
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        svLogger.info("Prim Serlvet Context: Context initialized...");
        processRequest(WCEventTracker.KEY_LISTENER_INIT_AdvEnvXMLPrimServletContextListener);
    }

    public void processRequest(String key) {
        // Test Field injection
        EnvXMLPrimTestHelper.testEnvXMLPrimInjection(CLASS_NAME, key, true, ifchar, ifbyte, ifshort, ifint, iflong, ifboolean, ifdouble, iffloat);
        // Test Method Injection
        EnvXMLPrimTestHelper.testEnvXMLPrimInjection(CLASS_NAME, key, false, imchar, imbyte, imshort, imint, imlong, imboolean, imdouble, imfloat);
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