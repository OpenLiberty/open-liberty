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

import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTBaseMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;

import componenttest.app.FATServlet;

/**
 * Test Name: MDTest_JMS
 *
 * Test Descriptions:
 *
 * This class implements a ejb container FVT testcase for testing EJB container
 * component implementation of the JCA 1.6 MessageEndpointFactory and MessageEndpoint
 * interfaces. <p>
 *
 * In the description of each test, option A message delivery implies the
 * following sequence of invocations occur: <p>
 * <ol>
 * <li>RA invokes MessageEndpointFactory.createEndpoint method to create a MessageEndpoint proxy.
 * <li>RA uses MessageEndpoint proxy to invoke a method of the messaging type interface (e.g MDB method).
 * <li>RA invokes MessageEndpoint.release method.
 * </ol>
 * <p>
 * Option B message delivery implies the following sequence of invocations occur:
 * <ol>
 * <li>RA invokes MessageEndpointFactory.createEndpoint method to create a MessageEndpoint proxy.
 * <li>RA invokes MessageEndpoint.beforeDelivery on created MessageEndpoint proxy.
 * <li>RA uses MessageEndpoint proxy to invoke a method of the messaging type interface (e.g MDB method).
 * <li>RA invokes MessageEndpoint.afterDelivery on created MessageEndpoint proxy.
 * <li>RA invokes MessageEndpoint.release method.
 * </ol>
 * <p>
 * In the description, "MessageEndpoint is created with an XAResource object" means a
 * non-null reference to an XAResource object provided by RA is passed on the createEndpoint
 * invocation. "MessageEndpoint is created without an XAResource object" means a
 * null reference is passed on the createEndpoint invocation for the XAResource parameter.
 *
 * Test Matrix:
 * <ol>
 * <li>Testing message delivery options
 * <li> XAResource Delivery Option Tx Attribute
 * <li>testRequiredXAOptionA: Yes A Required
 * <li>testRequiredNonXAOptionA: No A Required
 * <li>testRequiredXAOptionB: Yes B Required
 * <li>testRequiredNonXAOptionB: No B Required
 * <li>testNotSupportedXAOptionA: Yes A NotSupported
 * <li>testNotSupportedNonXAOptionA: No A NotSupported
 * <li>testNotSupportedXAOptionB: Yes B NotSupported
 * <li>testNotSupportedNonXAOptionB: No B NotSupported
 * <li>testBMTXAOptionA: Yes A Bean Managed
 * <li>testBMTNonXAOptionA: No A Bean Managed
 * <li>testBMTXAOptionB: Yes B Bean Managed
 * <li>testBMTNonXAOptionB: No B Bean Managed
 * </ol>
 */
public class JMS_MDServlet extends FATServlet {
    private FVTBaseMessageProvider baseProvider = null;

    public void prepareTRA() throws Exception {
        baseProvider = (FVTBaseMessageProvider) new InitialContext().lookup("java:comp/env/BaseMessageProvider");
        System.out.println("Looked up BaseMessageProvider");
        baseProvider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery and that commit is driven on the XAResource
     * object provided by the RA.
     */
    public void testRequiredXAOptionA() throws Exception {
        String deliveryID = "MD_test4a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSRequired", 41);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSRequired", 41, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("CMTJMSRequired", "message1", 41);

        System.out.println("testRequiredXAOptionA message: " + message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery and that a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testRequiredNonXAOptionA() throws Exception {
        String deliveryID = "MD_test4b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSRequired");

        // Add a option A delivery.
        message.add("CMTJMSRequired", "message1");
        message.add("CMTJMSRequired", "message2");

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and commit method on the XAResource object
     * provided by the RA is driven.
     */
    public void testRequiredXAOptionB() throws Exception {
        String deliveryID = "MD_test4c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSRequired", 43);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSRequired", 43, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("CMTJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 43);
        message.add("CMTJMSRequired", "message1", m, 43);
        message.addDelivery("CMTJMSRequired", FVTMessage.AFTER_DELIVERY, null, 43);
        // message.addRelease("CMTJMSRequired", 43);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit should be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a MDB method
     * deployed with <i>Required</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testRequiredNonXAOptionB() throws Exception {
        String deliveryID = "MD_test4d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSRequired");

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("CMTJMSRequired", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTJMSRequired", "message1", m);
        message.addDelivery("CMTJMSRequired", FVTMessage.AFTER_DELIVERY, null);

        message.addDelivery("CMTJMSRequired", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTJMSRequired", "message2", m);
        message.addDelivery("CMTJMSRequired", FVTMessage.AFTER_DELIVERY, null);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Not Supported</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that the XAResource object provided
     * by the RA is not enlisted in the local transaction.
     */
    public void testNotSupportedXAOptionA() throws Exception {
        String deliveryID = "MD_test5a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSNotSupported", 51);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSNotSupported", 51, new FVTXAResourceImpl());

        // Add a option A delivery
        message.add("CMTJMSNotSupported", "message1", 51);
        message.add("CMTJMSNotSupported", "message2", 51);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns false for a method with the Required attribute.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a MDB method deployed
     * with <i>Not Supported</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testNotSupportedNonXAOptionA() throws Exception {
        String deliveryID = "MD_test5b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSNotSupported");

        // Add a option A delivery
        message.add("CMTJMSNotSupported", "message1");
        //message.addRelease("CMTJMSNotSupported", 51);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns false for a method with the Required attribute.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a MDB method deployed
     * with <i>Not Supported</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and that a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testNotSupportedXAOptionB() throws Exception {
        String deliveryID = "MD_test5c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSNotSupported", 53);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTJMSNotSupported", 53, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = javax.jms.MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("CMTJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m, 53);
        message.add("CMTJMSNotSupported", "message1", m, 53);
        message.addDelivery("CMTJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 53);

        message.addDelivery("CMTJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m, 53);
        message.add("CMTJMSNotSupported", "message2", m, 53);
        message.addDelivery("CMTJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 53);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a MDB method
     * deployed with <i>Not Supported</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testNotSupportedNonXAOptionB() throws Exception {
        String deliveryID = "MD_test5d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTJMSNotSupported");

        // Add a option B transacted delivery to another instance.
        Method m = javax.jms.MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("CMTJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTJMSNotSupported", "message1", m);
        message.addDelivery("CMTJMSNotSupported", FVTMessage.AFTER_DELIVERY, null);
        // message.addRelease("CMTJMSNotSupported");

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.",
                   !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created with a XAResource object and
     * option A message delivery is used to invoke a method in an MDB deployed as
     * <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that the XAResource object provided
     * by the RA is not enlisted in the global transaction that is
     * started by the MDB method.
     */
    public void testBMTXAOptionA() throws Exception {
        String deliveryID = "MD_test6a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTJMS", 61);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTJMS", 61, new FVTXAResourceImpl());

        // Add a option A
        message.add("BMTJMS", "message1", 61);
        // message.addRelease("BMTJMS", 61);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns false for a method in a BMT MDB.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a method in an MDB deployed as
     * <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that a RA XAResource object is not
     * enlisted in the global transaction that is started by the MDB method.
     */
    public void testBMTNonXAOptionA() throws Exception {
        String deliveryID = "MD_test6b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTJMS");

        // Add a option A
        message.add("BMTJMS", "message1");
        message.add("BMTJMS", "message2");

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns false for a method in a BMT MDB.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a method in an MDB deployed
     * as <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and that a RA XAResource object is not
     * enlisted in the global transaction started by the MDB method.
     */
    public void testBMTXAOptionB() throws Exception {
        String deliveryID = "MD_test6c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTJMS", 63);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTJMS", 63, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = javax.jms.MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("BMTJMS", FVTMessage.BEFORE_DELIVERY, m, 63);
        message.add("BMTJMS", "message1", m, 63);
        message.addDelivery("BMTJMS", FVTMessage.AFTER_DELIVERY, null, 63);
        // message.addRelease("BMTJMS", 63);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns false for a method in a BMT MDB.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a method deployed as
     * <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and that a RA XAResource object is not
     * enlisted in the global transaction started by the MDB method.
     */
    public void testBMTNonXAOptionB() throws Exception {
        String deliveryID = "MD_test6d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTJMS");

        // Add a option B transacted delivery to another instance.
        Method m = javax.jms.MessageListener.class.getMethod("onMessage", new Class[] { Message.class });
        message.addDelivery("BMTJMS", FVTMessage.BEFORE_DELIVERY, m);
        message.add("BMTJMS", "message1", m);
        message.addDelivery("BMTJMS", FVTMessage.AFTER_DELIVERY, null);

        message.addDelivery("BMTJMS", FVTMessage.BEFORE_DELIVERY, m);
        message.add("BMTJMS", "message2", m);
        message.addDelivery("BMTJMS", FVTMessage.AFTER_DELIVERY, null);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns false for a method in a BMT MDB.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }
}
