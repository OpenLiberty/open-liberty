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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkEvent;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.ConcurBMTException;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.ConcurBMTNonJMS;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.ConcurCMTException;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.ConcurCMTNonJMS;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.ConcurrencyInfo;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.EndpointBMTException;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.EndpointBMTNonJMS;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.EndpointCMTException;
import com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb.EndpointCMTNonJMS;
import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.core.XidImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;
import com.ibm.ws.ejbcontainer.fat.rar.work.FVTWorkDispatcher;

import componenttest.app.FATServlet;

/**
 * Test Name: MsgEndpoint_CDTest
 *
 * Test Descriptions:
 *
 * This test class is to cover complex message delivery scenarios. This includes
 * multiple messages delivered to different MDBs serially and concurrently.
 *
 * Test Matrix:
 *
 * Testing message delivery options
 *
 * testImportedTxOptionBNonXA_BMT_NotSupported
 * testNonImportedTxOptionAXA_NotSupported_Required
 * testImportedTxOptionBNonXATwoMessages_Required_BMT
 * testNonImportedTxOptionBXA_BMT_NotSupported
 * testImportedTxOptionANonXAException_NotSupported_Required
 * testNonImportedTxOptionBXAException_Required_BMT
 * testNonImportedTxOptionAXA_BMT_NotSupported
 * testImportedTxOptionANonXA_NotSupported_Required
 * testImportedTxOptionBNonXA_Required_BMT
 * testImportedTxOptionAXA_BMT_NotSupported
 * testImportedTxOptionBNonXA_NotSupported_Required
 * testNonImportedTxOptionBXA_Required_BMT
 * testNonImportedTxOptionANonXA_BMT
 * testImportedTxOptionBNonXA_NotSupported
 * testNonImportedTxOptionAXA_Required
 *
 */
public class MsgEndpoint_CDServlet extends FATServlet {
    private FVTMessageProvider provider = null;

    public void prepareTRA() throws Exception {
        provider = (FVTMessageProvider) new InitialContext().lookup("java:comp/env/MessageProvider");
        System.out.println("Looked up MessageProvider");
        provider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 BMT Yes B No
     * MDB2 NotSupported Yes B No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionBNonXA_BMT_NotSupported() throws Exception {
        String deliveryID1 = "test1a_1";
        String deliveryID2 = "test1a_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurBMTNonJMS.concurrentInfo = ci;
        ConcurBMTNonJMS.syncObject = syncObject;
        ConcurCMTNonJMS.concurrentInfo = ci;
        ConcurCMTNonJMS.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("ConcurBMTNonJMS", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("ConcurBMTNonJMS", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("ConcurBMTNonJMS", "message1", m1, 1001);
        message1.addDelivery("ConcurBMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 1001);

        XidImpl xid1 = XidImpl.createXid(111, 101); // d248457.1

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurCMTNonJMSNotSupported", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("ConcurCMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("ConcurCMTNonJMSNotSupported", "message2", m2, 1002);
        message2.addDelivery("ConcurCMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 1002);

        XidImpl xid2 = XidImpl.createXid(112, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            FATHelper.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        if (xaTerm2.prepare(xid2) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm2.commit(xid2, false);
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurBMTNonJMS", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurCMTNonJMSNotSupported", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 NotSupported No A Yes
     * MDB2 Required No A Yes
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testNonImportedTxOptionAXA_NotSupported_Required() throws Exception {
        String deliveryID1 = "test1b_1";
        String deliveryID2 = "test1b_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurBMTNonJMS.concurrentInfo = ci;
        ConcurBMTNonJMS.syncObject = syncObject;
        ConcurCMTNonJMS.concurrentInfo = ci;
        ConcurCMTNonJMS.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option A delivery.
        message1.addTestResult("ConcurCMTNonJMSNotSupported", 1001);
        message1.addXAResource("ConcurCMTNonJMSNotSupported", 1001, new FVTXAResourceImpl());
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.add("ConcurCMTNonJMSNotSupported", "message1", m1, 1001);

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurCMTNonJMSRequired", 1002);
        message2.addXAResource("ConcurCMTNonJMSRequired", 1002, new FVTXAResourceImpl());
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.add("ConcurCMTNonJMSRequired", "message2", m2, 1002);

        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            FATHelper.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurCMTNonJMSNotSupported", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurCMTNonJMSRequired", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 Required Yes B No
     * MDB2 BMT Yes B No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionBNonXATwoMessages_Required_BMT() throws Exception {
        String deliveryID1 = "test1c_1";
        String deliveryID2 = "test1c_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurBMTNonJMS.concurrentInfo = ci;
        ConcurBMTNonJMS.syncObject = syncObject;
        ConcurCMTNonJMS.concurrentInfo = ci;
        ConcurCMTNonJMS.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("ConcurCMTNonJMSRequired", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("ConcurCMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("ConcurCMTNonJMSRequired", "message1", m1, 1001);
        message1.addDelivery("ConcurCMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 1001);

        XidImpl xid1 = XidImpl.createXid(131, 101); // d248457.1
        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurBMTNonJMS", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("ConcurBMTNonJMS", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("ConcurBMTNonJMS", "message2", m2, 1002);
        message2.addDelivery("ConcurBMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 1002);

        XidImpl xid2 = XidImpl.createXid(132, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 2 seconds for message delivery");
            FATHelper.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        if (xaTerm2.prepare(xid2) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm2.commit(xid2, false);
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurCMTNonJMSRequired", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurBMTNonJMS", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     * One of the two MDBs throws RuntimeException at the beginning.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 BMT No B Yes
     * MDB2 NotSupported No B Yes
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testNonImportedTxOptionBXA_BMT_NotSupported() throws Exception {
        String deliveryID1 = "test2a_1";
        String deliveryID2 = "test2a_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurBMTNonJMS.concurrentInfo = ci;
        ConcurBMTNonJMS.syncObject = syncObject;
        ConcurCMTException.concurrentInfo = ci;
        ConcurCMTException.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("ConcurBMTNonJMS", 1001);
        message1.addXAResource("ConcurBMTNonJMS", 1001, new FVTXAResourceImpl());
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("ConcurBMTNonJMS", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("ConcurBMTNonJMS", "message1", m1, 1001);
        message1.addDelivery("ConcurBMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 1001);

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // Initialize the entry flag
        ConcurCMTException.mdbInvokedTheFirstTime = true;

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurCMTExceptionNotSupported", 1002);
        message2.addXAResource("ConcurCMTExceptionNotSupported", 1002, new FVTXAResourceImpl());
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("ConcurCMTExceptionNotSupported", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("ConcurCMTExceptionNotSupported", "message2", m2, 1002);
        message2.addDelivery("ConcurCMTExceptionNotSupported", FVTMessage.AFTER_DELIVERY, null, 1002);

        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            FATHelper.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurBMTNonJMS", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurCMTExceptionNotSupported", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     * One of the two MDBs throws RuntimeException at the beginning.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 NotSupported Yes A No
     * MDB2 Required Yes A No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionANonXAException_NotSupported_Required() throws Exception {
        String deliveryID1 = "test2b_1";
        String deliveryID2 = "test2b_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurCMTNonJMS.concurrentInfo = ci;
        ConcurCMTNonJMS.syncObject = syncObject;
        ConcurCMTException.concurrentInfo = ci;
        ConcurCMTException.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option A delivery.
        message1.addTestResult("ConcurCMTNonJMSNotSupported", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.add("ConcurCMTNonJMSNotSupported", "message1", m1, 1001);

        XidImpl xid1 = XidImpl.createXid(221, 101); // d248457.1
        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        // Initialize the entry flag
        ConcurCMTException.mdbInvokedTheFirstTime = true;

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurCMTExceptionRequired", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.add("ConcurCMTExceptionRequired", "message2", m2, 1002);

        XidImpl xid2 = XidImpl.createXid(222, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            FATHelper.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        try {

            if (xaTerm2.prepare(xid2) == XAResource.XA_OK) {
                // Now commit xid
                xaTerm2.commit(xid2, false);
            }

            fail("Global tran xid 2 should be rolled back by the MDB.");

        } catch (XAException xae) {
            // xaTerm2.rollback(xid2);
            System.out.println("Global tran xid 2 is rolled back.");
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurCMTNonJMSNotSupported", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurCMTExceptionRequired", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     * One of the two MDBs throws RuntimeException at the beginning.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 Required No B Yes
     * MDB2 BMT No B Yes
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testNonImportedTxOptionBXAException_Required_BMT() throws Exception {
        String deliveryID1 = "test2c_1";
        String deliveryID2 = "test2c_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurCMTNonJMS.concurrentInfo = ci;
        ConcurCMTNonJMS.syncObject = syncObject;
        ConcurBMTException.concurrentInfo = ci;
        ConcurBMTException.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("ConcurCMTNonJMSRequired", 1001);
        message1.addXAResource("ConcurCMTNonJMSRequired", 1001, new FVTXAResourceImpl());
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("ConcurCMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("ConcurCMTNonJMSRequired", "message1", m1, 1001);
        message1.addDelivery("ConcurCMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 1001);

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // Initialize the entry flag
        ConcurBMTException.mdbInvokedTheFirstTime = true;

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurBMTException", 1002);
        message2.addXAResource("ConcurBMTException", 1002, new FVTXAResourceImpl());
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("ConcurBMTException", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("ConcurBMTException", "message2", m2, 1002);
        message2.addDelivery("ConcurBMTException", FVTMessage.AFTER_DELIVERY, null, 1002);

        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            FATHelper.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurCMTNonJMSRequired", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurBMTException", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Two different MDBs. Each MDB receives two serial messages.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 BMT No A Yes
     * MDB2 NotSupported No A Yes
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testNonImportedTxOptionAXA_BMT_NotSupported() throws Exception {
        String deliveryID1 = "test3a_1";
        String deliveryID2 = "test3a_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        EndpointBMTNonJMS.initMsgLatch();

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("BMTNonJMS", 1001);
        message1.addXAResource("BMTNonJMS", 1001, new FVTXAResourceImpl());
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.add("BMTNonJMS", "message11", m1, 1001);
        message1.add("BMTNonJMS", "message12", m1, 1001);

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        EndpointCMTNonJMS.initMsgLatch();

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("CMTNonJMSNotSupported", 1002);
        message2.addXAResource("CMTNonJMSNotSupported", 1002, new FVTXAResourceImpl());
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.add("CMTNonJMSNotSupported", "message21", m2, 1002);
        message2.add("CMTNonJMSNotSupported", "message22", m2, 1002);

        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        System.out.println("Waiting up to 3 minutes for message delivery");
        EndpointBMTNonJMS.svMsgLatch.await(3 * 60, TimeUnit.SECONDS);
        EndpointCMTNonJMS.svMsgLatch2.await(3 * 60, TimeUnit.SECONDS);

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "BMTNonJMS", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "CMTNonJMSNotSupported", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 NotSupported Yes A No
     * MDB2 Required Yes A No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionANonXA_NotSupported_Required() throws Exception {
        String deliveryID1 = "test3b_1";
        String deliveryID2 = "test3b_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        EndpointCMTNonJMS.initMsgLatch();

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option A delivery.
        message1.addTestResult("CMTNonJMSNotSupported", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.add("CMTNonJMSNotSupported", "message11", m1, 1001);
        message1.add("CMTNonJMSNotSupported", "message12", m1, 1001);

        XidImpl xid1 = XidImpl.createXid(321, 101); // d248457.1
        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("CMTNonJMSRequired", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.add("CMTNonJMSRequired", "message21", m2, 1002);
        message2.add("CMTNonJMSRequired", "message22", m2, 1002);

        XidImpl xid2 = XidImpl.createXid(322, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        System.out.println("Waiting up to 3 minutes for message delivery");
        EndpointCMTNonJMS.svMsgLatch1.await(3 * 60, TimeUnit.SECONDS);
        EndpointCMTNonJMS.svMsgLatch2.await(3 * 60, TimeUnit.SECONDS);

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        if (xaTerm2.prepare(xid2) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm2.commit(xid2, false);
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "CMTNonJMSNotSupported", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "CMTNonJMSRequired", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to two different MDBs. Synchronization
     * is added to both MDBs message listener methods to ensure the message deliveries are concurrent.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 Required Yes B No
     * MDB2 BMT Yes B No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionBNonXA_Required_BMT() throws Exception {
        String deliveryID1 = "test3c_1";
        String deliveryID2 = "test3c_2";

        prepareTRA();

        EndpointCMTNonJMS.initMsgLatch();
        FVTWorkDispatcher.initMessageLatch(2);

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("CMTNonJMSRequired", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("CMTNonJMSRequired", "message11", m1, 1001);
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 1001);
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("CMTNonJMSRequired", "message12", m1, 1001);
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 1001);

        XidImpl xid1 = XidImpl.createXid(331, 101); // d248457.1
        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        EndpointBMTNonJMS.initMsgLatch();

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("BMTNonJMS", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("BMTNonJMS", "message21", m2, 1002);
        message2.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 1002);
        message2.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("BMTNonJMS", "message22", m2, 1002);
        message2.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 1002);

        XidImpl xid2 = XidImpl.createXid(332, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        System.out.println("Waiting up to 3 minutes for message delivery");
        EndpointCMTNonJMS.svMsgLatch1.await(3 * 60, TimeUnit.SECONDS);
        EndpointBMTNonJMS.svMsgLatch.await(3 * 60, TimeUnit.SECONDS);

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        if (xaTerm2.prepare(xid2) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm2.commit(xid2, false);
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "CMTNonJMSRequired", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "BMTNonJMS", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Two different MDBs. Each MDB receives two serial messages.
     * One of the two MDBs throws RuntimeException at the first entry of onStringMessage
     * method. The MDB retries the second time and complete the method call without
     * exception.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 BMT Yes A Yes
     * MDB2 NotSupported Yes A Yes
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionAXA_BMT_NotSupported() throws Exception {
        String deliveryID1 = "test4a_1";
        String deliveryID2 = "test4a_2";

        prepareTRA();

        EndpointBMTNonJMS.initMsgLatch();
        FVTWorkDispatcher.initMessageLatch(2);

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("BMTNonJMS", 1001);
        message1.addXAResource("BMTNonJMS", 1001, new FVTXAResourceImpl());
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.add("BMTNonJMS", "message11", m1, 1001);
        message1.add("BMTNonJMS", "message12", m1, 1001);

        XidImpl xid1 = XidImpl.createXid(411, 101); // d248457.1
        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        // Initialize the entry flag
        EndpointCMTException.mdbInvokedTheFirstTime = true;
        EndpointCMTException.initMsgLatch();

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("EndpointCMTExceptionNotSupported", 1002);
        message2.addXAResource("EndpointCMTExceptionNotSupported", 1002, new FVTXAResourceImpl());
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.add("EndpointCMTExceptionNotSupported", "message21", m2, 1002);
        message2.add("EndpointCMTExceptionNotSupported", "message22", m2, 1002);

        XidImpl xid2 = XidImpl.createXid(412, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        System.out.println("Waiting up to 3 minutes for message delivery");
        EndpointBMTNonJMS.svMsgLatch.await(3 * 60, TimeUnit.SECONDS);
        EndpointCMTException.svMsgLatch.await(3 * 60, TimeUnit.SECONDS);

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        if (xaTerm2.prepare(xid2) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm2.commit(xid2, false);
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "BMTNonJMS", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "EndpointCMTExceptionNotSupported", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        // assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Two different MDBs. Each MDB receives two serial messages.
     * One of the two MDBs throws RuntimeException at the first entry of onStringMessage
     * method. The MDB retries the second time and complete the method call without
     * exception.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 NotSupported Yes B No
     * MDB2 Required Yes B No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionBNonXA_NotSupported_Required() throws Exception {
        String deliveryID1 = "test4b_1";
        String deliveryID2 = "test4b_2";

        prepareTRA();

        EndpointCMTNonJMS.initMsgLatch();
        FVTWorkDispatcher.initMessageLatch(2);

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option A delivery.
        message1.addTestResult("CMTNonJMSNotSupported", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("CMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("CMTNonJMSNotSupported", "message11", m1, 1001);
        message1.addDelivery("CMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 1001);
        message1.addDelivery("CMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("CMTNonJMSNotSupported", "message12", m1, 1001);
        message1.addDelivery("CMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 1001);

        XidImpl xid1 = XidImpl.createXid(421, 101); // d248457.1
        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        // Initialize the entry flag
        EndpointCMTException.mdbInvokedTheFirstTime = true;
        EndpointCMTException.initMsgLatch();

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("EndpointCMTExceptionRequired", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("EndpointCMTExceptionRequired", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("EndpointCMTExceptionRequired", "message21", m2, 1002);
        message2.addDelivery("EndpointCMTExceptionRequired", FVTMessage.AFTER_DELIVERY, null, 1002);
        message2.addDelivery("EndpointCMTExceptionRequired", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("EndpointCMTExceptionRequired", "message22", m2, 1002);
        message2.addDelivery("EndpointCMTExceptionRequired", FVTMessage.AFTER_DELIVERY, null, 1002);

        XidImpl xid2 = XidImpl.createXid(422, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        System.out.println("Waiting up to 3 minutes for message delivery");
        EndpointCMTNonJMS.svMsgLatch1.await(3 * 60, TimeUnit.SECONDS);
        EndpointCMTException.svMsgLatch.await(3 * 60, TimeUnit.SECONDS);

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        System.out.println("Global tran xid1 should be committed.");

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        try {
            if (xaTerm2.prepare(xid2) == XAResource.XA_OK) {
                // Now commit xid
                xaTerm2.commit(xid2, false);
            }

            fail("Global tran xid 2 should be rolled back by the MDB.");
        } catch (XAException xae) {
            // xaTerm2.rollback(xid2);
            System.out.println("Global tran xid 2 is rolled back.");
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "CMTNonJMSNotSupported", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "EndpointCMTExceptionRequired", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Two different MDBs. Each MDB receives two serial messages.
     * One of the two MDBs throws RuntimeException at the first entry of onStringMessage
     * method. The MDB retries the second time and complete the method call without
     * exception.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 Required No B Yes
     * MDB2 BMT No B Yes
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testNonImportedTxOptionBXA_Required_BMT() throws Exception {
        String deliveryID1 = "test4c_1";
        String deliveryID2 = "test4c_2";

        prepareTRA();

        EndpointCMTNonJMS.initMsgLatch();
        FVTWorkDispatcher.initMessageLatch(2);

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("CMTNonJMSRequired", 1001);
        message1.addXAResource("CMTNonJMSRequired", 1001, new FVTXAResourceImpl());
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("CMTNonJMSRequired", "message11", m1, 1001);
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 1001);
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("CMTNonJMSRequired", "message12", m1, 1001);
        message1.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 1001);

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // Initialize the entry flag
        EndpointBMTException.mdbInvokedTheFirstTime = true;
        EndpointBMTException.initMsgLatch();

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("EndpointBMTException", 1002);
        message2.addXAResource("EndpointBMTException", 1002, new FVTXAResourceImpl());
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("EndpointBMTException", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("EndpointBMTException", "message21", m2, 1002);
        message2.addDelivery("EndpointBMTException", FVTMessage.AFTER_DELIVERY, null, 1002);
        message2.addDelivery("EndpointBMTException", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("EndpointBMTException", "message22", m2, 1002);
        message2.addDelivery("EndpointBMTException", FVTMessage.AFTER_DELIVERY, null, 1002);

        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        System.out.println("Waiting up to 3 minutes for message delivery");
        EndpointCMTNonJMS.svMsgLatch1.await(3 * 60, TimeUnit.SECONDS);
        EndpointBMTException.svMsgLatch.await(3 * 60, TimeUnit.SECONDS);

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "CMTNonJMSRequired", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "EndpointBMTException", 1002);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        // assertTrue("Number of messages delivered is 2", results[0].getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to the same MDB concurrently.
     * Synchronization is added to the MDB message listener method to ensure the message
     * deliveries are concurrent.
     *
     * According to JCA 1.5, RA must not use the same endpoint instance concurrently. This test
     * case is to verify the exceptional behavior of EJB container.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB BMT No A No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testNonImportedTxOptionANonXA_BMT() throws Exception {
        String deliveryID1 = "test5a_1";
        String deliveryID2 = "test5a_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurBMTNonJMS.concurrentInfo = ci;
        ConcurBMTNonJMS.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("ConcurBMTNonJMS", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.add("ConcurBMTNonJMS", "message1", m1, 1001);

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurBMTNonJMS", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.add("ConcurBMTNonJMS", "message2", m2, 1002);

        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            Thread.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurBMTNonJMS", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurBMTNonJMS", 1002);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to the same MDB concurrently.
     * Synchronization is added to the MDB message listener method to ensure the message
     * deliveries are concurrent.
     *
     * According to JCA 1.5, RA must not use the same endpoint instance concurrently. This test
     * case is to verify the exceptional behavior of EJB container.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 NotSupported Yes B No
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testImportedTxOptionBNonXA_NotSupported() throws Exception {
        String deliveryID1 = "test5b_1";
        String deliveryID2 = "test5b_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurCMTNonJMS.concurrentInfo = ci;
        ConcurCMTNonJMS.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option A delivery.
        message1.addTestResult("ConcurCMTNonJMSNotSupported", 1001);
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.addDelivery("ConcurCMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m1, 1001);
        message1.add("ConcurCMTNonJMSNotSupported", "message1", m1, 1001);
        message1.addDelivery("ConcurCMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 1001);

        XidImpl xid1 = XidImpl.createXid(521, 101); // d248457.1
        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, xid1, FVTMessageProvider.START_WORK);

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurCMTNonJMSNotSupported", 1002);
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.addDelivery("ConcurCMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m2, 1002);
        message2.add("ConcurCMTNonJMSNotSupported", "message2", m2, 1002);
        message2.addDelivery("ConcurCMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 1002);

        XidImpl xid2 = XidImpl.createXid(522, 101); // d248457.1
        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, xid2, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            Thread.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        // Now prepare xid1
        XATerminator xaTerm1 = provider.getXATerminator();
        if (xaTerm1.prepare(xid1) == javax.transaction.xa.XAResource.XA_OK) {
            // Now commit xid
            xaTerm1.commit(xid1, false);
        }

        // Now prepare xid2
        XATerminator xaTerm2 = provider.getXATerminator();
        if (xaTerm2.prepare(xid2) == javax.transaction.xa.XAResource.XA_OK) {
            // Now commit xid
            xaTerm2.commit(xid2, false);
        }

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurCMTNonJMSNotSupported", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurCMTNonJMSNotSupported", 1002);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }

    /**
     * <b>Description: </b> Multiple messages are delivered to the same MDB concurrently.
     * Synchronization is added to the MDB message listener method to ensure the message
     * deliveries are concurrent.
     *
     * According to JCA 1.5, RA must not use the same endpoint instance concurrently. This test
     * case is to verify the exceptional behavior of EJB container.
     *
     * Tx Attribute Imported Tx Option XAResource
     * MDB1 Required No A Yes
     *
     * <b>Results: </b> The deliveries is complete with correct results.
     */
    public void testNonImportedTxOptionAXA_Required() throws Exception {
        String deliveryID1 = "test5c_1";
        String deliveryID2 = "test5c_2";

        prepareTRA();
        FVTWorkDispatcher.initMessageLatch(2);

        // Initialize the concurrency information to 2 concurrent messages
        ConcurrencyInfo ci = new ConcurrencyInfo(2);
        Object syncObject = new Object();
        ConcurCMTNonJMS.concurrentInfo = ci;
        ConcurCMTNonJMS.syncObject = syncObject;

        // construct first message to BMT MDB
        FVTMessage message1 = new FVTMessage();

        // Add a option B delivery.
        message1.addTestResult("ConcurCMTNonJMSRequired", 1001);
        message1.addXAResource("ConcurCMTNonJMSRequired", 1001, new FVTXAResourceImpl());
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message1.add("ConcurCMTNonJMSRequired", "message1", m1, 1001);

        provider.sendMessageWait(deliveryID1, message1, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // construct second message to NotSupported MDB
        FVTMessage message2 = new FVTMessage();

        // Add a option B delivery.
        message2.addTestResult("ConcurCMTNonJMSRequired", 1002);
        message2.addXAResource("ConcurCMTNonJMSRequired", 1002, new FVTXAResourceImpl());
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message2.add("ConcurCMTNonJMSRequired", "message2", m2, 1002);

        provider.sendMessageWait(deliveryID2, message2, WorkEvent.WORK_STARTED, null, FVTMessageProvider.START_WORK);

        // The above messages may be delivered at different speeds, depending
        // on the system (i.e. very slowly on Z), so continue to wait for
        // the messages to be delivered for up to 30 seconds.       d248457
        int waitCount = 0;
        while (ci.getConcurrentMsgNumber() > 0 && waitCount++ < 60) {
            System.out.println("Waiting 0.5 seconds for message delivery");
            Thread.sleep(500);
        }
        System.out.println("Messages not delivered : " + ci.getConcurrentMsgNumber());

        // Once delivered, wait for the messages to complete processing.
        System.out.println("Waiting up to 60 seconds for message completion");
        FVTWorkDispatcher.awaitMessageLatch();

        System.out.println(message2.toString());

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID1, "ConcurCMTNonJMSRequired", 1001);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID2, "ConcurCMTNonJMSRequired", 1002);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
    }
}