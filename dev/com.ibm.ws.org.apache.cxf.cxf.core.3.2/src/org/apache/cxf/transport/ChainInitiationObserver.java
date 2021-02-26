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

package org.apache.cxf.transport;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.https.CertConstraintsInterceptor;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class ChainInitiationObserver implements MessageObserver {
    protected Endpoint endpoint;
    protected Bus bus;
    protected ClassLoader loader;

    private final PhaseChainCache chainCache = new PhaseChainCache();

    public ChainInitiationObserver(Endpoint endpoint, Bus bus) {
        super();
        this.endpoint = endpoint;
        this.bus = bus;
        if (bus != null) {
            loader = bus.getExtension(ClassLoader.class);
        }
    }

    @FFDCIgnore(value = { RuntimeException.class })
    @Override
    public void onMessage(Message m) {
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);

        try {
            //no need reset TCClassloader as already set to bus
//            if (loader != null) {
//                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
//            }
            InterceptorChain phaseChain = null;

            if (m.getInterceptorChain() != null) {
                phaseChain = m.getInterceptorChain();
                // To make sure the phase chain is run by one thread once
                synchronized (phaseChain) {
                    if (phaseChain.getState() == InterceptorChain.State.PAUSED
                        || phaseChain.getState() == InterceptorChain.State.SUSPENDED) {
                        phaseChain.resume();
                        return;
                    }
                }
            }

            Message message = getBinding().createMessage(m);
            Exchange exchange = message.getExchange();
            if (exchange == null) {
                exchange = new ExchangeImpl();
                m.setExchange(exchange);
            }
            exchange.setInMessage(message);
            setExchangeProperties(exchange, message);

            InterceptorProvider dbp = null;
            if (endpoint.getService().getDataBinding() instanceof InterceptorProvider) {
                dbp = (InterceptorProvider) endpoint.getService().getDataBinding();
            }
            // setup chain
            if (dbp == null) {
                phaseChain = chainCache.get(bus.getExtension(PhaseManager.class).getInPhases(),
                                            bus.getInInterceptors(),
                                            endpoint.getService().getInInterceptors(),
                                            endpoint.getInInterceptors(),
                                            getBinding().getInInterceptors());
            } else {
                phaseChain = chainCache.get(bus.getExtension(PhaseManager.class).getInPhases(),
                                            bus.getInInterceptors(),
                                            endpoint.getService().getInInterceptors(),
                                            endpoint.getInInterceptors(),
                                            getBinding().getInInterceptors(),
                                            dbp.getInInterceptors());
            }

            message.setInterceptorChain(phaseChain);

            phaseChain.setFaultObserver(endpoint.getOutFaultObserver());

            addToChain(phaseChain, message);

            try {
                phaseChain.doIntercept(message);
            } catch (RuntimeException re) {
                throw re;
            }

        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
//            if (origLoader != null) {
//                origLoader.reset();
//            }
        }
    }

    private void addToChain(InterceptorChain chain, Message m) {
        Collection<InterceptorProvider> providers = CastUtils.cast((Collection<?>) m.get(Message.INTERCEPTOR_PROVIDERS));
        if (providers != null) {
            for (InterceptorProvider p : providers) {
                chain.add(p.getInInterceptors());
            }
        }
        Collection<Interceptor<? extends Message>> is = CastUtils.cast((Collection<?>) m.get(Message.IN_INTERCEPTORS));
        if (is != null) {
            //this helps to detect if need add CertConstraintsInterceptor to chain
            String rqURL = (String) m.get(Message.REQUEST_URL);
            boolean isHttps = (rqURL != null && rqURL.indexOf("https:") > -1) ? true : false;
            for (Interceptor<? extends Message> i : is) {
                if (i instanceof CertConstraintsInterceptor && isHttps == false) {
                    continue;
                }

                chain.add(i);
            }
        }
        if (m.getDestination() instanceof InterceptorProvider) {
            chain.add(((InterceptorProvider) m.getDestination()).getInInterceptors());
        }
    }

    protected Binding getBinding() {
        return endpoint.getBinding();
    }

    protected void setExchangeProperties(Exchange exchange, Message m) {
        exchange.put(Endpoint.class, endpoint);
        exchange.put(Binding.class, getBinding());
        exchange.put(Bus.class, bus);
        if (exchange.getDestination() == null) {
            exchange.setDestination(m.getDestination());
        }
        if (endpoint != null && endpoint.getService() != null) {
            exchange.put(Service.class, endpoint.getService());

            EndpointInfo endpointInfo = endpoint.getEndpointInfo();

            if (endpointInfo.getService() != null) {
                QName serviceQName = endpointInfo.getService().getName();
                exchange.put(Message.WSDL_SERVICE, serviceQName);

                QName interfaceQName = endpointInfo.getService().getInterface().getName();
                exchange.put(Message.WSDL_INTERFACE, interfaceQName);

                QName portQName = endpointInfo.getName();
                exchange.put(Message.WSDL_PORT, portQName);
                URI wsdlDescription = endpointInfo.getProperty("URI", URI.class);
                if (wsdlDescription == null && !endpointInfo.hasProperty("URI")) {
                    String address = endpointInfo.getAddress();
                    try {
                        wsdlDescription = new URI(address + "?wsdl");
                    } catch (URISyntaxException e) {
                        // do nothing
                    }
                    endpointInfo.setProperty("URI", wsdlDescription);
                }
                exchange.put(Message.WSDL_DESCRIPTION, wsdlDescription);
            }
        } else {
            exchange.put(Service.class, null);
        }
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

}
