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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Stateful;
import javax.ejb.Timer;

@Stateful
@Local
public class SimpleSFSBBean implements SimpleSFSBLocal {

    private final static String CLASSNAME = SimpleSFSBBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private List<Timer> ivAllTimers = new ArrayList<Timer>();
    private Timer ivCurrentTimer;
    private String ivCurrentTimerInfo;

    @EJB
    private SimpleTimerLocal ivTimerBean;

    private boolean ivPassivationFlag = false;

    @PrePassivate
    public void passivating() {
        svLogger.logp(Level.INFO, CLASSNAME, "passivating",
                      "Passivating {0}; ivCurrentTimer.getInfo() == {1}",
                      new Object[] { this, ivCurrentTimerInfo });
    }

    @PostActivate
    public void activating() {
        svLogger.logp(Level.INFO, CLASSNAME, "activating", "Activating " + this);
        svLogger.logp(Level.INFO, CLASSNAME, "activating",
                      "ivCurrentTimer = {0}, ivCurrentTimer.getInfo() = {1} ivAllTimers = {2}",
                      new Object[] { ivCurrentTimer, ivCurrentTimerInfo, ivAllTimers });
        ivPassivationFlag = true;
    }

    @Override
    public Timer cancelTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "cancelTimer", info);
        }

        if (info == null) {
            IllegalArgumentException iae = new IllegalArgumentException();
            svLogger.throwing(CLASSNAME, "cancelTimer", iae);
            throw iae;
        }

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "cancelTimer", "Timers in ivAllTimers:");
        }

        Timer timerToCancel = null;
        int i = 0;
        //for (Timer t : ivAllTimers) {
        for (Timer t : ivTimerBean.getTimers()) {
            if (svLogger.isLoggable(Level.FINEST)) {
                svLogger.logp(Level.FINEST, CLASSNAME, "cancelTimer", "\t:ivAllTimers( {0} ) == {1}",
                              new Object[] { i, t });
            }
            if (info.equals(t.getInfo())) {
                timerToCancel = t;

                break;
            }
        }

        if (timerToCancel != null) {
            ivAllTimers.remove(timerToCancel);
            if (ivCurrentTimer != null && ivCurrentTimer.equals(timerToCancel)) {
                ivCurrentTimer = null;
                ivCurrentTimerInfo = null;
            }
            timerToCancel.cancel();

        }

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "cancelTimer", timerToCancel);
        }

        return timerToCancel;
    }

    @Override
    public Timer createTimer(String info) {
        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.entering(CLASSNAME, "createTimer", info);
        }

        Timer t = ivTimerBean.createTimer(info);
        ivCurrentTimer = t;
        ivCurrentTimerInfo = info;
        ivAllTimers.add(t);

        if (svLogger.isLoggable(Level.FINER)) {
            svLogger.exiting(CLASSNAME, "createTimer", t);
        }

        return t;
    }

    @Override
    public Collection<Timer> getAllTimers() {
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "getAllTimers", "Timers = {0}", ivAllTimers);
        }

        //return ivAllTimers;
        return ivTimerBean.getTimers();
    }

    @Override
    public Timer getCurrentTimer() {
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "getCurrentTimer", "ivCurrentTimer = {0}", ivCurrentTimer);
        }
        return ivCurrentTimer;
    }

    @Override
    public String getCurrentTimerInfo() {
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "getCurrentTimerInfo", "ivCurrentTimerInfo = {0}", ivCurrentTimerInfo);
        }
        return ivCurrentTimerInfo;
    }

    @Override
    public boolean hasBeenPassivated() {
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "hasBeenPassivated", "ivPassivationFlag = {0}", ivPassivationFlag);
        }
        return ivPassivationFlag;
    }

    @Override
    public void resetPassivationFlag() {
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, "resetPassivationFlag", "ivPassivationFlag is now false, it was {0}", ivPassivationFlag);
        }
        ivPassivationFlag = false;
    }

    @Override
    public void clearAllTimers() {
        ivTimerBean.clearAllTimers();
    }
}
