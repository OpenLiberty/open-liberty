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
package org.apache.cxf.jaxrs.sse;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.sse.interceptor.SseInterceptor;

@Provider(value = Type.Feature, scope = Scope.Server)
public class SseFeature extends DelegatingFeature<SseFeature.Portable> {

    public SseFeature() {
        super(new Portable());
    }

    public static class Portable implements AbstractPortableFeature {

        @Override
        public void initialize(Server server, Bus bus) {
            final List<Object> providers = new ArrayList<>();

            providers.add(new SseContextProvider());
            providers.add(new SseEventSinkContextProvider());

            ((ServerProviderFactory) server.getEndpoint().get(
                    ServerProviderFactory.class.getName())).setUserProviders(providers);
        }
        
        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(new SseInterceptor());
        }
    }
}
