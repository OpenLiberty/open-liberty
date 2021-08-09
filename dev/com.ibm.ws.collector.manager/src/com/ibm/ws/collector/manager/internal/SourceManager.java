/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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

import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.Source;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

public class SourceManager {

    /* Source identifier, typically a concatenated string of source name and location */
    private String sourceId;

    /* Reference to the source implementation */
    private Source source;

    /* Reference to the buffer manager instance */
    private BufferManagerImpl bufferMgr;

    /* List of subscribed handlers */
    private final Set<String> subscribers;

    private final int BUFFER_SIZE = 10000;

    public SourceManager(Source source) {
        setSource(source);
        subscribers = new HashSet<String>();
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        if (source != null) {
            this.source = source;
            this.sourceId = CollectorManagerUtils.getSourceId(source);
        }
    }

    public void unsetSource(Source source) {
        if (this.source == source) {
            this.source = null;
        }
    }

    public BufferManager getBufferManager() {
        return bufferMgr;
    }

    public void setBufferManager(BufferManager bufferMgr) {
        this.bufferMgr = (BufferManagerImpl) bufferMgr;
    }

    /**
     * Method to add a subscriber to the source
     * Source manager will allocate a buffer when the source has
     * atleast one subscriber.
     *
     * @param handler The handler that is to be added as subscriber
     */
    public void addSubscriber(Handler handler) {
        String handlerId = CollectorManagerUtils.getHandlerId(handler);

        //temporary exception for audit Source  //must change later
        if (source.getSourceName().trim().equals("audit") && subscribers.isEmpty()) {
            //First subscriber, assign a buffer.
            bufferMgr = new BufferManagerImpl(BUFFER_SIZE, sourceId, false);
            //Inform the source that a buffer is now available
            //and it can start sending events to this buffer.
            source.setBufferManager(this.bufferMgr);
        }
        subscribers.add(handlerId);
        /*
         * Inform the handler that this source/buffer/conduit is now available:
         * Synchronous Handler - Add handler as synchronous handler to the Buffer/Conduit's sync handler set
         * Asynchronous Handler - Add handler as asynchronous handler into the Buffer/Conduit
         *
         */
        if (handler instanceof SynchronousHandler) {
            bufferMgr.addSyncHandler((SynchronousHandler) handler);
        } else {
            bufferMgr.addHandler(handlerId);
        }

    }

    /**
     * Method to remove a subscriber from the source
     * Source manager deallocate BufferManager when last subscriber goes away
     *
     * @param handler The handler that is to be removed as subscriber
     * @return a boolean to indicate whether last subscriber has been removed, true for yes false for no
     */
    public boolean removeSubscriber(Handler handler) {
        String handlerId = CollectorManagerUtils.getHandlerId(handler);
        subscribers.remove(handlerId);
        /*
         * Inform the handler that this source/buffer will no longer be available:
         * Synchronous Handler: Remove the synchronous handler from the Buffer/Conduit's sync handler set
         * Asynchronous Handler: Remove the asynchronous handler from the the Buffer/Conduit
         */
        if (handler instanceof SynchronousHandler) {
            bufferMgr.removeSyncHandler((SynchronousHandler) handler);
        } else {
            bufferMgr.removeHandler(handlerId);
        }

        if (subscribers.isEmpty()) {

            /*
             * Can not set bufferMgr to null (in here or in the source )if this SrcMgr belongs
             * to LogSource or TraceSource
             */
            if (!sourceId.contains(CollectorConstants.MESSAGES_SOURCE) && !sourceId.contains(CollectorConstants.TRACE_SOURCE)) {
                /*
                 * Last subscriber, unassign the buffer
                 * Inform the source that buffer will no longer be available
                 * and it should stop sending events to this buffer.
                 */
                source.unsetBufferManager(bufferMgr);
                bufferMgr = null;
            }
            return true;
        }
        return false;
    }

    public Set<String> getSubscriptions() {
        return subscribers;
    }
}
