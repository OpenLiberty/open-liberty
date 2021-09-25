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

import java.io.Serializable;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleHelper;
import com.ibm.ws.ejbcontainer.timer.np.shared.LifecycleIntf;

@Singleton
@Startup
public class LifecycleBean implements LifecycleIntf {
    @EJB
    private LifecycleHelperBean ivHelper;

    @PostConstruct
    public void postConstruct() {
        ivHelper.createTimer(0, INFO_POST_CONSTRUCT);
        LifecycleHelper.svPostConstructTimers = ivHelper.getTimers();

        FATHelper.sleep(DELAY);

        LifecycleHelper.svPostConstructTimerFired = LifecycleHelper.svTimerFired.get(INFO_POST_CONSTRUCT);
    }

    @Override
    public Date createTimer(long duration, Serializable info) {
        Timer timer = ivHelper.createTimer(duration, info);
        return timer.getNextTimeout();
    }

    @Override
    public void createTimerAsync(long duration, Serializable info) {
        ivHelper.createTimerAsync(duration, info);
    }

    @PreDestroy
    private void preDestroy() {
        // Try to create a timer (should not succeed).
        ivHelper.createPreDestroyTimer();

        // Get the timers.
        LifecycleHelper.svPreDestroyTimers = ivHelper.getTimers();
    }
}
