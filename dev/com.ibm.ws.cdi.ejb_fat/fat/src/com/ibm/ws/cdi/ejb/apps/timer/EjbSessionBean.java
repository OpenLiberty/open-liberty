/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.ws.cdi.ejb.apps.timer.view.EjbSessionBean2Local;
import com.ibm.ws.cdi.ejb.apps.timer.view.EjbSessionBeanLocal;

/**
 * Session Bean implementation class EjbSessionBean
 */
@Stateless
@Local(EjbSessionBeanLocal.class)
@LocalBean
public class EjbSessionBean implements EjbSessionBeanLocal {
    @Resource
    TimerService timerService;
    @Inject
    SessionScopedCounter sesCounter;
    @EJB
    EjbSessionBean2Local bean2;

    private static final long TIMEOUT = 1;

    /**
     * Default constructor.
     */
    public EjbSessionBean() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getStack() {
        return sesCounter.getStack();
    }

    @Override
    public int getSesCount() {
        return sesCounter.get();
    }

    @Override
    public void incCounters() {
        incRequestCounter();
        sesCounter.increment();
    }

    @Timeout
    void ping(Timer timer) {
        System.out.println("Timeout occurred");
        incRequestCounter();
    }

    @Override
    public void incRequestCounter() {
        bean2.incCount();
    }

    @PostConstruct
    void postConstruct() {
        System.out.println(String.format("%s@%08x created", this.getClass().getSimpleName(), System.identityHashCode(this)));
    }

    @PreDestroy
    void preDestroy() {
        System.out.println(String.format("%s@%08x destroyed", this.getClass().getSimpleName(), System.identityHashCode(this)));
    }

    @Override
    public int getReqCount() {
        return bean2.getCount();
    }

    @Override
    public void incCountersViaTimer() {
        System.out.println("CL: " + timerService.getClass().getClassLoader());
        TimerConfig config = new TimerConfig();
        config.setPersistent(false);
        timerService.createSingleActionTimer(TIMEOUT, config);
        sesCounter.increment();
    }
}
