/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.core.ejb;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

@SuppressWarnings("serial")
public class LateWarningBean implements SessionBean, TimedObject {

    private SessionContext ivContext;

    private static boolean svHasSlept = false;

    public void createIntervalTimer(long intervalDuration, long threshold) {
        try {

            TimerConfig timerConfig = new TimerConfig("Interval: PersistentTimerCoreEJB: LateWarningBean:" + threshold, true);
            TimerService timerService = ivContext.getTimerService();
            timerService.createIntervalTimer(0l, intervalDuration, timerConfig);

        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void cancelIntervalTimer() {

        TimerService timerService = ivContext.getTimerService();
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

    public void ejbCreate() throws CreateException {
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {

    }

    @Override
    public void setSessionContext(SessionContext sc) throws EJBException, RemoteException {
        ivContext = sc;
    }

    @Override
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
