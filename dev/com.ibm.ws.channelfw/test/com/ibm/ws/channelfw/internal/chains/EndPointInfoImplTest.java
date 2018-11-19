/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal.chains;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationListener;

import org.junit.Test;

import com.ibm.websphere.channelfw.EndPointInfo;

/**
 *
 */
public class EndPointInfoImplTest {

    class TestListener implements NotificationListener {
        int callCount = 0;
        Notification notif;

        @Override
        public void handleNotification(Notification notif, Object handback) {
            this.callCount++;
            this.notif = notif;
        }

    }

    /**
     * Test to make sure a IllegalArgumentException is thrown when creating endpoint with a null name
     * <p/>
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#EndPointInfoImpl(java.lang.String, java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_validateConstructor_nullName() throws Exception {
        System.out.println("\b");
        System.out.println("\u0007");
        new EndPointInfoImpl(null, "localhost", 9080);
    }

    /**
     * Test to make sure a IllegalArgumentException is thrown when creating endpoint with an empty name
     * <p/>
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#EndPointInfoImpl(java.lang.String, java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_validateConstructor_emptyName() throws Exception {
        System.out.println("\b");
        System.out.println("\u0007");
        new EndPointInfoImpl("", "localhost", 9080);
    }

    /**
     * Test to make sure a IllegalArgumentException is thrown when creating endpoint with a null host
     * <p/>
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#EndPointInfoImpl(java.lang.String, java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_validateConstructor_nullHost() throws Exception {
        new EndPointInfoImpl("ep", null, 9080);
    }

    /**
     * Test to make sure a IllegalArgumentException is thrown when creating endpoint with an empty host
     * <p/>
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#EndPointInfoImpl(java.lang.String, java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_validateConstructor_emptyHost() throws Exception {
        new EndPointInfoImpl("ep", "", 9080);
    }

    /**
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#EndPointInfoImpl(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void test_validateConstructor() throws Exception {
        EndPointInfo ep = new EndPointInfoImpl("ep", "localhost", 9080);
        assertEquals("Did not get back the expected endpoint name",
                     "ep", ep.getName());
        assertEquals("Did not get back the expected endpoint host",
                     "localhost", ep.getHost());
        assertEquals("Did not get back the expected endpoint port",
                     9080, ep.getPort());
        assertEquals("Did not get back the expected toString",
                     "EndPoint ep=localhost:9080", ep.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#updateHost(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_updateHost_invalid_null() throws Exception {
        EndPointInfoImpl ep = new EndPointInfoImpl("ep", "localhost", 9080);
        ep.updateHost(null);
    }

    /**
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#updateHost(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_updateHost_invalid_empty() throws Exception {
        EndPointInfoImpl ep = new EndPointInfoImpl("ep", "localhost", 9080);
        ep.updateHost("");
    }

    /**
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#updateHost(java.lang.String)}.
     */
    @Test
    public void test_updateHost_valid() throws Exception {
        EndPointInfoImpl ep = new EndPointInfoImpl("ep", "localhost", 9080);

        TestListener listener = new TestListener();
        ep.addNotificationListener(listener, null, null);

        // Check the direct behaviour
        String prev = ep.updateHost("newhost");
        assertEquals("Did not get back the expected previous host",
                     "localhost", prev);
        assertEquals("Did not get back the expected new host",
                     "newhost", ep.getHost());

        // Check the notification listener count and received object
        assertEquals("Notification listener should have only be called once",
                     1, listener.callCount);
        assertSame("Notification was not received from the EndPointInfoImpl object",
                   ep, listener.notif.getSource());
        assertTrue("Notification was not an AttributeChangeNotification",
                   listener.notif instanceof AttributeChangeNotification);

        AttributeChangeNotification acn = (AttributeChangeNotification) listener.notif;
        // Common Notification values
        assertTrue("Notification sequence number was not incremented",
                   acn.getSequenceNumber() > 0);
        assertTrue("Notification time stamp was not non-zero",
                   acn.getTimeStamp() > 0);
        assertEquals("Notification was not of the expected type",
                     "jmx.attribute.change", acn.getType());
        assertEquals("Notification message was not the expected message",
                     "EndPoint ep=newhost:9080 host value changed", acn.getMessage());

        // AttributeChangeNotification specific values
        assertEquals("AttributeChangeNotification attribute name was not correct",
                     "Host", acn.getAttributeName());
        assertEquals("AttributeChangeNotification attribute type was not correct",
                     "java.lang.String", acn.getAttributeType());
        assertEquals("AttributeChangeNotification old value was not correct",
                     "localhost", acn.getOldValue());
        assertEquals("AttributeChangeNotification new value was not correct",
                     "newhost", acn.getNewValue());

    }

    /**
     * Test method for {@link com.ibm.ws.channelfw.internal.chains.EndPointInfoImpl#updatePort(int)}.
     */
    @Test
    public void test_updatePort() throws Exception {
        EndPointInfoImpl ep = new EndPointInfoImpl("ep", "localhost", 9080);

        TestListener listener = new TestListener();
        ep.addNotificationListener(listener, null, null);

        // Check the direct behaviour
        int prev = ep.updatePort(9081);
        assertEquals("Did not get back the expected previous port",
                     9080, prev);
        assertEquals("Did not get back the expected new port",
                     9081, ep.getPort());

        // Check the notification listener count and received object
        assertEquals("Notification listener should have only be called once",
                     1, listener.callCount);
        assertSame("Notification was not received from the EndPointInfoImpl object",
                   ep, listener.notif.getSource());
        assertTrue("Notification was not an AttributeChangeNotification",
                   listener.notif instanceof AttributeChangeNotification);

        AttributeChangeNotification acn = (AttributeChangeNotification) listener.notif;
        // Common Notification values
        assertTrue("Notification sequence number was not incremented",
                   acn.getSequenceNumber() > 0);
        assertTrue("Notification time stamp was not non-zero",
                   acn.getTimeStamp() > 0);
        assertEquals("Notification was not of the expected type",
                     "jmx.attribute.change", acn.getType());
        assertEquals("Notification message was not the expected message",
                     "EndPoint ep=localhost:9081 port value changed", acn.getMessage());

        // AttributeChangeNotification specific values
        assertEquals("AttributeChangeNotification attribute name was not correct",
                     "Port", acn.getAttributeName());
        assertEquals("AttributeChangeNotification attribute type was not correct",
                     "java.lang.Integer", acn.getAttributeType());
        assertEquals("AttributeChangeNotification old value was not correct",
                     9080, acn.getOldValue());
        assertEquals("AttributeChangeNotification new value was not correct",
                     9081, acn.getNewValue());

    }

}
