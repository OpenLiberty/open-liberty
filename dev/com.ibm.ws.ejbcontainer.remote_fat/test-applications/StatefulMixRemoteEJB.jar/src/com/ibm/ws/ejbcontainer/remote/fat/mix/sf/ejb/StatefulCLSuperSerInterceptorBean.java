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
@Interceptors({ SuperCLSerInterceptor.class })
@ExcludeDefaultInterceptors
public class StatefulCLSuperSerInterceptorBean {
    private final static String CLASSNAME = StatefulCLSuperSerInterceptorBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final String[] EXPECTED_RESULTS_REMOTE = new String[] {
                                                                           "StatefulCLSuperInterceptorBean.construct",
                                                                           "BaseCLSerInterceptor.prePassivate:Default",
                                                                           "SuperCLSerInterceptor.prePassivate:Default",
                                                                           "StatefulCLSuperInterceptorBean.passivate",
                                                                           "BaseCLSerInterceptor.postActivate:Default",
                                                                           "SuperCLSerInterceptor.postActivate:Default",
                                                                           "StatefulCLSuperInterceptorBean.activate",
                                                                           "BaseCLSerInterceptor.aroundInvoke:Remote",
                                                                           "SuperCLSerInterceptor.aroundInvoke:Remote",
                                                                           "BaseCLSerInterceptor.prePassivate:Remote",
                                                                           "SuperCLSerInterceptor.prePassivate:Remote",
                                                                           "StatefulCLSuperInterceptorBean.passivate",
                                                                           "BaseCLSerInterceptor.postActivate:Remote",
                                                                           "SuperCLSerInterceptor.postActivate:Remote",
                                                                           "StatefulCLSuperInterceptorBean.activate",
                                                                           "BaseCLSerInterceptor.aroundInvoke:Remote",
                                                                           "SuperCLSerInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL = new String[] {
                                                                          "StatefulCLSuperInterceptorBean.construct",
                                                                          "BaseCLSerInterceptor.prePassivate:Default",
                                                                          "SuperCLSerInterceptor.prePassivate:Default",
                                                                          "StatefulCLSuperInterceptorBean.passivate",
                                                                          "BaseCLSerInterceptor.postActivate:Default",
                                                                          "SuperCLSerInterceptor.postActivate:Default",
                                                                          "StatefulCLSuperInterceptorBean.activate",
                                                                          "BaseCLSerInterceptor.aroundInvoke:Local",
                                                                          "SuperCLSerInterceptor.aroundInvoke:Local",
                                                                          "BaseCLSerInterceptor.prePassivate:Local",
                                                                          "SuperCLSerInterceptor.prePassivate:Local",
                                                                          "StatefulCLSuperInterceptorBean.passivate",
                                                                          "BaseCLSerInterceptor.postActivate:Local",
                                                                          "SuperCLSerInterceptor.postActivate:Local",
                                                                          "StatefulCLSuperInterceptorBean.activate",
                                                                          "BaseCLSerInterceptor.aroundInvoke:Local",
                                                                          "SuperCLSerInterceptor.aroundInvoke:Local" };

    private static final String[] EXPECTED_RESULTS_REMOTE_STATIC = new String[] {
                                                                                  "StatefulCLSuperInterceptorBean.construct",
                                                                                  "CLNonSerStaticInterceptor.prePassivate:Default",
                                                                                  "StatefulCLSuperInterceptorBean.passivate",
                                                                                  "CLNonSerStaticInterceptor.postActivate:Default",
                                                                                  "StatefulCLSuperInterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Remote",
                                                                                  "CLNonSerStaticInterceptor.prePassivate:Remote",
                                                                                  "StatefulCLSuperInterceptorBean.passivate",
                                                                                  "CLNonSerStaticInterceptor.postActivate:Remote",
                                                                                  "StatefulCLSuperInterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL_STATIC = new String[] {
                                                                                 "StatefulCLSuperInterceptorBean.construct",
                                                                                 "CLNonSerStaticInterceptor.prePassivate:Default",
                                                                                 "StatefulCLSuperInterceptorBean.passivate",
                                                                                 "CLNonSerStaticInterceptor.postActivate:Default",
                                                                                 "StatefulCLSuperInterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Local",
                                                                                 "CLNonSerStaticInterceptor.prePassivate:Local",
                                                                                 "StatefulCLSuperInterceptorBean.passivate",
                                                                                 "CLNonSerStaticInterceptor.postActivate:Local",
                                                                                 "StatefulCLSuperInterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Local" };

    private String strValue = "Default";

    @PostConstruct
    public void construct() {
        svLogger.info("StatefulCLSuperInterceptorBean.construct was called");
        PassivationTracker.clearAll();
        PassivationTracker.addMessage("StatefulCLSuperInterceptorBean.construct");
    }

    @PrePassivate
    public void passivate() {
        PassivationTracker.addMessage("StatefulCLSuperInterceptorBean.passivate");
    }

    @PostActivate
    public void activate() {
        PassivationTracker.addMessage("StatefulCLSuperInterceptorBean.activate");
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
