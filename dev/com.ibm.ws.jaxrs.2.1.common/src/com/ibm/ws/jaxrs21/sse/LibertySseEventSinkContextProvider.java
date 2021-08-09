/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.sse;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.sse.OutboundSseEventBodyWriter;
import org.apache.cxf.message.Message;

/**
 * This class is invoked prior to invoking resource methods containing
 * <code>SseEventSink</code> parameters annotated with the <code>@Context</code>
 * annotation.  It provides the injectable implementation of SseEventSink and sets up
 * the server runtime to execute similar to async resource methods.
 */
public class LibertySseEventSinkContextProvider implements ContextProvider<SseEventSink> {

    /* (non-Javadoc)
     * @see org.apache.cxf.jaxrs.ext.ContextProvider#createContext(org.apache.cxf.message.Message)
     */
    @Override
    public SseEventSink createContext(Message message) {
        MessageBodyWriter<OutboundSseEvent> writer = new OutboundSseEventBodyWriter(ServerProviderFactory.getInstance(message), message.getExchange());
        LibertySseEventSinkImpl impl = new LibertySseEventSinkImpl(writer, message);
        message.put(SseEventSink.class.getName(), impl);
        
        // treat SSE methods like async methods
        AsyncResponseImpl ar = new AsyncResponseImpl(message);
        message.put(AsyncResponseImpl.class.getName(), ar);
        
        return impl;
    }

}
