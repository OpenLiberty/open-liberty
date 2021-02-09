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
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.security.Principal;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.MessageDrivenContext;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

public class MDBTimedBMTBean implements MessageListener {
    private MessageDrivenContext myMessageDrivenCtx = null;

    public static int svTimeoutCounts[] = new int[10];

    private TimerService ivTimerService;

    // These fields hold the test results for EJB callback methods
    private Object ivSetMessageDrivenContextResults;
    private static Object svEjbTimeoutResults = null;

    public static Object results;

    public void ejbCreate() throws CreateException {
    }

    public MDBTimedBMTBean() {
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
            System.out.println("Unexpected exception from getTimerService(): " + th);
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
        String messageID = null;

        results = null;

        // send the result vector through the reply queue
        try {
            text = ((TextMessage) msg).getText();
            System.out.println("MDBTimedBMTBean.onMessage(), msg text ->: " + text);

            if (text.equalsIgnoreCase("test09")) {
                System.out.println("Case : test09.");
                System.out.println("Creating a Timer to test access in ejbTimeout ...");
                TimerService ts = myMessageDrivenCtx.getTimerService();
                ivTimerService = ts;
                createTimer(2000, "testContextMethods-BMT");
                System.out.println("Waiting for timer to expire ...");
                Thread.sleep(4000);
                System.out.println("Returning the results ...");

                int counter = 0;
                int maxSleepTime = 60000;

                System.out.println("Waiting for getEjbTimeoutResults ...");
                while (counter < maxSleepTime && svEjbTimeoutResults == null) {
                    Thread.sleep(1000);
                    counter++;
                }
                results = svEjbTimeoutResults;
            } else if (text.equalsIgnoreCase("test11")) {
                System.out.println("Case : test11.");
                results = ivSetMessageDrivenContextResults;
            } else {
                System.out.println("*Error : Unknown test case.");
                results = null;
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
     * @return Timer created with 1 minutie duration and specified info.
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
    public boolean createTimer(long duration, Serializable info) {
        svEjbTimeoutResults = null;
        TimerConfig tCfg = new TimerConfig();
        tCfg.setInfo(info);
        tCfg.setPersistent(false);
        Timer timer = ivTimerService.createSingleActionTimer(duration, tCfg);
        return (timer != null);
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * Used by tests :
     * <ul>
     * <li> {@link TimerMDBOperationsTest#test09} </ul> <p>
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
            if (test.equals("testContextMethods-BMT")) {
                timer.cancel();
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
     * <li> {@link TimerMDBOperationsTest#test05} - BMT
     * </ul> <p>
     *
     * This test method will confirm the following for BMT (test09):
     * <ol>
     * <li> EJBContext.getEJBHome() fails with IllegalStateException
     * <li> EJBContext.getEJBLocalHome() fails with IllegalStateException
     * <li> EJBContext.getUserTransaction() allows
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() fails with IllegalStateException --- d184955
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getRollbackOnly() failed
     * <li> EJBContext.setRollbackOnly() failed
     * </ol> <p>
     *
     */
    private void testContextMethods() throws Exception {
        UserTransaction userTran = null;
        userTran = myMessageDrivenCtx.getUserTransaction();

        // -----------------------------------------------------------------------
        // 1 - Verify EJBContext.getEJBHome() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling getEJBHome()");
            EJBHome ejbHome = myMessageDrivenCtx.getEJBHome();
            fail("1 ---> getEJBHome should fail.");
        } catch (IllegalStateException ise) {
            System.out.println("1 ---> Caught expected exception: " + ise);
        }

        // -----------------------------------------------------------------------
        // 2 - Verify EJBContext.getEJBLocalHome() fails with IllegalStateException
        // -----------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling getEJBLocalHome()");
            EJBLocalHome ejbHome = myMessageDrivenCtx.getEJBLocalHome();
            fail("2 ---> getEJBLocalHome should fail.");
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
        //     BMT - allows
        // -----------------------------------------------------------------------
        System.out.println("testContextMethods: Calling UserTran.begin()");
        userTran.begin();
        assertEquals("5 --> Started UserTransaction", userTran.getStatus(), Status.STATUS_ACTIVE);
        userTran.commit();

        // -----------------------------------------------------------------------
        // 6 - Verify EJBContext.getTimerService() works
        // -----------------------------------------------------------------------
        {
            System.out.println("testContextMethods: Calling getTimerService()");
            TimerService ts = myMessageDrivenCtx.getTimerService();
            assertNotNull("6 ---> Got TimerService", ts);
        }

        // --------------------------------------------------------------------
        // 7 - Verify EJBContext.getRollbackOnly() - fails with IllegalStateException
        // --------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling getRollbackOnly()");
            boolean rollback = myMessageDrivenCtx.getRollbackOnly();
            fail("7 --> getRollbackOnly should have failed!");
        } catch (IllegalStateException ise) {
            System.out.println("7 --> Caught expected exception: " + ise);
        }

        // --------------------------------------------------------------------
        // 8 - Verify EJBContext.setRollbackOnly() fails with IllegalStateException
        // --------------------------------------------------------------------
        try {
            System.out.println("testContextMethods: Calling setRollbackOnly()");
            myMessageDrivenCtx.setRollbackOnly();
            fail("8 --> setRollbackOnly should have failed!");
        } catch (IllegalStateException ise) {
            System.out.println("8 --> Caught expected exception: " + ise);
        }
    }
}