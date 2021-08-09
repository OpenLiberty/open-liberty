/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import static junit.framework.Assert.assertEquals;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

@Local(StatefulCLInterceptorLocal.class)
@Remote(StatefulCLInterceptorRemote.class)
@Stateful
@Interceptors({ CLSerInterceptor.class })
// CLNonSerInterceptor is also defined via XML
@ExcludeDefaultInterceptors
public class StatefulNonSerCL2InterceptorBean {
    private final static String CLASSNAME = StatefulNonSerCL2InterceptorBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final String[] EXPECTED_RESULTS_REMOTE = new String[] {
                                                                           "StatefulNonSerCL2InterceptorBean.construct",
                                                                           "CLNonSerInterceptor.prePassivate:Default",
                                                                           "CLSerInterceptor.prePassivate:Default",
                                                                           "StatefulNonSerCL2InterceptorBean.passivate",
                                                                           "CLNonSerInterceptor.postActivate:Default",
                                                                           "CLSerInterceptor.postActivate:Default",
                                                                           "StatefulNonSerCL2InterceptorBean.activate",
                                                                           "CLNonSerInterceptor.aroundInvoke:Remote",
                                                                           "CLSerInterceptor.aroundInvoke:Remote",
                                                                           "CLNonSerInterceptor.prePassivate:Remote",
                                                                           "CLSerInterceptor.prePassivate:Remote",
                                                                           "StatefulNonSerCL2InterceptorBean.passivate",
                                                                           "CLNonSerInterceptor.postActivate:Remote",
                                                                           "CLSerInterceptor.postActivate:Remote",
                                                                           "StatefulNonSerCL2InterceptorBean.activate",
                                                                           "CLNonSerInterceptor.aroundInvoke:Remote",
                                                                           "CLSerInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL = new String[] {
                                                                          "StatefulNonSerCL2InterceptorBean.construct",
                                                                          "CLNonSerInterceptor.prePassivate:Default",
                                                                          "CLSerInterceptor.prePassivate:Default",
                                                                          "StatefulNonSerCL2InterceptorBean.passivate",
                                                                          "CLNonSerInterceptor.postActivate:Default",
                                                                          "CLSerInterceptor.postActivate:Default",
                                                                          "StatefulNonSerCL2InterceptorBean.activate",
                                                                          "CLNonSerInterceptor.aroundInvoke:Local",
                                                                          "CLSerInterceptor.aroundInvoke:Local",
                                                                          "CLNonSerInterceptor.prePassivate:Local",
                                                                          "CLSerInterceptor.prePassivate:Local",
                                                                          "StatefulNonSerCL2InterceptorBean.passivate",
                                                                          "CLNonSerInterceptor.postActivate:Local",
                                                                          "CLSerInterceptor.postActivate:Local",
                                                                          "StatefulNonSerCL2InterceptorBean.activate",
                                                                          "CLNonSerInterceptor.aroundInvoke:Local",
                                                                          "CLSerInterceptor.aroundInvoke:Local" };

    private static final String[] EXPECTED_RESULTS_REMOTE_STATIC = new String[] {
                                                                                  "StatefulNonSerCL2InterceptorBean.construct",
                                                                                  "CLNonSerInterceptor.prePassivate:Default",
                                                                                  "CLSerInterceptor.prePassivate:Default",
                                                                                  "StatefulNonSerCL2InterceptorBean.passivate",
                                                                                  "CLNonSerInterceptor.postActivate:Default",
                                                                                  "CLSerInterceptor.postActivate:Default",
                                                                                  "StatefulNonSerCL2InterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Default",
                                                                                  "CLNonSerInterceptor.prePassivate:Default",
                                                                                  "CLSerInterceptor.prePassivate:Default",
                                                                                  "StatefulNonSerCL2InterceptorBean.passivate",
                                                                                  "CLNonSerInterceptor.postActivate:Default",
                                                                                  "CLSerInterceptor.postActivate:Default",
                                                                                  "StatefulNonSerCL2InterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL_STATIC = new String[] {
                                                                                 "StatefulNonSerCL2InterceptorBean.construct",
                                                                                 "CLNonSerInterceptor.prePassivate:Default",
                                                                                 "CLSerInterceptor.prePassivate:Default",
                                                                                 "StatefulNonSerCL2InterceptorBean.passivate",
                                                                                 "CLNonSerInterceptor.postActivate:Default",
                                                                                 "CLSerInterceptor.postActivate:Default",
                                                                                 "StatefulNonSerCL2InterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Default",
                                                                                 "CLNonSerInterceptor.prePassivate:Default",
                                                                                 "CLSerInterceptor.prePassivate:Default",
                                                                                 "StatefulNonSerCL2InterceptorBean.passivate",
                                                                                 "CLNonSerInterceptor.postActivate:Default",
                                                                                 "CLSerInterceptor.postActivate:Default",
                                                                                 "StatefulNonSerCL2InterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Local" };

    private String strValue = "Default";

    @PostConstruct
    public void construct() {
        svLogger.info("StatefulNonSerCL2InterceptorBean.construct was called");
        PassivationTracker.clearAll();
        PassivationTracker.addMessage("StatefulNonSerCL2InterceptorBean.construct");
    }

    @PrePassivate
    public void passivate() {
        PassivationTracker.addMessage("StatefulNonSerCL2InterceptorBean.passivate");
    }

    @PostActivate
    public void activate() {
        PassivationTracker.addMessage("StatefulNonSerCL2InterceptorBean.activate");
    }

    public void interceptorStart(String key) {
        strValue = key;
    }

    public void interceptorEnd(String key) {
        assertEquals("Comparing passivated value to expected value: " + strValue, key, strValue);

        if (key.equals("Remote")) {
            PassivationTracker.compareMessages(EXPECTED_RESULTS_REMOTE);
        } else {
            PassivationTracker.compareMessages(EXPECTED_RESULTS_LOCAL);
        }

    }

    @ExcludeClassInterceptors
    @Interceptors({ CLNonSerStaticInterceptor.class })
    public void interceptorStaticStart(String key) {
        strValue = key;
    }

    @ExcludeClassInterceptors
    @Interceptors({ CLNonSerStaticInterceptor.class })
    public void interceptorStaticEnd(String key) {
        assertEquals("Comparing passivated value to expected value: " + strValue, key, strValue);

        if (key.equals("Remote")) {
            PassivationTracker.compareMessages(EXPECTED_RESULTS_REMOTE_STATIC);
        } else {
            PassivationTracker.compareMessages(EXPECTED_RESULTS_LOCAL_STATIC);
        }

    }

    @Remove
    public void finish() {
    }
}
