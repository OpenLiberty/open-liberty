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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public final class ClientProviderFactory extends ProviderFactory {
    private static final TraceComponent tc = Tr.register(ClientProviderFactory.class);
    private final List<ProviderInfo<ClientRequestFilter>> clientRequestFilters;
    private final List<ProviderInfo<ClientResponseFilter>> clientResponseFilters;
    private final List<ProviderInfo<ResponseExceptionMapper<?>>> responseExceptionMappers;

    private ClientProviderFactory(Bus bus) {
        super(bus);

        //Liberty 226760 begin
        String javaVersion = AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty("java.version");
            }
        });

        CharSequence cs = "1.7";

        //In Java 7 CopyOnWriteArrayList doesn't support "sort".
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, javaVersion);
        }
        if (javaVersion.contains(cs)) {
            this.clientRequestFilters = new ArrayList<ProviderInfo<ClientRequestFilter>>(1);
            this.clientResponseFilters = new ArrayList<ProviderInfo<ClientResponseFilter>>(1);
            this.responseExceptionMappers = new ArrayList<ProviderInfo<ResponseExceptionMapper<?>>>(1);
        } else {
            this.clientRequestFilters = new CopyOnWriteArrayList<ProviderInfo<ClientRequestFilter>>();
            this.clientResponseFilters = new CopyOnWriteArrayList<ProviderInfo<ClientResponseFilter>>();
            this.responseExceptionMappers = new CopyOnWriteArrayList<ProviderInfo<ResponseExceptionMapper<?>>>();
        }
        //Liberty 226760 end
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
        Endpoint e = m.getExchange().getEndpoint();
        return (ClientProviderFactory) e.get(CLIENT_FACTORY_NAME);
    }

    public static ClientProviderFactory getInstance(Endpoint e) {
        return (ClientProviderFactory) e.get(CLIENT_FACTORY_NAME);
    }

    @Override
    protected void setProviders(boolean custom, boolean busGlobal, Object... providers) {
        List<ProviderInfo<? extends Object>> theProviders = prepareProviders(custom, busGlobal, providers, null);
        super.setCommonProviders(theProviders);

        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider.getProvider());
            if (filterContractSupported(provider, providerCls, ClientRequestFilter.class)) {
                addProviderToList(clientRequestFilters, provider);
            }

            if (filterContractSupported(provider, providerCls, ClientResponseFilter.class)) {
                addProviderToList(clientResponseFilters, provider);
            }

            if (ResponseExceptionMapper.class.isAssignableFrom(providerCls)) {
                addProviderToList(responseExceptionMappers, provider);
            }
        }

        Collections.sort(clientRequestFilters,
                         new BindingPriorityComparator(ClientRequestFilter.class, true));
        Collections.sort(clientResponseFilters,
                         new BindingPriorityComparator(ClientResponseFilter.class, false));

        injectContextProxies(responseExceptionMappers, clientRequestFilters, clientResponseFilters);
    }

    @SuppressWarnings("unchecked")
    public <T extends Throwable> ResponseExceptionMapper<T> createResponseExceptionMapper(
                                                                                          Message m, Class<?> paramType) {

        List<ResponseExceptionMapper<?>> candidates = new LinkedList<ResponseExceptionMapper<?>>();

        for (ProviderInfo<ResponseExceptionMapper<?>> em : responseExceptionMappers) {
            if (handleMapper(em, paramType, m, ResponseExceptionMapper.class, true)) {
                candidates.add(em.getProvider());
            }
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ProviderFactory.ClassComparator(paramType));
        return (ResponseExceptionMapper<T>) candidates.get(0);
    }

    @Override
    public void clearProviders() {
        super.clearProviders();
        responseExceptionMappers.clear();
        clientRequestFilters.clear();
        clientResponseFilters.clear();
    }

    public List<ProviderInfo<ClientRequestFilter>> getClientRequestFilters() {
        return Collections.unmodifiableList(clientRequestFilters);
    }

    public List<ProviderInfo<ClientResponseFilter>> getClientResponseFilters() {
        return Collections.unmodifiableList(clientResponseFilters);
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

}