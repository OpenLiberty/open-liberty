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
package com.ibm.ws.http.channel.internal.inbound;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventHandler;
import com.ibm.websphere.event.Topic;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpMessages;

/**
 * OSGI event handler to handle HTTP pipelined request events.
 */
@Component(configurationPid = "com.ibm.ws.http.HttpPipelineEventHandler", property = { "event.topics=com/ibm/ws/http/channel/events/PIPELINING", "service.vendor=IBM" })
public class HttpPipelineEventHandler implements EventHandler {

    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpPipelineEventHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Event topic for pipelined request handling */
    public static final Topic TOPIC_PIPELINING = new Topic("com/ibm/ws/http/channel/events/PIPELINING");

    /**
     * Constructor.
     */
    public HttpPipelineEventHandler() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created the HTTP pipeline event handler");
        }
    }

    /**
     * DS method for activation of this service component.
     * 
     * @param context
     */
    @SuppressWarnings("unused")
    @Activate
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating the HTTP pipeline event handler");
        }
    }

    /**
     * DS method for deactivation of this service component.
     * 
     * @param context
     */
    @SuppressWarnings("unused")
    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating the HTTP pipeline event handler");
        }
    }

    /*
     * @see
     * org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.
     * Event)
     */
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Received event topic: " + topic);
        }
        HttpInboundLink link = event.getProperty(CallbackIDs.CALLBACK_HTTPICL.getName(), HttpInboundLink.class);
        if (null != link) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Processing request on new thread; " + link);
            }
            link.ready(link.getVirtualConnection());
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to process pipelined request with null link");
            }
        }
    }

}
