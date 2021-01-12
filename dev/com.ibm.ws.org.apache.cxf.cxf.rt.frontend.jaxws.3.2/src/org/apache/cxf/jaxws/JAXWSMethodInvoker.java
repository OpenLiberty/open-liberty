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

    // public static final String COPY_SOAP_HEADERS_BY_FAULT = "org.apache.cxf.fault.copySoapHeaders";  Liberty change: line removed\
    private static final Logger LOG = LogUtils.getL7dLogger(JAXWSMethodInvoker.class);  // Liberty change: line added

    javax.xml.ws.spi.Invoker invoker;

    public JAXWSMethodInvoker(final Object bean) {
        super(new SingletonFactory(bean));
    }

    public JAXWSMethodInvoker(Factory factory) {
        super(factory);
    }
    public JAXWSMethodInvoker(javax.xml.ws.spi.Invoker i) {
        super(null);
        invoker = i;
    }

    @Override
    protected Object performInvocation(Exchange exchange, final Object serviceObject, Method m,
                                       Object[] paramArray) throws Exception {
        if (invoker != null) {
            return invoker.invoke(m, paramArray);
        }
        return super.performInvocation(exchange, serviceObject, m, paramArray);
    }

    @Override
    protected Object invoke(Exchange exchange,
                            final Object serviceObject, Method m,
                            List<Object> params) {

        // set up the webservice request context
        WrappedMessageContext ctx = new WrappedMessageContext(exchange.getInMessage(), Scope.APPLICATION);

        Map<String, Object> handlerScopedStuff = removeHandlerProperties(ctx);

        WebServiceContextImpl.setMessageContext(ctx); // Liberty change: final MessageContext oldCtx =  assignment is removed
        List<Object> res = null;
        try {
            if ((params == null || params.isEmpty()) && serviceObject instanceof Provider) {
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
            // if (MessageUtils.getContextualBoolean(exchange.getInMessage(), COPY_SOAP_HEADERS_BY_FAULT, true)) { Liberty change: if block is removed
                updateHeader(exchange, ctx);
            // }
            throw f;
        } finally {
          // Liberty change: lines below are added
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

          LOG.log(Level.FINE, "Checking for org.apache.cxf.stax.force-start-document in MessageContext...");
          boolean forceXmlDecl = MessageUtils.isTrue(ctx.get("org.apache.cxf.stax.force-start-document"));
          if (forceXmlDecl) {
             LOG.log(Level.FINE, "Setting org.apache.cxf.stax.force-start-document property in Message: " + forceXmlDecl);
             Message m2 = exchange.getOutMessage();
             if (m2 != null) {
                 LOG.log(Level.FINE, "Setting force-start-document in Message: " + m2);
                 m2.put("org.apache.cxf.stax.force-start-document", Boolean.TRUE);
             } // Liberty change: end
           }
          /* Liberty change: if block below is removed
            //restore the WebServiceContextImpl's ThreadLocal variable to the previous value
            if (oldCtx == null) {
                WebServiceContextImpl.clear();
            } else {
                WebServiceContextImpl.setMessageContext(oldCtx);
            } */

            // Liberty change: 2 lines below are added
            //clear the WebServiceContextImpl's ThreadLocal variable
            WebServiceContextImpl.clear();

            addHandlerProperties(ctx, handlerScopedStuff);
        }
        return res;
    }

    private void changeToOneway(Exchange exchange) {
        exchange.setOneWay(true);
        // exchange.setOutMessage(null);  Liberty change: line removed
        javax.servlet.http.HttpServletResponse httpresp =
            (javax.servlet.http.HttpServletResponse)exchange.getInMessage().
                get("HTTP.RESPONSE");
        if (httpresp != null) {
            httpresp.setStatus(202);
        }
    }

}
