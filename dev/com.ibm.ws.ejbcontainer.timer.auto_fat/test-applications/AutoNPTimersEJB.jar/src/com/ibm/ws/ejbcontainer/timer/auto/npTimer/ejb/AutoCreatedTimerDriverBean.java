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

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;

@Singleton
@Local(AutoCreatedTimerDriver.class)
public class AutoCreatedTimerDriverBean implements AutoCreatedTimerDriver {
    private static final String CLASS_NAME = AutoCreatedTimerDriverBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static String COUNT_KEY = "count";
    public static String TIMESTAMP_KEY = "timestamps";
    public static String SCHEDULE_KEY = "scheduledExpression";
    public static String NEXT_TIMEOUT_KEY = "nextTimeout";
    public static String TIMEZONE_KEY = "timezone"; // F743-16271

    public static String BEAN_A = "AutoCreatedTimerA";
    public static String BEAN_X = "AutoCreatedTimerX";
    public static String BEAN_M = "AutoCreatedTimerM";
    public static String BEAN_NI = "AutoCreatedTimerNI";
    public static String BEAN_SS = "AutoCreatedTimerSS";

    public static String GRANDCHILD_BEAN = "GrandchildBean";
    public static String CHILD_BEAN = "ChildBean";
    public static String PARENT_BEAN = "ParentBean";

    public static final String PARENT_BEAN_METHOD_ONE = "ParentBean.method_one";
    public static final String PARENT_BEAN_METHOD_TWO = "ParentBean.method_two";
    public static final String PARENT_BEAN_METHOD_THREE = "ParentBean.method_three";
    public static final String CHILD_BEAN_METHOD_ONE = "ChildBean.method_one";
    public static final String CHILD_BEAN_METHOD_TWO = "ChildBean.method_two";
    public static final String CHILD_BEAN_METHOD_THREE = "ChildBean.method_three";
    public static final String GRANDCHILD_BEAN_METHOD_THREE = "GrandchildBean.method_three";

    private static CountDownLatch timersExpiredLatch = new CountDownLatch(14);

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

    @EJB(name = "ejb/AutoCreatedTimerX", beanName = "AutoCreatedTimerXBean")
    AutoCreatedTimerX ivAutoCreatedTimerX;

    @EJB(name = "ejb/AutoCreatedTimerM", beanName = "AutoCreatedTimerMBean")
    AutoCreatedTimerM ivAutoCreatedTimerM;

    @EJB(name = "ejb/AutoCreatedTimerNI", beanName = "AutoCreatedTimerNIBean")
    AutoCreatedTimerNI ivAutoCreatedTimerNI;

    @EJB(name = "ejb/AutoCreatedTimerSS", beanName = "AutoCreatedTimerSSBean")
    AutoCreatedTimerSS ivAutoCreatedTimerSS;

    @EJB(name = "ejb/GrandchildBean", beanName = "GrandchildBean")
    GrandchildBean ivGrandchildBean;

    @EJB(name = "ejb/childBean", beanName = "ChildBean")
    ChildBean ivChildBean;

    @EJB(name = "ejb/parentBean", beanName = "ParentBean")
    ParentBean ivParentBean;

    @Override
    @TransactionAttribute(NOT_SUPPORTED) // prevents transaction from timing out after 2 minutes
    public void setup() {
        try {
            svLogger.info("Waiting for timers to expire; or 5 minutes");
            timersExpiredLatch.await(5, TimeUnit.MINUTES);
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
        if (AutoCreatedTimerABean.SECONDS_EXACT.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.seconds_exact_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.seconds_exact_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.seconds_exact_next);
        } else if (AutoCreatedTimerABean.SECONDS_INTERVAL.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.seconds_interval_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.seconds_interval_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.seconds_interval_next);
        } else if (AutoCreatedTimerABean.MINUTES_INTERVAL.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.minutes_interval_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.minutes_interval_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.minutes_interval_next);
        } else if (AutoCreatedTimerABean.MONTH_RANGE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.month_range_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.month_range_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.month_range_next);
        } else if (AutoCreatedTimerABean.RANGE_AND_LIST.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.range_and_list_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.range_and_list_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.range_and_list_next);
        } else if (AutoCreatedTimerABean.MULTIPLE_ATTRIBUTES_COMBINED.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerABean.multiple_attributes_combined_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerABean.multiple_attributes_combined_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerABean.multiple_attributes_combined_next);
        } else if (AutoCreatedTimerABean.FIRST_SCHEDULE.equals(resultsToGet)) {
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
        } else if (AutoCreatedTimerXBean.SECONDS_RANGE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerXBean.seconds_range_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerXBean.seconds_range_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerXBean.seconds_range_next);
        } else if (AutoCreatedTimerXBean.SECONDS_LIST.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerXBean.seconds_list_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerXBean.seconds_list_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerXBean.seconds_list_next);
        } else if (AutoCreatedTimerXBean.MINUTES_RANGE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerXBean.minutes_range_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerXBean.minutes_range_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerXBean.minutes_range_next);
        } else if (AutoCreatedTimerXBean.MULTIPLE_SETTINGS_DONT_CONFLICT.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerXBean.multiple_settings_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerXBean.multiple_settings_timestamps);
            props.put(NEXT_TIMEOUT_KEY, AutoCreatedTimerXBean.multiple_settings_next);
        } else if (AutoCreatedTimerXBean.START_GATE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerXBean.start_gate_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerXBean.start_gate_timestamps);
        } else if (AutoCreatedTimerXBean.STOP_GATE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerXBean.stop_gate_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerXBean.stop_gate_timestamps);
        }
        // begin F743-16271
        else if (AutoCreatedTimerXBean.TIME_ZONE.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerXBean.time_zone_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerXBean.time_zone_timestamps);
            props.put(TIMEZONE_KEY, AutoCreatedTimerXBean.time_zone_zone);
        }
        // end F743-16271
        else if (AutoCreatedTimerSSBean.SINGLETON_STARTUP.equals(resultsToGet)) {
            props.put(COUNT_KEY, Integer.valueOf(AutoCreatedTimerSSBean.singleton_startup_count));
            props.put(TIMESTAMP_KEY, AutoCreatedTimerSSBean.singleton_startup_timestamps);
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
        } else if (BEAN_X.equals(beanWithTimer)) {
            return ivAutoCreatedTimerX.getTimerData(infoToMatchOn);
        } else if (BEAN_M.equals(beanWithTimer)) {
            return ivAutoCreatedTimerM.getTimerData(infoToMatchOn);
        } else if (BEAN_NI.equals(beanWithTimer)) {
            return ivAutoCreatedTimerNI.getTimerData(infoToMatchOn);
        } else {
            svLogger.info("Specified bean **" + beanWithTimer + "** does not support handing back timer data.");
            return null;
        }
    }

    @Override
    public boolean getTimerPersistentStatus(String beanWithTimer, String infoToMatchOn) throws Exception {
        svLogger.info("Getting timer persistent status from bean **" + beanWithTimer + "** for timer instance **" + infoToMatchOn + "**");
        if (BEAN_A.equals(beanWithTimer)) {
            return ivAutoCreatedTimerA.getPersistentStatus(infoToMatchOn);
        } else {
            svLogger.info("Specified bean **" + beanWithTimer + "** does not support handing back timer data.");
            return false;
        }
    }

    @Override
    public boolean didSingletonStartupBeanSeeItsTimer() {
        return AutoCreatedTimerSSBean.foundTimer;
    }

    @Override
    public boolean didNIBeanFindItsTimer() {
        return AutoCreatedTimerNIBean.foundTheTimer;
    }

    @Override
    public boolean didNIBeanHaveNullInfo() {
        return AutoCreatedTimerNIBean.infoWasNull;
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
    public HashSet<String> getTimerInfos(String beanToGetTimerInfosFor) {
        svLogger.info("Getting timer infos for bean **" + beanToGetTimerInfosFor + "**");
        if (GRANDCHILD_BEAN.equals(beanToGetTimerInfosFor)) {
            return ivGrandchildBean.getTimerInfos();
        } else if (CHILD_BEAN.equals(beanToGetTimerInfosFor)) {
            return ivChildBean.getTimerInfos();
        } else if (PARENT_BEAN.equals(beanToGetTimerInfosFor)) {
            return ivParentBean.getTimerInfos();
        } else {
            svLogger.info("Specified bean **" + beanToGetTimerInfosFor + "** does not support handing back timer data.");
            return new HashSet<String>();
        }
    }

    @Override
    public HashMap<String, HashSet<Class<?>>> getTimerMethodToInvokingClassMap() {
        return ParentBean.svTimerMethodsToInvokingClasses;
    }

    @Override
    public void clearAllTimers() {
        svLogger.info("AutoCreatedTimerDriverBean: Clearing all timers...");
        ivAutoCreatedTimerA.clearAllTimers();
        ivAutoCreatedTimerM.clearAllTimers();
        ivAutoCreatedTimerNI.clearAllTimers();
        ivAutoCreatedTimerSS.clearAllTimers();
        ivAutoCreatedTimerX.clearAllTimers();
        ivChildBean.clearAllTimers();
        ivGrandchildBean.clearAllTimers();
        ivParentBean.clearAllTimers();

        if (timersExpiredLatch.getCount() != 0) {
            throw new IllegalStateException("Not all Timers Expired: waiting on " + timersExpiredLatch.getCount());
        }
        svLogger.info("AutoCreatedTimerDriverBean: All timers cleared.");
    }

}
