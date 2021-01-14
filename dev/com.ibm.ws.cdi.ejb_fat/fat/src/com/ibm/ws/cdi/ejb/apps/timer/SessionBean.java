/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.timer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.ibm.ws.cdi.ejb.apps.timer.view.SessionBeanLocal;

/**
 *
 */
@Dependent
@Stateless
public class SessionBean implements SessionBeanLocal {
    private static final BlockingQueue<Object> updates = new ArrayBlockingQueue<Object>(1);

    static {
        queueUpdate();
    }

    private static final long TIMEOUT = 1;
    private String value;
    boolean contextException;

    @Resource
    TimerService timerService;

    @Inject
    ApplicationScopedCounter counter;

    @Inject
    RequestScopedBean bean;

    @Timeout
    void ping(Timer timer) {
        System.out.println("Timeout occurred in Session Bean");
        try {
            this.value = bean.getValue();
            counter.increment();
        } catch (ContextNotActiveException e) {
            this.contextException = true;
            this.value = ("FAILED ContextNotActiveException occured: " + e);
        }
        queueUpdate();
    }

    private static void queueUpdate() {
        do {
            try {
                updates.put(new Object());
                System.out.println("Update token added.");
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
    }

    @Override
    public String getValue() {
        try {
            Object token = updates.poll(2, TimeUnit.SECONDS);
            if (token == null) {
                return "ERROR Retrieval timeout reached before update token available.";
            }
            if (this.contextException) {
                return this.value;
            }
            return ("counter = " + counter.get());
        } catch (InterruptedException e) {
            return "ERROR: InterruptedException occurred: " + e;
        }
    }

    @Override
    public void setUpTimer() {
        System.out.println("CL: " + timerService.getClass().getClassLoader());

        TimerConfig config = new TimerConfig();
        config.setPersistent(false);

        timerService.createSingleActionTimer(TIMEOUT, config);
    }
}
