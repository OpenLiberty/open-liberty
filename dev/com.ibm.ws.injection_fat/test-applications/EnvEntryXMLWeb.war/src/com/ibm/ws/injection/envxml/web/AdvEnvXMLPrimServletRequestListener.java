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
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

public class AdvEnvXMLPrimServletRequestListener implements ServletRequestListener {
    private static final String CLASS_NAME = AdvEnvXMLPrimServletRequestListener.class.getName();
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

    @SuppressWarnings("unused")
    private InitialContext initCtx;

    public AdvEnvXMLPrimServletRequestListener() {
        try {
            initCtx = new InitialContext();
        } catch (NamingException e) {
            svLogger.info("Error setting up the context");
            e.printStackTrace();
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        // Do Nothing
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        svLogger.info("Prim Servlet Request: Request Initialized...");
        processRequest(sre, WCEventTracker.KEY_LISTENER_INIT_AdvEnvXMLPrimServletRequestListener);
    }

    public void processRequest(ServletRequestEvent sre, String key) {
        String testName = sre.getServletRequest().getParameter("testMethod");
        if (testName != null && testName.toString().equals("testEnvXMLPrimRequestListener")) {
            // Test Field injection
            EnvXMLPrimTestHelper.testEnvXMLPrimInjection(CLASS_NAME, key, true, ifchar, ifbyte, ifshort, ifint, iflong, ifboolean, ifdouble, iffloat);
            // Test Method Injection
            EnvXMLPrimTestHelper.testEnvXMLPrimInjection(CLASS_NAME, key, false, imchar, imbyte, imshort, imint, imlong, imboolean, imdouble, imfloat);
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