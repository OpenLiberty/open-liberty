/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.timer.np.config.late.ejb;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

@Singleton
public class LateWarningBean implements LateWarning {

    @Resource
    private TimerService timerService;

    private static boolean svHasSlept = false;
    private static CountDownLatch CancelLatch = null;

    @Override
    public void createIntervalTimer(long intervalDuration, long threshold) {
        svHasSlept = false;
        CancelLatch = new CountDownLatch(1);
        try {
            TimerConfig timerConfig = new TimerConfig("Interval: NpTimerConfigLateWarningEJB: LateWarningBean:" + threshold, false);
            timerService.createIntervalTimer(0l, intervalDuration, timerConfig);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Override
    public void cancelIntervalTimer() {
        // Timer will cancel itself on second run; allow some time for timeout to complete
        CountDownLatch cancelLatch = CancelLatch;
        if (cancelLatch != null) {
            try {
                cancelLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.out);
            }
        }

        // Cleanup any timers that are still around, just in case.
        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException nso) {

            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    @Timeout
    public void ejbTimeout(Timer timer) {
        if (!svHasSlept) {
            String info = (String) timer.getInfo();
            String[] tokens = info.split(":");
            long threshold = Long.parseLong(tokens[tokens.length - 1]);
            FATHelper.sleep(threshold + 10 * 1000L);
            svHasSlept = true;
        } else {
            timer.cancel();
            CancelLatch.countDown();
        }
    }

}
