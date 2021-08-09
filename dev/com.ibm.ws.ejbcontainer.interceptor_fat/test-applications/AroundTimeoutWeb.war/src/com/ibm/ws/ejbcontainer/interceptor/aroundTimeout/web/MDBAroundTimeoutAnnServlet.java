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

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATJMSHelper;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.MDTimerBean;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb.TimerData;

import componenttest.app.FATServlet;

/**
 * Test that the AroundTimeout Interceptor methods get called in the correct
 * order and prior to the timeout callback method being executed for
 * Message-Driven beans.
 */
@SuppressWarnings("serial")
@WebServlet("/MDBAroundTimeoutAnnServlet")
public class MDBAroundTimeoutAnnServlet extends FATServlet {
    private final static String CLASS_NAME = MDBAroundTimeoutAnnServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource
    UserTransaction userTran;

    // JNDI for JMS Resources
    String qcfName = "WSTestQCF";
    String requestQueueName = "InterceptorMDBReqQueue";

    /**
     * Test that the AroundTimeout Interceptor methods get called in the
     * correct order for a persistent automatic timer on a Message-Driven
     * bean. <p>
     *
     * Verifies the following are performed in the correct order:
     * <ul>
     * <li> AroundTimeout on an interceptor class
     * <li> AroundTimeout on the MDB class
     * <li> timeout method
     * </ul>
     **/
    @Test
    public void testMDBPersistentAutomaticTimerInterceptorsAnn() throws Exception {
        // Wait for auto-timers to run, then verify results
        MDTimerBean.getAutoTimerLatch().await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        verifyResults(MDTimerBean.PERSISTENT_AUTO_TIMER_INFO, "ejbSchedule");
    }

    /**
     * Test that the AroundTimeout Interceptor methods get called in the
     * correct order for a non-persistent automatic timer on a Message-Driven
     * bean. <p>
     *
     * Verifies the following are performed in the correct order:
     * <ul>
     * <li> AroundTimeout on an interceptor class
     * <li> AroundTimeout on the MDB class
     * <li> timeout method
     * </ul>
     **/
    @Test
    public void testMDBNonPersistentAutomaticTimerInterceptorsAnn() throws Exception {
        // Wait for auto-timers to run, then verify results
        MDTimerBean.getAutoTimerLatch().await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);
        verifyResults(MDTimerBean.NON_PERSISTENT_AUTO_TIMER_INFO, "ejbSchedule");
    }

    /**
     * Test that the AroundTimeout Interceptor methods get called in the
     * correct order for a persistent programmatic timer on a Message-Driven
     * bean. <p>
     *
     * Verifies the following are performed in the correct order:
     * <ul>
     * <li> AroundTimeout on an interceptor class
     * <li> AroundTimeout on the MDB class
     * <li> timeout method
     * </ul>
     **/
    @Test
    public void testMDBPersistentProgrammaticTimerInterceptorsAnn() throws Exception {
        CountDownLatch timerLatch = new CountDownLatch(1);
        String timerKey = "MDTimerBean-Programmatic-1:persistent";

        svLogger.info("--> Sending message to MDB : " + timerKey);

        userTran.begin();

        MDTimerBean.setTimerLatch(timerLatch);
        String msgID = putQueueMessage(timerKey, qcfName, requestQueueName);
        userTran.commit();
        svLogger.info("--> Sent msgID : " + msgID);

        // Delay to insure the message is delivered, and timer has run
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        verifyResults(timerKey, "ejbTimeout");
    }

    /**
     * Test that the AroundTimeout Interceptor methods get called in the
     * correct order for a non-persistent programmatic timer on a
     * Message-Driven bean. <p>
     *
     * Verifies the following are performed in the correct order:
     * <ul>
     * <li> AroundTimeout on an interceptor class
     * <li> AroundTimeout on the MDB class
     * <li> timeout method
     * </ul>
     **/
    @Test
    public void testMDBNonPersistentProgrammaticTimerInterceptorsAnn() throws Exception {
        CountDownLatch timerLatch = new CountDownLatch(1);
        String timerKey = "MDTimerBean-Programmatic-2";

        svLogger.info("--> Sending message to MDB : " + timerKey);

        userTran.begin();

        MDTimerBean.setTimerLatch(timerLatch);
        String msgID = putQueueMessage(timerKey, qcfName, requestQueueName);
        userTran.commit();
        svLogger.info("--> Sent msgID : " + msgID);

        // Delay to insure the message is delivered, and timer has run
        timerLatch.await(MAX_TIMER_WAIT, TimeUnit.MILLISECONDS);

        verifyResults(timerKey, "ejbTimeout");
    }

    /**
     * Internal method that verifies the result by obtaining the entries
     * logged by the interceptors and timer methods.
     *
     * @param timerKey the info string used to create the timer; this is
     *            the key into the logged timer entries.
     * @param method the specific timer method that should have been called
     *            for the timer being verified.
     **/
    private void verifyResults(String timerKey,
                               String method) throws Exception {
        svLogger.info("--> Looking up timer key = " + timerKey);
        TimerData td = TimerData.svIntEventMap.remove(timerKey);
        svLogger.info("--> TimerData.svIntEventMap = " + TimerData.svIntEventMap);
        svLogger.info("--> td = " + td);

        ArrayList<String> resultList = td.getIntEvents();
        svLogger.info("--> resultList = " + resultList);
        int size = resultList.size();
        svLogger.info("--> resultList.size() = " + size);

        assertEquals("--> Size of interceptor event result list should be 3 " +
                     "(interceptor class's around invoke, bean's around invoke, " +
                     "and bean's timeout method).", 3, size);

        for (int i = 0; i < size; i++) {
            String currentEvent = resultList.get(i);
            svLogger.info("--> resultList[" + i + "] contains: " + currentEvent);

            switch (i) {
                case 0:
                    if (currentEvent.contains("ATOInterceptor")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains(method) // ejbTimeout or ejbSchedule
                        && currentEvent.contains(timerKey)) {
                        assertTrue("--> [" + i + "] The first interceptor event was correct.", true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results.");
                    }
                    break;

                case 1:
                    if (currentEvent.contains("MDTimerBean")
                        && currentEvent.contains(".aroundTimeout")
                        && currentEvent.contains(method) // ejbTimeout or ejbSchedule
                        && currentEvent.contains(timerKey)) {
                        assertTrue("--> [" + i + "] The second interceptor event was correct.", true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results.");
                    }
                    break;

                case 2:
                    if (currentEvent.contains("MDTimerBean")
                        && currentEvent.contains(method) // ejbTimeout or ejbSchedule
                        && currentEvent.contains(timerKey)
                        && currentEvent.contains("fired")) {
                        assertTrue("--> [" + i + "] The " + method +
                                   " method returned expected results.", true);
                    } else {
                        fail("--> resultList[" + i + "] did not return the expected results.");
                    }
                    break;

                default:
                    fail("--> [" + i + "] The resultList contained more items than expected.");
            }
        }
    }

    /**
     * Put a text message on a specified queue
     *
     * @param text
     *            : message
     * @param queueConnectionFactory
     *            : JNDI context for queue connection factory
     * @param queue
     *            : JNDI context for the queue
     * @return Message ID
     * @throws Exception
     */
    public static String putQueueMessage(Object text, String queueConnectionFactory, String queue) throws Exception {
        svLogger.info("Requests to put message '" + text + "' with Queue Connection Factory '" + queueConnectionFactory + "' and Queue '" + queue + "'.");
        String messageID = null;

        FATJMSHelper jms = new FATJMSHelper();

        // Create a connection
        jms.createQueueConnection(queueConnectionFactory);

        // Create queue session
        jms.createQueueSession();

        // Create a QueueSender
        jms.createQueueSender(queue);

        // Create a message to send to the queue...
        messageID = jms.sendMessageToQueue(text);

        // Close the connection and queue
        jms.closeQueueConnection();

        return messageID;
    }
}
