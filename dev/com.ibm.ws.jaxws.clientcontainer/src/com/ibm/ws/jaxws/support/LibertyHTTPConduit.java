/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.util.ThreadContextAccessor;

/**
 * LibertyHTTPConduit extends HTTPConduit so that we can set the TCCL when run the handleResponseInternal asynchronously
 */
public class LibertyHTTPConduit extends HTTPConduit {

    //save the bus so that we can get classloder from it.
    private final Bus bus;

    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = ThreadContextAccessor.getThreadContextAccessor();

    public LibertyHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
        this.bus = b;
    }

    @Override
    protected String getAddress() {
        return super.getAddress();
    }

    @Override
    protected void finalizeConfig() {
        super.finalizeConfig();
    }

    @Override
    protected OutputStream createOutputStream(Message message, HttpURLConnection connection, boolean needToCacheRequest, boolean isChunking, int chunkThreshold) {
        return new LibertyWrappedOutputStream(message, connection, needToCacheRequest, isChunking, chunkThreshold, getConduitName());
    }

    protected class LibertyWrappedOutputStream extends HTTPConduit.WrappedOutputStream {

        protected LibertyWrappedOutputStream(Message outMessage, HttpURLConnection connection, boolean possibleRetransmit, boolean isChunking, int chunkThreshold,
                                             String conduitName) {
            super(outMessage, connection, possibleRetransmit, isChunking, chunkThreshold, conduitName);
        }

        //handleResponse will call handleResponseInternal either synchronously or asynchronously
        //so if call asynchronously, we set the thread context classloader because liberty executor won't set anything when run the task.
        @Override
        protected void handleResponseInternal() throws IOException {
            if (outMessage == null
                   || outMessage.getExchange() == null
                   || outMessage.getExchange().isSynchronous()) {
                super.handleResponseInternal();
            } else {
                ClassLoader oldCl = THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread());
                try {
                    // get the classloader from bus
                    ClassLoader cl = bus.getExtension(ClassLoader.class);
                    if (cl != null) {
                        THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), cl);
                    }
                    super.handleResponseInternal();
                } finally {
                    THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), oldCl);
                }
            }
        }

    }
}
