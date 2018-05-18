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
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.TimerData;

import componenttest.app.FATServlet;

/**
 * Test that the AroundTimeout Interceptor methods get called in the correct
 * order and prior to the timeout callback method being executed.
 */
@SuppressWarnings("serial")
@WebServlet("/SLSBAroundTimeoutAnnServlet")
public class SLSBAroundTimeoutAnnServlet extends FATServlet {
    private final static String CLASS_NAME = SLSBAroundTimeoutAnnServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String SINGLE_ACTION_TIMER_INFO = "singleAction";

    @EJB(beanName = "AroundTimeoutAnnEJB/SLTimedObjectBean")
    LocalInterface ivBean;

    private LocalInterface lookupSLTimedObjectBean() throws NamingException {
        return ivBean;
    }

    /**
     * B1 is a Stateless Session bean that implements the TimedObject
     * interface (i.e. must have an ejbTimeout method).
     * Verify that the order that the aroundTimeout interceptor methods is:
     * 1) the Class level interceptor class's aroundTimeout method
     * 2) B1's aroundTimeout method
     *
     * Verify that the timeout callback method was the last event in the chain.
     */
    @Test
    public void testInterceptors1Ann() throws Exception {
        LocalInterface bean = lookupSLTimedObjectBean();

        svLogger.info("--> Just before calling the bean method to create a SingleActionTimer");
        CountDownLatch timerLatch = bean.createSingleActionTimer(SINGLE_ACTION_TIMER_INFO);
        svLogger.info("--> SingleActionTimer created with info set to: " + SINGLE_ACTION_TIMER_INFO + ". Waiting for timer at most " + MAX_TIMER_WAIT + " ms.");
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        TimerData td = TimerData.svIntEventMap.get(SINGLE_ACTION_TIMER_INFO);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);

        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> The size of the interceptor event result list should be 3 " +
                     "(interceptor class's around invoke, bean's around invoke, and bean's timeout method).",
                     3, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    if (currentEvent.contains("ATOInterceptor")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains("ejbTimeout")
                        && currentEvent.contains("singleAction")) {
                        assertTrue("--> [" + i + "] The first interceptor event was correct.", true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results.");
                    }
                    break;

                case 1:
                    if (currentEvent.contains("SLTimedObjectBean")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains("ejbTimeout")
                        && currentEvent.contains("singleAction")) {
                        assertTrue("--> [" + i + "] The second interceptor event was correct.", true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results.");
                    }
                    break;

                case 2:
                    if (currentEvent.contains("SLTimedObjectBean")
                        && currentEvent.contains(".ejbTimeout")
                        && currentEvent.contains("singleAction")) {
                        assertTrue("--> [" + i + "] The ejbTimeout method returned expected results.",
                                   true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results.");
                    }
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }
}
