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
@Interceptors({ CLNonSerInterceptor.class })
@ExcludeDefaultInterceptors
public class StatefulCLInterceptorBean {
    private final static String CLASSNAME = StatefulCLInterceptorBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final String[] EXPECTED_RESULTS_REMOTE = new String[] {
                                                                           "StatefulCLInterceptorBean.construct",
                                                                           "CLNonSerInterceptor.prePassivate:Default",
                                                                           "StatefulCLInterceptorBean.passivate",
                                                                           "CLNonSerInterceptor.postActivate:Default",
                                                                           "StatefulCLInterceptorBean.activate",
                                                                           "CLNonSerInterceptor.aroundInvoke:Remote",
                                                                           "CLNonSerInterceptor.prePassivate:Remote",
                                                                           "StatefulCLInterceptorBean.passivate",
                                                                           "CLNonSerInterceptor.postActivate:Remote",
                                                                           "StatefulCLInterceptorBean.activate",
                                                                           "CLNonSerInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL = new String[] {
                                                                          "StatefulCLInterceptorBean.construct",
                                                                          "CLNonSerInterceptor.prePassivate:Default",
                                                                          "StatefulCLInterceptorBean.passivate",
                                                                          "CLNonSerInterceptor.postActivate:Default",
                                                                          "StatefulCLInterceptorBean.activate",
                                                                          "CLNonSerInterceptor.aroundInvoke:Local",
                                                                          "CLNonSerInterceptor.prePassivate:Local",
                                                                          "StatefulCLInterceptorBean.passivate",
                                                                          "CLNonSerInterceptor.postActivate:Local",
                                                                          "StatefulCLInterceptorBean.activate",
                                                                          "CLNonSerInterceptor.aroundInvoke:Local" };

    private static final String[] EXPECTED_RESULTS_REMOTE_STATIC = new String[] {
                                                                                  "StatefulCLInterceptorBean.construct",
                                                                                  "CLNonSerStaticInterceptor.prePassivate:Default",
                                                                                  "StatefulCLInterceptorBean.passivate",
                                                                                  "CLNonSerStaticInterceptor.postActivate:Default",
                                                                                  "StatefulCLInterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Remote",
                                                                                  "CLNonSerStaticInterceptor.prePassivate:Remote",
                                                                                  "StatefulCLInterceptorBean.passivate",
                                                                                  "CLNonSerStaticInterceptor.postActivate:Remote",
                                                                                  "StatefulCLInterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL_STATIC = new String[] {
                                                                                 "StatefulCLInterceptorBean.construct",
                                                                                 "CLNonSerStaticInterceptor.prePassivate:Default",
                                                                                 "StatefulCLInterceptorBean.passivate",
                                                                                 "CLNonSerStaticInterceptor.postActivate:Default",
                                                                                 "StatefulCLInterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Local",
                                                                                 "CLNonSerStaticInterceptor.prePassivate:Local",
                                                                                 "StatefulCLInterceptorBean.passivate",
                                                                                 "CLNonSerStaticInterceptor.postActivate:Local",
                                                                                 "StatefulCLInterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Local" };

    private String strValue = "Default";

    @PostConstruct
    public void construct() {
        svLogger.info("StatefulCLInterceptorBean.construct was called");
        PassivationTracker.clearAll();
        PassivationTracker.addMessage("StatefulCLInterceptorBean.construct");
    }

    @PrePassivate
    public void passivate() {
        PassivationTracker.addMessage("StatefulCLInterceptorBean.passivate");
    }

    @PostActivate
    public void activate() {
        PassivationTracker.addMessage("StatefulCLInterceptorBean.activate");
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
