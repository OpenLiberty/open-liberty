/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.lifecycle.ejb;

import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.INFO_POST_CONSTRUCT;
import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.INFO_PRE_DESTROY;
import static com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf.MAX_WAIT;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleHelper;

/**
 * Manages timers for LifecycleBean. We use a separate bean component because
 * LifecycleBean cannot loop back on itself while postConstruct and preDestroy
 * are executing.
 */
@Stateless
public class LifecycleHelperBean {
    private static final Logger svLogger = Logger.getLogger(LifecycleHelperBean.class.getName());

    @Resource
    private TimerService ivTimerService;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Timer createTimer(long duration, Serializable info) {
        Timer timer = ivTimerService.createSingleActionTimer(duration, new TimerConfig(info, false));
        return timer;
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createTimerAsync(long duration, Serializable info) {
        ivTimerService.createSingleActionTimer(duration, new TimerConfig(info, false));

        try {
            LifecycleHelper.svAsyncCreateTimerLatch.await(MAX_WAIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createPreDestroyTimer() {
        try {
            LifecycleHelper.svPreDestroyTimerCreated = createTimer(0, INFO_PRE_DESTROY);
        } catch (EJBException ex) {
            svLogger.log(Level.FINEST, "expected exception", ex);
        }
    }

    public Collection<Timer> getTimers() {
        return ivTimerService.getTimers();
    }

    @Timeout
    private void timeout(Timer timer) {
        Serializable info = timer.getInfo();
        LifecycleHelper.svTimerFired.put(info, new Date());
        if (INFO_POST_CONSTRUCT.equals(info)) {
            LifecycleHelper.svPostConstructTimerLatch.countDown();
        }
    }
}
