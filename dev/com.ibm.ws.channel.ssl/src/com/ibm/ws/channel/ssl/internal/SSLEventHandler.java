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
package com.ibm.ws.channel.ssl.internal;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventHandler;
import com.ibm.websphere.event.Topic;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Event handler for the SSL channel.
 */
public class SSLEventHandler implements EventHandler {

    private static final TraceComponent tc = Tr.register(
                                                         SSLEventHandler.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);

    /** Event topic used for queued SSL work */
    public static final Topic TOPIC_QUEUED_WORK = new Topic("com/ibm/ws/channel/ssl/QUEUED_WORK");
    public static final String TOPIC_QUEUED_WORK_NAME = TOPIC_QUEUED_WORK.getName();
    /** Event property key for finding the actual runnable task */
    public static final String KEY_RUNNABLE = "SSLWork";

    /**
     * Constructor.
     */
    public SSLEventHandler() {
    // nothing to do
    }

    /**
     * Activate this component.
     * 
     * @param context
     */
    protected void activate(ComponentContext context) {
    // nothing
    }

    /**
     * Deactivate this component.
     * 
     * @param context
     */
    protected void deactivate(ComponentContext context) {
    // nothing
    }

    /*
     * @see com.ibm.websphere.event.EventHandler#handleEvent(com.ibm.websphere.event.Event)
     */
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        if (topic.equals(TOPIC_QUEUED_WORK_NAME)) {
            Runnable r = event.getProperty(KEY_RUNNABLE, Runnable.class);
            if (null != r) {
                r.run();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to find runnable in event; " + topic);
                }
            }
        }
    }

}
