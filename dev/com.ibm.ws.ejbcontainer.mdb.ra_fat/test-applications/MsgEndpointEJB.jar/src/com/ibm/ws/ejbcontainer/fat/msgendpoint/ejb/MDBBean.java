/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Collection;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.MessageDrivenContext;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class MDBBean implements MessageListener {
    @Resource
    private MessageDrivenContext myMessageDrivenCtx;

    @EJB
    StatelessTimedLocalObject sltl;

    public static Object results = null;

    /**
     * This method is called when the Message Driven Bean is created. It currently does nothing.
     *
     * @exception javax.ejb.CreateException
     * @exception javax.ejb.EJBException
     */
    public void ejbCreate() throws CreateException {
    }

    /**
     * This method is called when the Message Driven Bean is removed from the server.
     *
     * @exception javax.ejb.EJBException
     */
    public void ejbRemove() {
    }

    /**
     * This method returns the MessageDrivenContext for this Message Driven Bean. The object returned
     * is the same object that is passed in when setMessageDrivenContext is called
     *
     * @return javax.ejb.MessageDrivenContext
     */
    public MessageDrivenContext getMessageDrivenContext() {
        return myMessageDrivenCtx;
    }

    @Resource
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        // Nothing to do
    }

    /**
     * The onMessage method extracts the text and message id of the message and print the text to the
     * Application Server standard out and calls put message with the message id and text.
     *
     * @param msg javax.jms.Message This should be a TextMessage.
     */
    @Override
    public void onMessage(Message msg) {
        String text = null;
        String messageID = null;

        // send the result vector through the reply queue
        try {
            text = ((TextMessage) msg).getText();
            System.out.println("senderBean.onMessage(), msg text ->: " + text);

            try {
                testTimerService();
                results = true;
            } catch (Throwable t) {
                System.out.println("test failure: " + t);
                results = t;
            }

        } catch (Exception err) {
            System.out.println(err);
        }
        return;
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Message Driven
     * bean that does not implement the TimedObject interface. <p>
     *
     * Used by test : {@link TimerMDBOperationsTest#test01} <p>
     *
     * This test method will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    public void testTimerService() throws Exception {
        TimerService ts = null;
        Timer timer = null;
        String timerInfo = null;
        TimerConfig tCfg = new TimerConfig();

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() returns a valid TimerService
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling getTimerService()");
        ts = myMessageDrivenCtx.getTimerService();
        assertNotNull("1 ---> Got TimerService", ts);

        // -----------------------------------------------------------------------
        // 2 - Verify TimerService.createTimer() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("testTimerService: Calling TimerService.createTimer()");
            tCfg.setInfo((Serializable) null);
            tCfg.setPersistent(false);
            timer = ts.createSingleActionTimer(60000, tCfg);
            fail("2 ---> createTimer should have failed!");
            timer.cancel();
        } catch (IllegalStateException ise) {
            System.out.println("2 ---> Caught expected exception: " + ise);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify TimerService.getTimers() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("testTimerService: Calling getTimers()");
            Collection timers = ts.getTimers();
            fail("3 ---> getTimers should have failed!");
        } catch (IllegalStateException ise) {
            System.out.println("3 ---> Caught expected exception: " + ise);
        }

        // -----------------------------------------------------------------------
        // Verify that Timer method operations may be performed.
        // - must get a Timer from a different bean, since this is not TimedObject
        // -----------------------------------------------------------------------
        {
            System.out.println("Creating a timer for StatelessTimed Bean ...");
            timerInfo = "StatelessBean:" + System.currentTimeMillis();
            timer = sltl.createTimer(timerInfo);
        }

        // -----------------------------------------------------------------------
        // 4 - Verify Timer.getTimeRemaining() works
        // -----------------------------------------------------------------------
        {
            System.out.println("testTimerService: Calling Timer.getTimeRemaining()");
            long remaining = timer.getTimeRemaining();
            assertTrue("4 ---> Timer.getTimeRemaining() worked: " + remaining, remaining >= 1 && remaining <= 60000);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify Timer.getInfo() works
        // -----------------------------------------------------------------------
        {
            System.out.println("testTimerService: Calling Timer.getInfo()");
            Object curInfo = timer.getInfo();
            assertEquals("5 ---> Timer.getInfo() worked: " + curInfo, timerInfo, curInfo);
        }

        // -----------------------------------------------------------------------
        // 9 - Verify Timer.cancel() works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling Timer.cancel()");
        timer.cancel();
        System.out.println("9 ---> Timer.cancel() worked");

        // -----------------------------------------------------------------------
        // 10 - Verify NoSuchObjectLocalException occurs accessing canceled timer
        // -----------------------------------------------------------------------
        try {
            System.out.println("testTimerService: Calling Timer.getInfo() on cancelled Timer");
            timer.getInfo();
            fail("10 --> Timer.getInfo() worked - expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            System.out.println("10 --> Caught expected exception: " + nso);
        }
    }
}