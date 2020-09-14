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

import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTBaseMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;

import componenttest.app.FATServlet;

/**
 * Test Name: MDTest_NonJMS
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
 * <li>XAResource Delivery Option Tx Attribute
 * <li>testXAOptionARequired: Yes A Required
 * <li>testNonXAOptionARequired: No A Required
 * <li>testXAOptionBRequired: Yes B Required
 * <li>testNonXAOptionBRequired: No B Required
 * <li>testXAOptionANotSupported: Yes A NotSupported
 * <li>testNonXAOptionANotSupported: No A NotSupported
 * <li>testXAOptionBNotSupported: Yes B NotSupported
 * <li>testNonXAOptionBNotSupported: No B NotSupported
 * <li>testXAOptionABMT: Yes A Bean Managed
 * <li>testNonXAOptionABMT: No A Bean Managed
 * <li>testXAOptionBBMT: Yes B Bean Managed
 * <li>testNonXAOptionBBMT: No B Bean Managed
 * <li>testXAOptionARequiredInherited: Yes A Required
 * <li>testXAOptionBRequiredInherited: Yes B Required
 * </ol>
 */
public class NonJMS_MDServlet extends FATServlet {
    private FVTBaseMessageProvider baseProvider = null;

    public void prepareTRA() throws Exception {
        baseProvider = (FVTBaseMessageProvider) new InitialContext().lookup("java:comp/env/BaseMessageProvider");
        System.out.println("Looked up BaseMessageProvider");
        baseProvider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Required</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option A message delivery and that commit is driven on the XAResource
     * object provided by the RA.
     */
    public void testXAOptionARequired() throws Exception {
        String deliveryID = "MD_test1a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 11);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 11, new FVTXAResourceImpl());

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "message1", m, 11);
        message.add("CMTNonJMSRequired", "message2", m, 11);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
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
    public void testNonXAOptionARequired() throws Exception {
        String deliveryID = "MD_test1b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired");

        // Add a option A delivery.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "message2", m);
        // message.addRelease("CMTNonJMSRequired");

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);

        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
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
    public void testXAOptionBRequired() throws Exception {
        String deliveryID = "MD_test1c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 13);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 13, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 13);
        message.add("CMTNonJMSRequired", "message1", m, 13);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 13);

        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 13);
        message.add("CMTNonJMSRequired", "message2", m, 13);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 13);
        // message.addRelease("CMTNonJMSRequired", 13);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 2", results.getNumberOfMessagesDelivered() == 2);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit should be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
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
    public void testNonXAOptionBRequired() throws Exception {
        String deliveryID = "MD_test1d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired");

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTNonJMSRequired", "message2", m);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, m);
        // message.addRelease("CMTNonJMSRequired");

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke a MDB method deployed
     * with <i>Not Supported</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that the XAResource object provided
     * by the RA is not enlisted in the local transaction.
     */
    public void testXAOptionANotSupported() throws Exception {
        String deliveryID = "MD_test2a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSNotSupported", 21);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSNotSupported", 21, new FVTXAResourceImpl());

        // Add a option A delivery
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSNotSupported", "message1", m, 21);
        // message.addRelease("CMTNonJMSNotSupported", 21);

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
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a MDB method deployed
     * with <i>Not Supported</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testNonXAOptionANotSupported() throws Exception {
        String deliveryID = "MD_test2b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSNotSupported");

        // Add a option A delivery
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSNotSupported", "message1", m);
        message.add("CMTNonJMSNotSupported", "message2", m);

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
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a MDB method deployed
     * with <i>Not Supported</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and that a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testXAOptionBNotSupported() throws Exception {
        String deliveryID = "MD_test2c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSNotSupported", 23);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSNotSupported", 23, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m, 23);
        message.add("CMTNonJMSNotSupported", "message2", m, 23);
        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null, 23);
        // message.addRelease("CMTNonJMSNotSupported", 23);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", !results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option B is used for this test.", results.optionBMessageDeliveryUsed());
        // assertTrue("This message delivery is in a local transaction context.", results.mdbInvokedInLocalTransactionContext());
        assertFalse("The commit should not be driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertFalse("The RA XAResource should not be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a MDB method
     * deployed with <i>Not Supported</i> transaction attribute.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and a RA XAResource object is not
     * enlisted in the global transaction.
     */
    public void testNonXAOptionBNotSupported() throws Exception {
        String deliveryID = "MD_test2d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSNotSupported");

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTNonJMSNotSupported", "message1", m);
        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null);

        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTNonJMSNotSupported", "message2", m);
        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, null);

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
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object and
     * option A message delivery is used to invoke a method in an MDB deployed as
     * <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that the XAResource object provided
     * by the RA is not enlisted in the global transaction that is
     * started by the MDB method.
     */
    public void testXAOptionABMT() throws Exception {
        String deliveryID = "MD_test3a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMS", 31);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTNonJMS", 31, new FVTXAResourceImpl());

        // Add a option A
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("BMTNonJMS", "message1", m, 31);
        message.add("BMTNonJMS", "message2", m, 31);

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
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object and
     * option A message delivery is used to invoke a method in an MDB deployed as
     * <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a local transaction
     * using option A message delivery and that a RA XAResource object is not
     * enlisted in the global transaction that is started by the MDB method.
     */
    public void testNonXAOptionABMT() throws Exception {
        String deliveryID = "MD_test3b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMS");

        // Add a option A
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("BMTNonJMS", "message1", m);
        // message.addRelease("BMTNonJMS");

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
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke a method in an MDB deployed
     * as <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     *
     * Two serial messages are sent to the same endpoint.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and that a RA XAResource object is not
     * enlisted in the global transaction started by the MDB method.
     */
    public void testXAOptionBBMT() throws Exception {
        String deliveryID = "MD_test3c";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMS", 33);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("BMTNonJMS", 33, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m, 33);
        message.add("BMTNonJMS", "message1", m, 33);
        message.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 33);

        message.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m, 33);
        message.add("BMTNonJMS", "message2", m, 33);
        message.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, null, 33);

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

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created without a XAResource object
     * and option B message delivery is used to invoke a method deployed as
     * <i>Bean Managed</i>. The MDB method gets a UserTransaction, begins a
     * UserTransaction, and commits the UserTransaction prior to returning to its caller.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and that a RA XAResource object is not
     * enlisted in the global transaction started by the MDB method.
     */
    public void testNonXAOptionBBMT() throws Exception {
        String deliveryID = "MD_test3d";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMS");

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m);
        message.add("BMTNonJMS", "message1", m);
        message.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, null);
        // message.addRelease("BMTNonJMS");

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
     * <b>Description: </b>NonJMS MessageEndpoint is created with an XAResource object and
     * option A message delivery used to invoke onMessage in inherited interface deployed
     * with <i>Required</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify onMessage method is invoked in a global transaction
     * using option A message delivery and that commit is driven on the XAResource
     * object provided by the RA.
     */
    public void testXAOptionARequiredInherited() throws Exception {
        String deliveryID = "MD_test4a";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 41);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 41, new FVTXAResourceImpl());

        // Add a option A delivery.
        message.add("CMTNonJMSRequired", "message1", 41);

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("isDeliveryTransacted returns true for a method with the Required attribute.", results.isDeliveryTransacted());
        assertTrue("Number of messages delivered is 1", results.getNumberOfMessagesDelivered() == 1);
        assertTrue("Delivery option A is used for this test.", results.optionAMessageDeliveryUsed());
        // assertTrue("This message delivery is in a global transaction context.", results.mdbInvokedInGlobalTransactionContext());
        assertTrue("The commit is driven on the XAResource provided by TRA.", results.raXaResourceCommitWasDriven());
        assertTrue("The RA XAResource should be enlisted in the global transaction.", results.raXaResourceEnlisted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>Non-JMS MessageEndpoint is created with a XAResource object
     * and option B message delivery is used to invoke inherited interface method
     * method (eg onMessage) deployed with <i>Required</i> transaction attribute.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked in a global transaction
     * using option B message delivery and commit method on the XAResource object
     * provided by the RA is driven.
     */
    public void testXAOptionBRequiredInherited() throws Exception {
        String deliveryID = "MD_test4b";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 43);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 43, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = javax.jms.MessageListener.class.getMethod("onMessage", new Class[] { javax.jms.Message.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 43);
        message.add("CMTNonJMSRequired", "message1", m, 43);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 43);
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
}
