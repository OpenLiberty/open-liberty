/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.internal;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.Source;

public class HandlerManager {

    /* Handler identifier, typically this is same as the name of the handler */
    private String handlerId;

    /* Reference to the handler implementation */
    private Handler handler;

    /* List of subscribed sources */
    private final Set<String> subscribedSources;

    /*
     * List of pending subscriptions for this handler, when a handler subscribes to a
     * source and the source is not available, collector manager will add that source
     * to the handler's pending subscription list.
     */
    private final Set<String> pendingSubscriptions;

    public HandlerManager(Handler handler) {
        setHandler(handler);
        subscribedSources = new HashSet<String>();
        pendingSubscriptions = new HashSet<String>();
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        if (handler != null) {
            this.handler = handler;
            this.handlerId = CollectorManagerUtils.getHandlerId(handler);
        }
    }

    public void unsetHandler(Handler handler) {
        if (this.handler == handler) {
            this.handler = null;
        }
    }

    /*
     * Method to add a subscribed source
     * A source can either be in pending subscription list or the
     * subscribed source list.
     */
    public void addSubscribedSource(Source source) {
        String sourceId = CollectorManagerUtils.getSourceId(source);
        //If present in pending subscription list remove it.
        pendingSubscriptions.remove(sourceId);
        subscribedSources.add(sourceId);
    }

    /*
     * Method to remove a subscribed source.
     */
    public void removeSubscribedSource(Source source) {
        String sourceId = CollectorManagerUtils.getSourceId(source);
        subscribedSources.remove(sourceId);
    }

    /*
     * Method to add a source to pending subscription list
     */
    public void addPendingSubscription(String sourceId) {
        if (!subscribedSources.contains(sourceId)) {
            pendingSubscriptions.add(sourceId);
        }
    }

    public Set<String> getSubsribedSources() {
        return subscribedSources;
    }

    public Set<String> getPendingSubscriptions() {
        return pendingSubscriptions;
    }
}
