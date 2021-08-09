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
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * Implementation of an adapter that will transform OSGi bundle lifecycle events
 * into Event Admin <code>Event</code>s as specified in section 113.6.4 of
 * the Event Admin Service version 1.1 spec.
 */
public final class BundleEventAdapter implements BundleListener {

    /**
     * Topic hierarchy under which bundle events should be broadcast.
     */
    private final static String BUNDLE_EVENT_TOPIC_PREFIX = "org/osgi/framework/BundleEvent/";

    /**
     * Reference to the <code>EventAdmin</code> implementation we'll use to
     * post the events.
     */
    private final EventAdmin eventAdmin;

    public BundleEventAdapter(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    /**
     * Receive notification of a bundle lifecycle change event and adapt it to
     * the format required for the <code>EventAdmin</code> service.
     * 
     * @param bundleEvent
     *            the bundle lifecycle event to publish as an <code>Event</code>
     */
    public void bundleChanged(BundleEvent bundleEvent) {
        final String topic = getTopic(bundleEvent);

        // Bail quickly if the event is one that should be ignored
        if (topic == null) {
            return;
        }

        // Event properties
        Map<String, Object> eventProperties = new HashMap<String, Object>();

        // "event" --> the original event object
        eventProperties.put(EventConstants.EVENT, bundleEvent);

        // Non-null result from getBundle
        // "bundle.id" --> source bundle id as Long
        // "bundle.symbolicName" --> source bundle's symbolic name
        // "bundle" --> the source bundle object
        Bundle bundle = bundleEvent.getBundle();
        if (bundle != null) {
            eventProperties.put(EventConstants.BUNDLE_ID, Long.valueOf(bundle.getBundleId()));
            String symbolicName = bundle.getSymbolicName();
            if (symbolicName != null) {
                eventProperties.put(EventConstants.BUNDLE_SYMBOLICNAME, symbolicName);
            }
            eventProperties.put(EventConstants.BUNDLE, bundle);
        }

        // Construct and fire the event
        Event event = new Event(topic, eventProperties);
        eventAdmin.postEvent(event);
    }

    /**
     * Determine the appropriate topic to use for the Bundle Event.
     * 
     * @param bundleEvent
     *            the bundle event that is being adapted
     * @return the topic or null if the event is not supported
     */
    private String getTopic(BundleEvent bundleEvent) {
        StringBuilder topic = new StringBuilder(BUNDLE_EVENT_TOPIC_PREFIX);

        switch (bundleEvent.getType()) {
            case BundleEvent.INSTALLED:
                topic.append("INSTALLED");
                break;
            case BundleEvent.STARTED:
                topic.append("STARTED");
                break;
            case BundleEvent.STOPPED:
                topic.append("STOPPED");
                break;
            case BundleEvent.UPDATED:
                topic.append("UPDATED");
                break;
            case BundleEvent.UNINSTALLED:
                topic.append("UNINSTALLED");
                break;
            case BundleEvent.RESOLVED:
                topic.append("RESOLVED");
                break;
            case BundleEvent.UNRESOLVED:
                topic.append("UNRESOLVED");
                break;
            default:
                return null;
        }

        return topic.toString();
    }
}
