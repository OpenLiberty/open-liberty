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

import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.SuperDuperTimerCallback.SUPER_DUPER_AUTO_TIMER_INFO;
import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.SuperTimerCallback.SUPER_AUTO_TIMER_INFO;
import static com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.TimerData.MAX_TIMER_WAIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.LocalInterface;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.SuperDuperTimerCallback;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.SuperTimerCallback;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.TimerData;

import componenttest.app.FATServlet;

/**
 * Test that the AroundTimeout Interceptor methods get called in the correct
 * order and prior to the timeout callback method being executed.
 */
@SuppressWarnings("serial")
@WebServlet("/InheritedTimerCallbackAnnServlet")
public class InheritedTimerCallbackAnnServlet extends FATServlet {
    private final static String CLASS_NAME = InheritedTimerCallbackAnnServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String SINGLE_ACTION_SUPER_TIMER_INFO = "superTimeoutTimerCallbackTimer";

    @EJB(beanName = "AroundTimeoutAnnEJB/InheritedTimerCallbackBean")
    LocalInterface ivBean;

    private LocalInterface lookupLocalBean(String beanName) throws NamingException {
        if (beanName.equals("InheritedTimerCallbackBean")) {
            return ivBean;
        }

        throw new IllegalArgumentException(beanName);
    }

    /**
     * Verify that an aroundTimeout method on the bean class will get invoked on
     * all timer callback methods that may be inherited from the bean's superclasses.
     *
     * Class E has a method annotated with Schedule - superDuperAutoTimer(Timer t)
     * with an Interceptors ({ML1, ML2}) annotation, where ML1 extends SuperML1
     * Class C extends Class E,
     * Class C has a class level Interceptors({CL1, CL2}) annotation
     * Class C has a method annotated with Timeout - superTimeout(Timer t)
     * Class C has a method annotated with Schedule - superAutoTimer(Timer t)
     * Class A extends class C (does not override C's timeout() )
     * Class A has an AroundTimeout method -aTO()
     * SuperML1, ML1, ML2, CL1, CL2 are all interceptor classes with an
     * aroundTimeout method.
     *
     * Verify the aroundTimeout invocation order for each of the timer
     * callback methods:
     * superTimeout - aTO
     * superAutoTimer - aTO
     * superDuperAutoTimer - SuperML1, ML1, ML2, aTO
     */
    @Test
    public void testInheritedTimeoutCallbackAnn() throws Exception {
        LocalInterface bean = lookupLocalBean("InheritedTimerCallbackBean");

        svLogger.info("--> Just before calling the bean method to create a SingleActionTimer");
        CountDownLatch timerLatch = bean.createSingleActionTimer(SINGLE_ACTION_SUPER_TIMER_INFO);
        svLogger.info("--> SingleActionTimer created with info set to: " + SINGLE_ACTION_SUPER_TIMER_INFO + ". Waiting for timer at most " + MAX_TIMER_WAIT + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td_beanTimer = TimerData.svIntEventMap.get(SINGLE_ACTION_SUPER_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td_beanTimer = " + td_beanTimer);

        ArrayList<String> resultList = td_beanTimer.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 2 " +
                     "(the bean's aroundTimeout, and SuperTimerCallback's @Timeout method).", 2, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    if (currentEvent.contains("InheritedTimerCallbackBean")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains("superTimeoutTimerCallbackMethod")
                        && currentEvent.contains("superTimeoutTimerCallbackTimer")) {
                        assertTrue("--> [" + i + "] The first interceptor event was correct.", true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results, currentEvent = " + currentEvent + ".  resultList=" + resultList
                             + ".   TimerData.svIntEventMap = " + TimerData.svIntEventMap);
                    }
                    break;

                case 1:
                    if (currentEvent.contains("InheritedTimerCallbackBean")
                        && currentEvent.contains(".superTimeoutTimerCallbackMethod")
                        && currentEvent.contains("superTimeoutTimerCallbackTimer")) {
                        assertTrue("--> [" + i + "] The timeoutMethod returned expected results.", true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results.");
                    }
                    break;
                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }

        svLogger.info("--> Just before calling the static method to get back the automatic timer latch.");
        CountDownLatch autoTimerLatch = SuperTimerCallback.getSuperAutoTimerLatch();
        svLogger.info("--> Automatically created timer created with info set to: " + SUPER_AUTO_TIMER_INFO + ". Waiting for automatic timer at most " + MAX_TIMER_WAIT + " ms.");
        autoTimerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td_superTimer = TimerData.svIntEventMap.get(SUPER_AUTO_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td_superTimer = " + td_superTimer);

        ArrayList<String> superResultList = td_superTimer.getIntEvents();
        svLogger.info("--> superResultList = " + superResultList);

        size = superResultList.size();
        svLogger.info("--> superResultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 2 " +
                     "(the bean's aroundTimeout, the superClass's automatic timer callback method).", 2, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = superResultList.get(i);
            svLogger.info("--> superResultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    if (currentEvent.contains("InheritedTimerCallbackBean")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains("superTimerCallbackMethod")
                        && currentEvent.contains("superTimerCallbackTimer")) {
                        assertTrue("--> [" + i
                                   + "] The first interceptor event was correct.", true);
                    } else {
                        fail("--> superResultList[" + i + "] did not return the expected results, currentEvent = " + currentEvent + ".  superResultList=" + superResultList
                             + ".   TimerData.svIntEventMap = " + TimerData.svIntEventMap);
                    }
                    break;

                case 1:
                    if (currentEvent.contains("InheritedTimerCallbackBean")
                        && currentEvent.contains(".superTimerCallbackMethod")
                        && currentEvent.contains("superTimerCallbackTimer")) {
                        assertTrue("--> [" + i + "] The timeoutMethod returned expected results.", true);
                    } else {
                        fail("--> superResultList[" + i + "] did not return the expected results.");
                    }
                    break;
                default:
                    fail("--> [" + i + "] The superResultList contained more items than expected.");
            }
        }

        svLogger.info("--> Just before calling the static method to get back the automatic timer latch.");
        autoTimerLatch = SuperDuperTimerCallback.getSuperDuperAutoTimerLatch();
        svLogger.info("--> Automatically created timer created with info set to: " + SUPER_DUPER_AUTO_TIMER_INFO + ". Waiting for automatic timer at most " + MAX_TIMER_WAIT
                      + " ms.");
        autoTimerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td_superDuperTimer = TimerData.svIntEventMap.get(SUPER_DUPER_AUTO_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td_superDuperTimer = " + td_superDuperTimer);

        ArrayList<String> superDuperResultList = td_superDuperTimer.getIntEvents();
        svLogger.info("--> superDuperResultList = " + superDuperResultList);

        size = superDuperResultList.size();
        svLogger.info("--> superDuperResultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 5 " +
                     "(ML1.superAroundTimeout, ML1.aroundTimeout, ML2.aroundTimeout, " +
                     "the bean's aroundTimeout, the bean's superDuperTimerCallbackMethod ).", 5, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = superDuperResultList.get(i);
            svLogger.info("--> superDuperResultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    if (currentEvent.contains("ML1Interceptor")
                        && currentEvent.contains(".superAroundTimeout")
                        && currentEvent.contains("superDuperTimerCallbackMethod")
                        && currentEvent.contains("superDuperTimerCallbackTimer")) {
                        assertTrue("--> [" + i
                                   + "] The first interceptor event was correct.", true);
                    } else {
                        fail("--> superDuperResultList[" + i + "] did not return the expected results, currentEvent = " + currentEvent + ".  superDuperResultList="
                             + superDuperResultList + ".   TimerData.svIntEventMap = " + TimerData.svIntEventMap);
                    }
                    break;

                case 1:
                    if (currentEvent.contains("ML1Interceptor")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains("superDuperTimerCallbackMethod")
                        && currentEvent.contains("superDuperTimerCallbackTimer")) {
                        assertTrue("--> [" + i + "] The second interceptor event was correct.", true);
                    } else {
                        fail("--> superDuperResultList[" + i + "] did not return the expected results, currentEvent = " + currentEvent + ".  superDuperResultList="
                             + superDuperResultList + ".   TimerData.svIntEventMap = " + TimerData.svIntEventMap);
                    }
                    break;

                case 2:
                    if (currentEvent.contains("ML2Interceptor")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains("superDuperTimerCallbackMethod")
                        && currentEvent.contains("superDuperTimerCallbackTimer")) {
                        assertTrue("--> [" + i
                                   + "] The third interceptor event was correct.", true);
                    } else {
                        fail("--> superDuperResultList[" + i + "] did not return the expected results, currentEvent = " + currentEvent + ".  superDuperResultList="
                             + superDuperResultList + ".   TimerData.svIntEventMap = " + TimerData.svIntEventMap);
                    }
                    break;

                case 3:
                    if (currentEvent.contains("InheritedTimerCallbackBean")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains("superDuperTimerCallbackMethod")
                        && currentEvent.contains("superDuperTimerCallbackTimer")) {
                        assertTrue("--> [" + i + "] The 4th interceptor event was correct.", true);
                    } else {
                        fail("--> superDuperResultList[" + i + "] did not return the expected results, currentEvent = " + currentEvent + ".  superDuperResultList="
                             + superDuperResultList + ".   TimerData.svIntEventMap = " + TimerData.svIntEventMap);
                    }
                    break;

                case 4:
                    if (currentEvent.contains("InheritedTimerCallbackBean")
                        && currentEvent.contains(".superDuperTimerCallbackMethod")
                        && currentEvent.contains("superDuperTimerCallbackTimer")) {
                        assertTrue("--> [" + i + "] The timeoutMethod returned expected results.", true);
                    } else {
                        fail("--> superDuperResultList[" + i + "] did not return the expected results, currentEvent = " + currentEvent + ".  superDuperResultList="
                             + superDuperResultList + ".   TimerData.svIntEventMap = " + TimerData.svIntEventMap);
                    }
                    break;
                default:
                    fail("--> [" + i + "] The superDuperResultList contained more items than expected.");
            }
        }
    }
}
