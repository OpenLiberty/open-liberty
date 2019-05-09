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
public class StatefulDefInterceptorBean {
    private final static String CLASSNAME = StatefulDefInterceptorBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final String[] EXPECTED_RESULTS_REMOTE = new String[] {
                                                                           "StatefulDefInterceptorBean.construct",
                                                                           "DefNonSerInterceptor.prePassivate:Default",
                                                                           "StatefulDefInterceptorBean.passivate",
                                                                           "DefNonSerInterceptor.postActivate:Default",
                                                                           "StatefulDefInterceptorBean.activate",
                                                                           "DefNonSerInterceptor.aroundInvoke:Remote",
                                                                           "DefNonSerInterceptor.prePassivate:Remote",
                                                                           "StatefulDefInterceptorBean.passivate",
                                                                           "DefNonSerInterceptor.postActivate:Remote",
                                                                           "StatefulDefInterceptorBean.activate",
                                                                           "DefNonSerInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL = new String[] {
                                                                          "StatefulDefInterceptorBean.construct",
                                                                          "DefNonSerInterceptor.prePassivate:Default",
                                                                          "StatefulDefInterceptorBean.passivate",
                                                                          "DefNonSerInterceptor.postActivate:Default",
                                                                          "StatefulDefInterceptorBean.activate",
                                                                          "DefNonSerInterceptor.aroundInvoke:Local",
                                                                          "DefNonSerInterceptor.prePassivate:Local",
                                                                          "StatefulDefInterceptorBean.passivate",
                                                                          "DefNonSerInterceptor.postActivate:Local",
                                                                          "StatefulDefInterceptorBean.activate",
                                                                          "DefNonSerInterceptor.aroundInvoke:Local" };

    private static final String[] EXPECTED_RESULTS_REMOTE_STATIC = new String[] {
                                                                                  "StatefulDefInterceptorBean.construct",
                                                                                  "CLNonSerStaticInterceptor.prePassivate:Default",
                                                                                  "StatefulDefInterceptorBean.passivate",
                                                                                  "CLNonSerStaticInterceptor.postActivate:Default",
                                                                                  "StatefulDefInterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Remote",
                                                                                  "CLNonSerStaticInterceptor.prePassivate:Remote",
                                                                                  "StatefulDefInterceptorBean.passivate",
                                                                                  "CLNonSerStaticInterceptor.postActivate:Remote",
                                                                                  "StatefulDefInterceptorBean.activate",
                                                                                  "CLNonSerStaticInterceptor.aroundInvoke:Remote" };

    private static final String[] EXPECTED_RESULTS_LOCAL_STATIC = new String[] {
                                                                                 "StatefulDefInterceptorBean.construct",
                                                                                 "CLNonSerStaticInterceptor.prePassivate:Default",
                                                                                 "StatefulDefInterceptorBean.passivate",
                                                                                 "CLNonSerStaticInterceptor.postActivate:Default",
                                                                                 "StatefulDefInterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Local",
                                                                                 "CLNonSerStaticInterceptor.prePassivate:Local",
                                                                                 "StatefulDefInterceptorBean.passivate",
                                                                                 "CLNonSerStaticInterceptor.postActivate:Local",
                                                                                 "StatefulDefInterceptorBean.activate",
                                                                                 "CLNonSerStaticInterceptor.aroundInvoke:Local" };

    private String strValue = "Default";

    @PostConstruct
    public void construct() {
        svLogger.info("StatefulDefInterceptorBean.construct was called");
        PassivationTracker.clearAll();
        PassivationTracker.addMessage("StatefulDefInterceptorBean.construct");
    }

    @PrePassivate
    public void passivate() {
        PassivationTracker.addMessage("StatefulDefInterceptorBean.passivate");
    }

    @PostActivate
    public void activate() {
        PassivationTracker.addMessage("StatefulDefInterceptorBean.activate");
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
    @ExcludeDefaultInterceptors
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
