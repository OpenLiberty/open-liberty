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
package com.ibm.ws.ejbcontainer.fat.msgendpoint.web;

import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;

import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.MDBBean;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.MDBTimedBMTBean;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.MDBTimedCMTBean;
import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTBaseMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> TimerMDBOperationsTest .
 *
 * <dt><b>Test Author:</b> Alvin So <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Test to exercise the EJB Container Timer Service using Message Driven
 * beans. Verifies the "Allowed Operations" table in the EJB spcification
 * for Message Driven beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testNotTimedObject - TimerService access in MDB that does not implement TimedObject interface.
 * <li>testEJBCreate - TimerService access in ejbCreate.
 * <li>testOnMessage - TimerService access in onMessage method.
 * <li>testEJBTimeout - TimerService access in ejbTimeout.
 * <li>testCMTEJBTimeoutMessageDrivenContext - MessageDrivenContext access in ejbTimeout - CMT.
 * <li>testCMTConstructor - TimerService access in constructor - CMT.
 * <li>testCMTOnMessage - MessageDrivenContext access in onMessage - CMT.
 * <li>testSetMessageDrivenContext - TimerService access in setMessageDrivenContext.
 * <li>testBMTEJBTimeoutMessageDrivenContext - MessageDrivenContext access in ejbTimeout - BMT.
 * <li>testBMTOnMessage - MessageDrivenContext access in onMessage - BMT.
 * <li>testBMTConstructor - TimerService access in constructor - BMT.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
public class TimerMDBOperationsServlet extends FATServlet {
    private FVTBaseMessageProvider baseProvider = null;

    public void prepareTRA() throws Exception {
        baseProvider = (FVTBaseMessageProvider) new InitialContext().lookup("java:comp/env/BaseMessageProvider");
        System.out.println("Looked up BaseMessageProvider");
        baseProvider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    public static final int maxSleepTime = 300; // 5 minutes

    private void checkResults(Object results) {
        if (results instanceof Throwable) {
            throw new Error((Throwable) results);
        }
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Message
     * Driven bean that does not implement the TimedObject interface. <p>
     *
     * This test will confirm the following (MTM01) :
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
    public void testNotTimedObject() throws Exception {
        String deliveryID = "test01";

        prepareTRA();

        System.out.println("Enter EJB method : testTimerService()");

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBBean", 41);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBBean", 41, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBBean", "MDBBean - test01", 41);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        System.out.println("Exit EJB method : testTimerService()");

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBBean.results != null) {
                checkResults(MDBBean.results);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test getTimerService()/TimerService access from ejbCreate on a
     * Message Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following (MTM02) :
     * <ol>
     * <li> getTimerService() returns a valid TimerService
     * <li> TimerService.createTimer() fails with IllegalStateException
     * <li> TimerService.getTimers() fails with IllegalStateException
     * </ol>
     */
    public void testEJBCreate() throws Exception {
        String deliveryID = "test02";

        prepareTRA();

        System.out.println("Enter EJB method : ejbCreate()");

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedCMTBean", 42);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedCMTBean", 42, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedCMTBean", "test02", 42);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        System.out.println("Exit EJB method : ejbCreate()");

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedCMTBean.results[2] != null) {
                checkResults(MDBTimedCMTBean.results[2]);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test getTimerService()/TimerService access from a method on a Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
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
    public void testOnMessage() throws Exception {
        String deliveryID = "test03";

        prepareTRA();

        System.out.println("Enter EJB method : testTimerService()");

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedCMTBean", 43);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedCMTBean", 43, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedCMTBean", "test03", 43);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        System.out.println("Exit EJB method : testTimerService()");

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedCMTBean.results[3] != null) {
                checkResults(MDBTimedCMTBean.results[3]);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test getTimerService()/TimerService access from ejbTimeout on a Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
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
    public void testEJBTimeout() throws Exception {
        String deliveryID = "test04";

        prepareTRA();

        System.out.println("Enter EJB method : ejbTimeout()");

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedCMTBean", 44);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedCMTBean", 44, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedCMTBean", deliveryID, 44);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        System.out.println("Exit EJB method : ejbTimeout()");

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedCMTBean.results[4] != null) {
                checkResults(MDBTimedCMTBean.results[4]);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        System.out.println("Done waiting");
        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test illegal access from ejbTimeout on a CMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> EJBContext.getEJBHome() fails with IllegalStateException
     * <li> EJBContext.getEJBLocalHome() fails with IllegalStateException
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getUserTransaction() fails with IllegalStateException
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getRollbackOnly() - false works
     * <li> EJBContext.setRollbackOnly() works
     * </ol>
     */
    public void testCMTEJBTimeoutMessageDrivenContext() throws Exception {
        String deliveryID = "test05";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedCMTBean", 45);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedCMTBean", 45, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedCMTBean", deliveryID, 45);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedCMTBean.results[5] != null) {
                checkResults(MDBTimedCMTBean.results[5]);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test illegal access from Constructor on a CMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Constructor.getTimerService() fails with IllegalStateException
     * </ol>
     */
    public void testCMTConstructor() throws Exception {
        String deliveryID = "test06";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedCMTFailBean", 46);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedCMTFailBean", 46, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedCMTFailBean", deliveryID, 46);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);
    }

    /**
     * Test illegal access from onMessage on a CMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> onMessage.getTimerService() allows
     * </ol>
     */
    public void testCMTOnMessage() throws Exception {
        String deliveryID = "test07";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedCMTBean", 47);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedCMTBean", 47, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedCMTBean", deliveryID, 47);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedCMTBean.results[7] != null) {
                checkResults(MDBTimedCMTBean.results[7]);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test illegal access from setMessageDrivenContext on a CMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> setMessageDrivenContext.getTimerService() fails with IllegalStateException
     * </ol>
     */
    public void testSetMessageDrivenContext() throws Exception {
        String deliveryID = "test08";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedCMTBean", 48);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedCMTBean", 48, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedCMTBean", deliveryID, 48);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedCMTBean.results[8] != null) {
                checkResults(MDBTimedCMTBean.results[8]);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test illegal access from ejbTimeout on a BMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> EJBContext.getEJBHome() fails with IllegalStateException
     * <li> EJBContext.getEJBLocalHome() fails with IllegalStateException
     * <li> EJBContext.getUserTransaction() allows
     * <li> EJBContext.getCallerPrincipal() works
     * <li> EJBContext.isCallerInRole() works
     * <li> EJBContext.getTimerService() works
     * <li> EJBContext.getRollbackOnly() failed
     * <li> EJBContext.setRollbackOnly() failed
     * </ol>
     */
    public void testBMTEJBTimeoutMessageDrivenContext() throws Exception {
        String deliveryID = "test09";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedBMTBean", 49);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedBMTBean", 49, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedBMTBean", deliveryID, 49);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedBMTBean.results != null) {
                checkResults(MDBTimedBMTBean.results);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test illegal access from setMessageDrivenContext on a BMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> setMessageDrivenContext.getTimerService() fails with IllegalStateException
     * </ol>
     */
    public void testBMTOnMessage() throws Exception {
        String deliveryID = "test11";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedBMTBean", 50);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedBMTBean", 50, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedBMTBean", deliveryID, 50);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);

        int counter = 0;
        System.out.println("Waiting for results ...");
        while (counter < maxSleepTime) {
            if (MDBTimedBMTBean.results != null) {
                checkResults(MDBTimedBMTBean.results);
                break;
            }
            Thread.sleep(1000);
            counter++;
        }

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * Test illegal access from Constructor on a BMT Message
     * Driven bean that implements the TimedObject interface. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Constructor.getTimerService() fails with IllegalStateException
     * </ol>
     */
    public void testBMTConstructor() throws Exception {
        String deliveryID = "test10";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBTimedBMTFailBean", 51);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("MDBTimedBMTFailBean", 51, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("MDBTimedBMTFailBean", deliveryID, 51);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);
    }
}