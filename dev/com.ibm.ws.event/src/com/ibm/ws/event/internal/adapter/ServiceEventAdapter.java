/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.event.internal.adapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * Implementation of an adapter that will transform OSGi service events
 * into Event Admin <code>Event</code>s as specified in section 113.6.6
 * of the Event Admin Service version 1.1 spec.
 */
public final class ServiceEventAdapter implements AllServiceListener {

    /**
     * Topic hierarchy under which service events should be broadcast.
     */
    private final static String SERVICE_EVENT_TOPIC_PREFIX = "org/osgi/framework/ServiceEvent/";

    /**
     * Reference to the <code>EventAdmin</code> implementation we'll use to
     * post the events.
     */
    private final EventAdmin eventAdmin;

    public ServiceEventAdapter(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    /**
     * Receive notification of a service lifecycle change event and adapt it to
     * the format required for the <code>EventAdmin</code> service.
     * 
     * @param serviceEvent
     *            the service lifecycle event to publish as an <code>Event</code>
     */
    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        final String topic = getTopic(serviceEvent);

        // Bail quickly if the event is one that should be ignored
        if (topic == null) {
            return;
        }

        // Event properties
        Map<String, Object> eventProperties = new HashMap<String, Object>();

        // "event" --> the original event object
        eventProperties.put(EventConstants.EVENT, serviceEvent);

        // "service" --> result of getServiceReference
        // "service.id" --> the service's ID
        // "service.pid" --> the service's persistent ID if not null
        // "service.objectClass" --> the services object class
        ServiceReference serviceReference = serviceEvent.getServiceReference();
        if (serviceReference != null) {
            eventProperties.put(EventConstants.SERVICE, serviceReference);

            Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
            eventProperties.put(EventConstants.SERVICE_ID, serviceId);

            Object servicePersistentId = serviceReference.getProperty(Constants.SERVICE_PID);
            if (servicePersistentId != null) {
                // String[] must be coerced into Collection<String>
                if (servicePersistentId instanceof String[]) {
                    servicePersistentId = Arrays.asList((String[]) servicePersistentId);
                }
                eventProperties.put(EventConstants.SERVICE_PID, servicePersistentId);
            }

            String[] objectClass = (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
            if (objectClass != null) {
                eventProperties.put(EventConstants.SERVICE_OBJECTCLASS, objectClass);
            }
        }

        // Construct and fire the event
        Event event = new Event(topic, eventProperties);
        eventAdmin.postEvent(event);
    }

    /**
     * Determine the appropriate topic to use for the Service Event.
     * 
     * @param serviceEvent
     *            the service event that is being adapted
     * @return the topic or null if the event is not supported
     */
    private String getTopic(ServiceEvent serviceEvent) {
        StringBuilder topic = new StringBuilder(SERVICE_EVENT_TOPIC_PREFIX);

        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED:
                topic.append("REGISTERED");
                break;
            case ServiceEvent.MODIFIED:
                topic.append("MODIFIED");
                break;
            case ServiceEvent.UNREGISTERING:
                topic.append("UNREGISTERING");
                break;
            default:
                return null;
        }

        return topic.toString();
    }
}
