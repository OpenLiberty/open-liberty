/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.web;

import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb.TimerData.MAX_TIMER_WAIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb.ATAppExInterface;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb.ATNoExInterface;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb.TimerData;

import componenttest.app.FATServlet;

/**
 * Test that the AroundTimeout Interceptor methods defined with either no
 * throws clause or a throws clause that uses an application exception get
 * called in the correct order and prior to the timeout callback method being
 * executed.
 */
@SuppressWarnings("serial")
@WebServlet("/AroundTimeoutExcServlet")
public class AroundTimeoutExcServlet extends FATServlet {
    private final static String CLASS_NAME = AroundTimeoutExcServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String SINGLE_ACTION_TIMER_APP_EX_INFO = "SingleActionAppEx";
    private static final String SINGLE_ACTION_TIMER_NO_EX_INFO = "SingleActionNoEx";

    @EJB(beanName = "AroundTimeoutExcEJB/AroundTimeoutAppExBean")
    ATAppExInterface ivAppExBean;

    @EJB(beanName = "AroundTimeoutExcEJB/AroundTimeoutNoExBean")
    ATNoExInterface ivNoExBean;

    /**
     * The test EJB has a timeout callback method annotated with the Schedule
     * annotation.
     *
     * The following interceptors are defined and should be executed in this order:
     * Method-level Interceptor (AppExceptionInterceptor) with an AroundTimeout
     * method defined in XML with an application exception on the throws clause.
     * The EJB's superclass has an AroundTimeout method defined in annotations
     * with an application exception on the throws clause.
     * The EJB has an AroundTimeout method defined in XML with an application
     * exception on the throws clause.
     * The EJB's automatic timeout method should then fire.
     */
    @Test
    public void testAroundTimeoutScheduleAppEx() throws Exception {
        svLogger.info("--> Just before calling the bean method to get back the automatic timer latch.");
        CountDownLatch timerLatch = ivAppExBean.getAutoTimerLatch();
        svLogger.info("--> Automatically created timer created with info set to: " + ATAppExInterface.AUTO_TIMER_INFO + ". Waiting for automatic timer at most " + MAX_TIMER_WAIT
                      + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td = TimerData.svIntEventMap.get(ATAppExInterface.AUTO_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 4 " +
                     "(AppExceptionInterceptor, superclass's aroundTimeout, EJB's aroundTimeout, EJB's automatic timeout method.). ", 4, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AppExceptionInterceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 1:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutAppExBean")
                                                                                    && currentEvent.contains(".superAroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 2:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutAppExBean")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 3:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutAppExBean")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }

    /**
     * The test EJB has a timeout callback method annotated with the Schedule
     * annotation.
     *
     * The following interceptors are defined and should be executed in this order:
     * Class-level Interceptor (NoExceptionInterceptor) with an AroundTimeout
     * method defined in annotations without a throws clause.
     * The EJB has an AroundTimeout method defined in XML without a throws
     * clause.
     * The EJB's automatic timeout method should then fire.
     */
    @Test
    public void testAroundTimeoutScheduleNoEx() throws Exception {
        svLogger.info("--> Just before calling the bean method to get back the automatic timer latch.");
        CountDownLatch timerLatch = ivAppExBean.getAutoTimerLatch();
        svLogger.info("--> Automatically created timer created with info set to: " + ATNoExInterface.AUTO_TIMER_INFO + ". Waiting for automatic timer at most " + MAX_TIMER_WAIT
                      + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td = TimerData.svIntEventMap.get(ATNoExInterface.AUTO_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 3 " +
                     "(NoExceptionInterceptor, EJB's aroundTimeout, EJB's timeout method.). ", 3, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("NoExceptionInterceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 1:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutNoExBean")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 2:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutNoExBean")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }

    /**
     * The test EJB has a timeout callback method annotated with the Timeout
     * annotation.
     *
     * The following interceptors are defined and should be executed in this order:
     * Method-level Interceptor (AppExceptionInterceptor) with an AroundTimeout
     * method defined in XML with an application exception on the throws clause.
     * The EJB's superclass has an AroundTimeout method defined in annotations
     * with an application exception on the throws clause.
     * The EJB has an AroundTimeout method defined in XML with an application
     * exception on the throws clause.
     * The EJB's timeout method should then fire.
     */
    @Test
    public void testAroundTimeoutTimeoutAppEx() throws Exception {
        svLogger.info("--> Just before calling the bean method to create a SingleActionTimer");
        CountDownLatch timerLatch = ivAppExBean.createSingleActionTimer(SINGLE_ACTION_TIMER_APP_EX_INFO);
        svLogger.info("--> SingleActionTimer created with info set to " + SINGLE_ACTION_TIMER_APP_EX_INFO + ". Waiting for timer at most " + MAX_TIMER_WAIT + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td = TimerData.svIntEventMap.get(SINGLE_ACTION_TIMER_APP_EX_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 4 " +
                     "(AppExceptionInterceptor, superclass's aroundTimeout, EJB's aroundTimeout, EJB's timeout method.). ", 4, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AppExceptionInterceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("SingleActionAppEx"));
                    break;

                case 1:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutAppExBean")
                                                                                    && currentEvent.contains(".superAroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("SingleActionAppEx"));
                    break;

                case 2:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutAppExBean")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("SingleActionAppEx"));
                    break;

                case 3:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutAppExBean")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("SingleActionAppEx"));
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }

    /**
     * The test EJB has a timeout callback method annotated with the Timeout
     * annotation.
     *
     * The following interceptors are defined and should be executed in this order:
     * Class-level Interceptor (NoExceptionInterceptor) with an AroundTimeout
     * method defined in annotations without a throws clause.
     * The EJB has an AroundTimeout method defined in XML without a throws
     * clause.
     * The EJB's timeout method should then fire.
     */
    @Test
    public void testAroundTimeoutTimeoutNoEx() throws Exception {
        svLogger.info("--> Just before calling the bean method to create a SingleActionTimer");
        CountDownLatch timerLatch = ivNoExBean.createSingleActionTimer(SINGLE_ACTION_TIMER_NO_EX_INFO);
        svLogger.info("--> SingleActionTimer created with info set to " + SINGLE_ACTION_TIMER_NO_EX_INFO + ". Waiting for timer at most " + MAX_TIMER_WAIT + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td = TimerData.svIntEventMap.get(SINGLE_ACTION_TIMER_NO_EX_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 3 " +
                     "(NoExceptionInterceptor, EJB's aroundTimeout, EJB's timeout method.). ", 3, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("NoExceptionInterceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("SingleActionNoEx"));
                    break;

                case 1:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutNoExBean")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("SingleActionNoEx"));
                    break;

                case 2:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AroundTimeoutNoExBean")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("SingleActionNoEx"));
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }
}
