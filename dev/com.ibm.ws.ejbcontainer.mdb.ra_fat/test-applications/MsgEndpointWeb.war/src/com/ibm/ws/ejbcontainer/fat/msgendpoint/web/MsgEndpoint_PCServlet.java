/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.msgendpoint.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import com.ibm.websphere.kernel.server.ServerEndpointControlMBean;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointTestResults;
import com.ibm.ws.ejbcontainer.fat.rar.work.WorkRuntimeException;

import componenttest.app.FATServlet;

/**
 * Test Name: MsgEndpoint_PCTest
 *
 * Test Descriptions:
 *
 * This test class is to cover the pauseable component scenarios for MDBs
 **/

public class MsgEndpoint_PCServlet extends FATServlet {
    private FVTMessageProvider provider = null;

    private String mbeanObjectName = "WebSphere:feature=kernel,name=ServerEndpointControl";

    public void prepareTRA() throws Exception {
        provider = (FVTMessageProvider) new InitialContext().lookup("java:comp/env/MessageProvider");
        System.out.println("Looked up MessageProvider");
        provider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
    }

    public void testEndpointStartedWithAutoStartTrue() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);

        assertFalse((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMS" }, new String[] { String.class.getName() }));

        prepareTRA();

        String deliveryID1 = "TestMessage";

        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMS");
        message.add("BMTNonJMS", "message1");
        provider.sendDirectMessage(deliveryID1, message);

        MessageEndpointTestResults results1 = provider.getTestResult(deliveryID1);
        assertEquals(results1.getNumberOfMessagesDelivered(), 1);

        provider.releaseDeliveryId(deliveryID1);
    }

    public void testEndpointNotStartWithAutoStartFalse() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);

        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSNeverStarted" },
                                        new String[] { String.class.getName() }));

        prepareTRA();

        String deliveryID1 = "TestMessage";

        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMSNeverStarted");
        message.add("BMTNonJMSNeverStarted", "message1");
        try {
            provider.sendDirectMessage(deliveryID1, message);
            fail("Expected WorkRuntimeException to be thrown");
        } catch (WorkRuntimeException wre) {
            // expected
        }
        MessageEndpointTestResults results1 = provider.getTestResult(deliveryID1);
        assertNull(results1);

        provider.releaseDeliveryId(deliveryID1);
    }

    public void testEndpointNotStartWithAutoStartFalseJMS() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);

        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTJMSAutoStartFalse" },
                                        new String[] { String.class.getName() }));

        prepareTRA();

        String deliveryID1 = "TestMessage";

        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTJMSAutoStartFalse");
        message.add("BMTJMSAutoStartFalse", "message1");
        try {
            provider.sendDirectMessage(deliveryID1, message);
            fail("Expected WorkRuntimeException to be thrown");
        } catch (WorkRuntimeException wre) {
            // expected
        }
        MessageEndpointTestResults results1 = provider.getTestResult(deliveryID1);
        assertNull(results1);

        provider.releaseDeliveryId(deliveryID1);
    }

    /**
     * testEndpointGetListOfNames
     *
     * Test that we can get the list of pauseable endpoints from the mbean and that it
     * contains one of our message endpoints.
     *
     * @throws Exception
     */
    public void testEndpointGetListOfNames() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);
        prepareTRA();

        String name = provider.getActivationName("MDBBean");

        @SuppressWarnings("unchecked")
        List<String> endpoints = (List<String>) mbs.invoke(obn, "listEndpoints", new Object[] {}, new String[] {});

        // Check for at least a reasonable number of endpoints.
        // Not checking a set number in case other components are made pauseable.
        assertTrue(endpoints.size() > 5);
        assertTrue(endpoints.contains(name));
    }

    /**
     * testEndpointPauseAndResume
     *
     * Test to ensure message endpoints can be paused, at which point they will not accept messages,
     * and they can be resumed, at which point they will accept messages again.
     *
     * @throws Exception
     */
    public void testEndpointPauseAndResume() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);
        prepareTRA();

        String deliveryID1 = "TestMessage";

        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBBean");
        message.add("MDBBean", "message1");

        String name = provider.getActivationName("MDBBean");

        assertFalse("Endpoint should not have been paused", (Boolean) mbs.invoke(obn, "isPaused", new Object[] { name }, new String[] { String.class.getName() }));

        provider.sendDirectMessage(deliveryID1, message);

        MessageEndpointTestResults results1 = provider.getTestResult(deliveryID1);
        assertEquals(1, results1.getNumberOfMessagesDelivered());

        mbs.invoke(obn, "pause", new Object[] { name }, new String[] { String.class.getName() });

        assertTrue("Endpoint is not paused", (Boolean) mbs.invoke(obn, "isPaused", new Object[] { name }, new String[] { String.class.getName() }));

        try {
            provider.sendDirectMessage(deliveryID1, message);
        } catch (WorkRuntimeException wre) {
            // expected
        }
        results1 = provider.getTestResult(deliveryID1);
        assertNull("Message send should have failed", results1);

        mbs.invoke(obn, "resume", new Object[] { name }, new String[] { String.class.getName() });

        assertFalse("Endpoint should have been resumed", (Boolean) mbs.invoke(obn, "isPaused", new Object[] { name }, new String[] { String.class.getName() }));

        provider.sendDirectMessage(deliveryID1, message);

        results1 = provider.getTestResult(deliveryID1);
        assertEquals(1, results1.getNumberOfMessagesDelivered());
    }

    /**
     * testPauseInactive
     *
     * Test that the correct message is thrown when attempting to pause an inactive endpoint.
     * Also checks that the endpoint remains inactive.
     *
     * @throws Exception
     */
    public void testPauseInactive() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Use API to ensure that it is accessible to application
        ObjectName myMBean = new ObjectName("WebSphere:feature=kernel,name=ServerEndpointControl");
        ServerEndpointControlMBean serverEndpointControl = JMX.newMBeanProxy(mbs, myMBean, ServerEndpointControlMBean.class);

        ObjectName obn = new ObjectName(mbeanObjectName);
        prepareTRA();

        String deliveryID1 = "TestMessage";

        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBBean");
        message.add("MDBBean", "message1");

        String name = provider.getActivationName("MDBBean");

        assertFalse("Endpoint should not have been paused", serverEndpointControl.isPaused(name));

        serverEndpointControl.pause(name);

        assertTrue("Endpoint is not paused", serverEndpointControl.isPaused(name));

        serverEndpointControl.pause(name);

        assertTrue("Endpoint should have remained paused", serverEndpointControl.isPaused(name));

        try {
            provider.sendDirectMessage(deliveryID1, message);
        } catch (WorkRuntimeException wre) {
            // expected
        }
        MessageEndpointTestResults results1 = provider.getTestResult(deliveryID1);
        assertNull("Message send should have failed", results1);

        // Leave the endpoint active
        serverEndpointControl.resume(name);
        assertFalse("Endpoint should have been resumed", serverEndpointControl.isPaused(name));
    }

    /**
     * testResumeActive
     *
     * Test that the correct message is thrown when attempting to resume an active endpoint.
     * Also checks that the endpoint remains active.
     *
     * @throws Exception
     */
    public void testResumeActive() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);
        prepareTRA();

        String deliveryID1 = "TestMessage";

        FVTMessage message = new FVTMessage();
        message.addTestResult("MDBBean");
        message.add("MDBBean", "message1");

        String name = provider.getActivationName("MDBBean");

        assertFalse("Endpoint should not have been paused", (Boolean) mbs.invoke(obn, "isPaused", new Object[] { name }, new String[] { String.class.getName() }));

        mbs.invoke(obn, "resume", new Object[] { name }, new String[] { String.class.getName() });

        assertFalse("Endpoint should still have been active", (Boolean) mbs.invoke(obn, "isPaused", new Object[] { name }, new String[] { String.class.getName() }));

        provider.sendDirectMessage(deliveryID1, message);
        MessageEndpointTestResults results1 = provider.getTestResult(deliveryID1);
        assertEquals(1, results1.getNumberOfMessagesDelivered());
    }

    public void testResumeAndPauseEndpointWithAutoStartFalse() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);

        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" },
                                        new String[] { String.class.getName() }));

        prepareTRA();

        String deliveryID1 = "TestMessage";
        String deliveryID2 = "TestMessage2";
        String deliveryID3 = "TestMessage3";
        String deliveryID4 = "TestMessage4";

        FVTMessage message = new FVTMessage();
        message.addTestResult("BMTNonJMSFalseResume");
        message.add("BMTNonJMSFalseResume", "message1");
        try {
            provider.sendDirectMessage(deliveryID1, message);
            fail("Expected WorkRuntimeException to be thrown");
        } catch (WorkRuntimeException wre) {
            // expected
        }
        mbs.invoke(obn, "resume", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" }, new String[] { String.class.getName() });

        assertFalse((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" },
                                         new String[] { String.class.getName() }));

        FVTMessage message2 = new FVTMessage();
        message2.addTestResult("BMTNonJMSFalseResume");
        message2.add("BMTNonJMSFalseResume", "message2");
        provider.sendDirectMessage(deliveryID2, message2);

        mbs.invoke(obn, "pause", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" }, new String[] { String.class.getName() });

        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" },
                                        new String[] { String.class.getName() }));

        FVTMessage message3 = new FVTMessage();
        message3.addTestResult("BMTNonJMSFalseResume");
        message3.add("BMTNonJMSFalseResume", "message3");

        try {
            provider.sendDirectMessage(deliveryID3, message3);
            fail("Expected WorkRuntimeException to be thrown");
        } catch (WorkRuntimeException wre) {
            // expected
        }
        mbs.invoke(obn, "resume", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" }, new String[] { String.class.getName() });

        assertFalse((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" },
                                         new String[] { String.class.getName() }));

        FVTMessage message4 = new FVTMessage();
        message4.addTestResult("BMTNonJMSFalseResume");
        message4.add("BMTNonJMSFalseResume", "message4");
        provider.sendDirectMessage(deliveryID4, message4);

        mbs.invoke(obn, "pause", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" }, new String[] { String.class.getName() });

        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSFalseResume" },
                                        new String[] { String.class.getName() }));

        MessageEndpointTestResults results1 = provider.getTestResult(deliveryID1);
        assertNull(results1);
        MessageEndpointTestResults results2 = provider.getTestResult(deliveryID2);
        assertEquals(results2.getNumberOfMessagesDelivered(), 1);
        MessageEndpointTestResults results3 = provider.getTestResult(deliveryID3);
        assertNull(results3);
        MessageEndpointTestResults results4 = provider.getTestResult(deliveryID4);
        assertEquals(results4.getNumberOfMessagesDelivered(), 1);

        provider.releaseDeliveryId(deliveryID1);
        provider.releaseDeliveryId(deliveryID2);
        provider.releaseDeliveryId(deliveryID3);
        provider.releaseDeliveryId(deliveryID4);
    }

    public void testMultipleEndpointSameActSpec() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);

        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalse" },
                                        new String[] { String.class.getName() }));
        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalseDup" },
                                        new String[] { String.class.getName() }));

        // Resume BMTNonJMSAutoStartFalse, but not BMTNonJMSAutoStartFalseDup
        mbs.invoke(obn, "resume", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalse" }, new String[] { String.class.getName() });

        assertFalse((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalse" },
                                         new String[] { String.class.getName() }));
        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalseDup" },
                                        new String[] { String.class.getName() }));

        // Now resume BMTNonJMSAutoStartFalseDup
        mbs.invoke(obn, "resume", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalseDup" }, new String[] { String.class.getName() });

        assertFalse((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalse" },
                                         new String[] { String.class.getName() }));
        assertFalse((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSAutoStartFalseDup" },
                                         new String[] { String.class.getName() }));
    }

    public void testMDBWithNoActSpecBinding() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        ObjectName obn = new ObjectName(mbeanObjectName);

        assertTrue((Boolean) mbs.invoke(obn, "isPaused", new Object[] { "MsgEndpointApp#MsgEndpointEJB.jar#EndpointBMTNonJMSNoActSpec" },
                                        new String[] { String.class.getName() }));

    }
}
