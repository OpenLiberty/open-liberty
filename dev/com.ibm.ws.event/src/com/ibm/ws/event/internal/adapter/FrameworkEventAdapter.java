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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * Implementation of an adapter that will transform OSGi framework events into
 * Event Admin <code>Event</code>s as specified in section 113.6.3 of the
 * Event Admin Service version 1.1 spec.
 */
public final class FrameworkEventAdapter implements FrameworkListener {

    /**
     * Topic hierarchy under which framework events should be broadcast.
     */
    private final static String FRAMEWORK_EVENT_TOPIC_PREFIX = "org/osgi/framework/FrameworkEvent/";

    /**
     * Reference to the <code>EventAdmin</code> implementation we'll use to
     * post the events.
     */
    private final EventAdmin eventAdmin;

    public FrameworkEventAdapter(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    /**
     * Receive notification of a framework event and adapt it to the format
     * required for the <code>EventAdmin</code> service.
     * 
     * @param frameworkEvent
     *            the framework event to publish as an <code>Event</code>
     */
    public void frameworkEvent(FrameworkEvent frameworkEvent) {
        final String topic = getTopic(frameworkEvent);

        // Bail quickly if the event is one that should be ignored
        if (topic == null) {
            return;
        }

        // Event properties
        Map<String, Object> eventProperties = new HashMap<String, Object>();

        // "event" --> the original event object
        eventProperties.put(EventConstants.EVENT, frameworkEvent);

        // Non-null result from getBundle
        // "bundle.id" --> source bundle id as Long
        // "bundle.symbolicName" --> source bundle's symbolic name
        // "bundle" --> the source bundle object
        Bundle bundle = frameworkEvent.getBundle();
        if (bundle != null) {
            eventProperties.put(EventConstants.BUNDLE_ID, Long.valueOf(bundle.getBundleId()));
            String symbolicName = bundle.getSymbolicName();
            if (symbolicName != null) {
                eventProperties.put(EventConstants.BUNDLE_SYMBOLICNAME, symbolicName);
            }
            eventProperties.put(EventConstants.BUNDLE, bundle);
        }

        // Non-null result from getThrowable
        // "exception.class" --> fully-qualified class name of attached exception
        // "exception.message" --> the message from the attached exception
        // "exception" --> the Throwable associated with the event
        Throwable throwable = frameworkEvent.getThrowable();
        if (throwable != null) {
            eventProperties.put(EventConstants.EXCEPTION_CLASS, throwable.getClass().getName());
            String message = throwable.getMessage();
            if (message != null) {
                eventProperties.put(EventConstants.EXCEPTION_MESSAGE, message);
            }
            eventProperties.put(EventConstants.EXCEPTION, throwable);
        }

        // Construct and fire the event
        Event event = new Event(topic, eventProperties);
        eventAdmin.postEvent(event);
    }

    /**
     * Determine the appropriate topic to use for the Framework Event.
     * 
     * @param frameworkEvent
     *            the framework event that is being adapted
     * @return the topic or null if the event is not supported
     */
    private String getTopic(FrameworkEvent frameworkEvent) {
        StringBuilder topic = new StringBuilder(FRAMEWORK_EVENT_TOPIC_PREFIX);

        switch (frameworkEvent.getType()) {
            case FrameworkEvent.STARTED:
                topic.append("STARTED");
                break;
            case FrameworkEvent.ERROR:
                topic.append("ERROR");
                break;
            case FrameworkEvent.PACKAGES_REFRESHED:
                topic.append("PACKAGES_REFRESHED");
                break;
            case FrameworkEvent.STARTLEVEL_CHANGED:
                topic.append("STARTLEVEL_CHANGED");
                break;
            case FrameworkEvent.WARNING:
                topic.append("WARNING");
                break;
            case FrameworkEvent.INFO:
                topic.append("INFO");
                break;
            default:
                return null;
        }

        return topic.toString();
    }

}
