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
 * Test Name: MsgEndpoint_SMTest
 *
 * Test Descriptions:
 *
 * The test class is to cover the scenarios which should cause IllegalStateException
 * to be thrown.
 *
 *
 * Test Matrix:
 *
 * testXAWrongBeanMethod created with XA, beforeDelivery, MDB method different from MDB of beforeDelivery invocation
 * testXAMissingAfterDelivery created with XA, beforeDelivery, MDB method, release (missing calling afterDelivery)
 * testXAAfterDeliveryTwice created with XA, beforeDelivery, MDB method, afterDelivery, afterDelivery
 * testBeforeDeliveryAfterMethod created with XA, beforeDelivery, MDB method, beforeDelivery
 * testBeanMethodTwice created with XA, beforeDelivery, MDB method, MDB method
 * testReuseMessageEndpoint created with XA, beforeDelivery, MDB method, afterDelivery, release, beforeDelivery MDB method
 * testBeforeDeliveryTwiceWithAfterDelivery beforeDelivery, beforeDelivery MDB method afterDelivery
 * testMissingBeforeDelivery MDB Method, afterDelivery
 * testBeforeDeliveryTwice beforeDelivery, beforeDelivery MDB method
 * testReleaseBeforeMethod release, beforeDelivery MDB method
 * testMissingBeanMethodAndAfterDelivery
 */
public class MsgEndpoint_SMServlet extends FATServlet {
    private FVTBaseMessageProvider baseProvider = null;

    public void prepareTRA() throws Exception {
        baseProvider = (FVTBaseMessageProvider) new InitialContext().lookup("java:comp/env/BaseMessageProvider");
        System.out.println("Looked up BaseMessageProvider");
        baseProvider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object and
     * beforeDelivery is called for a method with the <i>Required</i> transaction
     * attribute. However, the RA tries to invoke a MDB method that is different
     * from the MDB method specified during beforeDelivery invocation.
     * <p>
     * <b>Results: </b>Verify MDB method is not invoked and the RA catches a
     * java.lang.IllegalStateException as a result of not invoking the correct
     * MDB method. Note, the RA exception handler must call afterDelivery followed
     * by release method to cleanup this error condition prior to next test.
     */
    public void testXAWrongBeanMethod() throws Exception {
        String deliveryID = "test01";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 101);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 101, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m1 = MessageListener.class.getMethod("onIntegerMessage", new Class[] { Integer.class });
        Method m2 = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m1, 101);
        message.add("CMTNonJMSRequired", "message1", m2, 101); // How to make it invoke a method different from m ????
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 101);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>From the same thread of execution,
     * MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called for a method with the <i>Requires</i> transaction
     * attribute, MDB method is invoked, and then release method is invoked without
     * first calling afterDelivery.
     * <p>
     * <b>Results: </b>Verify that RA does not catch a java.lang.IllegalStateException
     * as a result of not calling afterDelivery prior to release call. Verify the
     * transaction started by the beforeDelivery call was aborted by rolling back
     * the transaction as a result of release being called instead of afterDelivery.
     */
    public void testXAMissingAfterDelivery() throws Exception {
        String deliveryID = "test02";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 102);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 102, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 102);
        message.add("CMTNonJMSRequired", "message1", m, 102);
        message.addRelease("CMTNonJMSRequired", 102);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertFalse("IllegalStateException was not caught.", results.raCaughtIllegalStateException()); //d192893
        assertTrue("transaction was rolled back", results.raXaResourceRollbackWasDriven()); //d192893
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called for a method with the <i>Requires</i> transaction
     * attribute, MDB method is invoked, afterDelivery is invoked, and then
     * afterDelivery is invoked a second time.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked using message delivery option B
     * and that the RA catches a java.lang.IllegalStateException as a result of not
     * calling afterDelivery twice for single message delivery. Note, the RA exception handler
     * must call release method to cleanup this error condition prior to next test.
     */
    public void testXAAfterDeliveryTwice() throws Exception {
        String deliveryID = "test03";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 103);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 103, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 103);
        message.add("CMTNonJMSRequired", "message1", m, 103);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 103);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 103);
        message.addRelease("CMTNonJMSRequired", 103);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called for a method with the <i>Requires</i> transaction
     * attribute, MDB method is invoked, and beforeDelivery is invoked again.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked using message delivery option B
     * and that the RA catches a java.lang.IllegalStateException as a result of not
     * calling beforeDelivery a second time without a matching afterDelivery for the
     * first beforeDelivery call. Note, the RA exception handler must call afterDelivery
     * followed by release method to cleanup this error condition prior to next test.
     */
    public void testBeforeDeliveryAfterMethod() throws Exception {
        String deliveryID = "test04";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 104);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 104, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 104);
        message.add("CMTNonJMSRequired", "message1", m, 104);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 104);
        message.addRelease("CMTNonJMSRequired", 104);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called for a method with the <i>Requires</i> transaction
     * attribute, MDB method is invoked, and MDB method is invoked again.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked using message delivery option B
     * and that the RA catches a java.lang.IllegalStateException as a result of not
     * calling MDB method more than once per beforeDelivery/afterDelivery pair.
     * Note, the RA exception handler must call afterDelivery
     * followed by release method to cleanup this error condition prior to next test.
     */
    public void testBeanMethodTwice() throws Exception {
        String deliveryID = "test05";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 105);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 105, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 105);
        message.add("CMTNonJMSRequired", "message1", m, 105);
        message.add("CMTNonJMSRequired", "message1", m, 105);
        message.addRelease("CMTNonJMSRequired", 105);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called for a method with the <i>Requires</i> transaction
     * attribute, MDB method is invoked, afterDelivery is invoked, release is invoked,
     * and then beforeDelivery is invoked a second time using the same MessageEndpoint
     * proxy object.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked using message delivery option B
     * and that the RA catches a java.lang.IllegalStateException as a result of not
     * calling beforeDelivery a second time after release method was invoked. Note,
     * the RA exception handler does not need to do anything to cleanup this error.
     */
    public void testReuseMessageEndpoint() throws Exception {
        String deliveryID = "test06";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 106);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 106, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 106);
        message.add("CMTNonJMSRequired", "message1", m, 106);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 106);
        message.addRelease("CMTNonJMSRequired", 106);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 106);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called twice for a method with the <i>Required</i> transaction
     * attribute, MDB method is invoked, afterDelivery is invoked.
     * <p>
     * <b>Results: </b>Verify MDB method is invoked using message delivery option B
     * and that the RA catches a java.lang.IllegalStateException as a result of not
     * calling beforeDelivery a second time. Note, the RA exception handler does
     * not need to do anything to cleanup this error.
     */
    public void testBeforeDeliveryTwiceWithAfterDelivery() throws Exception {
        String deliveryID = "test07";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 107);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 107, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 107);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 107);
        message.add("CMTNonJMSRequired", "message1", m, 107);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 107);
        message.addRelease("CMTNonJMSRequired", 107);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is not called for a method with the <i>Required</i> transaction
     * attribute, MDB method is invoked, afterDelivery is invoked.
     * <p>
     * <b>Results: </b>Verify that the RA catches a java.lang.IllegalStateException
     * as a result of not calling beforeDelivery before invoking the MDB method.
     * Note, the RA exception handler does not need to do anything to cleanup this error.
     */
    public void testMissingBeforeDelivery() throws Exception {
        String deliveryID = "test08";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 108);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 108, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.add("CMTNonJMSRequired", "message1", m, 108);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.AFTER_DELIVERY, null, 108);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called twice for a method with the <i>Required</i> transaction
     * attribute, MDB method is invoked, afterDelivery is not invoked.
     * <p>
     * <b>Results: </b>Verify that the RA catches a java.lang.IllegalStateException
     * as a result of calling beforeDelivery twice before invoking the MDB method.
     * Note, the RA exception handler does not need to do anything to cleanup this error.
     */
    public void testBeforeDeliveryTwice() throws Exception {
        String deliveryID = "test09";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 109);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 109, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 109);
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 109);
        message.add("CMTNonJMSRequired", "message1", m, 109);
        message.addRelease("CMTNonJMSRequired", 109);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>MessageEndpoint is created with a XAResource object,
     * beforeDelivery is not called for a method with the <i>Required</i> transaction
     * attribute, MDB method is invoked, beforeDelivery is invoked.
     * <p>
     * <b>Results: </b>Verify that the RA catches a java.lang.IllegalStateException
     * as a result of not calling beforeDelivery before invoking the MDB method or calling
     * beforeDelivery after the MDB method.
     * Note, the RA exception handler does not need to do anything to cleanup this error.
     */
    public void testReleaseBeforeMethod() throws Exception {
        String deliveryID = "test10";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 110);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 110, new FVTXAResourceImpl());

        message.addRelease("CMTNonJMSRequired", 110);

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 110);
        message.addRelease("CMTNonJMSRequired", 110);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertTrue("IllegalStateException Caught.", results.raCaughtIllegalStateException());
    }

    /**
     * <b>Description: </b>From the same thread of execution,
     * MessageEndpoint is created with a XAResource object,
     * beforeDelivery is called for a method with the <i>Requires</i> transaction
     * attribute, and then release method is invoked without calling neither the MDB
     * method nor afterDelivery.
     * <p>
     * <b>Results: </b>Verify that RA does not catch a java.lang.IllegalStateException
     * as a result of not calling afterDelivery prior to release call. Verify the
     * transaction started by the beforeDelivery call was aborted by rolling back
     * the transaction as a result of release being called instead of afterDelivery.
     */
    public void testMissingBeanMethodAndAfterDelivery() throws Exception {
        String deliveryID = "test11";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSRequired", 111);

        // Add a FVTXAResourceImpl for this delivery.
        message.addXAResource("CMTNonJMSRequired", 111, new FVTXAResourceImpl());

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSRequired", FVTMessage.BEFORE_DELIVERY, m, 111);
        message.addRelease("CMTNonJMSRequired", 111);

        System.out.println(message.toString());
        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertFalse("IllegalStateException was not caught.", results.raCaughtIllegalStateException()); //d192893
        assertTrue("transaction was rolled back", results.raXaResourceRollbackWasDriven()); //d192893
    }
}