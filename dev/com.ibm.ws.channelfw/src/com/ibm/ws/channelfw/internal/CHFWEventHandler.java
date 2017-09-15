/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelUtils;
import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventHandler;
import com.ibm.websphere.event.Topic;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * Event handler for channel framework specific events.
 */
public class CHFWEventHandler implements EventHandler {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(CHFWEventHandler.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /** Event when factories are missing during chain loads */
    public static final Topic EVENT_CHECK_MISSING = new Topic("com/ibm/websphere/channelfw/CHECK_MISSING");

    /**
     * Constructor.
     */
    public CHFWEventHandler() {
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
     * @see
     * com.ibm.websphere.event.EventHandler#handleEvent(com.ibm.websphere.event
     * .Event)
     */
    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        if (topic.equalsIgnoreCase(ChannelFramework.EVENT_STOPCHAIN.getName())) {
            String chainName = event.getProperty(ChannelFramework.EVENT_CHAINNAME, String.class);
            if (null != chainName) {
                stopChain(chainName, event);
            } else {
                String channelName = event.getProperty(ChannelFramework.EVENT_CHANNELNAME, String.class);
                if (null != channelName) {
                    stopChannel(channelName, event);
                }
            }
        } else if (topic.equalsIgnoreCase(EVENT_CHECK_MISSING.getName())) {
            ChannelUtils.checkMissingConfig();
        }
    }

    /**
     * Stop the explicit chain provided.
     * 
     * @param name
     * @param event
     */
    private void stopChain(String name, Event event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stop chain event; chain=" + name);
        }
        ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        try {
            if (cf.isChainRunning(name)) {
                // stop the chain now.. 
                cf.stopChain(name, 0L);
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "stopChain", new Object[] { event, cf });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error stopping chain; " + e);
            }
        }
    }

    /**
     * Stop chains using the provided channel.
     * 
     * @param name
     * @param event
     */
    private void stopChannel(String name, Event event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stop chain event; channel=" + name);
        }
        ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
        try {
            ChainData[] chains = cf.getAllChains(name);
            for (ChainData chain : chains) {
                if (cf.isChainRunning(chain)) {
                    // stop the chain now.. 
                    cf.stopChain(chain, 0);
                }
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "stopChannel", new Object[] { event, cf });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error stopping chain; " + e);
            }
        }
    }
}
