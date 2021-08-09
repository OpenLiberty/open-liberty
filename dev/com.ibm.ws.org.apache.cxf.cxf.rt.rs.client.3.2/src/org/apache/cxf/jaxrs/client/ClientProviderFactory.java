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
package org.apache.cxf.jaxrs.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.RxInvokerProvider;
import javax.ws.rs.core.Configuration;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.ThreadLocalProxyCopyOnWriteArraySet;
import org.apache.cxf.message.Message;

public final class ClientProviderFactory extends ProviderFactory {
    //Liberty code change start
    private final AtomicReferenceProviderList<ClientRequestFilter> clientRequestFilters =
        new AtomicReferenceProviderList<>();
    private final AtomicReferenceProviderList<ClientResponseFilter> clientResponseFilters =
        new AtomicReferenceProviderList<>();
    private final AtomicReferenceProviderList<ResponseExceptionMapper<?>> responseExceptionMappers =
        new AtomicReferenceProviderList<>();
    //Liberty code change end

    private RxInvokerProvider<?> rxInvokerProvider;

    private ClientProviderFactory(Bus bus) {
        super(bus);
    }

    public static ClientProviderFactory createInstance(Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        ClientProviderFactory factory = new ClientProviderFactory(bus);
        ProviderFactory.initFactory(factory);
        factory.setBusProviders();
        return factory;
    }

    public static ClientProviderFactory getInstance(Message m) {
        return getInstance(m.getExchange().getEndpoint());
    }

    public static ClientProviderFactory getInstance(Endpoint e) {
        return (ClientProviderFactory) e.get(CLIENT_FACTORY_NAME);
    }

    @Override
    protected void setProviders(boolean custom, boolean busGlobal, Object... providers) {
        List<ProviderInfo<? extends Object>> theProviders = prepareProviders(custom, busGlobal, providers, null);
        super.setCommonProviders(theProviders);

        //Liberty code change start
        List<ProviderInfo<ClientRequestFilter>> newClientRequestFilters = new ArrayList<>();
        List<ProviderInfo<ClientResponseFilter>> newClientResponseFilters = new ArrayList<>();
        List<ProviderInfo<ResponseExceptionMapper<?>>> newResponseExceptionMappers = new ArrayList<>();
        //Liberty code change end

        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider.getProvider());
            if (providerCls == Object.class) {
                // If the provider is a lambda, ClassHelper.getRealClass returns Object.class
                providerCls = provider.getProvider().getClass();
            }
            if (filterContractSupported(provider, providerCls, ClientRequestFilter.class)) {
                //Liberty code change start
                addProviderToList(newClientRequestFilters, provider);
                //Liberty code change end
            }

            if (filterContractSupported(provider, providerCls, ClientResponseFilter.class)) {
                //Liberty code change start
                addProviderToList(newClientResponseFilters, provider);
                //Liberty code change end
            }

            if (ResponseExceptionMapper.class.isAssignableFrom(providerCls)) {
                //Liberty code change start
                addProviderToList(newResponseExceptionMappers, provider);
                //Liberty code change end
            }

            if (RxInvokerProvider.class.isAssignableFrom(providerCls)) {
                this.rxInvokerProvider = RxInvokerProvider.class.cast(provider.getProvider());
            }
        }

        //Liberty code change start
        if (newClientRequestFilters.size() > 0) {
            clientRequestFilters.addAndSortProviders(newClientRequestFilters,
                       new BindingPriorityComparator<ClientRequestFilter>(ClientRequestFilter.class, true), false);
        }
        if (newClientResponseFilters.size() > 0) {
            clientResponseFilters.addAndSortProviders(newClientResponseFilters, 
                       new BindingPriorityComparator<ClientResponseFilter>(ClientResponseFilter.class, false), false);
        }
        if (newResponseExceptionMappers.size() > 0) {
            responseExceptionMappers.addProviders(newResponseExceptionMappers);
        }

        injectContextProxies(responseExceptionMappers.get(), clientRequestFilters.get(), clientResponseFilters.get());
        //Liberty code change start
    }

    @SuppressWarnings("unchecked")
    public <T extends Throwable> ResponseExceptionMapper<T> createResponseExceptionMapper(
                                 Message m, Class<?> paramType) {

        //Liberty code change start
        return (ResponseExceptionMapper<T>)responseExceptionMappers.get().stream()
        //Liberty code change end
                .filter(em -> handleMapper(em, paramType, m, ResponseExceptionMapper.class, true))
                .map(ProviderInfo::getProvider)
                .sorted(new ProviderFactory.ClassComparator(paramType))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void clearProviders() {
        super.clearProviders();
        responseExceptionMappers.clear();
        clientRequestFilters.clear();
        clientResponseFilters.clear();
    }

    public List<ProviderInfo<ClientRequestFilter>> getClientRequestFilters() {
        //Liberty code change start
        return Collections.unmodifiableList(clientRequestFilters.get());
        //Liberty code change end
    }

    public List<ProviderInfo<ClientResponseFilter>> getClientResponseFilters() {
        //Liberty code change start
        return Collections.unmodifiableList(clientResponseFilters.get());
        //Liberty code change end
    }

    @Override
    public Configuration getConfiguration(Message m) {
        return (Configuration) m.getExchange().getOutMessage().getContextualProperty(Configuration.class.getName());
    }

    //Liberty code change start defect 181480
    @Override
    public void clearThreadLocalProxies() {
        //Clear saved ThreadLocalProxy object set.
        //All the created ThreadLocalProxy objects are added to the set in AbstractResourceInfo class
        @SuppressWarnings("unchecked")
        ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>> proxySet = (ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>>) getBus().getProperty(AbstractResourceInfo.PROXY_SET);
        if (proxySet != null) {
            Object[] proxies = proxySet.toArray();
            for (Object proxy : proxies) {
                ((ThreadLocalProxy<?>) proxy).remove();
            }
        }
    }
    //Liberty code change end

    public RxInvokerProvider<?> getRxInvokerProvider() {
        return rxInvokerProvider;
    }
}
