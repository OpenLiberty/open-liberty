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
package org.apache.cxf.microprofile.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Configuration;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public final class MicroProfileClientProviderFactory extends ProviderFactory {
    static final String CLIENT_FACTORY_NAME = MicroProfileClientProviderFactory.class.getName();
    private List<ProviderInfo<ResponseExceptionMapper<?>>> responseExceptionMappers =
            new ArrayList<ProviderInfo<ResponseExceptionMapper<?>>>(1);
    private final Comparator<ProviderInfo<?>> comparator;

    private MicroProfileClientProviderFactory(Bus bus, Comparator<ProviderInfo<?>> comparator) {
        super(bus);
        this.comparator = comparator;
    }

    public static MicroProfileClientProviderFactory createInstance(Bus bus,
                                                                   Comparator<ProviderInfo<?>> comparator) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        MicroProfileClientProviderFactory factory = new MicroProfileClientProviderFactory(bus, comparator);
        ProviderFactory.initFactory(factory);
        factory.setBusProviders();
        return factory;
    }

    public static MicroProfileClientProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().getEndpoint();
        return getInstance(e);
    }

    public static MicroProfileClientProviderFactory getInstance(Endpoint e) {
        return (MicroProfileClientProviderFactory)e.get(CLIENT_FACTORY_NAME);
    }

    static Comparator<ProviderInfo<?>> createComparator(MicroProfileClientFactoryBean bean) {
        Comparator<ProviderInfo<?>> parent = ProviderFactory::compareCustomStatus;
        return new ContractComparator(bean, parent);
    }

    @Override
    protected void setProviders(boolean custom, boolean busGlobal, Object... providers) {
        List<ProviderInfo<?>> theProviders =
                prepareProviders(custom, busGlobal, providers, null);
        super.setCommonProviders(theProviders);
        for (ProviderInfo<?> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider.getProvider());

            if (ResponseExceptionMapper.class.isAssignableFrom(providerCls)) {
                addProviderToList(responseExceptionMappers, provider);
            }

        }
        responseExceptionMappers.sort(comparator);

        injectContextProxies(responseExceptionMappers);
    }

    public List<ResponseExceptionMapper<?>> createResponseExceptionMapper(Message m, Class<?> paramType) {

        if (responseExceptionMappers.isEmpty()) {
            return Collections.emptyList();
        }
        List<ResponseExceptionMapper<?>> candidates =
            responseExceptionMappers.stream().map(ProviderInfo::getProvider).collect(Collectors.toList());
        candidates.sort(new ResponseExceptionMapperComparator());
        return Collections.unmodifiableList(candidates);
    }

    @Override
    public void clearProviders() {
        super.clearProviders();
        responseExceptionMappers.clear();
    }

    @Override
    public Configuration getConfiguration(Message m) {
        return (Configuration)m.getExchange().getOutMessage()
                .getContextualProperty(Configuration.class.getName());
    }

    private class ResponseExceptionMapperComparator implements Comparator<ResponseExceptionMapper<?>> {

        @Override
        public int compare(ResponseExceptionMapper<?> oLeft, ResponseExceptionMapper<?> oRight) {
            return oLeft.getPriority() - oRight.getPriority();
        }
    }
}
