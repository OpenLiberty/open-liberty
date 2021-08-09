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

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class AdvEnvAnnPrimFilter implements Filter {
    private static final String CLASS_NAME = AdvEnvAnnPrimFilter.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

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

    private final String[] fieldNames = { "ifchar", "ifbyte", "ifshort", "ifint", "iflong", "ifboolean", "ifdouble", "iffloat" };
    private final String[] methodNames = { "imchar", "imbyte", "imshort", "imint", "imlong", "imboolean", "imdouble", "imfloat" };

    @Override
    public void doFilter(ServletRequest sr, ServletResponse sp,
                         FilterChain fc) throws IOException, ServletException {
        if (sr.getParameter("testMethod").equals("testEnvAnnPrimServletFilter")) {
            svLogger.info("Testing in doFilter...");
            processRequest(WCEventTracker.KEY_FILTER_DOFILTER_AdvEnvAnnPrimFilter);
        }

        fc.doFilter(sr, sp);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // Do Nothing
    }

    @Override
    public void destroy() {
        // Do Nothing
    }

    public void processRequest(String key) {
        // Test Field injection
        EnvAnnPrimTestHelper.testEnvAnnPrimInjection(CLASS_NAME, key, ifchar, ifbyte, ifshort, ifint, iflong, ifboolean, ifdouble, iffloat, fieldNames);
        // Test Method Injection
        EnvAnnPrimTestHelper.testEnvAnnPrimInjection(CLASS_NAME, key, imchar, imbyte, imshort, imint, imlong, imboolean, imdouble, imfloat, methodNames);
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