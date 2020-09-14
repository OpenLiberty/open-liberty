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

import java.lang.reflect.Method;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.InitialContext;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkEvent;

import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.core.XidImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;

import componenttest.app.FATServlet;

/**
 * Test Name: MMTest_JMS
 *
 * Test Descriptions:
 *
 * Scenarios to cover sending multiple messages to different MDBs.
 * This test class covers JMS MDBs.
 *
 * Test Matrix:
 * <ol>
 * <li>Testing message delivery options
 * <li> Delivery Option Imported Tx
 * <li>testOptionA: A No
 * <li>testOptionAImportedTx: A Yes
 * <li>testOptionB: B No
 * <li>testOptionBImportedTx: B Yes
 * </ol>
 */
public class JMS_MMServlet extends FATServlet {
    private FVTMessageProvider provider = null;

    public void prepareTRA() throws Exception {
        provider = (FVTMessageProvider) new InitialContext().lookup("java:comp/env/MessageProvider");
        System.out.println("Looked up MessageProvider");
        provider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery and that commit is driven on the XAResource
     * object provided by the RA.
     */
    public void testOptionA() throws Exception {
        String deliveryID = "MM_test1a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTJMSRequired", 11);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSRequired", 11, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.add("CMTJMSRequired", "message1", m1, 11);

        // The second message to BMT MDB
        message.addTestResult("BMTJMS", 11);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTJMS", 11, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.add("BMTJMS", "message1", m2, 11);

        System.out.println(message.toString());

        provider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTJMSRequired", 11);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTJMS", 11);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery and that a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testOptionAImportedTx() throws Exception {
        String deliveryID = "MM_test1b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTJMSRequired", 12);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSRequired", 12, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.add("CMTJMSRequired", "message1", m1, 12);

        // The second message to BMT MDB
        message.addTestResult("BMTJMS", 12);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTJMS", 12, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.add("BMTJMS", "message1", m2, 12);

        System.out.println(message.toString());

        XidImpl xid = XidImpl.createXid(10);
        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) //patch
        {
            // Now commit xid
            xaTerm.commit(xid, false);
        }

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTJMSRequired", 12);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTJMS", 12);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and commit method on the XAResource object
     * provided by the RA is driven.
     */
    public void testOptionB() throws Exception {
        String deliveryID = "MM_test1c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTJMSRequired", 13);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSRequired", 13, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("CMTJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 13);
        message.add("CMTJMSRequired", "message1", m1, 13);
        message.addDelivery("CMTJMSRequired", FVTMessage.AFTER_DELIVERY, m1, 13);

        // The second message to BMT MDB
        message.addTestResult("BMTJMS", 13);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTJMS", 13, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("BMTJMS", FVTMessage.BEFORE_DELIVERY, m2, 13);
        message.add("BMTJMS", "message1", m2, 13);
        message.addDelivery("BMTJMS", FVTMessage.AFTER_DELIVERY, m2, 13);

        System.out.println(message.toString());

        provider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTJMSRequired", 13);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in a global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTJMS", 13);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a MDB method
     * deployed with <i>Required</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testOptionBImportedTx() throws Exception {
        String deliveryID = "MM_test1d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTJMSRequired", 14);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSRequired", 14, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("CMTJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 14);
        message.add("CMTJMSRequired", "message1", m1, 14);
        message.addDelivery("CMTJMSRequired", FVTMessage.AFTER_DELIVERY, m1, 14);

        // The second message to BMT MDB
        message.addTestResult("BMTJMS", 14);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTJMS", 14, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("BMTJMS", FVTMessage.BEFORE_DELIVERY, m2, 14);
        message.add("BMTJMS", "message1", m2, 14);
        message.addDelivery("BMTJMS", FVTMessage.AFTER_DELIVERY, m2, 14);

        System.out.println(message.toString());

        XidImpl xid = XidImpl.createXid(10);
        provider.sendMessageWait(deliveryID, message, WorkEvent.WORK_COMPLETED, xid, FVTMessageProvider.DO_WORK);

        // Now prepare xid
        XATerminator xaTerm = provider.getXATerminator();
        if (xaTerm.prepare(xid) == javax.transaction.xa.XAResource.XA_OK) //patch
        {
            // Now commit xid
            xaTerm.commit(xid, false);
        }

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTJMSRequired", 14);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in a global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTJMS", 14);
        assertTrue("The second delivery should contain only one test result.", results.length == 1);
        assertFalse("isDeliveryTransacted returns false for a method with the BMT attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results[0].mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        provider.releaseDeliveryId(deliveryID);
    }
}
