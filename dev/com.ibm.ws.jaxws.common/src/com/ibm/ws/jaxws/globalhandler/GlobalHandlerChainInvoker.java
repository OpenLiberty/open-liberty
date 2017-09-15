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

package com.ibm.ws.jaxws.globalhandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.webservices.handler.Handler;

/**
 * invoke the global handlers in a registered handler chain
 * The glovalHandlers is a sorted list, we invoke the handleMessage() method of them one by one
 * If there is any exception thrown, we will call handleFault() method of all previously called handlers.
 */
public class GlobalHandlerChainInvoker {

    private static final TraceComponent tc = Tr.register(GlobalHandlerChainInvoker.class);

    private final List<Handler> invokedHandlers = new ArrayList<Handler>();

    public boolean invokeGlobalHandlers(GlobalHandlerJaxWsMessageContext context, List<Handler> handlersList) {
        return invokeHandlerChain(handlersList, context);
    }

    private boolean invokeHandlerChain(List<Handler> handlerChain, GlobalHandlerJaxWsMessageContext ctx) {
        if (handlerChain.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "no registered global handlers");
            }
            return true;
        }
        boolean continueProcessing = true;
        try {
            continueProcessing = invokeHandleMessage(handlerChain, ctx);
        } finally {
            WebServiceContextImpl.clear();
        }
        return continueProcessing;

    }

    @FFDCIgnore({ Exception.class })
    private boolean invokeHandleMessage(List<Handler> handlerChain, GlobalHandlerJaxWsMessageContext ctx) {
        boolean continueProcessing = true;
        Handler currentHandler = null;
        try {

            Iterator<Handler> it = handlerChain.iterator();
            while (it.hasNext()) {
                Handler h = it.next();
                currentHandler = h;
                //remember which handler is under processing so that we can trace the handler out once it encounters errors
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Start calling handleMessage() method " + "for handler: " + currentHandler.getClass().getName());
                }
                h.handleMessage(ctx);
                markHandlerInvoked(h);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Finished calling handleMessage() method " + "for handler: " + currentHandler.getClass().getName());
                }
            }

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && currentHandler != null) {
                Tr.debug(tc, "handleMessage() method raised exception" + e.getMessage() + "for handler: " + currentHandler.getClass().getName());
            }

            // when Exception occurs, call handleFault() method for all previous called global handlers, but not call current
            // global handler's handleFault() method
            invokeReversedHandleFault(ctx);

            continueProcessing = false;

            throw new RuntimeException(e);
        }

        return continueProcessing;
    }

    private void invokeReversedHandleFault(GlobalHandlerJaxWsMessageContext ctx) {
        Handler h = null;
        try {
            int index = invokedHandlers.size() - 1;
            while (index >= 0) {
                h = invokedHandlers.get(index);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Start calling handleFault() method " + "for handler: " + h.getClass().getName());
                }
                h.handleFault(ctx);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Finished calling handleFault() method " + "for handler: " + h.getClass().getName());
                }
                index--;
            }
        } catch (RuntimeException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception in handleFault() method on global handler  " + h.getClass().getName() + e.getMessage());
            }

            throw e;
        } catch (Throwable e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception in handleFault() method on global handler  " + h.getClass().getName() + e.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    private void markHandlerInvoked(Handler h) {
        if (!invokedHandlers.contains(h)) {
            invokedHandlers.add(h);
        }
    }

}
