/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.timer.auto.np.ejb;

import static jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Local;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;

@Singleton
@Local(AutoCreatedTimerDriver.class)
public class AutoCreatedTimerDriverBean implements AutoCreatedTimerDriver {
    private static final String CLASS_NAME = AutoCreatedTimerDriverBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static String COUNT_KEY = "count";
    public static String TIMESTAMP_KEY = "timestamps";
    public static String SCHEDULE_KEY = "scheduledExpression";
    public static String NEXT_TIMEOUT_KEY = "nextTimeout";

    public static String BEAN_A = "AutoCreatedTimerA";
    public static String BEAN_M = "AutoCreatedTimerM";

    private static CountDownLatch timersExpiredLatch = new CountDownLatch(3);

    public static void countDown(String timerInfo) {
        svLogger.info("countDown: Timer " + timerInfo + " has reached the required number of expirations");
        if (timersExpiredLatch.getCount() == 0) {
            throw new IllegalStateException("Too many Timers Expired");
        }
        timersExpiredLatch.countDown();
    }

    @Resource
    SessionContext ivContext;

    @EJB(name = "ejb/AutoCreatedTimerA", beanName = "AutoCreatedTimerABean")
    AutoCreatedTimerA ivAutoCreatedTimerA;

    @EJB(name = "ejb/AutoCreatedTimerM", beanName = "AutoCreatedTimerMBean")
    AutoCreatedTimerM ivAutoCreatedTimerM;

    @Override
    @TransactionAttribute(NOT_SUPPORTED) // prevents transaction from timing out after 2 minutes
    public void setup() {
        try {
            svLogger.info("Waiting for timers to expire; or 5 minutes");
            timersExpiredLatch.await(5, TimeUnit.MINUTES);
            if (timersExpiredLatch.getCount() != 0) {
                svLogger.info("Waiting for timers to expire 1 extra minute; likely a server pause delayed timers; still waiting on " + timersExpiredLatch.getCount());
                timersExpiredLatch.await(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            throw new EJBException("Unexpected exception waiting.", ex);
        }
        if (timersExpiredLatch.getCount() != 0) {
            throw new EJBException("Not all expected timers expired; still waiting on " + timersExpiredLatch.getCount());
        }
        svLogger.info("setup complete : Expected timers have all expired");
    }

    @Override
    public Properties getTimeoutResults(String resultsToGet) {
        svLogger.info("Getting results for **" + resultsToGet + "**");
        Properties props = new Properties();
        if (AutoCreatedTimerABean.FIRST_SCHEDULE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.first_schedule_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.first_schedule_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.first_schedule_next);
        } else if (AutoCreatedTimerABean.SECOND_SCHEDULE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.second_schedule_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.second_schedule_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.second_schedule_next);
        } else if (AutoCreatedTimerABean.THIRD_SCHEDULE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.third_schedule_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.third_schedule_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.third_schedule_next);
        } else if (AutoCreatedTimerABean.PROGRAMATIC_TIMEOUT.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.programatic_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.programatic_timestamps);
        } else {
            svLogger.info("Specified bean **" + resultsToGet + "** is not supported...");
        }
        return props;
    }

    @Override
    public Properties getTimerScheduleData(String beanWithTimer, String infoToMatchOn) throws Exception {
        svLogger.info("Getting timer schedule data from bean **" + beanWithTimer + "** for timer instance **" + infoToMatchOn + "**");
        if (BEAN_A.equals(beanWithTimer)) {
            return ivAutoCreatedTimerA.getTimerData(infoToMatchOn);
        } else if (BEAN_M.equals(beanWithTimer)) {
            return ivAutoCreatedTimerM.getTimerData(infoToMatchOn);
        } else {
            svLogger.info("Specified bean **" + beanWithTimer + "** does not support handing back timer data.");
            return null;
        }
    }

    @Override
    public void driveCreationOfProgramaticTimer() {
        ivAutoCreatedTimerA.createProgramaticTimer();
    }

    @Override
    public void waitForProgramaticTimer(long maxTimeToWait) {
        ivAutoCreatedTimerA.waitForProgramaticTimer(maxTimeToWait);
    }

    @Override
    public void clearAllTimers() {
        svLogger.info("AutoCreatedTimerDriverBean: Clearing all timers...");
        ivAutoCreatedTimerA.clearAllTimers();
        ivAutoCreatedTimerM.clearAllTimers();

        if (timersExpiredLatch.getCount() != 0) {
            throw new IllegalStateException("Not all Timers Expired: waiting on " + timersExpiredLatch.getCount());
        }
        svLogger.info("AutoCreatedTimerDriverBean: All timers cleared.");
    }

}
