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

import com.ibm.ws.ejbcontainer.fat.rar.message.FVTBaseMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;

import componenttest.app.FATServlet;

/**
 * Test Name: MsgEndpoint_IFTest
 *
 * Test Descriptions:
 *
 * This class implements a ejb container FVT testcase for testing EJB container
 * component implementation of the JCA 1.5 MessageEndpointFactory and MessageEndpoint
 * interfaces. <p>
 *
 * Test Matrix:
 * <ol>
 * <li>Testing isDeliveryTransaction method
 * <li>testRequired: isDeliveryTransacted for "Required"
 * <li>testNotSupported: isDeliveryTransacted for "NotSupported"
 * <li>testBMT: isDeliveryTransacted for "Bean Managed"
 * <li>
 * </ol>
 *
 */
public class MsgEndpoint_IFServlet extends FATServlet {
    private FVTBaseMessageProvider baseProvider = null;

    public void prepareTRA() throws Exception {
        baseProvider = (FVTBaseMessageProvider) new InitialContext().lookup("java:comp/env/BaseMessageProvider");
        System.out.println("Looked up BaseMessageProvider");
        baseProvider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    /**
     * <b>Description: </b>isDeliveryTransacted called for a <i>Required</i> method.
     * Deploy an MDB so that a method in the messaging type
     * interface is deployed to use the <i>Required</i> transaction attribute.
     * RA calls MessageEndpointFactory.createEndpoint and then calls
     * isDeliveryTransacted on the MessageEndpoint returned by createEndpoint method.
     * <p>
     * <b>Results: </b>Verify boolean true is returned by the isDeliveryTransacted method.
     */
    public void testRequired() throws Exception {
        String deliveryID = "IF_test01";

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

        assertTrue("IF_test01: isDeliveryTransacted should return true for a method with the Required attribute.", results.isDeliveryTransacted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>isDeliveryTransacted called for a <i>Not Supported</i> method.
     * Deploy an MDB so that a method in the messaging type
     * interface is deployed to use the <i>Not Supported</i> transaction attribute.
     * RA calls MessageEndpointFactory.createEndpoint and then calls
     * isDeliveryTransacted on the MessageEndpoint returned by createEndpoint method.
     * <p>
     * <b>Results: </b>Verify boolean false is returned by the isDeliveryTransacted method.
     */
    public void testNotSupported() throws Exception {
        String deliveryID = "IF_test02";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("CMTNonJMSNotSupported");

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.BEFORE_DELIVERY, m);
        message.add("CMTNonJMSNotSupported", "message2", m);
        message.addDelivery("CMTNonJMSNotSupported", FVTMessage.AFTER_DELIVERY, m);
        // message.addRelease("CMTNonJMSRequired");

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertFalse("IF_test02: isDeliveryTransacted should return false for a method with attribute Not Supported.", results.isDeliveryTransacted());

        baseProvider.releaseDeliveryId(deliveryID);
    }

    /**
     * <b>Description: </b>isDeliveryTransacted called for a <i>Bean Managed</i> method.
     * Deploy an MDB so that a method in the messaging type
     * interface is deployed to use the <i>Bean Managed</i> transaction attribute.
     * RA calls MessageEndpointFactory.createEndpoint and then calls
     * isDeliveryTransacted on the MessageEndpoint returned by createEndpoint method.
     * <p>
     * <b>Results: </b>Verify boolean false is returned by the isDeliveryTransacted method.
     */
    public void testBMT() throws Exception {
        String deliveryID = "IF_test03";

        prepareTRA();

        // construct a FVTMessage
        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMS");

        // Add a option B transacted delivery to another instance.
        Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
        message.addDelivery("BMTNonJMS", FVTMessage.BEFORE_DELIVERY, m);
        message.add("BMTNonJMS", "message2", m);
        message.addDelivery("BMTNonJMS", FVTMessage.AFTER_DELIVERY, m);
        // message.addRelease("CMTNonJMSRequired");

        System.out.println(message.toString());

        baseProvider.sendDirectMessage(deliveryID, message);

        MessageEndpointTestResults results = baseProvider.getTestResult(deliveryID);
        assertFalse("IF_test03: isDeliveryTransacted should return false for a method with the BMT attribute.", results.isDeliveryTransacted());

        baseProvider.releaseDeliveryId(deliveryID);
    }
}