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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.MessageDrivenContext;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.transaction.UserTransaction;

public class MDBTimedCMTBean implements MessageListener {

    public static final long INTERVAL = 30 * 1000;
    public static final long TIMER_PRECISION = 900;

    public MessageDrivenContext myMessageDrivenCtx = null;

    public static int svTimeoutCounts[] = new int[10];

    public TimerService ivTimerService;

    // These fields hold the test results for EJB callback methods
    public Object ivSetMessageDrivenContextResults;
    public Object ivEjbCreateResults;
    public static Object svEjbTimeoutResults = null;

    public static Object resultTC41 = null;
    public static Object results[] = new Object[10];

    public static Timer timer[] = null;

    public MDBTimedCMTBean() {
    }

    /**
     * Test getTimerService()/TimerService access from ejbCreate on a
     * Message Driven bean that implements the TimedObject interface. <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container creates a MDB, the results will be stored
     * in an instance variable. The test may then grab any instance (all
     * the same) and extract the results ({@link #getEjbCreateResults}). <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * </ol> <p>
     */
    public void ejbCreate() throws CreateException {
        TimerService ts = null;
        Timer timer = null;

        try {
            // -----------------------------------------------------------------------
            // 1 - Verify getTimerService() returns a valid TimerService
            // -----------------------------------------------------------------------
            System.out.println("ejbCreate: Calling getTimerService()");
            ts = myMessageDrivenCtx.getTimerService();
            ivTimerService = ts;
            assertNotNull("1 ---> Got TimerService", ts);

            // -----------------------------------------------------------------------
            // 2 - Verify TimerService.createTimer() fails with IllegalStateException
            // -----------------------------------------------------------------------
            try {
                System.out.println("ejbCreate: Calling TimerService.createTimer()");
                timer = createTimer(60000, (Serializable) null);
                fail("2 ---> createTimer should have failed!");
                timer.cancel();
            } catch (IllegalStateException ise) {
                System.out.println("2 ---> Caught expected exception: " + ise);
            }

            // -----------------------------------------------------------------------
            // 3 - Verify TimerService.getTimers() fails with IllegalStateException
            // -----------------------------------------------------------------------
            try {
                System.out.println("ejbCreate: Calling getTimers()");
                Collection<Timer> timers = ivTimerService.getTimers();
                fail("3 ---> getTimers should have failed! " + timers);
            } catch (IllegalStateException ise) {
                System.out.println("3 ---> Caught expected exception: " + ise);
            }

            ivEjbCreateResults = true;
        } catch (Throwable t) {
            System.out.println("test failure: " + t);
            ivEjbCreateResults = t;
        }
    }

    /**
     * This method is called when the Message Driven Bean is removed from the server.
     *
     * @exception javax.ejb.EJBException
     */
    public void ejbRemove() {
    }

    /**
     * This method returns the MessageDrivenContext for this Message Driven Bean.
     * The object returned is the same object that is passed in when
     * setMessageDrivenContext is called. <p>
     *
     * @return javax.ejb.MessageDrivenContext
     */
    public MessageDrivenContext getMessageDrivenContext() {
        return myMessageDrivenCtx;
    }

    /**
     * This message stores the MessageDrivenContext in case it is needed later,
     * or the getMessageDrivenContext method is called. <p>
     *
     * Test illegal access from setMessageDrivenContext on a CMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> setMessageDrivenContext.getTimerService() fails with IllegalStateException
     * </ol>
     *
     * @param ctx javax.ejb.MessageDrivenContext
     * @exception javax.ejb.EJBException The exception description.
     */
    @Resource
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        myMessageDrivenCtx = ctx;

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("setSessionContext: Calling getTimerService()");
            myMessageDrivenCtx.getTimerService();
            fail("1 ---> getTimerService should have failed!");
        } catch (IllegalStateException ise) {
            System.out.println("1 ---> Caught expected exception: " + ise);
            ivSetMessageDrivenContextResults = true;
        } catch (Throwable th) {
            System.out.println("test failure: " + th);
            ivSetMessageDrivenContextResults = th;
        }
    }

    /**
     * The onMessage method extracts the text and message id of the message and
     * print the text to the Application Server standard out and calls put message
     * with the message id and text. <p>
     *
     * @param msg javax.jms.Message This should be a TextMessage.
     */
    @Override
    public void onMessage(Message msg) {
        String text = null;

        // send the result vector through the reply queue
        try {
            text = ((TextMessage) msg).getText();
            System.out.println("MDBTimedCMTBean.onMessage(), msg text ->: " + text);

            if (text.equalsIgnoreCase("test02")) {
                System.out.println("Case : test02.");
                results[2] = ivEjbCreateResults;
                cancelTimers();
            } else if (text.equalsIgnoreCase("test03")) {
                System.out.println("Case : test03.");
                results[3] = testTimerServiceIndirect(1);
                cancelTimers();
            } else if (text.equalsIgnoreCase("test04")) {
                System.out.println("Case : test04.");
                System.out.println("Creating a Timer to test access in ejbTimeout ...");
                TimerService ts = myMessageDrivenCtx.getTimerService();
                ivTimerService = ts;
                createTimer(2000, "testTimerService");
                System.out.println("Waiting for timer to expire ...");

                Thread.sleep(4000);

                int counter = 0;
                int maxSleepTime = 180000; // d267800

                System.out.println("Waiting for getEjbTimeoutResults ...");
                while (counter < maxSleepTime) {
                    if (svEjbTimeoutResults != null) {
                        Thread.sleep(2000);
                        resultTC41 = svEjbTimeoutResults;
                        break;
                    }
                    Thread.sleep(1000);
                    counter++;
                }

                if (resultTC41 instanceof Boolean) {
                    try {
                        // -----------------------------------------------------------------------
                        // 18 - Verify ejbTimeout is executed for valid Timers
                        // -----------------------------------------------------------------------
                        {
                            System.out.println("testTimerService: Waiting for timers to expire. . .");

                            boolean successful = true;
                            if (resultTC41 == null) {
                                System.out.println("getEjbTimeoutResults returns null.");
                            }
                            System.out.println("testTimerService: Waiting for timers to expire. . .");

                            System.out.println("Sleep for 62 seconds.");
                            Thread.sleep(62000);

                            System.out.println("18 ---> Verify ejbTimeout is executed for valid Timers");
                            // See if all except cancelled timers have expired once...
                            for (int i = 0; i < timer.length; i++) {
                                switch (i) {
                                    case 5:
                                        if (svTimeoutCounts[i] != 0) {
                                            successful = false;
                                            System.out.println("Cancelled Timer[" + i + "] executed: " + timer[i]);
                                        }
                                        break;

                                    default:
                                        if (svTimeoutCounts[i] != 1) {
                                            successful = false;
                                            System.out.println("Timer[" + i + "] not executed once: " + timer[i]);
                                        }
                                        break;
                                }
                            }

                            assertTrue("18 --> ejbTimeout executed on 5 Timers", successful);
                        }

                        // -----------------------------------------------------------------------
                        // 19 - Verify NoSuchObjectLocalException occurs accessing expired timer
                        // -----------------------------------------------------------------------
                        try {
                            System.out.println("testTimerService: Calling Timer.getInfo() on expired Timer");
                            System.out.println("19 ---> testTimerService: Calling Timer.getInfo() on expired Timer");

                            timer[1].getInfo();
                            fail("19 --> Timer.getInfo() worked - expected NoSuchObjectLocalException");
                        } catch (NoSuchObjectLocalException nso) {
                            System.out.println("19 --> Caught expected exception: " + nso);
                        }

                        // -----------------------------------------------------------------------
                        // 20 - Verify TimerService.getTimers() does not return expired Timers
                        // -----------------------------------------------------------------------
                        {
                            System.out.println("testTimerService: Calling getTimers()");
                            System.out.println("20 ---> testTimerService: Calling getTimers()");
                            Collection<Timer> timers = ts.getTimers();

                            // Print out the results for debug purposes...
                            Object[] timersArray = timers.toArray();
                            for (int i = 0; i < timersArray.length; i++) {
                                System.out.println("  returned : " + timersArray[i]);
                            }

                            assertEquals("20 --> getTimers returned 2 Timers", 2, timers.size());

                            // Make sure they are the correct timers...
                            for (int i = 0; i < timer.length; i++) {
                                switch (i) {
                                    case 0:
                                    case 1:
                                    case 3:
                                    case 5:
                                        if (timers.contains(timer[i]))
                                            fail("Timer[" + i + "] returned: " + timer[i]);
                                        break;

                                    default:
                                        if (!timers.contains(timer[i]))
                                            fail("Timer[" + i + "] not returned: " + timer[i]);
                                        break;
                                }
                            }
                        }

                        // -----------------------------------------------------------------------
                        // 21 - Verify Timer.getNextTimeout() on repeating Timer works
                        // -----------------------------------------------------------------------
                        {
                            System.out.println("testTimerService: Calling Timer.getNextTimeout()");
                            System.out.println("21 ---> testTimerService: Calling Timer.getNextTimeout()");
                            Date nextTime = timer[4].getNextTimeout();
                            long remaining = nextTime.getTime() - System.currentTimeMillis();
                            assertTrue("21 --> Timer.getNextTimeout() worked: " + remaining, remaining >= (1 - TIMER_PRECISION) && remaining <= (INTERVAL + TIMER_PRECISION));
                        }

                        // -----------------------------------------------------------------------
                        // 22 - Verify ejbTimeout is executed multiple times for repeating Timers
                        // -----------------------------------------------------------------------
                        {
                            boolean successful = true;

                            System.out.println("testTimerService: Waiting for timers to expire. . .");
                            System.out.println("22 ---> testTimerService: Waiting for timers to expire. . .");
                            Thread.sleep(32000);

                            // See if all except cancelled timers have expired once...
                            // and repeating timers have executed twice...
                            for (int i = 0; i < timer.length; i++) {
                                switch (i) {
                                    case 2:
                                    case 4:
                                        if (svTimeoutCounts[i] != 2) {
                                            successful = false;
                                            System.out.println("Timer[" + i + "] not executed twice: " + timer[i]);
                                        }
                                        break;

                                    case 5:
                                        if (svTimeoutCounts[i] != 0) {
                                            successful = false;
                                            System.out.println("Cancelled Timer[" + i + "] executed: " + timer[i]);
                                        }
                                        break;

                                    default:
                                        if (svTimeoutCounts[i] != 1) {
                                            successful = false;
                                            System.out.println("Timer[" + i + "] not executed once: " + timer[i]);
                                        }
                                        break;
                                }
                            }

                            assertTrue("22 --> ejbTimeout executed on 2 Timers", successful);
                        }

                        // -----------------------------------------------------------------------
                        // 23 - Verify NoSuchObjectLocalException occurs accessing self cancelled
                        //      timer
                        // -----------------------------------------------------------------------
                        try {
                            System.out.println("testTimerService: Calling Timer.getInfo() on self cancelled Timer");
                            System.out.println("23 ---> testTimerService: Calling Timer.getInfo() on self cancelled Timer");
                            timer[4].getInfo();
                            fail("23 --> Timer.getInfo() worked - " + "expected NoSuchObjectLocalException");
                        } catch (NoSuchObjectLocalException nso) {
                            System.out.println("23 --> Caught expected exception: " + nso);
                        }

                        // -----------------------------------------------------------------------
                        // 24 - Verify TimerService.getTimers() returns empty collection after all
                        //      Timers have expired or been cancelled.
                        // -----------------------------------------------------------------------
                        {
                            System.out.println("testTimerService: Calling getTimers()");
                            System.out.println("24 ---> testTimerService: Calling getTimers()");
                            Collection<Timer> timers = ts.getTimers();

                            // Print out the results for debug purposes...
                            Object[] timersArray = timers.toArray();
                            for (int i = 0; i < timersArray.length; i++) {
                                System.out.println("  returned : " + timersArray[i]);
                            }

                            assertEquals("24 --> getTimers returned 0 Timers", 0, timers.size());
                        }
                    } catch (Throwable th) {
                        System.out.println("test failure: " + th);
                        resultTC41 = th;
                    }

                    results[4] = resultTC41;
                }

                cancelTimers();
            } else if (text.equalsIgnoreCase("test05")) {
                System.out.println("Case : test05.");
                System.out.println("Creating a Timer to test access in ejbTimeout ...");
                TimerService ts = myMessageDrivenCtx.getTimerService();
                ivTimerService = ts;

                createTimer(2000, "testContextMethods-CMT");
                System.out.println("Waiting for timer to expire ...");
                Thread.sleep(4000);

                int counter = 0;
                int maxSleepTime = 180000; // d267800

                System.out.println("Waiting for getEjbTimeoutResults ...");
                while (counter < maxSleepTime) {
                    if (svEjbTimeoutResults != null) {
                        results[5] = svEjbTimeoutResults;
                        break;
                    }
                    Thread.sleep(1000);
                    counter++;
                }
                if (results[5] == null) {
                    System.out.println("getEjbTimeoutResults returns null.");
                }
                cancelTimers();
            } else if (text.equalsIgnoreCase("test07")) {
                System.out.println("Case : test07.");
                TimerService ts = null;
                Object r;

                // -----------------------------------------------------------------------
                // 1 - Verify getTimerService() returns a valid TimerService
                // -----------------------------------------------------------------------
                try {
                    System.out.println("EJB Constructor: Calling getTimerService()");
                    ts = myMessageDrivenCtx.getTimerService();
                    assertNotNull("1 ---> Got TimerService", ts);
                    r = true;
                } catch (Throwable th) {
                    System.out.println("test failure: " + th);
                    r = th;
                }
                results[7] = r;
                cancelTimers();
            } else if (text.equalsIgnoreCase("test08")) {
                System.out.println("Case : test08.");
                results[8] = ivSetMessageDrivenContextResults;
                cancelTimers();
            } else {
                System.out.println(" *Error : Unknown test case.");
            }

        } catch (Exception err) {
            System.out.println(err);
        }
        return;
    }

    /**
     * Utility method that may be used to create a Timer when a Timer is
     * required to perform a test, but cannot be created directly by
     * the bean performing the test. For example, if the bean performing
     * the test does not implement the TimedObject interface. <p>
     *
     * Local interface only! <p>
     *
     * Used by test : {@link TimerMDBOperationsTest#test01} <p>
     *
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with 1 minute duration and specified info.
     **/
    public Timer createTimer(Serializable info) {
        TimerConfig tCfg = new TimerConfig();
        tCfg.setInfo(info);
        tCfg.setPersistent(false);
        Timer timer = ivTimerService.createSingleActionTimer(60000, tCfg);
        return timer;
    }

    /**
     * Utility method to create a Timer remotely. This method is for use
     * by tests that are testing the expiration of a Timer and execution
     * of the ejbTimeout method. <p>
     *
     * Also clears the results from any previous ejbTimeout tests. <p>
     *
     * Used by tests :
     * <ul>
     * <li> {@link TimerMDBOperationsTest#test05} <li> {@link TimerMDBOperationsTest#test06} <li> {@link TimerMDBOperationsTest#test07} </ul> <p>
     *
     * @param duration duration of the Timer to create
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with the duration and info specified.
     **/
    public Timer createTimer(long duration, Serializable info) {
        svEjbTimeoutResults = null;
        TimerConfig tCfg = new TimerConfig();
        tCfg.setInfo(info);
        tCfg.setPersistent(false);
        Timer timer = ivTimerService.createSingleActionTimer(duration, tCfg);
        return timer;
    }

    public Timer createTimer(long duration, long interval, Serializable info) {
        svEjbTimeoutResults = null;
        TimerConfig tCfg = new TimerConfig();
        tCfg.setInfo(info);
        tCfg.setPersistent(false);
        Timer timer = ivTimerService.createIntervalTimer(duration, interval, tCfg);
        return timer;
    }

    public Timer createTimer(Date date, Serializable info) {
        svEjbTimeoutResults = null;
        TimerConfig tCfg = new TimerConfig();
        tCfg.setInfo(info);
        tCfg.setPersistent(false);
        Timer timer = ivTimerService.createSingleActionTimer(date, tCfg);
        return timer;
    }

    public Timer createTimer(Date date, long interval, Serializable info) {
        svEjbTimeoutResults = null;
        TimerConfig tCfg = new TimerConfig();
        tCfg.setInfo(info);
        tCfg.setPersistent(false);
        Timer timer = ivTimerService.createIntervalTimer(date, interval, tCfg);
        return timer;
    }

    private Object testTimerServiceIndirect(int phase) {
        try {
            testTimerService(phase);
            return true;
        } catch (Throwable t) {
            System.out.println("test failure: " + t);
            return t;
        }
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Stateless
     * Session bean that implements the TimedObject interface. <p>
     *
     * Used by test : {@link TimerMDBOperationsTest#test04} <p>
     *
     * This test method will confirm the following :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer(duration, null) works
     * <li> TimerService.createTimer(duration, info) works
     * <li> TimerService.createTimer(duration, interval, info) works
     * <li> TimerService.createTimer(date, info) works
     * <li> TimerService.createTimer(date, interval, info) works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> TimerService.getTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * <li> ejbTimeout is executed for valid Timers
     * <li> NoSuchObjectLocalException occurs accessing expired timer
     * <li> TimerService.getTimers() does not return expired Timers
     * <li> Timer.getNextTimeout() on repeating Timer works
     * <li> ejbTimeout is executed multiple times for repeating Timers
     * <li> NoSuchObjectLocalException occurs accessing self cancelled timer
     * <li> TimerService.getTimers() returns empty collection after all Timers
     * have expired or been cancelled.
     * </ol>
     */
    public void testTimerService(int phase) throws Exception {
        long remaining = 0;
        Date expiration = null;
        Object timerInfo = null;
        Collection<Timer> timers = null;
        Object[] timersArray;

        TimerService ts = null;
        timer = new Timer[6];

        svTimeoutCounts = new int[timer.length];

        // -----------------------------------------------------------------------
        // 1 - Verify getTimerService() returns a valid TimerService
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling getTimerService()");
        System.out.println("1 ---> testTimerService: Calling getTimerService()");
        ts = myMessageDrivenCtx.getTimerService();
        ivTimerService = ts;
        assertNotNull("1 ---> Got TimerService", ts);

        // -----------------------------------------------------------------------
        // 2 - Verify TimerService.createTimer(duration, null) works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling TimerService.createTimer(duration, null)");
        System.out.println("2 ---> testTimerService: Calling TimerService.createTimer(duration, null)");
        timer[0] = createTimer(60000, (Serializable) null);
        assertNotNull("2 ---> TimerService.createTimer(duration, null) worked", timer[0]);

        // -----------------------------------------------------------------------
        // 3 - Verify TimerService.createTimer(duration, info) works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling TimerService.createTimer(duration, info)");
        System.out.println("3 ---> testTimerService: Calling TimerService.createTimer(duration, info)");
        timer[1] = createTimer(60000, new Integer(1));
        assertNotNull("3 ---> TimerService.createTimer(duration, info) worked", timer[1]);

        // -----------------------------------------------------------------------
        // 4 - Verify TimerService.createTimer(duration, interval, info) works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling TimerService.createTimer(duration, interval, info)");
        System.out.println("4 ---> testTimerService: Calling TimerService.createTimer(duration, interval, info)");
        timer[2] = createTimer(60000, INTERVAL, new Integer(2));
        assertNotNull("4 ---> TimerService.createTimer(duration, interval, info) worked", timer[2]);

        // -----------------------------------------------------------------------
        // 5 - Verify TimerService.createTimer(date, info) works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling TimerService.createTimer(date, info)");
        System.out.println("5 ---> testTimerService: Calling TimerService.createTimer(date, info)");
        expiration = new Date(System.currentTimeMillis() + 60000);
        timer[3] = createTimer(expiration, new Integer(3));
        assertNotNull("5 ---> TimerService.createTimer(date, info) worked", timer[3]);

        // -----------------------------------------------------------------------
        // 6 - Verify TimerService.createTimer(date, interval, info) works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling TimerService.createTimer(date, interval, info)");
        System.out.println("6 ---> testTimerService: Calling TimerService.createTimer(date, interval, info)");
        expiration = new Date(System.currentTimeMillis() + 60000);
        timer[4] = createTimer(expiration, INTERVAL, new Integer(4));
        assertNotNull("6 ---> TimerService.createTimer(date, interval, info) worked", timer[4]);

        // -----------------------------------------------------------------------
        // 7 - Verify Timer.getTimeRemaining() on single event Timer works
        // -----------------------------------------------------------------------
        // Create an extra timer for testing now, and testing cancel later...
        System.out.println("testTimerService: Calling TimerService.createTimer(duration, info)");
        System.out.println("7 ---> testTimerService: Calling TimerService.createTimer(duration, info)");
        timer[5] = createTimer(60000, new Integer(5));
        System.out.println("testTimerService: Calling Timer.getTimeRemaining()");
        remaining = timer[5].getTimeRemaining();
        assertTrue("7 ---> Timer.getTimeRemaining() worked: " + remaining, remaining >= 1 && remaining <= 60000);

        // -----------------------------------------------------------------------
        // 8 - Verify Timer.getTimeRemaining() on repeating Timer works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling Timer.getTimeRemaining()");
        System.out.println("8 ---> testTimerService: Calling Timer.getTimeRemaining()");
        remaining = timer[2].getTimeRemaining();
        assertTrue("8 ---> Timer.getTimeRemaining() worked: " + remaining, remaining >= 1 && remaining <= 60000);

        // -----------------------------------------------------------------------
        // 9 - Verify Timer.getInfo() returning null works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling Timer.getInfo()");
        System.out.println("9 ---> testTimerService: Calling Timer.getInfo()");
        timerInfo = timer[0].getInfo();
        assertNull("9 ---> Timer.getInfo() worked: " + timerInfo + "", timerInfo);

        // -----------------------------------------------------------------------
        // 10 - Verify Timer.getInfo() returning serializable works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling Timer.getInfo()");
        System.out.println("10 ---> testTimerService: Calling Timer.getInfo()");
        timerInfo = timer[1].getInfo();
        assertEquals("10 --> Timer.getInfo() worked: " + timerInfo + "", new Integer(1), timerInfo);

        // -----------------------------------------------------------------------
        // 14 - Verify TimerService.getTimers() returns all created Timers
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling getTimers()");
        System.out.println("14 ---> testTimerService: Calling getTimers()");
        timers = ts.getTimers();

        // Print out the results for debug purposes...
        timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            System.out.println("  returned : " + timersArray[i]);
        }

        assertEquals("14 --> getTimers returned 6 Timers", 6, timers.size());

        // Make sure they are the correct timers...
        for (int i = 0; i < timer.length; i++) {
            if (!timers.contains(timer[i]))
                fail("Timer[" + i + "] not returned: " + timer[i]);
        }

        // -----------------------------------------------------------------------
        // 15 - Verify Timer.cancel() works
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling Timer.cancel()");
        System.out.println("15 ---> testTimerService: Calling Timer.cancel()");
        timer[5].cancel();
        System.out.println("15 --> Timer.cancel() worked");

        // -----------------------------------------------------------------------
        // 16 - Verify NoSuchObjectLocalException occurs accessing canceled timer
        // -----------------------------------------------------------------------
        try {
            System.out.println("testTimerService: Calling Timer.getInfo() on cancelled Timer");
            System.out.println("16 ---> testTimerService: Calling Timer.getInfo() on cancelled Timer");
            timer[5].getInfo();
            fail("16 --> Timer.getInfo() worked - expected NoSuchObjectLocalException");
        } catch (NoSuchObjectLocalException nso) {
            System.out.println("16 --> Caught expected exception: " + nso);
        }

        // -----------------------------------------------------------------------
        // 17 - Verify TimerService.getTimers() does not return cancelled Timers
        // -----------------------------------------------------------------------
        System.out.println("testTimerService: Calling getTimers()");
        System.out.println("17 ---> testTimerService: Calling getTimers()");
        timers = ts.getTimers();

        // Print out the results for debug purposes...
        timersArray = timers.toArray();
        for (int i = 0; i < timersArray.length; i++) {
            System.out.println("  returned : " + timersArray[i]);
        }

        assertEquals("17 --> getTimers returned 5 Timers", 5, timers.size());

        // Make sure they are the correct timers...
        for (int i = 0; i < timer.length; i++) {
            switch (i) {
                case 5:
                    if (timers.contains(timer[i]))
                        fail("Timer[" + i + "] returned: " + timer[i]);
                    break;

                default:
                    if (!timers.contains(timer[i]))
                        fail("Timer[" + i + "] not returned: " + timer[i]);
                    break;
            }
        }

        if (phase == 1) {
            // -----------------------------------------------------------------------
            // 18 - Verify ejbTimeout is executed for valid Timers
            // -----------------------------------------------------------------------
            {
                boolean successful = true;

                System.out.println("testTimerService: Waiting for timers to expire. . .");
                System.out.println("18 ---> testTimerService: Waiting for timers to expire. . .");

                Thread.sleep(62000);

                // See if all except cancelled timers have expired once...
                for (int i = 0; i < timer.length; i++) {
                    switch (i) {
                        case 5:
                            if (svTimeoutCounts[i] != 0) {
                                successful = false;
                                System.out.println("Cancelled Timer[" + i + "] executed: " + timer[i]);
                            }
                            break;

                        default:
                            if (svTimeoutCounts[i] != 1) {
                                successful = false;
                                System.out.println("Timer[" + i + "] not executed once: " + timer[i]);
                            }
                            break;
                    }
                }

                assertTrue("18 --> ejbTimeout executed on 5 Timers", successful);
            }

            // -----------------------------------------------------------------------
            // 19 - Verify NoSuchObjectLocalException occurs accessing expired timer
            // -----------------------------------------------------------------------
            try {
                System.out.println("testTimerService: Calling Timer.getInfo() on expired Timer");
                System.out.println("19 ---> testTimerService: Calling Timer.getInfo() on expired Timer");
                timer[1].getInfo();
                fail("19 --> Timer.getInfo() worked - expected NoSuchObjectLocalException");
            } catch (NoSuchObjectLocalException nso) {
                System.out.println("19 --> Caught expected exception: " + nso);
            }

            // -----------------------------------------------------------------------
            // 20 - Verify TimerService.getTimers() does not return expired Timers
            // -----------------------------------------------------------------------
            System.out.println("testTimerService: Calling getTimers()");
            System.out.println("20 ---> testTimerService: Calling getTimers()");
            timers = ts.getTimers();

            // Print out the results for debug purposes...
            timersArray = timers.toArray();
            for (int i = 0; i < timersArray.length; i++) {
                System.out.println("  returned : " + timersArray[i]);
            }

            assertEquals("20 --> getTimers returned 2 Timers", 2, timers.size());

            // Make sure they are the correct timers...
            for (int i = 0; i < timer.length; i++) {
                switch (i) {
                    case 0:
                    case 1:
                    case 3:
                    case 5:
                        if (timers.contains(timer[i]))
                            fail("Timer[" + i + "] returned: " + timer[i]);
                        break;

                    default:
                        if (!timers.contains(timer[i]))
                            fail("Timer[" + i + "] not returned: " + timer[i]);
                        break;
                }
            }

            // -----------------------------------------------------------------------
            // 21 - Verify Timer.getNextTimeout() on repeating Timer works
            // -----------------------------------------------------------------------
            System.out.println("testTimerService: Calling Timer.getNextTimeout()");
            System.out.println("21 ---> testTimerService: Calling Timer.getNextTimeout()");
            Date nextTime = timer[2].getNextTimeout();
            remaining = nextTime.getTime() - System.currentTimeMillis();
            assertTrue("21 --> Timer.getNextTimeout() worked: " + remaining, remaining >= (1 - TIMER_PRECISION) && remaining <= (INTERVAL + TIMER_PRECISION));

            // -----------------------------------------------------------------------
            // 22 - Verify ejbTimeout is executed multiple times for repeating Timers
            // -----------------------------------------------------------------------
            boolean successful = true;

            System.out.println("testTimerService: Waiting for timers to expire. . .");
            System.out.println("22 ---> testTimerService: Waiting for timers to expire. . .");
            Thread.sleep(32000);

            // See if all except cancelled timers have expired once...
            // and repeating timers have executed twice...
            for (int i = 0; i < timer.length; i++) {
                switch (i) {
                    case 2:
                    case 4:
                        if (svTimeoutCounts[i] != 2) {
                            successful = false;
                            System.out.println("Timer[" + i + "] not executed twice: " + timer[i]);
                        }
                        break;

                    case 5:
                        if (svTimeoutCounts[i] != 0) {
                            successful = false;
                            System.out.println("Cancelled Timer[" + i + "] executed: " + timer[i]);
                        }
                        break;

                    default:
                        if (svTimeoutCounts[i] != 1) {
                            successful = false;
                            System.out.println("Timer[" + i + "] not executed once: " + timer[i]);
                        }
                        break;
                }
            }

            assertTrue("22 --> ejbTimeout executed on 2 Timers", successful);

            // -----------------------------------------------------------------------
            // 23 - Verify NoSuchObjectLocalException occurs accessing self cancelled
            //      timer
            // -----------------------------------------------------------------------
            try {
                System.out.println("testTimerService: Calling Timer.getInfo() on self cancelled Timer");
                System.out.println("23 ---> testTimerService: Calling Timer.getInfo() on self cancelled Timer");
                timer[2].getInfo();
                fail("23 --> Timer.getInfo() worked - " + "expected NoSuchObjectLocalException");
            } catch (NoSuchObjectLocalException nso) {
                System.out.println("23 --> Caught expected exception: " + nso);
            }

            // -----------------------------------------------------------------------
            // 24 - Verify TimerService.getTimers() returns empty collection after all
            //      Timers have expired or been cancelled.
            // -----------------------------------------------------------------------
            System.out.println("testTimerService: Calling getTimers()");
            System.out.println("24 ---> testTimerService: Calling getTimers()");
            timers = ts.getTimers();

            // Print out the results for debug purposes...
            timersArray = timers.toArray();
            for (int i = 0; i < timersArray.length; i++) {
                System.out.println("  returned : " + timersArray[i]);
            }

            assertEquals("24 --> getTimers returned 0 Timers", 0, timers.size());
        }
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * Used by tests :
     * <ul>
     * <li> {@link TimerMDBOperationsTest#test04} <li> {@link TimerMDBOperationsTest#test05} </ul> <p>
     *
     * Since this method may not be called directly, but is called whenever
     * the Container expires a Timer, the results will be stored
     * in a static variable. The test may then grab any instance (all
     * the same) and extract the results ({@link #getEjbTimeoutResults}).
     * A static variable must be used, since only one of possibly many
     * bean instances will be used to execute the test. <p>
     *
     * This test method will confirm the following for test04(), when the
     * Timer info object is the String "testTimerService":
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer(duration, null) works
     * <li> TimerService.createTimer(duration, info) works
     * <li> TimerService.createTimer(duration, interval, info) works
     * <li> TimerService.createTimer(date, info) works
     * <li> TimerService.createTimer(date, interval, info) works
     * <li> Timer.getTimeRemaining() on single event Timer works
     * <li> Timer.getTimeRemaining() on repeating Timer works
     * <li> Timer.getInfo() returning null works
     * <li> Timer.getInfo() returning serializable works
     * <li> TimerService.getTimers() returns all created Timers
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * <li> TimerService.getTimers() does not return cancelled Timers
     * <li> ejbTimeout is executed for valid Timers
     * <li> NoSuchObjectLocalException occurs accessing expired timer
     * <li> TimerService.getTimers() does not return expired Timers
     * <li> Timer.getNextTimeout() on repeating Timer works
     * <li> ejbTimeout is executed multiple times for repeating Timers
     * <li> NoSuchObjectLocalException occurs accessing self cancelled timer
     * <li> TimerService.getTimers() returns empty collection after all Timers
     * have expired or been cancelled.
     * </ol> <p>
     *
     * This test method will confirm the following for test05(), when the
     * Timer info object is the String "testContextMethods-CMT":
     * <ol>
     * <li> EJBContext.getEJBHome() fails with IllegalStateException
     * <li> EJBContext.getEJBLocalHome() fails with IllegalStateException
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getRollbackOnly() - false works
     * <li> EJBContext.setRollbackOnly() works
     * </ol>
     **/
    @Timeout
    public void timeout(Timer timer) {
        int timerIndex = -1;
        Object info = timer.getInfo();

        System.out.println("Calling ejbTimeout for [" + info + "].");

        // -----------------------------------------------------------------------
        // When the 'info' object is 'null' or an 'Integer', then this method
        // uses the Integer (0 for null) to differentiate between timers, and
        // keeps a count of how often each timer is executed.  Repeating timers
        // are cancelled after the 2nd expiration.
        // -----------------------------------------------------------------------
        if (info == null)
            timerIndex = 0;
        else if (info instanceof Integer)
            timerIndex = ((Integer) info).intValue();

        if (timerIndex >= 0) {
            svTimeoutCounts[timerIndex]++;

            System.out.println("Timer " + timerIndex + " expired " + svTimeoutCounts[timerIndex] + " time(s)");

            if (svTimeoutCounts[timerIndex] > 1)
                timer.cancel();

            return;
        }

        // For debug, just print out that the timer expired, and the name of the
        // test it will execute.
        System.out.println("Timer expired: " + info);

        // -----------------------------------------------------------------------
        // Execute Test - if info is a String, then it is probably the name
        //                of a test to execute.
        // -----------------------------------------------------------------------
        if (info instanceof String) {
            String test = (String) info;

            // --------------------------------------------------------------------
            // Test getTimerService/TimerService access.....
            // --------------------------------------------------------------------

            // Timer Service access in ejbTimeout should be identical to that of a
            // business method..... so just call the business method implementation.
            // This is a direct Java call... so is functionally equivalent to the
            // code being here.  Saves duplicating the code.
            if (test.equals("testTimerService")) {
                // Cancel self, so this Timer doesn't interfere with getTimers()
                // calls in test.
                timer.cancel();

                svEjbTimeoutResults = testTimerServiceIndirect(2);
            }

            // --------------------------------------------------------------------
            // Test SessionContext method access for CMT bean.....
            // --------------------------------------------------------------------

            else if (test.equals("testContextMethods-CMT")) {
                try {
                    testContextMethods();
                    svEjbTimeoutResults = true;
                } catch (Throwable t) {
                    System.out.println("test failure: " + t);
                    svEjbTimeoutResults = t;
                }
            }
        }
    }

    /**
     * Test SessionContext method access from a method on a Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * Used by tests :
     * <ul>
     * <li> {@link TimerMDBOperationsTest#test05} - CMT
     * </ul> <p>
     *
     * This test method will confirm the following for CMT (test05):
     * <ol>
     * <li> EJBContext.getEJBHome() fails with IllegalStateException
     * <li> EJBContext.getEJBLocalHome() fails with IllegalStateException
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() fails with IllegalStateException --- d184955
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getRollbackOnly() - fails with IllegalStateException
     * <li> EJBContext.setRollbackOnly() - fails with IllegalStateException
     * </ol> <p>
     *
     */
    public void testContextMethods() {
        // -----------------------------------------------------------------------
        // 1 - Verify EJBContext.getEJBHome() fails with
        //     IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling getEJBHome()");
            EJBHome ejbHome = myMessageDrivenCtx.getEJBHome();
            fail("1 ---> getEJBHome should fail. " + ejbHome);
        } catch (IllegalStateException ise) {
            System.out.println("1 ---> Caught expected exception: " + ise);
        }

        // -----------------------------------------------------------------------
        // 2 - Verify EJBContext.getEJBLocalHome() fails with
        //     IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling getEJBLocalHome()");
            EJBLocalHome ejbHome = myMessageDrivenCtx.getEJBLocalHome();
            fail("2 ---> getEJBLocalHome should fail. " + ejbHome);
        } catch (IllegalStateException ise) {
            System.out.println("2 ---> Caught expected exception: " + ise);
        }

        // -----------------------------------------------------------------------
        // 3 - Verify EJBContext.getCallerPrincipal() works
        // -----------------------------------------------------------------------
        {
            System.out.println("testContextMethods: Calling getCallerPrincipal()");
            Principal principal = myMessageDrivenCtx.getCallerPrincipal();
            assertNotNull("3 ---> Got CallerPrincipal", principal);
        }

        // -----------------------------------------------------------------------
        // 4 - Verify EJBContext.isCallerInRole() fails with ISE  --- d184955
        // -----------------------------------------------------------------------
        {
            System.out.println("testContextMethods: Calling isCallerInRole()");
            boolean inRole = myMessageDrivenCtx.isCallerInRole("test");
            assertFalse("4 ---> isCallerInRole() returns false", inRole);
        }

        // -----------------------------------------------------------------------
        // 5 - Verify EJBContext.getUserTransaction()
        //     CMT - fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling getUserTransaction()");
            UserTransaction userTran = myMessageDrivenCtx.getUserTransaction();
            fail("5 ---> getUserTransaction should have failed! " + userTran);
        } catch (IllegalStateException ise) {
            System.out.println("5 ---> Caught expected exception: " + ise);
        }

        // -----------------------------------------------------------------------
        // 6 - Verify EJBContext.getTimerService() works
        // -----------------------------------------------------------------------
        {
            System.out.println("testContextMethods: Calling getTimerService()");
            TimerService ts = myMessageDrivenCtx.getTimerService();
            assertNotNull("6 ---> Got TimerService", ts);
        }

        // --------------------------------------------------------------------
        // 7 - Verify EJBContext.getRollbackOnly() - false works           CMT
        // --------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling getRollbackOnly()");
            boolean rollback = myMessageDrivenCtx.getRollbackOnly();
            assertFalse("7 --> Got RollbackOnly", rollback);
        } catch (IllegalStateException ise) {
            System.out.println("7 ---> Caught expected exception: " + ise);
        }

        // --------------------------------------------------------------------
        // 8 - Verify EJBContext.setRollbackOnly() works                   CMT
        // --------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling setRollbackOnly()");
            myMessageDrivenCtx.setRollbackOnly();
            fail("8 --> Got setRollbackOnly");
        } catch (java.lang.IllegalStateException ise) {
            System.out.println("8 ---> Caught expected exception: " + ise);
        }
    }

    public void cancelTimers() {
        TimerService ts = myMessageDrivenCtx.getTimerService();

        for (Timer timer : ts.getTimers()) {
            try {
                System.out.println("Canceling timer -> " + timer.getInfo());
                timer.cancel();
            } catch (NoSuchObjectLocalException nsoex) {
                System.out.println(timer + " already removed");
            }
        }
    }
}