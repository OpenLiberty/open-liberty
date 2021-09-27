/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.v32.shared;

import java.util.Collection;
import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.ejb.TimerConfig;

/**
 * Bean implementation for a basic session bean that implements
 * a timeout callback method. It contains methods to test the TimerService
 * getAllTimers API. <p>
 **/
public abstract class AbstractTimerTestBean extends AbstractTestBean implements TimerTestBean {
    private static final Logger logger = Logger.getLogger(AbstractTimerTestBean.class.getName());

    public static final long MAX_WAIT_TIME = 3 * 60 * 1000;

    @Override
    public void createTimers(int create, String info) {

        String module = (String) context.lookup("java:module/ModuleName");
        String timerInfo = module + " : " + info;

        for (int i = 0; i < create; i++) {
            TimerConfig timerConfig = new TimerConfig(timerInfo, false);
            Timer timer = timerService.createSingleActionTimer(DEFAULT_EXPIRATION, timerConfig);
            logger.info("Created timer = " + timer);
        }
    }

    @Override
    public void cancelTwoTimers() {

        boolean programmatic = false, automatic = false;

        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            String timerInfo = (String) timer.getInfo();
            if (timerInfo.startsWith("Automatic :")) {
                if (!automatic) {
                    timer.cancel();
                    automatic = true;
                    singletonTestBean.adjustExpectedAutomaticTimerCount(-1);
                }
            } else {
                if (!programmatic) {
                    timer.cancel();
                    programmatic = true;
                }
            }
            if (automatic && programmatic) {
                break;
            }
        }
        if (!automatic || !programmatic) {
            throw new IllegalStateException("Timers not found : automatic = " + automatic + ", programmatic = " + programmatic);
        }
    }
}
