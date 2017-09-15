/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.event;

import java.util.Map;

/**
 * The event engine supplies a variety of APIs for creating and sending events
 * through the system. These events can be sent synchronously or asynchronously
 * and can pass along additional information through the event property map to
 * the eventual handler of each item.
 */
public interface EventEngine {

    /**
     * Configuration property that must be set on the <code>EventHandler</code> services if the handler implementation is reentrant. The OSGi Event
     * Admin Service Specification is written such that an implementation must
     * not assume a handler is reentrant.
     * <p>
     * When this property is set to the <code>String</code> value of <code>true</code>, a handler may be called on multiple threads simultaneously. The implication of this is that
     * events may arrive out of order.
     * 
     * @see https://mail.osgi.org/pipermail/osgi-dev/2006-April/000069.html
     */
    String REENTRANT_HANDLER = "reentrant.handler";

    /**
     * Create an event for the specified topic.
     * 
     * @param topic
     *            the <code>Topic</code> to associate with the event
     * @return the <code>Event</code> to post or send
     */
    Event createEvent(Topic topic);

    /**
     * Create an event for the specified topic.
     * 
     * @param topic
     *            the topic name to associate with the event.
     * @return the Event to post or send
     * 
     * @deprecated use createEvent(Topic) for mainline events
     */
    @Deprecated
    Event createEvent(String topic);

    /**
     * Schedule an <code>Event</code> to be delivered to <code>EventHandler</code> s
     * that are interested in the associated topic. Control will be returned to
     * the
     * caller once the event has been scheduled. The event will be delivered
     * asynchronously.
     * 
     * @param event
     *            the event to deliver
     * @return the <code>EventHandle</code> to the Event that was posted
     *         asynchronously.
     */
    EventHandle postEvent(Event event);

    /**
     * @deprecated use the event object and reserved keys for mainline events
     */
    @Deprecated
    EventHandle postEvent(String topic, Map<?, ?> properties);

    /**
     * Schedule an <code>Event</code> to be delivered to <code>EventHandler</code> s
     * that are interested in the associated topic. Control will be returned to
     * the
     * caller once the event has been scheduled. The event will be delivered
     * asynchronously. The context Map provided will be used to set the properties
     * on the
     * event. A null or empty map will be treated as no properties for the event.
     * 
     * @param topic
     *            of the event to deliver
     * @param context
     *            properties to set on the event
     * @return the <code>EventHandle</code> to the Event that was posted
     *         asynchronously.
     */
    EventHandle postEvent(Topic topic, Map<?, ?> context);

    /**
     * Schedule an <code>Event</code> to be delivered to <code>EventHandler</code>s
     * that are interested in the associated topic. Control will be returned to the
     * caller once the event has been scheduled. The event will be delivered
     * asynchronously. The context Map provided will be used to set the properties on the
     * event. A null or empty map will be treated as no properties for the event.
     * 
     * Additionally, a parent <code>Event</code> is provided to be set on the new <code>Event</code>
     * This should be used when this method is called from a scheduler such that the original
     * <code>Event</code> on the stack may have been cleaned up. By passing in the parent on this
     * method, the inheritable context can be preserved.
     * 
     * @param topic of the event to deliver
     * @param context properties to set on the event
     * @param parent to be set on the new Event
     * @return the <code>EventHandle</code> to the Event that was posted asynchronously.
     */
    EventHandle postEvent(Topic topic, Map<?, ?> context, Event parent);

    /**
     * Deliver an <code>Event</code> to <code>EventHandler</code>s that are
     * interested in the associated topic. The event delivery is synchronous
     * from the sender's perspective and control will not return until all
     * handlers have completed handling the event.
     * 
     * @param event
     *            the event to deliver
     * @return <code>Map<String, Object></code> containing the properties of the
     *         completed Event
     */
    EventHandle sendEvent(Event event);

    /**
     * @deprecated use the event object and reserved keys for mainline events
     */
    @Deprecated
    EventHandle sendEvent(String topic, Map<?, ?> properties);

    /**
     * Deliver an <code>Event</code> to <code>EventHandler</code>s that are
     * interested in the associated topic. The event delivery is synchronous
     * from the sender's perspective and control will not return until all
     * handlers have completed handling the event. A null or empty map will be
     * treated as no properties for the event.
     * 
     * @param topic
     *            of the event to deliver
     * @param context
     *            properties to set on the event
     * @return <code>Map<String, Object></code> containing the properties of the
     *         completed Event
     */
    EventHandle sendEvent(Topic topic, Map<?, ?> context);
}
