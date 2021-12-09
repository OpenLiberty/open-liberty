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
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

@Stateless
@Local
public class SimpleTimerBean implements SimpleTimerLocal {

    private final static String CLASSNAME = SimpleTimerBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @Resource
    private TimerService ivTS;

    @Timeout
    public void timeout(Timer timer) {
        svLogger.logp(Level.INFO, CLASSNAME, "timeout", "timer = {0}", timer);
    }

    @Override
    public Timer createTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "createTimer", info);
        }

        Timer t = TimerHelper.createTimer(ivTS, 30 * 1000l, null, info, false, null);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "createTimer", t);
        }

        return t;
    }

    @Override
    public Collection<Timer> getTimers() {
        Collection<Timer> timers = ivTS.getTimers();

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "getTimers", "Timers: {0}", timers);
        }

        return timers;
    }

    @Override
    public Collection<String> getInfoOfAllTimers() {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "getInfoOfAllTimers");
        }

        Collection<String> infos = new ArrayList<String>();
        Collection<Timer> timers = ivTS.getTimers();
        for (Timer t : timers) {
            infos.add((String) t.getInfo());
        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "getInfoOfAllTimers", infos);
        }

        return infos;
    }

    @Override
    public void clearAllTimers() {

        for (Object o : ivTS.getTimers()) {
            ((Timer) o).cancel();
        }
    }

}
