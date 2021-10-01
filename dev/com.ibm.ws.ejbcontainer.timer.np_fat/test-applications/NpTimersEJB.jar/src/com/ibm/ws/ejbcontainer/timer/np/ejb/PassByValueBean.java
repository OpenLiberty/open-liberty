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

import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class PassByValueBean {
    @Resource
    private TimerService ivTimerService;
    @Resource
    private SessionContext ivContext;

    private Test ivTest;
    @SuppressWarnings("rawtypes")
    private Exchanger ivExchanger;

    private PassByValueBean getBusinessObject() {
        return ivContext.getBusinessObject(PassByValueBean.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void test(Test test, boolean persistent) {
        ivTest = test;
        ivExchanger = new Exchanger();
        TimerData data = getBusinessObject().createTimer(persistent);

        ivTest.check("initial", new TimerData(data.ivTimer));

        try {
            // Wait for the timeout method to be called.  Send a dummy string
            // and receive TimerData from the timeout method.
            TimerData timeoutData1 = (TimerData) ivExchanger.exchange("test1", 30, TimeUnit.SECONDS);
            ivTest.check("timeout #1", timeoutData1);

            // Notify the timeout method to continue.
            ivExchanger.exchange(true, 30, TimeUnit.SECONDS);

            // Wait for the timeout method to be called again.  Send a dummy
            // string and receive a TimerData from the timeout method.
            TimerData timeoutData2 = (TimerData) ivExchanger.exchange("test2", 30, TimeUnit.SECONDS);
            ivTest.check("timeout #2", timeoutData2);

            // Notify the timeout method to cancel.
            ivExchanger.exchange(false, 30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public TimerData createTimer(boolean persistent) {
        ArrayList<String> info = new ArrayList<String>();
        ScheduleExpression schedule = new ScheduleExpression().second("*").minute("*").hour("*");
        Timer timer = ivTimerService.createCalendarTimer(schedule, new TimerConfig(info, persistent));

        TimerData data = new TimerData(timer, info, schedule);
        ivTest.modify(data);
        ivTest.check("uncommitted", new TimerData(timer));

        return data;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void clearAllTimers() {
        for (Timer timer : ivTimerService.getTimers()) {
            try {
                timer.cancel();
            } catch (NoSuchObjectLocalException ex) {
                Logger.getLogger(getClass().getName()).info("Ignoring exception; timer destroyed after final expiration : " + ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Timeout
    private void timeout(Timer timer) {
        try {
            // Notify the test method to begin testing.
            TimerData data = new TimerData(timer);
            ivExchanger.exchange(data, 30, TimeUnit.SECONDS);

            // Wait for the test method to finish testing, and then modify the
            // data for the next timeout or cancel self if test is over.
            if ((Boolean) ivExchanger.exchange("timeout", 30, TimeUnit.SECONDS)) {
                ivTest.modify(data);
            } else {
                timer.cancel();
            }
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static enum Test {
        INFO {
            @Override
            void modify(TimerData data) {
                data.ivInfo.add("modified");
            }

            @Override
            void check(String description, TimerData data) {
                List<String> info = data.ivInfo;
                assertTrue(description + " info = " + info, info.isEmpty());
            }
        },

        SCHEDULE {
            @Override
            void modify(TimerData data) {
                data.ivSchedule.second("0/1");
            }

            @Override
            void check(String description, TimerData data) {
                String second = data.ivSchedule.getSecond();
                assertTrue(description + " schedule = " + second, second.equals("*"));
            }
        };

        abstract void modify(TimerData data);

        abstract void check(String description, TimerData data);
    }

    private static class TimerData {
        Timer ivTimer;
        ArrayList<String> ivInfo;
        ScheduleExpression ivSchedule;

        @SuppressWarnings("unchecked")
        public TimerData(Timer timer) {
            this(timer, (ArrayList<String>) timer.getInfo(), timer.getSchedule());
        }

        public TimerData(Timer timer, ArrayList<String> info, ScheduleExpression schedule) {
            ivTimer = timer;
            ivInfo = info;
            ivSchedule = schedule;
        }
    }
}
