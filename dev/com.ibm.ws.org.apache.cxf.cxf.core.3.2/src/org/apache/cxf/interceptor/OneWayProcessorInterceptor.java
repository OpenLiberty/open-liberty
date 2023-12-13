/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.workqueue.WorkQueueManager;


/**
 *
 */
public class OneWayProcessorInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String USE_ORIGINAL_THREAD
        = OneWayProcessorInterceptor.class.getName() + ".USE_ORIGINAL_THREAD";
    private static final Logger LOG = LogUtils.getL7dLogger(OneWayProcessorInterceptor.class);

    public OneWayProcessorInterceptor() {
        super(Phase.PRE_LOGICAL);
    }
    public OneWayProcessorInterceptor(String phase) {
        super(phase);
    }
    
    @Override
    public void handleFault(Message message) {
        if (message.getExchange().isOneWay()
            && !isRequestor(message)) {
            //in a one way, if an exception is thrown, the stream needs to be closed
            InputStream in = message.getContent(InputStream.class);
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //ignore
                }
            }

        }
    }
    public void handleMessage(Message message) {

        if (message.getExchange().isOneWay()
            && !isRequestor(message)
            && message.get(OneWayProcessorInterceptor.class) == null
            && message.getExchange().get(Executor.class) == null) {
            //one way on server side, fork the rest of this chain onto the
            //workqueue, call the Outgoing chain directly.

            message.put(OneWayProcessorInterceptor.class, this);
            final InterceptorChain chain = message.getInterceptorChain();

            boolean robust =
                MessageUtils.getContextualBoolean(message, Message.ROBUST_ONEWAY, false);

            boolean useOriginalThread =
                MessageUtils.getContextualBoolean(message, USE_ORIGINAL_THREAD, false);

            if (!useOriginalThread && !robust) {
                //need to suck in all the data from the input stream as
                //the transport might discard any data on the stream when this
                //thread unwinds or when the empty response is sent back
                DelegatingInputStream in = message.getContent(DelegatingInputStream.class);
                if (in != null) {
                    in.cacheInput();
                }
            }

            if (robust) {
                // continue to invoke the chain
                chain.pause();
                chain.resume();
                if (message.getContent(Exception.class) != null) {
                    // CXF-5629 fault has been delivered alread in resume()
                    return;
                }
            }

            try {
                Message partial = createMessage(message.getExchange());
                partial.remove(Message.CONTENT_TYPE);
                partial.setExchange(message.getExchange());
                Conduit conduit = message.getExchange().getDestination()
                    .getBackChannel(message);
                if (conduit != null) {
                    message.getExchange().setInMessage(null);
                    //for a one-way, the back channel could be
                    //null if it knows it cannot send anything.
                    conduit.prepare(partial);
                    conduit.close(partial);
                    message.getExchange().setInMessage(message);
                }
            } catch (IOException e) {
                //IGNORE
            }

            if (!useOriginalThread && !robust) {
                chain.pause();
                try {
                    final Object lock = new Object();
                    synchronized (lock) {
                        message.getExchange().getBus().getExtension(WorkQueueManager.class)
                            .getAutomaticWorkQueue().execute(new Runnable() {
                                public void run() {
                                    synchronized (lock) {
                                        lock.notifyAll();
                                    }
                                    chain.resume();
                                }
                            });
                        //wait a few milliseconds for the background thread to start processing
                        //Mostly just to make an attempt at keeping the ordering of the
                        //messages coming in from a client.  Not guaranteed though.
                        lock.wait(20L);
                    }
                } catch (RejectedExecutionException e) {
                    LOG.warning(
                        "Executor queue is full, run the oneway invocation task in caller thread."
                        + "  Users can specify a larger executor queue to avoid this.");
                    // only block the thread if the prop is unset or set to false, otherwise let it go
                    if (!MessageUtils.getContextualBoolean(message,
                            "org.apache.cxf.oneway.rejected_execution_exception", false)) {
                        //the executor queue is full, so run the task in the caller thread
                        chain.unpause();
                    }

                } catch (InterruptedException e) {
                    //ignore - likely a busy work queue so we'll just let the one-way go
                }
            }
        }
    }

    private static Message createMessage(Exchange exchange) {
        Endpoint ep = exchange.getEndpoint();
        Message msg = null;
        if (ep != null) {
            msg = new MessageImpl();
            msg.setExchange(exchange);
            msg = ep.getBinding().createMessage(msg);
        }
        return msg;
    }


}
