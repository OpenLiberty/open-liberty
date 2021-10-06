/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.config.late.ejb;

import java.util.Collection;

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

    @Override
    public void createIntervalTimer(long intervalDuration, long threshold) {
        try {
            TimerConfig timerConfig = new TimerConfig("Interval: NpTimerConfigLateWarningEJB: LateWarningBean:" + threshold, false);
            timerService.createIntervalTimer(0l, intervalDuration, timerConfig);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Override
    public void cancelIntervalTimer() {

        Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException nso) {

            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }
        }
        svHasSlept = false;
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
        }
    }

}
