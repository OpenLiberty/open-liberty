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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.ws.Holder;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.common.logging.LogUtils;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Invokes a Binding's invoker with the <code>INVOCATION_INPUT</code> from
 * the Exchange.
 */

public class ServiceInvokerInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(ServiceInvokerInterceptor.class);  // Liberty Change

    public ServiceInvokerInterceptor() {
        super(Phase.INVOKE);
    }

    public void handleMessage(@Sensitive final Message message) {  // Liberty Change
        final Exchange exchange = message.getExchange();
        final Endpoint endpoint = exchange.getEndpoint();
        final Service service = endpoint.getService();
        final Invoker invoker = service.getInvoker();

        Runnable invocation = new Runnable() {

            public void run() {
                Exchange runableEx = message.getExchange();
                Object result = invoker.invoke(runableEx, getInvokee(message));
                if (!exchange.isOneWay()) {
                    Endpoint ep = exchange.getEndpoint();

                    Message outMessage = runableEx.getOutMessage();
                    if (outMessage == null) {
                        outMessage = new MessageImpl(16, 1); // perf: size 16 / factor 1 to avoid resize operation
                        outMessage.setExchange(exchange);
                        outMessage = ep.getBinding().createMessage(outMessage);
                        exchange.setOutMessage(outMessage);
                    }
                    copyJaxwsProperties(message, outMessage);
                    if (result != null) {
                        MessageContentsList resList = null;
                        if (result instanceof MessageContentsList) {
			    // Liberty Change start
			    LOG.finest("Result is instance of MessageContentsList"); 
                            resList = (MessageContentsList)result;
                        } else if (result instanceof List) {
			    LOG.finest("Result is instance of List");
                            resList = new MessageContentsList((List<?>)result);
                        } else if (result.getClass().isArray()) {
                            resList = new MessageContentsList((Object[])result);
			    LOG.finest("Result is Array");
                        } else {
			    LOG.finest("Calling setContent for Object.class");    
			    // Liberty Change end	
                            outMessage.setContent(Object.class, result);
                        }
                        if (resList != null) {
                            outMessage.setContent(List.class, resList);
                        }
                    }
                }
            }

        };

        Executor executor = getExecutor(endpoint);
        Executor executor2 = exchange.get(Executor.class);
        if (executor2 == executor || executor == null
            || !(message.getInterceptorChain() instanceof PhaseInterceptorChain)) {
            // already executing on the appropriate executor
            invocation.run();
        } else {
            exchange.put(Executor.class, executor);
            // The current thread holds the lock on PhaseInterceptorChain.
            // In order to avoid the executor threads deadlocking on any of
            // synchronized PhaseInterceptorChain methods the current thread
            // needs to release the chain lock and re-acquire it after the
            // executor thread is done

            final PhaseInterceptorChain chain = (PhaseInterceptorChain)message.getInterceptorChain();
            final AtomicBoolean contextSwitched = new AtomicBoolean();
            final FutureTask<Object> o = new FutureTask<Object>(invocation, null) {
                @Override
                protected void done() {
                    super.done();
                    if (contextSwitched.get()) {
                        PhaseInterceptorChain.setCurrentMessage(chain, null);
                        message.remove(Message.THREAD_CONTEXT_SWITCHED);
                    }
                    chain.releaseChain();
                }

                @Override
                public void run() {
                    if (PhaseInterceptorChain.setCurrentMessage(chain, message)) {
                        contextSwitched.set(true);
                        message.put(Message.THREAD_CONTEXT_SWITCHED, true);
                    }

                    synchronized (chain) {
                        super.run();
                    }
                }
            };
            synchronized (chain) {
                executor.execute(o);
                // the task will already be done if the executor uses the current thread
                // but the chain lock status still needs to be re-set
                chain.releaseAndAcquireChain();
            }
            try {
                o.get();
            } catch (InterruptedException e) {
                throw new Fault(e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)e.getCause();
                }
                throw new Fault(e.getCause());
            }
        }
    }

    private Object getInvokee(Message message) {
	// Liberty Change start
        LOG.finest("getInvokee: getContent from List: " + message.getClass().getCanonicalName());
        Object invokee = message.getContent(List.class);
        if (invokee == null) {
	    LOG.finest("getInvokee: getContent from Object");
            invokee = message.getContent(Object.class);
        }
	if (LOG.isLoggable(Level.FINEST)) {
	   if (invokee instanceof List) {
              for (Object o1 : (List)invokee) {
		 if (o1 != null) {
                    LOG.finest("Invokee param: " + o1.getClass().getCanonicalName());
                    if (o1 instanceof Holder) {
                       if (((Holder)o1).value != null) {
                          LOG.finest("Invokee Holder type: " + ((Holder)o1).value.getClass());
                       }
                    }
		 }
              }
           }
	}
	// Liberty Change end

        return invokee;
    }

    /**
     * Get the Executor for this invocation.
     * @param endpoint
     */
    private Executor getExecutor(final Endpoint endpoint) {
        return endpoint.getService().getExecutor();
    }

    private void copyJaxwsProperties(Message inMsg, Message outMsg) {
        outMsg.put(Message.WSDL_OPERATION, inMsg.get(Message.WSDL_OPERATION));
        outMsg.put(Message.WSDL_SERVICE, inMsg.get(Message.WSDL_SERVICE));
        outMsg.put(Message.WSDL_INTERFACE, inMsg.get(Message.WSDL_INTERFACE));
        outMsg.put(Message.WSDL_PORT, inMsg.get(Message.WSDL_PORT));
        outMsg.put(Message.WSDL_DESCRIPTION, inMsg.get(Message.WSDL_DESCRIPTION));
    }
}
