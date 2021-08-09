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
package com.ibm.ws.event.internal.adapter;

import static com.ibm.ws.event.internal.adapter.EventMatcher.anEventMatching;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

public class ServiceEventAdapterTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void testServiceChangedRegistered() {
        testServiceEvent(ServiceEvent.REGISTERED, "REGISTERED");
    }

    @Test
    public void testServiceChangedModified() {
        testServiceEvent(ServiceEvent.MODIFIED, "MODIFIED");
    }

    @Test
    public void testServiceChangedUnregistering() {
        testServiceEvent(ServiceEvent.UNREGISTERING, "UNREGISTERING");
    }

    @Test
    public void testServiceChangedUnknown() {
        final ServiceReference serviceReference = context.mock(ServiceReference.class);
        context.checking(new Expectations() {
            {
            }
        });

        final EventAdmin eventAdmin = context.mock(EventAdmin.class);
        context.checking(new Expectations() {
            {
            }
        });

        ServiceEventAdapter adapter = new ServiceEventAdapter(eventAdmin);
        adapter.serviceChanged(new ServiceEvent(Integer.MIN_VALUE, serviceReference));
    }

    private void testServiceEvent(int serviceEventId, String serviceEventName) {
        final ServiceReference serviceReference = context.mock(ServiceReference.class);
        context.checking(new Expectations() {
            {
                oneOf(serviceReference).getProperty(Constants.SERVICE_ID);
                will(returnValue(Long.valueOf(Long.MAX_VALUE)));
                oneOf(serviceReference).getProperty(Constants.SERVICE_PID);
                will(returnValue("test.pid.value"));
                oneOf(serviceReference).getProperty(Constants.OBJECTCLASS);
                will(returnValue(new String[] { "test.objectClass.value" }));
            }
        });

        final String topic = "org/osgi/framework/ServiceEvent/" + serviceEventName;
        final Map<String, Object> eventProperties = new HashMap<String, Object>();
        eventProperties.put(EventConstants.SERVICE, serviceReference);
        eventProperties.put(EventConstants.SERVICE_ID, Long.valueOf(Long.MAX_VALUE));
        eventProperties.put(EventConstants.SERVICE_PID, "test.pid.value");
        eventProperties.put(EventConstants.SERVICE_OBJECTCLASS, new String[] { "test.objectClass.value" });
        final Event event = new Event(topic, eventProperties);

        final EventAdmin eventAdmin = context.mock(EventAdmin.class);
        context.checking(new Expectations() {
            {
                oneOf(eventAdmin).postEvent(with(anEventMatching(event)));
            }
        });

        ServiceEventAdapter adapter = new ServiceEventAdapter(eventAdmin);
        adapter.serviceChanged(new ServiceEvent(serviceEventId, serviceReference));
    }
}
