/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerDelegate;

import org.junit.Test;

import com.ibm.ws.jmx.connector.client.rest.ClientProvider;

/**
 *
 */
public class NotificationTargetInformationTest {

    private static final String HOST_NAME = "skywalker.torolab.ibm.com";
    private static final String SERVER_NAME = "myServer";
    private static final String SERVER_USER_DIR = "/dev/wlp/usr";

    @Test
    public void checkObjectName() {
        NotificationTargetInformation nlk = getNoRoutingKey();
        assertEquals("Expected ObjectNames to be equal.", MBeanServerDelegate.DELEGATE_NAME, nlk.getName());
        assertEquals("Expected canonical form of object names to be equal.", MBeanServerDelegate.DELEGATE_NAME.getCanonicalName(), nlk.getNameAsString());
    }

    @Test
    public void checkRoutingInfo() {
        Map<String, Object> routingInfo = getRoutingInfo();
        NotificationTargetInformation nlk1 = getRoutingKey();
        NotificationTargetInformation nlk2 = getRoutingKey2();
        assertEquals("Expected routing info maps to be equal.", routingInfo, nlk1.getRoutingInformation());
        assertEquals("Expected routing info maps to be equal.", routingInfo, nlk2.getRoutingInformation());
    }

    @Test
    public void checkEquals() {
        NotificationTargetInformation noRoutingKey1 = getNoRoutingKey();
        NotificationTargetInformation noRoutingKey2 = getNoRoutingKey();

        NotificationTargetInformation routingKey1 = getRoutingKey();
        NotificationTargetInformation routingKey2 = getRoutingKey();

        assertTrue(noRoutingKey1.equals(noRoutingKey1));
        assertFalse(routingKey1.equals(new Integer(3)));

        assertTrue(noRoutingKey1.equals(noRoutingKey2));
        assertTrue(routingKey2.equals(routingKey1));

        assertFalse(noRoutingKey1.equals(routingKey1));
        assertFalse(routingKey2.equals(noRoutingKey1));
    }

    @Test
    public void checkHashCode() {
        NotificationTargetInformation noRoutingKey = getNoRoutingKey();
        NotificationTargetInformation routingKey = getRoutingKey();

        assertEquals("Expected hashCode() to be consistent.", noRoutingKey.hashCode(), noRoutingKey.hashCode());
        assertEquals("Expected hashCode() to be consistent.", routingKey.hashCode(), routingKey.hashCode());

        Map<NotificationTargetInformation, String> map = new HashMap<NotificationTargetInformation, String>();
        map.put(noRoutingKey, "abc");
        map.put(routingKey, "xyz");

        assertEquals("Expected to find 'abc' in the map.", "abc", map.get(noRoutingKey));
        assertEquals("Expected to find 'xyz' in the map.", "xyz", map.get(routingKey));
    }

    private NotificationTargetInformation getNoRoutingKey() {
        return new NotificationTargetInformation(MBeanServerDelegate.DELEGATE_NAME);
    }

    private NotificationTargetInformation getRoutingKey() {
        return new NotificationTargetInformation(MBeanServerDelegate.DELEGATE_NAME,
                                           HOST_NAME, SERVER_NAME, SERVER_USER_DIR);
    }

    private NotificationTargetInformation getRoutingKey2() {
        return new NotificationTargetInformation(MBeanServerDelegate.DELEGATE_NAME, getRoutingInfo());
    }

    private Map<String, Object> getRoutingInfo() {
        Map<String, Object> routingInfo = new HashMap<String, Object>();
        routingInfo.put(ClientProvider.ROUTING_KEY_HOST_NAME, HOST_NAME);
        routingInfo.put(ClientProvider.ROUTING_KEY_SERVER_NAME, SERVER_NAME);
        routingInfo.put(ClientProvider.ROUTING_KEY_SERVER_USER_DIR, SERVER_USER_DIR);
        return routingInfo;
    }
}
