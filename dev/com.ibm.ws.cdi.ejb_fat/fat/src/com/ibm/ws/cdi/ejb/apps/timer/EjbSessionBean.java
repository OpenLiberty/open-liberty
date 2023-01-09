/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.timer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import javax.enterprise.context.ContextNotActiveException;

/**
 * Session Bean implementation class EjbSessionBean
 */
@Stateless
@Local(EjbSessionBeanLocal.class)
@LocalBean
@MyCDIInterceptorBinding
public class EjbSessionBean implements EjbSessionBeanLocal {
    private static final long TIMEOUT = 1;

    @Resource
    TimerService timerService;

    @Inject RequestScopedBean rsb;

    @Inject SessionScopedBean ssb;

    private int requestScopedIndex = -1;
    public static Boolean canAccessRequestScope = null;
    public static Boolean seperateRequestScopes = null;
    public static Boolean sessionScopeInactive = null;


    @Timeout
    void ping(Timer timer) {
        //Test One. Is a request scope active in the context of an EJB timer?
        try {
            if (rsb.toString() != null) {
                canAccessRequestScope = true;
            } else {
                canAccessRequestScope = false;
            }
        } catch (ContextNotActiveException e) {
            canAccessRequestScope = false;
        }

        //Test two. Is it a different request scope to outside the timer?
        try {
            if (rsb.getIndex() != requestScopedIndex) {
                seperateRequestScopes = true;
            } else {
                seperateRequestScopes = false;
            }
        } catch (ContextNotActiveException e) {
            canAccessRequestScope = false;
        }

        //Test three. Is the session scope inactive in the context of an EJB timer?
        try {
            ssb.toString();
            sessionScopeInactive = false;
        } catch (ContextNotActiveException e) {
            sessionScopeInactive = true;
        }
    }

    @Override
    public void initTimer() {
        requestScopedIndex = rsb.getIndex();

        System.out.println("CL: " + timerService.getClass().getClassLoader());
        TimerConfig config = new TimerConfig();
        config.setPersistent(false);
        timerService.createSingleActionTimer(TIMEOUT, config);
    }
}
