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

package org.apache.cxf.jaxws;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.Provider;
import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.invoker.SingletonFactory;

public class JAXWSMethodInvoker extends AbstractJAXWSMethodInvoker {

    private static final Logger LOG = LogUtils.getL7dLogger(JAXWSMethodInvoker.class);

    public JAXWSMethodInvoker(final Object bean) {
        super(new SingletonFactory(bean));
    }
    
    public JAXWSMethodInvoker(Factory factory) {
        super(factory);
    }
     
    @Override
    protected Object invoke(Exchange exchange, 
                            final Object serviceObject, Method m,
                            List<Object> params) {
        
        // set up the webservice request context 
        WrappedMessageContext ctx = new WrappedMessageContext(exchange.getInMessage(), Scope.APPLICATION);
        
        Map<String, Object> handlerScopedStuff = removeHandlerProperties(ctx);
        
        WebServiceContextImpl.setMessageContext(ctx);
        List<Object> res = null;
        try {
            if ((params == null || params.isEmpty()) && m.getDeclaringClass().equals(Provider.class)) {
                params = Collections.singletonList(null);
            }
            res = CastUtils.cast((List<?>)super.invoke(exchange, serviceObject, m, params));
                        
            if ((serviceObject instanceof Provider) 
                && MessageUtils.getContextualBoolean(exchange.getInMessage(), 
                                                     "jaxws.provider.interpretNullAsOneway",
                                                     true)
                && (res != null && !res.isEmpty() && res.get(0) == null)
                && exchange.getInMessage().getInterceptorChain().getState() == InterceptorChain.State.EXECUTING) {
                // treat the non-oneway call as oneway when a provider returns null
                // and the chain is not suspended due to a continuation suspend
                res = null;
                changeToOneway(exchange);
            }
            //update the webservice response context
            updateWebServiceContext(exchange, ctx);
        } catch (Fault f) {
            //get chance to copy over customer's header
            updateHeader(exchange, ctx);
            throw f;
        } finally {
            if (serviceObject instanceof Provider) {
                LOG.log(Level.FINE, "Checking for cxf.add.attachments property in MessageContext...");
                boolean addAttachments = MessageUtils.isTrue(ctx.get("cxf.add.attachments"));
                if (addAttachments) {
                    LOG.log(Level.FINE, "Setting cxf.add.attachments property in Message: " + addAttachments);
                    Message m1 = exchange.getOutMessage();
                    if (m1 != null) {
                        m1.put("cxf.add.attachments", Boolean.TRUE);
                    }
                }
            }

            //clear the WebServiceContextImpl's ThreadLocal variable
            WebServiceContextImpl.clear();
            
            addHandlerProperties(ctx, handlerScopedStuff);
        }
        return res;
    }

    private void changeToOneway(Exchange exchange) {
        exchange.setOneWay(true);
        javax.servlet.http.HttpServletResponse httpresp = 
            (javax.servlet.http.HttpServletResponse)exchange.getInMessage().
                get("HTTP.RESPONSE");
        if (httpresp != null) {
            httpresp.setStatus(202);
        }
    }
    
}
