/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.sse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.sse.NoSuitableMessageBodyWriterException;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs21.clientconfig.JAXRSClientCompletionStageFactoryConfig;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.threading.CompletionStageFactory;

/**
 * This class implements the <code>SseEventSink</code> that is injected into
 * resource fields/methods and represent a client's handle to SSE events.
 */
public class LibertySseEventSinkImpl implements SseEventSink {
    private final static TraceComponent tc = Tr.register(LibertySseEventSinkImpl.class);

    private final MessageBodyWriter<OutboundSseEvent> writer;
    private final Message message;
    private final HttpServletResponse response;
    private volatile boolean closed;
    
    private static final CompletionStageFactory completionStageFactory = JAXRSClientCompletionStageFactoryConfig.getCompletionStageFactory();
    
    // (From ManagedCompletableFuture)  The Java SE 8 CompletableFuture lacks certain important methods, namely defaultExecutor and newIncompleteFuture.
    static final boolean JAVA8;
    static final boolean COMPLETION_STAGE_FACTORY_IS_NULL;
    static {
        int version = JavaInfo.majorVersion();
        JAVA8 = version == 8;        
        COMPLETION_STAGE_FACTORY_IS_NULL = completionStageFactory == null;
    }

    public LibertySseEventSinkImpl(MessageBodyWriter<OutboundSseEvent> writer, Message message) {
        this.writer = writer;
        this.message = message;
        this.response = message.get(HttpServletResponse.class);

        message.getExchange().put(JAXRSUtils.IGNORE_MESSAGE_WRITERS, "true");
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.sse.SseEventSink#close()
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                response.getOutputStream().close();
                HttpServletRequest req = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
                if (req != null) {
                    req.getAsyncContext().complete();
                }
            } catch (Exception ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to close response stream", ex);
                }
            } finally {
                ServerProviderFactory.releaseRequestState(message);
            }
        }

    }

    /* (non-Javadoc)
     * @see javax.ws.rs.sse.SseEventSink#isClosed()
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.sse.SseEventSink#send(javax.ws.rs.sse.OutboundSseEvent)
     */
    @FFDCIgnore({WebApplicationException.class, IOException.class, NoSuitableMessageBodyWriterException.class})
    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        final CompletableFuture<?> future = createCompleteableFuture();

        if (!closed) {
            if (writer != null) {
                ByteArrayOutputStream os = null;
                try {
                    os = new ByteArrayOutputStream();
                    writer.writeTo(event, event.getClass(), null, new Annotation [] {}, event.getMediaType(), null, os);

                    String eventContents = os.toString();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "send - sending " + eventContents);
                    }

                    if (!response.isCommitted()) {
                        response.setHeader("Content-Type", MediaType.SERVER_SENT_EVENTS);
                        response.flushBuffer();
                    }

                    //TODO: this seems like a bug, but most SSE clients seem to expect a named event
                    //      so for now, we will provide one if one is not provided by the user
                    if (event.getName() == null) {
                        response.getOutputStream().print("    UnnamedEvent\n");
                    }
                    response.getOutputStream().println(eventContents);
                    response.getOutputStream().flush();

                    return CompletableFuture.completedFuture(eventContents);
                } catch (NoSuitableMessageBodyWriterException ex) {
                    handleException(ex, future, event);
                    throw new IllegalArgumentException("No suitable message body writer for OutboundSseEvent created with data " + event.getData() + " and mediaType " + event.getMediaType() + ". The data contained within the OutboundSseEvent must match the mediaType."); 
                } catch (WebApplicationException ex) {
                    handleException(ex, future, event);
                } catch (IOException ex) {
                    handleException(ex, future, event);
                } finally {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException ex) {
                            //ignore
                        }
                    }
                }
            } else {  //no writer
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No MessageBodyWriter - returning null for event:  " + event);
                }
                future.complete(null);
            }  
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SseEventSink is closed - failed sending event:  " + event);
            }
            throw new IllegalStateException("SseEventSink is closed.");  
        }

        return future;
    }
    
    private void handleException(Throwable t, CompletableFuture future, OutboundSseEvent event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "send - failed sending event " + event);
        }
        future.completeExceptionally(t);
        close();
    }

    private CompletableFuture<?> createCompleteableFuture() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            if (JAVA8 || COMPLETION_STAGE_FACTORY_IS_NULL) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Running on Java 8 or in an Java SE environment.  Using ForkJoinPool.commonPool()");
                }
                return new CompletableFuture<>();
            } 
            // Use Liberty thread pool
            return completionStageFactory.newIncompleteFuture();
        }
        return AccessController.doPrivileged((PrivilegedAction<CompletableFuture<?>>)() -> {
            if (JAVA8 || COMPLETION_STAGE_FACTORY_IS_NULL) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Running on Java 8 or in an Java SE environment.  Using ForkJoinPool.commonPool()");
                }
                return new CompletableFuture<>();
            }
            // Use Liberty thread pool
            return completionStageFactory.newIncompleteFuture();
        });
    }
}
