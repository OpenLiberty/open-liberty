/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb;

import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Stateless(name = "AutoCreatedTimerNIBean")
@Local(AutoCreatedTimerNI.class)
public class AutoCreatedTimerNIBean {
    private static final String CLASS_NAME = AutoCreatedTimerNIBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static boolean foundTheTimer = false;
    public static boolean infoWasNull = false;

    @Resource
    private TimerService ivTS;

    public Properties getTimerData(String infoToMatchOn) {
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer oneTimer : timers) {
            foundTheTimer = true;

            String info = (String) oneTimer.getInfo();
            svLogger.info("The info object for the 'noInfoTimer' is: **" + info + "**");
            infoWasNull = (info == null) ? true : false;

            break;
        }
        //For this bean, there should only be one timer, and we don't care about its schedule data, so we always return null.
        return null;
    }

    @Schedule(hour = "5", persistent = false)
    public void noInfoTimer(Timer timer) {
        svLogger.info("The noInfoTimer method got called back into.");
    }

    public void clearAllTimers() {
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

}
