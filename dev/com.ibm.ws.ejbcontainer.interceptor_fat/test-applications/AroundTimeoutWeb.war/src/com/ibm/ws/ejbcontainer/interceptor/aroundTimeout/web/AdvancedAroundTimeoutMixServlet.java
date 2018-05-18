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

import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_mix.ejb.AdvancedInterface.AUTO_TIMER_INFO;
import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_mix.ejb.TimerData.MAX_TIMER_WAIT;
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

import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_mix.ejb.AdvancedInterface;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_mix.ejb.TimerData;

import componenttest.app.FATServlet;

/**
 * Test that the AroundTimeout Interceptor methods get called in the correct
 * order and prior to the timeout callback method being executed.
 */
@SuppressWarnings("serial")
@WebServlet("/AdvancedAroundTimeoutMixServlet")
public class AdvancedAroundTimeoutMixServlet extends FATServlet {
    private final static String CLASS_NAME = AdvancedAroundTimeoutMixServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String SINGLE_ACTION_TIMER_INFO = "advancedSingleAction";

    @EJB(beanName = "AroundTimeoutMixEJB/AdvancedAroundTimeoutBean")
    AdvancedInterface ivBean;

    /**
     * Bean B1 has a timeout callback method annotated with the Schedule
     * annotation. B1 has a class level Interceptors annotation defined listing
     * Interceptors CLI1 and CLI2 which both have aroundTimeout methods. CLI2
     * extends SuperCLI2 which defines an aroundTimeout method. B1 extends a
     * class, SuperB1, that has an AroundTimeout method. B1's timeout callback
     * method is annotated with a method level Interceptors annotation that has
     * multiple interceptor classes listed: MLI1 and MLI2. MLI1 extends another
     * Interceptor class, SuperMLI1, that has an AroundTimeout method. B1 defines
     * its own AroundTimeout method.
     *
     * Verify the order (per spec) that the AroundTimeout methods get called.
     *
     * Order should be: CLI1, SuperCLI2, CLI2, SuperMLI1, MLI1, MLI2, SuperB1's
     * aroundTimeout, B1's aroundTimeout, B1's automatic timeout method.
     */
    @Test
    public void testAdvancedAroundTimeoutInterceptorScheduleMix() throws Exception {
        svLogger.info("--> Just before calling the bean method to get back the automatic timer latch.");
        CountDownLatch timerLatch = ivBean.getAutoTimerLatch();
        svLogger.info("--> Automatically created timer created with info set to: " + AUTO_TIMER_INFO + ". Waiting for automatic timer at most " + MAX_TIMER_WAIT + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td = TimerData.svIntEventMap.get(AUTO_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 11 " +
                     "(Default1, Default2, CLI1, SuperCLI2, CLI2, SuperMLI1, MLI1, MLI2, SuperB1's " +
                     "aroundTimeout, B1's aroundTimeout, B1's automatic timeout method.).",
                     11, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("Default1Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 1:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("Default2Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 2:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("CL1Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 3:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.  CurrentEvent=" + currentEvent,
                               currentEvent.contains("CL2Interceptor")
                                                                                                                  && currentEvent.contains(".superAroundTimeout")
                                                                                                                  && currentEvent.contains("autoTimeoutMethod")
                                                                                                                  && currentEvent.contains("automaticTimer"));
                    break;

                case 4:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("CL2Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 5:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("ML1Interceptor")
                                                                                    && currentEvent.contains(".superAroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 6:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("ML1Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 7:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("ML2Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 8:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AdvancedAroundTimeoutBean")
                                                                                    && currentEvent.contains(".superAdvancedAroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 9:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AdvancedAroundTimeoutBean")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                case 10:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AdvancedAroundTimeoutBean")
                                                                                    && currentEvent.contains("autoTimeoutMethod")
                                                                                    && currentEvent.contains("automaticTimer"));
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }

    /**
     * Bean B1 has a timeout callback method annotated with the Timeout
     * annotation. B1 has a class level Interceptors annotation defined listing
     * Interceptors CLI1 and CLI2 which both have aroundTimeout methods. CLI2
     * extends SuperCLI2 which defines an aroundTimeout method. B1 extends a
     * class, SuperB1, that has an AroundTimeout method. B1's timeout callback
     * method is annotated with a method level Interceptors annotation that has
     * multiple interceptor classes listed: MLI1 and MLI2. MLI1 extends another
     * Interceptor class, SuperMLI1, that has an AroundTimeout method. B1 defines
     * its own AroundTimeout method.
     *
     * Verify the order (per spec) that the AroundTimeout methods get called.
     *
     * Order should be: CLI1, SuperCLI2, CLI2, SuperMLI1, MLI1, MLI2, SuperB1's
     * aroundTimeout, B1's aroundTimeout, B1's timeout method.
     */
    @Test
    public void testAdvancedAroundTimeoutInterceptorTimeoutMix() throws Exception {
        svLogger.info("--> Just before calling the bean method to create a SingleActionTimer");
        CountDownLatch timerLatch = ivBean.createSingleActionTimer(SINGLE_ACTION_TIMER_INFO);
        svLogger.info("--> SingleActionTimer created with info set to " + SINGLE_ACTION_TIMER_INFO + ". Waiting for timer at most " + MAX_TIMER_WAIT + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td = TimerData.svIntEventMap.get(SINGLE_ACTION_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 11 " +
                     "(Default1, Default2, CLI1, SuperCLI2, CLI2, SuperMLI1, MLI1, MLI2, SuperB1's " +
                     "aroundTimeout, B1's aroundTimeout, B1's timeout method.). " +
                     "Actual event result list: " + resultList,
                     11, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("Default1Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 1:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("Default2Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 2:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("CL1Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 3:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.  CurrentEvent=" + currentEvent,
                               currentEvent.contains("CL2Interceptor")
                                                                                                                  && currentEvent.contains(".superAroundTimeout")
                                                                                                                  && currentEvent.contains("timeoutMethod")
                                                                                                                  && currentEvent.contains("advancedSingleAction"));
                    break;

                case 4:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("CL2Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 5:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("ML1Interceptor")
                                                                                    && currentEvent.contains(".superAroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 6:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("ML1Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 7:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("ML2Interceptor")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 8:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AdvancedAroundTimeoutBean")
                                                                                    && currentEvent.contains(".superAdvancedAroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 9:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AdvancedAroundTimeoutBean")
                                                                                    && currentEvent.contains(".aroundTimeout")
                                                                                    && currentEvent.contains("timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                case 10:
                    assertTrue("--> The interceptor at [" + i + "] was incorrect.",
                               currentEvent.contains("AdvancedAroundTimeoutBean")
                                                                                    && currentEvent.contains(".timeoutMethod")
                                                                                    && currentEvent.contains("advancedSingleAction"));
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }
}
