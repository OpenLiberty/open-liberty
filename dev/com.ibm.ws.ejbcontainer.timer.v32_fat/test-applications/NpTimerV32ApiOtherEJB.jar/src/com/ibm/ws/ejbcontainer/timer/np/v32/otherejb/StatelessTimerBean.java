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
package com.ibm.ws.ejbcontainer.timer.np.v32.otherejb;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;

import com.ibm.ws.ejbcontainer.timer.np.v32.shared.AbstractTimerTestBean;
import com.ibm.ws.ejbcontainer.timer.np.v32.shared.TimerTestBean;

/**
 * Bean implementation for a basic Stateless session bean that implements
 * a timeout callback method. It contains methods to test the TimerService
 * getAllTimers API. <p>
 **/
@Stateless
public class StatelessTimerBean extends AbstractTimerTestBean implements TimerTestBean {
    private static final Logger logger = Logger.getLogger(StatelessTimerBean.class.getName());

    private static final String module = "NpTimerV32ApiOtherEJB";

    private static CountDownLatch timerLatch = null;
    private static CountDownLatch[] timerIntervalLatches = null;

    @Schedules({
                 @Schedule(year = "2100", minute = "*/3", info = "Automatic : NpTimerV32ApiOtherEJB : Every 3 Minutes in 2100", persistent = false),
                 @Schedule(year = "2100", minute = "*/5", info = "Automatic : NpTimerV32ApiOtherEJB : Every 5 Minutes in 2100", persistent = false),
                 @Schedule(year = "2100", minute = "*/11", info = "Automatic : NpTimerV32ApiOtherEJB : Every 11 Minutes in 2100", persistent = false)
    })
    void automaticTimeout(Timer timer) {
        logger.info("Running Timer " + timer.getInfo());
    }

    @Timeout
    void timeout(Timer timer) {
        String timerInfo = (String) timer.getInfo();
        logger.info("Running Timer " + timerInfo);
        if (timerInfo.startsWith("SingleAction")) {
            timerLatch.countDown();
        } else if (timerInfo.startsWith("Interval")) {
            if (timerIntervalLatches[0].getCount() == 1) {
                timerIntervalLatches[0].countDown();
            } else {
                try {
                    timerIntervalLatches[1].await(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
                timer.cancel();
                timerIntervalLatches[2].countDown();
            }
        }
    }

    @Override
    public CountDownLatch createSingleActionTimer(String info) {

        timerLatch = new CountDownLatch(1);
        String timerInfo = "SingleAction : " + module + " : " + info;
        TimerConfig timerConfig = new TimerConfig(timerInfo, false);
        Timer timer = timerService.createSingleActionTimer(SINGLE_ACTION_EXPIRATION, timerConfig);
        logger.info("Created timer = " + timer);

        return timerLatch;
    }

    @Override
    public CountDownLatch[] createIntervalTimer(String info) {

        timerIntervalLatches = new CountDownLatch[] { new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1) };
        String timerInfo = "Interval : " + module + " : " + info;
        TimerConfig timerConfig = new TimerConfig(timerInfo, false);
        Timer timer = timerService.createIntervalTimer(SINGLE_ACTION_EXPIRATION, SINGLE_ACTION_EXPIRATION, timerConfig);
        logger.info("Created timer = " + timer);

        return timerIntervalLatches;
    }
}
