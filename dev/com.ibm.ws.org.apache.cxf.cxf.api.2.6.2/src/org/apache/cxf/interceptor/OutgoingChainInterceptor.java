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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.PreexistingConduitSelector;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.transport.Conduit;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;


public class OutgoingChainInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(OutgoingChainInterceptor.class);
    
    private PhaseChainCache chainCache = new PhaseChainCache();
    
    @Trivial
    public OutgoingChainInterceptor() {
        super(Phase.POST_INVOKE);
    }

    public void handleMessage(@Sensitive Message message) {
        LOG.entering("OutgoingChainInterceptor", "handleMessage");
        Exchange ex = message.getExchange();
        BindingOperationInfo binding = ex.get(BindingOperationInfo.class);
        //if we get this far, we're going to be outputting some valid content, but we COULD
        //also be "echoing" some of the content from the input.   Thus, we need to 
        //mark it as requiring the input to be cached.   
        message.put("cxf.io.cacheinput", Boolean.TRUE);
        if (null != binding && null != binding.getOperationInfo() && binding.getOperationInfo().isOneWay()) {
            closeInput(message);
            LOG.exiting("OutgoingChainInterceptor", "handleMessage");
            return;
        }
        Message out = ex.getOutMessage();
        if (out != null) {
            getBackChannelConduit(message);
            if (binding != null) {
                out.put(MessageInfo.class, binding.getOperationInfo().getOutput());
                out.put(BindingMessageInfo.class, binding.getOutput());
            }
            
            InterceptorChain outChain = out.getInterceptorChain();
            if (outChain == null) {
                outChain = OutgoingChainInterceptor.getChain(ex, chainCache);
                out.setInterceptorChain(outChain);
            }
            outChain.doIntercept(out);
        }
        LOG.exiting("OutgoingChainInterceptor", "handleMessage");
    }
    
    @Trivial
    private void closeInput(Message message) {
        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            try {
                is.close();
                message.removeContent(InputStream.class);
            } catch (IOException ioex) {
                //ignore
            }
        }
    }

    @Trivial
    protected static Conduit getBackChannelConduit(Message message) {
        LOG.entering("OutgoingChainInterceptor", "getBackChannelConduit");
        Conduit conduit = null;
        Exchange ex = message.getExchange();
        if (ex.getConduit(message) == null
            && ex.getDestination() != null) {
            try {
                conduit = ex.getDestination().getBackChannel(ex.getInMessage(), null, null);
                ex.put(ConduitSelector.class, 
                       new PreexistingConduitSelector(conduit, ex.get(Endpoint.class)));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        LOG.exiting("OutgoingChainInterceptor", "getBackChannelConduit");
        return conduit;
    }
    
    @Trivial
    public static InterceptorChain getOutInterceptorChain(Exchange ex) {
        Bus bus = ex.get(Bus.class);
        Binding binding = ex.get(Binding.class);
        PhaseManager pm = bus.getExtension(PhaseManager.class);
        PhaseInterceptorChain chain = new PhaseInterceptorChain(pm.getOutPhases());
        
        Endpoint ep = ex.get(Endpoint.class);
        List<Interceptor<? extends Message>> il = ep.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + il);
        }
        chain.add(il);
        il = ep.getService().getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by service: " + il);
        }
        chain.add(il);
        il = bus.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + il);
        }
        chain.add(il);        
        if (binding != null) {
            il = binding.getOutInterceptors();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by binding: " + il);
            }
            chain.add(il);
        }
        modifyChain(chain, ex);
        chain.setFaultObserver(ep.getOutFaultObserver());
        return chain;
    }

    @Trivial
    private static void modifyChain(PhaseInterceptorChain chain, Exchange ex) {
        LOG.entering("OutgoingChainInterceptor", "modifyChain");
        modifyChain(chain, ex.getInMessage());
        modifyChain(chain, ex.getOutMessage());
        LOG.exiting("OutgoingChainInterceptor", "modifyChain");
    }

    @Trivial
    private static void modifyChain(PhaseInterceptorChain chain, Message m) {
        LOG.entering("OutgoingChainInterceptor", "modifyChain");
        if (m == null) {
            LOG.exiting("OutgoingChainInterceptor", "modifyChain");
            return;
        }
        Collection<InterceptorProvider> providers 
            = CastUtils.cast((Collection<?>)m.get(Message.INTERCEPTOR_PROVIDERS));
        if (providers != null) {
            for (InterceptorProvider p : providers) {
                chain.add(p.getOutInterceptors());
            }
        }
        Collection<Interceptor<? extends Message>> is 
            = CastUtils.cast((Collection<?>)m.get(Message.OUT_INTERCEPTORS));
        if (is != null) {
            chain.add(is);
        }
        if (m.getDestination() instanceof InterceptorProvider) {
            chain.add(((InterceptorProvider)m.getDestination()).getOutInterceptors());
        }
        LOG.exiting("OutgoingChainInterceptor", "modifyChain");
    }
    
    @Trivial
    private static PhaseInterceptorChain getChain(Exchange ex, PhaseChainCache chainCache) {
        Bus bus = ex.get(Bus.class);
        Binding binding = ex.get(Binding.class);
        
        Endpoint ep = ex.get(Endpoint.class);
        
        List<Interceptor<? extends Message>> i1 = bus.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + i1);
        }
        List<Interceptor<? extends Message>> i2 = ep.getService().getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by service: " + i2);
        }
        List<Interceptor<? extends Message>> i3 = ep.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + i3);
        }
        List<Interceptor<? extends Message>> i4 = null;
        if (binding != null) {
            i4 = binding.getOutInterceptors();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by binding: " + i4);
            }
        }
        List<Interceptor<? extends Message>> i5 = null;
        if (ep.getService().getDataBinding() instanceof InterceptorProvider) {
            i5 = ((InterceptorProvider)ep.getService().getDataBinding()).getOutInterceptors();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by databinding: " + i5);
            }
            if (i4 == null) {
                i4 = i5;
                i5 = null;
            }
        }
        PhaseInterceptorChain chain;
        if (i5 != null) {
            chain = chainCache.get(bus.getExtension(PhaseManager.class).getOutPhases(),
                                   i1, i2, i3, i4, i5);
        } else if (i4 != null) {
            chain = chainCache.get(bus.getExtension(PhaseManager.class).getOutPhases(),
                                   i1, i2, i3, i4);
        } else {
            chain = chainCache.get(bus.getExtension(PhaseManager.class).getOutPhases(),
                                   i1, i2, i3);
        }
        
        modifyChain(chain, ex);
        chain.setFaultObserver(ep.getOutFaultObserver());
        return chain;
    }
    
    
}

