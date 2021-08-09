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

import javax.naming.InitialContext;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkEvent;

import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.core.XidImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;

import componenttest.app.FATServlet;

/**
 * Test Name: MMTest_JMS
 *
 * Test Descriptions:
 *
 * Scenarios to cover sending multiple messages to different MDBs.
 * This test class covers Non-JMS MDBs.
 *
 * Test Matrix:
 * <ol>
 * <li>Testing message delivery options
 * <li> Delivery Option Imported Tx
 * <li>test2a: A No
 * <li>test2b: A Yes
 * <li>test2c: B No
 * <li>test2d: B Yes
 * </ol>
 */
public class NonJMS_MMServlet extends FATServlet {
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
    public void testNonJMSOptionANonImportedTx() throws Exception {
        String deliveryID = "MM_test2a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTNonJMSRequired", 21);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 21, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "message1", m1, 21);

        // The second message to BMT MDB
        message.addTestResult("BMTNonJMS", 21);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTNonJMS", 21, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("BMTNonJMS", "message1", m2, 21);

        System.out.println(message.toString());

        provider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTNonJMSRequired", 21);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTNonJMS", 21);
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
    public void testNonJMSOptionAImportedTx() throws Exception {
        String deliveryID = "MM_test2b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTNonJMSRequired", 22);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 22, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "message1", m1, 22);

        // The second message to BMT MDB
        message.addTestResult("BMTNonJMS", 22);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTNonJMS", 22, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("BMTNonJMS", "message1", m2, 22);

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

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTNonJMSRequired", 22);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results[0].optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTNonJMS", 22);
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
    public void testNonJMSOptionBNonImportedTx() throws Exception {
        String deliveryID = "MM_test2c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTNonJMSRequired", 23);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 23, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 23);
        message.add("CMTNonJMSRequired", "message1", m1, 23);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 23);

        // The second message to BMT MDB
        message.addTestResult("BMTNonJMS", 23);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTNonJMS", 23, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m2, 23);
        message.add("BMTNonJMS", "message1", m2, 23);
        message.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 23);

        System.out.println(message.toString());

        provider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTNonJMSRequired", 23);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource is enlisted in a global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTNonJMS", 23);
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
    public void testNonJMSOptionBImportedTx() throws Exception {
        String deliveryID = "MM_test2d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();

        // The first message to CMT MDB
        message.addTestResult("CMTNonJMSRequired", 24);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 24, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m1 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 24);
        message.add("CMTNonJMSRequired", "message1", m1, 24);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, m1, 24);

        // The second message to BMT MDB
        message.addTestResult("BMTNonJMS", 24);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTNonJMS", 24, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m2, 24);
        message.add("BMTNonJMS", "message1", m2, 24);
        message.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, m2, 24);

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

        MessageEndpointTestResults results[] = provider.getTestResults(deliveryID, "CMTNonJMSRequired", 24);
        assertTrue("The first delivery should contain only one test result.", results.length == 1);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results[0].isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results[0].getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results[0].optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results[0].mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results[0].raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in a global transaction.", results[0].raXaResourceEnlisted());

        results = provider.getTestResults(deliveryID, "BMTNonJMS", 24);
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